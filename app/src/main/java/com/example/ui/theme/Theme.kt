package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private tailrec fun Context.findActivityWindow(): Window? = when (this) {
    is Activity -> window
    is ContextWrapper -> baseContext.findActivityWindow()
    else -> null
}

enum class AppThemePalette(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val primaryColor: Color,
    val lightPrimaryColor: Color,
    val darkBgColor: Color,
    val darkSurfaceColor: Color,
    val darkSurfaceVariant: Color,
    val lightBgColor: Color,
    val lightSurfaceColor: Color,
    val lightSurfaceVariant: Color,
    val accentColor: Color,
    val onPrimaryDark: Color = Color.Black,
    val onPrimaryLight: Color = Color.White
) {
    OOZOX_INFINITY(
        id = "OOZOX_INFINITY",
        titleAr = "أوزوكس الرسمي ♾️ (الأصلي)",
        titleEn = "OOZOX Official",
        primaryColor = Color(0xFF00E5C0), // Bright Vivid Emerald Cyan Accent
        lightPrimaryColor = Color(0xFF00897B), // Rich Deep Emerald Teal
        darkBgColor = Color(0xFF070B12),  // High-Contrast Deep Obsidian
        darkSurfaceColor = Color(0xFF0F1728), // High-Contrast Slate Surface
        darkSurfaceVariant = Color(0xFF19243C),
        lightBgColor = Color(0xFFF6F8FC),
        lightSurfaceColor = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFE6EEF8),
        accentColor = Color(0xFF00FFD5), // Bright Glow Cyan
        onPrimaryDark = Color(0xFF000000),
        onPrimaryLight = Color(0xFFFFFFFF)
    ),
    OOZOX_PORCELAIN(
        id = "OOZOX_PORCELAIN",
        titleAr = "أوزوكس بيضاء فاخرة ⚪",
        titleEn = "OOZOX Porcelain Pure",
        primaryColor = Color(0xFF2DD4BF),
        lightPrimaryColor = Color(0xFF0F766E),
        darkBgColor = Color(0xFF0E1726),
        darkSurfaceColor = Color(0xFF1A263C),
        darkSurfaceVariant = Color(0xFF283854),
        lightBgColor = Color(0xFFF1F5F9),
        lightSurfaceColor = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFDBE5F1),
        accentColor = Color(0xFF00BFA5),
        onPrimaryDark = Color(0xFF000000),
        onPrimaryLight = Color(0xFFFFFFFF)
    ),
    CYBER_SAPPHIRE(
        id = "CYBER_SAPPHIRE",
        titleAr = "الياقوت الأزرق 🌌",
        titleEn = "Sapphire Cyber",
        primaryColor = Color(0xFF00B0FF),
        lightPrimaryColor = Color(0xFF0284C7),
        darkBgColor = Color(0xFF030A1A),
        darkSurfaceColor = Color(0xFF0B1733),
        darkSurfaceVariant = Color(0xFF14264E),
        lightBgColor = Color(0xFFEFF6FF),
        lightSurfaceColor = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFDBEAFE),
        accentColor = Color(0xFF38BDF8),
        onPrimaryDark = Color(0xFF000000),
        onPrimaryLight = Color(0xFFFFFFFF)
    ),
    ROYAL_GOLD(
        id = "ROYAL_GOLD",
        titleAr = "الذهب الملكي 👑",
        titleEn = "Royal Gold",
        primaryColor = Color(0xFFFFB300),
        lightPrimaryColor = Color(0xFFB45309),
        darkBgColor = Color(0xFF0D0B06),
        darkSurfaceColor = Color(0xFF1C170E),
        darkSurfaceVariant = Color(0xFF2E2517),
        lightBgColor = Color(0xFFFFFDF7),
        lightSurfaceColor = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFF7ECD8),
        accentColor = Color(0xFFFFD54F),
        onPrimaryDark = Color(0xFF000000),
        onPrimaryLight = Color(0xFFFFFFFF)
    ),
    EMERALD_GOLD(
        id = "EMERALD_GOLD",
        titleAr = "الزمرد والألماس 💎",
        titleEn = "Emerald & Gold",
        primaryColor = Color(0xFF10E096),
        lightPrimaryColor = Color(0xFF047857),
        darkBgColor = Color(0xFF011A13),
        darkSurfaceColor = Color(0xFF042E22),
        darkSurfaceVariant = Color(0xFF084534),
        lightBgColor = Color(0xFFF0FDF4),
        lightSurfaceColor = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFDCFCE7),
        accentColor = Color(0xFF34D399),
        onPrimaryDark = Color(0xFF000000),
        onPrimaryLight = Color(0xFFFFFFFF)
    ),
    MIDNIGHT_ONYX(
        id = "MIDNIGHT_ONYX",
        titleAr = "أونيكس الفاخر 🖤",
        titleEn = "Midnight Onyx",
        primaryColor = Color(0xFFE2E8F0),
        lightPrimaryColor = Color(0xFF0F172A),
        darkBgColor = Color(0xFF000000),
        darkSurfaceColor = Color(0xFF141414),
        darkSurfaceVariant = Color(0xFF242424),
        lightBgColor = Color(0xFFF8FAFC),
        lightSurfaceColor = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFE2E8F0),
        accentColor = Color(0xFF94A3B8),
        onPrimaryDark = Color(0xFF000000),
        onPrimaryLight = Color(0xFFFFFFFF)
    ),
    VELVET_PURPLE(
        id = "VELVET_PURPLE",
        titleAr = "المخمل البنفسجي 🔮",
        titleEn = "Velvet Royal",
        primaryColor = Color(0xFFC084FC),
        lightPrimaryColor = Color(0xFF7E22CE),
        darkBgColor = Color(0xFF0D051A),
        darkSurfaceColor = Color(0xFF1B0E33),
        darkSurfaceVariant = Color(0xFF2C1850),
        lightBgColor = Color(0xFFFAF5FF),
        lightSurfaceColor = Color(0xFFFFFFFF),
        lightSurfaceVariant = Color(0xFFF3E8FF),
        accentColor = Color(0xFFE879F9),
        onPrimaryDark = Color(0xFF000000),
        onPrimaryLight = Color(0xFFFFFFFF)
    );

    fun localizedTitle(language: com.example.util.AppLanguage): String =
        com.example.util.Translations.get("theme.$id", language)

    companion object {
        fun fromId(id: String?): AppThemePalette = entries.find { it.id == id } ?: OOZOX_INFINITY
    }
}

@Composable
fun GoldChatTheme(
    darkTheme: Boolean = false,
    palette: AppThemePalette = AppThemePalette.OOZOX_INFINITY,
    fontScale: AppFontScale = AppFontScale.STANDARD,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.primaryColor,
            onPrimary = palette.onPrimaryDark,
            primaryContainer = palette.primaryColor.copy(alpha = 0.20f),
            onPrimaryContainer = palette.accentColor,
            secondary = palette.accentColor,
            onSecondary = Color.Black,
            secondaryContainer = palette.darkSurfaceVariant,
            onSecondaryContainer = Color(0xFFF8FAFC),
            tertiary = Color(0xFFFBBF24), // Warm Bullion Gold
            onTertiary = Color(0xFF000000),
            tertiaryContainer = Color(0xFF2E2305),
            onTertiaryContainer = Color(0xFFFDE68A),
            background = palette.darkBgColor,
            onBackground = Color(0xFFF8FAFC),
            surface = palette.darkSurfaceColor,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = palette.darkSurfaceVariant,
            onSurfaceVariant = Color(0xFFCBD5E1),
            outline = palette.primaryColor.copy(alpha = 0.35f),
            outlineVariant = Color(0xFF1E293B),
            error = Color(0xFFF87171),
            onError = Color(0xFF000000),
            errorContainer = Color(0xFF7F1D1D).copy(alpha = 0.45f),
            onErrorContainer = Color(0xFFFECACA)
        )
    } else {
        lightColorScheme(
            primary = palette.lightPrimaryColor,
            onPrimary = palette.onPrimaryLight,
            primaryContainer = palette.lightPrimaryColor.copy(alpha = 0.12f),
            onPrimaryContainer = palette.lightPrimaryColor,
            secondary = palette.primaryColor,
            onSecondary = Color.White,
            secondaryContainer = palette.lightSurfaceVariant,
            onSecondaryContainer = Color(0xFF0F172A),
            tertiary = Color(0xFFB45309), // Rich Gold Amber (Contrast 5.8:1 on white)
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFEF3C7),
            onTertiaryContainer = Color(0xFF78350F),
            background = palette.lightBgColor,
            onBackground = Color(0xFF0F172A),
            surface = palette.lightSurfaceColor,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = palette.lightSurfaceVariant,
            onSurfaceVariant = Color(0xFF334155),
            outline = palette.lightPrimaryColor.copy(alpha = 0.35f),
            outlineVariant = Color(0xFFE2E8F0),
            error = Color(0xFFDC2626), // High contrast error crimson (>4.7:1 on white)
            onError = Color.White,
            errorContainer = Color(0xFFFEE2E2),
            onErrorContainer = Color(0xFF991B1B)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val dialogWindow = (view.parent as? DialogWindowProvider)?.window
            val activity = view.context.findActivity()
            val window = dialogWindow ?: activity?.window

            if (dialogWindow == null && activity is ComponentActivity) {
                activity.enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.argb(0x45, 0x0B, 0x0F, 0x17))
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.argb(0x16, 0x0F, 0x17, 0x2A),
                            android.graphics.Color.argb(0x2E, 0x0F, 0x17, 0x2A)
                        )
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.argb(0x55, 0x07, 0x0B, 0x12))
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.argb(0x14, 0x0F, 0x17, 0x2A),
                            android.graphics.Color.argb(0x2E, 0x0F, 0x17, 0x2A)
                        )
                    }
                )
            }

            if (window != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    @Suppress("DEPRECATION")
                    window.statusBarColor = if (darkTheme) {
                        android.graphics.Color.argb(0x45, 0x0B, 0x0F, 0x17)
                    } else {
                        android.graphics.Color.argb(0x16, 0x0F, 0x17, 0x2A)
                    }
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = if (darkTheme) {
                        android.graphics.Color.argb(0x55, 0x07, 0x0B, 0x12)
                    } else {
                        android.graphics.Color.argb(0x14, 0x0F, 0x17, 0x2A)
                    }
                }
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalAppFontScale provides fontScale
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getScaledTypography(fontScale),
            content = content
        )
    }
}

