package com.example.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * OOZOX Luxury Vector Icon System
 * Modern, minimalist, ultra-high-precision 24x24 vector icons
 * Engineered for zero overhead, pixel-perfect scalability, and premium branding.
 */
object AppIcons {

    // --- Bottom Navigation Core Suite ---

    /**
     * Modern Chat & Communications Dialogue Bubble
     */
    val Chats: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Chats",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Main Chat Bubble Body
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(20f, 2f)
                horizontalLineTo(4f)
                curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
                verticalLineTo(17f)
                curveTo(2f, 18.1f, 2.9f, 19f, 4f, 19f)
                horizontalLineTo(7f)
                verticalLineTo(22.5f)
                curveTo(7f, 22.89f, 7.45f, 23.09f, 7.74f, 22.83f)
                lineTo(12.2f, 19f)
                horizontalLineTo(20f)
                curveTo(21.1f, 19f, 22f, 18.1f, 22f, 17f)
                verticalLineTo(4f)
                curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f)
                close()
                // Minimalist Dialogue Indicator Slits
                moveTo(7.5f, 9f)
                curveTo(6.95f, 9f, 6.5f, 8.55f, 6.5f, 8f)
                curveTo(6.5f, 7.45f, 6.95f, 7f, 7.5f, 7f)
                horizontalLineTo(16.5f)
                curveTo(17.05f, 7f, 17.5f, 7.45f, 17.5f, 8f)
                curveTo(17.5f, 8.55f, 17.05f, 9f, 16.5f, 9f)
                horizontalLineTo(7.5f)
                close()
                moveTo(7.5f, 13.5f)
                curveTo(6.95f, 13.5f, 6.5f, 13.05f, 6.5f, 12.5f)
                curveTo(6.5f, 11.95f, 6.95f, 11.5f, 7.5f, 11.5f)
                horizontalLineTo(13.5f)
                curveTo(14.05f, 11.5f, 14.5f, 11.95f, 14.5f, 12.5f)
                curveTo(14.5f, 13.05f, 14.05f, 13.5f, 13.5f, 13.5f)
                horizontalLineTo(7.5f)
                close()
            }
        }.build()
    }

    /**
     * Modern Projects & Workspace Stack
     */
    val Projects: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Projects",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Upper Deck
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1f
            ) {
                moveTo(12f, 2f)
                lineTo(2.5f, 7f)
                lineTo(12f, 12f)
                lineTo(21.5f, 7f)
                lineTo(12f, 2f)
                close()
            }
            // Middle Isometric Layer
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.5f, 12.2f)
                lineTo(12f, 16.5f)
                lineTo(20.5f, 12.2f)
            }
            // Bottom Isometric Layer
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.5f, 17f)
                lineTo(12f, 21.3f)
                lineTo(20.5f, 17f)
            }
        }.build()
    }

    /**
     * Executive Business Center Briefcase Icon
     */
    val Business: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Business",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1f
            ) {
                moveTo(20f, 6f)
                horizontalLineTo(16f)
                verticalLineTo(4f)
                curveTo(16f, 2.9f, 15.1f, 2f, 14f, 2f)
                horizontalLineTo(10f)
                curveTo(8.9f, 2f, 8f, 2.9f, 8f, 4f)
                verticalLineTo(6f)
                horizontalLineTo(4f)
                curveTo(2.9f, 6f, 2f, 6.9f, 2f, 8f)
                verticalLineTo(19f)
                curveTo(2f, 20.1f, 2.9f, 21f, 4f, 21f)
                horizontalLineTo(20f)
                curveTo(21.1f, 21f, 22f, 20.1f, 22f, 19f)
                verticalLineTo(8f)
                curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
                close()
                moveTo(10f, 4f)
                horizontalLineTo(14f)
                verticalLineTo(6f)
                horizontalLineTo(10f)
                verticalLineTo(4f)
                close()
                moveTo(20f, 19f)
                horizontalLineTo(4f)
                verticalLineTo(13f)
                horizontalLineTo(9f)
                verticalLineTo(14f)
                curveTo(9f, 14.55f, 9.45f, 15f, 10f, 15f)
                horizontalLineTo(14f)
                curveTo(14.55f, 15f, 15f, 14.55f, 15f, 14f)
                verticalLineTo(13f)
                horizontalLineTo(20f)
                verticalLineTo(19f)
                close()
            }
        }.build()
    }

    /**
     * Luxury Gold Bullion / Ingot Facet
     */
    val Gold: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Gold",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Gold Ingot 3D Trapeze
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1f
            ) {
                moveTo(5f, 5f)
                horizontalLineTo(19f)
                lineTo(22f, 11f)
                horizontalLineTo(2f)
                lineTo(5f, 5f)
                close()
                moveTo(2f, 13f)
                horizontalLineTo(22f)
                lineTo(19.5f, 19f)
                horizontalLineTo(4.5f)
                lineTo(2f, 13f)
                close()
            }
            // Premium Specular Highlight
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7f, 8f)
                lineTo(17f, 8f)
            }
        }.build()
    }

    /**
     * Minimalist Financial Ledger / Invoice Receipt
     */
    val Transactions: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Transactions",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1f
            ) {
                moveTo(18f, 2f)
                horizontalLineTo(6f)
                curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
                verticalLineTo(21.2f)
                lineTo(7f, 19.5f)
                lineTo(10f, 21.2f)
                lineTo(12f, 20f)
                lineTo(14f, 21.2f)
                lineTo(17f, 19.5f)
                lineTo(20f, 21.2f)
                verticalLineTo(4f)
                curveTo(20f, 2.9f, 19.1f, 2f, 18f, 2f)
                close()
                // Receipt Slits & Financial Bars
                moveTo(8f, 7f)
                horizontalLineTo(16f)
                verticalLineTo(8.8f)
                horizontalLineTo(8f)
                verticalLineTo(7f)
                close()
                moveTo(8f, 10.8f)
                horizontalLineTo(16f)
                verticalLineTo(12.6f)
                horizontalLineTo(8f)
                verticalLineTo(10.8f)
                close()
                moveTo(8f, 14.6f)
                horizontalLineTo(12.5f)
                verticalLineTo(16.4f)
                horizontalLineTo(8f)
                verticalLineTo(14.6f)
                close()
            }
        }.build()
    }

    /**
     * Modern Executive Profile Avatar Silhouette
     */
    val Profile: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Profile",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Head
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1f
            ) {
                moveTo(12f, 12f)
                curveTo(14.48f, 12f, 16.5f, 9.98f, 16.5f, 7.5f)
                curveTo(16.5f, 5.02f, 14.48f, 3f, 12f, 3f)
                curveTo(9.52f, 3f, 7.5f, 5.02f, 7.5f, 7.5f)
                curveTo(7.5f, 9.98f, 9.52f, 12f, 12f, 12f)
                close()
                // Torso with dynamic contour
                moveTo(12f, 14.5f)
                curveTo(7.98f, 14.5f, 4.5f, 16.5f, 4.5f, 19.8f)
                verticalLineTo(21f)
                horizontalLineTo(19.5f)
                verticalLineTo(19.8f)
                curveTo(19.5f, 16.5f, 16.02f, 14.5f, 12f, 14.5f)
                close()
            }
        }.build()
    }

    // --- Executive Services & Drawer Suite ---

    /**
     * Modern AI Magic Sparkle
     */
    val AIAccountant: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.AIAccountant",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Major 4-Point Star
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1f
            ) {
                moveTo(12f, 2f)
                curveTo(12f, 7.5f, 16.5f, 12f, 22f, 12f)
                curveTo(16.5f, 12f, 12f, 16.5f, 12f, 22f)
                curveTo(12f, 16.5f, 7.5f, 12f, 2f, 12f)
                curveTo(7.5f, 12f, 12f, 7.5f, 12f, 2f)
                close()
            }
            // Secondary Minor Spark
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.85f
            ) {
                moveTo(19f, 3f)
                curveTo(19f, 4.6f, 20.4f, 6f, 22f, 6f)
                curveTo(20.4f, 6f, 19f, 7.4f, 19f, 9f)
                curveTo(19f, 7.4f, 17.6f, 6f, 16f, 6f)
                curveTo(17.6f, 6f, 19f, 4.6f, 19f, 3f)
                close()
            }
        }.build()
    }

    /**
     * Corporate Enterprise Tower
     */
    val Corporate: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Corporate",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1f
            ) {
                moveTo(10f, 2f)
                horizontalLineTo(4f)
                curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f)
                verticalLineTo(21f)
                horizontalLineTo(22f)
                verticalLineTo(9f)
                curveTo(22f, 7.9f, 21.1f, 7f, 20f, 7f)
                horizontalLineTo(12f)
                verticalLineTo(4f)
                curveTo(12f, 2.9f, 11.1f, 2f, 10f, 2f)
                close()
                // Windows Grid
                moveTo(6f, 6f)
                horizontalLineTo(8f)
                verticalLineTo(8f)
                horizontalLineTo(6f)
                verticalLineTo(6f)
                close()
                moveTo(6f, 11f)
                horizontalLineTo(8f)
                verticalLineTo(13f)
                horizontalLineTo(6f)
                verticalLineTo(11f)
                close()
                moveTo(6f, 16f)
                horizontalLineTo(8f)
                verticalLineTo(18f)
                horizontalLineTo(6f)
                verticalLineTo(16f)
                close()
                moveTo(14f, 11f)
                horizontalLineTo(18f)
                verticalLineTo(13f)
                horizontalLineTo(14f)
                verticalLineTo(11f)
                close()
                moveTo(14f, 16f)
                horizontalLineTo(18f)
                verticalLineTo(18f)
                horizontalLineTo(14f)
                verticalLineTo(16f)
                close()
            }
        }.build()
    }

    /**
     * Royal VIP Crown
     */
    val Crown: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Crown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1f
            ) {
                moveTo(3f, 19f)
                horizontalLineTo(21f)
                verticalLineTo(21f)
                horizontalLineTo(3f)
                verticalLineTo(19f)
                close()
                moveTo(3f, 17f)
                lineTo(5f, 6f)
                lineTo(9.5f, 11.5f)
                lineTo(12f, 4f)
                lineTo(14.5f, 11.5f)
                lineTo(19f, 6f)
                lineTo(21f, 17f)
                horizontalLineTo(3f)
                close()
            }
        }.build()
    }

    /**
     * Concentric Signal Radar / Proximity Beacon
     */
    val Radar: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Radar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Central Pulse Beacon
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(12f, 12f)
                moveToRelative(-2.5f, 0f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14.5f, 12f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 9.5f, 12f)
                close()
            }
            // Radar Inner Orbit
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 6.5f)
                arcTo(5.5f, 5.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17.5f, 12f)
                arcTo(5.5f, 5.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 17.5f)
            }
            // Radar Outer Sweep
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 2.5f)
                arcTo(9.5f, 9.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 2.5f, 12f)
            }
        }.build()
    }

    /**
     * Modern Command Terminal Window
     */
    val Terminal: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Terminal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(20f, 4f)
                horizontalLineTo(4f)
                curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
                verticalLineTo(18f)
                curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                horizontalLineTo(20f)
                curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                verticalLineTo(6f)
                curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f)
                close()
                // Prompt Chevron
                moveTo(6.5f, 8.5f)
                lineTo(10f, 12f)
                lineTo(6.5f, 15.5f)
                lineTo(5.5f, 14.5f)
                lineTo(8f, 12f)
                lineTo(5.5f, 9.5f)
                lineTo(6.5f, 8.5f)
                close()
                // Cursor Bar
                moveTo(11.5f, 14.5f)
                horizontalLineTo(16.5f)
                verticalLineTo(16f)
                horizontalLineTo(11.5f)
                verticalLineTo(14.5f)
                close()
            }
        }.build()
    }

    /**
     * Encrypted Cloud Vault
     */
    val EncryptedVault: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.EncryptedVault",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(19.35f, 10.04f)
                curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
                curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
                curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
                curveTo(0f, 17.31f, 2.69f, 20f, 6f, 20f)
                horizontalLineTo(19f)
                curveTo(21.76f, 20f, 24f, 17.76f, 24f, 15f)
                curveTo(24f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
                close()
                // Keyhole in Vault
                moveTo(12f, 11f)
                curveTo(13.1f, 11f, 14f, 11.9f, 14f, 13f)
                curveTo(14f, 13.72f, 13.61f, 14.35f, 13f, 14.7f)
                verticalLineTo(17.5f)
                horizontalLineTo(11f)
                verticalLineTo(14.7f)
                curveTo(10.39f, 14.35f, 10f, 13.72f, 10f, 13f)
                curveTo(10f, 11.9f, 10.9f, 11f, 12f, 11f)
                close()
            }
        }.build()
    }

    /**
     * Modern Biometric Fingerprint Arch
     */
    val Biometric: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Biometric",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                moveTo(22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                moveTo(6.5f, 12f)
                curveTo(6.5f, 8.96f, 8.96f, 6.5f, 12f, 6.5f)
                curveTo(15.04f, 6.5f, 17.5f, 8.96f, 17.5f, 12f)
                verticalLineTo(17.5f)
                moveTo(12f, 10f)
                curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
                verticalLineTo(19.5f)
                moveTo(14f, 12f)
                verticalLineTo(21.5f)
            }
        }.build()
    }

    /**
     * Minimalist Asymmetric Luxury Menu Burger
     */
    val Menu: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Menu",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3f, 6f)
                lineTo(21f, 6f)
                moveTo(3f, 12f)
                lineTo(15f, 12f)
                moveTo(3f, 18f)
                lineTo(18.5f, 18f)
            }
        }.build()
    }

    /**
     * Sidebar Collapse / Back
     */
    val MenuClose: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.MenuClose",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 6f)
                lineTo(6f, 18f)
                moveTo(6f, 6f)
                lineTo(18f, 18f)
            }
        }.build()
    }

    /**
     * Minimalist Verified Badge
     */
    val Verified: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Verified",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(23f, 12f)
                lineTo(20.56f, 9.22f)
                lineTo(20.9f, 5.54f)
                lineTo(17.29f, 4.72f)
                lineTo(15.4f, 1.54f)
                lineTo(12f, 3f)
                lineTo(8.6f, 1.54f)
                lineTo(6.71f, 4.72f)
                lineTo(3.1f, 5.53f)
                lineTo(3.44f, 9.21f)
                lineTo(1f, 12f)
                lineTo(3.44f, 14.78f)
                lineTo(3.1f, 18.47f)
                lineTo(6.71f, 19.29f)
                lineTo(8.6f, 22.47f)
                lineTo(12f, 21f)
                lineTo(15.4f, 22.46f)
                lineTo(17.29f, 19.28f)
                lineTo(20.9f, 18.46f)
                lineTo(20.56f, 14.78f)
                lineTo(23f, 12f)
                close()
                // Checkmark
                moveTo(10.09f, 16.72f)
                lineTo(6.29f, 12.91f)
                lineTo(7.7f, 11.5f)
                lineTo(10.09f, 13.89f)
                lineTo(16.59f, 7.39f)
                lineTo(18f, 8.81f)
                lineTo(10.09f, 16.72f)
                close()
            }
        }.build()
    }

    /**
     * Modern Crescent Moon (Dark Mode)
     */
    val Moon: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Moon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(12.3f, 2f)
                curveTo(6.78f, 2.05f, 2.3f, 6.55f, 2.3f, 12.1f)
                curveTo(2.3f, 17.65f, 6.8f, 22.1f, 12.35f, 22.1f)
                curveTo(16.58f, 22.1f, 20.2f, 19.46f, 21.57f, 15.5f)
                curveTo(16.63f, 16.03f, 12.18f, 12.17f, 12.18f, 7.23f)
                curveTo(12.18f, 5.25f, 12.82f, 3.42f, 13.91f, 1.9f)
                curveTo(13.38f, 1.96f, 12.84f, 2f, 12.3f, 2f)
                close()
            }
        }.build()
    }

    /**
     * Modern Sun with Clean Radial Rays (Light Mode)
     */
    val Sun: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Sun",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Solar Core
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(12f, 7f)
                curveTo(9.24f, 7f, 7f, 9.24f, 7f, 12f)
                curveTo(7f, 14.76f, 9.24f, 17f, 12f, 17f)
                curveTo(14.76f, 17f, 17f, 14.76f, 17f, 12f)
                curveTo(17f, 9.24f, 14.76f, 7f, 12f, 7f)
                close()
            }
            // Radial Rays
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 1.5f); lineTo(12f, 4f)
                moveTo(12f, 20f); lineTo(12f, 22.5f)
                moveTo(1.5f, 12f); lineTo(4f, 12f)
                moveTo(20f, 12f); lineTo(22.5f, 12f)
                moveTo(4.6f, 4.6f); lineTo(6.4f, 6.4f)
                moveTo(17.6f, 17.6f); lineTo(19.4f, 19.4f)
                moveTo(19.4f, 4.6f); lineTo(17.6f, 6.4f)
                moveTo(6.4f, 17.6f); lineTo(4.6f, 19.4f)
            }
        }.build()
    }

    /**
     * Minimalist 3-Dot Vertical Overflow Menu
     */
    val MoreVert: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.MoreVert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 6f)
                moveToRelative(-2f, 0f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14f, 6f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10f, 6f)
                close()
                moveTo(12f, 12f)
                moveToRelative(-2f, 0f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14f, 12f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10f, 12f)
                close()
                moveTo(12f, 18f)
                moveToRelative(-2f, 0f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14f, 18f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10f, 18f)
                close()
            }
        }.build()
    }

    /**
     * Modern Folder with Centered Plus Symbol
     */
    val FolderPlus: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.FolderPlus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(20f, 6f)
                horizontalLineTo(12f)
                lineTo(10f, 4f)
                horizontalLineTo(4f)
                curveTo(2.9f, 4f, 2.01f, 4.9f, 2.01f, 6f)
                lineTo(2f, 18f)
                curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                horizontalLineTo(20f)
                curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                verticalLineTo(8f)
                curveTo(22f, 6.9f, 21.1f, 6f, 20f, 6f)
                close()
                // Plus inside folder
                moveTo(12f, 10f)
                verticalLineTo(16f)
                moveTo(9f, 13f)
                horizontalLineTo(15f)
            }
        }.build()
    }

    /**
     * Sleek Precision Search Lens
     */
    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(11f, 11f)
                moveToRelative(-7f, 0f)
                arcTo(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, 18f, 11f)
                arcTo(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, 4f, 11f)
                close()
                moveTo(16f, 16f)
                lineTo(21.5f, 21.5f)
            }
        }.build()
    }

    /**
     * Modern Tuning Equalizer Sliders (Filter)
     */
    val Filter: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Filter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Slider 1
                moveTo(4f, 6f); lineTo(20f, 6f)
                // Slider 2
                moveTo(4f, 12f); lineTo(20f, 12f)
                // Slider 3
                moveTo(4f, 18f); lineTo(20f, 18f)
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(8f, 6f)
                moveToRelative(-2.5f, 0f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10.5f, 6f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 5.5f, 6f)
                close()
                moveTo(16f, 12f)
                moveToRelative(-2.5f, 0f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 18.5f, 12f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 13.5f, 12f)
                close()
                moveTo(10f, 18f)
                moveToRelative(-2.5f, 0f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12.5f, 18f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 7.5f, 18f)
                close()
            }
        }.build()
    }

    /**
     * Luxury Security Shield
     */
    val Shield: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Shield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                lineTo(4f, 5f)
                verticalLineTo(11.09f)
                curveTo(4f, 16.14f, 7.41f, 20.85f, 12f, 22f)
                curveTo(16.59f, 20.85f, 20f, 16.14f, 20f, 11.09f)
                verticalLineTo(5f)
                lineTo(12f, 2f)
                close()
            }
        }.build()
    }

    /**
     * Modern Minimalist Padlock
     */
    val Lock: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Lock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(18f, 8f)
                horizontalLineTo(17f)
                verticalLineTo(6f)
                curveTo(17f, 3.24f, 14.76f, 1f, 12f, 1f)
                curveTo(9.24f, 1f, 7f, 3.24f, 7f, 6f)
                verticalLineTo(8f)
                horizontalLineTo(6f)
                curveTo(4.9f, 8f, 4f, 8.9f, 4f, 10f)
                verticalLineTo(20f)
                curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
                horizontalLineTo(18f)
                curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
                verticalLineTo(10f)
                curveTo(20f, 8.9f, 19.1f, 8f, 18f, 8f)
                close()
                moveTo(9f, 6f)
                curveTo(9f, 4.34f, 10.34f, 3f, 12f, 3f)
                curveTo(13.66f, 3f, 15f, 4.34f, 15f, 6f)
                verticalLineTo(8f)
                horizontalLineTo(9f)
                verticalLineTo(6f)
                close()
            }
        }.build()
    }

    /**
     * Minimalist Clean Single Check
     */
    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.5f, 12.5f)
                lineTo(9.5f, 17.5f)
                lineTo(19.5f, 6.5f)
            }
        }.build()
    }

    /**
     * Precision Double Check (Delivered & Read)
     */
    val DoubleCheck: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.DoubleCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // First Tick
                moveTo(2f, 12.5f)
                lineTo(6.5f, 17f)
                lineTo(16f, 6.5f)
                // Second Tick
                moveTo(8.5f, 12.5f)
                lineTo(12.5f, 17f)
                lineTo(22f, 6.5f)
            }
        }.build()
    }

    /**
     * Modern Paper Plane Send Arrow
     */
    val Send: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Send",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(2.01f, 21f)
                lineTo(23f, 12f)
                lineTo(2.01f, 3f)
                lineTo(2f, 10f)
                lineTo(17f, 12f)
                lineTo(2f, 14f)
                close()
            }
        }.build()
    }

    /**
     * Minimalist Studio Microphone
     */
    val Mic: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Mic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 14f)
                curveTo(13.66f, 14f, 15f, 12.66f, 15f, 11f)
                verticalLineTo(5f)
                curveTo(15f, 3.34f, 13.66f, 2f, 12f, 2f)
                curveTo(10.34f, 2f, 9f, 3.34f, 9f, 5f)
                verticalLineTo(11f)
                curveTo(9f, 12.66f, 10.34f, 14f, 12f, 14f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5.5f, 10f)
                curveTo(5.5f, 13.58f, 8.42f, 16.5f, 12f, 16.5f)
                curveTo(15.58f, 16.5f, 18.5f, 13.58f, 18.5f, 10f)
                moveTo(12f, 16.5f)
                lineTo(12f, 21.5f)
            }
        }.build()
    }

    /**
     * Clean Circular Synchronization
     */
    val Sync: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppIcons.Sync",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 4f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20f, 12f)
                lineTo(22f, 10f)
                moveTo(12f, 20f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, 12f)
                lineTo(2f, 14f)
            }
        }.build()
    }
}
