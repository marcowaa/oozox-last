package com.example.util

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID

@Serializable
enum class SignalingMessageType {
    OFFER,
    ANSWER,
    ICE_CANDIDATE,
    CALL_REQUEST,
    CALL_ACCEPTED,
    CALL_REJECTED,
    HANGUP,
    HEARTBEAT
}

@Serializable
data class SignalingPayload(
    val type: SignalingMessageType,
    val callId: String,
    val senderId: String,
    val targetId: String,
    val sdp: String? = null,
    val candidate: IceCandidateData? = null,
    val isVideo: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class IceCandidateData(
    val sdpMid: String?,
    val sdpMLineIndex: Int,
    val sdpCandidate: String
)

@Serializable
data class IceServerConfig(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

/**
 * Client-Side WebRTC Signaling & Call Lifecycle Manager
 * Handles bidirectional WebSocket events, ICE Candidate exchange, and SDP negotiations.
 */
class WebRtcSignalingManager(
    private val currentUserId: String,
    private val serverUrl: String = "wss://signaling.oozox.com/ws",
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null

    private val _connectionState = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _connectionState

    private val _incomingCalls = MutableSharedFlow<SignalingPayload>()
    val incomingCalls: SharedFlow<SignalingPayload> = _incomingCalls

    private val _remoteSdpEvents = MutableSharedFlow<SignalingPayload>()
    val remoteSdpEvents: SharedFlow<SignalingPayload> = _remoteSdpEvents

    private val _remoteIceCandidates = MutableSharedFlow<IceCandidateData>()
    val remoteIceCandidates: SharedFlow<IceCandidateData> = _remoteIceCandidates

    private val _callEndEvents = MutableSharedFlow<String>()
    val callEndEvents: SharedFlow<String> = _callEndEvents

    // Public STUN discovery only by default; TURN credentials MUST be injected via updateEphemeralIceServers()
    val defaultIceServers = listOf(
        IceServerConfig(urls = listOf("stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302"))
    )

    private val _activeIceServers = MutableStateFlow<List<IceServerConfig>>(defaultIceServers)
    val activeIceServers: StateFlow<List<IceServerConfig>> = _activeIceServers

    /**
     * Updates active ICE servers with Ephemeral Coturn credentials fetched from backend REST API
     */
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
        val request = Request.Builder()
            .url(serverUrl)
            .header("Authorization", "Bearer $authToken")
            .build()

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
                Log.e("WebRtcSignaling", "Signaling WebSocket failure: ${t.message}")
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
                val ping = SignalingPayload(
                    type = SignalingMessageType.HEARTBEAT,
                    callId = "",
                    senderId = currentUserId,
                    targetId = "SERVER"
                )
                sendMessage(ping)
            }
        }
    }

    fun initiateCall(targetUserId: String, isVideo: Boolean): String {
        val callId = UUID.randomUUID().toString()
        val payload = SignalingPayload(
            type = SignalingMessageType.CALL_REQUEST,
            callId = callId,
            senderId = currentUserId,
            targetId = targetUserId,
            isVideo = isVideo
        )
        sendMessage(payload)
        return callId
    }

    fun sendOffer(callId: String, targetUserId: String, sdp: String, isVideo: Boolean) {
        val payload = SignalingPayload(
            type = SignalingMessageType.OFFER,
            callId = callId,
            senderId = currentUserId,
            targetId = targetUserId,
            sdp = sdp,
            isVideo = isVideo
        )
        sendMessage(payload)
    }

    fun sendAnswer(callId: String, targetUserId: String, sdp: String) {
        val payload = SignalingPayload(
            type = SignalingMessageType.ANSWER,
            callId = callId,
            senderId = currentUserId,
            targetId = targetUserId,
            sdp = sdp
        )
        sendMessage(payload)
    }

    fun sendIceCandidate(callId: String, targetUserId: String, candidate: IceCandidateData) {
        val payload = SignalingPayload(
            type = SignalingMessageType.ICE_CANDIDATE,
            callId = callId,
            senderId = currentUserId,
            targetId = targetUserId,
            candidate = candidate
        )
        sendMessage(payload)
    }

    fun endCall(callId: String, targetUserId: String) {
        val payload = SignalingPayload(
            type = SignalingMessageType.HANGUP,
            callId = callId,
            senderId = currentUserId,
            targetId = targetUserId
        )
        sendMessage(payload)
        coroutineScope.launch {
            _callEndEvents.emit(callId)
        }
    }

    /**
     * Dispatches incoming raw WebSocket JSON message to appropriate flow.
     * Supports newline-delimited JSON (NDJSON) batches to avoid parsing errors
     * if the server flushes multiple frames in a single TCP/WS payload.
     */
    fun onRawMessageReceived(messageJson: String) {
        messageJson.lineSequence().filter { it.isNotBlank() }.forEach { rawLine ->
            try {
                val payload = json.decodeFromString<SignalingPayload>(rawLine.trim())
                coroutineScope.launch {
                    when (payload.type) {
                        SignalingMessageType.CALL_REQUEST -> _incomingCalls.emit(payload)
                        SignalingMessageType.OFFER, SignalingMessageType.ANSWER -> _remoteSdpEvents.emit(payload)
                        SignalingMessageType.ICE_CANDIDATE -> payload.candidate?.let { _remoteIceCandidates.emit(it) }
                        SignalingMessageType.HANGUP, SignalingMessageType.CALL_REJECTED -> _callEndEvents.emit(payload.callId)
                        SignalingMessageType.CALL_ACCEPTED, SignalingMessageType.HEARTBEAT -> Unit
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendMessage(payload: SignalingPayload) {
        try {
            val serialized = json.encodeToString(payload)
            webSocket?.send(serialized)
        } catch (e: Exception) {
            Log.e("WebRtcSignaling", "Failed sending signaling message", e)
        }
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Client disconnected")
        webSocket = null
        _connectionState.value = false
        coroutineScope.cancel()
    }
}
