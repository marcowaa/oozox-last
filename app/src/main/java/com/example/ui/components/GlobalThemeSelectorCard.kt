package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LocalAppTheme
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.GoldPrimary
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage
import com.example.util.tr

/**
 * Global Theme Palette Selector Card
 * Displays selectable vibrant, high-contrast themes allowing the user
 * to visibly switch between OOZOX Obsidian, Royal Gold, Cyber Sapphire, Emerald, etc.
 */
@Composable
fun GlobalThemeSelectorCard(
    modifier: Modifier = Modifier
) {
    val themeController = LocalAppTheme.current
    val currentPalette = themeController.currentPalette
    val isDark = themeController.isDarkTheme
    val lang = LocalAppLanguage.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("global_theme_selector_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("theme.selector_title"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tr("theme.selector_sub"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // List of available palettes
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppThemePalette.entries.forEach { palette ->
                    val isSelected = palette == currentPalette
                    val previewBg = if (isDark) palette.darkBgColor else palette.lightBgColor
                    val previewSurface = if (isDark) palette.darkSurfaceColor else palette.lightSurfaceColor
                    val previewPrimary = if (isDark) palette.primaryColor else palette.lightPrimaryColor
                    val localizedName = palette.localizedTitle(lang)

                    Surface(
                        onClick = { themeController.setPalette(palette) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) previewPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) previewPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("theme_palette_option_${palette.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Palette preview circle dots
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.sweepGradient(
                                                listOf(
                                                    previewPrimary,
                                                    palette.accentColor,
                                                    previewSurface,
                                                    previewBg,
                                                    previewPrimary
                                                )
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = localizedName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) previewPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (lang != AppLanguage.ENGLISH && localizedName != palette.titleEn) {
                                        Text(
                                            text = palette.titleEn,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(previewPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = tr("common.selected"),
                                        tint = if (isDark) palette.onPrimaryDark else palette.onPrimaryLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
