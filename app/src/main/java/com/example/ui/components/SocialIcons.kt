package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val WhatsAppGreen = Color(0xFF25D366)
val WhatsAppGreenDark = Color(0xFF1EBE5D)
val TelegramBlue = Color(0xFF2AABEE)
val TelegramBlueDark = Color(0xFF229ED9)
val SmsGold = Color(0xFFF59E0B)
val SmsGoldDark = Color(0xFFD97706)

/**
 * Authentic Pixel-Perfect WhatsApp Vector Icon
 * Matches official WhatsApp brand specifications with speech bubble tail & handset
 */
@Composable
fun WhatsAppIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = WhatsAppGreen
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Outer green circle with subtle vertical gradient
            drawCircle(
                brush = Brush.verticalGradient(
                    listOf(WhatsAppGreen, WhatsAppGreenDark)
                ),
                radius = w * 0.48f,
                center = Offset(w * 0.5f, h * 0.5f)
            )

            // Inner white speech bubble path
            val bubblePath = Path().apply {
                moveTo(w * 0.50f, h * 0.16f)
                cubicTo(w * 0.72f, h * 0.16f, w * 0.84f, h * 0.28f, w * 0.84f, h * 0.48f)
                cubicTo(w * 0.84f, h * 0.68f, w * 0.68f, h * 0.80f, w * 0.50f, h * 0.80f)
                cubicTo(w * 0.44f, h * 0.80f, w * 0.38f, h * 0.78f, w * 0.33f, h * 0.75f)
                lineTo(w * 0.20f, h * 0.82f)
                lineTo(w * 0.24f, h * 0.69f)
                cubicTo(w * 0.18f, h * 0.63f, w * 0.16f, h * 0.55f, w * 0.16f, h * 0.48f)
                cubicTo(w * 0.16f, h * 0.28f, w * 0.28f, h * 0.16f, w * 0.50f, h * 0.16f)
                close()
            }

            drawPath(
                path = bubblePath,
                color = Color.White.copy(alpha = 0.25f),
                style = Stroke(width = w * 0.05f, join = StrokeJoin.Round, cap = StrokeCap.Round)
            )

            // White phone handset inside with authentic curvature
            val phonePath = Path().apply {
                moveTo(w * 0.36f, h * 0.32f)
                cubicTo(w * 0.34f, h * 0.35f, w * 0.33f, h * 0.40f, w * 0.37f, h * 0.48f)
                cubicTo(w * 0.42f, h * 0.58f, w * 0.50f, h * 0.66f, w * 0.59f, h * 0.70f)
                cubicTo(w * 0.65f, h * 0.73f, w * 0.69f, h * 0.72f, w * 0.72f, h * 0.69f)
                lineTo(w * 0.76f, h * 0.64f)
                cubicTo(w * 0.78f, h * 0.61f, w * 0.77f, h * 0.58f, w * 0.73f, h * 0.56f)
                lineTo(w * 0.65f, h * 0.51f)
                cubicTo(w * 0.62f, h * 0.49f, w * 0.59f, h * 0.50f, w * 0.57f, h * 0.53f)
                lineTo(w * 0.55f, h * 0.55f)
                cubicTo(w * 0.54f, h * 0.56f, w * 0.52f, h * 0.56f, w * 0.50f, h * 0.54f)
                cubicTo(w * 0.47f, h * 0.51f, w * 0.43f, h * 0.46f, w * 0.42f, h * 0.43f)
                cubicTo(w * 0.41f, h * 0.41f, w * 0.42f, h * 0.40f, w * 0.43f, h * 0.39f)
                lineTo(w * 0.46f, h * 0.36f)
                cubicTo(w * 0.48f, h * 0.33f, w * 0.48f, h * 0.30f, w * 0.46f, h * 0.28f)
                lineTo(w * 0.42f, h * 0.21f)
                cubicTo(w * 0.40f, h * 0.18f, w * 0.37f, h * 0.19f, w * 0.35f, h * 0.21f)
                close()
            }

            drawPath(
                path = phonePath,
                color = Color.White,
                style = Fill
            )
        }
    }
}

/**
 * Authentic Pixel-Perfect Telegram Vector Icon
 * Matches official Telegram paper airplane geometry and gradient
 */
@Composable
fun TelegramIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = TelegramBlue
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Circle background with official Telegram blue gradient
            drawCircle(
                brush = Brush.verticalGradient(
                    listOf(TelegramBlue, TelegramBlueDark)
                ),
                radius = w * 0.48f,
                center = Offset(w * 0.5f, h * 0.5f)
            )

            // Official Telegram paper airplane
            // Main body
            val planeBody = Path().apply {
                moveTo(w * 0.23f, h * 0.48f)
                lineTo(w * 0.77f, h * 0.25f)
                lineTo(w * 0.67f, h * 0.75f)
                lineTo(w * 0.51f, h * 0.63f)
                lineTo(w * 0.42f, h * 0.71f)
                lineTo(w * 0.44f, h * 0.57f)
                close()
            }

            drawPath(
                path = planeBody,
                color = Color.White,
                style = Fill
            )

            // Airplane shadow/fold detail
            val planeFold = Path().apply {
                moveTo(w * 0.51f, h * 0.63f)
                lineTo(w * 0.44f, h * 0.57f)
                lineTo(w * 0.69f, h * 0.34f)
                close()
            }

            drawPath(
                path = planeFold,
                color = Color(0xFFD2EAF6),
                style = Fill
            )

            val planeBackFold = Path().apply {
                moveTo(w * 0.44f, h * 0.57f)
                lineTo(w * 0.42f, h * 0.71f)
                lineTo(w * 0.48f, h * 0.60f)
                close()
            }

            drawPath(
                path = planeBackFold,
                color = Color(0xFFB5DCF1),
                style = Fill
            )
        }
    }
}

/**
 * Authentic Pixel-Perfect SMS Message Vector Icon
 * Clean high-contrast communication bubble with message lines
 */
@Composable
fun SmsIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = SmsGold
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Golden gradient circle
            drawCircle(
                brush = Brush.verticalGradient(
                    listOf(SmsGold, SmsGoldDark)
                ),
                radius = w * 0.48f,
                center = Offset(w * 0.5f, h * 0.5f)
            )

            // Modern curved SMS dialogue bubble
            val bubblePath = Path().apply {
                moveTo(w * 0.24f, h * 0.28f)
                lineTo(w * 0.76f, h * 0.28f)
                cubicTo(w * 0.83f, h * 0.28f, w * 0.86f, h * 0.32f, w * 0.86f, h * 0.40f)
                lineTo(w * 0.86f, h * 0.60f)
                cubicTo(w * 0.86f, h * 0.68f, w * 0.83f, h * 0.72f, w * 0.76f, h * 0.72f)
                lineTo(w * 0.44f, h * 0.72f)
                lineTo(w * 0.28f, h * 0.82f)
                lineTo(w * 0.30f, h * 0.72f)
                lineTo(w * 0.24f, h * 0.72f)
                cubicTo(w * 0.17f, h * 0.72f, w * 0.14f, h * 0.68f, w * 0.14f, h * 0.60f)
                lineTo(w * 0.14f, h * 0.40f)
                cubicTo(w * 0.14f, h * 0.32f, w * 0.17f, h * 0.28f, w * 0.24f, h * 0.28f)
                close()
            }

            drawPath(
                path = bubblePath,
                color = Color.White,
                style = Fill
            )

            // Three communication message dots inside
            val dotRadius = w * 0.045f
            val dotY = h * 0.50f

            drawCircle(
                color = SmsGoldDark,
                radius = dotRadius,
                center = Offset(w * 0.35f, dotY)
            )

            drawCircle(
                color = SmsGoldDark,
                radius = dotRadius,
                center = Offset(w * 0.50f, dotY)
            )

            drawCircle(
                color = SmsGoldDark,
                radius = dotRadius,
                center = Offset(w * 0.65f, dotY)
            )
        }
    }
}
