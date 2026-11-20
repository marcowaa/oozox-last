package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standardized Design System Icon Sizes
 * Strictly derived from the 8pt geometric modular scale.
 */
enum class AppIconSize(val dp: Dp) {
    XS(12.dp),
    SM(16.dp),
    MD(24.dp),
    LG(32.dp),
    XL(48.dp)
}

/**
 * Reusable, High-Precision Universal Icon Component
 *
 * @param imageVector The ImageVector asset or AppIcons vector token.
 * @param contentDescription Optional accessibility label (TalkBack).
 * @param modifier Custom Modifier to apply to the icon layout.
 * @param size Standardized IconSize enum token (SM=16.dp, MD=24.dp, LG=32.dp). Defaults to MD (24.dp).
 * @param tint Dynamic color tint. When Color.Unspecified, resolves dynamically to LocalContentColor.current.
 */
@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: AppIconSize = AppIconSize.MD,
    tint: Color = Color.Unspecified
) {
    val resolvedTint = if (tint == Color.Unspecified) LocalContentColor.current else tint

    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = resolvedTint,
        modifier = modifier.size(size.dp)
    )
}
