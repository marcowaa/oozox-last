package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.UUID

enum class AppToastType {
    SUCCESS,
    INFO,
    WARNING,
    ERROR
}

data class AppToastData(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val type: AppToastType = AppToastType.SUCCESS,
    val durationMillis: Long = 3200L,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

class AppToastController {
    var activeToast by mutableStateOf<AppToastData?>(null)
        private set

    fun showToast(
        message: String,
        type: AppToastType = AppToastType.SUCCESS,
        durationMillis: Long = 3200L,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        activeToast = AppToastData(
            message = message,
            type = type,
            durationMillis = durationMillis,
            actionLabel = actionLabel,
            onAction = onAction
        )
    }

    fun dismissToast() {
        activeToast = null
    }
}

object ToastNotificationBus {
    private val _toastEvents = kotlinx.coroutines.flow.MutableSharedFlow<AppToastData>(extraBufferCapacity = 30)
    val toastEvents: SharedFlow<AppToastData> = _toastEvents.asSharedFlow()

    fun showToast(
        message: String,
        type: AppToastType = AppToastType.SUCCESS,
        durationMillis: Long = 3500L,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        _toastEvents.tryEmit(
            AppToastData(
                message = message,
                type = type,
                durationMillis = durationMillis,
                actionLabel = actionLabel,
                onAction = onAction
            )
        )
    }

    fun showProjectUpdate(
        projectName: String,
        updateDescription: String,
        onAction: (() -> Unit)? = null
    ) {
        showToast(
            message = "🏗️ تحديث مشروع ($projectName): $updateDescription",
            type = AppToastType.INFO,
            durationMillis = 4200L,
            actionLabel = "عرض",
            onAction = onAction
        )
    }

    fun showNewMessage(
        senderName: String,
        messageText: String,
        onAction: (() -> Unit)? = null
    ) {
        val snippet = if (messageText.length > 60) messageText.take(57) + "..." else messageText
        showToast(
            message = "$senderName: $snippet",
            type = AppToastType.SUCCESS,
            durationMillis = 4000L,
            actionLabel = "رد",
            onAction = onAction
        )
    }
}

val LocalAppToast = staticCompositionLocalOf { AppToastController() }

@Composable
fun ToastNotificationHost(
    toastController: AppToastController,
    modifier: Modifier = Modifier
) {
    val currentToast = toastController.activeToast

    // Connect to global ToastNotificationBus events
    LaunchedEffect(Unit) {
        ToastNotificationBus.toastEvents.collect { event: AppToastData ->
            toastController.showToast(
                message = event.message,
                type = event.type,
                durationMillis = event.durationMillis,
                actionLabel = event.actionLabel,
                onAction = event.onAction
            )
        }
    }

    LaunchedEffect(currentToast) {
        if (currentToast != null) {
            delay(currentToast.durationMillis)
            if (toastController.activeToast?.id == currentToast.id) {
                toastController.dismissToast()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(9999f),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = currentToast != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
            ) + fadeIn(animationSpec = tween(250)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(200)
            ) + fadeOut(animationSpec = tween(150)),
            modifier = Modifier.padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            if (currentToast != null) {
                ToastNotificationPill(
                    toast = currentToast,
                    onDismiss = { toastController.dismissToast() }
                )
            }
        }
    }
}

@Composable
fun ToastNotificationPill(
    toast: AppToastData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (toast.type) {
        AppToastType.SUCCESS -> Icons.Default.CheckCircle
        AppToastType.INFO -> Icons.Default.Info
        AppToastType.WARNING -> Icons.Default.Warning
        AppToastType.ERROR -> Icons.Default.Error
    }

    val iconColor: Color = when (toast.type) {
        AppToastType.SUCCESS -> StatusGreen
        AppToastType.INFO -> MaterialTheme.colorScheme.primary
        AppToastType.WARNING -> StatusOrange
        AppToastType.ERROR -> StatusRed
    }

    val borderColor: Color = when (toast.type) {
        AppToastType.SUCCESS -> StatusGreen.copy(alpha = 0.7f)
        AppToastType.INFO -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        AppToastType.WARNING -> StatusOrange.copy(alpha = 0.7f)
        AppToastType.ERROR -> StatusRed.copy(alpha = 0.7f)
    }

    Surface(
        modifier = modifier
            .shadow(elevation = 14.dp, shape = RoundedCornerShape(24.dp), spotColor = iconColor.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(24.dp))
            .clickable { onDismiss() }
            .testTag("app_toast_notification_pill"),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        border = BorderStroke(1.5.dp, borderColor),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = toast.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (toast.actionLabel != null && toast.onAction != null) {
                Surface(
                    onClick = {
                        toast.onAction.invoke()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = toast.actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
