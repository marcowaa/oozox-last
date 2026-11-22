package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val OozoxArabicFont = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold),
    Font(R.font.cairo, FontWeight.ExtraBold)
)

val OozoxDisplayFont = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

/**
 * Global Font Scaling enum for accessibility and enhanced visual readability.
 */
enum class AppFontScale(
    val scaleFactor: Float,
    val titleAr: String,
    val descriptionAr: String,
    val subtitle: String
) {
    STANDARD(1.0f, "الحجم القياسي (100%)", "الحجم الافتراضي لواجهات التطبيق", "عادي"),
    COMFORTABLE(1.15f, "مريح ومحسن (115%)", "قراءة أسهل ومريحة للعين", "مريح"),
    LARGE(1.30f, "كبير وواضح (130%)", "نصوص وأرقام بارزة وعريضة", "كبير"),
    EXTRA_LARGE(1.45f, "فائق الوضوح (145%)", "مخصص لضعاف النظر وكبار السن", "فائق");

    fun localizedTitle(language: com.example.util.AppLanguage): String {
        val key = when (this) {
            STANDARD -> "font_scale.standard_t"
            COMFORTABLE -> "font_scale.comfortable_t"
            LARGE -> "font_scale.large_t"
            EXTRA_LARGE -> "font_scale.xlarge_t"
        }
        return com.example.util.Translations.get(key, language)
    }

    fun localizedDescription(language: com.example.util.AppLanguage): String {
        val key = when (this) {
            STANDARD -> "font_scale.standard_d"
            COMFORTABLE -> "font_scale.comfortable_d"
            LARGE -> "font_scale.large_d"
            EXTRA_LARGE -> "font_scale.xlarge_d"
        }
        return com.example.util.Translations.get(key, language)
    }

    companion object {
        fun fromName(name: String?): AppFontScale =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: STANDARD
    }
}

val LocalAppFontScale = staticCompositionLocalOf { AppFontScale.STANDARD }

/**
 * Creates dynamic Material3 Typography using the bundled Cairo editorial typeface,
 * scaled cleanly by the user's selected accessibility font scale.
 */
fun getScaledTypography(fontScale: AppFontScale): Typography {
    val factor = fontScale.scaleFactor
    return Typography(
        displayLarge = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.Bold,
            fontSize = (46 * factor).sp,
            lineHeight = (54 * factor).sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.Bold,
            fontSize = (36 * factor).sp,
            lineHeight = (44 * factor).sp,
            letterSpacing = (-0.25).sp
        ),
        displaySmall = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = (30 * factor).sp,
            lineHeight = (38 * factor).sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.Bold,
            fontSize = (28 * factor).sp,
            lineHeight = (36 * factor).sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.Bold,
            fontSize = (24 * factor).sp,
            lineHeight = (32 * factor).sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.Bold,
            fontSize = (21 * factor).sp,
            lineHeight = (28 * factor).sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.Bold,
            fontSize = (19 * factor).sp,
            lineHeight = (26 * factor).sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * factor).sp,
            lineHeight = (23 * factor).sp,
            letterSpacing = 0.1.sp
        ),
        titleSmall = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14 * factor).sp,
            lineHeight = (20 * factor).sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.Normal,
            fontSize = (15.5 * factor).sp,
            lineHeight = (23 * factor).sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5 * factor).sp,
            lineHeight = (20 * factor).sp,
            letterSpacing = 0.15.sp
        ),
        bodySmall = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.Normal,
            fontSize = (11.5 * factor).sp,
            lineHeight = (16 * factor).sp,
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13.5 * factor).sp,
            lineHeight = (18 * factor).sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = (11.5 * factor).sp,
            lineHeight = (16 * factor).sp,
            letterSpacing = 0.25.sp
        ),
        labelSmall = TextStyle(
            fontFamily = OozoxArabicFont,
            fontWeight = FontWeight.Medium,
            fontSize = (10.5 * factor).sp,
            lineHeight = (14 * factor).sp,
            letterSpacing = 0.3.sp
        )
    )
}
