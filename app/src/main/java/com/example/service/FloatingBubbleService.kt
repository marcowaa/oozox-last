package com.example.service

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.activity.BubbleMediaBridgeActivity
import com.example.data.model.HashtagEntryDirection
import com.example.data.model.HashtagEntryEntity
import com.example.data.model.HashtagEntryType
import com.example.data.model.InvoiceEntity
import com.example.data.model.MessageEntity
import com.example.data.model.MessageType
import com.example.data.model.QuickBusinessReplies
import com.example.data.model.TransactionEntity
import com.example.data.repository.GoldChatRepository
import com.example.ui.AppThemeStorage
import com.example.ui.components.CallState
import com.example.ui.components.CallType
import com.example.ui.components.CoverPhotoEditorDialog
import com.example.ui.components.ImageFrameAspectMode
import com.example.ui.components.InlineChatVideoPlayer
import com.example.ui.components.MediaChooserKind
import com.example.ui.components.MediaPreviewItem
import com.example.ui.components.MediaPreviewModal
import com.example.ui.components.MediaSourceChooserCard
import com.example.ui.components.OozoxThumbnailImage
import com.example.ui.components.VideoEditorDialog
import com.example.ui.screens.DocumentMessageItem
import com.example.ui.screens.VoiceMessagePlayer
import com.example.ui.theme.*
import com.example.util.AudioPlayerManager
import com.example.util.AudioRecorderManager
import com.example.util.BubbleConversation
import com.example.util.DocumentInfo
import com.example.util.FileUtils
import com.example.util.HashtagUtils
import com.example.util.MessengerBubbleManager
import com.example.util.MessengerBubbleState
import com.example.util.ShortCodeGenerator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.hypot

/**
 * Android Foreground Service delivering true Facebook Messenger style
 * Floating Chat Heads (Bubbles) over the home screen and other applications.
 */
class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var repository: GoldChatRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var bubbleContainer: FrameLayout? = null
    private var bubbleComposeView: ComposeView? = null
    private var bubbleLifecycleOwner: FloatingLifecycleOwner? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    private var dismissContainer: FrameLayout? = null
    private var dismissComposeView: ComposeView? = null
    private var dismissLifecycleOwner: FloatingLifecycleOwner? = null
    private var dismissParams: WindowManager.LayoutParams? = null
    private var isDismissAttached = false

    private var expandedContainer: FrameLayout? = null
    private var expandedComposeView: ComposeView? = null
    private var expandedLifecycleOwner: FloatingLifecycleOwner? = null
    private var expandedParams: WindowManager.LayoutParams? = null
    private var isExpandedAttached = false

    // Drag and positioning state
    private var isOverDismissZone = mutableStateOf(false)
    private var isDockedOnRight = mutableStateOf(true)
    private var isMessagePreviewVisible = mutableStateOf(false)
    private var isIdleTucked = mutableStateOf(false)
    private var currentBubblePositionX = 0
    private var currentBubblePositionY = 300
    private var activeSnapAnimator: ValueAnimator? = null

    private val overlayType: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (MessengerBubbleManager.isAppInForeground.value) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_OPEN_BUBBLE -> {
                MessengerBubbleManager.setDoNotDisturb(this, false)
                MessengerBubbleManager.expandBubble()
            }
            ACTION_TOGGLE_DND -> {
                MessengerBubbleManager.toggleDoNotDisturb(this)
            }
            ACTION_RESUME_FROM_DND -> {
                MessengerBubbleManager.setDoNotDisturb(this, false)
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        if (MessengerBubbleManager.isAppInForeground.value) {
            stopSelf()
            return
        }
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            repository = GoldChatRepository(applicationContext)

            createNotificationChannel()
            startForegroundWithNotification()

            MessengerBubbleManager.setSystemOverlayActive(true)

            val metrics = resources.displayMetrics
            val bubbleSizePx = (68 * metrics.density).toInt()
            currentBubblePositionX = metrics.widthPixels - bubbleSizePx
            currentBubblePositionY = (metrics.heightPixels * 0.35f).toInt()

            setupBubbleView()
            setupDismissView()
            setupExpandedView()

            observeBubbleState()
            schedulePreviewAutoHide()
        } catch (e: Exception) {
            android.util.Log.e("FloatingBubbleService", "Failed to initialize FloatingBubbleService", e)
            MessengerBubbleManager.setSystemOverlayActive(false)
            stopSelf()
        }
    }

    private var previewHideJob: Job? = null
    private var idleDockJob: Job? = null
    private var idleAnimator: ValueAnimator? = null
    private var idleTuckAnimator: ValueAnimator? = null
    private var isIdleDocked = false

    private fun schedulePreviewAutoHide() {
        previewHideJob?.cancel()
        previewHideJob = serviceScope.launch {
            delay(3800)
            if (isMessagePreviewVisible.value) {
                isMessagePreviewVisible.value = false
                updateBubbleWindowSize(false)
            }
            scheduleIdleDock()
        }
    }

    private fun scheduleIdleDock() {
        idleDockJob?.cancel()
        idleDockJob = serviceScope.launch {
            // Stage 1: Become noticeably translucent when idle
            delay(2600)
            if (!isMessagePreviewVisible.value && !isExpandedAttached && !isIdleDocked) {
                animateBubbleAlpha(targetAlpha = 0.42f, durationMs = 450)
                isIdleDocked = true
            }
            // Stage 2: After longer idle, tuck half the bubble off-edge flush with screen edge
            delay(3400)
            if (!isMessagePreviewVisible.value && !isExpandedAttached && isIdleDocked) {
                isIdleTucked.value = true
                animateBubbleHalfTuck(durationMs = 420)
            }
        }
    }

    private fun animateBubbleAlpha(targetAlpha: Float, durationMs: Long) {
        val params = bubbleParams ?: return
        val startAlpha = params.alpha
        idleAnimator?.cancel()
        idleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                val f = va.animatedFraction
                params.alpha = (startAlpha + (targetAlpha - startAlpha) * f).coerceIn(0.35f, 1f)
                if (bubbleContainer?.isAttachedToWindow == true) {
                    try {
                        windowManager.updateViewLayout(bubbleContainer, params)
                    } catch (_: Exception) {}
                }
            }
        }
        idleAnimator?.start()
    }

    private fun animateBubbleHalfTuck(durationMs: Long) {
        val params = bubbleParams ?: return
        val metrics = resources.displayMetrics
        val bubbleSizePx = (68 * metrics.density).toInt()
        val startX = params.x
        val targetX = if (isDockedOnRight.value) {
            metrics.widthPixels - (bubbleSizePx / 2)
        } else {
            -(bubbleSizePx / 2)
        }
        idleTuckAnimator?.cancel()
        idleTuckAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                val f = va.animatedFraction
                params.x = (startX + (targetX - startX) * f).toInt()
                if (bubbleContainer?.isAttachedToWindow == true) {
                    try {
                        windowManager.updateViewLayout(bubbleContainer, params)
                    } catch (_: Exception) {}
                }
            }
        }
        idleTuckAnimator?.start()
    }

    private fun wakeFromIdle() {
        idleDockJob?.cancel()
        idleAnimator?.cancel()
        idleTuckAnimator?.cancel()
        val wasTucked = isIdleTucked.value
        isIdleTucked.value = false
        val params = bubbleParams ?: return
        val metrics = resources.displayMetrics
        val bubbleSizePx = (68 * metrics.density).toInt()
        if (isIdleDocked || wasTucked || params.alpha < 0.99f) {
            isIdleDocked = false
            params.alpha = 1.0f
            if (!isMessagePreviewVisible.value) {
                params.x = if (isDockedOnRight.value) metrics.widthPixels - bubbleSizePx else 0
            }
            if (bubbleContainer?.isAttachedToWindow == true) {
                try {
                    windowManager.updateViewLayout(bubbleContainer, params)
                } catch (_: Exception) {}
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "فقاعات الدردشة العائمة",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "استمرار عمل فقاعة الدردشة العائمة فوق التطبيقات والشاشة الرئيسية"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildServiceNotification(isDnd: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openBubbleIntent = Intent(this, FloatingBubbleService::class.java).apply {
            action = if (isDnd) ACTION_RESUME_FROM_DND else ACTION_OPEN_BUBBLE
        }
        val openBubblePendingIntent = PendingIntent.getService(
            this,
            1,
            openBubbleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleDndIntent = Intent(this, FloatingBubbleService::class.java).apply {
            action = ACTION_TOGGLE_DND
        }
        val toggleDndPendingIntent = PendingIntent.getService(
            this,
            2,
            toggleDndIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isDnd) "فقاعة Oozox (وضع عدم الإزعاج)" else "فقاعة دردشة Oozox العائمة")
            .setContentText(
                if (isDnd) "الفقاعة مخفية مؤقتاً والخدمة تعمل في الخلفية"
                else "المحادثة نشطة وتطفو فوق جميع التطبيقات والشاشة الرئيسية"
            )
            .setColor(0xFFD4AF37.toInt())
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_send,
                if (isDnd) "إظهار الفقاعة" else "فتح الفقاعة",
                openBubblePendingIntent
            )
            .addAction(
                android.R.drawable.ic_lock_silent_mode,
                if (isDnd) "إلغاء الصامت" else "عدم الإزعاج",
                toggleDndPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateServiceNotification(isDnd: Boolean) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, buildServiceNotification(isDnd))
        } catch (_: Exception) {}
    }

    private fun startForegroundWithNotification() {
        val notification = buildServiceNotification(MessengerBubbleManager.bubbleState.value.isDoNotDisturb)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingBubbleService", "Foreground start failed with type, retrying basic", e)
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (fallbackEx: Exception) {
                android.util.Log.e("FloatingBubbleService", "Fallback startForeground failed", fallbackEx)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBubbleView() {
        val density = resources.displayMetrics.density
        val metrics = resources.displayMetrics
        val bubbleSizePx = (68 * density).toInt()

        bubbleLifecycleOwner = FloatingLifecycleOwner().apply {
            onCreate()
            onStart()
        }

        val initialWidth = bubbleSizePx
        val initialHeight = bubbleSizePx
        val initialX = (metrics.widthPixels - bubbleSizePx).coerceAtLeast(0)

        bubbleParams = WindowManager.LayoutParams(
            initialWidth,
            initialHeight,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = currentBubblePositionY
        }

        bubbleComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(bubbleLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(bubbleLifecycleOwner)
            setViewTreeViewModelStoreOwner(bubbleLifecycleOwner)

            setContent {
                BubbleAppTheme {
                    val bubbleState by MessengerBubbleManager.bubbleState.collectAsState()
                    val isDocked by remember { isDockedOnRight }
                    val showPreview by remember { isMessagePreviewVisible }
                    val isTucked by remember { isIdleTucked }

                    FloatingBubbleHeadContent(
                        bubbleState = bubbleState,
                        repository = repository,
                        isDockedOnRight = isDocked,
                        isMessagePreviewVisible = showPreview,
                        isIdleTucked = isTucked,
                        onTogglePreview = { visible ->
                            if (visible) wakeFromIdle()
                            isMessagePreviewVisible.value = visible
                            updateBubbleWindowSize(visible)
                            if (visible) schedulePreviewAutoHide() else scheduleIdleDock()
                        },
                        onQuickReplyClick = {
                            wakeFromIdle()
                            MessengerBubbleManager.expandBubble()
                        }
                    )
                }
            }
        }

        bubbleContainer = DraggableBubbleFrameLayout(this, bubbleSizePx).apply {
            setViewTreeLifecycleOwner(bubbleLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(bubbleLifecycleOwner)
            setViewTreeViewModelStoreOwner(bubbleLifecycleOwner)
            addView(bubbleComposeView)
        }

        try {
            windowManager.addView(bubbleContainer, bubbleParams)
        } catch (e: Exception) {
            android.util.Log.e("FloatingBubbleService", "Error adding bubble view", e)
        }
    }

    private fun updateBubbleWindowSize(showPreview: Boolean) {
        val params = bubbleParams ?: return
        val metrics = resources.displayMetrics
        val density = metrics.density
        val bubbleSizePx = (68 * density).toInt()

        if (showPreview) {
            val previewWidth = (310 * density).toInt().coerceAtMost((metrics.widthPixels * 0.90f).toInt())
            params.width = previewWidth
            params.height = (96 * density).toInt()
            if (isDockedOnRight.value) {
                params.x = (metrics.widthPixels - previewWidth).coerceAtLeast(0)
            } else {
                params.x = 0
            }
        } else {
            params.width = bubbleSizePx
            params.height = bubbleSizePx
            if (isDockedOnRight.value) {
                params.x = metrics.widthPixels - bubbleSizePx
            } else {
                params.x = 0
            }
        }

        if (bubbleContainer?.isAttachedToWindow == true) {
            try {
                windowManager.updateViewLayout(bubbleContainer, params)
            } catch (e: Exception) {
                android.util.Log.e("FloatingBubbleService", "Error updating bubble layout", e)
            }
        }
    }

    /**
     * Custom FrameLayout that intercepts drag and long-press gestures cleanly above
     * ComposeView children while letting clean single taps on sub-buttons (e.g. preview close) pass through
     * and expanding cleanly on single tap of the bubble head.
     */
    @SuppressLint("ViewConstructor")
    private inner class DraggableBubbleFrameLayout(
        context: Context,
        private val bubbleSizePx: Int
    ) : FrameLayout(context) {
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var initialX = 0
        private var initialY = 0
        private var touchStartTime = 0L
        private var isDragging = false
        private var isLongPressDismissArmed = false
        private var velocityTracker: VelocityTracker? = null
        private val longPressHandler = android.os.Handler(android.os.Looper.getMainLooper())
        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

        private val longPressRunnable = Runnable {
            isLongPressDismissArmed = true
            previewHideJob?.cancel()
            if (isMessagePreviewVisible.value) {
                isMessagePreviewVisible.value = false
                updateBubbleWindowSize(false)
            }
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showDismissTarget()
        }

        override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
            val params = bubbleParams ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    activeSnapAnimator?.cancel()
                    wakeFromIdle()
                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain().apply { addMovement(event) }
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialX = params.x
                    initialY = params.y
                    touchStartTime = System.currentTimeMillis()
                    isDragging = false
                    isLongPressDismissArmed = false
                    longPressHandler.removeCallbacks(longPressRunnable)
                    longPressHandler.postDelayed(
                        longPressRunnable,
                        ViewConfiguration.getLongPressTimeout().toLong()
                    )
                    return false
                }
                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addMovement(event)
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (isLongPressDismissArmed || hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                        isDragging = true
                        longPressHandler.removeCallbacks(longPressRunnable)
                        previewHideJob?.cancel()
                        if (isMessagePreviewVisible.value) {
                            isMessagePreviewVisible.value = false
                            updateBubbleWindowSize(false)
                            initialX = params.x
                            initialTouchX = event.rawX
                        }
                        showDismissTarget()
                        return true
                    }
                    return false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressHandler.removeCallbacks(longPressRunnable)
                    if (isLongPressDismissArmed && !isDragging) {
                        isLongPressDismissArmed = false
                        hideDismissTarget()
                        scheduleIdleDock()
                        return true
                    }
                    return false
                }
            }
            return false
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val params = bubbleParams ?: return false
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addMovement(event)
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (!isDragging && (isLongPressDismissArmed || hypot(dx.toDouble(), dy.toDouble()) > touchSlop)) {
                        isDragging = true
                        longPressHandler.removeCallbacks(longPressRunnable)
                        previewHideJob?.cancel()
                        if (isMessagePreviewVisible.value) {
                            isMessagePreviewVisible.value = false
                            updateBubbleWindowSize(false)
                            initialX = params.x
                            initialTouchX = event.rawX
                        }
                        showDismissTarget()
                    }

                    if (isDragging) {
                        var nextX = (initialX + dx).toInt().coerceIn(-bubbleSizePx / 3, screenWidth - (bubbleSizePx * 2) / 3)
                        var nextY = (initialY + dy).toInt().coerceIn(0, screenHeight - bubbleSizePx)

                        val dismissTargetSizePx = (80 * metrics.density).toInt()
                        val dismissCenterY = screenHeight - (64 * metrics.density) - (dismissTargetSizePx / 2f)
                        val dismissCenterX = screenWidth / 2f
                        val bubbleCenterX = nextX + bubbleSizePx / 2f
                        val bubbleCenterY = nextY + bubbleSizePx / 2f

                        val dist = hypot((bubbleCenterX - dismissCenterX).toDouble(), (bubbleCenterY - dismissCenterY).toDouble())
                        val isNearBottom = nextY > screenHeight - (180 * metrics.density)
                        val overZone = dist < (135 * metrics.density) || isNearBottom

                        if (overZone) {
                            val targetSnapX = (dismissCenterX - bubbleSizePx / 2f).toInt()
                            val targetSnapY = (dismissCenterY - bubbleSizePx / 2f).toInt()
                            nextX = (nextX * 0.35f + targetSnapX * 0.65f).toInt()
                            nextY = (nextY * 0.35f + targetSnapY * 0.65f).toInt()
                        } else if (!isLongPressDismissArmed) {
                            val edgeZonePx = (18 * metrics.density).toInt()
                            val rightEdgeX = screenWidth - bubbleSizePx
                            val leftEdgeX = 0
                            if (nextX <= leftEdgeX + edgeZonePx) {
                                nextX = (nextX * 0.72f + leftEdgeX * 0.28f).toInt()
                            } else if (nextX >= rightEdgeX - edgeZonePx) {
                                nextX = (nextX * 0.72f + rightEdgeX * 0.28f).toInt()
                            }
                        }

                        params.x = nextX
                        params.y = nextY

                        if (overZone != isOverDismissZone.value) {
                            isOverDismissZone.value = overZone
                            if (overZone) {
                                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                        }

                        val dragDownProgress = ((params.y - (screenHeight * 0.35f)) / (screenHeight * 0.45f)).coerceIn(0f, 1f)
                        params.alpha = if (overZone) 0.38f else (1.0f - (dragDownProgress * 0.55f)).coerceIn(0.45f, 1.0f)

                        if (isAttachedToWindow) {
                            try {
                                windowManager.updateViewLayout(this, params)
                            } catch (_: Exception) {}
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressHandler.removeCallbacks(longPressRunnable)
                    velocityTracker?.addMovement(event)
                    velocityTracker?.computeCurrentVelocity(1000, 4500f)
                    val vx = velocityTracker?.xVelocity ?: 0f
                    val vy = velocityTracker?.yVelocity ?: 0f
                    velocityTracker?.recycle()
                    velocityTracker = null

                    val isNearBottom = params.y > screenHeight - (180 * metrics.density)
                    val isFastDownwardFling = vy > 850f && params.y > screenHeight * 0.65f
                    if (isDragging && (isOverDismissZone.value || isNearBottom || isFastDownwardFling)) {
                        isDragging = false
                        isOverDismissZone.value = false
                        isLongPressDismissArmed = false
                        val dismissTargetSizePx = (80 * metrics.density).toInt()
                        val dismissCenterY = screenHeight - (64 * metrics.density) - (dismissTargetSizePx / 2f)
                        val dismissCenterX = screenWidth / 2f

                        animateDismissBubble(
                            startX = params.x,
                            startY = params.y,
                            targetX = (dismissCenterX - bubbleSizePx / 2f).toInt(),
                            targetY = (dismissCenterY - bubbleSizePx / 2f).toInt()
                        ) {
                            hideDismissTarget()
                            MessengerBubbleManager.dismissBubble()
                            stopSelf()
                        }
                        return true
                    }

                    hideDismissTarget()

                    if (isDragging) {
                        isDragging = false
                        isLongPressDismissArmed = false
                        val isRight = when {
                            vx > 550f -> true
                            vx < -550f -> false
                            else -> (params.x + bubbleSizePx / 2 >= screenWidth / 2)
                        }
                        isDockedOnRight.value = isRight
                        val snapTargetX = if (isRight) screenWidth - bubbleSizePx else 0
                        val minY = (20 * metrics.density).toInt()
                        val maxY = (screenHeight - bubbleSizePx - (20 * metrics.density).toInt()).coerceAtLeast(minY)
                        val inertiaTargetY = (params.y + vy * 0.16f).toInt().coerceIn(minY, maxY)

                        animateBubbleSnap(
                            startX = params.x,
                            startY = params.y,
                            targetX = snapTargetX,
                            targetY = inertiaTargetY
                        ) {
                            scheduleIdleDock()
                        }
                        return true
                    } else {
                        val wasLongPress = isLongPressDismissArmed
                        isLongPressDismissArmed = false
                        val duration = System.currentTimeMillis() - touchStartTime
                        if (!wasLongPress && duration < 360) {
                            currentBubblePositionY = params.y
                            isDockedOnRight.value = (params.x + bubbleSizePx / 2 >= screenWidth / 2)
                            MessengerBubbleManager.expandBubble()
                            return true
                        }
                    }
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }

    private fun animateBubbleSnap(
        startX: Int,
        startY: Int,
        targetX: Int,
        targetY: Int,
        onEnd: () -> Unit = {}
    ) {
        val params = bubbleParams ?: return
        val startAlpha = params.alpha
        val xInterpolator = OvershootInterpolator(1.05f)
        val yInterpolator = DecelerateInterpolator(1.8f)

        activeSnapAnimator?.cancel()
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 280
            addUpdateListener { va ->
                val rawFraction = va.animatedFraction
                val xFraction = xInterpolator.getInterpolation(rawFraction)
                val yFraction = yInterpolator.getInterpolation(rawFraction)
                params.x = (startX + (targetX - startX) * xFraction).toInt()
                params.y = (startY + (targetY - startY) * yFraction).toInt()
                // Smoothly restore opacity to 1.0f
                params.alpha = (startAlpha + (1.0f - startAlpha) * rawFraction).coerceIn(0f, 1f)
                if (bubbleContainer?.isAttachedToWindow == true) {
                    try {
                        windowManager.updateViewLayout(bubbleContainer, params)
                    } catch (_: Exception) {}
                }
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    params.alpha = 1.0f
                    onEnd()
                }
            })
        }
        activeSnapAnimator = animator
        animator.start()
        currentBubblePositionX = targetX
        currentBubblePositionY = targetY
    }

    private fun animateDismissBubble(
        startX: Int,
        startY: Int,
        targetX: Int,
        targetY: Int,
        onEnd: () -> Unit = {}
    ) {
        val params = bubbleParams ?: run {
            onEnd()
            return
        }
        val startAlpha = params.alpha
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260
            interpolator = AccelerateInterpolator()
            addUpdateListener { va ->
                val fraction = va.animatedFraction
                params.x = (startX + (targetX - startX) * fraction).toInt()
                params.y = (startY + (targetY - startY) * fraction).toInt()
                params.alpha = (startAlpha * (1f - fraction)).coerceAtLeast(0f)
                if (bubbleContainer?.isAttachedToWindow == true) {
                    try {
                        windowManager.updateViewLayout(bubbleContainer, params)
                    } catch (_: Exception) {}
                }
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd()
                }
            })
        }
        animator.start()
    }

    private fun setupDismissView() {
        val density = resources.displayMetrics.density
        val targetSizePx = (80 * density).toInt()

        dismissLifecycleOwner = FloatingLifecycleOwner().apply {
            onCreate()
            onStart()
        }

        dismissParams = WindowManager.LayoutParams(
            targetSizePx,
            targetSizePx,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (64 * density).toInt()
        }

        dismissComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(dismissLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(dismissLifecycleOwner)
            setViewTreeViewModelStoreOwner(dismissLifecycleOwner)

            setContent {
                BubbleAppTheme {
                    val isHovered by remember { isOverDismissZone }
                    DismissTargetContent(isHovered = isHovered)
                }
            }
        }

        dismissContainer = FrameLayout(this).apply {
            setViewTreeLifecycleOwner(dismissLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(dismissLifecycleOwner)
            setViewTreeViewModelStoreOwner(dismissLifecycleOwner)
            addView(dismissComposeView)
        }
    }

    private fun showDismissTarget() {
        val container = dismissContainer ?: return
        val params = dismissParams ?: return
        if (!isDismissAttached) {
            try {
                container.animate().cancel()
                container.alpha = 0f
                container.translationY = 48f * resources.displayMetrics.density
                windowManager.addView(container, params)
                isDismissAttached = true
                container.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180L)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .start()
            } catch (e: Exception) {
                android.util.Log.e("FloatingBubbleService", "Error showing dismiss target", e)
            }
        }
    }

    private fun hideDismissTarget() {
        val container = dismissContainer
        if (isDismissAttached && container != null) {
            isDismissAttached = false
            container.animate().cancel()
            container.animate()
                .alpha(0f)
                .translationY(40f * resources.displayMetrics.density)
                .setDuration(130L)
                .setInterpolator(AccelerateInterpolator(1.3f))
                .withEndAction {
                    try {
                        if (container.isAttachedToWindow) {
                            windowManager.removeView(container)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingBubbleService", "Error hiding dismiss target", e)
                    }
                }
                .start()
        }
        isOverDismissZone.value = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupExpandedView() {
        expandedLifecycleOwner = FloatingLifecycleOwner().apply {
            onCreate()
            onStart()
        }

        expandedParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        expandedComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(expandedLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(expandedLifecycleOwner)
            setViewTreeViewModelStoreOwner(expandedLifecycleOwner)

            setContent {
                BubbleAppTheme {
                    val bubbleState by MessengerBubbleManager.bubbleState.collectAsState()
                    val isRightSide by remember { isDockedOnRight }
                    FloatingExpandedChatWindowContent(
                        bubbleState = bubbleState,
                        repository = repository,
                        isAnchoredRight = isRightSide,
                        onMinimize = { MessengerBubbleManager.minimizeBubble() },
                        onCloseChat = { chatId -> MessengerBubbleManager.removeBubble(chatId) },
                        onSwitchChat = { chatId -> MessengerBubbleManager.switchActiveChat(chatId) },
                        onOpenInFullApp = { chatId ->
                            MessengerBubbleManager.minimizeBubble()
                            val intent = Intent(this@FloatingBubbleService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("OPEN_CHAT_ID", chatId)
                                putExtra(com.example.util.NotificationHelper.EXTRA_CHAT_ID, chatId)
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }

        expandedContainer = FrameLayout(this).apply {
            isFocusableInTouchMode = true
            setViewTreeLifecycleOwner(expandedLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(expandedLifecycleOwner)
            setViewTreeViewModelStoreOwner(expandedLifecycleOwner)
            addView(expandedComposeView)
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    MessengerBubbleManager.minimizeBubble()
                    true
                } else {
                    false
                }
            }
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    MessengerBubbleManager.minimizeBubble()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun showExpandedWindow() {
        val container = expandedContainer ?: return
        val params = expandedParams ?: return
        if (!isExpandedAttached) {
            try {
                val metrics = resources.displayMetrics
                val isRight = isDockedOnRight.value

                params.gravity = Gravity.TOP or Gravity.START
                params.width = WindowManager.LayoutParams.MATCH_PARENT
                params.height = WindowManager.LayoutParams.MATCH_PARENT
                params.x = 0
                params.y = 0

                container.animate().cancel()
                container.pivotX = if (isRight) metrics.widthPixels.toFloat() else 0f
                container.pivotY = 0f
                container.alpha = 0f
                container.scaleX = 0.94f
                container.scaleY = 0.94f
                container.translationX = 0f
                container.translationY = -24f * metrics.density

                windowManager.addView(container, params)
                isExpandedAttached = true
                container.requestFocus()

                bubbleContainer?.let { bubble ->
                    bubble.animate().cancel()
                    bubble.animate()
                        .alpha(0f)
                        .scaleX(0.70f)
                        .scaleY(0.70f)
                        .setDuration(120L)
                        .withEndAction { bubble.visibility = View.GONE }
                        .start()
                }

                container.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setDuration(240L)
                    .setInterpolator(DecelerateInterpolator(1.6f))
                    .start()
            } catch (e: Exception) {
                android.util.Log.e("FloatingBubbleService", "Error showing expanded chat window", e)
            }
        }
    }

    private fun hideExpandedWindow(immediate: Boolean = false) {
        val container = expandedContainer
        val params = expandedParams
        if (isExpandedAttached && container != null) {
            if (immediate) {
                try {
                    container.animate().cancel()
                    windowManager.removeView(container)
                } catch (e: Exception) {
                    android.util.Log.e("FloatingBubbleService", "Error hiding expanded chat window", e)
                } finally {
                    isExpandedAttached = false
                }
                restoreBubbleAfterCollapse()
            } else {
                isExpandedAttached = false
                val metrics = resources.displayMetrics
                val isRight = isDockedOnRight.value
                if (params != null) {
                    val pivotXFraction = if (isRight) 1.0f else 0.0f
                    val pivotYFraction = ((currentBubblePositionY - params.y).toFloat() / params.height.coerceAtLeast(1)).coerceIn(0.10f, 0.90f)
                    container.pivotX = params.width * pivotXFraction
                    container.pivotY = params.height * pivotYFraction
                }
                container.animate().cancel()
                container.animate()
                    .alpha(0f)
                    .scaleX(0.84f)
                    .scaleY(0.84f)
                    .translationX(if (isRight) 64f * metrics.density else -64f * metrics.density)
                    .setDuration(160L)
                    .setInterpolator(AccelerateInterpolator(1.3f))
                    .withEndAction {
                        try {
                            if (container.isAttachedToWindow) {
                                windowManager.removeView(container)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FloatingBubbleService", "Error removing expanded chat window", e)
                        }
                        restoreBubbleAfterCollapse()
                    }
                    .start()
            }
        } else {
            restoreBubbleAfterCollapse()
        }
    }

    private fun restoreBubbleAfterCollapse() {
        bubbleContainer?.let { bubble ->
            bubble.animate().cancel()
            bubble.visibility = View.VISIBLE
            bubble.alpha = 0f
            bubble.scaleX = 0.72f
            bubble.scaleY = 0.72f
            bubble.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220L)
                .setInterpolator(OvershootInterpolator(1.25f))
                .start()
        }
        wakeFromIdle()
        scheduleIdleDock()
    }

    private fun observeBubbleState() {
        serviceScope.launch {
            MessengerBubbleManager.bubbleState.collectLatest { state ->
                if (!state.isVisible || state.conversations.isEmpty()) {
                    // No active conversations, stop service
                    stopSelf()
                    return@collectLatest
                }

                updateServiceNotification(state.isDoNotDisturb)

                if (state.isDoNotDisturb) {
                    // Temporarily hide bubble from home screen while keeping background service alive
                    idleDockJob?.cancel()
                    previewHideJob?.cancel()
                    hideDismissTarget()
                    if (isExpandedAttached && expandedContainer != null) {
                        try {
                            windowManager.removeView(expandedContainer)
                            isExpandedAttached = false
                        } catch (_: Exception) {}
                    }
                    bubbleContainer?.visibility = View.GONE
                    return@collectLatest
                }

                if (state.isExpanded) {
                    showExpandedWindow()
                } else {
                    hideExpandedWindow()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MessengerBubbleManager.setSystemOverlayActive(false)
        serviceScope.cancel()

        hideDismissTarget()
        hideExpandedWindow(immediate = true)

        if (bubbleContainer != null && bubbleContainer?.isAttachedToWindow == true) {
            try {
                windowManager.removeView(bubbleContainer)
            } catch (_: Exception) {}
        }

        bubbleLifecycleOwner?.onDestroy()
        dismissLifecycleOwner?.onDestroy()
        expandedLifecycleOwner?.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "oozox_floating_bubbles_channel"
        const val NOTIFICATION_ID = 9021
        const val ACTION_OPEN_BUBBLE = "com.example.action.OPEN_BUBBLE"
        const val ACTION_TOGGLE_DND = "com.example.action.TOGGLE_DND"
        const val ACTION_RESUME_FROM_DND = "com.example.action.RESUME_FROM_DND"
    }
}

/**
 * Live-synced theme wrapper for floating overlay windows so the bubble always
 * matches the user's selected in-app palette, dark/light mode, and font scale.
 */
@Composable
private fun BubbleAppTheme(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences(AppThemeStorage.PREFS_NAME, Context.MODE_PRIVATE)
    }

    var isDark by remember {
        mutableStateOf(prefs.getBoolean(AppThemeStorage.KEY_IS_DARK_THEME, true))
    }
    var palette by remember {
        mutableStateOf(
            AppThemePalette.fromId(prefs.getString(AppThemeStorage.KEY_THEME_PALETTE, "OOZOX_INFINITY"))
        )
    }
    var fontScale by remember {
        mutableStateOf(
            AppFontScale.fromName(prefs.getString(AppThemeStorage.KEY_FONT_SCALE, "STANDARD"))
        )
    }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            when (key) {
                AppThemeStorage.KEY_IS_DARK_THEME ->
                    isDark = sp.getBoolean(AppThemeStorage.KEY_IS_DARK_THEME, true)
                AppThemeStorage.KEY_THEME_PALETTE ->
                    palette = AppThemePalette.fromId(sp.getString(AppThemeStorage.KEY_THEME_PALETTE, "OOZOX_INFINITY"))
                AppThemeStorage.KEY_FONT_SCALE ->
                    fontScale = AppFontScale.fromName(sp.getString(AppThemeStorage.KEY_FONT_SCALE, "STANDARD"))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    GoldChatTheme(
        darkTheme = isDark,
        palette = palette,
        fontScale = fontScale,
        content = content
    )
}

/**
 * Minimized Floating Bubble Head connected to Room Database
 * Displays the latest messages from Room Database inside a floating preview cloud
 * and triggers instant quick reply popup window.
 */
@Composable
private fun FloatingBubbleHeadContent(
    bubbleState: MessengerBubbleState,
    repository: GoldChatRepository,
    isDockedOnRight: Boolean,
    isMessagePreviewVisible: Boolean,
    isIdleTucked: Boolean,
    onTogglePreview: (Boolean) -> Unit,
    onQuickReplyClick: () -> Unit
) {
    val messages by repository.getRecentMessagesForChat(bubbleState.activeChatId, limit = 1).collectAsState(initial = emptyList())
    val currentUser by repository.currentUser.collectAsState(initial = null)
    val latestMessage = remember(messages) { messages.lastOrNull() }

    val allChats by repository.allChats.collectAsState(initial = emptyList())
    val currentChat = remember(allChats, bubbleState.activeChatId) {
        allChats.find { it.id == bubbleState.activeChatId }
    }
    val unreadCount = currentChat?.unreadCount ?: bubbleState.totalUnreadCount

    LaunchedEffect(latestMessage?.id) {
        if (latestMessage != null && latestMessage.senderId != (currentUser?.id ?: "me")) {
            onTogglePreview(true)
        }
    }

    val entryScale = remember { Animatable(0.25f) }
    val entryAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            entryScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch {
            entryAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220)
            )
        }
    }

    val showCard = isMessagePreviewVisible && latestMessage != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (showCard) 92.dp else 68.dp)
            .graphicsLayer {
                scaleX = entryScale.value
                scaleY = entryScale.value
                alpha = entryAlpha.value
            }
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isDockedOnRight) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDockedOnRight) {
            AnimatedVisibility(
                visible = showCard,
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.90f, animationSpec = tween(200)),
                exit = fadeOut(tween(130)) + scaleOut(targetScale = 0.90f, animationSpec = tween(130))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (latestMessage != null) {
                        FloatingLatestMessagePreviewCard(
                            contactName = bubbleState.contactName,
                            latestMessage = latestMessage,
                            isMe = latestMessage.senderId == (currentUser?.id ?: "me"),
                            isOnline = bubbleState.isOnline,
                            onQuickReply = onQuickReplyClick,
                            onDismiss = { onTogglePreview(false) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }
            FloatingCircularBubbleHead(
                bubbleState = bubbleState,
                unreadCount = unreadCount,
                isOnline = bubbleState.isOnline,
                onClick = onQuickReplyClick
            )
        } else {
            FloatingCircularBubbleHead(
                bubbleState = bubbleState,
                unreadCount = unreadCount,
                isOnline = bubbleState.isOnline,
                onClick = onQuickReplyClick
            )
            AnimatedVisibility(
                visible = showCard,
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.90f, animationSpec = tween(200)),
                exit = fadeOut(tween(130)) + scaleOut(targetScale = 0.90f, animationSpec = tween(130))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (latestMessage != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        FloatingLatestMessagePreviewCard(
                            contactName = bubbleState.contactName,
                            latestMessage = latestMessage,
                            isMe = latestMessage.senderId == (currentUser?.id ?: "me"),
                            isOnline = bubbleState.isOnline,
                            onQuickReply = onQuickReplyClick,
                            onDismiss = { onTogglePreview(false) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingLatestMessagePreviewCard(
    contactName: String,
    latestMessage: MessageEntity,
    isMe: Boolean,
    isOnline: Boolean = true,
    onQuickReply: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(latestMessage.timestamp) { timeFormat.format(Date(latestMessage.timestamp)) }

    Surface(
        modifier = Modifier
            .widthIn(min = 172.dp, max = 228.dp)
            .heightIn(min = 58.dp, max = 78.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(18.dp))
            .clickable(onClick = onQuickReply),
        shape = RoundedCornerShape(18.dp),
        color = colors.surface.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) Color(0xFF10B981) else colors.onSurfaceVariant.copy(alpha = 0.45f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = contactName,
                        color = colors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = formattedTime,
                        color = colors.onSurfaceVariant,
                        fontSize = 9.5.sp
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant.copy(alpha = 0.75f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إخفاء",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            val messageSnippet = when (latestMessage.type) {
                MessageType.VOICE.name -> "رسالة صوتية"
                MessageType.IMAGE.name -> "صورة"
                MessageType.DOCUMENT.name -> "ملف مرفق"
                else -> {
                    val prefix = if (isMe) "أنت: " else ""
                    prefix + latestMessage.content
                }
            }
            Text(
                text = messageSnippet,
                color = colors.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.5.sp
            )
        }
    }
}

/**
 * Refined Messenger-style Circular Floating Chat Head synced with OOZOX branding.
 */
@Composable
private fun FloatingCircularBubbleHead(
    bubbleState: MessengerBubbleState,
    unreadCount: Int,
    isOnline: Boolean = bubbleState.isOnline,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val hasUnread = unreadCount > 0

    Box(
        modifier = Modifier
            .size(66.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bubbleState.conversations.size > 1) {
            val secondary = bubbleState.conversations.getOrNull(1)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = (-3).dp, y = (-3).dp)
                    .scale(0.86f)
                    .rotate(-7f)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
                    .border(1.dp, GoldPrimary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = secondary?.contactName?.trim()?.take(1)?.ifBlank { "O" } ?: "O",
                    color = colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Main concentric Messenger chat head with OOZOX gold ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = if (hasUnread) 12.dp else 8.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            GoldPrimary,
                            GoldAccent,
                            colors.primary
                        )
                    )
                )
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                colors.surfaceVariant,
                                colors.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val initial = bubbleState.contactName.trim().take(1).ifBlank { "O" }
                Text(
                    text = initial,
                    color = colors.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Online Status Dot
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-1).dp, y = (-1).dp)
                .size(13.dp)
                .clip(CircleShape)
                .background(if (isOnline) Color(0xFF10B981) else Color(0xFF6B7280))
                .border(2.dp, colors.surface, CircleShape)
        )

        // Crisp Unread Pill Badge
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .defaultMinSize(minWidth = 19.dp, minHeight = 19.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
                    .border(1.5.dp, colors.surface, CircleShape)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Bottom Drag Dismiss Zone (Sleek Translucent Target Circle)
 */
@Composable
private fun DismissTargetContent(isHovered: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.22f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dismiss_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(if (isHovered) 66.dp else 54.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(if (isHovered) Color(0xFFEF4444) else Color(0xD9111827))
                .border(
                    width = 1.8.dp,
                    color = if (isHovered) Color.White else Color(0xFFEF4444).copy(alpha = 0.85f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "إغلاق الفقاعة",
                tint = Color.White,
                modifier = Modifier.size(if (isHovered) 30.dp else 22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isHovered) "أفلت للإغلاق" else "اسحب هنا",
            color = if (isHovered) Color(0xFFEF4444) else Color.White,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Expanded Floating Chat Window (Top-anchored full-width Messenger + OOZOX Chat parity)
 * - Tapping any empty area around the heads or below the chat card closes/minimizes cleanly.
 * - Full feature parity: Voice/Video calls, Photos & Videos (Camera/Gallery only + Editors),
 *   Audio recording + Voice draft playback + Mic/Camera toggle, Interactive #Hashtags & 8-char Codes,
 *   Replies, and Reactions — rendered completely inside overlay-safe Boxes (never Dialogs).
 */
@Composable
private fun FloatingExpandedChatWindowContent(
    bubbleState: MessengerBubbleState,
    repository: GoldChatRepository,
    isAnchoredRight: Boolean = true,
    onMinimize: () -> Unit,
    onCloseChat: (String) -> Unit,
    onSwitchChat: (String) -> Unit,
    onOpenInFullApp: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val activeChatId = bubbleState.activeChatId
    val activeConv = bubbleState.activeConversation

    val messages by repository.getRecentMessagesForChat(activeChatId, limit = 40).collectAsState(initial = emptyList())
    val currentUser by repository.currentUser.collectAsState(initial = null)
    val allHashtags by repository.allHashtagEntries.collectAsState(initial = emptyList())
    val allInvoices by repository.allInvoices.collectAsState(initial = emptyList())
    val allTransactions by repository.allTransactions.collectAsState(initial = emptyList())
    val pendingBubbleMedia by MessengerBubbleManager.pendingBubbleMedia.collectAsState()

    var messageInputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Reply & Quick action state
    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var actionTargetMessage by remember { mutableStateOf<MessageEntity?>(null) }

    // Attachment & Media states (2-option Camera / Gallery + Photo & Video editors)
    var showAttachmentPanel by remember { mutableStateOf(false) }
    var activeMediaChooserKind by remember { mutableStateOf<MediaChooserKind?>(null) }
    var pendingPhotoUriForEditor by remember { mutableStateOf<Uri?>(null) }
    var pendingVideoUriForEditor by remember { mutableStateOf<Uri?>(null) }
    var attachedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var attachedMediaIsVideo by remember { mutableStateOf(false) }
    var attachedDocumentInfo by remember { mutableStateOf<DocumentInfo?>(null) }
    var activeMediaPreview by remember { mutableStateOf<MediaPreviewItem?>(null) }

    // Quick #Hashtag Composer overlay
    var showHashtagComposer by remember { mutableStateOf(false) }
    var hashtagTagInput by remember { mutableStateOf("") }
    var hashtagAmountInput by remember { mutableStateOf("") }
    var hashtagDirection by remember { mutableStateOf(HashtagEntryDirection.DEBIT) }

    // Financial Inspector overlay (clickable #hashtag or 8-char code)
    var inspectedFinancialToken by remember { mutableStateOf<String?>(null) }

    // Inline Audio Recorder & Draft Player + Mic/Camera Toggle
    var isActionCameraMode by remember { mutableStateOf(false) }
    var isInlineRecordingAudio by remember { mutableStateOf(false) }
    var inlineRecordSeconds by remember { mutableIntStateOf(0) }
    var inlineRecordAmplitudes by remember { mutableStateOf<List<Float>>(emptyList()) }
    var isInlineRecordCancelZone by remember { mutableStateOf(false) }
    var voiceDraftPath by remember { mutableStateOf<String?>(null) }
    var voiceDraftDurationSec by remember { mutableIntStateOf(0) }
    var isDraftAudioPlaying by remember { mutableStateOf(false) }
    var draftAudioProgress by remember { mutableFloatStateOf(0f) }

    // Active In-Bubble Call Overlay state
    var activeCallType by remember { mutableStateOf<CallType?>(null) }
    var callState by remember { mutableStateOf(CallState.OUTGOING_RINGING) }
    var callSeconds by remember { mutableIntStateOf(0) }
    var isCallMuted by remember { mutableStateOf(false) }
    var isCallSpeaker by remember { mutableStateOf(false) }
    var isCallVideoOn by remember { mutableStateOf(true) }

    LaunchedEffect(activeChatId, currentUser?.id) {
        currentUser?.id?.let { uid ->
            repository.markChatAsRead(activeChatId, uid)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Consume media picked from BubbleMediaBridgeActivity
    LaunchedEffect(pendingBubbleMedia) {
        val result = pendingBubbleMedia ?: return@LaunchedEffect
        MessengerBubbleManager.consumeBubbleMediaResult()
        if (result.isDocument) {
            attachedDocumentInfo = DocumentInfo(
                uri = result.uri,
                fileName = result.fileName ?: "مستند_مرفق",
                fileSize = result.fileSize,
                mimeType = result.mimeType ?: "application/octet-stream",
                isImage = false
            )
            attachedMediaUri = null
        } else if (result.isVideo) {
            pendingVideoUriForEditor = result.uri
        } else {
            pendingPhotoUriForEditor = result.uri
        }
    }

    LaunchedEffect(isInlineRecordingAudio) {
        if (isInlineRecordingAudio) {
            inlineRecordAmplitudes = emptyList()
            val startAt = System.currentTimeMillis()
            while (isInlineRecordingAudio) {
                inlineRecordSeconds = ((System.currentTimeMillis() - startAt) / 1000L).toInt()
                val amp = AudioRecorderManager.getMaxAmplitude()
                val norm = (amp / 18000f).coerceIn(0.18f, 1f)
                inlineRecordAmplitudes = (inlineRecordAmplitudes + norm).takeLast(22)
                delay(120L)
            }
        }
    }

    LaunchedEffect(activeCallType, callState) {
        val type = activeCallType ?: return@LaunchedEffect
        if (callState == CallState.OUTGOING_RINGING) {
            delay(1800L)
            callState = CallState.CONNECTING
            delay(1000L)
            callState = CallState.CONNECTED
        } else if (callState == CallState.CONNECTED) {
            while (activeCallType != null && callState == CallState.CONNECTED) {
                delay(1000L)
                callSeconds++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AudioRecorderManager.cancelRecording()
            AudioPlayerManager.stop()
        }
    }

    val colors = MaterialTheme.colorScheme
    val currentUid = currentUser?.id ?: "me"

    fun dispatchTextOrReply(rawText: String, forcedType: String? = null) {
        val cleanText = rawText.trim()
        if (cleanText.isBlank()) return
        val reply = replyingToMessage
        val finalContent = if (reply != null) {
            val repSnippet = when (reply.type) {
                MessageType.VOICE.name -> "رسالة صوتية"
                MessageType.IMAGE.name -> if (FileUtils.isVideoUriString(reply.content.substringBefore("||"))) "فيديو" else "صورة"
                MessageType.DOCUMENT.name -> "مستند"
                else -> reply.content.substringAfter("\n\n", reply.content).take(60)
            }
            "↩️ رد على ${reply.senderName}:\n\"$repSnippet\"\n\n$cleanText"
        } else {
            cleanText
        }
        val detectedType = forcedType ?: if (HashtagUtils.parseHashtag(cleanText) != null) {
            MessageType.HASHTAG.name
        } else {
            MessageType.TEXT.name
        }
        coroutineScope.launch {
            val user = currentUser
            val senderId = user?.id ?: "me"
            val senderName = user?.name ?: "المستخدم"
            repository.sendMessage(
                chatId = activeChatId,
                senderId = senderId,
                senderName = senderName,
                content = finalContent,
                type = detectedType
            )
            repository.autoProcessHashtagFromMessage(
                chatId = activeChatId,
                content = cleanText,
                senderId = senderId,
                senderName = senderName
            )
        }
        replyingToMessage = null
    }

    fun startInBubbleCall(type: CallType) {
        val needed = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            .any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (needed) {
            MessengerBubbleManager.launchBubbleMediaBridge(
                context = context,
                action = BubbleMediaBridgeActivity.ACTION_REQUEST_CALL_PERMS,
                chatId = activeChatId
            )
        }
        isCallMuted = false
        isCallSpeaker = (type == CallType.VIDEO)
        isCallVideoOn = (type == CallType.VIDEO)
        callSeconds = 0
        callState = CallState.OUTGOING_RINGING
        activeCallType = type
    }

    fun endInBubbleCall() {
        val type = activeCallType ?: return
        val dur = callSeconds
        activeCallType = null
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.isSpeakerphoneOn = false
        coroutineScope.launch {
            val user = currentUser
            val senderId = user?.id ?: "me"
            val senderName = user?.name ?: "المستخدم"
            val formattedDur = String.format(Locale.US, "%02d:%02d", dur / 60, dur % 60)
            if (dur > 0) {
                repository.sendMessage(
                    chatId = activeChatId,
                    senderId = senderId,
                    senderName = senderName,
                    content = "انتهت ${type.label} ($formattedDur)",
                    type = MessageType.CALL.name
                )
            } else {
                repository.sendMessage(
                    chatId = activeChatId,
                    senderId = senderId,
                    senderName = senderName,
                    content = "مكالمة فائتة (${type.label})",
                    type = MessageType.MISSED_CALL.name
                )
            }
        }
    }

    // Fullscreen overlay root: tapping ANY empty space outside the bubble window minimizes the bubble!
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
            .pointerInput(Unit) {
                detectTapGestures { onMinimize() }
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("floating_expanded_root_backdrop")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 4.dp, start = 4.dp, end = 4.dp)
        ) {
            // 1. Messenger Chat Heads Strip flush at top, aligned to bubble dock side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = if (isAnchoredRight) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bubbleState.conversations.forEach { conv ->
                    val isSelected = conv.chatId == activeChatId
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(50.dp)
                            .pointerInput(conv.chatId) {
                                detectTapGestures { onSwitchChat(conv.chatId) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 46.dp else 40.dp)
                                .shadow(elevation = if (isSelected) 8.dp else 2.dp, shape = CircleShape)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.primaryContainer else colors.surface)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) GoldPrimary else colors.outlineVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conv.contactName.trim().take(1).ifBlank { "O" },
                                color = if (isSelected) GoldPrimary else colors.onSurfaceVariant,
                                fontSize = if (isSelected) 16.sp else 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .border(1.dp, colors.outlineVariant, CircleShape)
                                    .clickable { onCloseChat(conv.chatId) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق محادثة ${conv.contactName}",
                                    tint = colors.onSurface,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }

                        if (conv.unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                                    .border(1.2.dp, colors.surface, CircleShape)
                                    .padding(horizontal = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (conv.unreadCount > 9) "9+" else conv.unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-2).dp, y = (-2).dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (conv.isOnline) Color(0xFF10B981) else Color(0xFF6B7280))
                                .border(1.5.dp, colors.surface, CircleShape)
                        )
                    }
                }
            }

            // 2. Full-width Chat Card anchored cleanly at top with breathing space below for empty-tap dismissal
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.86f)
                    .shadow(elevation = 18.dp, shape = RoundedCornerShape(22.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { /* Consume taps inside card so empty backdrop tap doesn't trigger */ }
                    }
                    .testTag("floating_expanded_chat_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.99f)),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header matching ChatDetailScreen order & controls
                    val isConvOnline = activeConv?.isOnline ?: true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .background(colors.surfaceVariant.copy(alpha = 0.42f))
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onOpenInFullApp(activeChatId) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.primaryContainer)
                                    .border(1.2.dp, GoldPrimary.copy(alpha = 0.65f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeConv?.contactName?.take(1)?.ifBlank { "O" } ?: "O",
                                    color = colors.primary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isConvOnline) Color(0xFF10B981) else Color(0xFF9CA3AF))
                                    .border(1.5.dp, colors.surface, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenInFullApp(activeChatId) }
                        ) {
                            Text(
                                text = activeConv?.contactName ?: "محادثة",
                                color = colors.onSurface,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isConvOnline) "متصل الآن" else "غير متصل",
                                color = if (isConvOnline) Color(0xFF10B981) else colors.onSurfaceVariant,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Video Call Button
                        IconButton(
                            onClick = { startInBubbleCall(CallType.VIDEO) },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("bubble_video_call_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "مكالمة فيديو",
                                tint = adaptiveGoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Audio Call Button
                        IconButton(
                            onClick = { startInBubbleCall(CallType.AUDIO) },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("bubble_audio_call_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "مكالمة صوتية",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        IconButton(
                            onClick = { MessengerBubbleManager.setDoNotDisturb(context, true) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = "وضع عدم الإزعاج",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { onOpenInFullApp(activeChatId) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "فتح في التطبيق",
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onMinimize,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "تصغير كفقاعة",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)

                    // Message Thread
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(colors.background)
                    ) {
                        if (messages.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = GoldPrimary.copy(alpha = 0.55f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "ابدأ المحادثة السريعة",
                                    color = colors.onSurfaceVariant,
                                    fontSize = 12.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                items(messages.size, key = { idx -> messages[idx].id }) { idx ->
                                    val msg = messages[idx]
                                    val isMe = msg.senderId == currentUid
                                    val prevSame = idx > 0 && (messages[idx - 1].senderId == currentUid) == isMe
                                    val nextSame = idx < messages.lastIndex && (messages[idx + 1].senderId == currentUid) == isMe

                                    Box(
                                        modifier = Modifier.padding(
                                            top = if (prevSame) 2.dp else 6.dp,
                                            bottom = if (nextSame) 0.dp else 1.dp
                                        )
                                    ) {
                                        FloatingMessageBubble(
                                            msg = msg,
                                            isMe = isMe,
                                            isFirstInGroup = !prevSame,
                                            isLastInGroup = !nextSame,
                                            isActionMenuOpen = actionTargetMessage?.id == msg.id,
                                            onOpenActionMenu = {
                                                actionTargetMessage = if (actionTargetMessage?.id == msg.id) null else msg
                                            },
                                            onDismissActionMenu = { actionTargetMessage = null },
                                            onReactionClick = { reaction ->
                                                actionTargetMessage = null
                                                coroutineScope.launch {
                                                    repository.updateMessageReaction(msg.id, reaction)
                                                }
                                            },
                                            onReplyClick = {
                                                actionTargetMessage = null
                                                replyingToMessage = msg
                                            },
                                            onMediaClick = { previewItem -> activeMediaPreview = previewItem },
                                            onCodeOrHashtagClick = { token ->
                                                inspectedFinancialToken = token
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.35f), thickness = 0.8.dp)

                    // Reply Preview Banner
                    AnimatedVisibility(visible = replyingToMessage != null) {
                        val rep = replyingToMessage
                        if (rep != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceVariant.copy(alpha = 0.55f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(26.dp)
                                            .background(GoldPrimary, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "رد على ${rep.senderName}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary
                                        )
                                        Text(
                                            text = when (rep.type) {
                                                MessageType.VOICE.name -> "رسالة صوتية"
                                                MessageType.IMAGE.name -> if (FileUtils.isVideoUriString(rep.content.substringBefore("||"))) "فيديو" else "صورة"
                                                MessageType.DOCUMENT.name -> "مستند"
                                                else -> rep.content.substringAfter("\n\n", rep.content)
                                            },
                                            fontSize = 11.sp,
                                            color = colors.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { replyingToMessage = null },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "إلغاء الرد", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Pending Attached Photo/Video Preview Strip
                    AnimatedVisibility(visible = attachedMediaUri != null) {
                        val uri = attachedMediaUri
                        if (uri != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OozoxThumbnailImage(
                                    model = uri,
                                    width = 48.dp,
                                    height = 48.dp,
                                    shape = RoundedCornerShape(10.dp),
                                    contentScale = ContentScale.Fit,
                                    contentDescription = "معاينة المرفق",
                                    onClick = {
                                        activeMediaPreview = MediaPreviewItem(
                                            uri = uri.toString(),
                                            isVideo = attachedMediaIsVideo
                                        )
                                    }
                                )
                                Text(
                                    text = if (attachedMediaIsVideo) "فيديو جاهز للإرسال" else "صورة جاهزة للإرسال",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (attachedMediaIsVideo) {
                                            pendingVideoUriForEditor = uri
                                        } else {
                                            pendingPhotoUriForEditor = uri
                                        }
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = "تعديل", tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { attachedMediaUri = null },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف المرفق", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Pending Document Strip
                    AnimatedVisibility(visible = attachedDocumentInfo != null) {
                        val doc = attachedDocumentInfo
                        if (doc != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.fileName, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(FileUtils.formatFileSize(doc.fileSize), fontSize = 10.sp, color = colors.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = { attachedDocumentInfo = null },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "حذف الملف", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Compact + Attachment Bar (Photo, Video, Financial #, File)
                    AnimatedVisibility(visible = showAttachmentPanel) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            BubbleAttachOptionTile(
                                icon = Icons.Default.Photo,
                                label = "صورة",
                                tint = Color(0xFF10B981),
                                onClick = {
                                    showAttachmentPanel = false
                                    activeMediaChooserKind = MediaChooserKind.PHOTO
                                }
                            )
                            BubbleAttachOptionTile(
                                icon = Icons.Default.Videocam,
                                label = "فيديو",
                                tint = GoldPrimary,
                                onClick = {
                                    showAttachmentPanel = false
                                    activeMediaChooserKind = MediaChooserKind.VIDEO
                                }
                            )
                            BubbleAttachOptionTile(
                                icon = Icons.Default.Tag,
                                label = "هاشتاج مالي",
                                tint = Color(0xFF3B82F6),
                                onClick = {
                                    showAttachmentPanel = false
                                    showHashtagComposer = true
                                }
                            )
                            BubbleAttachOptionTile(
                                icon = Icons.Default.AttachFile,
                                label = "ملف",
                                tint = colors.primary,
                                onClick = {
                                    showAttachmentPanel = false
                                    MessengerBubbleManager.launchBubbleMediaBridge(
                                        context = context,
                                        action = BubbleMediaBridgeActivity.ACTION_DOCUMENT,
                                        chatId = activeChatId
                                    )
                                }
                            )
                        }
                    }

                    // Quick #Hashtags + Replies Bar
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickPills = listOf("#فاتورة ", "#سداد ", "#توريد ") + QuickBusinessReplies.defaultReplies
                        items(quickPills) { chip ->
                            val isTagPrefix = chip.startsWith("#")
                            Surface(
                                onClick = {
                                    if (isTagPrefix) {
                                        messageInputText = chip + messageInputText.trimStart()
                                    } else {
                                        dispatchTextOrReply(chip, MessageType.TEXT.name)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isTagPrefix) GoldPrimary.copy(alpha = 0.14f) else colors.surfaceVariant.copy(alpha = 0.65f),
                                border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = if (isTagPrefix) 0.55f else 0.25f))
                            ) {
                                Text(
                                    text = chip.trim(),
                                    color = if (isTagPrefix) GoldPrimary else colors.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = if (isTagPrefix) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Composer Bar matching ChatDetailScreen (+ Attachment | Center Recorder/VoiceDraft/Text | Send or Mic<->Camera)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { showAttachmentPanel = !showAttachmentPanel },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (showAttachmentPanel) GoldPrimary.copy(alpha = 0.18f)
                                    else colors.surfaceVariant.copy(alpha = 0.55f)
                                )
                                .testTag("floating_bubble_attach_btn")
                        ) {
                            Icon(
                                imageVector = if (showAttachmentPanel) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "إرفاق",
                                tint = if (showAttachmentPanel) GoldPrimary else colors.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        when {
                            isInlineRecordingAudio -> {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = if (isInlineRecordCancelZone) Color(0xFFEF4444).copy(alpha = 0.14f) else colors.surfaceVariant.copy(alpha = 0.7f),
                                    border = BorderStroke(1.dp, if (isInlineRecordCancelZone) Color(0xFFEF4444) else GoldPrimary.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444))
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%d:%02d", inlineRecordSeconds / 60, inlineRecordSeconds % 60),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isInlineRecordCancelZone) Color(0xFFEF4444) else colors.onSurface
                                        )
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(22.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val bars = if (inlineRecordAmplitudes.isEmpty()) List(14) { 0.25f } else inlineRecordAmplitudes
                                            bars.forEach { norm ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height((4 + (norm * 16)).dp)
                                                        .clip(CircleShape)
                                                        .background(if (isInlineRecordCancelZone) Color(0xFFEF4444) else GoldPrimary)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "اسحب للإلغاء",
                                            tint = if (isInlineRecordCancelZone) Color(0xFFEF4444) else colors.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            voiceDraftPath != null -> {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = colors.surfaceVariant.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (isDraftAudioPlaying) {
                                                    AudioPlayerManager.stop()
                                                    isDraftAudioPlaying = false
                                                }
                                                val p = voiceDraftPath
                                                voiceDraftPath = null
                                                voiceDraftDurationSec = 0
                                                draftAudioProgress = 0f
                                                if (!p.isNullOrBlank()) {
                                                    try { File(p).delete() } catch (_: Exception) {}
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف التسجيل", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(adaptiveGoldPrimary)
                                                .clickable {
                                                    val path = voiceDraftPath ?: return@clickable
                                                    if (isDraftAudioPlaying) {
                                                        AudioPlayerManager.stop()
                                                        isDraftAudioPlaying = false
                                                    } else {
                                                        isDraftAudioPlaying = true
                                                        AudioPlayerManager.play(
                                                            context = context,
                                                            pathOrUri = path,
                                                            onProgress = { cur, tot ->
                                                                if (tot > 0) {
                                                                    draftAudioProgress = (cur.toFloat() / tot.toFloat()).coerceIn(0f, 1f)
                                                                }
                                                            },
                                                            onComplete = {
                                                                isDraftAudioPlaying = false
                                                                draftAudioProgress = 0f
                                                            }
                                                        )
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isDraftAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isDraftAudioPlaying) "إيقاف" else "تشغيل",
                                                tint = Color.Black,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(22.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val totalBars = 14
                                            repeat(totalBars) { idx ->
                                                val frac = idx.toFloat() / totalBars.toFloat()
                                                val passed = frac <= draftAudioProgress
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height((5 + ((idx * 7) % 13)).dp)
                                                        .clip(CircleShape)
                                                        .background(if (passed) adaptiveGoldPrimary else adaptiveGoldPrimary.copy(alpha = 0.3f))
                                                )
                                            }
                                        }

                                        Text(
                                            text = String.format(Locale.US, "%d:%02d", voiceDraftDurationSec / 60, voiceDraftDurationSec % 60),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onSurface,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                    }
                                }
                            }

                            else -> {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = colors.surfaceVariant.copy(alpha = 0.55f),
                                    border = BorderStroke(0.8.dp, colors.outlineVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 42.dp, max = 96.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (messageInputText.isEmpty()) {
                                            Text(
                                                text = if (attachedMediaUri != null || attachedDocumentInfo != null) "تعليق..." else "رسالة...",
                                                fontSize = 13.5.sp,
                                                color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                        BasicTextField(
                                            value = messageInputText,
                                            onValueChange = { messageInputText = it },
                                            textStyle = TextStyle(fontSize = 13.5.sp, color = colors.onSurface),
                                            cursorBrush = SolidColor(GoldPrimary),
                                            maxLines = 3,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("floating_bubble_input_field")
                                        )
                                    }
                                }
                            }
                        }

                        val hasContentToSend = messageInputText.isNotBlank() ||
                            attachedMediaUri != null ||
                            attachedDocumentInfo != null ||
                            voiceDraftPath != null

                        if (hasContentToSend) {
                            IconButton(
                                onClick = {
                                    when {
                                        voiceDraftPath != null -> {
                                            if (isDraftAudioPlaying) {
                                                AudioPlayerManager.stop()
                                                isDraftAudioPlaying = false
                                            }
                                            val dur = voiceDraftDurationSec.coerceAtLeast(1)
                                            val payload = "${voiceDraftPath}||$dur||"
                                            coroutineScope.launch {
                                                val user = currentUser
                                                repository.sendMessage(
                                                    chatId = activeChatId,
                                                    senderId = user?.id ?: "me",
                                                    senderName = user?.name ?: "المستخدم",
                                                    content = payload,
                                                    type = MessageType.VOICE.name
                                                )
                                            }
                                            voiceDraftPath = null
                                            voiceDraftDurationSec = 0
                                            draftAudioProgress = 0f
                                            replyingToMessage = null
                                        }
                                        attachedDocumentInfo != null -> {
                                            val doc = attachedDocumentInfo ?: return@IconButton
                                            val payload = "${doc.uri}||${doc.fileName}||${doc.fileSize}||${messageInputText.trim()}"
                                            coroutineScope.launch {
                                                val user = currentUser
                                                repository.sendMessage(
                                                    chatId = activeChatId,
                                                    senderId = user?.id ?: "me",
                                                    senderName = user?.name ?: "المستخدم",
                                                    content = payload,
                                                    type = MessageType.DOCUMENT.name
                                                )
                                            }
                                            attachedDocumentInfo = null
                                            messageInputText = ""
                                            replyingToMessage = null
                                        }
                                        attachedMediaUri != null -> {
                                            val mediaUri = attachedMediaUri!!
                                            val caption = messageInputText.trim()
                                            val payload = if (caption.isNotBlank()) "${mediaUri}||$caption" else mediaUri.toString()
                                            coroutineScope.launch {
                                                val user = currentUser
                                                repository.sendMessage(
                                                    chatId = activeChatId,
                                                    senderId = user?.id ?: "me",
                                                    senderName = user?.name ?: "المستخدم",
                                                    content = payload,
                                                    type = MessageType.IMAGE.name
                                                )
                                            }
                                            attachedMediaUri = null
                                            messageInputText = ""
                                            replyingToMessage = null
                                        }
                                        messageInputText.isNotBlank() -> {
                                            val text = messageInputText
                                            messageInputText = ""
                                            dispatchTextOrReply(text)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary)
                                    .testTag("floating_bubble_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "إرسال",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            val cancelThresholdPx = with(density) { 80.dp.toPx() }
                            val swipeDownCamPx = with(density) { 22.dp.toPx() }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isInlineRecordingAudio && isInlineRecordCancelZone -> Color(0xFFEF4444)
                                            isInlineRecordingAudio -> Color(0xFFEF4444)
                                            isActionCameraMode -> Color(0xFF10B981)
                                            else -> GoldPrimary
                                        }
                                    )
                                    .pointerInput(isActionCameraMode, activeChatId) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val touchSlop = viewConfiguration.touchSlop
                                            var totalDx = 0f
                                            var totalDy = 0f
                                            var isDragging = false
                                            var recordingStarted = false
                                            var recStartMs = 0L
                                            var camTriggered = false

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id }
                                                    ?: event.changes.firstOrNull()
                                                    ?: break

                                                if (!change.pressed) {
                                                    if (recordingStarted) {
                                                        isInlineRecordingAudio = false
                                                        if (isInlineRecordCancelZone) {
                                                            AudioRecorderManager.cancelRecording()
                                                            isInlineRecordCancelZone = false
                                                        } else {
                                                            val recorded = AudioRecorderManager.stopRecording()
                                                            val heldMs = System.currentTimeMillis() - recStartMs
                                                            if (recorded != null && recorded.exists() && heldMs >= 320L) {
                                                                voiceDraftPath = recorded.absolutePath
                                                                voiceDraftDurationSec = (heldMs / 1000L).toInt().coerceAtLeast(1)
                                                                draftAudioProgress = 0f
                                                                isDraftAudioPlaying = false
                                                            } else if (recorded != null && recorded.exists()) {
                                                                try { recorded.delete() } catch (_: Exception) {}
                                                            }
                                                        }
                                                    } else if (!isDragging && !camTriggered) {
                                                        isActionCameraMode = !isActionCameraMode
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    break
                                                }

                                                val delta = change.position - change.previousPosition
                                                totalDx += delta.x
                                                totalDy += delta.y

                                                if (!isDragging && hypot(totalDx.toDouble(), totalDy.toDouble()) > touchSlop) {
                                                    isDragging = true
                                                    if (!isActionCameraMode) {
                                                        val hasMic = ContextCompat.checkSelfPermission(
                                                            context,
                                                            Manifest.permission.RECORD_AUDIO
                                                        ) == PackageManager.PERMISSION_GRANTED
                                                        if (!hasMic) {
                                                            MessengerBubbleManager.launchBubbleMediaBridge(
                                                                context = context,
                                                                action = BubbleMediaBridgeActivity.ACTION_REQUEST_MIC,
                                                                chatId = activeChatId
                                                            )
                                                            break
                                                        }
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        val file = AudioRecorderManager.startRecording(context)
                                                        if (file != null) {
                                                            recordingStarted = true
                                                            recStartMs = System.currentTimeMillis()
                                                            inlineRecordSeconds = 0
                                                            isInlineRecordCancelZone = false
                                                            isInlineRecordingAudio = true
                                                        }
                                                    }
                                                }

                                                if (isDragging) {
                                                    change.consume()
                                                    if (!isActionCameraMode && recordingStarted) {
                                                        val cancelNow = kotlin.math.abs(totalDx) > cancelThresholdPx
                                                        if (cancelNow != isInlineRecordCancelZone) {
                                                            isInlineRecordCancelZone = cancelNow
                                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                    } else if (isActionCameraMode && !camTriggered && totalDy > swipeDownCamPx) {
                                                        camTriggered = true
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        MessengerBubbleManager.launchBubbleMediaBridge(
                                                            context = context,
                                                            action = BubbleMediaBridgeActivity.ACTION_VIDEO_CAMERA,
                                                            chatId = activeChatId
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .testTag("floating_bubble_mic_cam_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        isInlineRecordingAudio && isInlineRecordCancelZone -> Icons.Default.Close
                                        isInlineRecordingAudio -> Icons.Default.Mic
                                        isActionCameraMode -> Icons.Default.Videocam
                                        else -> Icons.Default.Mic
                                    },
                                    contentDescription = if (isActionCameraMode) "تسجيل فيديو" else "تسجيل صوتي",
                                    tint = if (isInlineRecordingAudio || isActionCameraMode) Color.White else Color.Black,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overlay 1: Minimal 2-Option Media Chooser (Camera / Gallery ONLY, no Dialog window!)
        val chooserKind = activeMediaChooserKind
        if (chooserKind != null) {
            MediaSourceChooserCard(
                kind = chooserKind,
                onCameraClick = {
                    activeMediaChooserKind = null
                    MessengerBubbleManager.launchBubbleMediaBridge(
                        context = context,
                        action = if (chooserKind == MediaChooserKind.PHOTO) {
                            BubbleMediaBridgeActivity.ACTION_PHOTO_CAMERA
                        } else {
                            BubbleMediaBridgeActivity.ACTION_VIDEO_CAMERA
                        },
                        chatId = activeChatId
                    )
                },
                onGalleryClick = {
                    activeMediaChooserKind = null
                    MessengerBubbleManager.launchBubbleMediaBridge(
                        context = context,
                        action = if (chooserKind == MediaChooserKind.PHOTO) {
                            BubbleMediaBridgeActivity.ACTION_PHOTO_GALLERY
                        } else {
                            BubbleMediaBridgeActivity.ACTION_VIDEO_GALLERY
                        },
                        chatId = activeChatId
                    )
                },
                onDismiss = { activeMediaChooserKind = null },
                useOverlayBox = true
            )
        }

        // Overlay 2: Photo Editor (overlay-safe Box mode)
        val photoToEdit = pendingPhotoUriForEditor
        if (photoToEdit != null) {
            CoverPhotoEditorDialog(
                imageUri = photoToEdit,
                title = "تعديل الصورة",
                frameMode = ImageFrameAspectMode.PRODUCT_PHOTO,
                useOverlayBox = true,
                onSaveCroppedCover = { savedPath ->
                    pendingPhotoUriForEditor = null
                    attachedMediaUri = Uri.parse(savedPath)
                    attachedMediaIsVideo = false
                },
                onDismiss = {
                    pendingPhotoUriForEditor = null
                }
            )
        }

        // Overlay 3: Video Editor (overlay-safe Box mode)
        val videoToEdit = pendingVideoUriForEditor
        if (videoToEdit != null) {
            VideoEditorDialog(
                videoUri = videoToEdit,
                initialCaption = messageInputText,
                showCaptionField = true,
                targetFolder = "chat_videos",
                useOverlayBox = true,
                onConfirmSave = { editedUri, _, editedCaption ->
                    pendingVideoUriForEditor = null
                    attachedMediaUri = editedUri
                    attachedMediaIsVideo = true
                    if (editedCaption.isNotBlank()) {
                        messageInputText = editedCaption
                    }
                },
                onDismiss = {
                    pendingVideoUriForEditor = null
                }
            )
        }

        // Overlay 4: Fullscreen Media Preview (Photos & Videos, overlay-safe Box mode)
        val preview = activeMediaPreview
        if (preview != null) {
            MediaPreviewModal(
                item = preview,
                useOverlayBox = true,
                onDismiss = { activeMediaPreview = null }
            )
        }

        // Overlay 5: In-Bubble Quick Financial #Hashtag Composer (overlay-safe Box)
        if (showHashtagComposer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) { detectTapGestures { showHashtagComposer = false } }
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) { detectTapGestures { } }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("قيد #هاشتاج مالي سريع", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showHashtagComposer = false }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("فاتورة", "سداد", "دفعة", "توريد", "عهدة")) { preset ->
                                FilterChip(
                                    selected = hashtagTagInput == preset,
                                    onClick = { hashtagTagInput = preset },
                                    label = { Text("#$preset", fontSize = 12.sp) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = hashtagTagInput,
                            onValueChange = { hashtagTagInput = it },
                            label = { Text("اسم الوسم (بدون #)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = hashtagAmountInput,
                            onValueChange = { hashtagAmountInput = it },
                            label = { Text("المبلغ (ج.م)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = hashtagDirection == HashtagEntryDirection.DEBIT,
                                onClick = { hashtagDirection = HashtagEntryDirection.DEBIT },
                                label = { Text("عليه / مدين") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = hashtagDirection == HashtagEntryDirection.CREDIT,
                                onClick = { hashtagDirection = HashtagEntryDirection.CREDIT },
                                label = { Text("له / دائن") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Button(
                            onClick = {
                                val cleanTag = hashtagTagInput.trim().removePrefix("#").ifBlank { "فاتورة" }
                                val amt = hashtagAmountInput.trim().toDoubleOrNull() ?: 0.0
                                showHashtagComposer = false
                                hashtagTagInput = ""
                                hashtagAmountInput = ""
                                val line = if (amt > 0.0) "#$cleanTag بمبلغ ${String.format(Locale.US, "%.2f", amt)} ج.م" else "#$cleanTag"
                                coroutineScope.launch {
                                    val user = currentUser
                                    val senderId = user?.id ?: "me"
                                    val senderName = user?.name ?: "المستخدم"
                                    repository.sendMessage(
                                        chatId = activeChatId,
                                        senderId = senderId,
                                        senderName = senderName,
                                        content = line,
                                        type = MessageType.HASHTAG.name
                                    )
                                    if (amt > 0.0) {
                                        repository.recordChatHashtagOperation(
                                            chatId = activeChatId,
                                            tag = cleanTag,
                                            entryType = if (hashtagDirection == HashtagEntryDirection.CREDIT) HashtagEntryType.PAYMENT else HashtagEntryType.INVOICE,
                                            description = line,
                                            amount = amt,
                                            direction = hashtagDirection,
                                            actorId = senderId,
                                            actorName = senderName,
                                            postChatMessage = false
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("إرسال وإثبات في الدفتر", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Overlay 6: Clickable #Hashtag & 8-char Invoice/Operation Inspector (overlay-safe Box)
        val token = inspectedFinancialToken
        if (token != null) {
            BubbleFinancialInspectorOverlay(
                token = token,
                hashtags = allHashtags,
                invoices = allInvoices,
                transactions = allTransactions,
                onDismiss = { inspectedFinancialToken = null },
                onOpenFullApp = {
                    inspectedFinancialToken = null
                    onOpenInFullApp(activeChatId)
                }
            )
        }

        // Overlay 7: Active Audio / Video Call Overlay inside Bubble (overlay-safe Box)
        val activeCall = activeCallType
        if (activeCall != null) {
            BubbleActiveCallOverlay(
                callType = activeCall,
                contactName = activeConv?.contactName ?: "محادثة",
                callState = callState,
                callSeconds = callSeconds,
                isMuted = isCallMuted,
                isSpeakerOn = isCallSpeaker,
                isVideoOn = isCallVideoOn,
                onToggleMute = { isCallMuted = !isCallMuted },
                onToggleSpeaker = {
                    isCallSpeaker = !isCallSpeaker
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    audioManager?.isSpeakerphoneOn = isCallSpeaker
                },
                onToggleVideo = { isCallVideoOn = !isCallVideoOn },
                onEndCall = { endInBubbleCall() }
            )
        }
    }
}

@Composable
private fun BubbleAttachOptionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun BubbleActiveCallOverlay(
    callType: CallType,
    contactName: String,
    callState: CallState,
    callSeconds: Int,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isVideoOn: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onEndCall: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "bubble_call_pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse_scale"
    )
    val stateLabel = when (callState) {
        CallState.OUTGOING_RINGING -> "جاري الرنين..."
        CallState.CONNECTING -> "جاري التوصيل المشفر..."
        CallState.CONNECTED -> String.format(Locale.US, "%02d:%02d", callSeconds / 60, callSeconds % 60)
        else -> "مكالمة نشطة"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF50B0E17))
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 36.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = GoldPrimary.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                        Text(callType.label, color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .scale(if (callState != CallState.CONNECTED) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.18f))
                        .border(2.dp, GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contactName.trim().take(1).ifBlank { "O" },
                        color = GoldPrimary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = contactName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stateLabel,
                    color = if (callState == CallState.CONNECTED) Color(0xFF10B981) else Color(0xFFCBD5E1),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "كتم الميكروفون",
                        tint = if (isMuted) Color.Black else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (callType == CallType.VIDEO) {
                    IconButton(
                        onClick = onToggleVideo,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (!isVideoOn) Color.White else Color.White.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = if (isVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "تبديل الكاميرا",
                            tint = if (!isVideoOn) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "مكبر الصوت",
                        tint = if (isSpeakerOn) Color.Black else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .testTag("bubble_end_call_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "إنهاء المكالمة",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BubbleFinancialInspectorOverlay(
    token: String,
    hashtags: List<HashtagEntryEntity>,
    invoices: List<InvoiceEntity>,
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit,
    onOpenFullApp: () -> Unit
) {
    val cleanToken = remember(token) { token.trim().removePrefix("#") }
    val matchingInvoice = remember(cleanToken, invoices) {
        invoices.firstOrNull {
            it.invoiceNumber.equals(cleanToken, ignoreCase = true) ||
                it.id.equals(cleanToken, ignoreCase = true)
        }
    }
    val matchingTx = remember(cleanToken, transactions) {
        transactions.firstOrNull { it.id.equals(cleanToken, ignoreCase = true) }
    }
    val matchingEntries = remember(cleanToken, hashtags) {
        hashtags.filter {
            it.description.contains(cleanToken, ignoreCase = true) ||
                it.id.equals(cleanToken, ignoreCase = true) ||
                it.linkedInvoiceId.equals(cleanToken, ignoreCase = true)
        }
    }

    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = GoldPrimary)
                        Text("تفاصيل القيد: $token", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                if (matchingInvoice != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = GoldPrimary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("فاتورة رقم ${matchingInvoice.invoiceNumber}", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 14.sp)
                            Text("العميل: ${matchingInvoice.receiverName}", fontSize = 12.5.sp)
                            Text("الإجمالي: ${String.format(Locale.US, "%.2f", matchingInvoice.totalAmount)} ج.م", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("الحالة: ${matchingInvoice.status}", fontSize = 12.sp, color = colors.onSurfaceVariant)
                        }
                    }
                }

                if (matchingTx != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("عملية مالية: ${matchingTx.id}", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.5.sp)
                            Text("القيمة: ${String.format(Locale.US, "%.2f", matchingTx.totalPrice)} ج.م", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            if (matchingTx.weight > 0.0) {
                                Text("الوزن: ${matchingTx.weight} جم (عيار ${matchingTx.goldKarat})", fontSize = 12.sp)
                            }
                            val txNotes = matchingTx.notes
                            if (!txNotes.isNullOrBlank()) {
                                Text(txNotes, fontSize = 12.sp, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (matchingEntries.isNotEmpty()) {
                    val totals = HashtagUtils.calculateHashtagTotals(matchingEntries)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("ملخص الوسم (${matchingEntries.size} قيود)", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text("المستحق (عليه): ${String.format(Locale.US, "%.2f", totals.totalDebit)} ج.م", fontSize = 12.sp)
                            Text("المدفوع (له): ${String.format(Locale.US, "%.2f", totals.totalCredit)} ج.م", fontSize = 12.sp)
                            Text("المتبقي: ${String.format(Locale.US, "%.2f", totals.remaining)} ج.م", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 12.5.sp)
                        }
                    }
                }

                if (matchingInvoice == null && matchingTx == null && matchingEntries.isEmpty()) {
                    Text(
                        text = "يمكن استعراض هذا القيد وجميع الحسابات المرتبطة به داخل دفتر المحاسبة.",
                        fontSize = 12.5.sp,
                        color = colors.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onOpenFullApp,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("فتح السجل الكامل في التطبيق", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FloatingMessageBubble(
    msg: MessageEntity,
    isMe: Boolean,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true,
    isActionMenuOpen: Boolean = false,
    onOpenActionMenu: () -> Unit = {},
    onDismissActionMenu: () -> Unit = {},
    onReactionClick: ((String) -> Unit)? = null,
    onReplyClick: () -> Unit = {},
    onMediaClick: (MediaPreviewItem) -> Unit = {},
    onCodeOrHashtagClick: (String) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formattedTime = remember(msg.timestamp) { timeFormat.format(Date(msg.timestamp)) }

    val extractedHashtag = remember(msg.content) { HashtagUtils.parseHashtag(msg.content) }
    val extractedCode = remember(msg.content) { ShortCodeGenerator.extractAllCodes(msg.content).firstOrNull() }
    val clickableFinancialToken = extractedCode ?: extractedHashtag?.let { "#$it" }
    val isFinancialMsg = clickableFinancialToken != null ||
        msg.type == MessageType.INVOICE.name ||
        msg.type == MessageType.PAYMENT.name ||
        msg.type == MessageType.HASHTAG.name

    val isImageMsg = msg.type == MessageType.IMAGE.name
    val isVoiceMsg = msg.type == MessageType.VOICE.name
    val isDocMsg = msg.type == MessageType.DOCUMENT.name
    val isCallMsg = msg.type == MessageType.CALL.name || msg.type == MessageType.MISSED_CALL.name
    val isMissedCall = msg.type == MessageType.MISSED_CALL.name

    val bubbleShape = RoundedCornerShape(
        topStart = if (!isMe && !isFirstInGroup) 6.dp else 18.dp,
        topEnd = if (isMe && !isFirstInGroup) 6.dp else 18.dp,
        bottomStart = if (!isMe && !isLastInGroup) 6.dp else if (!isMe) 5.dp else 18.dp,
        bottomEnd = if (isMe && !isLastInGroup) 6.dp else if (isMe) 5.dp else 18.dp
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            // Quick Reactions & Reply Action Bar on tap/long-press
            AnimatedVisibility(visible = isActionMenuOpen) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("👍", "❤️", "👌", "🔥", "✅").forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onReactionClick?.invoke(emoji) }
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(colors.outlineVariant)
                        )
                        IconButton(
                            onClick = onReplyClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Reply, contentDescription = "رد", tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = onDismissActionMenu,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }

            Surface(
                shape = bubbleShape,
                color = when {
                    isMe && isFinancialMsg -> GoldPrimary.copy(alpha = 0.92f)
                    isMe -> colors.primary
                    isFinancialMsg -> colors.tertiaryContainer
                    else -> colors.surfaceVariant
                },
                border = if (isFinancialMsg) BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)) else null,
                modifier = Modifier
                    .widthIn(max = 296.dp)
                    .pointerInput(msg.id, clickableFinancialToken) {
                        detectTapGestures(
                            onLongPress = { onOpenActionMenu() },
                            onTap = {
                                if (clickableFinancialToken != null) {
                                    onCodeOrHashtagClick(clickableFinancialToken)
                                } else {
                                    onOpenActionMenu()
                                }
                            }
                        )
                    }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    val textColor = when {
                        isMe && isFinancialMsg -> Color.Black
                        isMe -> colors.onPrimary
                        isFinancialMsg -> colors.onTertiaryContainer
                        else -> colors.onSurfaceVariant
                    }

                    if (clickableFinancialToken != null) {
                        Surface(
                            onClick = { onCodeOrHashtagClick(clickableFinancialToken) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isMe) Color.Black.copy(alpha = 0.14f) else GoldPrimary.copy(alpha = 0.16f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "$clickableFinancialToken • معاينة القيد",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMe) Color.Black else GoldPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    when {
                        isDocMsg -> {
                            val parts = msg.content.split("||")
                            DocumentMessageItem(
                                fileUri = parts.getOrNull(0) ?: "",
                                fileName = parts.getOrNull(1) ?: "مستند_مرفق",
                                fileSize = parts.getOrNull(2)?.toLongOrNull() ?: 0L,
                                caption = if (parts.size > 3) parts[3] else "",
                                isMe = isMe,
                                zoomScale = 0.95f
                            )
                        }
                        isImageMsg -> {
                            val imageParts = msg.content.split("||", limit = 2)
                            val mediaUri = imageParts[0].trim()
                            val caption = if (imageParts.size > 1) imageParts[1].trim() else ""
                            val isVid = FileUtils.isVideoUriString(mediaUri)
                            val previewItem = MediaPreviewItem(
                                uri = mediaUri,
                                caption = caption,
                                senderName = msg.senderName,
                                timestamp = msg.timestamp,
                                isVideo = isVid
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isVid) {
                                    InlineChatVideoPlayer(
                                        videoUri = mediaUri,
                                        width = 216.dp,
                                        height = 160.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        onExpandFullscreen = { onMediaClick(previewItem) }
                                    )
                                } else {
                                    OozoxThumbnailImage(
                                        model = mediaUri,
                                        width = 216.dp,
                                        height = 160.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        contentScale = ContentScale.Fit,
                                        contentDescription = "صورة المحادثة",
                                        onClick = { onMediaClick(previewItem) }
                                    )
                                }
                                if (caption.isNotBlank()) {
                                    Text(text = caption, color = textColor, fontSize = 12.5.sp)
                                }
                            }
                        }
                        isVoiceMsg -> {
                            VoiceMessagePlayer(
                                content = msg.content,
                                isMe = isMe,
                                zoomScale = 0.92f
                            )
                        }
                        isCallMsg -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isMissedCall) Color(0xFFEF4444).copy(alpha = 0.16f) else GoldPrimary.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isMissedCall) Icons.Default.PhoneMissed else Icons.Default.Call,
                                        contentDescription = null,
                                        tint = if (isMissedCall) Color(0xFFEF4444) else GoldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = msg.content,
                                        color = if (isMissedCall) Color(0xFFEF4444) else textColor,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        else -> {
                            if (msg.content.startsWith("↩️ رد على ") && msg.content.contains("\n\n")) {
                                val quoteBlock = msg.content.substringBefore("\n\n")
                                val mainBody = msg.content.substringAfter("\n\n")
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.12f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        text = quoteBlock.removePrefix("↩️ "),
                                        fontSize = 10.5.sp,
                                        color = textColor.copy(alpha = 0.8f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = mainBody,
                                    color = textColor,
                                    fontSize = 12.8.sp,
                                    lineHeight = 17.5.sp
                                )
                            } else {
                                Text(
                                    text = msg.content,
                                    color = textColor,
                                    fontSize = 12.8.sp,
                                    lineHeight = 17.5.sp
                                )
                            }
                        }
                    }

                    if (isLastInGroup) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formattedTime,
                                fontSize = 9.5.sp,
                                color = textColor.copy(alpha = 0.68f)
                            )
                            if (isMe) {
                                Spacer(modifier = Modifier.width(3.dp))
                                val (tickIcon, tickTint) = when (msg.status) {
                                    "READ" -> Icons.Default.DoneAll to textColor
                                    "DELIVERED" -> Icons.Default.DoneAll to textColor.copy(alpha = 0.65f)
                                    else -> Icons.Default.Done to textColor.copy(alpha = 0.65f)
                                }
                                Icon(
                                    imageVector = tickIcon,
                                    contentDescription = msg.status,
                                    tint = tickTint,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!msg.reaction.isNullOrBlank()) {
                Surface(
                    onClick = { onReactionClick?.invoke(msg.reaction) },
                    shape = CircleShape,
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.outlineVariant),
                    modifier = Modifier
                        .offset(y = (-4).dp)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = msg.reaction,
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
