package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MessageEntity
import com.example.data.model.MessageType
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Modern Floating Chat Bubbles System (Facebook Messenger Style).
 * Delivers persistent, multi-conversation floating chat heads with spring physics,
 * account switching (Personal vs Business), in-bubble notification center,
 * full settings control, and rich ledger integration.
 */
@Composable
fun MessengerBubbleOverlay(
    viewModel: MainViewModel,
    onNavigateToFullChat: (String) -> Unit
) {
    val context = LocalContext.current
    val bubbleState by MessengerBubbleManager.bubbleState.collectAsStateWithLifecycle()
    val isEnabled by MessengerBubbleManager.isBubbleFeatureEnabled.collectAsStateWithLifecycle()
    val showPermissionDialog by MessengerBubbleManager.showPermissionDialog.collectAsStateWithLifecycle()

    if (showPermissionDialog) {
        FloatingBubblePermissionDialog(
            onDismiss = {
                // If user dismisses or chooses local in-app usage, ensure bubble is still available inside the app!
                MessengerBubbleManager.setShowPermissionDialog(false)
                MessengerBubbleManager.ensureActiveBubble(context, autoExpand = true)
            },
            onGrant = {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                } catch (_: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
                MessengerBubbleManager.setShowPermissionDialog(false)
                MessengerBubbleManager.requestPermissionWithAutoLaunch(context)
            }
        )
    }

    // Floating bubble is exclusively displayed outside the app via FloatingBubbleService when the app is closed/backgrounded.
    // Inside the open app, we do not render a duplicate in-app bubble overlay.
    return

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val haptic = LocalHapticFeedback.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val bubbleSizePx = with(density) { 68.dp.toPx() }

    // Floating drag offset state
    var offsetX by remember { mutableStateOf(screenWidthPx - bubbleSizePx - with(density) { 16.dp.toPx() }) }
    var offsetY by remember { mutableStateOf(screenHeightPx * 0.35f) }
    var isDragging by remember { mutableStateOf(false) }
    var isOverDismissZone by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }
    var isDockedOnRight by remember { mutableStateOf(true) }
    var showTooltipPreview by remember { mutableStateOf(true) }
    var hasMounted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        hasMounted = true
    }

    LaunchedEffect(showTooltipPreview, bubbleState.isExpanded, bubbleState.lastMessageText) {
        if (showTooltipPreview && !bubbleState.isExpanded) {
            delay(3800)
            showTooltipPreview = false
        }
    }

    // Dismiss target zone bounds (at bottom center of screen)
    val dismissTargetY = screenHeightPx - with(density) { 130.dp.toPx() }
    val dismissTargetX = (screenWidthPx - with(density) { 72.dp.toPx() }) / 2f

    // Dynamic Fade Out & Scale calculations while dragging down towards bottom
    val dragDownProgress = remember(offsetY, screenHeightPx) {
        ((offsetY - (screenHeightPx * 0.32f)) / (screenHeightPx * 0.45f)).coerceIn(0f, 1f)
    }

    val targetAlpha = when {
        !hasMounted -> 0f
        isDismissing -> 0f
        isOverDismissZone -> 0.25f
        isDragging -> (1f - (dragDownProgress * 0.65f)).coerceIn(0.35f, 1f)
        else -> 1f
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = if (isDismissing) 200 else if (isDragging) 40 else 240),
        label = "bubble_fade_alpha"
    )

    val targetScale = when {
        !hasMounted -> 0.25f
        isDismissing -> 0.15f
        isOverDismissZone -> 0.78f
        else -> 1f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bubble_scale"
    )

    // Minimized Floating Head(s) View
    if (!bubbleState.isExpanded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            // Dismiss target zone at bottom center when dragging
            AnimatedVisibility(
                visible = isDragging || isDismissing,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isOverDismissZone) 80.dp else 64.dp)
                            .clip(CircleShape)
                            .background(if (isOverDismissZone) StatusRed else Color(0xCC000000))
                            .border(
                                width = 2.dp,
                                color = if (isOverDismissZone) Color.White else StatusRed.copy(alpha = 0.8f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق الفقاعة",
                            tint = Color.White,
                            modifier = Modifier.size(if (isOverDismissZone) 36.dp else 26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isOverDismissZone) "أفلت للإغلاق" else "اسحب هنا للإغلاق",
                        color = if (isOverDismissZone) StatusRed else Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Draggable Floating Bubble with Real-time Snap and Message Preview Cloud
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .graphicsLayer {
                        alpha = animatedAlpha
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                if (isDismissing) return@detectDragGestures
                                isDragging = true
                                showTooltipPreview = false
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = {
                                isDragging = false
                                val isNearBottom = offsetY > screenHeightPx - with(density) { 150.dp.toPx() }
                                if (isOverDismissZone || isNearBottom) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isDismissing = true
                                    coroutineScope.launch {
                                        val startX = offsetX
                                        val startY = offsetY
                                        val targetX = dismissTargetX + with(density) { 4.dp.toPx() }
                                        val targetY = dismissTargetY - with(density) { 6.dp.toPx() }
                                        val steps = 12
                                        for (step in 1..steps) {
                                            val fraction = step / steps.toFloat()
                                            offsetX = startX + (targetX - startX) * fraction
                                            offsetY = startY + (targetY - startY) * fraction
                                            delay(14)
                                        }
                                        MessengerBubbleManager.dismissBubble()
                                        isOverDismissZone = false
                                        isDismissing = false
                                    }
                                } else {
                                    // Snap smoothly to left or right screen border
                                    val isRight = (offsetX + bubbleSizePx / 2 >= screenWidthPx / 2)
                                    isDockedOnRight = isRight
                                    val snapTargetX = if (isRight) {
                                        screenWidthPx - bubbleSizePx - with(density) { 12.dp.toPx() }
                                    } else {
                                        with(density) { 12.dp.toPx() }
                                    }
                                    coroutineScope.launch {
                                        val startX = offsetX
                                        val steps = 10
                                        for (step in 1..steps) {
                                            val fraction = step / steps.toFloat()
                                            offsetX = startX + (snapTargetX - startX) * fraction
                                            delay(12)
                                        }
                                        offsetX = snapTargetX
                                        showTooltipPreview = true
                                    }
                                    offsetY = offsetY.coerceIn(
                                        with(density) { 60.dp.toPx() },
                                        screenHeightPx - bubbleSizePx - with(density) { 100.dp.toPx() }
                                    )
                                }
                            },
                            onDragCancel = {
                                isDragging = false
                                isOverDismissZone = false
                            },
                            onDrag = { change, dragAmount ->
                                if (isDismissing) return@detectDragGestures
                                change.consume()
                                offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx - bubbleSizePx)
                                offsetY = (offsetY + dragAmount.y).coerceIn(
                                    with(density) { 40.dp.toPx() },
                                    screenHeightPx - bubbleSizePx - with(density) { 30.dp.toPx() }
                                )

                                val distanceToDismiss = kotlin.math.hypot(
                                    offsetX - dismissTargetX,
                                    offsetY - dismissTargetY
                                )
                                val isNearBottom = offsetY > screenHeightPx - with(density) { 150.dp.toPx() }
                                val nowOver = distanceToDismiss < with(density) { 110.dp.toPx() } || isNearBottom
                                if (nowOver != isOverDismissZone) {
                                    isOverDismissZone = nowOver
                                    if (nowOver) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                        )
                    }
                    .testTag("floating_messenger_bubble_root")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isDockedOnRight) Arrangement.End else Arrangement.Start
                ) {
                    // Preview Cloud on the Left if docked on the right
                    if (isDockedOnRight && showTooltipPreview && bubbleState.lastMessageText.isNotBlank()) {
                        BubbleSneakPeekCard(
                            contactName = bubbleState.contactName,
                            messageText = bubbleState.lastMessageText,
                            accountType = bubbleState.activeAccountType,
                            isOnline = bubbleState.isOnline,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                MessengerBubbleManager.expandBubble()
                            },
                            onDismiss = { showTooltipPreview = false }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Circular Bubble Head(s)
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isDragging && !isDismissing) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    MessengerBubbleManager.expandBubble()
                                }
                            }
                            .shadow(elevation = 16.dp, shape = CircleShape)
                            .testTag("floating_chat_bubble_head")
                    ) {
                        // Secondary stacked head behind primary if more than 1 conversation
                        if (bubbleState.conversations.size > 1) {
                            val secondaryConv = bubbleState.conversations.getOrNull(1)
                            if (secondaryConv != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .offset(x = (-5).dp, y = (-5).dp)
                                        .scale(0.88f)
                                        .rotate(-8f)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E242B))
                                        .border(1.5.dp, GoldAccent.copy(alpha = 0.7f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = secondaryConv.contactName.trim().take(1).ifBlank { "2" },
                                        color = GoldAccent,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Pulse animation when unread messages are present
                        val infiniteTransition = rememberInfiniteTransition(label = "bubble_pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = if (bubbleState.totalUnreadCount > 0) 1.08f else 1.02f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_scale"
                        )

                        // Primary Head Container
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            GoldPrimary,
                                            GoldLight,
                                            GoldAccent,
                                            if (bubbleState.activeAccountType == BubbleAccountType.PERSONAL) Color(0xFF3B82F6) else Color(0xFFD4AF37),
                                            GoldPrimary
                                        )
                                    )
                                )
                                .padding(2.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                val initialLetter = bubbleState.contactName.trim().take(1).ifBlank { "O" }
                                Text(
                                    text = initialLetter,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = adaptiveGoldPrimary
                                )

                                // Online / Offline dot indicator on avatar edge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(2.dp)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (bubbleState.isOnline) StatusGreen else Color(0xFF9CA3AF))
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                )

                                // Account type badge (Icon)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(2.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (bubbleState.activeAccountType == BubbleAccountType.PERSONAL) Color(0xFF1E3A8A) else Color(0xFF3B2F08)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (bubbleState.activeAccountType == BubbleAccountType.PERSONAL) Icons.Default.Person else Icons.Default.Work,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }

                        // Total Unread Count Badge
                        if (bubbleState.totalUnreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 3.dp, y = (-3).dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(StatusRed)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (bubbleState.totalUnreadCount > 9) "9+" else bubbleState.totalUnreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Multi-Bubble Count Indicator (if > 1 conversations open)
                        if (bubbleState.conversations.size > 1) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = (-3).dp, y = (-2).dp)
                                    .size(19.dp)
                                    .clip(CircleShape)
                                    .background(adaptiveGoldPrimary)
                                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${bubbleState.conversations.size}",
                                    color = adaptiveOnPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Preview Cloud on the Right if docked on the left
                    if (!isDockedOnRight && showTooltipPreview && bubbleState.lastMessageText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        BubbleSneakPeekCard(
                            contactName = bubbleState.contactName,
                            messageText = bubbleState.lastMessageText,
                            accountType = bubbleState.activeAccountType,
                            isOnline = bubbleState.isOnline,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                MessengerBubbleManager.expandBubble()
                            },
                            onDismiss = { showTooltipPreview = false }
                        )
                    }
                }
            }
        }
    }

    // Expanded Floating Chat Window (Messenger OS Style)
    if (bubbleState.isExpanded) {
        ExpandedMessengerChatDialog(
            bubbleState = bubbleState,
            viewModel = viewModel,
            onMinimize = { MessengerBubbleManager.minimizeBubble() },
            onClose = { MessengerBubbleManager.removeBubble(bubbleState.activeChatId) },
            onDismissAll = { MessengerBubbleManager.dismissBubble() },
            onSwitchChat = { targetId -> MessengerBubbleManager.switchActiveChat(targetId) },
            onOpenFullChat = {
                val activeId = bubbleState.activeChatId
                MessengerBubbleManager.minimizeBubble()
                onNavigateToFullChat(activeId)
            }
        )
    }
}

/**
 * Messenger preview sneak-peek tooltip attached to the floating head.
 */
@Composable
private fun BubbleSneakPeekCard(
    contactName: String,
    messageText: String,
    accountType: BubbleAccountType,
    isOnline: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .widthIn(min = 190.dp, max = 240.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ),
        border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
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
                            .background(if (isOnline) Color(0xFF10B981) else Color(0xFF9CA3AF))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = contactName,
                        color = GoldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (accountType == BubbleAccountType.PERSONAL) Icons.Default.Person else Icons.Default.Work,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إخفاء",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = messageText,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onClick,
                    shape = CircleShape,
                    color = GoldPrimary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "فتح",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Floating Multi-Conversation Dialog Window with interactive real-time messaging,
 * account switching (Personal vs Business), Notification Center, in-bubble Settings,
 * and quick accounting ledger.
 */
@Composable
private fun ExpandedMessengerChatDialog(
    bubbleState: MessengerBubbleState,
    viewModel: MainViewModel,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onDismissAll: () -> Unit,
    onSwitchChat: (String) -> Unit,
    onOpenFullChat: () -> Unit
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    val activeChatId = bubbleState.activeChatId
    val messages by viewModel.getRecentMessagesForChat(activeChatId, limit = 30).collectAsStateWithLifecycle(initialValue = emptyList())
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val currentConv = bubbleState.activeConversation

    // Auto-scroll to latest message on new items
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onMinimize,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.82f)
                .testTag("messenger_bubble_expanded_window"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
            ),
            border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.55f)),
            elevation = CardDefaults.cardElevation(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Top Multi-Conversation Tab Carousel
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(bubbleState.getFilteredConversations(), key = { it.chatId }) { conv ->
                                val isSelected = (conv.chatId == activeChatId)
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) GoldPrimary.copy(alpha = 0.25f) else Color.Transparent,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.2.dp else 0.8.dp,
                                        color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.clickable {
                                        onSwitchChat(conv.chatId)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) GoldPrimary else GoldDark),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = conv.contactName.take(1).ifBlank { "O" },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) adaptiveOnPrimary else GoldLight
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (conv.isOnline) StatusGreen else Color(0xFF9CA3AF))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = conv.contactName,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) GoldLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        if (conv.unreadCount > 0 && !isSelected) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(StatusRed)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Switch bubble account filter button
                        IconButton(
                            onClick = {
                                val nextFilter = if (bubbleState.accountFilter == BubbleAccountType.BUSINESS) {
                                    BubbleAccountType.PERSONAL
                                } else {
                                    BubbleAccountType.BUSINESS
                                }
                                MessengerBubbleManager.setAccountFilter(context, nextFilter)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "تبديل نوع حساب الفقاعة",
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                // 2. Messenger Header Bar (Contact info, Account switcher, Actions)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Contact Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary.copy(alpha = 0.2f))
                                    .border(1.2.dp, GoldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = bubbleState.contactName.trim().take(1).ifBlank { "O" },
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(if (bubbleState.isOnline) StatusGreen else Color(0xFF9CA3AF))
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = bubbleState.contactName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (bubbleState.isOnline) StatusGreen else Color(0xFF9CA3AF))
                                        .clickable {
                                            MessengerBubbleManager.toggleOnlineStatus(bubbleState.chatId)
                                        }
                                )
                            }
                            Text(
                                text = "${bubbleState.contactRole} • ${currentConv?.lastSeenText ?: "نشط الآن"}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // Action Controls (Full Chat, Float Outside, Minimize, Close)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOpenFullChat,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = "فتح المحادثة كاملة",
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Float outside the app (Messenger system overlay)
                        IconButton(
                            onClick = {
                                if (MessengerBubbleManager.canDrawOverlays(context)) {
                                    MessengerBubbleManager.minimizeBubble()
                                    MessengerBubbleManager.startFloatingService(context)
                                } else {
                                    MessengerBubbleManager.setShowPermissionDialog(true)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "تشغيل كفقاعة عائمة خارج التطبيق",
                                tint = GoldLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onMinimize,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "تصغير إلى فقاعة",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق هذه الفقاعة",
                                tint = StatusRed,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                // 3. Dual Account Switcher Pill (Personal 👤 vs Work/Business 💼)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Work / Business mode pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (bubbleState.accountFilter == BubbleAccountType.BUSINESS) GoldPrimary else Color.Transparent,
                            modifier = Modifier.clickable {
                                MessengerBubbleManager.setAccountFilter(context, BubbleAccountType.BUSINESS)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = null,
                                    tint = if (bubbleState.accountFilter == BubbleAccountType.BUSINESS) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "أعمال",
                                    fontSize = 11.sp,
                                    fontWeight = if (bubbleState.accountFilter == BubbleAccountType.BUSINESS) FontWeight.Bold else FontWeight.Normal,
                                    color = if (bubbleState.accountFilter == BubbleAccountType.BUSINESS) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Personal mode pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (bubbleState.accountFilter == BubbleAccountType.PERSONAL) Color(0xFF3B82F6) else Color.Transparent,
                            modifier = Modifier.clickable {
                                MessengerBubbleManager.setAccountFilter(context, BubbleAccountType.PERSONAL)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (bubbleState.accountFilter == BubbleAccountType.PERSONAL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "شخصي",
                                    fontSize = 11.sp,
                                    fontWeight = if (bubbleState.accountFilter == BubbleAccountType.PERSONAL) FontWeight.Bold else FontWeight.Normal,
                                    color = if (bubbleState.accountFilter == BubbleAccountType.PERSONAL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // In-bubble Tab Navigation Selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        BubbleActiveTab.values().forEach { tab ->
                            val isSelectedTab = (bubbleState.activeTab == tab)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelectedTab) GoldPrimary.copy(alpha = 0.18f) else Color.Transparent,
                                modifier = Modifier.clickable {
                                    MessengerBubbleManager.setActiveTab(tab)
                                }
                            ) {
                                Icon(
                                    imageVector = when (tab) {
                                        BubbleActiveTab.CHAT -> Icons.Default.ChatBubbleOutline
                                        BubbleActiveTab.NOTIFICATIONS -> Icons.Default.NotificationsNone
                                        BubbleActiveTab.ACCOUNT_INFO -> Icons.Default.AccountBalanceWallet
                                        BubbleActiveTab.SETTINGS -> Icons.Default.Settings
                                    },
                                    contentDescription = tab.labelAr,
                                    tint = if (isSelectedTab) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(horizontal = 7.dp, vertical = 5.dp)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // 4. Main Body Content Based on Active Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (bubbleState.activeTab) {
                        BubbleActiveTab.CHAT -> {
                            ChatTabBody(
                                messages = messages,
                                currentUserId = currentUser?.id,
                                contactName = bubbleState.contactName,
                                listState = listState
                            )
                        }
                        BubbleActiveTab.NOTIFICATIONS -> {
                            NotificationsTabBody(
                                notifications = bubbleState.notifications,
                                onMarkRead = { id -> MessengerBubbleManager.markNotificationRead(id) }
                            )
                        }
                        BubbleActiveTab.ACCOUNT_INFO -> {
                            AccountInfoTabBody(
                                conversation = currentConv,
                                onOpenFullChat = onOpenFullChat
                            )
                        }
                        BubbleActiveTab.SETTINGS -> {
                            SettingsTabBody(
                                bubbleState = bubbleState,
                                context = context,
                                onDismissAll = onDismissAll
                            )
                        }
                    }
                }

                // 5. Bottom Interactive Controls (Quick Replies & Message Input)
                if (bubbleState.activeTab == BubbleActiveTab.CHAT) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    // Quick Response Suggestions Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val replies = if (bubbleState.accountFilter == BubbleAccountType.PERSONAL) {
                            QuickBusinessReplies.personalReplies
                        } else {
                            QuickBusinessReplies.defaultReplies
                        }
                        items(replies) { chipText ->
                            SuggestionChip(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.sendMessage(activeChatId, chipText)
                                },
                                label = { Text(chipText, fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    labelColor = GoldLight
                                ),
                                border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Message Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.sendMessage(
                                    chatId = activeChatId,
                                    content = "رسالة صوتية مسجلة من الفقاعة العائمة (0:08)",
                                    type = MessageType.VOICE.name
                                )
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.15f))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "تسجيل صوتي",
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("messenger_bubble_input_field"),
                            placeholder = {
                                Text(
                                    "رسالة...",
                                    fontSize = 12.5.sp
                                )
                            },
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                val trimmed = textInput.trim()
                                if (trimmed.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.sendMessage(activeChatId, trimmed)
                                    textInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(adaptiveGoldPrimary)
                                .testTag("messenger_bubble_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال",
                                tint = adaptiveOnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chat tab messages body.
 */
@Composable
private fun ChatTabBody(
    messages: List<MessageEntity>,
    currentUserId: String?,
    contactName: String,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
    ) {
        if (messages.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = GoldPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "محادثة فورية نشطة",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "تواصل مباشرة مع $contactName دون الحاجة للخروج من شاشتك",
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMe = (msg.senderId == currentUserId)
                    MessengerBubbleChatItem(message = msg, isMe = isMe)
                }
            }
        }
    }
}

/**
 * Notifications tab body inside the floating bubble.
 */
@Composable
private fun NotificationsTabBody(
    notifications: List<BubbleNotificationItem>,
    onMarkRead: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "التنبيهات والإشعارات",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(notifications, key = { it.id }) { notif ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else Color(0xFF1E242B)
                ),
                border = BorderStroke(1.dp, if (notif.isRead) Color.Transparent else GoldPrimary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when (notif.type) {
                                    "DEAL" -> StatusGreen.copy(alpha = 0.15f)
                                    "GOLD_RATE" -> GoldPrimary.copy(alpha = 0.15f)
                                    "INVOICE" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                    else -> Color.White.copy(alpha = 0.1f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (notif.type) {
                                "DEAL" -> Icons.Default.Handshake
                                "GOLD_RATE" -> Icons.Default.TrendingUp
                                "INVOICE" -> Icons.Default.ReceiptLong
                                else -> Icons.Default.Notifications
                            },
                            contentDescription = null,
                            tint = when (notif.type) {
                                "DEAL" -> StatusGreen
                                "GOLD_RATE" -> GoldPrimary
                                "INVOICE" -> Color(0xFF3B82F6)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = notif.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (notif.isRead) MaterialTheme.colorScheme.onSurface else GoldLight
                            )
                            Text(
                                text = notif.timeAgo,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = notif.body,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Account & Ledger info tab body inside the floating bubble.
 */
@Composable
private fun AccountInfoTabBody(
    conversation: BubbleConversation?,
    onOpenFullChat: () -> Unit
) {
    if (conversation == null) return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = conversation.contactName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                        Text(text = conversation.contactRole, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusGreen.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, StatusGreen.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "حساب موثق",
                            fontSize = 10.sp,
                            color = StatusGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Financial Overview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "الرصيد المالي", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = conversation.cashBalance, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                    }
                    Column {
                        Text(text = "رصيد الذهب", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = conversation.goldBalance, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    }
                    Column {
                        Text(text = "الهاتف", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = conversation.phoneNumber, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Button(
            onClick = onOpenFullChat,
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "كشف الحساب والفواتير", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
        }
    }
}

/**
 * Settings tab body inside the floating bubble.
 */
@Composable
private fun SettingsTabBody(
    bubbleState: MessengerBubbleState,
    context: android.content.Context,
    onDismissAll: () -> Unit
) {
    val hasOverlay = MessengerBubbleManager.canDrawOverlays(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "إعدادات الفقاعة العائمة",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary
        )

        // Notification Sound Icon Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (bubbleState.soundEnabled) GoldPrimary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (bubbleState.soundEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = if (bubbleState.soundEnabled) GoldPrimary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "التنبيهات",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = { MessengerBubbleManager.setSoundEnabled(context, !bubbleState.soundEnabled) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (bubbleState.soundEnabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = "تبديل",
                        tint = if (bubbleState.soundEnabled) StatusGreen else Color.Gray,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Auto-expand on new message Icon Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (bubbleState.autoExpandOnNewMessage) GoldPrimary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (bubbleState.autoExpandOnNewMessage) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (bubbleState.autoExpandOnNewMessage) GoldPrimary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "الفتح التلقائي",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = { MessengerBubbleManager.setAutoExpandOnNewMessage(context, !bubbleState.autoExpandOnNewMessage) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (bubbleState.autoExpandOnNewMessage) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = "تبديل",
                        tint = if (bubbleState.autoExpandOnNewMessage) StatusGreen else Color.Gray,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // System Overlay Permission Icon Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (hasOverlay) StatusGreen.copy(alpha = 0.18f) else Color.Red.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = if (hasOverlay) StatusGreen else StatusRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "الظهور خارج التطبيق",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = {
                        if (!hasOverlay) {
                            MessengerBubbleManager.requestPermissionWithAutoLaunch(context)
                        }
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (hasOverlay) Icons.Default.CheckCircle else Icons.Default.Settings,
                        contentDescription = "إذن الظهور",
                        tint = if (hasOverlay) StatusGreen else GoldPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onDismissAll,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = StatusRed, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "إغلاق الكل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Message Bubble Item inside the floating Messenger Dialog.
 */
@Composable
private fun MessengerBubbleChatItem(
    message: MessageEntity,
    isMe: Boolean
) {
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormatter.format(Date(message.timestamp)) }
    val isHashtagEntry = remember(message.content) { message.content.contains("#") }
    val isAudio = remember(message.type) { message.type == MessageType.VOICE.name }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 270.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 16.dp
                ),
                color = when {
                    isHashtagEntry -> GoldDark.copy(alpha = 0.6f)
                    isMe -> adaptiveGoldPrimary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                border = if (isHashtagEntry) BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)) else null,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (!isMe) {
                        Text(
                            text = message.senderName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    if (isAudio) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = if (isMe) adaptiveOnPrimary else GoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = message.content,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isMe) adaptiveOnPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Text(
                            text = message.content,
                            fontSize = 13.5.sp,
                            color = when {
                                isHashtagEntry -> Color.White
                                isMe -> adaptiveOnPrimary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }

                    if (isHashtagEntry) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "قيد محاسبي مدقق آلياً",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (isMe) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "تم التسليم",
                        tint = GoldPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
