package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.util.FcmNotificationManager
import com.example.util.NotificationHelper
import com.example.util.WebRtcSignalingManager

/**
 * Foreground service keeping 1-on-1 encrypted WebRTC audio/video calls active
 * when the user minimizes the call or switches screens.
 */
class CallForegroundService : Service() {

    companion object {
        const val CHANNEL_CALLS = "oozox_active_calls_channel"
        const val NOTIFICATION_ID = 4401

        const val ACTION_START = "com.example.service.CallForegroundService.START"
        const val ACTION_STOP = "com.example.service.CallForegroundService.STOP"
        const val ACTION_NOTIFICATION_HANGUP = "com.example.service.CallForegroundService.NOTIFICATION_HANGUP"
        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_PEER_NAME = "extra_peer_name"
        const val EXTRA_IS_VIDEO = "extra_is_video"

        @Volatile
        private var activeHangupCallback: (() -> Unit)? = null

        @Volatile
        var activeSignalingManager: WebRtcSignalingManager? = null

        fun registerActiveCallHangup(onHangup: () -> Unit) {
            activeHangupCallback = onHangup
        }

        fun clearActiveCallHangup() {
            activeHangupCallback = null
        }

        fun rejectIncomingCallSilently(callId: String, callerId: String, isVideo: Boolean) {
            if (callId.isNotBlank() && callerId.isNotBlank()) {
                activeSignalingManager?.rejectCall(callId, callerId, isVideo)
            }
            if (callId.isNotBlank()) {
                FcmNotificationManager.notifyCallEnded(callId)
            }
        }

        fun start(context: Context, callId: String, peerName: String, isVideo: Boolean) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_PEER_NAME, peerName)
                putExtra(EXTRA_IS_VIDEO, isVideo)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        }

        fun stop(context: Context) {
            activeHangupCallback = null
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.stopService(intent)
            } catch (_: Exception) {
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_NOTIFICATION_HANGUP -> {
                val callId = intent.getStringExtra(EXTRA_CALL_ID).orEmpty()
                val cb = activeHangupCallback
                activeHangupCallback = null
                cb?.invoke()
                if (callId.isNotBlank()) {
                    NotificationHelper.cancelIncomingCallNotification(this, callId)
                    FcmNotificationManager.notifyCallEnded(callId)
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_STOP -> {
                activeHangupCallback = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val callId = intent?.getStringExtra(EXTRA_CALL_ID).orEmpty()
                val peerName = intent?.getStringExtra(EXTRA_PEER_NAME).orEmpty().ifBlank { "OOZOX" }
                val isVideo = intent?.getBooleanExtra(EXTRA_IS_VIDEO, false) == true
                ensureChannel()
                val notification = buildNotification(callId, peerName, isVideo)
                val fgsType = resolveForegroundType(isVideo)
                try {
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
                } catch (_: SecurityException) {
                    try {
                        startForeground(NOTIFICATION_ID, notification)
                    } catch (_: Exception) {
                        stopSelf()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun resolveForegroundType(isVideo: Boolean): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return 0
        val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasCam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        var mask = 0
        if (hasMic) mask = mask or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        if (isVideo && hasCam) mask = mask or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        return if (mask == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            mask
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_CALLS) == null) {
                val ch = NotificationChannel(
                    CHANNEL_CALLS,
                    "المكالمات الصوتية والمرئية",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "إشعار المكالمة الجارية في OOZOX"
                }
                manager.createNotificationChannel(ch)
            }
        }
    }

    private fun buildNotification(callId: String, peerName: String, isVideo: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endIntent = Intent(this, CallForegroundService::class.java).apply {
            action = ACTION_NOTIFICATION_HANGUP
            putExtra(EXTRA_CALL_ID, callId)
        }
        val endPending = PendingIntent.getService(
            this,
            1,
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_CALLS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isVideo) "مكالمة فيديو جارية • $peerName" else "مكالمة صوتية جارية • $peerName")
            .setContentText("اتصال مشفر P2P نشط في الخلفية")
            .setOngoing(true)
            .setContentIntent(contentPending)
            .addAction(0, "إنهاء", endPending)
            .build()
    }
}
