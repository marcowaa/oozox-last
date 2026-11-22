package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.UserEntity
import com.example.data.model.UserType
import com.example.util.*

private fun parseStudioHex(hex: String, fallback: Color): Color =
    try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }

/**
 * Interactive Client Business Card Canvas supporting custom photo backgrounds, text size/color styling,
 * custom links with deep-link keywords, embedded QR matrix, and click-to-open Domain/App landing.
 */
@Composable
fun ClientCardLivePreview(
    user: UserEntity,
    config: ClientCardStyleConfig,
    trustScore: Int,
    reliabilitySummary: String,
    onCardClick: () -> Unit,
    onLinkClick: (ClientCardLinkItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val startColor = remember(config.presetTheme) { parseStudioHex(config.presetTheme.startHex, Color(0xFF0F172A)) }
    val endColor = remember(config.presetTheme) { parseStudioHex(config.presetTheme.endHex, Color(0xFF1E293B)) }
    val textColor = remember(config.textColorHex) { parseStudioHex(config.textColorHex, Color.White) }
    val accentColor = remember(config.accentColorHex) { parseStudioHex(config.accentColorHex, Color(0xFFD4AF37)) }

    val scaleFactor = when (config.fontScaleLevel) {
        0 -> 0.88f
        2 -> 1.14f
        else -> 1.0f
    }

    val scrimColor = remember(textColor, config.overlayAlpha) {
        if (textColor.luminance() > 0.5f) Color.Black.copy(alpha = config.overlayAlpha)
        else Color.White.copy(alpha = config.overlayAlpha)
    }

    val handle = user.username.ifBlank { user.id.take(8) }
    val domainCardUrl = remember(handle) { ClientCardDeepLinkRouter.domainCardUrl(handle) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f)),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("client_card_live_preview")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 1. Background Layer: Photo or Designer Gradient
            if (!config.backgroundImageUri.isNullOrBlank()) {
                AsyncImage(
                    model = config.backgroundImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(scrimColor)
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Brush.linearGradient(listOf(startColor, endColor)))
                )
            }

            // 2. Foreground Card Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header: Avatar, Name, Role, Verified, Trust Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        OozoxAvatarImage(
                            model = user.avatarUri,
                            name = config.displayName,
                            size = 50.dp,
                            isTrader = user.type == UserType.GOLD_TRADER.name,
                            contentDescription = config.displayName
                        )
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = config.displayName,
                                    color = textColor,
                                    fontSize = (16f * scaleFactor).sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (user.isVerified || user.verificationStatus == "APPROVED") {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = "@$handle • ${config.roleTitle}",
                                color = accentColor,
                                fontSize = (11.5f * scaleFactor).sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = trFmt("card_studio.trust_pill_fmt", "score" to trustScore),
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (config.includeBio && config.bioText.isNotBlank()) {
                    Text(
                        text = config.bioText,
                        color = textColor.copy(alpha = 0.88f),
                        fontSize = (12f * scaleFactor).sp,
                        lineHeight = (17f * scaleFactor).sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider(color = accentColor.copy(alpha = 0.3f))

                // Info Rows
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (config.includeStore && config.storeText.isNotBlank()) {
                        CardMetaRow(
                            icon = Icons.Default.Storefront,
                            text = config.storeText,
                            textColor = textColor,
                            accentColor = accentColor,
                            fontSize = (12f * scaleFactor).sp
                        )
                    }
                    if (config.includePhone && config.phoneText.isNotBlank()) {
                        CardMetaRow(
                            icon = Icons.Default.Phone,
                            text = config.phoneText,
                            textColor = textColor,
                            accentColor = accentColor,
                            fontSize = (12f * scaleFactor).sp
                        )
                    }
                    if (config.includeReliability) {
                        CardMetaRow(
                            icon = Icons.Default.VerifiedUser,
                            text = reliabilitySummary,
                            textColor = textColor,
                            accentColor = accentColor,
                            fontSize = (11.5f * scaleFactor).sp
                        )
                    }
                }

                // Custom Clickable Links & Deep-Link Keywords
                if (config.customLinks.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        config.customLinks.take(4).forEach { link ->
                            Surface(
                                onClick = { onLinkClick(link) },
                                shape = RoundedCornerShape(10.dp),
                                color = accentColor.copy(alpha = 0.14f),
                                border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = when (link.kind) {
                                                ClientCardLinkKind.APP_DEEP_LINK -> Icons.Default.PhoneAndroid
                                                ClientCardLinkKind.DOMAIN_CARD -> Icons.Default.Language
                                                ClientCardLinkKind.WHATSAPP -> Icons.Default.Chat
                                                ClientCardLinkKind.INSTAGRAM -> Icons.Default.PhotoCamera
                                                ClientCardLinkKind.WEBSITE -> Icons.Default.Link
                                            },
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = link.keyword,
                                            color = textColor,
                                            fontSize = (11.5f * scaleFactor).sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Domain & App Download Action Bar + Mini QR
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QrMatrixRenderer(
                            content = domainCardUrl,
                            size = 44.dp,
                            primaryColor = accentColor,
                            accentColor = textColor
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("card_studio.open_card_cta"),
                                color = textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = domainCardUrl,
                                    color = accentColor,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardMetaRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    textColor: Color,
    accentColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
        Text(
            text = text,
            color = textColor.copy(alpha = 0.92f),
            fontSize = fontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Interactive Studio Content for Tab 2 ("كارت العميل"):
 * - Live interactive preview with custom photo/theme background, text sizing/coloring, custom deep links
 * - Compact sub-tabs (البيانات | الخلفية والنص | الروابط | السوشيال)
 * - Direct Social Share buttons & Domain/App landing preview
 */
@Composable
fun ClientCardStudioTabContent(
    user: UserEntity,
    config: ClientCardStyleConfig,
    onConfigChange: (ClientCardStyleConfig) -> Unit,
    trustScore: Int,
    reliabilitySummary: String,
    projectsCount: Int = 0,
    completedDealsCount: Int = 0
) {
    val context = LocalContext.current
    val currentLanguage = LocalAppLanguage.current
    var studioMode by remember { mutableIntStateOf(0) } // 0: Data, 1: Theme/Text, 2: Links & DeepLinks, 3: Social & Domain
    var showLandingModal by remember { mutableStateOf(false) }
    var pendingBgEditUri by remember { mutableStateOf<Uri?>(null) }

    // Inline new link input states
    var newLinkKeyword by remember { mutableStateOf("") }
    var newLinkUrl by remember { mutableStateOf("") }
    var newLinkKind by remember { mutableStateOf(ClientCardLinkKind.WEBSITE) }

    // Zero-permission Photo Picker for card background photo -> opens uncropped zoom-to-frame editor
    val bgPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingBgEditUri = uri
        }
    }

    if (pendingBgEditUri != null) {
        CoverPhotoEditorDialog(
            imageUri = pendingBgEditUri!!,
            title = tr("card_studio.bg_crop_title"),
            frameMode = ImageFrameAspectMode.BANNER,
            onSaveCroppedCover = { savedPath ->
                val persisted = ClientCardPrefsStore.persistBackgroundPhoto(context, Uri.parse(savedPath), user.id)
                onConfigChange(config.copy(backgroundImageUri = persisted))
                pendingBgEditUri = null
            },
            onDismiss = { pendingBgEditUri = null }
        )
    }

    val handle = user.username.ifBlank { user.id.take(8) }
    val domainCardUrl = remember(handle) { ClientCardDeepLinkRouter.domainCardUrl(handle) }
    val appDeepLinkUrl = remember(handle) { ClientCardDeepLinkRouter.appDeepLinkUrl(handle) }
    val downloadUrl = remember(handle) { ClientCardDeepLinkRouter.domainDownloadUrl(handle) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Live Interactive Client Card
        ClientCardLivePreview(
            user = user,
            config = config,
            trustScore = trustScore,
            reliabilitySummary = reliabilitySummary,
            onCardClick = { showLandingModal = true },
            onLinkClick = { link ->
                ClientCardDeepLinkRouter.openExternalDeepLinkOrDomain(context, link.url, handle)
            }
        )

        // 2. Studio Tool Selector Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                0 to tr("card_studio.tab_data"),
                1 to tr("card_studio.tab_style"),
                2 to tr("card_studio.tab_links"),
                3 to tr("card_studio.tab_social")
            ).forEach { (idx, label) ->
                FilterChip(
                    selected = studioMode == idx,
                    onClick = { studioMode = idx },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        // 3. Studio Editor Panel
        when (studioMode) {
            0 -> {
                // DATA VISIBILITY + DIRECT INLINE TEXT EDITING
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = config.includeStore, onClick = { onConfigChange(config.copy(includeStore = !config.includeStore)) }, label = { Text(tr("card_studio.chip_store"), style = MaterialTheme.typography.labelSmall) })
                    FilterChip(selected = config.includeBio, onClick = { onConfigChange(config.copy(includeBio = !config.includeBio)) }, label = { Text(tr("card_studio.chip_bio"), style = MaterialTheme.typography.labelSmall) })
                    FilterChip(selected = config.includeReliability, onClick = { onConfigChange(config.copy(includeReliability = !config.includeReliability)) }, label = { Text(tr("card_studio.chip_rel"), style = MaterialTheme.typography.labelSmall) })
                    FilterChip(selected = config.includePhone, onClick = { onConfigChange(config.copy(includePhone = !config.includePhone)) }, label = { Text(tr("card_studio.chip_phone"), style = MaterialTheme.typography.labelSmall) })
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = config.displayName,
                        onValueChange = { onConfigChange(config.copy(displayName = it)) },
                        label = { Text(tr("card_studio.field_name"), style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = config.roleTitle,
                        onValueChange = { onConfigChange(config.copy(roleTitle = it)) },
                        label = { Text(tr("card_studio.field_role"), style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = config.storeText,
                            onValueChange = { onConfigChange(config.copy(storeText = it)) },
                            label = { Text(tr("card_studio.chip_store"), style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = config.phoneText,
                            onValueChange = { onConfigChange(config.copy(phoneText = it)) },
                            label = { Text(tr("card_studio.chip_phone"), style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = config.bioText,
                        onValueChange = { onConfigChange(config.copy(bioText = it)) },
                        label = { Text(tr("card_studio.field_bio"), style = MaterialTheme.typography.bodySmall) },
                        placeholder = { Text(tr("card_studio.field_bio_hint"), style = MaterialTheme.typography.bodySmall) },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            1 -> {
                // BACKGROUND IMAGE + THEME PRESETS + FONT SIZE + TEXT & ACCENT COLORS
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    bgPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_pick_card_bg_image")
                            ) {
                                Text(
                                    text = if (config.backgroundImageUri == null) tr("card_studio.pick_bg") else tr("card_studio.change_bg"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (!config.backgroundImageUri.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { onConfigChange(config.copy(backgroundImageUri = null)) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text(tr("card_studio.remove_bg"), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        if (!config.backgroundImageUri.isNullOrBlank()) {
                            Column {
                                Text(
                                    text = trFmt("card_studio.overlay_alpha_fmt", "pct" to (config.overlayAlpha * 100).toInt()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = config.overlayAlpha,
                                    onValueChange = { onConfigChange(config.copy(overlayAlpha = it)) },
                                    valueRange = 0.15f..0.88f
                                )
                            }
                        }

                        Text(tr("card_studio.preset_bgs"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ClientCardPresetTheme.entries.forEach { preset ->
                                val isSelected = config.presetTheme == preset && config.backgroundImageUri == null
                                Surface(
                                    onClick = {
                                        onConfigChange(
                                            config.copy(
                                                presetTheme = preset,
                                                backgroundImageUri = null,
                                                textColorHex = preset.defaultTextHex,
                                                accentColorHex = preset.defaultAccentHex
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(parseStudioHex(preset.startHex, Color.Black))
                                                .border(1.dp, parseStudioHex(preset.defaultAccentHex, Color.Gray), CircleShape)
                                        )
                                        Text(preset.localizedTitle(currentLanguage), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // Text Size Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tr("card_studio.font_size"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    0 to tr("card_studio.size_sm"),
                                    1 to tr("card_studio.size_md"),
                                    2 to tr("card_studio.size_lg")
                                ).forEach { (lvl, lbl) ->
                                    FilterChip(
                                        selected = config.fontScaleLevel == lvl,
                                        onClick = { onConfigChange(config.copy(fontScaleLevel = lvl)) },
                                        label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        // Text Color Swatches
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tr("card_studio.text_color"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("#FFFFFF", "#F8FAFC", "#0F172A", "#FACC15", "#38BDF8", "#34D399").forEach { hex ->
                                    val selected = config.textColorHex.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(parseStudioHex(hex, Color.White))
                                            .border(
                                                width = if (selected) 2.5.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8),
                                                shape = CircleShape
                                            )
                                            .clickable { onConfigChange(config.copy(textColorHex = hex)) }
                                    )
                                }
                            }
                        }

                        // Accent Color Swatches
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tr("card_studio.accent_color"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("#D4AF37", "#38BDF8", "#34D399", "#F472B6", "#FB923C", "#1E3A8A").forEach { hex ->
                                    val selected = config.accentColorHex.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(parseStudioHex(hex, Color.Yellow))
                                            .border(
                                                width = if (selected) 2.5.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8),
                                                shape = CircleShape
                                            )
                                            .clickable { onConfigChange(config.copy(accentColorHex = hex)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // CUSTOM LINKS + DEEP-LINK KEYWORDS MANAGER
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = tr("card_studio.add_link_title"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val appLinkDefaultKw = tr("card_studio.dl_app_kw")
                        val domainLinkDefaultKw = tr("card_studio.dl_domain_kw")
                        val waLinkDefaultKw = tr("card_studio.dl_wa_kw")

                        // Quick Deep Link Templates
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    val item = ClientCardLinkItem(
                                        keyword = appLinkDefaultKw,
                                        url = "oozox://card/$handle",
                                        kind = ClientCardLinkKind.APP_DEEP_LINK
                                    )
                                    onConfigChange(config.copy(customLinks = config.customLinks + item))
                                },
                                label = { Text(tr("card_studio.dl_app_chip"), style = MaterialTheme.typography.labelSmall) }
                            )
                            AssistChip(
                                onClick = {
                                    val item = ClientCardLinkItem(
                                        keyword = domainLinkDefaultKw,
                                        url = domainCardUrl,
                                        kind = ClientCardLinkKind.DOMAIN_CARD
                                    )
                                    onConfigChange(config.copy(customLinks = config.customLinks + item))
                                },
                                label = { Text(tr("card_studio.dl_domain_chip"), style = MaterialTheme.typography.labelSmall) }
                            )
                            AssistChip(
                                onClick = {
                                    newLinkKeyword = waLinkDefaultKw
                                    newLinkUrl = "https://wa.me/${config.phoneText.filter { it.isDigit() }}"
                                    newLinkKind = ClientCardLinkKind.WHATSAPP
                                },
                                label = { Text(tr("card_studio.dl_wa_chip"), style = MaterialTheme.typography.labelSmall) }
                            )
                        }

                        OutlinedTextField(
                            value = newLinkKeyword,
                            onValueChange = { newLinkKeyword = it },
                            label = { Text(tr("card_studio.link_kw_label"), style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newLinkUrl,
                                onValueChange = { newLinkUrl = it },
                                label = { Text(tr("card_studio.link_url_label"), style = MaterialTheme.typography.bodySmall) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (newLinkKeyword.isNotBlank() && newLinkUrl.isNotBlank()) {
                                        val inferredKind = when {
                                            newLinkUrl.startsWith("oozox://", true) -> ClientCardLinkKind.APP_DEEP_LINK
                                            newLinkUrl.contains("oozox.com", true) -> ClientCardLinkKind.DOMAIN_CARD
                                            newLinkUrl.contains("wa.me", true) || newLinkUrl.contains("whatsapp", true) -> ClientCardLinkKind.WHATSAPP
                                            newLinkUrl.contains("instagram", true) -> ClientCardLinkKind.INSTAGRAM
                                            else -> newLinkKind
                                        }
                                        val item = ClientCardLinkItem(
                                            keyword = newLinkKeyword.trim(),
                                            url = newLinkUrl.trim(),
                                            kind = inferredKind
                                        )
                                        onConfigChange(config.copy(customLinks = config.customLinks + item))
                                        newLinkKeyword = ""
                                        newLinkUrl = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(tr("card_studio.add_btn"), fontWeight = FontWeight.Bold)
                            }
                        }

                        if (config.customLinks.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            config.customLinks.forEach { link ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = link.keyword,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = link.url,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            onConfigChange(config.copy(customLinks = config.customLinks.filterNot { it.id == link.id }))
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = tr("card_studio.delete_link_cd"),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // SOCIAL MEDIA AESTHETIC SHARING + PROJECT DOMAIN & APP DOWNLOAD LINKS
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = tr("card_studio.social_title"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                SocialSharePlatform.WHATSAPP,
                                SocialSharePlatform.TELEGRAM,
                                SocialSharePlatform.INSTAGRAM,
                                SocialSharePlatform.FACEBOOK
                            ).forEach { platform ->
                                OutlinedButton(
                                    onClick = {
                                        ClientCardRenderer.shareCardToSocialPlatform(
                                            context = context,
                                            user = user,
                                            config = config,
                                            trustScore = trustScore,
                                            reliabilitySummary = reliabilitySummary,
                                            platform = platform,
                                            projectsCount = projectsCount,
                                            completedDealsCount = completedDealsCount
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text(
                                        text = platform.localizedLabel(currentLanguage),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        Text(
                            text = tr("card_studio.landing_links_title"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )

                        val domainCopiedMsg = tr("card_studio.domain_copied")
                        val deeplinkCopiedMsg = tr("card_studio.deeplink_copied")
                        val downloadCopiedMsg = tr("card_studio.download_copied")

                        DomainLinkCopyRow(
                            label = tr("card_studio.domain_url_lbl"),
                            url = domainCardUrl,
                            onCopy = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("OOZOX Card Domain", domainCardUrl))
                                Toast.makeText(context, domainCopiedMsg, Toast.LENGTH_SHORT).show()
                            }
                        )
                        DomainLinkCopyRow(
                            label = tr("card_studio.deeplink_lbl"),
                            url = appDeepLinkUrl,
                            onCopy = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("OOZOX Deep Link", appDeepLinkUrl))
                                Toast.makeText(context, deeplinkCopiedMsg, Toast.LENGTH_SHORT).show()
                            }
                        )
                        DomainLinkCopyRow(
                            label = tr("card_studio.download_lbl"),
                            url = downloadUrl,
                            onCopy = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(ClipData.newPlainText("OOZOX Download", downloadUrl))
                                Toast.makeText(context, downloadCopiedMsg, Toast.LENGTH_SHORT).show()
                            }
                        )

                        OutlinedButton(
                            onClick = { showLandingModal = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text(tr("card_studio.preview_visitor_btn"), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }

    if (showLandingModal) {
        VisitedDomainCardDialog(
            user = user,
            config = config,
            trustScore = trustScore,
            reliabilitySummary = reliabilitySummary,
            onDismiss = { showLandingModal = false }
        )
    }
}

@Composable
private fun DomainLinkCopyRow(
    label: String,
    url: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = tr("card_studio.copy_cd"), modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Interactive Landing View displayed when a shared card link (`oozox://card/...` or `https://oozox.com/card/...`)
 * is tapped — offers instant in-app view, opening on project domain oozox.com, or app download page.
 */
@Composable
fun VisitedDomainCardDialog(
    user: UserEntity,
    config: ClientCardStyleConfig,
    trustScore: Int,
    reliabilitySummary: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentLanguage = LocalAppLanguage.current
    val handle = user.username.ifBlank { user.id.take(8) }
    val domainUrl = remember(handle) { ClientCardDeepLinkRouter.domainCardUrl(handle) }
    val downloadUrl = remember(handle) { ClientCardDeepLinkRouter.domainDownloadUrl(handle) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides currentLanguage.direction) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 500.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("card_studio.portal_title"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr("card_studio.portal_sub"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = tr("common.close"))
                        }
                    }

                    ClientCardLivePreview(
                        user = user,
                        config = config,
                        trustScore = trustScore,
                        reliabilitySummary = reliabilitySummary,
                        onCardClick = {},
                        onLinkClick = { link ->
                            ClientCardDeepLinkRouter.openExternalDeepLinkOrDomain(context, link.url, handle)
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                ClientCardDeepLinkRouter.openExternalDeepLinkOrDomain(context, downloadUrl, handle)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(tr("card_studio.download_app"), fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                ClientCardDeepLinkRouter.openExternalDeepLinkOrDomain(context, domainUrl, handle)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(tr("card_studio.open_web"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
