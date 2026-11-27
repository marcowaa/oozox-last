package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

val WhatsAppGreen = Color(0xFF25D366)
val WhatsAppGreenDark = Color(0xFF128C7E)
val TelegramBlue = Color(0xFF2AABEE)
val TelegramBlueDark = Color(0xFF1E96C8)
val SmsGold = Color(0xFFF59E0B)
val SmsGoldDark = Color(0xFFD97706)

/**
 * High-Resolution Vector WhatsApp Brand Icon
 */
@Composable
fun WhatsAppIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = WhatsAppGreen
) {
    Image(
        painter = painterResource(id = R.drawable.ic_brand_whatsapp),
        contentDescription = "WhatsApp",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

/**
 * High-Resolution Vector Telegram Brand Icon
 */
@Composable
fun TelegramIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = TelegramBlue
) {
    Image(
        painter = painterResource(id = R.drawable.ic_brand_telegram),
        contentDescription = "Telegram",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}

/**
 * High-Resolution Vector SMS Brand Icon
 */
@Composable
fun SmsIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    tint: Color = SmsGold
) {
    Image(
        painter = painterResource(id = R.drawable.ic_brand_sms),
        contentDescription = "SMS",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}
