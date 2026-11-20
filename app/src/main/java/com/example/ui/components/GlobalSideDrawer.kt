package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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

/**
 * High-level identifiers for primary sections in the side navigation drawer.
 */
enum class DrawerSection(val id: String, val title: String) {
    HOME("home", "الرئيسية والمحادثات"),
    AI_ACCOUNTANT("ai_accountant", "المحاسب المالي"),
    COMPANY_ACCOUNTING("company_accounting", "المركز المحاسبي وإدارة الشركات"),
    SUBSCRIPTIONS("subscriptions", "باقات وعضوية VIP"),
    ENCRYPTED_VAULT("encrypted_vault", "الخزينة والنسخ المشفر"),
    LANGUAGE("language", "لغة النظام والواجهة"),
    ADMIN("admin", "لوحة الإدارة والرقابة المركزية"),
    GOLD("gold", "قسم الذهب والمعادن الثمينة"),
    PROJECTS("projects", "إدارة المشاريع والاستثمارات")
}

/**
 * Enterprise-Grade Luxury Side Navigation Drawer
 * Completely reorganized with clean visual hierarchy, rich subtitles, high-contrast badges,
 * active section highlighting, unified typography scale, and standard Material 3 >=48dp touch targets.
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
                    .height(52.dp),
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
                            text = "منصة الأعمال",
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
                        .size(44.dp)
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
                            contentDescription = Translations.get("close", currentLanguage),
                            tint = MaterialTheme.colorScheme.onSurface,
                            size = AppIconSize.SM
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. User Profile Summary Card (Elevated Luxury Card)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.2.dp, if (isDark) adaptiveGoldPrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                shadowElevation = if (isDark) 2.dp else 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Avatar with Ring
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isDark) GoldPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
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

                        // Online green dot indicator
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
                                text = currentUser?.name ?: Translations.get("account_info", currentLanguage),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentUser?.isVerified == true || isAdmin) {
                                AppIcon(
                                    imageVector = AppIcons.Verified,
                                    contentDescription = "Verified",
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
                                border = BorderStroke(0.6.dp, if (isDark) GoldPrimary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = if (currentUser?.type == UserType.GOLD_TRADER.name)
                                        "تاجر ذهب"
                                    else if (isAdmin)
                                        "إدارة النظام"
                                    else
                                        "حساب أعمال",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = "متصل",
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
                        .testTag("drawer_gold_ticker_chip")
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCloseDrawer()
                            onOpenGold()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
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
                            text = "ذهب عيار 21: ${GoldUtils.formatCurrency(goldPriceKarat21)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) GoldLight else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حي",
                            fontSize = 9.sp,
                            color = adaptiveStatusGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "المحادثات النشطة ($activeChatsCount)",
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
                // SECTION 0: الرئيسية
                Column {
                    SectionHeader(title = "الرئيسية")
                    Spacer(modifier = Modifier.height(6.dp))
                    EnhancedDrawerItem(
                        icon = AppIcons.Chats,
                        iconTint = if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary,
                        iconBg = if (isDark) GoldPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        title = "المحادثات",
                        subtitle = "المحادثات والقيود المالية",
                        badgeText = "الرئيسية",
                        badgeTint = if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary,
                        badgeBg = if (isDark) GoldPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCloseDrawer()
                            onOpenHome()
                        }
                    )
                }

                // SECTION: قسم الذهب المستقل
                Column {
                    SectionHeader(title = "قسم الذهب والمعادن")
                    Spacer(modifier = Modifier.height(6.dp))
                    EnhancedDrawerItem(
                        icon = Icons.Default.MonetizationOn,
                        iconTint = if (isDark) GoldLight else Color(0xFFB45309),
                        iconBg = if (isDark) GoldPrimary.copy(alpha = 0.2f) else Color(0xFFFDE68A),
                        title = "قسم الذهب",
                        subtitle = "أسعار الذهب، حاسبة المصنعية والمشتريات",
                        badgeText = "ذهب",
                        badgeTint = if (isDark) GoldLight else Color(0xFFB45309),
                        badgeBg = if (isDark) GoldPrimary.copy(alpha = 0.2f) else Color(0xFFFDE68A),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCloseDrawer()
                            onOpenGold()
                        }
                    )
                }

                // SECTION 1: الخدمات الذكية
                Column {
                    SectionHeader(title = "الخدمات المحاسبية والمالية")
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // AI Voice Business Agent
                        EnhancedDrawerItem(
                            icon = Icons.Default.Mic,
                            iconTint = if (isDark) OozoxTeal else MaterialTheme.colorScheme.primary,
                            iconBg = if (isDark) OozoxTeal.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            title = "المساعد الصوتي",
                            subtitle = "أوامر وتسجيل بالصوت",
                            badgeText = "صوتي",
                            badgeTint = if (isDark) OozoxTeal else MaterialTheme.colorScheme.primary,
                            badgeBg = if (isDark) OozoxTeal.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCloseDrawer()
                                onOpenVoiceBusinessAgent()
                            }
                        )

                        // AI Accountant
                        EnhancedDrawerItem(
                            icon = AppIcons.AIAccountant,
                            iconTint = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9),
                            iconBg = if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.16f) else Color(0xFF8B5CF6).copy(alpha = 0.10f),
                            title = "المحاسب المالي",
                            subtitle = "تحليل العمليات والحسابات",
                            badgeText = "PRO",
                            badgeTint = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9),
                            badgeBg = if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.16f) else Color(0xFF8B5CF6).copy(alpha = 0.10f),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCloseDrawer()
                                onOpenAIAccountant()
                            }
                        )

                        // Company Accounting Hub
                        EnhancedDrawerItem(
                            icon = AppIcons.Corporate,
                            iconTint = if (isDark) GoldLight else MaterialTheme.colorScheme.primary,
                            iconBg = if (isDark) GoldDark.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            title = "المركز المحاسبي",
                            subtitle = "اليومية العامة والشركات",
                            badgeText = "PRO",
                            badgeTint = if (isDark) GoldLight else MaterialTheme.colorScheme.primary,
                            badgeBg = if (isDark) GoldPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCloseDrawer()
                                onOpenCompanyAccounting()
                            }
                        )

                        // Invoice Verification
                        EnhancedDrawerItem(
                            icon = Icons.Default.Verified,
                            iconTint = if (isDark) OozoxTeal else MaterialTheme.colorScheme.primary,
                            iconBg = if (isDark) OozoxTeal.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            title = "التحقق من الفاتورة",
                            subtitle = "فحص التوقيع المائي الرقمي",
                            badgeText = "توثيق",
                            badgeTint = if (isDark) OozoxTeal else MaterialTheme.colorScheme.primary,
                            badgeBg = if (isDark) OozoxTeal.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCloseDrawer()
                                onOpenInvoiceVerification()
                            }
                        )
                    }
                }

                // SECTION 2: الاشتراكات والأمان
                Column {
                    SectionHeader(title = "الاشتراكات والأمان")
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Subscriptions & VIP
                        EnhancedDrawerItem(
                            icon = AppIcons.Crown,
                            iconTint = if (isDark) GoldAccent else Color(0xFFB45309),
                            iconBg = if (isDark) GoldPrimary.copy(alpha = 0.16f) else Color(0xFFF59E0B).copy(alpha = 0.10f),
                            title = "الاشتراكات",
                            subtitle = "باقات وعضوية VIP",
                            badgeText = "VIP",
                            badgeTint = if (isDark) GoldAccent else Color(0xFFB45309),
                            badgeBg = if (isDark) GoldPrimary.copy(alpha = 0.16f) else Color(0xFFF59E0B).copy(alpha = 0.10f),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCloseDrawer()
                                onOpenSubscriptions()
                            }
                        )

                        // Encrypted Vault
                        EnhancedDrawerItem(
                            icon = AppIcons.EncryptedVault,
                            iconTint = if (isDark) Color(0xFF818CF8) else Color(0xFF4338CA),
                            iconBg = if (isDark) Color(0xFF4F46E5).copy(alpha = 0.16f) else Color(0xFF4F46E5).copy(alpha = 0.10f),
                            title = "الخزينة المشفرة",
                            subtitle = "النسخ الاحتياطي والأمان",
                            badgeText = "مشفر",
                            badgeTint = if (isDark) Color(0xFF818CF8) else Color(0xFF4338CA),
                            badgeBg = if (isDark) Color(0xFF4F46E5).copy(alpha = 0.16f) else Color(0xFF4F46E5).copy(alpha = 0.10f),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCloseDrawer()
                                onOpenBackupsVault()
                            }
                        )
                    }
                }

                // SECTION 3: التفضيلات والنظام
                Column {
                    SectionHeader(title = "التفضيلات والنظام")
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Language
                        EnhancedDrawerItem(
                            icon = Icons.Default.Language,
                            iconTint = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                            iconBg = if (isDark) Color(0xFF059669).copy(alpha = 0.16f) else Color(0xFF059669).copy(alpha = 0.10f),
                            title = "اللغة",
                            subtitle = "تخصيص لغة التطبيق",
                            badgeText = "${currentLanguage.flag} ${currentLanguage.displayName}",
                            badgeTint = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                            badgeBg = if (isDark) Color(0xFF059669).copy(alpha = 0.16f) else Color(0xFF059669).copy(alpha = 0.10f),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showLanguageModal = true
                            }
                        )

                        // Admin Dashboard (if admin)
                        if (isAdmin) {
                            EnhancedDrawerItem(
                                icon = Icons.Default.AdminPanelSettings,
                                iconTint = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C),
                                iconBg = if (isDark) Color(0xFFDC2626).copy(alpha = 0.16f) else Color(0xFFDC2626).copy(alpha = 0.10f),
                                title = "لوحة الإدارة",
                                subtitle = "إدارة النظام والمستخدمين",
                                badgeText = "ADMIN",
                                badgeTint = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C),
                                badgeBg = if (isDark) Color(0xFFDC2626).copy(alpha = 0.16f) else Color(0xFFDC2626).copy(alpha = 0.10f),
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
                // Theme Mode Switcher & Color Palette Selector Card
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
                        // Switch & Day/Night Toggle
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
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark)
                                            themeController.currentPalette.primaryColor.copy(alpha = 0.15f)
                                        else
                                            Color(0xFFFEF3C7)
                                    )
                                    .clickable(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            themeController.toggleTheme()
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                AppIcon(
                                    imageVector = if (isDark) AppIcons.Moon else AppIcons.Sun,
                                    contentDescription = if (isDark) "الوضع الداكن" else "الوضع الفاتح",
                                    tint = if (isDark) themeController.currentPalette.primaryColor else Color(0xFFD97706),
                                    size = AppIconSize.SM
                                )
                            }
                        }

                        // Vertical Divider
                        Box(
                            modifier = Modifier
                                .height(24.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        )

                        // Circular Color Palette Dots
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
                                        .clickable(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                themeController.setPalette(palette)
                                            }
                                        ),
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
                            text = Translations.get("logout", currentLanguage),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Brand Identity & Official Motto Footer
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

    // Language Selector Modal
    if (showLanguageModal) {
        AlertDialog(
            onDismissRequest = { showLanguageModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "اختر لغة التطبيق والواجهة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = lang == currentLanguage
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLanguageChange(lang)
                                showLanguageModal = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) (if (isDark) GoldPrimary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) (if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = lang.flag, fontSize = 20.sp)
                                    Text(
                                        text = lang.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) (if (isDark) GoldLight else Color(0xFF0F172A)) else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (isSelected) {
                                    AppIcon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary,
                                        size = AppIconSize.SM
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showLanguageModal = false },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = Translations.get("close", currentLanguage),
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) GoldPrimary else MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

/**
 * Section Header Component with professional subtle label styling and theme accent.
 */
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

/**
 * Clean & Elegant Drawer Item Card with rich two-line text (Title + Subtitle),
 * crisp icon container, neat category badge, and uniform high-contrast design.
 */
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
        modifier = Modifier.fillMaxWidth()
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
                // Leading Icon in Styled Container
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

                // Title & Descriptive Subtitle with unified typography
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

            // Trailing Section: Category Badge and Navigation Arrow
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
