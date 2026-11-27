package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.MessageType
import com.example.data.repository.AuthRepository
import com.example.service.CallForegroundService
import com.example.ui.theme.*
import com.example.util.ApiCallAuthorizeRequest
import com.example.util.IceServerConfig
import com.example.util.RetrofitClient
import com.example.util.WebRtcPeerManager
import com.example.util.WebRtcSignalingManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import java.util.UUID
import kotlin.math.roundToInt

enum class CallType(val label: String, val icon: String) {
    AUDIO("مكالمة صوتية مشفرة", "📞"),
    VIDEO("مكالمة فيديو عالية الدقة", "📹")
}

enum class CallState {
    OUTGOING_RINGING,
    INCOMING_RINGING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    MUTED,
    HOLD,
    ENDED
}

/**
 * Advanced Call Overlay supporting all Voice & Video Call States,
 * with Rich Cards, Quality Indicators, Audio Visualizers, Picture-in-Picture,
 * and Minimized Floating Call Card for multitasking.
 */
@Composable
fun CallOverlayDialog(
    callType: CallType,
    contactName: String,
    contactAvatarUri: String? = null,
    isTrader: Boolean = false,
    isIncoming: Boolean = false,
    calleeUserId: String = "",
    currentUserName: String = "",
    incomingCallId: String? = null,
    incomingOfferSdp: String? = null,
    signalingManager: WebRtcSignalingManager? = null,
    onBudgetExceeded: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSendMessageNotice: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isMinimized by remember { mutableStateOf(false) }
    var callState by remember {
        mutableStateOf(if (isIncoming) CallState.INCOMING_RINGING else CallState.OUTGOING_RINGING)
    }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(callType == CallType.VIDEO) }
    var isVideoCameraFront by remember { mutableStateOf(true) }
    var isVideoEnabled by remember { mutableStateOf(callType == CallType.VIDEO) }
    var isOnHold by remember { mutableStateOf(false) }
    var callDurationSeconds by remember { mutableIntStateOf(0) }
    var networkQuality by remember { mutableStateOf("P2P مشفر") }
    var activeCallId by remember { mutableStateOf(incomingCallId ?: UUID.randomUUID().toString()) }
    var peerManager by remember { mutableStateOf<WebRtcPeerManager?>(null) }

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    // Permission checks
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasMicPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasMicPermission
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
    }

    LaunchedEffect(Unit) {
        val needed = mutableListOf<String>()
        if (!hasMicPermission) needed.add(Manifest.permission.RECORD_AUDIO)
        if (callType == CallType.VIDEO && !hasCameraPermission) needed.add(Manifest.permission.CAMERA)
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    suspend fun fetchIceServersWithTurn(): List<IceServerConfig> {
        val base = listOf(
            IceServerConfig(urls = listOf("stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302"))
        )
        return try {
            val turnRes = RetrofitClient.apiService().fetchTurnCredentials()
            val turnBody = turnRes.body()
            if (turnRes.isSuccessful && turnBody != null && turnBody.urls.isNotEmpty()) {
                val merged = base + IceServerConfig(
                    urls = turnBody.urls,
                    username = turnBody.username.takeIf { it.isNotBlank() },
                    credential = turnBody.credential.takeIf { it.isNotBlank() }
                )
                signalingManager?.updateEphemeralIceServers(merged)
                merged
            } else base
        } catch (_: Exception) {
            base
        }
    }

    fun buildPeerManager(iceServers: List<IceServerConfig>, resolvedCallId: String): WebRtcPeerManager {
        return WebRtcPeerManager(
            context = context,
            isVideoCall = callType == CallType.VIDEO,
            iceServers = iceServers,
            onLocalIceCandidate = { cand ->
                if (calleeUserId.isNotBlank()) {
                    signalingManager?.sendIceCandidate(resolvedCallId, calleeUserId, cand)
                }
            },
            onConnectionStateChanged = { state ->
                scope.launch {
                    when (state) {
                        PeerConnection.PeerConnectionState.CONNECTED -> {
                            callState = CallState.CONNECTED
                            networkQuality = "P2P HD (DTLS-SRTP)"
                        }
                        PeerConnection.PeerConnectionState.DISCONNECTED -> {
                            if (callState == CallState.CONNECTED) {
                                callState = CallState.RECONNECTING
                                networkQuality = "إعادة اتصال..."
                            }
                        }
                        PeerConnection.PeerConnectionState.FAILED,
                        PeerConnection.PeerConnectionState.CLOSED -> {
                            if (callState != CallState.ENDED) {
                                callState = CallState.ENDED
                                onDismiss()
                            }
                        }
                        else -> Unit
                    }
                }
            }
        )
    }

    // Outgoing Call: Backend Budget Check (/api/calls/authorize) -> TURN -> Foreground Service -> WebRTC Offer
    LaunchedEffect(isIncoming, calleeUserId, callType) {
        if (!isIncoming) {
            var resolvedCallId = activeCallId
            var callAuthExp = 0L
            var callAuthSignature = ""

            if (RetrofitClient.hasActiveSession() && calleeUserId.isNotBlank()) {
                try {
                    val authRes = RetrofitClient.apiService().authorizeCall(
                        ApiCallAuthorizeRequest(
                            calleeId = calleeUserId,
                            callType = if (callType == CallType.VIDEO) "VIDEO" else "AUDIO",
                            callId = resolvedCallId
                        )
                    )
                    if (!authRes.isSuccessful) {
                        val parsed = AuthRepository.parseErrorBody(
                            authRes.errorBody()?.string(),
                            com.example.util.AppErrorMapper.fromHttpStatus(
                                authRes.code(),
                                fallbackAr = "تعذر بدء المكالمة",
                                fallbackEn = "Couldn't start call"
                            )
                        )
                        if (authRes.code() == 429 || parsed.code.contains("BUDGET", true) || parsed.code.contains("LIMIT", true)) {
                            onBudgetExceeded(parsed.message)
                            callState = CallState.ENDED
                            onDismiss()
                            return@LaunchedEffect
                        }
                    } else {
                        authRes.body()?.data?.let { d ->
                            if (d.callId.isNotBlank()) {
                                resolvedCallId = d.callId
                                activeCallId = d.callId
                            }
                            callAuthExp = d.exp
                            callAuthSignature = d.callAuthSignature
                        }
                    }
                } catch (_: Exception) {
                }
            }

            CallForegroundService.start(context, resolvedCallId, contactName, callType == CallType.VIDEO)
            val iceServers = fetchIceServersWithTurn()
            val pm = buildPeerManager(iceServers, resolvedCallId)
            peerManager = pm
            pm.setMicrophoneMuted(isMuted)
            audioManager?.isSpeakerphoneOn = isSpeakerOn

            if (calleeUserId.isNotBlank()) {
                pm.createOffer(
                    onOfferSdp = { sdp ->
                        signalingManager?.sendCallOffer(
                            callId = resolvedCallId,
                            targetUserId = calleeUserId,
                            sdp = sdp,
                            isVideo = callType == CallType.VIDEO,
                            callAuthExp = callAuthExp,
                            callAuthSignature = callAuthSignature,
                            senderName = currentUserName
                        )
                    }
                )
            }
        }
    }

    // Listen to remote SDP Answer, ICE Candidates, Hangup, and Budget Blocked signaling frames
    LaunchedEffect(signalingManager, activeCallId) {
        val mgr = signalingManager ?: return@LaunchedEffect
        launch {
            mgr.remoteSdpEvents.collect { ev ->
                if (ev.callId == activeCallId && !ev.sdp.isNullOrBlank()) {
                    callState = CallState.CONNECTING
                    peerManager?.handleRemoteAnswer(ev.sdp)
                }
            }
        }
        launch {
            mgr.remoteIceCandidates.collect { (cId, cand) ->
                if (cId == activeCallId || cId.isBlank()) {
                    peerManager?.addRemoteIceCandidate(cand)
                }
            }
        }
        launch {
            mgr.callEndEvents.collect { endedCallId ->
                if (endedCallId == activeCallId && callState != CallState.ENDED) {
                    callState = CallState.ENDED
                    onDismiss()
                }
            }
        }
        launch {
            mgr.budgetBlockedEvents.collect { blockedId ->
                if (blockedId == activeCallId || blockedId.isBlank()) {
                    onBudgetExceeded("تم إيقاف المكالمات الجديدة لبلوغ الحد الأقصى من الميزانية الشهرية.")
                    callState = CallState.ENDED
                    onDismiss()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            peerManager?.dispose()
            peerManager = null
            CallForegroundService.stop(context)
        }
    }

    LaunchedEffect(callState) {
        if (callState == CallState.CONNECTED || callState == CallState.HOLD) {
            while (callState == CallState.CONNECTED || callState == CallState.HOLD) {
                delay(1000L)
                if (!isOnHold) {
                    callDurationSeconds++
                }
            }
        }
    }

    fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "CallPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Render Minimized Floating Card OR Full Screen Overlay
    if (isMinimized) {
        MinimizedFloatingCallCard(
            contactName = contactName,
            contactAvatarUri = contactAvatarUri,
            callType = callType,
            callState = callState,
            durationText = formatDuration(callDurationSeconds),
            isMuted = isMuted,
            onExpand = { isMinimized = false },
            onEndCall = {
                callState = CallState.ENDED
                if (callDurationSeconds > 0) {
                    onSendMessageNotice("انتهت ${callType.label} (المدة: ${formatDuration(callDurationSeconds)})", MessageType.CALL.name)
                } else {
                    onSendMessageNotice("مكالمة فائتة (${callType.label})", MessageType.MISSED_CALL.name)
                }
                onDismiss()
            },
            onToggleMute = { isMuted = !isMuted }
        )
    } else {
        Dialog(
            onDismissRequest = {
                // If user clicks back, minimize instead of abruptly disconnecting
                if (callState == CallState.CONNECTED || callState == CallState.CONNECTING || callState == CallState.HOLD) {
                    isMinimized = true
                } else {
                    onDismiss()
                }
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF070B14),
                                Color.Black
                            )
                        )
                    )
            ) {
                // Fullscreen Remote Video Stream (SurfaceViewRenderer backed by WebRTC)
                if (callType == CallType.VIDEO && isVideoEnabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        val activePm = peerManager
                        if (activePm != null) {
                            AndroidView(
                                factory = { ctx ->
                                    SurfaceViewRenderer(ctx).apply {
                                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                                        setEnableHardwareScaler(true)
                                        activePm.attachRenderers(localView = null, remoteView = this)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        if (callState != CallState.CONNECTED) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = GoldPrimary.copy(alpha = 0.55f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "بث الفيديو المباشر عالي الدقة (WebRTC P2P)",
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Foreground Call Stage (Respects Safe Drawing Insets without Overlap)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Navigation Bar: Minimize, Encryption & Signal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minimize button
                        Surface(
                            onClick = { isMinimized = true },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .size(42.dp)
                                .testTag("btn_minimize_call")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "تصغير المكالمة",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Encryption Pill Badge
                        Surface(
                            color = GoldDark.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "مشفر",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "E2EE مشفر 256-bit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        // Network Quality Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                Text(networkQuality, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    val statusText = when (callState) {
                        CallState.OUTGOING_RINGING -> "جاري الاتصال والرنين..."
                        CallState.INCOMING_RINGING -> "مكالمة واردة..."
                        CallState.CONNECTING -> "جاري إنشاء الاتصال وتشفير الصوت..."
                        CallState.CONNECTED -> if (isOnHold) "المكالمة معلقة" else formatDuration(callDurationSeconds)
                        CallState.RECONNECTING -> "جاري إعادة الاتصال..."
                        CallState.MUTED -> "الميكروفون مكتوم (${formatDuration(callDurationSeconds)})"
                        CallState.HOLD -> "المكالمة معلقة (${formatDuration(callDurationSeconds)})"
                        CallState.ENDED -> "تم إنهاء المكالمة"
                    }

                    val statusColor = when (callState) {
                        CallState.CONNECTED -> if (isOnHold) adaptiveStatusOrange else GoldLight
                        CallState.ENDED -> Color(0xFFFCA5A5)
                        else -> Color.White.copy(alpha = 0.85f)
                    }

                    // Middle Stage: Video Header Row (No Overlap) OR Centered Audio Visualizer
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (callType == CallType.VIDEO && isVideoEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Caller Info Card on Start side
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.Black.copy(alpha = 0.42f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = contactName,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isTrader) {
                                            Surface(
                                                color = GoldPrimary.copy(alpha = 0.22f),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
                                            ) {
                                                Text(
                                                    text = "تاجر وصائغ ذهب معتمد",
                                                    fontSize = 10.5.sp,
                                                    color = GoldLight,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = statusText,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = statusColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Picture in Picture (Self Video Preview) safely occupying End slot
                                Box(
                                    modifier = Modifier
                                        .size(width = 104.dp, height = 144.dp)
                                        .shadow(8.dp, RoundedCornerShape(16.dp))
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF334155))
                                        .border(2.dp, GoldPrimary, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val activePm = peerManager
                                    if (activePm != null && hasCameraPermission) {
                                        AndroidView(
                                            factory = { ctx ->
                                                SurfaceViewRenderer(ctx).apply {
                                                    setZOrderMediaOverlay(true)
                                                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                                                    setEnableHardwareScaler(true)
                                                    activePm.attachRenderers(localView = this, remoteView = null)
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isVideoCameraFront) Icons.Default.Person else Icons.Default.FlipCameraAndroid,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isVideoCameraFront) "كاميرا أمامية" else "كاميرا خلفية",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Centered Audio Call Stage
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = contactName,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isTrader) {
                                        Surface(
                                            color = GoldPrimary.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "تاجر وصائغ ذهب معتمد",
                                                fontSize = 11.sp,
                                                color = GoldLight,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = statusText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = statusColor,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Box(contentAlignment = Alignment.Center) {
                                    Box(
                                        modifier = Modifier
                                            .size(160.dp * (if (callState == CallState.CONNECTED && !isMuted && !isOnHold) pulseScale else 1f))
                                            .clip(CircleShape)
                                            .background(GoldPrimary.copy(alpha = 0.15f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(136.dp)
                                            .clip(CircleShape)
                                            .background(GoldDark.copy(alpha = 0.45f))
                                    )
                                    OozoxAvatarImage(
                                        model = contactAvatarUri,
                                        name = contactName,
                                        size = 115.dp,
                                        isTrader = isTrader,
                                        contentDescription = contactName
                                    )
                                }

                                if (callState == CallState.CONNECTED && !isMuted && !isOnHold) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        val heights = listOf(8.dp, 18.dp, 28.dp, 14.dp, 34.dp, 22.dp, 12.dp, 26.dp, 16.dp, 30.dp, 10.dp)
                                        heights.forEachIndexed { idx, height ->
                                            val animatedH by infiniteTransition.animateFloat(
                                                initialValue = 6f,
                                                targetValue = height.value,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(380 + (idx * 50), easing = LinearEasing),
                                                    repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "bar_$idx"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(4.5.dp)
                                                    .height(animatedH.dp)
                                                    .background(
                                                        brush = Brush.verticalGradient(
                                                            listOf(GoldPrimary, GoldLight)
                                                        ),
                                                        shape = RoundedCornerShape(3.dp)
                                                    )
                                            )
                                        }
                                    }
                                } else if (isOnHold) {
                                    Surface(
                                        color = StatusOrange.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, StatusOrange)
                                    ) {
                                        Text(
                                            text = "تم تعليق المكالمة مؤقتاً",
                                            color = StatusOrange,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Call Controls Grid & Action Buttons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp, start = 4.dp, end = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (callState == CallState.INCOMING_RINGING) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Button(
                                        onClick = {
                                            callState = CallState.ENDED
                                            if (calleeUserId.isNotBlank()) {
                                                signalingManager?.rejectCall(activeCallId, calleeUserId, callType == CallType.VIDEO)
                                            }
                                            onSendMessageNotice("مكالمة فائتة (${callType.label})", MessageType.MISSED_CALL.name)
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        shape = CircleShape,
                                        modifier = Modifier.size(68.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.CallEnd, contentDescription = "رفض", tint = Color.White, modifier = Modifier.size(32.dp))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("رفض", color = Color.White, fontSize = 12.sp)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Button(
                                        onClick = {
                                            callState = CallState.CONNECTING
                                            scope.launch {
                                                CallForegroundService.start(context, activeCallId, contactName, callType == CallType.VIDEO)
                                                val iceServers = fetchIceServersWithTurn()
                                                val pm = buildPeerManager(iceServers, activeCallId)
                                                peerManager = pm
                                                pm.setMicrophoneMuted(isMuted)
                                                audioManager?.isSpeakerphoneOn = isSpeakerOn
                                                val offerSdp = incomingOfferSdp
                                                if (!offerSdp.isNullOrBlank()) {
                                                    pm.handleRemoteOfferAndCreateAnswer(
                                                        offerSdp = offerSdp,
                                                        onAnswerSdp = { ansSdp ->
                                                            if (calleeUserId.isNotBlank()) {
                                                                signalingManager?.sendCallAccept(activeCallId, calleeUserId, ansSdp, callType == CallType.VIDEO)
                                                            }
                                                            callState = CallState.CONNECTED
                                                        }
                                                    )
                                                } else {
                                                    callState = CallState.CONNECTED
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = CircleShape,
                                        modifier = Modifier.size(68.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "قبول", tint = Color.White, modifier = Modifier.size(32.dp))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("رد وتحدث", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledIconButton(
                                    onClick = {
                                        isMuted = !isMuted
                                        peerManager?.setMicrophoneMuted(isMuted || isOnHold)
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (isMuted) Color(0xFFEF4444) else Color.White.copy(alpha = 0.18f),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "كتم الميكروفون",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                FilledIconButton(
                                    onClick = {
                                        isSpeakerOn = !isSpeakerOn
                                        audioManager?.isSpeakerphoneOn = isSpeakerOn
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (isSpeakerOn) GoldPrimary else Color.White.copy(alpha = 0.18f),
                                        contentColor = if (isSpeakerOn) Color.Black else Color.White
                                    ),
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                        contentDescription = "مكبر الصوت",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                FilledIconButton(
                                    onClick = {
                                        isOnHold = !isOnHold
                                        callState = if (isOnHold) CallState.HOLD else CallState.CONNECTED
                                        peerManager?.setMicrophoneMuted(isMuted || isOnHold)
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (isOnHold) StatusOrange else Color.White.copy(alpha = 0.18f),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = "تعليق المكالمة",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                if (callType == CallType.VIDEO) {
                                    FilledIconButton(
                                        onClick = {
                                            isVideoEnabled = !isVideoEnabled
                                            peerManager?.setVideoEnabled(isVideoEnabled)
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = if (!isVideoEnabled) Color(0xFFEF4444) else Color.White.copy(alpha = 0.18f),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                            contentDescription = "تشغيل/إيقاف الفيديو",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    FilledIconButton(
                                        onClick = {
                                            isVideoCameraFront = !isVideoCameraFront
                                            peerManager?.switchCamera()
                                        },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = Color.White.copy(alpha = 0.18f),
                                            contentColor = GoldAccent
                                        ),
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FlipCameraAndroid,
                                            contentDescription = "تبديل الكاميرا",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    callState = CallState.ENDED
                                    if (calleeUserId.isNotBlank()) {
                                        signalingManager?.endCall(activeCallId, calleeUserId, callType == CallType.VIDEO)
                                    }
                                    if (callDurationSeconds > 0) {
                                        onSendMessageNotice("انتهت ${callType.label} (المدة: ${formatDuration(callDurationSeconds)})", MessageType.CALL.name)
                                    } else {
                                        onSendMessageNotice("مكالمة فائتة (${callType.label})", MessageType.MISSED_CALL.name)
                                    }
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(68.dp)
                                    .testTag("btn_end_call"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "إنهاء المكالمة",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Minimized Floating Call Card for Multitasking
 * Displays caller name, duration, mute status, and actions to re-expand or end call.
 */
@Composable
fun MinimizedFloatingCallCard(
    contactName: String,
    contactAvatarUri: String? = null,
    callType: CallType,
    callState: CallState,
    durationText: String,
    isMuted: Boolean,
    onExpand: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.5.dp, GoldPrimary),
            modifier = modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .widthIn(max = 280.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .clickable { onExpand() }
                .testTag("minimized_call_card")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Avatar with pulsating ring
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.2f))
                    )
                    OozoxAvatarImage(
                        model = contactAvatarUri,
                        name = contactName,
                        size = 36.dp,
                        contentDescription = contactName
                    )
                }

                // Caller info & timer
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contactName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "$durationText • ${if (callType == CallType.VIDEO) "فيديو" else "صوت"}",
                            fontSize = 11.sp,
                            color = GoldLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Mute toggle
                OozoxCircleIconButton(
                    onClick = onToggleMute,
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "كتم",
                    tint = if (isMuted) Color(0xFFEF4444) else GoldLight,
                    containerColor = Color.White.copy(alpha = 0.08f),
                    circleSize = OozoxCircleButtonDefaults.CircleSize,
                    touchSize = 40.dp
                )

                // End call button
                OozoxCircleIconButton(
                    onClick = onEndCall,
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "إنهاء",
                    tint = Color.White,
                    containerColor = Color(0xFFDC2626),
                    circleSize = OozoxCircleButtonDefaults.CircleSize,
                    touchSize = 40.dp
                )
            }
        }
    }
}
