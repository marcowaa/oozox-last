package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Unified circular icon button metrics across the entire OOZOX app.
 *
 * Separating the interactive touch frame ([TouchSize] = 48.dp) from the inner
 * visual circle ([CircleSize] = 36.dp) eliminates Material 3
 * minimumInteractiveComponentSize() overflow/overlap when circular buttons sit
 * side-by-side in headers, dialogs, and toolbars.
 */
object OozoxCircleButtonDefaults {
    val TouchSize: Dp = 48.dp
    val CircleSize: Dp = 36.dp
    val IconSize: Dp = 18.dp
}

@Composable
fun OozoxCircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    border: BorderStroke? = null,
    circleSize: Dp = OozoxCircleButtonDefaults.CircleSize,
    touchSize: Dp = OozoxCircleButtonDefaults.TouchSize,
    content: @Composable BoxScope.() -> Unit
) {
    val effectiveTouchSize = maxOf(touchSize, circleSize)
    if (effectiveTouchSize >= OozoxCircleButtonDefaults.TouchSize) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.size(effectiveTouchSize)
        ) {
            Surface(
                shape = CircleShape,
                color = containerColor,
                border = border,
                modifier = Modifier.size(circleSize)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                    content = content
                )
            }
        }
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = modifier
                .size(effectiveTouchSize)
                .clip(CircleShape)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, radius = effectiveTouchSize / 2),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = containerColor,
                border = border,
                modifier = Modifier.size(circleSize)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                    content = content
                )
            }
        }
    }
}

@Composable
fun OozoxCircleIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    border: BorderStroke? = null,
    circleSize: Dp = OozoxCircleButtonDefaults.CircleSize,
    touchSize: Dp = OozoxCircleButtonDefaults.TouchSize,
    iconSize: Dp = OozoxCircleButtonDefaults.IconSize
) {
    OozoxCircleIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = containerColor,
        border = border,
        circleSize = circleSize,
        touchSize = touchSize
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
