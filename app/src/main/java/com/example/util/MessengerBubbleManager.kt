package com.example.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.example.activity.BubbleMediaBridgeActivity
import com.example.service.FloatingBubbleService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Account types supported within the floating Messenger bubble.
 * Allows instant toggling between personal conversations and commercial gold trades.
 */
enum class BubbleAccountType(val labelAr: String, val icon: String = "") {
    PERSONAL("حساب شخصي", ""),
    BUSINESS("حساب العمل", "")
}

/**
 * Navigation tabs inside the expanded floating Messenger dialog.
 */
enum class BubbleActiveTab(val labelAr: String) {
    CHAT("المحادثة"),
    NOTIFICATIONS("الإشعارات"),
    ACCOUNT_INFO("ملف الشغل"),
    SETTINGS("الإعدادات")
}

/**
 * Data model representing an active floating chat bubble conversation.
 */
data class BubbleConversation(
    val chatId: String,
    val contactName: String,
    val contactAvatarUri: String? = null,
    val contactRole: String = "تاجر ذهب معتمد",
    val lastMessageText: String = "",
    val unreadCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true,
    val accountType: BubbleAccountType = BubbleAccountType.BUSINESS,
    val phoneNumber: String = "+20 100 123 4567",
    val cashBalance: String = "+45,000 ج.م",
    val goldBalance: String = "120.50 جم عيار 21",
    val lastSeenText: String = "متصل الآن"
)

/**
 * Notification model displayed inside the bubble's Notification Center.
 */
data class BubbleNotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val timeAgo: String,
    val type: String, // "DEAL", "INVOICE", "GOLD_RATE", "SYSTEM"
    val isRead: Boolean = false,
    val actionLabel: String = "معاينة"
)

/**
 * Professional business quick replies for rapid executive communication.
 */
object QuickBusinessReplies {
    val defaultReplies = listOf(
        "موافق",
        "تم الاستلام",
        "بانتظار التأكيد",
        "تم التحويل",
        "أرسل الفاتورة",
        "تم اعتماد الصفقة",
        "جاهز للتسليم",
        "شكراً لتعاملكم",
        "قيد المراجعة",
        "تم تأكيد السعر"
    )

    val personalReplies = listOf(
        "أهلاً بك",
        "تمام",
        "سأتواصل لاحقاً",
        "شكراً جزيلاً",
        "تم الوصول",
        "في أمان الله"
    )
}

/**
 * Comprehensive data state representing the floating chat heads & window.
 */
data class MessengerBubbleState(
    val isVisible: Boolean = false,
    val isExpanded: Boolean = false,
    val activeChatId: String = "",
    val conversations: List<BubbleConversation> = emptyList(),
    val accountFilter: BubbleAccountType = BubbleAccountType.BUSINESS,
    val activeTab: BubbleActiveTab = BubbleActiveTab.CHAT,
    val soundEnabled: Boolean = true,
    val autoExpandOnNewMessage: Boolean = true,
    val isDoNotDisturb: Boolean = false,
    val notifications: List<BubbleNotificationItem> = emptyList()
) {
    // Convenience accessor for the currently active conversation
    val activeConversation: BubbleConversation?
        get() = conversations.find { it.chatId == activeChatId } ?: conversations.firstOrNull()

    val chatId: String
        get() = activeConversation?.chatId ?: activeChatId

    val contactName: String
        get() = activeConversation?.contactName ?: ""

    val contactAvatarUri: String?
        get() = activeConversation?.contactAvatarUri

    val contactRole: String
        get() = activeConversation?.contactRole ?: "شريك تجاري"

    val lastMessageText: String
        get() = activeConversation?.lastMessageText ?: ""

    val unreadCount: Int
        get() = activeConversation?.unreadCount ?: 0

    val totalUnreadCount: Int
        get() = conversations.sumOf { it.unreadCount }

    val timestamp: Long
        get() = activeConversation?.timestamp ?: System.currentTimeMillis()

    val isOnline: Boolean
        get() = activeConversation?.isOnline ?: true

    val activeAccountType: BubbleAccountType
        get() = activeConversation?.accountType ?: accountFilter

    fun getFilteredConversations(): List<BubbleConversation> {
        val filtered = conversations.filter { it.accountType == accountFilter }
        return if (filtered.isNotEmpty()) filtered else conversations
    }
}

/**
 * Singleton manager to control floating chat heads (Facebook Messenger style).
 * Supports multi-account switching (Personal vs Business), in-bubble notification center,
 * full settings control, multi-bubble stacking, and edge docking outside & inside the app.
 */
object MessengerBubbleManager {
    private const val PREFS_NAME = "oozox_messenger_bubble_prefs"
    private const val KEY_BUBBLE_ENABLED = "is_messenger_bubble_enabled"
    private const val KEY_SOUND_ENABLED = "bubble_sound_enabled"
    private const val KEY_AUTO_EXPAND = "bubble_auto_expand_enabled"
    private const val KEY_ACCOUNT_FILTER = "bubble_account_filter"
    private const val KEY_DND_ENABLED = "bubble_dnd_enabled"

    private var appContext: Context? = null

    private val _bubbleState = MutableStateFlow(MessengerBubbleState())
    val bubbleState: StateFlow<MessengerBubbleState> = _bubbleState.asStateFlow()

    private val _isBubbleFeatureEnabled = MutableStateFlow(true)
    val isBubbleFeatureEnabled: StateFlow<Boolean> = _isBubbleFeatureEnabled.asStateFlow()

    // Tracks if true system overlay (outside the app) is running
    private val _isSystemOverlayActive = MutableStateFlow(false)
    val isSystemOverlayActive: StateFlow<Boolean> = _isSystemOverlayActive.asStateFlow()

    // Tracks if the main app activity is currently in the foreground
    private val _isAppInForeground = MutableStateFlow(true)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    // Triggers permission dialog if user needs to grant SYSTEM_ALERT_WINDOW
    private val _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog: StateFlow<Boolean> = _showPermissionDialog.asStateFlow()

    // Pending auto-launch flag when permission is granted
    private var pendingAutoLaunchOnPermission: Boolean = false

    // Pending navigation when user clicks "Open in full app" from floating bubble
    private val _pendingChatNavigation = MutableStateFlow<String?>(null)
    val pendingChatNavigation: StateFlow<String?> = _pendingChatNavigation.asStateFlow()

    data class BubbleMediaResult(
        val chatId: String,
        val uri: Uri,
        val isVideo: Boolean,
        val isDocument: Boolean = false,
        val fileName: String? = null,
        val fileSize: Long = 0L,
        val mimeType: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _pendingBubbleMedia = MutableStateFlow<BubbleMediaResult?>(null)
    val pendingBubbleMedia: StateFlow<BubbleMediaResult?> = _pendingBubbleMedia.asStateFlow()

    fun deliverBubbleMediaResult(
        chatId: String,
        uri: Uri,
        isVideo: Boolean,
        isDocument: Boolean = false,
        fileName: String? = null,
        fileSize: Long = 0L,
        mimeType: String? = null
    ) {
        _pendingBubbleMedia.value = BubbleMediaResult(
            chatId = chatId,
            uri = uri,
            isVideo = isVideo,
            isDocument = isDocument,
            fileName = fileName,
            fileSize = fileSize,
            mimeType = mimeType
        )
    }

    fun consumeBubbleMediaResult() {
        _pendingBubbleMedia.value = null
    }

    fun launchBubbleMediaBridge(context: Context, action: String, chatId: String) {
        try {
            val intent = Intent(context, BubbleMediaBridgeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra(BubbleMediaBridgeActivity.EXTRA_ACTION, action)
                putExtra(BubbleMediaBridgeActivity.EXTRA_CHAT_ID, chatId)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("MessengerBubbleManager", "Failed to launch BubbleMediaBridgeActivity", e)
        }
    }

    // Real notifications arrive dynamically during app usage

    private var lifecycleCallbacksRegistered = false
    private var startedActivityCount = 0

    fun initialize(context: Context) {
        val appCtx = context.applicationContext
        if (appContext != null && lifecycleCallbacksRegistered) return
        appContext = appCtx

        if (!lifecycleCallbacksRegistered && appCtx is Application) {
            lifecycleCallbacksRegistered = true
            appCtx.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {
                    if (activity is BubbleMediaBridgeActivity) return
                    startedActivityCount++
                    if (startedActivityCount > 0) {
                        setAppInForeground(activity.applicationContext, true)
                    }
                }
                override fun onActivityResumed(activity: Activity) {
                    if (activity is BubbleMediaBridgeActivity) return
                    if (startedActivityCount == 0) startedActivityCount = 1
                    setAppInForeground(activity.applicationContext, true)
                }
                override fun onActivityPaused(activity: Activity) {
                    if (activity is BubbleMediaBridgeActivity) return
                }
                override fun onActivityStopped(activity: Activity) {
                    if (activity is BubbleMediaBridgeActivity) return
                    if (!activity.isChangingConfigurations) {
                        startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
                        if (startedActivityCount == 0) {
                            setAppInForeground(activity.applicationContext, false)
                        }
                    }
                }
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            })
        }

        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_BUBBLE_ENABLED, true)
        val sound = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        val autoExpand = prefs.getBoolean(KEY_AUTO_EXPAND, true)
        val dndEnabled = prefs.getBoolean(KEY_DND_ENABLED, false)
        val filterName = prefs.getString(KEY_ACCOUNT_FILTER, BubbleAccountType.BUSINESS.name)
        val filter = try {
            BubbleAccountType.valueOf(filterName ?: BubbleAccountType.BUSINESS.name)
        } catch (_: Exception) {
            BubbleAccountType.BUSINESS
        }

        _isBubbleFeatureEnabled.value = enabled
        _bubbleState.value = _bubbleState.value.copy(
            soundEnabled = sound,
            autoExpandOnNewMessage = autoExpand,
            isDoNotDisturb = dndEnabled,
            accountFilter = filter
        )
    }

    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun setAppInForeground(context: Context, inForeground: Boolean) {
        _isAppInForeground.value = inForeground
        if (inForeground) {
            // App is open -> hide floating bubble immediately
            if (_isSystemOverlayActive.value) {
                stopFloatingService(context)
            }
        } else {
            // App is closed/backgrounded -> show floating bubble if permitted & enabled
            if (canDrawOverlays(context) && _isBubbleFeatureEnabled.value) {
                if (!_bubbleState.value.isVisible || _bubbleState.value.conversations.isEmpty()) {
                    ensureActiveBubble(context, autoExpand = false)
                }
                startFloatingService(context)
            }
        }
    }

    fun setSystemOverlayActive(active: Boolean) {
        _isSystemOverlayActive.value = active
    }

    fun setShowPermissionDialog(show: Boolean) {
        _showPermissionDialog.value = show
    }

    fun requestPermissionWithAutoLaunch(context: Context) {
        pendingAutoLaunchOnPermission = true
        if (canDrawOverlays(context)) {
            onOverlayPermissionGranted(context)
        } else {
            _showPermissionDialog.value = true
        }
    }

    /**
     * Called when overlay permission is confirmed or activity resumes after granting.
     * Prepares the bubble state; the system overlay will appear as soon as the app is backgrounded.
     */
    fun onOverlayPermissionGranted(context: Context) {
        setBubbleFeatureEnabled(context, true)
        _showPermissionDialog.value = false
        pendingAutoLaunchOnPermission = false
        ensureActiveBubble(context, autoExpand = false)
        if (!_isAppInForeground.value) {
            startFloatingService(context)
        }
    }

    fun navigateToChatInApp(chatId: String) {
        _pendingChatNavigation.value = chatId
    }

    fun clearPendingChatNavigation() {
        _pendingChatNavigation.value = null
    }

    fun startFloatingService(context: Context) {
        if (!canDrawOverlays(context) || !_isBubbleFeatureEnabled.value || _isAppInForeground.value) return
        try {
            val intent = Intent(context, FloatingBubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MessengerBubbleManager", "Error starting FloatingBubbleService", e)
        }
    }

    fun stopFloatingService(context: Context) {
        try {
            val intent = Intent(context, FloatingBubbleService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            android.util.Log.e("MessengerBubbleManager", "Error stopping FloatingBubbleService", e)
        }
        _isSystemOverlayActive.value = false
    }

    fun setBubbleFeatureEnabled(context: Context, enabled: Boolean) {
        _isBubbleFeatureEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BUBBLE_ENABLED, enabled).apply()
        if (!enabled) {
            dismissBubble()
        }
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        _bubbleState.value = _bubbleState.value.copy(soundEnabled = enabled)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun setAutoExpandOnNewMessage(context: Context, enabled: Boolean) {
        _bubbleState.value = _bubbleState.value.copy(autoExpandOnNewMessage = enabled)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_EXPAND, enabled).apply()
    }

    /**
     * Enables or disables Do Not Disturb (silent) mode for the floating bubble.
     * When enabled, the bubble is temporarily hidden from the home screen while keeping
     * background service processes alive so incoming messages continue to sync.
     */
    fun setDoNotDisturb(context: Context, enabled: Boolean) {
        _bubbleState.value = _bubbleState.value.copy(
            isDoNotDisturb = enabled,
            isExpanded = if (enabled) false else _bubbleState.value.isExpanded
        )
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DND_ENABLED, enabled).apply()
    }

    fun toggleDoNotDisturb(context: Context) {
        setDoNotDisturb(context, !_bubbleState.value.isDoNotDisturb)
    }

    fun setAccountFilter(context: Context, type: BubbleAccountType) {
        _bubbleState.value = _bubbleState.value.copy(accountFilter = type)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACCOUNT_FILTER, type.name).apply()

        // Select appropriate active conversation under this filter
        val matching = _bubbleState.value.conversations.firstOrNull { it.accountType == type }
        if (matching != null) {
            _bubbleState.value = _bubbleState.value.copy(activeChatId = matching.chatId)
        }
    }

    fun setActiveTab(tab: BubbleActiveTab) {
        _bubbleState.value = _bubbleState.value.copy(activeTab = tab)
    }

    fun markNotificationRead(id: String) {
        val updated = _bubbleState.value.notifications.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        _bubbleState.value = _bubbleState.value.copy(notifications = updated)
    }

    /**
     * Activates the floating bubble when real conversations exist.
     */
    fun ensureActiveBubble(context: Context, autoExpand: Boolean = true, preferredAccountType: BubbleAccountType? = null) {
        val current = _bubbleState.value
        val targetType = preferredAccountType ?: current.accountFilter

        if (current.conversations.isEmpty()) {
            _bubbleState.value = current.copy(
                isVisible = true,
                isExpanded = autoExpand,
                accountFilter = targetType
            )
        } else {
            val matching = current.conversations.firstOrNull { it.accountType == targetType }
                ?: current.conversations.first()
            _bubbleState.value = current.copy(
                isVisible = true,
                isExpanded = autoExpand,
                activeChatId = matching.chatId,
                accountFilter = targetType
            )
        }

        if (canDrawOverlays(context)) {
            startFloatingService(context)
        }
    }

    fun showBubble(
        chatId: String,
        contactName: String,
        contactAvatarUri: String? = null,
        contactRole: String = "تاجر ذهب معتمد",
        messageText: String = "",
        autoExpand: Boolean = false,
        isOnline: Boolean = true,
        accountType: BubbleAccountType = BubbleAccountType.BUSINESS
    ) {
        if (!_isBubbleFeatureEnabled.value || chatId.isBlank()) return

        val current = _bubbleState.value
        val existingIndex = current.conversations.indexOfFirst { it.chatId == chatId }

        val newConversation = if (existingIndex >= 0) {
            val old = current.conversations[existingIndex]
            val newUnread = if (current.activeChatId == chatId && current.isExpanded) 0 else old.unreadCount + 1
            old.copy(
                contactName = contactName.ifBlank { old.contactName },
                contactAvatarUri = contactAvatarUri ?: old.contactAvatarUri,
                contactRole = contactRole,
                lastMessageText = messageText.ifBlank { old.lastMessageText },
                unreadCount = if (autoExpand) 0 else newUnread,
                timestamp = System.currentTimeMillis(),
                isOnline = isOnline,
                accountType = accountType
            )
        } else {
            BubbleConversation(
                chatId = chatId,
                contactName = contactName.ifBlank { "محادثة" },
                contactAvatarUri = contactAvatarUri,
                contactRole = contactRole,
                lastMessageText = messageText,
                unreadCount = if (autoExpand) 0 else 1,
                timestamp = System.currentTimeMillis(),
                isOnline = isOnline,
                accountType = accountType
            )
        }

        // Place the active conversation at the front of the list
        val updatedConversations = mutableListOf<BubbleConversation>()
        updatedConversations.add(newConversation)
        current.conversations.forEach {
            if (it.chatId != chatId) {
                updatedConversations.add(it)
            }
        }

        // Limit to max 6 floating bubbles
        val cappedList = updatedConversations.take(6)

        val targetActiveId = if (autoExpand || current.activeChatId.isBlank() || current.conversations.none { it.chatId == current.activeChatId }) {
            chatId
        } else {
            current.activeChatId
        }

        val effectiveAutoExpand = autoExpand && !current.isDoNotDisturb

        _bubbleState.value = current.copy(
            isVisible = true,
            isExpanded = effectiveAutoExpand || (current.isExpanded && current.activeChatId == chatId && !current.isDoNotDisturb),
            activeChatId = targetActiveId,
            conversations = cappedList,
            accountFilter = accountType,
            notifications = current.notifications
        )

        // If system overlay permission is granted and app is backgrounded, launch outside-the-app floating service!
        val ctx = appContext
        if (ctx != null) {
            if (canDrawOverlays(ctx)) {
                if (!_isAppInForeground.value) {
                    startFloatingService(ctx)
                }
            } else if (_isAppInForeground.value) {
                _showPermissionDialog.value = true
            }
        }
    }

    fun switchActiveChat(chatId: String) {
        val current = _bubbleState.value
        val updatedConversations = current.conversations.map {
            if (it.chatId == chatId) it.copy(unreadCount = 0) else it
        }
        val targetConv = updatedConversations.find { it.chatId == chatId }
        _bubbleState.value = current.copy(
            activeChatId = chatId,
            isExpanded = true,
            conversations = updatedConversations,
            accountFilter = targetConv?.accountType ?: current.accountFilter,
            activeTab = BubbleActiveTab.CHAT
        )
    }

    fun removeBubble(chatId: String) {
        val current = _bubbleState.value
        val remaining = current.conversations.filter { it.chatId != chatId }
        if (remaining.isEmpty()) {
            dismissBubble()
        } else {
            val nextActiveId = if (current.activeChatId == chatId) remaining.first().chatId else current.activeChatId
            _bubbleState.value = current.copy(
                activeChatId = nextActiveId,
                conversations = remaining
            )
        }
    }

    fun expandBubble(chatId: String? = null) {
        val current = _bubbleState.value
        val targetId = chatId ?: current.activeChatId.ifBlank { current.conversations.firstOrNull()?.chatId ?: "" }
        val updatedConversations = current.conversations.map {
            if (it.chatId == targetId) it.copy(unreadCount = 0) else it
        }
        _bubbleState.value = current.copy(
            isExpanded = true,
            activeChatId = targetId,
            conversations = updatedConversations
        )
    }

    fun minimizeBubble() {
        _bubbleState.value = _bubbleState.value.copy(
            isExpanded = false
        )
    }

    fun toggleExpanded() {
        val current = _bubbleState.value
        if (current.isExpanded) {
            minimizeBubble()
        } else {
            expandBubble()
        }
    }

    fun dismissBubble() {
        _bubbleState.value = _bubbleState.value.copy(
            isVisible = false,
            isExpanded = false,
            activeChatId = "",
            conversations = emptyList()
        )
        val ctx = appContext
        if (ctx != null && _isSystemOverlayActive.value) {
            stopFloatingService(ctx)
        }
    }

    fun setOnlineStatus(chatId: String, isOnline: Boolean) {
        val current = _bubbleState.value
        val updated = current.conversations.map {
            if (it.chatId == chatId) it.copy(isOnline = isOnline) else it
        }
        _bubbleState.value = current.copy(conversations = updated)
    }

    fun toggleOnlineStatus(chatId: String? = null) {
        val current = _bubbleState.value
        val targetId = chatId ?: current.activeChatId.ifBlank { current.conversations.firstOrNull()?.chatId ?: "" }
        val updated = current.conversations.map {
            if (it.chatId == targetId) it.copy(isOnline = !it.isOnline) else it
        }
        _bubbleState.value = current.copy(conversations = updated)
    }
}
