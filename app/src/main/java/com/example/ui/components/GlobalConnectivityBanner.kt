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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.GoldPrimary
import com.example.util.OfflineSyncManager

/**
 * GlobalConnectivityBanner component that observes network state changes and displays
 * a persistent, non-intrusive banner when the user is offline, indicating that they
 * are viewing cached data.
 */
@Composable
fun GlobalConnectivityBanner(
    modifier: Modifier = Modifier,
    onRetryClick: (() -> Unit)? = null
) {
    val isOnline by OfflineSyncManager.isNetworkOnline.collectAsStateWithLifecycle()
    val pendingSyncCount by OfflineSyncManager.pendingSyncCount.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bannerBg = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF3F8)
    val bannerPrimaryText = if (isDark) Color.White else Color(0xFF0F172A)
    val bannerSecondaryText = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val accentTint = if (isDark) GoldPrimary else Color(0xFFB45309)

    AnimatedVisibility(
        visible = !isOnline,
        enter = expandVertically(animationSpec = tween(280)) + fadeIn(animationSpec = tween(250)),
        exit = shrinkVertically(animationSpec = tween(240)) + fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        Surface(
            color = bannerBg,
            contentColor = bannerPrimaryText,
            tonalElevation = 2.dp,
            shadowElevation = if (isDark) 4.dp else 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .testTag("global_connectivity_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 9.dp),
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
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE11D48).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "غير متصل بالإنترنت",
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "أنت غير متصل بالإنترنت",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = bannerPrimaryText
                            )
                            Surface(
                                color = accentTint.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "بيانات مخزنة (Cached)",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentTint,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = if (pendingSyncCount > 0) {
                                "تعرض البيانات المخزنة محلياً • هناك $pendingSyncCount تعديل سيتم رفعه تلقائياً فور الاتصال"
                            } else {
                                "تعرض البيانات المخزنة محلياً بأمان وسرعة"
                            },
                            fontSize = 10.5.sp,
                            color = bannerSecondaryText,
                            maxLines = 1
                        )
                    }
                }

                if (onRetryClick != null) {
                    IconButton(
                        onClick = onRetryClick,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bannerPrimaryText.copy(alpha = 0.08f))
                            .testTag("connectivity_retry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "إعادة المحاولة",
                            tint = accentTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
