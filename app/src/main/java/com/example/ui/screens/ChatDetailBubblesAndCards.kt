package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceStatus
import com.example.data.model.MessageEntity
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.ProjectTaskEntity
import com.example.ui.components.InlineChatVideoPlayer
import com.example.ui.components.MediaPreviewItem
import com.example.ui.components.OozoxAvatarImage
import com.example.ui.components.OozoxThumbnailImage
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusOrange
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.ui.theme.adaptiveOnPrimary
import com.example.ui.viewmodel.TranslatedMessageUiState
import com.example.util.AudioPlayerManager
import com.example.util.ContentSafetyEngine
import com.example.util.FileUtils
import com.example.util.GoldUtils
import com.example.util.HashtagUtils
import com.example.util.ShortCodeGenerator
import com.example.util.TranslationStyleHelper
import java.io.File
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Unified ChatBubble component with modern, space-efficient design
 * and full compatibility with zoom scaling.
 */
@Composable
fun ChatBubble(
    msg: MessageEntity,
    isMe: Boolean,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true,
    zoomScale: Float = 1f,
    maxBubbleWidth: Dp = 300.dp,
    customFontColorStyle: String = "auto",
    customBubbleStyle: String = "auto",
    customFontColor: Color? = null,
    customBubbleColor: Color? = null,
    isAutoTranslateEnabled: Boolean = true,
    translateMyMessagesToo: Boolean = false,
    translationFontSize: String = "medium",
    translationColorStyle: String = "gold",
    translation: TranslatedMessageUiState? = null,
    invoicesForChat: List<InvoiceEntity> = emptyList(),
    onQuickPayInvoice: ((InvoiceEntity) -> Unit)? = null,
    onHashtagClick: ((String) -> Unit)? = null,
    onCodeClick: ((String) -> Unit)? = null,
    onTranslateClick: () -> Unit = {},
    onSpeakTranslation: (text: String, lang: String) -> Unit = { _, _ -> },
    onStopSpeaking: () -> Unit = {},
    isSpeakingTranslation: Boolean = false,
    onDismissTranslation: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onImageClick: (MediaPreviewItem) -> Unit = {},
    onOptionsClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onReactionClick: () -> Unit = {},
    onForwardClick: () -> Unit = {},
    onReply: () -> Unit = {},
    projectTasks: List<ProjectTaskEntity> = emptyList(),
    onWorkerSubmitTaskProof: (ProjectTaskEntity) -> Unit = {},
    onAdminVerifyTask: (ProjectTaskEntity) -> Unit = {},
    onMentionClick: ((String) -> Unit)? = null
) {
    AdaptiveMessageBubble(
        msg = msg,
        isMe = isMe,
        isFirstInGroup = isFirstInGroup,
        isLastInGroup = isLastInGroup,
        zoomScale = zoomScale,
        maxBubbleWidth = maxBubbleWidth,
        customFontColorStyle = customFontColorStyle,
        customBubbleStyle = customBubbleStyle,
        customFontColor = customFontColor,
        customBubbleColor = customBubbleColor,
        isAutoTranslateEnabled = isAutoTranslateEnabled,
        translateMyMessagesToo = translateMyMessagesToo,
        translationFontSize = translationFontSize,
        translationColorStyle = translationColorStyle,
        translation = translation,
        invoicesForChat = invoicesForChat,
        onQuickPayInvoice = onQuickPayInvoice,
        onHashtagClick = onHashtagClick,
        onCodeClick = onCodeClick,
        onTranslateClick = onTranslateClick,
        onSpeakTranslation = onSpeakTranslation,
        onStopSpeaking = onStopSpeaking,
        isSpeakingTranslation = isSpeakingTranslation,
        onDismissTranslation = onDismissTranslation,
        onAvatarClick = onAvatarClick,
        onImageClick = onImageClick,
        onOptionsClick = onOptionsClick,
        onLongPress = onLongPress,
        onReactionClick = onReactionClick,
        onForwardClick = onForwardClick,
        onReply = onReply,
        projectTasks = projectTasks,
        onWorkerSubmitTaskProof = onWorkerSubmitTaskProof,
        onAdminVerifyTask = onAdminVerifyTask,
        onMentionClick = onMentionClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdaptiveMessageBubble(
    msg: MessageEntity,
    isMe: Boolean,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true,
    zoomScale: Float = 1f,
    maxBubbleWidth: Dp = 300.dp,
    customFontColorStyle: String = "auto",
    customBubbleStyle: String = "auto",
    customFontColor: Color? = null,
    customBubbleColor: Color? = null,
    isAutoTranslateEnabled: Boolean = true,
    translateMyMessagesToo: Boolean = false,
    translationFontSize: String = "medium",
    translationColorStyle: String = "gold",
    translation: TranslatedMessageUiState? = null,
    invoicesForChat: List<InvoiceEntity> = emptyList(),
    onQuickPayInvoice: ((InvoiceEntity) -> Unit)? = null,
    onHashtagClick: ((String) -> Unit)? = null,
    onCodeClick: ((String) -> Unit)? = null,
    onTranslateClick: () -> Unit = {},
    onSpeakTranslation: (text: String, lang: String) -> Unit = { _, _ -> },
    onStopSpeaking: () -> Unit = {},
    isSpeakingTranslation: Boolean = false,
    onDismissTranslation: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onImageClick: (MediaPreviewItem) -> Unit = {},
    onOptionsClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onReactionClick: () -> Unit = {},
    onForwardClick: () -> Unit = {},
    onReply: () -> Unit = {},
    projectTasks: List<ProjectTaskEntity> = emptyList(),
    onWorkerSubmitTaskProof: (ProjectTaskEntity) -> Unit = {},
    onAdminVerifyTask: (ProjectTaskEntity) -> Unit = {},
    onMentionClick: ((String) -> Unit)? = null
) {
    val clampedZoom = zoomScale.coerceIn(0.75f, 1.85f)
    val haptic = LocalHapticFeedback.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swipe_offset"
    )
    val density = LocalDensity.current
    val replyThresholdPx = with(density) { 56.dp.toPx() }

    LaunchedEffect(msg.id, msg.content, isAutoTranslateEnabled, translateMyMessagesToo, isMe) {
        val shouldTranslateThisMessage = !isMe || translateMyMessagesToo
        if (isAutoTranslateEnabled && shouldTranslateThisMessage && msg.type == MessageType.TEXT.name && msg.content.isNotBlank() && translation == null) {
            val hasArabic = msg.content.any { it in '\u0600'..'\u06FF' }
            val hasLatin = msg.content.any { it in 'a'..'z' || it in 'A'..'Z' }
            if (hasLatin && !hasArabic) {
                onTranslateClick()
            }
        }
    }

    val isGoldPurchase = msg.type == MessageType.GOLD_PURCHASE.name
    val isInvoice = msg.type == MessageType.INVOICE.name
    val isPayment = msg.type == MessageType.PAYMENT.name
    val isVoice = msg.type == MessageType.VOICE.name
    val isDocument = msg.type == MessageType.DOCUMENT.name
    val isMissedCall = msg.type == MessageType.MISSED_CALL.name
    val isCall = msg.type == MessageType.CALL.name
    val isFieldTask = msg.type == "FIELD_TASK" || msg.content.trimStart().startsWith("[FIELD_TASK:")
    val isImage = msg.type == MessageType.IMAGE.name ||
        ((msg.content.startsWith("content://") || msg.content.startsWith("file://")) && !isDocument && !msg.content.contains(".pdf") && !msg.content.contains(".doc") && !msg.content.contains(".xls") && !msg.content.contains(".zip"))

    val innerCorner = if (isFirstInGroup && isLastInGroup) 16.dp else 6.dp
    val bubbleShape = if (isMe) {
        RoundedCornerShape(
            topStart = 16.dp,
            bottomStart = 16.dp,
            topEnd = if (isFirstInGroup) 16.dp else innerCorner,
            bottomEnd = if (isLastInGroup) 5.dp else innerCorner
        )
    } else {
        RoundedCornerShape(
            topEnd = 16.dp,
            bottomEnd = 16.dp,
            topStart = if (isFirstInGroup) 16.dp else innerCorner,
            bottomStart = if (isLastInGroup) 5.dp else innerCorner
        )
    }

    val hPadding = (7.5f * (0.88f + 0.12f * clampedZoom)).dp
    val vPadding = (3.2f * (0.88f + 0.12f * clampedZoom)).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 2.dp,
                vertical = if (!msg.reaction.isNullOrBlank()) 4.dp else 0.dp
            ),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            if (isLastInGroup) {
                Box(modifier = Modifier.clickable { onAvatarClick() }) {
                    OozoxAvatarImage(
                        model = null,
                        name = msg.senderName,
                        size = 30.dp,
                        isTrader = isGoldPurchase,
                        contentDescription = msg.senderName
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(30.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(msg.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX.absoluteValue > replyThresholdPx) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onReply()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = offsetX + dragAmount
                            offsetX = if (isMe) {
                                newOffset.coerceIn(-140f, 40f)
                            } else {
                                newOffset.coerceIn(-40f, 140f)
                            }
                        }
                    )
                },
            contentAlignment = if (isMe) Alignment.BottomEnd else Alignment.BottomStart
        ) {
            Surface(
                shape = bubbleShape,
                color = when {
                    isMissedCall -> Color(0xFFEF4444).copy(alpha = 0.15f)
                    isGoldPurchase -> GoldDark
                    isInvoice -> MaterialTheme.colorScheme.surfaceVariant
                    isPayment -> Color(0xFF064E3B)
                    customBubbleColor != null -> customBubbleColor
                    customBubbleStyle == "gold_dark" -> GoldDark
                    customBubbleStyle == "slate" -> Color(0xFF1E293B)
                    customBubbleStyle == "emerald_glass" -> Color(0xFF064E3B)
                    customBubbleStyle == "violet" -> Color(0xFF2E1065)
                    isImage -> if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    isMe -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                border = BorderStroke(
                    width = 0.8.dp,
                    color = when {
                        isGoldPurchase -> GoldPrimary
                        isInvoice -> Color(0xFF3B82F6).copy(alpha = 0.4f)
                        isPayment -> StatusGreen.copy(alpha = 0.5f)
                        isImage -> GoldPrimary.copy(alpha = 0.35f)
                        isMissedCall -> Color(0xFFEF4444).copy(alpha = 0.4f)
                        isMe -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
                    }
                ),
                shadowElevation = if (isGoldPurchase || isInvoice || isImage) 3.dp else 0.5.dp,
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .combinedClickable(
                        onClick = { onOptionsClick() },
                        onLongClick = { onLongPress() }
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = hPadding, vertical = vPadding)) {
                    BubbleHeaderRow(
                        msg = msg,
                        isMe = isMe,
                        isFirstInGroup = isFirstInGroup,
                        isGoldPurchase = isGoldPurchase,
                        isInvoice = isInvoice,
                        isPayment = isPayment,
                        isDocument = isDocument,
                        clampedZoom = clampedZoom
                    )

                    val safetyVerdict = remember(msg.content, msg.id, isImage, isDocument) {
                        val mediaUri = if (isImage || isDocument) msg.content.substringBefore("||") else null
                        ContentSafetyEngine.evaluate(
                            text = msg.content,
                            mediaUri = mediaUri,
                            isVideo = isImage && FileUtils.isVideoUriString(msg.content)
                        )
                    }
                    var isRevealed by rememberSaveable(msg.id) { mutableStateOf(false) }

                    if (!safetyVerdict.isSafe && !isRevealed) {
                        BubbleSafetyBlockedCard(
                            reason = safetyVerdict.reason,
                            clampedZoom = clampedZoom,
                            onReveal = { isRevealed = true }
                        )
                    } else {
                        if (!safetyVerdict.isSafe && isRevealed) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF7F1D1D).copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 5.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "محتوى حساس تم كشفه",
                                        fontSize = (10f * clampedZoom).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCA5A5)
                                    )
                                    Text(
                                        text = "إعادة الحجب",
                                        fontSize = (10f * clampedZoom).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldLight,
                                        modifier = Modifier.clickable { isRevealed = false }
                                    )
                                }
                            }
                        }

                        BubbleBodyContent(
                            msg = msg,
                            isMe = isMe,
                            isDocument = isDocument,
                            isImage = isImage,
                            isVoice = isVoice,
                            isMissedCall = isMissedCall,
                            isCall = isCall,
                            isFieldTask = isFieldTask,
                            isGoldPurchase = isGoldPurchase,
                            isPayment = isPayment,
                            clampedZoom = clampedZoom,
                            maxBubbleWidth = maxBubbleWidth,
                            customFontColorStyle = customFontColorStyle,
                            customFontColor = customFontColor,
                            onImageClick = onImageClick,
                            onHashtagClick = onHashtagClick,
                            onCodeClick = onCodeClick,
                            onMentionClick = onMentionClick,
                            projectTasks = projectTasks,
                            onWorkerSubmitTaskProof = onWorkerSubmitTaskProof,
                            onAdminVerifyTask = onAdminVerifyTask
                        )
                    }

                    AnimatedVisibility(
                        visible = translation != null,
                        enter = fadeIn(animationSpec = tween(durationMillis = 350, easing = EaseOutCubic)) +
                            expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 200, easing = EaseInCubic)) +
                            shrinkVertically(animationSpec = tween(durationMillis = 200))
                    ) {
                        if (translation != null) {
                            Column {
                                Spacer(modifier = Modifier.height(5.dp))
                                ContextualTranslationBubbleCard(
                                    translation = translation,
                                    translationFontSize = translationFontSize,
                                    translationColorStyle = translationColorStyle,
                                    zoomScale = clampedZoom,
                                    isSpeaking = isSpeakingTranslation,
                                    onSpeak = onSpeakTranslation,
                                    onStopSpeak = onStopSpeaking,
                                    onDismiss = onDismissTranslation,
                                    isMe = isMe
                                )
                            }
                        }
                    }

                    BubbleLinkedInvoiceFooter(
                        msg = msg,
                        isInvoice = isInvoice,
                        isPayment = isPayment,
                        isGoldPurchase = isGoldPurchase,
                        clampedZoom = clampedZoom,
                        invoicesForChat = invoicesForChat,
                        onQuickPayInvoice = onQuickPayInvoice
                    )

                    BubbleReadReceiptFooter(
                        msg = msg,
                        isMe = isMe,
                        isGoldPurchase = isGoldPurchase,
                        clampedZoom = clampedZoom,
                        onTranslateClick = onTranslateClick,
                        onReply = onReply,
                        onForwardClick = onForwardClick
                    )
                }
            }

            if (!msg.reaction.isNullOrBlank()) {
                Surface(
                    onClick = onReactionClick,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.2.dp, GoldPrimary),
                    shadowElevation = 4.dp,
                    modifier = Modifier.offset(
                        x = if (isMe) (-6).dp else 6.dp,
                        y = 10.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg.reaction,
                            fontSize = (12f * clampedZoom).sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleHeaderRow(
    msg: MessageEntity,
    isMe: Boolean,
    isFirstInGroup: Boolean,
    isGoldPurchase: Boolean,
    isInvoice: Boolean,
    isPayment: Boolean,
    isDocument: Boolean,
    clampedZoom: Float
) {
    val showSpecialBadge = isGoldPurchase || isInvoice || isPayment || isDocument
    val showSenderLabel = !isMe && isFirstInGroup
    if (!showSenderLabel && !showSpecialBadge) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 2.dp)
    ) {
        if (showSenderLabel) {
            Text(
                text = msg.senderName,
                fontSize = (11.5f * clampedZoom).sp,
                fontWeight = FontWeight.Bold,
                color = if (isGoldPurchase) GoldLight else GoldPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        if (showSpecialBadge) {
            val badgeText = when {
                isGoldPurchase -> "صفقة ذهب"
                isInvoice -> "فاتورة معتمدة"
                isPayment -> "إشعار سداد"
                isDocument -> "مستند"
                else -> ""
            }
            val badgeColor = when {
                isGoldPurchase -> GoldPrimary
                isInvoice -> Color(0xFF60A5FA)
                isPayment -> StatusGreen
                isDocument -> Color(0xFF60A5FA)
                else -> MaterialTheme.colorScheme.primary
            }
            if (badgeText.isNotEmpty()) {
                Text(
                    text = badgeText,
                    fontSize = (10.5f * clampedZoom).sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}

@Composable
private fun BubbleSafetyBlockedCard(
    reason: String,
    clampedZoom: Float,
    onReveal: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF7F1D1D).copy(alpha = 0.20f),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "درع الأمان",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "درع الأمان: تم حجب محتوى غير لائق",
                    fontWeight = FontWeight.Bold,
                    fontSize = (12f * clampedZoom).sp,
                    color = Color(0xFFFCA5A5)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = reason,
                fontSize = (11f * clampedZoom).sp,
                lineHeight = (15f * clampedZoom).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                onClick = onReveal,
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.25f),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "كشف المحتوى",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "كشف المحتوى على مسؤوليتي",
                        fontSize = (11f * clampedZoom).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun BubbleBodyContent(
    msg: MessageEntity,
    isMe: Boolean,
    isDocument: Boolean,
    isImage: Boolean,
    isVoice: Boolean,
    isMissedCall: Boolean,
    isCall: Boolean,
    isFieldTask: Boolean,
    isGoldPurchase: Boolean,
    isPayment: Boolean,
    clampedZoom: Float,
    maxBubbleWidth: Dp,
    customFontColorStyle: String,
    customFontColor: Color?,
    onImageClick: (MediaPreviewItem) -> Unit,
    onHashtagClick: ((String) -> Unit)?,
    onCodeClick: ((String) -> Unit)?,
    onMentionClick: ((String) -> Unit)?,
    projectTasks: List<ProjectTaskEntity>,
    onWorkerSubmitTaskProof: (ProjectTaskEntity) -> Unit,
    onAdminVerifyTask: (ProjectTaskEntity) -> Unit
) {
    when {
        isDocument -> {
            val parts = msg.content.split("||")
            val fileUri = parts.getOrNull(0) ?: ""
            val fileName = parts.getOrNull(1) ?: "مستند_مرفق"
            val fileSize = parts.getOrNull(2)?.toLongOrNull() ?: 0L
            val caption = if (parts.size > 3) parts[3] else ""

            DocumentMessageItem(
                fileUri = fileUri,
                fileName = fileName,
                fileSize = fileSize,
                caption = caption,
                isMe = isMe,
                zoomScale = clampedZoom
            )
        }
        isImage -> {
            val imageParts = msg.content.split("||", limit = 2)
            val imageUri = imageParts[0].trim()
            val caption = if (imageParts.size > 1) imageParts[1].trim() else ""
            val isVideo = FileUtils.isVideoUriString(imageUri)

            val previewItem = MediaPreviewItem(
                uri = imageUri,
                caption = caption,
                senderName = msg.senderName,
                timestamp = msg.timestamp,
                isVideo = isVideo
            )
            val thumbWidth = (228f * clampedZoom.coerceAtMost(1.35f)).dp.coerceAtMost((maxBubbleWidth - 22.dp).coerceAtLeast(140.dp))
            val thumbHeight = (182f * clampedZoom.coerceAtMost(1.35f)).dp

            Column(
                modifier = Modifier.padding(vertical = 2.dp),
                horizontalAlignment = Alignment.Start
            ) {
                if (isVideo) {
                    InlineChatVideoPlayer(
                        videoUri = imageUri,
                        width = thumbWidth,
                        height = thumbHeight,
                        shape = RoundedCornerShape(12.dp),
                        onExpandFullscreen = { onImageClick(previewItem) }
                    )
                } else {
                    OozoxThumbnailImage(
                        model = imageUri,
                        width = thumbWidth,
                        height = thumbHeight,
                        shape = RoundedCornerShape(12.dp),
                        contentScale = ContentScale.Fit,
                        contentDescription = "صورة المحادثة",
                        onClick = { onImageClick(previewItem) }
                    )
                }

                if (caption.isNotBlank()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = caption,
                        fontSize = (13.5f * clampedZoom).sp,
                        lineHeight = (18.5f * clampedZoom).sp,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        isVoice -> {
            VoiceMessagePlayer(
                content = msg.content,
                isMe = isMe,
                zoomScale = clampedZoom
            )
        }
        isMissedCall || isCall -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 3.dp, horizontal = 2.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isMissedCall) Color(0xFFEF4444).copy(alpha = 0.15f) else GoldPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isMissedCall) Icons.Default.PhoneMissed else Icons.Default.Call,
                            contentDescription = null,
                            tint = if (isMissedCall) Color(0xFFEF4444) else GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = if (isMissedCall) "مكالمة فائتة" else "مكالمة صوتية",
                        fontWeight = FontWeight.Bold,
                        fontSize = (14f * clampedZoom).sp,
                        color = if (isMissedCall) Color(0xFFEF4444) else if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = GoldUtils.formatCallExactDateTime(msg.timestamp),
                        fontSize = (11.5f * clampedZoom).sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        else -> {
            val textColor = when {
                isGoldPurchase -> GoldLight
                isPayment -> Color(0xFFD1FAE5)
                customFontColor != null -> customFontColor
                customFontColorStyle == "gold" -> GoldLight
                customFontColorStyle == "white" -> Color.White
                customFontColorStyle == "emerald" -> Color(0xFF10B981)
                customFontColorStyle == "platinum" -> Color(0xFFE2E8F0)
                isMe -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val uriHandler = LocalUriHandler.current
            val hasRepliedQuote = msg.content.startsWith("↩️ رد على ") && msg.content.contains("\n\n")
            val bodyToRender = if (hasRepliedQuote) msg.content.substringAfter("\n\n") else msg.content

            if (hasRepliedQuote) {
                val quoteHeader = msg.content.substringBefore("\n\n")
                Surface(
                    color = if (isMe) MaterialTheme.colorScheme.surface.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(24.dp)
                                .background(GoldPrimary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = quoteHeader,
                            fontSize = (11f * clampedZoom).sp,
                            lineHeight = (15f * clampedZoom).sp,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (isFieldTask) {
                BubbleFieldTaskCard(
                    msg = msg,
                    clampedZoom = clampedZoom,
                    projectTasks = projectTasks,
                    onHashtagClick = onHashtagClick,
                    onCodeClick = onCodeClick,
                    onMentionClick = onMentionClick,
                    onWorkerSubmitTaskProof = onWorkerSubmitTaskProof,
                    onAdminVerifyTask = onAdminVerifyTask
                )
            } else {
                RichTextMessageText(
                    text = bodyToRender,
                    textColor = textColor,
                    fontSize = (14.5f * clampedZoom).sp,
                    lineHeight = (20f * clampedZoom).sp,
                    onHashtagClick = { tag -> onHashtagClick?.invoke(tag) },
                    onCodeClick = { code -> onCodeClick?.invoke(code) },
                    onMentionClick = onMentionClick,
                    onLinkClick = { linkUrl ->
                        val isYt = linkUrl.contains("youtube.com") || linkUrl.contains("youtu.be")
                        val isVidLink = isYt || FileUtils.isVideoUriString(linkUrl)
                        val isImgLink = FileUtils.isImageNameOrMime(linkUrl.substringBefore('?'), "")
                        if (isVidLink || isImgLink) {
                            onImageClick(
                                MediaPreviewItem(
                                    uri = linkUrl,
                                    caption = "رابط مرفق في المحادثة",
                                    senderName = msg.senderName,
                                    timestamp = msg.timestamp,
                                    isVideo = isVidLink
                                )
                            )
                        } else {
                            try { uriHandler.openUri(linkUrl) } catch (_: Exception) {}
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BubbleFieldTaskCard(
    msg: MessageEntity,
    clampedZoom: Float,
    projectTasks: List<ProjectTaskEntity>,
    onHashtagClick: ((String) -> Unit)?,
    onCodeClick: ((String) -> Unit)?,
    onMentionClick: ((String) -> Unit)?,
    onWorkerSubmitTaskProof: (ProjectTaskEntity) -> Unit,
    onAdminVerifyTask: (ProjectTaskEntity) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val taskTokenMatch = Regex("^\\[FIELD_TASK:([^\\]\\n]+)\\]\\s*").find(msg.content.trim())
    val linkedTaskId = taskTokenMatch?.groupValues?.getOrNull(1)?.trim()
    val linkedTask = remember(linkedTaskId, projectTasks) {
        if (linkedTaskId.isNullOrBlank()) null
        else projectTasks.firstOrNull { it.id == linkedTaskId }
    }
    val displayTaskBody = if (taskTokenMatch != null) {
        msg.content.trim().removePrefix(taskTokenMatch.value).trim()
    } else {
        msg.content
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(
            1.dp,
            if (linkedTask?.isArchived == true) StatusGreen.copy(alpha = 0.55f) else GoldPrimary.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .testTag("chat_field_task_card_${linkedTaskId ?: msg.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentInd,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = linkedTask?.title ?: "مهمة عمل",
                        fontSize = (13f * clampedZoom).sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        maxLines = 2
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        linkedTask?.isArchived == true -> StatusGreen.copy(alpha = 0.16f)
                        linkedTask?.isSubmittedForReview == true -> Color(0xFF3B82F6).copy(alpha = 0.16f)
                        else -> Color(0xFFF59E0B).copy(alpha = 0.16f)
                    }
                ) {
                    Text(
                        text = when {
                            linkedTask?.isArchived == true -> "معتمدة"
                            linkedTask?.isSubmittedForReview == true -> "بانتظار الاعتماد"
                            else -> linkedTask?.deadlineDurationText ?: "قيد التنفيذ"
                        },
                        fontSize = (10f * clampedZoom).sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            linkedTask?.isArchived == true -> StatusGreen
                            linkedTask?.isSubmittedForReview == true -> Color(0xFF3B82F6)
                            else -> Color(0xFFF59E0B)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (displayTaskBody.isNotBlank()) {
                RichTextMessageText(
                    text = displayTaskBody,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    fontSize = (12.5f * clampedZoom).sp,
                    lineHeight = (18f * clampedZoom).sp,
                    onHashtagClick = onHashtagClick,
                    onCodeClick = onCodeClick,
                    onMentionClick = onMentionClick,
                    onLinkClick = { linkUrl ->
                        try { uriHandler.openUri(linkUrl) } catch (_: Exception) {}
                    }
                )
            }

            val assigneesText = linkedTask?.assignedMembersList?.ifBlank { linkedTask.assignedToName.orEmpty() }.orEmpty()
            if (assigneesText.isNotBlank() && !displayTaskBody.contains(assigneesText)) {
                RichTextMessageText(
                    text = "المكلف: $assigneesText",
                    textColor = Color(0xFF38BDF8),
                    fontSize = (11.5f * clampedZoom).sp,
                    lineHeight = (16f * clampedZoom).sp,
                    onMentionClick = onMentionClick,
                    onLinkClick = {}
                )
            }

            if (linkedTask != null && !linkedTask.isArchived) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!linkedTask.isSubmittedForReview) {
                        OutlinedButton(
                            onClick = { onWorkerSubmitTaskProof(linkedTask) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("chat_task_proof_btn_${linkedTask.id}")
                        ) {
                            Text("توثيق الإنجاز", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { onAdminVerifyTask(linkedTask) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("chat_task_verify_btn_${linkedTask.id}")
                    ) {
                        Text("اعتماد وأرشفة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BubbleLinkedInvoiceFooter(
    msg: MessageEntity,
    isInvoice: Boolean,
    isPayment: Boolean,
    isGoldPurchase: Boolean,
    clampedZoom: Float,
    invoicesForChat: List<InvoiceEntity>,
    onQuickPayInvoice: ((InvoiceEntity) -> Unit)?
) {
    val isHashtagOrFinance = isInvoice || isPayment || isGoldPurchase ||
        msg.type == MessageType.HASHTAG.name || msg.content.contains("#")
    if (!isHashtagOrFinance || invoicesForChat.isEmpty()) return

    val parsedTag = remember(msg.content) { HashtagUtils.parseHashtag(msg.content) }
    val linkedInvoice = remember(invoicesForChat, parsedTag, msg.content, msg.timestamp) {
        when {
            !parsedTag.isNullOrBlank() -> invoicesForChat.firstOrNull { inv ->
                inv.hashtagTag?.equals(parsedTag, ignoreCase = true) == true
            }
            isInvoice -> invoicesForChat.firstOrNull { inv ->
                inv.status != InvoiceStatus.PAID.name &&
                    (msg.content.contains(inv.invoiceNumber, ignoreCase = true) ||
                        msg.content.contains(inv.notes ?: "", ignoreCase = true))
            } ?: invoicesForChat.firstOrNull { it.status != InvoiceStatus.PAID.name }
            else -> null
        }
    } ?: return

    val remaining = (linkedInvoice.totalAmount - linkedInvoice.paidAmount).coerceAtLeast(0.0)
    val isFullyPaid = linkedInvoice.status == InvoiceStatus.PAID.name || remaining <= 0.01
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isFullyPaid) StatusGreen.copy(alpha = 0.14f) else StatusOrange.copy(alpha = 0.14f)
        ) {
            Text(
                text = if (isFullyPaid) "فاتورة مسددة" else "متبقي ${GoldUtils.formatCurrency(remaining)}",
                fontSize = (10f * clampedZoom).sp,
                fontWeight = FontWeight.Bold,
                color = if (isFullyPaid) StatusGreen else StatusOrange,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
        if (!isFullyPaid && onQuickPayInvoice != null) {
            Surface(
                onClick = { onQuickPayInvoice(linkedInvoice) },
                shape = RoundedCornerShape(8.dp),
                color = GoldPrimary.copy(alpha = 0.18f),
                border = BorderStroke(0.7.dp, GoldPrimary.copy(alpha = 0.45f))
            ) {
                Text(
                    text = "تسجيل سداد",
                    fontSize = (10f * clampedZoom).sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.BubbleReadReceiptFooter(
    msg: MessageEntity,
    isMe: Boolean,
    isGoldPurchase: Boolean,
    clampedZoom: Float,
    onTranslateClick: () -> Unit,
    onReply: () -> Unit,
    onForwardClick: () -> Unit
) {
    var showMessageDetails by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .align(Alignment.End)
            .clickable { showMessageDetails = !showMessageDetails }
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = GoldUtils.formatShortTime(msg.timestamp),
            fontSize = (9.5f * clampedZoom).sp,
            color = when {
                isGoldPurchase -> GoldLight.copy(alpha = 0.7f)
                isMe -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
            }
        )
        if (isMe) {
            when (msg.status) {
                MessageStatus.READ.name -> {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "تمت القراءة",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier
                            .size((14f * (0.85f + 0.15f * clampedZoom)).dp)
                            .testTag("read_receipt_read_${msg.id}")
                    )
                }
                MessageStatus.DELIVERED.name -> {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "تم التسليم",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier
                            .size((14f * (0.85f + 0.15f * clampedZoom)).dp)
                            .testTag("read_receipt_delivered_${msg.id}")
                    )
                }
                MessageStatus.PENDING.name -> {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "محفوظ محلياً في Room (في انتظار الاتصال)",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier
                            .size((12.5f * (0.85f + 0.15f * clampedZoom)).dp)
                            .testTag("read_receipt_pending_${msg.id}")
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "تم الإرسال",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size((13f * (0.85f + 0.15f * clampedZoom)).dp)
                            .testTag("read_receipt_sent_${msg.id}")
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = showMessageDetails,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val statusLabel = when (msg.status) {
                        MessageStatus.READ.name -> "تمت القراءة من المستلم"
                        MessageStatus.DELIVERED.name -> "تم التسليم بنجاح"
                        MessageStatus.PENDING.name -> "محفوظ محلياً في Room"
                        else -> "تم الإرسال عبر الخادم"
                    }
                    val statusColor = when (msg.status) {
                        MessageStatus.READ.name -> GoldPrimary
                        MessageStatus.DELIVERED.name -> StatusGreen
                        MessageStatus.PENDING.name -> Color(0xFFF59E0B)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = statusLabel,
                        fontSize = (9.5f * clampedZoom).sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = GoldUtils.formatExactDateTime(msg.timestamp),
                        fontSize = (9f * clampedZoom).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (msg.type == MessageType.TEXT.name && msg.content.isNotBlank()) {
                        Text(
                            text = "ترجمة",
                            fontSize = (10f * clampedZoom).sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary,
                            modifier = Modifier.clickable { onTranslateClick() }
                        )
                    }
                    Text(
                        text = "رد",
                        fontSize = (10f * clampedZoom).sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        modifier = Modifier.clickable { onReply() }
                    )
                    Text(
                        text = "توجيه",
                        fontSize = (10f * clampedZoom).sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        modifier = Modifier.clickable { onForwardClick() }
                    )
                }
            }
        }
    }
}

@Composable
fun MessageReactionDialog(
    message: MessageEntity,
    onDismiss: () -> Unit,
    onReactionSelect: (String) -> Unit,
    onReply: () -> Unit = {},
    onForward: () -> Unit,
    onCopy: () -> Unit,
    onTranslate: () -> Unit = {}
) {
    val emojis = listOf("معتمد", "تم الاطلاع", "موافق", "قيد المراجعة")
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)), RoundedCornerShape(20.dp))
                    .padding(20.dp)
                    .clickable(enabled = false) {},
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "التفاعل مع الرسالة",
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.5f)),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        emojis.forEach { emoji ->
                            val isSelected = message.reaction == emoji
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) adaptiveGoldPrimary.copy(alpha = 0.25f)
                                        else if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) adaptiveGoldPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onReactionSelect(emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (message.type == MessageType.VOICE.name) "تسجيل صوتي"
                            else if (message.type == MessageType.IMAGE.name) "صورة مرفقة"
                            else message.content.take(100),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onReply,
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = adaptiveOnPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رد", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onForward,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("توجيه", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onCopy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ", fontSize = 12.sp)
                    }
                }

                if (message.type == MessageType.TEXT.name && message.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onTranslate()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ترجمة سياقية",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }
                }

                if (!message.reaction.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { onReactionSelect(message.reaction) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إزالة التفاعل (${message.reaction})", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Contextual Translation Bubble Card:
 * Clean, lightweight, contextual translation rendered below the message text.
 * Powered by Gemini AI context analysis and Text-To-Speech audio pronunciation.
 */
@Composable
fun ContextualTranslationBubbleCard(
    translation: TranslatedMessageUiState,
    translationFontSize: String = "medium",
    translationColorStyle: String = "gold",
    zoomScale: Float = 1f,
    isSpeaking: Boolean,
    onSpeak: (String, String) -> Unit,
    onStopSpeak: () -> Unit,
    onDismiss: () -> Unit,
    isMe: Boolean
) {
    val clampedZoom = zoomScale.coerceIn(0.75f, 1.85f)
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val resolvedTextColor = TranslationStyleHelper.resolveColor(translationColorStyle, isDark)
    val resolvedFontSize = TranslationStyleHelper.resolveFontSize(translationFontSize) * clampedZoom
    val resolvedLineHeight = TranslationStyleHelper.resolveLineHeight(translationFontSize) * clampedZoom

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF1B2028).copy(alpha = 0.95f) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "ترجمة سياقية",
                        fontSize = (10.5f * clampedZoom).sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    val langLabel = if (translation.targetLang == "ar") "العربية" else "English"
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GoldPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Text(
                            text = langLabel,
                            fontSize = (9f * clampedZoom).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) GoldLight else GoldPrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (!translation.isLoading && translation.translatedText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isSpeaking) {
                                    onStopSpeak()
                                } else {
                                    onSpeak(translation.translatedText, translation.targetLang)
                                }
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "استماع صوتياً للترجمة",
                                tint = if (isSpeaking) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                clipboardManager.setText(AnnotatedString(translation.translatedText))
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "نسخ النص المترجم",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إخفاء الترجمة",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Crossfade(
                targetState = translation.isLoading,
                animationSpec = tween(durationMillis = 350, easing = EaseOutCubic),
                label = "translation_text_crossfade"
            ) { isLoading ->
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = GoldPrimary
                        )
                        Text(
                            text = "جارٍ استخراج الترجمة وفهم سياق المحادثة...",
                            fontSize = (11f * clampedZoom).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = translation.translatedText,
                        fontSize = resolvedFontSize.sp,
                        lineHeight = resolvedLineHeight.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = resolvedTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceMessagePlayer(
    content: String,
    isMe: Boolean,
    zoomScale: Float = 1f
) {
    val clampedZoom = zoomScale.coerceIn(0.75f, 1.85f)
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var durationSec by remember { mutableIntStateOf(10) }
    var currentSec by remember { mutableIntStateOf(0) }
    var audioPath by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf("") }

    LaunchedEffect(content) {
        if (content.contains("||")) {
            val parts = content.split("||")
            audioPath = parts.getOrNull(0)?.trim() ?: ""
            durationSec = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 10
            transcript = parts.getOrNull(2)?.trim() ?: ""
        } else {
            transcript = content
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isPlaying) {
                AudioPlayerManager.stop()
            }
        }
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    if (isPlaying) {
                        AudioPlayerManager.stop()
                        isPlaying = false
                    } else {
                        isPlaying = true
                        if (audioPath.isNotBlank() && File(audioPath).exists()) {
                            AudioPlayerManager.play(
                                context = context,
                                pathOrUri = audioPath,
                                onProgress = { cur, tot ->
                                    if (tot > 0) {
                                        currentProgress = cur.toFloat() / tot.toFloat()
                                        currentSec = cur / 1000
                                        durationSec = tot / 1000
                                    }
                                },
                                onComplete = {
                                    isPlaying = false
                                    currentProgress = 0f
                                    currentSec = 0
                                }
                            )
                        } else {
                            isPlaying = false
                            currentProgress = 0f
                            Toast.makeText(context, "الملف الصوتي غير متوفر على الجهاز", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) StatusOrange else adaptiveGoldPrimary)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل التسجيل الصوتي",
                    tint = if (isPlaying) Color.White else adaptiveOnPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(16) { index ->
                        val barHeight = (6 + ((index * 9) % 18)).dp
                        val isPassed = (index.toFloat() / 16f) <= currentProgress
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isPassed) GoldAccent else GoldPrimary.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format("%02d:%02d", currentSec / 60, currentSec % 60),
                        fontSize = (10f * clampedZoom).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format("%02d:%02d", durationSec / 60, durationSec % 60),
                        fontSize = (10f * clampedZoom).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (transcript.isNotBlank() && !transcript.startsWith("/")) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = transcript,
                fontSize = (11.5f * clampedZoom).sp,
                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = (15.5f * clampedZoom).sp
            )
        }
    }
}

@Composable
fun ForwardMessageDialog(
    messageToForward: MessageEntity,
    currentChatId: String,
    availableChats: List<ChatEntity>,
    onDismiss: () -> Unit,
    onForwardToChat: (targetChat: ChatEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredChats = remember(searchQuery, availableChats, currentChatId) {
        availableChats
            .filter { it.id != currentChatId }
            .filter {
                if (searchQuery.isBlank()) true
                else it.contactName.contains(searchQuery, ignoreCase = true) ||
                    it.contactType.contains(searchQuery, ignoreCase = true)
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        GoldPrimary.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "إعادة توجيه الرسالة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "اختر جهة الاتصال لإرسال الرسالة إليها",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "إلغاء",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (messageToForward.type) {
                                MessageType.VOICE.name -> Icons.Default.Mic
                                MessageType.IMAGE.name -> Icons.Default.Image
                                MessageType.GOLD_PURCHASE.name -> Icons.Default.MonetizationOn
                                else -> Icons.Default.ChatBubbleOutline
                            },
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (messageToForward.type) {
                                MessageType.VOICE.name -> "تسجيل صوتي"
                                MessageType.IMAGE.name -> "صورة مرفقة"
                                else -> messageToForward.content.take(80)
                            },
                            fontSize = 12.sp,
                            maxLines = 2,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في المحادثات...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        focusedLabelColor = GoldPrimary
                    ),
                    singleLine = true
                )

                if (filteredChats.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة" else "لا توجد محادثات أخرى متاحة لإعادة التوجيه",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        items(filteredChats, key = { it.id }) { targetChat ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onForwardToChat(targetChat) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OozoxAvatarImage(
                                            model = null,
                                            name = targetChat.contactName,
                                            size = 36.dp,
                                            isTrader = targetChat.contactType.contains("TRADER"),
                                            contentDescription = targetChat.contactName
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(targetChat.contactName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(targetChat.lastMessage?.take(30) ?: "", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Button(
                                        onClick = { onForwardToChat(targetChat) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = adaptiveGoldPrimary,
                                            contentColor = adaptiveOnPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("إرسال", color = adaptiveOnPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("إلغاء", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun DocumentMessageItem(
    fileUri: String,
    fileName: String,
    fileSize: Long,
    caption: String,
    isMe: Boolean,
    zoomScale: Float = 1f
) {
    val clampedZoom = zoomScale.coerceIn(0.75f, 1.85f)
    val context = LocalContext.current
    val isPdf = fileName.endsWith(".pdf", true)
    val isWord = fileName.endsWith(".doc", true) || fileName.endsWith(".docx", true)
    val isExcel = fileName.endsWith(".xls", true) || fileName.endsWith(".xlsx", true) || fileName.endsWith(".csv", true)
    val isArchive = fileName.endsWith(".zip", true) || fileName.endsWith(".rar", true)
    val badgeColor = when {
        isPdf -> Color(0xFFEF4444)
        isWord -> Color(0xFF2563EB)
        isExcel -> Color(0xFF10B981)
        isArchive -> Color(0xFFF59E0B)
        else -> GoldPrimary
    }
    val docIcon = when {
        isPdf -> Icons.Default.PictureAsPdf
        isWord -> Icons.Default.Description
        isExcel -> Icons.Default.TableChart
        isArchive -> Icons.Default.FolderZip
        else -> Icons.Default.InsertDriveFile
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = docIcon,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = badgeColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = FileUtils.getFileExtension(fileName).uppercase().ifBlank { "FILE" },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            if (fileSize > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = FileUtils.formatFileSize(fileSize),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { FileUtils.openDocument(context, fileUri, fileName) },
                        colors = ButtonDefaults.buttonColors(containerColor = badgeColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "فتح المستند",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { FileUtils.shareDocument(context, fileUri, fileName) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة المستند",
                            tint = badgeColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        if (caption.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = caption,
                fontSize = (13f * clampedZoom).sp,
                lineHeight = (18f * clampedZoom).sp,
                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ChatHourlyTimestampHeader(
    timestamp: Long,
    zoomScale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val clampedZoom = zoomScale.coerceIn(0.75f, 1.85f)
    val headerText = remember(timestamp) {
        GoldUtils.formatHourlyTimestampHeader(timestamp)
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
            tonalElevation = 1.dp,
            modifier = Modifier.testTag("hourly_timestamp_header_${timestamp}")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = headerText,
                    fontSize = (11f * clampedZoom).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun parseHexColor(hex: String): Color? {
    val cleanHex = hex.trim().removePrefix("#")
    if (cleanHex.length != 6 && cleanHex.length != 8) return null
    return try {
        val colorInt = if (cleanHex.length == 6) {
            0xFF000000.toInt() or cleanHex.toLong(16).toInt()
        } else {
            cleanHex.toLong(16).toInt()
        }
        Color(colorInt)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun RichTextMessageText(
    text: String,
    textColor: Color,
    fontSize: TextUnit = 14.5.sp,
    lineHeight: TextUnit = 20.sp,
    onHashtagClick: ((String) -> Unit)? = null,
    onCodeClick: ((String) -> Unit)? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onLinkClick: (String) -> Unit
) {
    val annotatedString = remember(text, textColor) {
        buildAnnotatedString {
            val linkRegex = Regex("\\[([^\\]]+)\\]\\(([^\\)]+)\\)|(https?://[^\\s]+|app://[^\\s]+)")
            val hashtagRegex = Regex("#([\\u0600-\\u06FF\\w]+)")
            val mentionRegex = Regex("@([\\u0600-\\u06FF\\w._-]+)")
            val codeRegex = ShortCodeGenerator.CODE_REGEX

            var lastIndex = 0
            val matches = (linkRegex.findAll(text) + hashtagRegex.findAll(text) + mentionRegex.findAll(text) + codeRegex.findAll(text))
                .sortedBy { it.range.first }
                .toList()

            for (match in matches) {
                if (match.range.first >= lastIndex) {
                    val rawMatch = match.value
                    val isValidMentionStart = !rawMatch.startsWith("@") ||
                        match.range.first == 0 ||
                        text[match.range.first - 1].isWhitespace() ||
                        text[match.range.first - 1] in listOf(':', '،', ',', '|', '(', '[')

                    if (!isValidMentionStart) {
                        continue
                    }

                    append(text.substring(lastIndex, match.range.first))

                    when {
                        rawMatch.startsWith("[") -> {
                            val label = match.groupValues.getOrNull(1) ?: ""
                            val url = match.groupValues.getOrNull(2) ?: ""
                            pushStringAnnotation(tag = "URL", annotation = url)
                            pushStyle(
                                SpanStyle(
                                    color = Color(0xFF3B82F6),
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append(label)
                            pop()
                            pop()
                        }
                        rawMatch.startsWith("http") || rawMatch.startsWith("app://") -> {
                            pushStringAnnotation(tag = "URL", annotation = rawMatch)
                            pushStyle(
                                SpanStyle(
                                    color = Color(0xFF3B82F6),
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append(rawMatch)
                            pop()
                            pop()
                        }
                        rawMatch.startsWith("#") -> {
                            val cleanTag = rawMatch.removePrefix("#")
                            val isSpecialTag = rawMatch.contains("له") || rawMatch.contains("عليه") || rawMatch.contains("ذهب") || rawMatch.contains("سداد") || rawMatch.contains("مبيعات") || rawMatch.contains("فاتورة")
                            pushStringAnnotation(tag = "HASHTAG", annotation = cleanTag)
                            pushStyle(
                                SpanStyle(
                                    color = if (isSpecialTag) GoldAccent else GoldPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append(rawMatch)
                            pop()
                            pop()
                        }
                        rawMatch.startsWith("@") -> {
                            val cleanMention = rawMatch.removePrefix("@")
                            pushStringAnnotation(tag = "MENTION", annotation = cleanMention)
                            pushStyle(
                                SpanStyle(
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            append(rawMatch)
                            pop()
                            pop()
                        }
                        codeRegex.matches(rawMatch) -> {
                            val normalizedCode = rawMatch.uppercase(Locale.US)
                            pushStringAnnotation(tag = "CODE", annotation = normalizedCode)
                            pushStyle(
                                SpanStyle(
                                    color = Color(0xFF0EA5E9),
                                    fontWeight = FontWeight.Black,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            append(normalizedCode)
                            pop()
                            pop()
                        }
                        else -> append(rawMatch)
                    }
                    lastIndex = match.range.last + 1
                }
            }
            if (lastIndex < text.length) {
                append(text.substring(lastIndex))
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = fontSize,
            lineHeight = lineHeight,
            color = textColor
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onLinkClick(annotation.item)
                    return@ClickableText
                }
            annotatedString.getStringAnnotations(tag = "HASHTAG", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onHashtagClick?.invoke(annotation.item)
                    return@ClickableText
                }
            annotatedString.getStringAnnotations(tag = "MENTION", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onMentionClick?.invoke(annotation.item)
                    return@ClickableText
                }
            annotatedString.getStringAnnotations(tag = "CODE", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onCodeClick?.invoke(annotation.item)
                }
        }
    )
}
