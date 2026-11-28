package com.example.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.CallForegroundService
import com.example.util.NotificationHelper

/**
 * Handles background notification action buttons (such as Declining an incoming call
 * directly from the heads-up/CallStyle notification without waking MainActivity).
 */
class IncomingCallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.getStringExtra(NotificationHelper.EXTRA_INCOMING_CALL_ACTION).orEmpty()
        val callId = intent?.getStringExtra(NotificationHelper.EXTRA_CALL_ID).orEmpty()
        val callerId = intent?.getStringExtra(NotificationHelper.EXTRA_CALLER_ID).orEmpty()
        val isVideo = intent?.getBooleanExtra(NotificationHelper.EXTRA_IS_VIDEO, false) == true

        if (action == NotificationHelper.CALL_ACTION_DECLINE) {
            NotificationHelper.cancelIncomingCallNotification(context, callId)
            IncomingCallActivity.dismissIfActive(callId)
            CallForegroundService.rejectIncomingCallSilently(callId, callerId, isVideo)
        }
    }
}
