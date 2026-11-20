package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.ui.theme.adaptiveOnPrimary
import com.example.ui.theme.adaptiveStatusBlue
import com.example.ui.theme.adaptiveStatusRed
import com.example.util.acctStr

/**
 * Ultra-Futuristic Kinetic Island Floating Taskbar for the Accounting Hub.
 * Matches the main app's LuxuryBottomSectionBar design:
 * - Floating capsule / pill shape (RoundedCornerShape 28dp)
 * - Pure-icon interface with zero text clutter
 * - Micro active dash indicator underneath the active tab
 * - Ambient radial magnetic glow behind selected icon
 * - Mathematically circular (1:1) notification/count badges
 */
@Composable
fun AccountingTaskbar(
    activeSection: Int,
    onSectionSelected: (Int) -> Unit,
    onVoiceRecordClick: () -> Unit,
    isVoiceRecording: Boolean = false,
    recordsCount: Int = 0,
    groupsCount: Int = 0,
    alertsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val primaryGold = adaptiveGoldPrimary
    val onPrimary = adaptiveOnPrimary
    val redColor = adaptiveStatusRed
    val blueColor = adaptiveStatusBlue

    val infiniteTransition = rememberInfiniteTransition(label = "taskbar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isVoiceRecording) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val borderBrush = remember(primaryGold) {
        Brush.horizontalGradient(
            colors = listOf(
                primaryGold.copy(alpha = 0.45f),
                primaryGold.copy(alpha = 0.20f),
                primaryGold.copy(alpha = 0.55f),
                primaryGold.copy(alpha = 0.45f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 12.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(
                width = 1.5.dp,
                brush = borderBrush
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Overview / Dashboard (-1)
                TaskbarPillItem(
                    icon = Icons.Default.Dashboard,
                    contentDescription = acctStr("sec_dashboard"),
                    isSelected = activeSection == -1,
                    badgeCount = alertsCount,
                    badgeColor = redColor,
                    badgeTag = "taskbar_badge_dashboard",
                    testTag = "accounting_tab_dashboard",
                    primaryAccent = primaryGold,
                    onPrimaryAccent = onPrimary,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSectionSelected(-1)
                    }
                )

                // 2. Daily Ledger / Journal (1)
                TaskbarPillItem(
                    icon = Icons.Default.ReceiptLong,
                    contentDescription = acctStr("sec_ledger"),
                    isSelected = activeSection == 1,
                    badgeCount = recordsCount,
                    badgeColor = primaryGold,
                    badgeTag = "taskbar_badge_ledger",
                    testTag = "accounting_tab_ledger",
                    primaryAccent = primaryGold,
                    onPrimaryAccent = onPrimary,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSectionSelected(1)
                    }
                )

                // 3. Center Floating Voice AI Action
                TaskbarPillItem(
                    icon = if (isVoiceRecording) Icons.Default.GraphicEq else Icons.Default.Mic,
                    contentDescription = acctStr("sec_voice"),
                    isSelected = isVoiceRecording,
                    badgeCount = 0,
                    badgeColor = primaryGold,
                    badgeTag = "taskbar_badge_voice",
                    testTag = "floating_voice_recording_btn",
                    primaryAccent = primaryGold,
                    onPrimaryAccent = onPrimary,
                    customScale = pulseScale,
                    activeTintColor = if (isVoiceRecording) redColor else primaryGold,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onVoiceRecordClick()
                    }
                )

                // 4. Entities / Companies (2)
                TaskbarPillItem(
                    icon = Icons.Default.Business,
                    contentDescription = acctStr("sec_groups"),
                    isSelected = activeSection == 2,
                    badgeCount = groupsCount,
                    badgeColor = blueColor,
                    badgeTag = "taskbar_badge_groups",
                    testTag = "accounting_tab_groups",
                    primaryAccent = primaryGold,
                    onPrimaryAccent = onPrimary,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSectionSelected(2)
                    }
                )

                // 5. Staff & Vaults (3 or 0)
                val isStaffOrVault = activeSection == 3 || activeSection == 0
                TaskbarPillItem(
                    icon = if (activeSection == 0) Icons.Default.Lock else Icons.Default.Groups,
                    contentDescription = if (activeSection == 0) acctStr("sec_vaults") else acctStr("sec_staff"),
                    isSelected = isStaffOrVault,
                    badgeCount = 0,
                    badgeColor = primaryGold,
                    badgeTag = "taskbar_badge_staff",
                    testTag = "accounting_tab_staff",
                    primaryAccent = primaryGold,
                    onPrimaryAccent = onPrimary,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSectionSelected(3)
                    }
                )
            }
        }
    }
}

@Composable
private fun RowScope.TaskbarPillItem(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    badgeCount: Int,
    badgeColor: Color,
    badgeTag: String,
    testTag: String,
    primaryAccent: Color,
    onPrimaryAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    customScale: Float = 1.0f,
    activeTintColor: Color? = null
) {
    val activeColor = activeTintColor ?: primaryAccent
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f * customScale else 1.0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "tab_scale"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        animationSpec = tween(durationMillis = 180),
        label = "icon_color"
    )

    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // Ambient magnetic radial glow for active tab
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                activeColor.copy(alpha = 0.22f),
                                activeColor.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.TopEnd,
                modifier = Modifier.scale(animatedScale)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )

                // Real-time circular notification badge (1:1 mathematical ratio)
                if (badgeCount > 0) {
                    CircularCounterBadge(
                        count = badgeCount,
                        containerColor = badgeColor,
                        contentColor = if (badgeColor == primaryAccent) onPrimaryAccent else Color.White,
                        modifier = Modifier
                            .offset(x = 6.dp, y = (-4).dp)
                            .testTag(badgeTag)
                    )
                }
            }

            // Dynamic micro active dot/dash indicator underneath active tab
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(tween(180)) + scaleIn(tween(180)),
                exit = fadeOut(tween(180)) + scaleOut(tween(180))
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .width(14.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(activeColor, activeColor.copy(alpha = 0.6f), activeColor)
                            )
                        )
                )
            }
        }
    }
}

/**
 * Guarantees a true 1:1 circular badge regardless of digit count ("1", "12", "99+")
 * or font scaling, avoiding oval stretching.
 */
@Composable
fun CircularCounterBadge(
    count: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val label = if (count > 99) "99+" else "$count"
    Surface(
        color = containerColor,
        shape = CircleShape,
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                val minDiameter = 18.dp.roundToPx()
                val horizontalPad = if (label.length >= 3) 4.dp.roundToPx() else 5.dp.roundToPx()
                val verticalPad = 4.dp.roundToPx()
                val diameter = maxOf(
                    placeable.width + horizontalPad,
                    placeable.height + verticalPad,
                    minDiameter
                )
                layout(diameter, diameter) {
                    placeable.placeRelative(
                        (diameter - placeable.width) / 2,
                        (diameter - placeable.height) / 2
                    )
                }
            }
        ) {
            Text(
                text = label,
                color = contentColor,
                fontSize = when {
                    label.length >= 3 -> 7.5.sp
                    label.length == 2 -> 8.5.sp
                    else -> 9.sp
                },
                lineHeight = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}
