package com.example.util

import androidx.compose.ui.graphics.Color

/**
 * Helper and definitions for Translation typography and contrasting color customization.
 * Enables comfortable, highly legible, and contrasted rendering of AI translations.
 */
data class TranslationColorOption(
    val id: String,
    val titleAr: String,
    val darkColor: Color,
    val lightColor: Color,
    val previewColor: Color
)

data class TranslationSizeOption(
    val id: String,
    val titleAr: String,
    val sizeSp: Float,
    val lineHeightSp: Float,
    val descriptionAr: String
)

object TranslationStyleHelper {

    val COLOR_OPTIONS = listOf(
        TranslationColorOption(
            id = "gold",
            titleAr = "ذهبي متألق ✨",
            darkColor = Color(0xFFFFD54F),
            lightColor = Color(0xFFB45309),
            previewColor = Color(0xFFFFD54F)
        ),
        TranslationColorOption(
            id = "emerald",
            titleAr = "أخضر زمردي 🌿",
            darkColor = Color(0xFF34D399),
            lightColor = Color(0xFF047857),
            previewColor = Color(0xFF10B981)
        ),
        TranslationColorOption(
            id = "cyan",
            titleAr = "أزرق سماوي 💎",
            darkColor = Color(0xFF38BDF8),
            lightColor = Color(0xFF0284C7),
            previewColor = Color(0xFF38BDF8)
        ),
        TranslationColorOption(
            id = "amber",
            titleAr = "كهرماني دافئ 🌅",
            darkColor = Color(0xFFFB923C),
            lightColor = Color(0xFFC2410C),
            previewColor = Color(0xFFF97316)
        ),
        TranslationColorOption(
            id = "white",
            titleAr = "أبيض / أسود ناصع ⚪",
            darkColor = Color(0xFFF8FAFC),
            lightColor = Color(0xFF0F172A),
            previewColor = Color(0xFFF1F5F9)
        ),
        TranslationColorOption(
            id = "contrast",
            titleAr = "أصفر عالي التباين ⚡",
            darkColor = Color(0xFFFDE047),
            lightColor = Color(0xFF713F12),
            previewColor = Color(0xFFFACC15)
        )
    )

    val SIZE_OPTIONS = listOf(
        TranslationSizeOption(
            id = "small",
            titleAr = "صغير (11sp)",
            sizeSp = 11f,
            lineHeightSp = 16f,
            descriptionAr = "مدمج وموفر للمساحة"
        ),
        TranslationSizeOption(
            id = "medium",
            titleAr = "متوسط متناسق (13sp)",
            sizeSp = 13f,
            lineHeightSp = 18.5f,
            descriptionAr = "الافتراضي الموصى به"
        ),
        TranslationSizeOption(
            id = "large",
            titleAr = "كبير مريح (15.5sp)",
            sizeSp = 15.5f,
            lineHeightSp = 22f,
            descriptionAr = "وضوح عالٍ وقراءة سريعة"
        ),
        TranslationSizeOption(
            id = "xlarge",
            titleAr = "كبير جداً (18sp)",
            sizeSp = 18f,
            lineHeightSp = 25f,
            descriptionAr = "أقصى سهولة للقراءة وضعاف البصر"
        )
    )

    fun resolveColor(styleId: String, isDark: Boolean): Color {
        val option = COLOR_OPTIONS.find { it.id.equals(styleId, ignoreCase = true) }
            ?: COLOR_OPTIONS.first()
        return if (isDark) option.darkColor else option.lightColor
    }

    fun resolveFontSize(sizeId: String): Float {
        val option = SIZE_OPTIONS.find { it.id.equals(sizeId, ignoreCase = true) }
            ?: SIZE_OPTIONS[1] // medium
        return option.sizeSp
    }

    fun resolveLineHeight(sizeId: String): Float {
        val option = SIZE_OPTIONS.find { it.id.equals(sizeId, ignoreCase = true) }
            ?: SIZE_OPTIONS[1] // medium
        return option.lineHeightSp
    }
}
