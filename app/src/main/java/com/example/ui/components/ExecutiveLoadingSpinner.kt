package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.ui.theme.adaptiveOnPrimary
import com.example.ui.theme.OozoxTeal

/**
 * Custom Executive Loading Spinner for high-security business apps.
 * Features dual counter-rotating luxury gradient arcs with a glowing center pulse.
 */
@Composable
fun ExecutiveLoadingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    strokeWidth: Dp = 2.5.dp,
    primaryColor: Color = adaptiveGoldPrimary,
    secondaryColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "executive_spinner")

    // Primary Clockwise Rotation
    val rotationClockwise by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_cw"
    )

    // Secondary Counter-Clockwise Rotation
    val rotationCounterClockwise by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_ccw"
    )

    // Center Pulse Alpha
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(canvasWidth - strokePx, canvasHeight - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Outer Track (Subtle background ring)
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f),
                radius = (canvasWidth - strokePx) / 2f,
                style = Stroke(width = strokePx)
            )

            // Outer Sweeping Arc (Clockwise)
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to primaryColor.copy(alpha = 0.1f),
                    0.7f to primaryColor,
                    1.0f to secondaryColor
                ),
                startAngle = rotationClockwise,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Inner Small Arc (Counter-Clockwise)
            val innerStrokePx = strokePx * 0.75f
            val innerInset = strokePx * 1.8f
            val innerSize = Size(canvasWidth - innerInset * 2, canvasHeight - innerInset * 2)
            val innerTopLeft = Offset(innerInset, innerInset)

            drawArc(
                color = secondaryColor.copy(alpha = 0.85f),
                startAngle = rotationCounterClockwise,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerSize,
                style = Stroke(width = innerStrokePx, cap = StrokeCap.Round)
            )

            // Center glowing core dot
            drawCircle(
                color = primaryColor.copy(alpha = pulseAlpha),
                radius = innerStrokePx * 1.2f,
                center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            )
        }
    }
}

/**
 * Button Loading State content with custom executive spinner and optional text
 */
@Composable
fun ExecutiveButtonLoadingIndicator(
    loadingText: String = "جاري المعالجة والمصادقة...",
    spinnerSize: Dp = 18.dp,
    textColor: Color = adaptiveOnPrimary,
    spinnerPrimaryColor: Color = adaptiveOnPrimary,
    spinnerSecondaryColor: Color = adaptiveGoldPrimary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        ExecutiveLoadingSpinner(
            size = spinnerSize,
            strokeWidth = 2.2.dp,
            primaryColor = spinnerPrimaryColor,
            secondaryColor = spinnerSecondaryColor
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = loadingText,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
