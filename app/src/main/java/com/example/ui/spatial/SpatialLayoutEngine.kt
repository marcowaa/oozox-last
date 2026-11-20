package com.example.ui.spatial

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.adaptiveGoldDark
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.ui.theme.adaptiveOnPrimary

enum class SpatialWindowSizeClass {
    COMPACT,   // Phones (<600dp)
    MEDIUM,    // Small Tablets / Foldables (600dp - 840dp)
    EXPANDED   // Large Tablets / Desktop (>840dp)
}

enum class SpatialOrientation {
    PORTRAIT,
    LANDSCAPE
}

enum class SpatialDeviceType(val label: String, val icon: String) {
    MOBILE("هاتف محمول", "phone_android"),
    TABLET("جهاز لوحي", "tablet_android"),
    DESKTOP("حاسوب مكتبي", "desktop_windows")
}

enum class SpatialPaneMode(val label: String, val description: String) {
    ONE_PANE("شاشة واحدة (1-Pane)", "عرض القائمة والتفاصيل بالتعاقب"),
    TWO_PANE("شاشتان (2-Pane)", "عرض القائمة والتفاصيل جنبًا إلى جنب"),
    THREE_PANE("3 شاشات (3-Pane)", "عرض الملاحة + التفاصيل + المساعد المالي والذهب")
}

/**
 * Real-time layout telemetry and state provided by SpatialLayoutEngine
 */
data class SpatialLayoutInfo(
    val widthDp: Dp = 360.dp,
    val heightDp: Dp = 800.dp,
    val orientation: SpatialOrientation = SpatialOrientation.PORTRAIT,
    val deviceType: SpatialDeviceType = SpatialDeviceType.MOBILE,
    val windowSizeClass: SpatialWindowSizeClass = SpatialWindowSizeClass.COMPACT,
    val effectivePaneMode: SpatialPaneMode = SpatialPaneMode.ONE_PANE,
    val overridePaneMode: SpatialPaneMode? = null,
    val onSetOverridePaneMode: (SpatialPaneMode?) -> Unit = {}
) {
    val isMultiPane: Boolean get() = effectivePaneMode != SpatialPaneMode.ONE_PANE
    val isThreePane: Boolean get() = effectivePaneMode == SpatialPaneMode.THREE_PANE
}

/**
 * CompositionLocal providing SpatialLayoutInfo app-wide context
 */
val LocalSpatialLayoutInfo = staticCompositionLocalOf { SpatialLayoutInfo() }

/**
 * Core SpatialLayoutEngine Provider wrapping application context.
 * Dynamically tracks window width, height, orientation, and device type,
 * providing responsive auto-layout logic for 1-pane, 2-pane, and 3-pane modes.
 */
@Composable
fun SpatialLayoutEngineProvider(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    var manualOverride by remember { mutableStateOf<SpatialPaneMode?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight

        val orientation = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            SpatialOrientation.LANDSCAPE
        } else {
            SpatialOrientation.PORTRAIT
        }

        // Determine device classification based on width & system metrics
        val deviceType = when {
            width >= 1000.dp -> SpatialDeviceType.DESKTOP
            width >= 600.dp || configuration.smallestScreenWidthDp >= 600 -> SpatialDeviceType.TABLET
            else -> SpatialDeviceType.MOBILE
        }

        // Determine Window Size Class
        val windowSizeClass = when {
            width < 600.dp -> SpatialWindowSizeClass.COMPACT
            width < 840.dp -> SpatialWindowSizeClass.MEDIUM
            else -> SpatialWindowSizeClass.EXPANDED
        }

        // Unified full-canvas mode across all screen widths (no split-screen halves)
        val autoPaneMode = SpatialPaneMode.ONE_PANE
        val effectivePaneMode = SpatialPaneMode.ONE_PANE

        val layoutInfo = remember(
            width, height, orientation, deviceType, windowSizeClass, effectivePaneMode, manualOverride
        ) {
            SpatialLayoutInfo(
                widthDp = width,
                heightDp = height,
                orientation = orientation,
                deviceType = deviceType,
                windowSizeClass = windowSizeClass,
                effectivePaneMode = effectivePaneMode,
                overridePaneMode = null,
                onSetOverridePaneMode = { manualOverride = SpatialPaneMode.ONE_PANE }
            )
        }

        CompositionLocalProvider(LocalSpatialLayoutInfo provides layoutInfo) {
            content()
        }
    }
}

/**
 * Telemetry Bar displaying SpatialLayoutEngine real-time metrics & mode selector
 */
@Composable
fun SpatialLayoutTelemetryBar(
    modifier: Modifier = Modifier,
    showDetailedControls: Boolean = true
) {
    val layoutInfo = LocalSpatialLayoutInfo.current

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldPrimary.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Live Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (layoutInfo.deviceType) {
                                SpatialDeviceType.MOBILE -> Icons.Default.PhoneAndroid
                                SpatialDeviceType.TABLET -> Icons.Default.TabletAndroid
                                SpatialDeviceType.DESKTOP -> Icons.Default.DesktopWindows
                            },
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = "${layoutInfo.deviceType.label} • ${layoutInfo.orientation.name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = GoldDark.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${layoutInfo.widthDp.value.toInt()}x${layoutInfo.heightDp.value.toInt()} dp",
                            fontSize = 10.sp,
                            color = GoldLight,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Active Pane Badge
                Surface(
                    color = when (layoutInfo.effectivePaneMode) {
                        SpatialPaneMode.ONE_PANE -> adaptiveGoldPrimary
                        SpatialPaneMode.TWO_PANE -> Color(0xFF10B981)
                        SpatialPaneMode.THREE_PANE -> Color(0xFF8B5CF6)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = layoutInfo.effectivePaneMode.label.split(" ")[0],
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (layoutInfo.effectivePaneMode) {
                            SpatialPaneMode.ONE_PANE -> adaptiveOnPrimary
                            else -> Color.White
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (showDetailedControls) {
                Spacer(modifier = Modifier.height(8.dp))

                // Selector for 1-Pane, 2-Pane, 3-Pane or Auto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isAuto = layoutInfo.overridePaneMode == null

                    FilterChip(
                        selected = isAuto,
                        onClick = { layoutInfo.onSetOverridePaneMode(null) },
                        label = { Text("تلقائي (Auto)", fontSize = 10.sp) },
                        leadingIcon = if (isAuto) {
                            { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = adaptiveGoldPrimary,
                            selectedLabelColor = adaptiveOnPrimary
                        )
                    )

                    SpatialPaneMode.entries.forEach { mode ->
                        val isSelected = layoutInfo.overridePaneMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { layoutInfo.onSetOverridePaneMode(mode) },
                            label = { Text(mode.label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = adaptiveGoldDark,
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Adaptive Multi-Pane Layout engine dynamically switching between 1-pane, 2-pane, and 3-pane presentations.
 */
@Composable
fun SpatialMultiPaneLayout(
    modifier: Modifier = Modifier,
    onePaneContent: @Composable () -> Unit,
    primaryPane: @Composable () -> Unit,
    secondaryPane: @Composable () -> Unit,
    tertiaryPane: (@Composable () -> Unit)? = null
) {
    val layoutInfo = LocalSpatialLayoutInfo.current

    when (layoutInfo.effectivePaneMode) {
        SpatialPaneMode.ONE_PANE -> {
            onePaneContent()
        }

        SpatialPaneMode.TWO_PANE -> {
            Row(modifier = modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    primaryPane()
                }

                VerticalDivider(
                    color = GoldPrimary.copy(alpha = 0.2f),
                    thickness = 1.dp
                )

                Surface(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    secondaryPane()
                }
            }
        }

        SpatialPaneMode.THREE_PANE -> {
            Row(modifier = modifier.fillMaxSize()) {
                // Pane 1: Navigation / Primary List
                Surface(
                    modifier = Modifier
                        .weight(0.28f)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    primaryPane()
                }

                VerticalDivider(
                    color = GoldPrimary.copy(alpha = 0.2f),
                    thickness = 1.dp
                )

                // Pane 2: Primary Content / Active Detail View
                Surface(
                    modifier = Modifier
                        .weight(0.44f)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    secondaryPane()
                }

                VerticalDivider(
                    color = GoldPrimary.copy(alpha = 0.2f),
                    thickness = 1.dp
                )

                // Pane 3: Spatial Context Inspector / AI & Financial Hub
                Surface(
                    modifier = Modifier
                        .weight(0.28f)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    if (tertiaryPane != null) {
                        tertiaryPane()
                    } else {
                        secondaryPane()
                    }
                }
            }
        }
    }
}

/**
 * Adaptive Container helper
 */
@Composable
fun SpatialAdaptiveContainer(
    modifier: Modifier = Modifier,
    compactContent: @Composable () -> Unit,
    mediumContent: @Composable (() -> Unit)? = null,
    expandedContent: @Composable (() -> Unit)? = null
) {
    val layoutInfo = LocalSpatialLayoutInfo.current

    when (layoutInfo.windowSizeClass) {
        SpatialWindowSizeClass.COMPACT -> compactContent()
        SpatialWindowSizeClass.MEDIUM -> (mediumContent ?: compactContent)()
        SpatialWindowSizeClass.EXPANDED -> (expandedContent ?: mediumContent ?: compactContent)()
    }
}
