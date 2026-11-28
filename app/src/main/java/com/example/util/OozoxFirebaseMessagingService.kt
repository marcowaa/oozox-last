package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class OozoxFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "OozoxFCM"
        const val PREFS_NAME = "oozox_fcm_prefs"
        const val KEY_FCM_TOKEN = "fcm_token"

        fun getSavedToken(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_FCM_TOKEN, null)
        }

        fun saveToken(context: Context, token: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received")
        saveToken(applicationContext, token)
        FcmNotificationManager.syncTokenWithServer(token, applicationContext)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val type = data["type"] ?: data["notification_type"] ?: data["event"] ?: "generic"

        when (type) {
            "chat_message", "chat" -> {
                val chatId = data["chat_id"] ?: data["chatId"] ?: "unknown"
                val senderName = data["sender_name"] ?: data["senderName"] ?: data["sender"] ?: notification?.title ?: "OOZOX"
                val messageText = data["message_text"] ?: data["messageText"] ?: data["message"] ?: data["text"] ?: data["content"] ?: data["body"] ?: notification?.body
                    ?: Translations.getFromContext(applicationContext, "fcm.new_message")
                val avatarUri = data["sender_avatar"] ?: data["avatar_url"] ?: data["avatarUri"]
                val isGroup = data["is_group"]?.toBooleanStrictOrNull() ?: (data["group_name"] != null || data["groupName"] != null)
                val groupName = data["group_name"] ?: data["groupName"]
                val isSubAccount = data["is_sub_account"]?.toBooleanStrictOrNull()
                    ?: (data["sub_account_id"] != null)
                    ?: (data["profile_id"] != null)
                    ?: false
                val isNotificationEnabled = (data["notifications_enabled"]?.toBooleanStrictOrNull() ?: true) &&
                    (data["silent_notifications"]?.toBooleanStrictOrNull() != true)

                NotificationHelper.showChatMessageNotification(
                    context = applicationContext,
                    chatId = chatId,
                    senderName = senderName,
                    messageText = messageText,
                    senderAvatarUri = avatarUri,
                    isSubAccount = isSubAccount,
                    isSubAccountNotificationEnabled = isNotificationEnabled,
                    isGroup = isGroup,
                    groupName = groupName
                )
            }

            // Real-time project & task notifications
            "project_update", "project", "project_task", "task_update", "new_task" -> {
                val projectId = data["project_id"] ?: data["projectId"] ?: ""
                val projectName = data["project_name"] ?: data["projectName"]
                    ?: Translations.getFromContext(applicationContext, "fcm.project_default")
                val taskTitle = data["task_title"] ?: data["taskTitle"] ?: data["title"] ?: notification?.title
                    ?: Translations.getFromContext(applicationContext, "fcm.new_task")
                val updateMessage = data["update_message"] ?: data["task_details"] ?: data["body"] ?: notification?.body
                    ?: Translations.getFromContext(applicationContext, "fcm.task_updated")
                NotificationHelper.showProjectUpdateNotification(
                    context = applicationContext,
                    projectId = projectId,
                    projectName = projectName,
                    title = taskTitle,
                    updateMessage = updateMessage
                )
            }

            // Real-time invoice status change notifications
            "invoice_status", "invoice_update", "invoice", "invoice_paid" -> {
                val invoiceId = data["invoice_id"] ?: data["invoiceId"] ?: data["id"] ?: ""
                val invoiceNumber = data["invoice_number"] ?: data["invoiceNumber"] ?: "#INV"
                val status = data["status"] ?: data["new_status"] ?: "PAID"
                val totalAmount = data["total_amount"]?.toDoubleOrNull()
                    ?: data["totalAmount"]?.toDoubleOrNull()
                    ?: data["amount"]?.toDoubleOrNull()
                    ?: 0.0
                val counterpart = data["counterpart_name"] ?: data["counterpart"] ?: data["user_name"]
                NotificationHelper.showInvoiceStatusNotification(
                    context = applicationContext,
                    invoiceId = invoiceId,
                    invoiceNumber = invoiceNumber,
                    status = status,
                    totalAmount = totalAmount,
                    counterpartName = counterpart
                )
            }

            "gold_deal", "deal", "transaction" -> {
                val transactionId = data["transaction_id"] ?: data["transactionId"] ?: "unknown"
                val title = data["title"] ?: notification?.title
                    ?: Translations.getFromContext(applicationContext, "fcm.gold_deal_title")
                val message = data["message"] ?: notification?.body
                    ?: Translations.getFromContext(applicationContext, "fcm.gold_deal_body")
                NotificationHelper.showGoldDealNotification(
                    context = applicationContext,
                    transactionId = transactionId,
                    title = title,
                    message = message
                )
            }

            "price_alert", "gold_alert" -> {
                val alertId = data["alert_id"] ?: "alert_${System.currentTimeMillis()}"
                val metalType = data["metal_type"] ?: "GOLD"
                val karat = data["karat"] ?: "21"
                val targetPrice = data["target_price"]?.toDoubleOrNull() ?: 0.0
                val currentPrice = data["current_price"]?.toDoubleOrNull() ?: 0.0
                val direction = data["direction"] ?: "ABOVE"
                val note = data["note"]
                NotificationHelper.showGoldPriceThresholdNotification(
                    context = applicationContext,
                    alertId = alertId,
                    metalType = metalType,
                    karat = karat,
                    targetPrice = targetPrice,
                    currentPrice = currentPrice,
                    direction = direction,
                    note = note
                )
            }

            "account_transfer_request", "ACCOUNT_TRANSFER_REQUEST" -> {
                val transferId = data["transferId"] ?: data["transfer_id"] ?: ""
                val deviceName = data["targetDeviceName"] ?: data["deviceName"] ?: "جهاز جديد"
                FcmNotificationManager.notifyIncomingTransferRequest(transferId)
                NotificationHelper.showSystemNotification(
                    context = applicationContext,
                    title = "طلب نقل حساب أوزوكس",
                    message = "يطلب الجهاز ($deviceName) تفويض نقل المحادثات والوسائط المشفرة."
                )
            }

            "session_revoked", "force_logout", "SESSION_REVOKED", "FORCE_LOGOUT" -> {
                val reason = data["reason"] ?: notification?.body ?: "تم إنهاء الجلسة بعد نقل الحساب إلى الهاتف الجديد."
                FcmNotificationManager.notifySessionRevoked(reason)
                NotificationHelper.showSystemNotification(
                    context = applicationContext,
                    title = "تم تسجيل الخروج من هذا الجهاز",
                    message = reason
                )
            }

            "incoming_call", "INCOMING_CALL" -> {
                val callId = data["callId"] ?: data["call_id"] ?: ""
                val callerId = data["callerId"] ?: data["caller_id"] ?: ""
                val callerName = data["callerName"] ?: data["caller_name"] ?: notification?.title ?: "مستخدم OOZOX"
                val isVid = data["callType"]?.equals("VIDEO", ignoreCase = true) == true
                if (callId.isNotBlank() && callerId.isNotBlank()) {
                    FcmNotificationManager.notifyIncomingCall(
                        SignalingPayload(
                            type = SignalingMessageType.CALL_OFFER,
                            callId = callId,
                            senderId = callerId,
                            senderName = callerName,
                            isVideo = isVid
                        )
                    )
                }
                NotificationHelper.showIncomingCallNotification(
                    context = applicationContext,
                    callId = callId.ifBlank { "fcm_${System.currentTimeMillis()}" },
                    callerId = callerId,
                    callerName = callerName,
                    isVideo = isVid
                )
            }

            "call_ended", "CALL_ENDED", "call_missed", "CALL_MISSED", "call_rejected", "CALL_REJECTED" -> {
                val callId = data["callId"] ?: data["call_id"] ?: ""
                if (callId.isNotBlank()) {
                    NotificationHelper.cancelIncomingCallNotification(applicationContext, callId)
                    com.example.activity.IncomingCallActivity.dismissIfActive(callId)
                    FcmNotificationManager.notifyCallEnded(callId)
                }
            }

            else -> {
                val title = notification?.title ?: data["title"]
                    ?: Translations.getFromContext(applicationContext, "fcm.system_title")
                val message = notification?.body ?: data["message"] ?: data["body"]
                    ?: Translations.getFromContext(applicationContext, "fcm.system_body")
                NotificationHelper.showSystemNotification(
                    context = applicationContext,
                    title = title,
                    message = message
                )
            }
        }
    }
}
