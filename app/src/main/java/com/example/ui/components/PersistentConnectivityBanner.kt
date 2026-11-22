package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldPrimary
import com.example.util.tr
import com.example.util.trFmt

/**
 * Persistent connectivity status banner component that appears when the app loses network access,
 * clearly informing the user that they are working in offline-cached mode backed by Room database.
 */
@Composable
fun PersistentConnectivityBanner(
    isOnline: Boolean,
    pendingSyncCount: Int = 0,
    modifier: Modifier = Modifier,
    onRetryConnection: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = expandVertically(animationSpec = tween(280)) + fadeIn(animationSpec = tween(250)),
        exit = shrinkVertically(animationSpec = tween(240)) + fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        Surface(
            color = Color(0xFF1E293B), // Premium Deep Slate
            contentColor = Color.White,
            tonalElevation = 6.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("persistent_connectivity_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE11D48).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = tr("banner.offline_title"),
                            tint = Color(0xFFFB7185),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = tr("banner.offline_mode_title"),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = tr("banner.cached_badge"),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = if (pendingSyncCount > 0) {
                                trFmt("banner.pending_sync_fmt", "count" to pendingSyncCount)
                            } else {
                                tr("banner.cached_safe")
                            },
                            fontSize = 10.5.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1
                        )
                    }
                }

                if (onRetryConnection != null) {
                    IconButton(
                        onClick = onRetryConnection,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .testTag("connectivity_retry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = tr("common.retry"),
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
