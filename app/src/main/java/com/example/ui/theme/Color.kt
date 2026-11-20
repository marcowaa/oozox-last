package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme

// --- OOZOX Brand Official Color Palette (from official identity) ---
val OozoxTeal = Color(0xFF00BFA5)         // Emerald Teal / Cyan Accent from upper-right curve (#00BFA5)
val OozoxTealLight = Color(0xFF00E5C0)    // Glow highlight (#00E5C0)
val OozoxNavyBody = Color(0xFF141A29)     // Infinity loop dark matte body (#141A29)
val OozoxObsidian = Color(0xFF0B0F17)     // App Deep Canvas Background (#0B0F17)
val OozoxSlateCard = Color(0xFF131926)    // Surface Card Background (#131926)
val OozoxSlateElevated = Color(0xFF1C2538)// Elevated Surface Variant (#1C2538)
val OozoxPorcelain = Color(0xFFF4F6F9)    // Pearl White Card Background (#F4F6F9)
val OozoxPlatinum = Color(0xFFF8FAFC)     // Crisp Ice White Text (#F8FAFC)
val OozoxMuted = Color(0xFF94A3B8)        // Slate Muted Text (#94A3B8)

// Brand Gradients
val OozoxInfinityGradient = Brush.horizontalGradient(
    colors = listOf(OozoxNavyBody, OozoxTeal, OozoxTealLight)
)
val OozoxCardGradient = Brush.verticalGradient(
    colors = listOf(OozoxSlateElevated, OozoxSlateCard)
)

// Legacy / Gold & Financial Constants (Synchronized with official identity)
val GoldPrimary = OozoxTeal              // Main brand primary accent
val GoldDark = Color(0xFF00897B)
val GoldAccent = OozoxTealLight
val GoldLight = OozoxPlatinum

val adaptiveGoldPrimary: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

val adaptiveGoldLight: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) GoldLight else Color(0xFF0F172A)

val adaptiveGoldDark: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) GoldDark else MaterialTheme.colorScheme.surfaceVariant

val adaptiveAccent: Color
    @Composable
    get() = MaterialTheme.colorScheme.secondary

val adaptiveOnPrimary: Color
    @Composable
    get() = MaterialTheme.colorScheme.onPrimary

val adaptiveOnSurface: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurface

val adaptiveOnSurfaceVariant: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val adaptiveSurface: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface

val adaptiveSurfaceVariant: Color
    @Composable
    get() = MaterialTheme.colorScheme.surfaceVariant

val adaptiveBorder: Color
    @Composable
    get() = MaterialTheme.colorScheme.outlineVariant

// WCAG AA Compliant Adaptive Semantic Status Colors
val adaptiveStatusGreen: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF10B981) else Color(0xFF047857)

val adaptiveStatusRed: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFF87171) else Color(0xFFB91C1C)

val adaptiveStatusOrange: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFFBBF24) else Color(0xFFB45309)

val adaptiveStatusBlue: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF38BDF8) else Color(0xFF0284C7)

val adaptiveRealGold: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFFCD34D) else Color(0xFFB45309)

val adaptivePlatinumSilver: Color
    @Composable
    get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFE2E8F0) else Color(0xFF475569)

val isDarkThemeActive: Boolean
    @Composable
    get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * OOZOX Enterprise Color System - Centralized Design Tokens (WCAG AA Compliant)
 * Adheres to 60-30-10 distribution: 60% Neutral Canvas, 30% Structural Elements, 10% Intentional Brand Accents.
 */
object AppColors {
    val primary: Color @Composable get() = MaterialTheme.colorScheme.primary
    val onPrimary: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
    val primaryContainer: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer: Color @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer

    val secondary: Color @Composable get() = MaterialTheme.colorScheme.secondary
    val onSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSecondary
    val secondaryContainer: Color @Composable get() = MaterialTheme.colorScheme.secondaryContainer
    val onSecondaryContainer: Color @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer

    val tertiary: Color @Composable get() = MaterialTheme.colorScheme.tertiary
    val onTertiary: Color @Composable get() = MaterialTheme.colorScheme.onTertiary
    val tertiaryContainer: Color @Composable get() = MaterialTheme.colorScheme.tertiaryContainer
    val onTertiaryContainer: Color @Composable get() = MaterialTheme.colorScheme.onTertiaryContainer

    val background: Color @Composable get() = MaterialTheme.colorScheme.background
    val onBackground: Color @Composable get() = MaterialTheme.colorScheme.onBackground

    val surface: Color @Composable get() = MaterialTheme.colorScheme.surface
    val onSurface: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val surfaceVariant: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

    val outline: Color @Composable get() = MaterialTheme.colorScheme.outline
    val outlineVariant: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant

    val error: Color @Composable get() = MaterialTheme.colorScheme.error
    val onError: Color @Composable get() = MaterialTheme.colorScheme.onError
    val errorContainer: Color @Composable get() = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer: Color @Composable get() = MaterialTheme.colorScheme.onErrorContainer

    // Semantic Status Tokens
    val success: Color @Composable get() = adaptiveStatusGreen
    val warning: Color @Composable get() = adaptiveStatusOrange
    val info: Color @Composable get() = adaptiveStatusBlue
    val goldAsset: Color @Composable get() = adaptiveRealGold
}

val RealGoldYellow = Color(0xFFFFD700)   // For physical gold bullion badges
val RealGoldAmber = Color(0xFFD97706)

val DarkBackground = OozoxObsidian
val DarkSurface = OozoxSlateCard
val DarkSurfaceVariant = OozoxSlateElevated

val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)

val TextPrimary = OozoxPlatinum
val TextSecondary = OozoxMuted

val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)

val StatusGreen = Color(0xFF10B981)
val StatusRed = Color(0xFFEF4444)
val StatusBlue = Color(0xFF009DFF)
val StatusOrange = Color(0xFFF59E0B)

