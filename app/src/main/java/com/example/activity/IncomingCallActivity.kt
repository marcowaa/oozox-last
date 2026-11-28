package com.example.activity

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.service.CallForegroundService
import com.example.ui.components.OozoxAvatarImage
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.util.FcmNotificationManager
import com.example.util.NotificationHelper
import kotlinx.coroutines.launch

/**
 * Full-screen lock-screen incoming call activity triggered via NotificationCompat.CallStyle
 * fullScreenIntent when a remote peer or FCM high-priority push initiates a call.
 */
class IncomingCallActivity : ComponentActivity() {

    companion object {
        @Volatile
        private var activeInstance: IncomingCallActivity? = null

        fun dismissIfActive(callId: String? = null) {
            val inst = activeInstance ?: return
            if (callId.isNullOrBlank() || inst.currentCallId == callId) {
                inst.runOnUiThread {
                    inst.stopRingtoneAndVibration()
                    inst.finishAndRemoveTaskSafely()
                }
            }
        }
    }

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var currentCallId: String = ""
    private var currentCallerId: String = ""
    private var currentCallerName: String = ""
    private var currentIsVideo: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        activeInstance = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        extractCallIntent(intent)
        if (intent?.getStringExtra(NotificationHelper.EXTRA_INCOMING_CALL_ACTION) == NotificationHelper.CALL_ACTION_DECLINE) {
            handleDecline()
            return
        }

        startRingtoneAndVibration()

        lifecycleScope.launch {
            FcmNotificationManager.callEndedPushEvents.collect { endedCallId ->
                if (endedCallId == currentCallId || currentCallId.isBlank()) {
                    NotificationHelper.cancelIncomingCallNotification(this@IncomingCallActivity, endedCallId)
                    stopRingtoneAndVibration()
                    finishAndRemoveTaskSafely()
                }
            }
        }

        setContent {
            MaterialTheme {
                IncomingCallScreen(
                    callerName = currentCallerName,
                    isVideo = currentIsVideo,
                    onDecline = { handleDecline() },
                    onAccept = { handleAccept() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractCallIntent(intent)
        if (intent.getStringExtra(NotificationHelper.EXTRA_INCOMING_CALL_ACTION) == NotificationHelper.CALL_ACTION_DECLINE) {
            handleDecline()
        }
    }

    private fun extractCallIntent(src: Intent?) {
        currentCallId = src?.getStringExtra(NotificationHelper.EXTRA_CALL_ID).orEmpty()
        currentCallerId = src?.getStringExtra(NotificationHelper.EXTRA_CALLER_ID).orEmpty()
        currentCallerName = src?.getStringExtra(NotificationHelper.EXTRA_CALLER_NAME).orEmpty().ifBlank { "مستخدم OOZOX" }
        currentIsVideo = src?.getBooleanExtra(NotificationHelper.EXTRA_IS_VIDEO, false) == true
    }

    private fun startRingtoneAndVibration() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ringtone = RingtoneManager.getRingtone(applicationContext, uri)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        isLooping = true
                    }
                    play()
                }
            }

            if (ringerMode == AudioManager.RINGER_MODE_NORMAL || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                val pattern = longArrayOf(0, 600, 450, 600, 900)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun stopRingtoneAndVibration() {
        try {
            ringtone?.stop()
        } catch (_: Exception) {
        }
        ringtone = null
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
        vibrator = null
    }

    private fun handleAccept() {
        stopRingtoneAndVibration()
        NotificationHelper.cancelIncomingCallNotification(this, currentCallId)

        val launchMain = {
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(NotificationHelper.EXTRA_INCOMING_CALL_ACTION, NotificationHelper.CALL_ACTION_ACCEPT)
                putExtra(NotificationHelper.EXTRA_CALL_ID, currentCallId)
                putExtra(NotificationHelper.EXTRA_CALLER_ID, currentCallerId)
                putExtra(NotificationHelper.EXTRA_CALLER_NAME, currentCallerName)
                putExtra(NotificationHelper.EXTRA_IS_VIDEO, currentIsVideo)
            }
            startActivity(mainIntent)
            finishAndRemoveTaskSafely()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km?.isKeyguardLocked == true) {
                km.requestDismissKeyguard(
                    this,
                    object : KeyguardManager.KeyguardDismissCallback() {
                        override fun onDismissSucceeded() {
                            launchMain()
                        }

                        override fun onDismissCancelled() {
                            launchMain()
                        }

                        override fun onDismissError() {
                            launchMain()
                        }
                    }
                )
                return
            }
        }
        launchMain()
    }

    private fun handleDecline() {
        stopRingtoneAndVibration()
        NotificationHelper.cancelIncomingCallNotification(this, currentCallId)
        CallForegroundService.rejectIncomingCallSilently(currentCallId, currentCallerId, currentIsVideo)
        finishAndRemoveTaskSafely()
    }

    private fun finishAndRemoveTaskSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        if (activeInstance === this) {
            activeInstance = null
        }
        stopRingtoneAndVibration()
        super.onDestroy()
    }
}

@Composable
private fun IncomingCallScreen(
    callerName: String,
    isVideo: Boolean,
    onDecline: () -> Unit,
    onAccept: () -> Unit
) {
    BackHandler {
        // Require explicit accept or decline while ringing
    }

    val infiniteTransition = rememberInfiniteTransition(label = "IncomingCallPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IncomingPulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF070B14), Color.Black)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = GoldDark.copy(alpha = 0.45f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isVideo) "مكالمة فيديو مشفرة واردة" else "مكالمة صوتية مشفرة واردة",
                        color = GoldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(168.dp * pulseScale)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.14f))
                    )
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(GoldDark.copy(alpha = 0.5f))
                    )
                    OozoxAvatarImage(
                        model = null,
                        name = callerName,
                        size = 118.dp,
                        contentDescription = callerName
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = callerName,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "OOZOX • اتصال آمن P2P",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 14.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onDecline,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("btn_incoming_decline")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "رفض المكالمة",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("رفض", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("btn_incoming_accept")
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = "رد على المكالمة",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("رد", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
