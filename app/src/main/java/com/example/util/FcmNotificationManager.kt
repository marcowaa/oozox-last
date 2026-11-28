package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object FcmNotificationManager {

    private const val TAG = "FcmNotificationManager"

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken = _fcmToken.asStateFlow()

    private val _isPushEnabled = MutableStateFlow(true)
    val isPushEnabled = _isPushEnabled.asStateFlow()

    private val _incomingTransferPushEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val incomingTransferPushEvents = _incomingTransferPushEvents.asSharedFlow()

    private val _sessionRevokedPushEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val sessionRevokedPushEvents = _sessionRevokedPushEvents.asSharedFlow()

    private val _incomingCallPushEvents = MutableSharedFlow<SignalingPayload>(extraBufferCapacity = 8)
    val incomingCallPushEvents = _incomingCallPushEvents.asSharedFlow()

    private val _callEndedPushEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val callEndedPushEvents = _callEndedPushEvents.asSharedFlow()

    private var isFirebaseReady = false

    fun initialize(context: Context) {
        NotificationHelper.createNotificationChannels(context)

        // Load cached token if previously issued by FCM
        val cachedToken = OozoxFirebaseMessagingService.getSavedToken(context)
        if (!cachedToken.isNullOrBlank() && !cachedToken.startsWith("oozox_device_")) {
            _fcmToken.value = cachedToken
        }

        // Initialize FirebaseApp using bundled google-services.json configuration only
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            isFirebaseReady = FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseApp unavailable (ensure google-services.json is configured): ${e.message}")
            isFirebaseReady = false
        }

        // Fetch or refresh genuine FCM registration token from Google Play Services
        if (isFirebaseReady) {
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(TAG, "FCM token unavailable: ${task.exception?.message}")
                        return@addOnCompleteListener
                    }

                    val token = task.result
                    Log.d(TAG, "FCM Registration Token fetched")
                    _fcmToken.value = token
                    OozoxFirebaseMessagingService.saveToken(context, token)
                    syncTokenWithServer(token, context)
                }

                subscribeToTopic("all_users")
                subscribeToTopic("projects_feed")
                subscribeToTopic("invoices_feed")
                subscribeToTopic("tasks_feed")
                subscribeToTopic("gold_alerts")
            } catch (e: Throwable) {
                Log.w(TAG, "FCM registration unavailable: ${e.message}")
            }
        } else {
            Log.i(TAG, "Firebase not configured in this build; push notifications require google-services.json.")
        }
    }

    fun syncTokenWithServer(token: String, context: Context? = null) {
        _fcmToken.value = token
        Log.d(TAG, "FCM Token synchronized locally")
        if (context != null && RetrofitClient.hasActiveSession()) {
            CoroutineScope(Dispatchers.IO).launch {
                com.example.data.repository.AuthRepository.registerFcmToken(context.applicationContext, token)
            }
        }
    }

    fun notifyIncomingTransferRequest(transferId: String) {
        _incomingTransferPushEvents.tryEmit(transferId)
    }

    fun notifySessionRevoked(reason: String) {
        _sessionRevokedPushEvents.tryEmit(reason)
    }

    fun notifyIncomingCall(payload: SignalingPayload) {
        _incomingCallPushEvents.tryEmit(payload)
    }

    fun notifyCallEnded(callId: String) {
        if (callId.isNotBlank()) {
            _callEndedPushEvents.tryEmit(callId)
        }
    }

    fun subscribeToTopic(topic: String) {
        if (!isFirebaseReady) return
        try {
            val sanitized = topic.replace("[^a-zA-Z0-9-_.~%]".toRegex(), "_")
            FirebaseMessaging.getInstance().subscribeToTopic(sanitized)
                .addOnSuccessListener {
                    Log.d(TAG, "Subscribed to FCM topic: $sanitized")
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "Failed subscribing to topic $sanitized: ${e.message}")
                }
        } catch (e: Throwable) {
            Log.d(TAG, "Topic subscription safely handled: ${e.message}")
        }
    }

    fun unsubscribeFromTopic(topic: String) {
        if (!isFirebaseReady) return
        try {
            val sanitized = topic.replace("[^a-zA-Z0-9-_.~%]".toRegex(), "_")
            FirebaseMessaging.getInstance().unsubscribeFromTopic(sanitized)
                .addOnSuccessListener {
                    Log.d(TAG, "Unsubscribed from FCM topic: $sanitized")
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "Failed unsubcribing from topic $sanitized: ${e.message}")
                }
        } catch (e: Throwable) {
            Log.d(TAG, "Topic unsubscription safely handled: ${e.message}")
        }
    }

    fun subscribeToProjectUpdates(projectId: String) {
        subscribeToTopic("project_$projectId")
    }

    fun subscribeToInvoiceUpdates(invoiceId: String) {
        subscribeToTopic("invoice_$invoiceId")
    }

    fun subscribeToChatUpdates(chatId: String) {
        subscribeToTopic("chat_$chatId")
    }

    /**
     * Sends local real-time notification when a new chat message arrives.
     */
    fun sendChatMessageNotification(
        context: Context,
        chatId: String,
        senderName: String,
        messageText: String,
        senderAvatarUri: String? = null,
        isSubAccount: Boolean = false,
        isSubAccountNotificationEnabled: Boolean = false
    ) {
        if (!_isPushEnabled.value) return
        NotificationHelper.showChatMessageNotification(
            context = context,
            chatId = chatId,
            senderName = senderName,
            messageText = messageText,
            senderAvatarUri = senderAvatarUri,
            isSubAccount = isSubAccount,
            isSubAccountNotificationEnabled = isSubAccountNotificationEnabled
        )
    }

    /**
     * Sends local real-time notification when a project is created, modified, or a task is assigned.
     */
    fun sendProjectTaskUpdateNotification(
        context: Context,
        projectId: String,
        projectName: String,
        taskTitle: String,
        updateMessage: String
    ) {
        if (!_isPushEnabled.value) return
        NotificationHelper.showProjectUpdateNotification(
            context = context,
            projectId = projectId,
            projectName = projectName,
            title = taskTitle,
            updateMessage = updateMessage
        )
    }

    fun sendProjectUpdateNotification(
        context: Context,
        projectId: String,
        projectName: String,
        title: String,
        updateMessage: String
    ) {
        sendProjectTaskUpdateNotification(context, projectId, projectName, title, updateMessage)
    }

    /**
     * Sends local real-time notification when an invoice status changes (PAID, OVERDUE, CANCELLED, etc.).
     */
    fun sendInvoiceStatusNotification(
        context: Context,
        invoiceId: String,
        invoiceNumber: String,
        status: String,
        totalAmount: Double,
        counterpartName: String? = null
    ) {
        if (!_isPushEnabled.value) return
        NotificationHelper.showInvoiceStatusNotification(
            context = context,
            invoiceId = invoiceId,
            invoiceNumber = invoiceNumber,
            status = status,
            totalAmount = totalAmount,
            counterpartName = counterpartName
        )
    }

    /**
     * Sends local real-time notification when a gold transaction/deal is placed or confirmed.
     */
    fun sendGoldDealNotification(
        context: Context,
        transactionId: String,
        title: String,
        message: String
    ) {
        if (!_isPushEnabled.value) return
        NotificationHelper.showGoldDealNotification(
            context = context,
            transactionId = transactionId,
            title = title,
            message = message
        )
    }

    /**
     * Sends local and FCM high priority urgent alert to the manager/admin
     */
    fun sendHighPriorityAlert(
        context: Context,
        title: String,
        message: String
    ) {
        if (!_isPushEnabled.value) return
        NotificationHelper.showGoldDealNotification(
            context = context,
            transactionId = "alert_${System.currentTimeMillis()}",
            title = title,
            message = message
        )
    }

    fun setPushNotificationsEnabled(enabled: Boolean) {
        _isPushEnabled.value = enabled
    }
}
