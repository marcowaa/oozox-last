package com.example.ui.spatial

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Spatial Communication Modes for Context-Aware Chat Canvas
 */
enum class ChatSpatialMode(val label: String, val iconName: String) {
    COMMUNICATION("محادثة", "chat"),
    GOLD("صفقات الذهب", "diamond"),
    FINANCIAL("الحسابات والمالية", "receipt"),
    PROJECT("المشاريع والميزانية", "business"),
    AI_ACCOUNTANT("المحاسب المالي", "auto_awesome")
}

/**
 * 3D Elevation Levels for Spatial UI Hierarchy
 */
object SpatialElevation {
    val Level0 = 0.dp  // Background Canvas
    val Level1 = 2.dp  // Navigation / Structural Rails
    val Level2 = 6.dp  // Conversation Cards / Item Surfaces
    val Level3 = 10.dp // Active Canvas / Floating Panels
    val Level4 = 16.dp // Context Sheets / Floating Overlays
    val Level5 = 24.dp // Transient Actions / Command Center
    val Level6 = 32.dp // Modals & Fullscreen Overlays
}

/**
 * Custom Ambient Drawing Modifier reacting to active Spatial Mode & MaterialTheme background
 */
@Composable
fun Modifier.spatialAmbientBackground(
    mode: ChatSpatialMode = ChatSpatialMode.COMMUNICATION
): Modifier {
    val baseBg = MaterialTheme.colorScheme.background
    return this.background(baseBg)
}

/**
 * 3D Spatial Surface Container with depth elevation, tactile scale response & soft gradient borders
 */
@Composable
fun SpatialSurface(
    modifier: Modifier = Modifier,
    elevation: Dp = SpatialElevation.Level2,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = backgroundColor,
            tonalElevation = elevation,
            shadowElevation = elevation,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            onClick = onClick
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = backgroundColor,
            tonalElevation = elevation,
            shadowElevation = elevation,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * Contextual Spatial Badge for Gold, Financial, AI, and Project Statuses
 */
@Composable
fun SpatialBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    icon: @Composable (() -> Unit)? = null
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, contentColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.invoke()
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
