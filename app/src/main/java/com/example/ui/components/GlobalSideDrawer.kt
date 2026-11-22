package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.model.UserType
import com.example.ui.LocalAppTheme
import com.example.ui.icons.AppIcons
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.GoldUtils
import com.example.util.Translations
import com.example.util.tr
import com.example.util.trFmt

/**
 * High-level identifiers for primary sections in the side navigation drawer.
 */
enum class DrawerSection(val id: String, val titleKey: String) {
    HOME("home", "drawer.sec_home_chats"),
    AI_ACCOUNTANT("ai_accountant", "drawer.ai_title"),
    COMPANY_ACCOUNTING("company_accounting", "drawer.hub_short_title"),
    SUBSCRIPTIONS("subscriptions", "drawer.subs_title"),
    ENCRYPTED_VAULT("encrypted_vault", "drawer.vault_title"),
    LANGUAGE("language", "drawer.lang_title"),
    ADMIN("admin", "drawer.admin_title"),
    GOLD("gold", "drawer.gold_section"),
    PROJECTS("projects", "drawer.project_section_title");

    fun localizedTitle(lang: AppLanguage): String = Translations.get(titleKey, lang)
}

/**
 * Enterprise-Grade Luxury Side Navigation Drawer (14-language synced)
 */
@Composable
fun GlobalSideDrawerContent(
    currentUser: UserEntity?,
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onCloseDrawer: () -> Unit,
    activeSection: DrawerSection = DrawerSection.HOME,
    onOpenHome: () -> Unit = {},
    onOpenGold: () -> Unit = {},
    onOpenAIAccountant: () -> Unit,
    onOpenVoiceBusinessAgent: () -> Unit = {},
    onOpenInvoiceVerification: () -> Unit = {},
    onOpenCompanyAccounting: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenNearby: () -> Unit = {},
    onOpenGoldDeal: () -> Unit = {},
    onOpenCommandTerminal: () -> Unit = {},
    onOpenBackupsVault: () -> Unit,
    onOpenBiometricSettings: () -> Unit = {},
    onOpenAdmin: () -> Unit,
    onForceSync: () -> Unit,
    onLogout: () -> Unit,
    goldPriceKarat21: Double = 0.0,
    activeChatsCount: Int = 0
) {
    val themeController = LocalAppTheme.current
    val isDark = themeController.isDarkTheme
    val haptic = LocalHapticFeedback.current
    var showLanguageModal by remember { mutableStateOf(false) }

    val isAdmin = remember(currentUser) {
        currentUser?.type == UserType.ADMIN.name ||
            currentUser?.name?.contains("Admin", ignoreCase = true) == true ||
            currentUser?.name?.contains("الإدارة", ignoreCase = true) == true
    }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerTonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 320.dp, max = 360.dp)
            .testTag("global_side_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // 1. Header Bar: Brand Logo, Title & Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(listOf(GoldPrimary, GoldLight, GoldDark))
                            )
                            .shadow(4.dp, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "O",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = adaptiveOnPrimary
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "OOZOX",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "BUSINESS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isDark) OozoxTeal else MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = tr("drawer.business_platform"),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OozoxTeal
                        )
                    }
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCloseDrawer()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("drawer_close_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(
                            imageVector = AppIcons.MenuClose,
                            contentDescription = tr("common.close"),
                            tint = MaterialTheme.colorScheme.onSurface,
                            size = AppIconSize.SM
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. User Profile Summary Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.2.dp,
                    if (isDark) adaptiveGoldPrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                ),
                shadowElevation = if (isDark) 2.dp else 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) GoldPrimary.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                            .border(1.5.dp, if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = currentUser?.name?.trim()?.take(1) ?: "O"
                        Text(
                            text = initial,
                            color = if (isDark) GoldLight else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(StatusGreen)
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentUser?.name ?: tr("drawer.account_info"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentUser?.isVerified == true || isAdmin) {
                                AppIcon(
                                    imageVector = AppIcons.Verified,
                                    contentDescription = tr("drawer.verified_badge"),
                                    tint = if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary,
                                    size = AppIconSize.SM
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDark) GoldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                border = BorderStroke(
                                    0.6.dp,
                                    if (isDark) GoldPrimary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                )
                            ) {
                                Text(
                                    text = when {
                                        currentUser?.type == UserType.GOLD_TRADER.name -> tr("drawer.role_gold_trader")
                                        isAdmin -> tr("drawer.role_system_admin")
                                        else -> tr("drawer.role_business")
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = tr("drawer.online_status"),
                                fontSize = 10.5.sp,
                                color = adaptiveStatusGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2.5 Live Gold Ticker & Active Chats Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = GoldDark.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .heightIn(min = 36.dp)
                        .testTag("drawer_gold_ticker_chip")
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCloseDrawer()
                            onOpenGold()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = adaptiveGoldPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${tr("drawer.gold_21_label")}: ${GoldUtils.formatCurrency(goldPriceKarat21, currentLanguage)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) GoldLight else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr("drawer.live_badge"),
                            fontSize = 9.sp,
                            color = adaptiveStatusGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = trFmt("drawer.active_chats_fmt", "count" to activeChatsCount),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Navigation Sections List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SECTION 0: Home
                Column {
                    SectionHeader(title = tr("drawer.sec_home"))
                    Spacer(modifier = Modifier.height(6.dp))
                    EnhancedDrawerItem(
                        icon = AppIcons.Chats,
                        iconTint = if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary,
                        iconBg = if (isDark) GoldPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        title = tr("drawer.chats_title"),
                        subtitle = tr("drawer.chats_sub"),
                        badgeText = tr("drawer.home_badge"),
                        badgeTint = if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary,
                        badgeBg = if (isDark) GoldPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        modifier = Modifier.testTag("drawer_nav_chats"),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCloseDrawer()
                            onOpenHome()
                        }
                    )
                }

                // SECTION: Gold Market
                Column {
                    SectionHeader(title = tr("drawer.sec_gold_metals"))
                    Spacer(modifier = Modifier.height(6.dp))
                    EnhancedDrawerItem(
                        icon = Icons.Default.MonetizationOn,
                        iconTint = if (isDark) GoldLight else Color(0xFFB45309),
                        iconBg = if (isDark) GoldPrimary.copy(alpha = 0.2f) else Color(0xFFFDE68A),
                        title = tr("drawer.gold_section"),
                        subtitle = tr("drawer.gold_sub"),
                        badgeText = tr("drawer.gold_badge"),
                        badgeTint = if (isDark) GoldLight else Color(0xFFB45309),
                        badgeBg = if (isDark) GoldPrimary.copy(alpha = 0.2f) else Color(0xFFFDE68A),
                        modifier = Modifier.testTag("drawer_nav_gold"),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCloseDrawer()
                            onOpenGold()
                        }
                    )
                }

                // SECTION 1: Accounting & Finance Services
                Column {
                    SectionHeader(title = tr("drawer.sec_finance"))
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EnhancedDrawerItem(
                            icon = Icons.Default.Mic,
                            iconTint = if (isDark) OozoxTeal else MaterialTheme.colorScheme.primary,
                            iconBg = if (isDark) OozoxTeal.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            title = tr("drawer.voice_assistant"),
                            subtitle = tr("drawer.voice_sub"),
                            badgeText = tr("drawer.voice_badge"),
                            badgeTint = if (isDark) OozoxTeal else MaterialTheme.colorScheme.primary,
                            badgeBg = if (isDark) OozoxTeal.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            modifier = Modifier.testTag("drawer_nav_voice"),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCloseDrawer()
                                onOpenVoiceBusinessAgent()
                            }
                        )

                        EnhancedDrawerItem(
                            icon = AppIcons.AIAccountant,
                            iconTint = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9),
                            iconBg = if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.16f) else Color(0xFF8B5CF6).copy(alpha = 0.10f),
                            title = tr("drawer.ai_title"),
                            subtitle = tr("drawer.ai_sub"),
                            badgeText = "PRO",
                            badgeTint = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9),
                            badgeBg = if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.16f) else Color(0xFF8B5CF6).copy(alpha = 0.10f),
                            modifier = Modifier.testTag("drawer_nav_ai"),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCloseDrawer()
                                onOpenAIAccountant()
                            }
                        )

                        EnhancedDrawerItem(
                            icon = AppIcons.Corporate,
                            iconTint = if (isDark) GoldLight else MaterialTheme.colorScheme.primary,
                            iconBg = if (isDark) GoldDark.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            title = tr("drawer.hub_short_title"),
                            subtitle = tr("drawer.company_acct_sub"),
                            badgeText = "PRO",
                            badgeTint = if (isDark) GoldLight else MaterialTheme.colorScheme.primary,
                            badgeBg = if (isDark) GoldPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            modifier = Modifier.testTag("drawer_nav_company_acct"),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCloseDrawer()
                                onOpenCompanyAccounting()
                            }
                        )

                        EnhancedDrawerItem(
                            icon = Icons.Default.Verified,
                            iconTint = if (isDark) OozoxTeal else MaterialTheme.colorScheme.primary,
                            iconBg = if (isDark) OozoxTeal.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            title = tr("drawer.invoice_verify"),
                            subtitle = tr("drawer.invoice_verify_sub"),
                            badgeText = tr("drawer.verify_badge"),
                            badgeTint = if (isDark) OozoxTeal else MaterialTheme.colorScheme.primary,
                            badgeBg = if (isDark) OozoxTeal.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            modifier = Modifier.testTag("drawer_nav_verify_invoice"),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCloseDrawer()
                                onOpenInvoiceVerification()
                            }
                        )
                    }
                }

                // SECTION 2: Subscriptions & Security
                Column {
                    SectionHeader(title = tr("drawer.sec_subs_security"))
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EnhancedDrawerItem(
                            icon = AppIcons.Crown,
                            iconTint = if (isDark) GoldAccent else Color(0xFFB45309),
                            iconBg = if (isDark) GoldPrimary.copy(alpha = 0.16f) else Color(0xFFF59E0B).copy(alpha = 0.10f),
                            title = tr("drawer.subs_title"),
                            subtitle = tr("drawer.subs_sub"),
                            badgeText = "VIP",
                            badgeTint = if (isDark) GoldAccent else Color(0xFFB45309),
                            badgeBg = if (isDark) GoldPrimary.copy(alpha = 0.16f) else Color(0xFFF59E0B).copy(alpha = 0.10f),
                            modifier = Modifier.testTag("drawer_nav_subscriptions"),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCloseDrawer()
                                onOpenSubscriptions()
                            }
                        )

                        EnhancedDrawerItem(
                            icon = AppIcons.EncryptedVault,
                            iconTint = if (isDark) Color(0xFF818CF8) else Color(0xFF4338CA),
                            iconBg = if (isDark) Color(0xFF4F46E5).copy(alpha = 0.16f) else Color(0xFF4F46E5).copy(alpha = 0.10f),
                            title = tr("drawer.vault_title"),
                            subtitle = tr("drawer.vault_sub"),
                            badgeText = tr("drawer.vault_badge"),
                            badgeTint = if (isDark) Color(0xFF818CF8) else Color(0xFF4338CA),
                            badgeBg = if (isDark) Color(0xFF4F46E5).copy(alpha = 0.16f) else Color(0xFF4F46E5).copy(alpha = 0.10f),
                            modifier = Modifier.testTag("drawer_nav_vault"),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCloseDrawer()
                                onOpenBackupsVault()
                            }
                        )
                    }
                }

                // SECTION 3: Preferences & System
                Column {
                    SectionHeader(title = tr("drawer.sec_prefs"))
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EnhancedDrawerItem(
                            icon = Icons.Default.Language,
                            iconTint = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                            iconBg = if (isDark) Color(0xFF059669).copy(alpha = 0.16f) else Color(0xFF059669).copy(alpha = 0.10f),
                            title = tr("drawer.lang_title"),
                            subtitle = tr("drawer.lang_sub"),
                            badgeText = "${currentLanguage.flag} ${currentLanguage.displayName}",
                            badgeTint = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                            badgeBg = if (isDark) Color(0xFF059669).copy(alpha = 0.16f) else Color(0xFF059669).copy(alpha = 0.10f),
                            modifier = Modifier.testTag("drawer_nav_language"),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showLanguageModal = true
                            }
                        )

                        if (isAdmin) {
                            EnhancedDrawerItem(
                                icon = Icons.Default.AdminPanelSettings,
                                iconTint = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C),
                                iconBg = if (isDark) Color(0xFFDC2626).copy(alpha = 0.16f) else Color(0xFFDC2626).copy(alpha = 0.10f),
                                title = tr("drawer.admin_title"),
                                subtitle = tr("drawer.admin_sub"),
                                badgeText = "ADMIN",
                                badgeTint = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C),
                                badgeBg = if (isDark) Color(0xFFDC2626).copy(alpha = 0.16f) else Color(0xFFDC2626).copy(alpha = 0.10f),
                                modifier = Modifier.testTag("drawer_nav_admin"),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCloseDrawer()
                                    onOpenAdmin()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(8.dp))

            // 4. Bottom Controls: Theme Customization & Logout
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("drawer_theme_toggle_row")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Switch(
                                checked = isDark,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    themeController.toggleTheme()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = themeController.currentPalette.primaryColor,
                                    checkedTrackColor = themeController.currentPalette.primaryColor.copy(alpha = 0.35f),
                                    uncheckedThumbColor = Color(0xFFD97706),
                                    uncheckedTrackColor = Color(0xFFFDE68A)
                                ),
                                modifier = Modifier.testTag("theme_mode_switch")
                            )

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark)
                                            themeController.currentPalette.primaryColor.copy(alpha = 0.15f)
                                        else
                                            Color(0xFFFEF3C7)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        themeController.toggleTheme()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AppIcon(
                                    imageVector = if (isDark) AppIcons.Moon else AppIcons.Sun,
                                    contentDescription = if (isDark) tr("theme.switch_to_dark") else tr("theme.switch_to_light"),
                                    tint = if (isDark) themeController.currentPalette.primaryColor else Color(0xFFD97706),
                                    size = AppIconSize.SM
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .height(24.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppThemePalette.entries.forEach { palette ->
                                val isSelected = themeController.currentPalette == palette
                                val dotColor = if (isDark) palette.primaryColor else palette.lightPrimaryColor

                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            themeController.setPalette(palette)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isSelected) 20.dp else 14.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(
                                                        2.dp,
                                                        if (isDark) Color.White else Color(0xFF0F172A),
                                                        CircleShape
                                                    )
                                                } else {
                                                    Modifier.border(
                                                        0.8.dp,
                                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                        CircleShape
                                                    )
                                                }
                                            )
                                            .testTag("theme_dot_${palette.id}")
                                    )
                                }
                            }
                        }
                    }
                }

                // Sign Out Button (Strict 48dp touch target)
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLogout()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("drawer_logout_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AppIcon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            size = AppIconSize.SM
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr("drawer.logout"),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "OOZOX Business",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OozoxTeal,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "oozox.com",
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }

    // 14-Language Picker Modal
    if (showLanguageModal) {
        AppLanguagePickerDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = onLanguageChange,
            onDismiss = { showLanguageModal = false }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    val themeController = LocalAppTheme.current
    val isDark = themeController.isDarkTheme
    val accentColor = if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun EnhancedDrawerItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeTint: Color = adaptiveGoldPrimary,
    badgeBg: Color = adaptiveGoldPrimary.copy(alpha = 0.15f),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val themeController = LocalAppTheme.current
    val isDark = themeController.isDarkTheme

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg)
                        .border(
                            width = 1.dp,
                            color = iconTint.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AppIcon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        size = AppIconSize.MD
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                        maxLines = 2,
                        lineHeight = 15.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg,
                        border = BorderStroke(1.dp, badgeTint.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTint,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
