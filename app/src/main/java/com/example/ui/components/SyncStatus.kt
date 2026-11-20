package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.RealGoldAmber
import com.example.util.OfflineSyncManager

/**
 * 'SyncStatus' component that displays a subtle pulsing indicator in the app header
 * when background workers are actively synchronizing data with the API server.
 */
@Composable
fun SyncStatus(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    showLabel: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val isSyncing by OfflineSyncManager.isSyncing.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = isSyncing,
        enter = fadeIn(tween(220)) + scaleIn(tween(220)),
        exit = fadeOut(tween(200)) + scaleOut(tween(200)),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "SyncStatusPulse")
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "SyncStatusRotation"
        )

        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(750, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "SyncStatusPulseScale"
        )

        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.75f,
            animationSpec = infiniteRepeatable(
                animation = tween(750, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "SyncStatusGlow"
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = glowAlpha)),
            shadowElevation = 3.dp,
            modifier = Modifier
                .testTag("app_header_sync_status")
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = if (showLabel) 8.dp else 6.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(size - 10.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "جاري المزامنة مع السيرفر",
                        tint = GoldPrimary,
                        modifier = Modifier
                            .size(size - 16.dp)
                            .graphicsLayer(rotationZ = rotationAngle)
                    )
                }

                if (showLabel) {
                    Text(
                        text = "مزامنة...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }
            }
        }
    }
}
