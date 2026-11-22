package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.MessageEntity
import com.example.data.model.MessageType
import com.example.data.model.ProjectTaskEntity
import com.example.data.model.UserEntity
import com.example.ui.components.MediaPreviewItem
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.ui.theme.adaptiveOnPrimary
import com.example.ui.viewmodel.TranslatedMessageUiState
import com.example.util.GoldUtils
import com.example.util.OfflineSyncManager
import com.example.util.TextToSpeechHelper
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun ColumnScope.ChatMessageStreamSection(
    chat: ChatEntity,
    messagesNewestFirst: List<MessageEntity>,
    firstUnreadIndex: Int,
    firstUnreadReversedIndex: Int,
    isInitialLoading: Boolean,
    isLoadingOlderMessages: Boolean,
    hasMoreOlderMessages: Boolean,
    onLoadOlderMessages: () -> Unit,
    listState: LazyListState,
    isImeOpen: Boolean,
    messageZoomScale: Float,
    animatedZoomScale: Float,
    showZoomPill: Boolean,
    onZoomChange: (Float) -> Unit,
    onResetZoom: () -> Unit,
    currentUser: UserEntity?,
    customFontColorStyle: String,
    customBubbleStyle: String,
    customFontColor: Color?,
    customBubbleColor: Color?,
    isAutoTranslateEnabled: Boolean,
    translateMyMessagesToo: Boolean,
    translationFontSize: String,
    translationColorStyle: String,
    translatedMessages: Map<String, TranslatedMessageUiState>,
    invoicesForChat: List<InvoiceEntity>,
    onQuickPayInvoice: ((InvoiceEntity) -> Unit)?,
    onHashtagClick: (String) -> Unit,
    onCodeClick: (String) -> Unit,
    onTranslateMessage: (MessageEntity) -> Unit,
    ttsHelper: TextToSpeechHelper,
    speakingMessageId: String?,
    onSpeakingMessageIdChange: (String?) -> Unit,
    onDismissTranslation: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onImageClick: (MediaPreviewItem) -> Unit,
    onOptionsClick: (MessageEntity) -> Unit,
    onReactionClick: (MessageEntity) -> Unit,
    onForwardClick: (MessageEntity) -> Unit,
    onReply: (MessageEntity) -> Unit,
    projectTasks: List<ProjectTaskEntity>,
    onWorkerSubmitTaskProof: (ProjectTaskEntity) -> Unit,
    onAdminVerifyTask: (ProjectTaskEntity) -> Unit,
    onMentionClick: (String) -> Unit,
    onSendMessage: (String, String) -> Unit,
    forwardSuccessMessage: String?,
    onDismissForwardSuccess: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val isNetworkOnline by OfflineSyncManager.isNetworkOnline.collectAsStateWithLifecycle()
    val isSyncing by OfflineSyncManager.isSyncing.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = !isNetworkOnline,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            color = Color(0xFFD97706).copy(alpha = 0.14f),
            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.35f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 3.dp)
                .testTag("offline_chat_cache_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "وضع عدم الاتصال — يُحفظ محلياً",
                        fontSize = 11.sp,
                        color = Color(0xFFD97706),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = { OfflineSyncManager.triggerImmediateSync(context) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text(
                        text = if (isSyncing) "جاري المزامنة..." else "مزامنة",
                        fontSize = 10.sp,
                        color = Color(0xFFD97706),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .pointerInput(messageZoomScale) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount >= 2) {
                            val zoomChange = event.calculateZoom()
                            if (zoomChange != 1f) {
                                onZoomChange(zoomChange)
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        val streamMaxWidth = maxWidth
        val streamMaxHeight = maxHeight

        val minBottomCushionDp = 12.dp
        val maxBottomCushionDp = (streamMaxHeight * 0.36f).coerceIn(135.dp, 280.dp)
        val defaultBottomCushionDp = (streamMaxHeight * 0.14f).coerceIn(58.dp, 108.dp)
        var userBottomCushionDp by rememberSaveable(chat.id) { mutableFloatStateOf(-1f) }
        val targetCushionDp = if (isImeOpen) {
            4.dp
        } else if (userBottomCushionDp < 0f) {
            defaultBottomCushionDp
        } else {
            userBottomCushionDp.dp.coerceIn(minBottomCushionDp, maxBottomCushionDp)
        }
        val effectiveBottomCushionDp by animateDpAsState(
            targetValue = targetCushionDp,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "chat_bottom_cushion"
        )

        val chatCushionNestedScroll = remember(isImeOpen, streamMaxHeight, density) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (isImeOpen || source != NestedScrollSource.UserInput) return Offset.Zero
                    if (available.y != 0f) {
                        val atVisualBottom = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 4
                        val deltaDp = with(density) { (-available.y).toDp().value }
                        val current = if (userBottomCushionDp < 0f) defaultBottomCushionDp.value else userBottomCushionDp
                        if (available.y < 0f || (available.y > 0f && atVisualBottom)) {
                            val next = (current + deltaDp).coerceIn(minBottomCushionDp.value, maxBottomCushionDp.value)
                            val appliedPx = with(density) { (next - current).dp.toPx() }
                            userBottomCushionDp = next
                            return Offset(0f, -appliedPx)
                        }
                    }
                    return Offset.Zero
                }
            }
        }

        when {
            messagesNewestFirst.isEmpty() && isInitialLoading -> {
                ChatMessagesLoadingSkeleton()
            }
            messagesNewestFirst.isEmpty() -> {
                ChatEmptyStateCard(
                    contactName = chat.contactName,
                    onSendMessage = onSendMessage
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(chatCushionNestedScroll),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    item(key = "chat_bottom_free_space") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(effectiveBottomCushionDp)
                                .then(
                                    if (!isImeOpen) {
                                        Modifier.pointerInput(streamMaxHeight) {
                                            detectVerticalDragGestures { _, dragAmount ->
                                                val current = if (userBottomCushionDp < 0f) {
                                                    defaultBottomCushionDp.value
                                                } else {
                                                    userBottomCushionDp
                                                }
                                                val deltaDp = with(density) { (-dragAmount).toDp().value }
                                                userBottomCushionDp = (current + deltaDp).coerceIn(
                                                    minBottomCushionDp.value,
                                                    maxBottomCushionDp.value
                                                )
                                            }
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .testTag("chat_bottom_free_space"),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (!isImeOpen && effectiveBottomCushionDp >= 24.dp) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .width(34.dp)
                                        .height(3.5.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.24f))
                                )
                            }
                        }
                    }

                    itemsIndexed(
                        items = messagesNewestFirst,
                        key = { _, msg: MessageEntity ->
                            msg.id.ifBlank { "${msg.timestamp}_${msg.senderId}" }
                        }
                    ) { revIndex, msg ->
                        val isMe = if (currentUser != null) {
                            msg.senderId == currentUser.id || msg.senderName == currentUser.name
                        } else {
                            msg.senderName != chat.contactName
                        }

                        val olderMsg = messagesNewestFirst.getOrNull(revIndex + 1)
                        val newerMsg = messagesNewestFirst.getOrNull(revIndex - 1)
                        val showHourHeader = olderMsg == null || !GoldUtils.isSameHour(olderMsg.timestamp, msg.timestamp)
                        val belowShowsHourHeader = newerMsg != null && !GoldUtils.isSameHour(msg.timestamp, newerMsg.timestamp)

                        val olderIsMe = olderMsg?.let { p ->
                            if (currentUser != null) p.senderId == currentUser.id || p.senderName == currentUser.name
                            else p.senderName != chat.contactName
                        }
                        val newerIsMe = newerMsg?.let { n ->
                            if (currentUser != null) n.senderId == currentUser.id || n.senderName == currentUser.name
                            else n.senderName != chat.contactName
                        }

                        val sameSenderAbove = olderMsg != null && !showHourHeader && olderIsMe == isMe && (isMe || olderMsg.senderName == msg.senderName)
                        val sameSenderBelow = newerMsg != null && !belowShowsHourHeader && newerIsMe == isMe && (isMe || newerMsg.senderName == msg.senderName)

                        val maxBubbleWidth = (streamMaxWidth * (if (isMe) 0.78f else 0.74f)).coerceAtLeast(180.dp)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = when {
                                        showHourHeader -> 5.dp
                                        sameSenderAbove -> 1.5.dp
                                        else -> 5.dp
                                    },
                                    bottom = if (sameSenderBelow) 0.dp else 1.dp
                                )
                        ) {
                            if (showHourHeader) {
                                ChatHourlyTimestampHeader(
                                    timestamp = msg.timestamp,
                                    zoomScale = animatedZoomScale,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp)
                                )
                            }

                            if (firstUnreadReversedIndex != -1 && revIndex == firstUnreadReversedIndex) {
                                UnreadMessagesDivider()
                            }

                            ChatBubble(
                                msg = msg,
                                isMe = isMe,
                                isFirstInGroup = !sameSenderAbove,
                                isLastInGroup = !sameSenderBelow,
                                zoomScale = animatedZoomScale,
                                maxBubbleWidth = maxBubbleWidth,
                                customFontColorStyle = customFontColorStyle,
                                customBubbleStyle = customBubbleStyle,
                                customFontColor = customFontColor,
                                customBubbleColor = customBubbleColor,
                                isAutoTranslateEnabled = isAutoTranslateEnabled,
                                translateMyMessagesToo = translateMyMessagesToo,
                                translationFontSize = translationFontSize,
                                translationColorStyle = translationColorStyle,
                                translation = translatedMessages[msg.id],
                                invoicesForChat = invoicesForChat,
                                onQuickPayInvoice = onQuickPayInvoice,
                                onHashtagClick = onHashtagClick,
                                onCodeClick = onCodeClick,
                                onTranslateClick = { onTranslateMessage(msg) },
                                onSpeakTranslation = { text, lang ->
                                    if (speakingMessageId == msg.id) {
                                        ttsHelper.stop()
                                        onSpeakingMessageIdChange(null)
                                    } else {
                                        ttsHelper.stop()
                                        onSpeakingMessageIdChange(msg.id)
                                        ttsHelper.speakText(
                                            text = text,
                                            languageCode = lang,
                                            onStart = { onSpeakingMessageIdChange(msg.id) },
                                            onDone = { onSpeakingMessageIdChange(null) }
                                        )
                                    }
                                },
                                onStopSpeaking = {
                                    ttsHelper.stop()
                                    onSpeakingMessageIdChange(null)
                                },
                                isSpeakingTranslation = (speakingMessageId == msg.id),
                                onDismissTranslation = { onDismissTranslation(msg.id) },
                                onAvatarClick = onAvatarClick,
                                onImageClick = onImageClick,
                                onOptionsClick = { onOptionsClick(msg) },
                                onLongPress = { onReactionClick(msg) },
                                onReactionClick = { onReactionClick(msg) },
                                onForwardClick = { onForwardClick(msg) },
                                onReply = { onReply(msg) },
                                projectTasks = projectTasks,
                                onWorkerSubmitTaskProof = onWorkerSubmitTaskProof,
                                onAdminVerifyTask = onAdminVerifyTask,
                                onMentionClick = onMentionClick
                            )
                        }
                    }

                    if (isLoadingOlderMessages || hasMoreOlderMessages) {
                        item(key = "chat_history_lazy_loader") {
                            LoadOlderMessagesFooterItem(
                                isLoadingOlderMessages = isLoadingOlderMessages,
                                onLoadOlderMessages = onLoadOlderMessages
                            )
                        }
                    }
                }

                val isScrolledUp = remember {
                    derivedStateOf { listState.firstVisibleItemIndex > 2 }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isScrolledUp.value,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 10.dp)
                ) {
                    Surface(
                        onClick = { scope.launch { listState.animateScrollToItem(0) } },
                        color = adaptiveGoldPrimary,
                        contentColor = adaptiveOnPrimary,
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 5.dp,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "التمرير للأسفل",
                                tint = adaptiveOnPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (firstUnreadIndex != -1) "رسائل جديدة" else "الأسفل",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = adaptiveOnPrimary
                            )
                        }
                    }
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showZoomPill || abs(messageZoomScale - 1f) > 0.03f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        ) {
            Surface(
                onClick = onResetZoom,
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
                shadowElevation = 4.dp,
                modifier = Modifier.testTag("chat_zoom_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${(animatedZoomScale * 100).roundToInt()}%",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (abs(messageZoomScale - 1f) > 0.03f) {
                        Text(
                            text = "إعادة ضبط",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldPrimary
                        )
                    }
                }
            }
        }
    }

    AnimatedVisibility(visible = forwardSuccessMessage != null) {
        Surface(
            color = StatusGreen.copy(alpha = 0.9f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(forwardSuccessMessage ?: "", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismissForwardSuccess, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatMessagesLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
    ) {
        val shimmerWidths = listOf(0.55f, 0.72f, 0.42f, 0.65f, 0.48f, 0.76f)
        shimmerWidths.forEachIndexed { idx, frac ->
            val alignEnd = idx % 2 == 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (alignEnd) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    },
                    modifier = Modifier
                        .fillMaxWidth(frac)
                        .height(if (idx % 3 == 0) 46.dp else 36.dp)
                ) {}
            }
        }
    }
}

@Composable
private fun ChatEmptyStateCard(
    contactName: String,
    onSendMessage: (String, String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(58.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = "بداية المحادثة مع $contactName",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "يمكنك إرسال رسائل أو توثيق صفقات الذهب وقيود #هاشتاج.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                SuggestionChip(
                    onClick = { onSendMessage("مرحباً، أود الاستفسار عن أسعار الذهب اليوم", MessageType.TEXT.name) },
                    label = { Text("استفسار عن الذهب", fontSize = 11.sp) }
                )
                SuggestionChip(
                    onClick = { onSendMessage("السلام عليكم، تم مراجعة الحسابات والقيود", MessageType.TEXT.name) },
                    label = { Text("مراجعة القيود", fontSize = 11.sp) }
                )
            }
        }
    }
}

@Composable
private fun UnreadMessagesDivider() {
    Surface(
        color = GoldPrimary.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MarkEmailUnread,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "رسائل غير مقروءة",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary
            )
        }
    }
}

@Composable
private fun LoadOlderMessagesFooterItem(
    isLoadingOlderMessages: Boolean,
    onLoadOlderMessages: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = { if (!isLoadingOlderMessages) onLoadOlderMessages() },
            enabled = !isLoadingOlderMessages,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.testTag("chat_load_older_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoadingOlderMessages) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 1.8.dp,
                        color = GoldPrimary
                    )
                    Text(
                        text = "جاري تحميل الرسائل الأقدم...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "تحميل الرسائل الأقدم",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
