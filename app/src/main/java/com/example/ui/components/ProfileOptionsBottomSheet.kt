package com.example.ui.components

import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.LocalAppTheme
import com.example.ui.theme.*

/**
 * WhatsApp-Style Settings & Profile Options Bottom Sheet
 * Clean, organized, categorized settings list with popup dialogs for multi-choice options
 * (Theme palettes, Font scales) that auto-save and dismiss upon selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileOptionsBottomSheet(
    onDismiss: () -> Unit,
    onExportCvPdf: () -> Unit = {},
    onShareProfile: () -> Unit = {},
    onCustomizeCv: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onOpenSubscriptions: () -> Unit = {},
    onOpenVerification: () -> Unit = {},
    isVerified: Boolean = false,
    onOpenSecuritySettings: () -> Unit,
    onOpenCallPrivacy: () -> Unit = {},
    onImportContacts: () -> Unit = {},
    onOpenNotificationTones: () -> Unit = {},
    onOpenFloatingBubble: () -> Unit = {},
    onOpenCompanyAccounting: () -> Unit = {},
    onOpenNearby: () -> Unit = {},
    onOpenDeviceTransfer: () -> Unit = {},
    onOpenDailyJournal: () -> Unit = {},
    onOpenGoogleBackup: () -> Unit = {},
    onOpenCloudBackups: () -> Unit,
    onOpenLocalBackup: () -> Unit = {},
    onOpenRestoreBackup: () -> Unit = {},
    onOpenSubAccounts: () -> Unit,
    onOpenRecoverySeed: () -> Unit,
    onOpenSecurityAudit: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit,
    onOpenHelpSupport: () -> Unit,
    onOpenStorefront: (() -> Unit)? = null,
    storeName: String? = null,
    onOpenBroadcast: (() -> Unit)? = null,
    onOpenAdmin: (() -> Unit)? = null,
    onDeleteAccount: () -> Unit = {},
    onLogout: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val themeController = LocalAppTheme.current
    val currentFontScale = themeController.fontScale
    val colors = MaterialTheme.colorScheme

    var showFontScaleDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = colors.primary.copy(alpha = 0.5f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "خيارات الصفحة الشخصية",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "المشاركة، التصدير، الأمان والإعدادات",
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = colors.onSurfaceVariant
                    )
                }
            }

            // --- SECTION 1: المشاركة والتصدير ---
            SettingsCategoryCard(title = "المشاركة والتصدير") {
                WhatsAppSettingsRow(
                    icon = Icons.Default.Share,
                    iconBg = colors.primary.copy(alpha = 0.14f),
                    iconTint = colors.primary,
                    title = "مشاركة الملف الشخصي",
                    subtitle = "مشاركة البطاقة المهنية وسجل الموثوقية",
                    onClick = {
                        onDismiss()
                        onShareProfile()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.PictureAsPdf,
                    iconBg = colors.secondary.copy(alpha = 0.14f),
                    iconTint = colors.secondary,
                    title = "تصدير السيرة الذاتية PDF",
                    subtitle = "حفظ ومشاركة نسخة PDF رسمية",
                    onClick = {
                        onDismiss()
                        onExportCvPdf()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.Tune,
                    iconBg = colors.surfaceVariant,
                    iconTint = colors.onSurfaceVariant,
                    title = "تخصيص السيرة الذاتية",
                    subtitle = "ترتيب الأقسام والبيانات المهنية",
                    onClick = {
                        onDismiss()
                        onCustomizeCv()
                    }
                )
            }

            // --- SECTION 2: الحساب والعضوية ---
            SettingsCategoryCard(title = "الحساب والعضوية") {
                WhatsAppSettingsRow(
                    icon = Icons.Default.Badge,
                    iconBg = colors.primary.copy(alpha = 0.12f),
                    iconTint = colors.primary,
                    title = "تعديل البيانات الشخصية",
                    subtitle = "الاسم، اسم المستخدم، المسمى الوظيفي والنبذة",
                    onClick = {
                        onDismiss()
                        onEditProfile()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.WorkspacePremium,
                    iconBg = colors.tertiary.copy(alpha = 0.14f),
                    iconTint = colors.tertiary,
                    title = "باقات الاشتراك والترقية",
                    subtitle = "إدارة الباقة والمزايا المتقدمة",
                    onClick = {
                        onDismiss()
                        onOpenSubscriptions()
                    }
                )
                if (!isVerified) {
                    WhatsAppDivider()
                    WhatsAppSettingsRow(
                        icon = Icons.Default.VerifiedUser,
                        iconBg = colors.tertiary.copy(alpha = 0.14f),
                        iconTint = colors.tertiary,
                        title = "توثيق الحساب",
                        subtitle = "تقديم طلب الاعتماد الرسمي",
                        onClick = {
                            onDismiss()
                            onOpenVerification()
                        }
                    )
                }
                if (onOpenStorefront != null) {
                    WhatsAppDivider()
                    WhatsAppSettingsRow(
                        icon = Icons.Default.Storefront,
                        iconBg = colors.primary.copy(alpha = 0.14f),
                        iconTint = colors.primary,
                        title = storeName?.takeIf { it.isNotBlank() } ?: "المتجر الخاص",
                        subtitle = "إدارة المنتجات والخدمات",
                        onClick = {
                            onDismiss()
                            onOpenStorefront()
                        }
                    )
                }
            }

            // --- SECTION 3: الأمان والخصوصية ---
            SettingsCategoryCard(title = "الأمان والخصوصية") {
                WhatsAppSettingsRow(
                    icon = Icons.Default.Security,
                    iconBg = StatusGreen.copy(alpha = 0.14f),
                    iconTint = StatusGreen,
                    title = "قفل التطبيق والبصمة",
                    subtitle = "رمز PIN والمصادقة البيومترية",
                    onClick = {
                        onDismiss()
                        onOpenSecuritySettings()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.PhoneCallback,
                    iconBg = colors.primary.copy(alpha = 0.14f),
                    iconTint = colors.primary,
                    title = "خصوصية المكالمات",
                    subtitle = "التحكم في المكالمات الصوتية والمرئية",
                    onClick = {
                        onDismiss()
                        onOpenCallPrivacy()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.Key,
                    iconBg = StatusOrange.copy(alpha = 0.14f),
                    iconTint = StatusOrange,
                    title = "كلمات الاسترداد",
                    subtitle = "12 كلمة لاستعادة الحساب",
                    onClick = {
                        onDismiss()
                        onOpenRecoverySeed()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.HealthAndSafety,
                    iconBg = StatusGreen.copy(alpha = 0.14f),
                    iconTint = StatusGreen,
                    title = "فحص أمان قاعدة البيانات",
                    subtitle = "تقرير التشفير والحماية",
                    onClick = {
                        onDismiss()
                        onOpenSecurityAudit()
                    }
                )
            }

            // --- SECTION 4: الأدوات والنسخ الاحتياطي ---
            SettingsCategoryCard(title = "الأدوات والنسخ الاحتياطي") {
                WhatsAppSettingsRow(
                    icon = Icons.Default.CloudQueue,
                    iconBg = colors.primary.copy(alpha = 0.14f),
                    iconTint = colors.primary,
                    title = "النسخ السحابي السريع",
                    subtitle = "مزامنة سحابية مشفرة",
                    onClick = {
                        onDismiss()
                        onOpenGoogleBackup()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.CloudSync,
                    iconBg = colors.secondary.copy(alpha = 0.14f),
                    iconTint = colors.secondary,
                    title = "خزانة النسخ المشفرة",
                    subtitle = "استعراض وإدارة النسخ السحابية",
                    onClick = {
                        onDismiss()
                        onOpenCloudBackups()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.Save,
                    iconBg = colors.surfaceVariant,
                    iconTint = colors.onSurfaceVariant,
                    title = "نسخ احتياطي محلي",
                    subtitle = "إنشاء ملف مشفر على الجهاز",
                    onClick = {
                        onDismiss()
                        onOpenLocalBackup()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.Restore,
                    iconBg = colors.surfaceVariant,
                    iconTint = colors.onSurfaceVariant,
                    title = "استعادة نسخة احتياطية",
                    subtitle = "استيراد البيانات من ملف محلي",
                    onClick = {
                        onDismiss()
                        onOpenRestoreBackup()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.AccountTree,
                    iconBg = colors.primary.copy(alpha = 0.14f),
                    iconTint = colors.primary,
                    title = "الحسابات الفرعية والمخفية",
                    subtitle = "إدارة الحسابات الموازية ودفاتر الشركات",
                    onClick = {
                        onDismiss()
                        onOpenSubAccounts()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.MenuBook,
                    iconBg = colors.tertiary.copy(alpha = 0.14f),
                    iconTint = colors.tertiary,
                    title = "دفتر اليومية المحاسبي",
                    subtitle = "قيود اليومية والإيرادات والمصروفات",
                    onClick = {
                        onDismiss()
                        onOpenDailyJournal()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.Radar,
                    iconBg = colors.primary.copy(alpha = 0.14f),
                    iconTint = colors.primary,
                    title = "رادار القريبين مني",
                    subtitle = "استكشاف التجار والمستثمرين جغرافياً",
                    onClick = {
                        onDismiss()
                        onOpenNearby()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.PhonelinkSetup,
                    iconBg = colors.secondary.copy(alpha = 0.14f),
                    iconTint = colors.secondary,
                    title = "نقل الحساب لهاتف آخر",
                    subtitle = "ربط مباشر بالرمز المشفر",
                    onClick = {
                        onDismiss()
                        onOpenDeviceTransfer()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.ChatBubble,
                    iconBg = colors.primary.copy(alpha = 0.14f),
                    iconTint = colors.primary,
                    title = "الفقاعة العائمة",
                    subtitle = "تشغيل وإدارة فقاعات الدردشة",
                    onClick = {
                        onDismiss()
                        onOpenFloatingBubble()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.NotificationsActive,
                    iconBg = colors.surfaceVariant,
                    iconTint = colors.onSurfaceVariant,
                    title = "نغمات وتنبيهات الرسائل",
                    subtitle = "تخصيص نغمة الإشعارات",
                    onClick = {
                        onDismiss()
                        onOpenNotificationTones()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.ContactPhone,
                    iconBg = colors.surfaceVariant,
                    iconTint = colors.onSurfaceVariant,
                    title = "استيراد جهات الاتصال",
                    subtitle = "مزامنة أرقام الهاتف",
                    onClick = {
                        onDismiss()
                        onImportContacts()
                    }
                )
                WhatsAppDivider()
                val fontScaleTitle = when (currentFontScale) {
                    AppFontScale.COMFORTABLE -> "مريح (115%)"
                    AppFontScale.LARGE -> "كبير (130%)"
                    AppFontScale.EXTRA_LARGE -> "فائق (145%)"
                    else -> "قياسي (100%)"
                }
                WhatsAppSettingsRow(
                    icon = Icons.Default.FormatSize,
                    iconBg = colors.surfaceVariant,
                    iconTint = colors.onSurfaceVariant,
                    title = "حجم الخط",
                    subtitle = fontScaleTitle,
                    onClick = { showFontScaleDialog = true }
                )
                if (onOpenBroadcast != null) {
                    WhatsAppDivider()
                    WhatsAppSettingsRow(
                        icon = Icons.Default.Campaign,
                        iconBg = colors.tertiary.copy(alpha = 0.14f),
                        iconTint = colors.tertiary,
                        title = "بث رسائل وإشعارات",
                        subtitle = "إرسال توجيهات للمستخدمين",
                        onClick = {
                            onDismiss()
                            onOpenBroadcast()
                        }
                    )
                }
                if (onOpenAdmin != null) {
                    WhatsAppDivider()
                    WhatsAppSettingsRow(
                        icon = Icons.Default.AdminPanelSettings,
                        iconBg = colors.error.copy(alpha = 0.14f),
                        iconTint = colors.error,
                        title = "لوحة التحكم الإدارية",
                        subtitle = "إدارة الحسابات والاشتراكات",
                        onClick = {
                            onDismiss()
                            onOpenAdmin()
                        }
                    )
                }
            }

            // --- SECTION 5: الدعم والحساب ---
            SettingsCategoryCard(title = "الدعم والحساب") {
                WhatsAppSettingsRow(
                    icon = Icons.Default.HelpOutline,
                    iconBg = colors.surfaceVariant,
                    iconTint = colors.onSurfaceVariant,
                    title = "الدعم الفني",
                    subtitle = "المساعدة والتواصل",
                    onClick = {
                        onDismiss()
                        onOpenHelpSupport()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.Policy,
                    iconBg = colors.surfaceVariant,
                    iconTint = colors.onSurfaceVariant,
                    title = "سياسة الخصوصية",
                    subtitle = "الأمان وشروط الاستخدام",
                    onClick = {
                        onDismiss()
                        onOpenPrivacyPolicy()
                    }
                )
                WhatsAppDivider()
                WhatsAppSettingsRow(
                    icon = Icons.Default.DeleteForever,
                    iconBg = colors.error.copy(alpha = 0.12f),
                    iconTint = colors.error,
                    title = "حذف الحساب نهائياً",
                    subtitle = "إزالة الحساب وكافة البيانات",
                    onClick = {
                        onDismiss()
                        onDeleteAccount()
                    }
                )
            }

            // --- LOGOUT ---
            Surface(
                onClick = {
                    onDismiss()
                    onLogout()
                },
                shape = RoundedCornerShape(16.dp),
                color = colors.error.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_profile_sheet_logout")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسجيل الخروج",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.error
                    )
                }
            }
        }
    }

    // --- POPUP DIALOG: FONT SCALE SELECTION (AUTO-SAVE & DISMISS) ---
    if (showFontScaleDialog) {
        Dialog(onDismissRequest = { showFontScaleDialog = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FormatSize, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حجم الخط وإمكانية القراءة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { showFontScaleDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text(
                        text = "اختر الحجم المناسب لعينيك لتكبير النصوص وكافة أرقام الحسابات:",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val scales = listOf(
                        Triple(AppFontScale.STANDARD, "الخط القياسي (100%)", "الحجم الافتراضي لكافة الشاشات"),
                        Triple(AppFontScale.COMFORTABLE, "الخط المريح (115%)", "زيادة طفيفة لراحة العين أثناء العمل"),
                        Triple(AppFontScale.LARGE, "الخط الكبير (130%)", "تمييز فائق للأرقام والأسعار والرسائل"),
                        Triple(AppFontScale.EXTRA_LARGE, "الخط الفائق (145%)", "مخصص لضعاف النظر وكبار السن بدون إجهاد")
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scales.forEach { (scale, title, desc) ->
                            val isSelected = scale == currentFontScale
                            Surface(
                                onClick = {
                                    themeController.setFontScale(scale)
                                    showFontScaleDialog = false // Auto-save & dismiss
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) OozoxTeal.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) OozoxTeal else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) OozoxTeal else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(OozoxTeal),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
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
    }
}

/**
 * WhatsApp Settings Category Container Card
 */
@Composable
private fun SettingsCategoryCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

/**
 * WhatsApp-Style Single Setting Row Item
 */
@Composable
private fun WhatsAppSettingsRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 11.sp,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun WhatsAppDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 58.dp, end = 14.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        thickness = 0.6.dp
    )
}
