package com.example.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

object SignalingMessageType {
    const val CALL_OFFER = "CALL_OFFER"
    const val CALL_ACCEPT = "CALL_ACCEPT"
    const val CALL_ACCEPTED = "CALL_ACCEPTED"
    const val CALL_ANSWER = "CALL_ANSWER"
    const val CALL_REJECT = "CALL_REJECT"
    const val CALL_END = "CALL_END"
    const val CALL_ENDED = "CALL_ENDED"
    const val CALL_MISSED = "CALL_MISSED"
    const val ICE_CANDIDATE = "ICE_CANDIDATE"
    const val CALL_BUDGET_BLOCKED = "CALL_BUDGET_BLOCKED"
    const val HEARTBEAT = "HEARTBEAT"
}

data class SignalingPayload(
    val type: String,
    val callId: String = "",
    val senderId: String = "",
    val targetId: String = "",
    val senderName: String = "",
    val sdp: String? = null,
    val candidate: IceCandidateData? = null,
    val isVideo: Boolean = false,
    val callAuthExp: Long = 0L,
    val callAuthSignature: String? = null,
    val reason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class IceCandidateData(
    val sdpMid: String?,
    val sdpMLineIndex: Int,
    val sdpCandidate: String
)

data class IceServerConfig(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

data class RealtimeTransferEvent(
    val type: String,
    val transferId: String = "",
    val status: String = ""
)

/**
 * Client-Side WebRTC Signaling & Realtime Event Manager
 * Connects to Go Signaling WS (/ws) with JWT + Device-ID headers and relays:
 * - 1-on-1 P2P Call SDP Offer/Answer + ICE Candidates + 100% Budget Signatures
 * - Cross-device encrypted account transfer requests & completions
 * - Remote force_logout / session_revoked events
 */
class WebRtcSignalingManager(
    private val currentUserId: String,
    private val deviceId: String = "",
    private val serverUrl: String = "wss://signaling.oozox.com/ws",
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null

    private val _connectionState = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _connectionState

    private val _incomingCalls = MutableSharedFlow<SignalingPayload>(extraBufferCapacity = 16)
    val incomingCalls: SharedFlow<SignalingPayload> = _incomingCalls

    private val _remoteSdpEvents = MutableSharedFlow<SignalingPayload>(extraBufferCapacity = 16)
    val remoteSdpEvents: SharedFlow<SignalingPayload> = _remoteSdpEvents

    private val _remoteIceCandidates = MutableSharedFlow<Pair<String, IceCandidateData>>(extraBufferCapacity = 64)
    val remoteIceCandidates: SharedFlow<Pair<String, IceCandidateData>> = _remoteIceCandidates

    private val _callEndEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val callEndEvents: SharedFlow<String> = _callEndEvents

    private val _budgetBlockedEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val budgetBlockedEvents: SharedFlow<String> = _budgetBlockedEvents

    private val _transferEvents = MutableSharedFlow<RealtimeTransferEvent>(extraBufferCapacity = 8)
    val transferEvents: SharedFlow<RealtimeTransferEvent> = _transferEvents

    private val _forceLogoutEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val forceLogoutEvents: SharedFlow<String> = _forceLogoutEvents

    val defaultIceServers = listOf(
        IceServerConfig(urls = listOf("stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302"))
    )

    private val _activeIceServers = MutableStateFlow(defaultIceServers)
    val activeIceServers: StateFlow<List<IceServerConfig>> = _activeIceServers

    fun updateEphemeralIceServers(servers: List<IceServerConfig>) {
        if (servers.isNotEmpty()) {
            _activeIceServers.value = servers
        }
    }

    fun connect(authToken: String? = RetrofitClient.getAuthToken()) {
        if (authToken.isNullOrBlank()) {
            Log.w("WebRtcSignaling", "Cannot connect WebSocket without authenticated JWT token")
            _connectionState.value = false
            return
        }
        webSocket?.cancel()
        val builder = Request.Builder()
            .url(serverUrl)
            .header("Authorization", "Bearer $authToken")
        if (deviceId.isNotBlank()) {
            builder.header("X-Device-Id", deviceId)
        }
        val request = builder.build()

        webSocket = RetrofitClient.okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                _connectionState.value = true
                startHeartbeat()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                onRawMessageReceived(text)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _connectionState.value = false
                heartbeatJob?.cancel()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w("WebRtcSignaling", "Signaling WebSocket closed/offline: ${t.message}")
                _connectionState.value = false
                heartbeatJob?.cancel()
            }
        })
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = coroutineScope.launch {
            while (_connectionState.value) {
                delay(30_000L)
                sendJson(
                    JSONObject().apply {
                        put("type", SignalingMessageType.HEARTBEAT)
                        put("senderId", currentUserId)
                        put("timestamp", System.currentTimeMillis())
                    }
                )
            }
        }
    }

    fun sendCallOffer(
        callId: String,
        targetUserId: String,
        sdp: String,
        isVideo: Boolean,
        callAuthExp: Long,
        callAuthSignature: String,
        senderName: String = ""
    ) {
        sendJson(
            JSONObject().apply {
                put("type", SignalingMessageType.CALL_OFFER)
                put("callId", callId)
                put("senderId", currentUserId)
                put("senderName", senderName)
                put("target", targetUserId)
                put("callType", if (isVideo) "VIDEO" else "AUDIO")
                put("isVideo", isVideo)
                put("sdp", sdp)
                put("callAuthExp", callAuthExp)
                put("callAuthSignature", callAuthSignature)
                put("timestamp", System.currentTimeMillis())
            }
        )
    }

    fun sendCallAccept(
        callId: String,
        targetUserId: String,
        sdp: String,
        isVideo: Boolean
    ) {
        sendJson(
            JSONObject().apply {
                put("type", SignalingMessageType.CALL_ACCEPT)
                put("callId", callId)
                put("senderId", currentUserId)
                put("target", targetUserId)
                put("callType", if (isVideo) "VIDEO" else "AUDIO")
                put("isVideo", isVideo)
                put("sdp", sdp)
                put("timestamp", System.currentTimeMillis())
            }
        )
    }

    fun sendIceCandidate(callId: String, targetUserId: String, candidate: IceCandidateData) {
        sendJson(
            JSONObject().apply {
                put("type", SignalingMessageType.ICE_CANDIDATE)
                put("callId", callId)
                put("senderId", currentUserId)
                put("target", targetUserId)
                put("candidate", JSONObject().apply {
                    put("sdpMid", candidate.sdpMid ?: "")
                    put("sdpMLineIndex", candidate.sdpMLineIndex)
                    put("sdpCandidate", candidate.sdpCandidate)
                })
                put("timestamp", System.currentTimeMillis())
            }
        )
    }

    fun rejectCall(callId: String, targetUserId: String, isVideo: Boolean, reason: String = "declined") {
        sendJson(
            JSONObject().apply {
                put("type", SignalingMessageType.CALL_REJECT)
                put("callId", callId)
                put("senderId", currentUserId)
                put("target", targetUserId)
                put("callType", if (isVideo) "VIDEO" else "AUDIO")
                put("reason", reason)
                put("timestamp", System.currentTimeMillis())
            }
        )
        coroutineScope.launch { _callEndEvents.emit(callId) }
    }

    fun endCall(callId: String, targetUserId: String, isVideo: Boolean = false, reason: String = "hangup") {
        sendJson(
            JSONObject().apply {
                put("type", SignalingMessageType.CALL_END)
                put("callId", callId)
                put("senderId", currentUserId)
                put("target", targetUserId)
                put("callType", if (isVideo) "VIDEO" else "AUDIO")
                put("reason", reason)
                put("timestamp", System.currentTimeMillis())
            }
        )
        coroutineScope.launch { _callEndEvents.emit(callId) }
    }

    /**
     * Dispatches incoming WebSocket JSON lines (supports NDJSON batches from Go Hub).
     */
    fun onRawMessageReceived(messageJson: String) {
        messageJson.lineSequence().filter { it.isNotBlank() }.forEach { rawLine ->
            try {
                val obj = JSONObject(rawLine.trim())
                val rawType = obj.optString("type").trim()
                if (rawType.isBlank()) return@forEach
                val normType = rawType.uppercase()

                coroutineScope.launch {
                    when (normType) {
                        "CALL_OFFER", "OFFER", "CALL_REQUEST" -> {
                            val isVid = obj.optBoolean("isVideo", false) ||
                                obj.optString("callType").equals("VIDEO", ignoreCase = true)
                            _incomingCalls.emit(
                                SignalingPayload(
                                    type = SignalingMessageType.CALL_OFFER,
                                    callId = obj.optString("callId"),
                                    senderId = obj.optString("senderId").ifBlank { obj.optString("from") },
                                    targetId = obj.optString("target").ifBlank { obj.optString("targetId") },
                                    senderName = obj.optString("senderName"),
                                    sdp = obj.optString("sdp").takeIf { it.isNotBlank() },
                                    isVideo = isVid
                                )
                            )
                        }
                        "CALL_ACCEPT", "CALL_ACCEPTED", "CALL_ANSWER", "ANSWER" -> {
                            val isVid = obj.optBoolean("isVideo", false) ||
                                obj.optString("callType").equals("VIDEO", ignoreCase = true)
                            _remoteSdpEvents.emit(
                                SignalingPayload(
                                    type = normType,
                                    callId = obj.optString("callId"),
                                    senderId = obj.optString("senderId").ifBlank { obj.optString("from") },
                                    targetId = obj.optString("target").ifBlank { obj.optString("targetId") },
                                    sdp = obj.optString("sdp").takeIf { it.isNotBlank() },
                                    isVideo = isVid
                                )
                            )
                        }
                        "ICE_CANDIDATE" -> {
                            val candObj = obj.optJSONObject("candidate")
                            if (candObj != null) {
                                val cand = IceCandidateData(
                                    sdpMid = candObj.optString("sdpMid").takeIf { it.isNotBlank() },
                                    sdpMLineIndex = candObj.optInt("sdpMLineIndex", 0),
                                    sdpCandidate = candObj.optString("sdpCandidate")
                                        .ifBlank { candObj.optString("candidate") }
                                )
                                if (cand.sdpCandidate.isNotBlank()) {
                                    _remoteIceCandidates.emit(obj.optString("callId") to cand)
                                }
                            }
                        }
                        "CALL_END", "CALL_ENDED", "HANGUP", "CALL_REJECT", "CALL_REJECTED", "CALL_MISSED" -> {
                            val cId = obj.optString("callId")
                            if (cId.isNotBlank()) {
                                _callEndEvents.emit(cId)
                            }
                        }
                        "CALL_BUDGET_BLOCKED" -> {
                            _budgetBlockedEvents.emit(obj.optString("callId"))
                        }
                        "ACCOUNT_TRANSFER_REQUEST", "ACCOUNT_TRANSFER_READY", "ACCOUNT_TRANSFER_COMPLETED" -> {
                            val payloadObj = obj.optJSONObject("payload")
                            _transferEvents.emit(
                                RealtimeTransferEvent(
                                    type = rawType,
                                    transferId = payloadObj?.optString("transferId").orEmpty(),
                                    status = payloadObj?.optString("status").orEmpty()
                                )
                            )
                        }
                        "FORCE_LOGOUT", "SESSION_REVOKED" -> {
                            val payloadObj = obj.optJSONObject("payload")
                            val reason = payloadObj?.optString("reason")
                                ?: obj.optString("reason")
                                ?: "تم تسجيل الخروج من هذا الجهاز بعد نقل الحساب أو إلغاء الجلسة."
                            _forceLogoutEvents.emit(reason)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("WebRtcSignaling", "Ignored non-standard signaling frame: ${e.message}")
            }
        }
    }

    private fun sendJson(obj: JSONObject) {
        try {
            webSocket?.send(obj.toString())
        } catch (e: Exception) {
            Log.e("WebRtcSignaling", "Failed sending signaling message", e)
        }
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Client disconnected")
        webSocket = null
        _connectionState.value = false
    }
}
