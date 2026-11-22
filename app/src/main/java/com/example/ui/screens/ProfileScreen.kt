package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.data.model.*
import com.example.ui.components.CoverPhotoEditorDialog
import com.example.ui.components.OozoxAvatarImage
import com.example.ui.components.OozoxBannerImage
import com.example.ui.components.OozoxThumbnailImage
import com.example.ui.components.SubscriptionUpgradeModal
import com.example.ui.components.AppToastType
import com.example.ui.components.LocalAppToast
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.CallPrivacy
import com.example.util.CallPrivacyManager
import com.example.util.ContactSyncEngine
import com.example.util.LocalAppLanguage
import com.example.util.Translations
import com.example.util.tr
import com.example.util.BiometricHelper
import com.example.util.BiometricStatus
import com.example.util.GoldUtils
import com.example.util.UserEvaluationEngine
import com.example.util.ReportExporter
import com.example.util.SecureBackupVaultEngine
import com.example.util.BackupResult
import com.example.util.CloudBackupRecord
import com.example.util.CloudStorageProvider
import com.example.util.BackupMetadataPreview
import com.example.util.SubscriptionFeatureGate
import com.example.util.SubscriptionPhase
import com.example.util.SecurityValidator
import com.example.ui.components.DeviceTransferDialog
import com.example.ui.components.CreateSubAccountDialog
import com.example.ui.components.CreateParallelSubAccountDialog
import com.example.ui.components.AccountRecoveryDialog
import com.example.ui.components.BroadcastMessagingDialog
import com.example.ui.components.ProfileOptionsBottomSheet
import com.example.ui.components.GoogleQuickBackupDialog
import com.example.ui.components.DailyFinancialJournalDialog
import com.example.data.model.DailyAccountingRecordEntity
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUser: UserEntity?,
    isOwnProfile: Boolean = true,
    onStartChatWithUser: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    adminConfig: AdminConfigEntity? = null,
    workGroups: List<WorkGroupEntity> = emptyList(),
    projects: List<ProjectEntity> = emptyList(),
    goldPurchases: List<GoldPurchaseEntity> = emptyList(),
    transactions: List<TransactionEntity> = emptyList(),
    hiddenProfiles: List<HiddenProfileEntity> = emptyList(),
    cloudBackups: List<CloudBackupRecord> = emptyList(),
    dailyRecords: List<DailyAccountingRecordEntity> = emptyList(),
    subscriptionPlans: List<SubscriptionPlanEntity> = emptyList(),
    activeAnnouncements: List<PromoAnnouncementEntity> = emptyList(),
    attendanceRecords: List<EmployeeAttendanceEntity> = emptyList(),
    isDarkTheme: Boolean = false,
    selectedPalette: com.example.ui.theme.AppThemePalette = com.example.ui.theme.AppThemePalette.ROYAL_GOLD,
    currentLanguage: AppLanguage = AppLanguage.ARABIC,
    onThemeToggle: (Boolean) -> Unit = {},
    onSelectPalette: (com.example.ui.theme.AppThemePalette) -> Unit = {},
    onLanguageChange: (AppLanguage) -> Unit = {},
    onUpdateProfileImages: (avatarUri: String?, coverUri: String?) -> Unit = { _, _ -> },
    onSubmitVerificationForm: (nationalId: String, commercialReg: String, shopAddress: String, notes: String) -> Unit = { _, _, _, _ -> },
    onSubmitSubscriptionRequest: (planId: String, planTitle: String, pricePaid: Double) -> Unit = { _, _, _ -> },
    onOpenNearbyClick: () -> Unit = {},
    onOpenSubscriptionsClick: () -> Unit = {},
    onOpenAdminClick: () -> Unit = {},
    onOpenCompanyAccountingClick: () -> Unit = {},
    onOpenMerchantStore: (merchantId: String?, isManageMode: Boolean) -> Unit = { _, _ -> },
    onUpdateAppLock: (enabled: Boolean, pin: String, biometricEnabled: Boolean) -> Unit = { _, _, _ -> },
    onCreateBackup: (passphrase: String, isGoogleDrive: Boolean) -> Unit = { _, _ -> },
    onCreateSubAccountsBackup: (passphrase: String, selectedProfileIds: List<String>?, provider: CloudStorageProvider) -> Unit = { _, _, _ -> },
    onRestoreBackup: (fileContent: String, passphrase: String) -> Unit = { _, _ -> },
    onDeleteCloudBackup: (backupId: String) -> Unit = {},
    onRefreshCloudBackups: () -> Unit = {},
    onCreateSubAccount: ((name: String, pin: String, company: String, role: String, cap: Double, silent: Boolean) -> Unit)? = null,
    onCreateParallelSubAccount: ((username: String, displayName: String, passwordSecret: String, mute: Boolean, discreet: Boolean) -> Unit)? = null,
    onAddDailyRecord: ((DailyAccountingRecordEntity) -> Unit)? = null,
    onDeleteDailyRecord: ((String) -> Unit)? = null,
    onSwitchProfile: ((HiddenProfileEntity) -> Unit)? = null,
    backupStatus: BackupResult? = null,
    isBackupProcessing: Boolean = false,
    onClearBackupStatus: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onLogout: () -> Unit = {},
    onSendBroadcast: ((BroadcastTargetCategory, String, String, Boolean, Boolean, (Int) -> Unit) -> Unit)? = null,
    onUpdateProfileInfo: ((name: String, username: String, type: UserType, jobTitle: String?, bio: String?, education: String?, sectionOrder: String?, storeName: String?, skills: String?) -> Unit)? = null
) {
    val user = currentUser ?: remember {
        UserEntity(
            id = "",
            name = "",
            phone = "",
            username = "",
            type = UserType.BUSINESSMAN.name,
            isVerified = false,
            createdAt = System.currentTimeMillis()
        )
    }

    var showAppLockDialog by remember { mutableStateOf(false) }
    var showNotificationToneDialog by remember { mutableStateOf(false) }
    var currentPinInput by remember { mutableStateOf(user.appLockPin ?: "") }
    var isAppLockActive by remember { mutableStateOf(user.appLockEnabled) }
    var showEditProfileModal by remember { mutableStateOf(false) }
    var showCvEditorModal by remember { mutableStateOf(false) }
    var showShareCvModal by remember { mutableStateOf(false) }
    var selectedCvTemplate by remember { mutableStateOf(com.example.ui.components.CvTemplateSpec.ATS_CLASSIC) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appToast = LocalAppToast.current
    var showSubscriptionUpgradeModal by remember { mutableStateOf(false) }
    var showVerificationModal by remember { mutableStateOf(false) }
    var showAvatarPickerModal by remember { mutableStateOf(false) }
    var showCoverPickerModal by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Backup & Restore State
    var showBackupModal by remember { mutableStateOf(false) }

    // Sub-Accounts Migration Hub State
    var showSubAccountsMigrationModal by remember { mutableStateOf(false) }

    // Advanced Security, Recovery, Migration & Sub-Account Hubs State
    var showDirectDeviceTransferModal by remember { mutableStateOf(false) }
    var showMnemonicPhraseModal by remember { mutableStateOf(false) }
    var showDirectSubAccountModal by remember { mutableStateOf(false) }
    var showCreateParallelSubAccountModal by remember { mutableStateOf(false) }
    var showDatabaseSecurityAuditModal by remember { mutableStateOf(false) }
    var showBroadcastModal by remember { mutableStateOf(false) }
    var showProfileOptionsMenu by remember { mutableStateOf(false) }
    val userMnemonicSeed = remember { SecurityValidator.generateRecoverySeed() }

    // Cloud Backups Vault Drawer / Modal
    var showCloudBackupsVaultModal by remember { mutableStateOf(false) }
    var showGoogleQuickBackupModal by remember { mutableStateOf(false) }
    var showDailyJournalModal by remember { mutableStateOf(false) }

    var showRestoreModal by remember { mutableStateOf(false) }
    var restoreFileContent by remember { mutableStateOf<String?>(null) }
    var restoreFileName by remember { mutableStateOf<String?>(null) }
    var restorePassphrase by remember { mutableStateOf("") }
    var restoreValidationError by remember { mutableStateOf<String?>(null) }
    var backupMetadataPreview by remember { mutableStateOf<BackupMetadataPreview?>(null) }

    val backupFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                if (content != null) {
                    restoreFileContent = content
                    restoreFileName = uri.lastPathSegment ?: "backup.oozox"
                    restoreValidationError = null
                    backupMetadataPreview = SecureBackupVaultEngine.inspectBackupMetadata(content, user.phone)
                } else {
                    restoreValidationError = "تعذر قراءة محتوى ملف النسخة الاحتياطية."
                }
            } catch (e: Exception) {
                restoreValidationError = "خطأ أثناء قراءة الملف: ${e.localizedMessage}"
            }
        }
    }

    // Camera Capture & Avatar Media State
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showCameraPermissionRationale by remember { mutableStateOf(false) }
    var avatarUpdatedFeedback by remember { mutableStateOf<String?>(null) }

    // Avatar / Cover Uri state
    var selectedAvatar by remember { mutableStateOf(user.avatarUri ?: "") }
    var selectedCover by remember { mutableStateOf(user.coverUri ?: "") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            val uriString = tempPhotoUri.toString()
            selectedAvatar = uriString
            onUpdateProfileImages(uriString, selectedCover)
            avatarUpdatedFeedback = "تم التقاط وتحديث الصورة الشخصية بنجاح 📸✨"
            showAvatarPickerModal = false
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val uriString = uri.toString()
            selectedAvatar = uriString
            onUpdateProfileImages(uriString, selectedCover)
            avatarUpdatedFeedback = "تم تعيين الصورة الشخصية من المعرض بنجاح 🖼️✨"
            showAvatarPickerModal = false
        }
    }

    // Cover Photo Editor & Picker State
    var pendingCoverEditUri by remember { mutableStateOf<Uri?>(null) }
    val coverPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingCoverEditUri = uri
            showCoverPickerModal = false
        }
    }

    // Call Privacy State
    var callPrivacyMode by remember { mutableStateOf(CallPrivacyManager.getCallPrivacy(context)) }
    var showCallPrivacyDialog by remember { mutableStateOf(false) }

    // Contacts Import State
    var isImportingContacts by remember { mutableStateOf(false) }
    val profileContactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isImportingContacts = true
            coroutineScope.launch {
                val imported = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    ContactSyncEngine.importPhoneContacts(context)
                }
                if (imported.isNotEmpty()) {
                    com.example.data.local.AppDatabase.getInstance(context).contactDao().insertContacts(imported)
                }
                isImportingContacts = false
                Toast.makeText(context, "تم استيراد وحفظ ${imported.size} جهة اتصال بنجاح في حسابك", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "يلزم منح إذن جهات الاتصال لاستيرادها", Toast.LENGTH_SHORT).show()
        }
    }

    val launchCameraCapture: () -> Unit = {
        try {
            val imagesDir = File(context.filesDir, "profile_photos").apply { mkdirs() }
            val imageFile = File(imagesDir, "AVATAR_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                imageFile
            )
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraCapture()
        } else {
            showCameraPermissionRationale = true
        }
    }

    val onCapturePhotoClick: () -> Unit = {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Evaluate Business Tier
    val businessEvaluation = remember(user, workGroups, projects, goldPurchases, transactions) {
        UserEvaluationEngine.evaluateUser(user, workGroups, projects, goldPurchases, transactions)
    }

    val totalCapitalSum = remember(projects) { projects.sumOf { it.estimatedCapital } }
    val totalGoldWeightSum = remember(goldPurchases) { goldPurchases.sumOf { it.extractedWeight } }

    val reliabilityRecords = remember(attendanceRecords) {
        UserEvaluationEngine.calculateReliabilityRecords(attendanceRecords)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // --- 1. Editorial Cover Banner & Overlapping Avatar ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(242.dp)
        ) {
            // Gradient Banner OR Custom Cover Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                val effectiveCover = selectedCover.ifBlank { user.coverUri ?: "" }
                val isImageFile = effectiveCover.isNotBlank() && (
                    effectiveCover.startsWith("/") ||
                    effectiveCover.startsWith("file://") ||
                    effectiveCover.startsWith("content://") ||
                    effectiveCover.endsWith(".jpg") ||
                    effectiveCover.endsWith(".jpeg") ||
                    effectiveCover.endsWith(".png")
                )

                if (isImageFile) {
                    AsyncImage(
                        model = effectiveCover,
                        contentDescription = "غلاف الحساب",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val tertiaryColor = MaterialTheme.colorScheme.tertiary
                    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHighest
                    val gradientBrush = when (effectiveCover) {
                        "خلفية فضية فاخرة" -> Brush.linearGradient(
                            listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF64748B))
                        )
                        "نمط المقاولات والعقارات" -> Brush.linearGradient(
                            listOf(Color(0xFF042F2E), Color(0xFF0F766E), Color(0xFF14B8A6))
                        )
                        else -> Brush.linearGradient(
                            listOf(
                                surfaceHigh,
                                primaryColor.copy(alpha = 0.32f),
                                tertiaryColor.copy(alpha = 0.24f)
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush = gradientBrush)
                            .drawBehind {
                                drawCircle(
                                    color = primaryColor.copy(alpha = 0.16f),
                                    radius = size.width * 0.42f,
                                    center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.25f)
                                )
                                drawCircle(
                                    color = tertiaryColor.copy(alpha = 0.12f),
                                    radius = size.width * 0.30f,
                                    center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.70f)
                                )
                            }
                    )
                }

                // Soft atmospheric scrim & seamless bottom fade into page background
                val bgColor = MaterialTheme.colorScheme.background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.28f),
                                    0.55f to Color.Transparent,
                                    1.0f to bgColor
                                )
                            )
                        )
                )

                // Top Action Bar (Owner: Menu & Cover Edit | Visitor: Back)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOwnProfile) {
                        Surface(
                            onClick = { showProfileOptionsMenu = true },
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_profile_hamburger_menu")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "قائمة خيارات الصفحة الشخصية",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = { showCoverPickerModal = true },
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_profile_change_cover")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "تغيير صورة الغلاف",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        if (onBackClick != null) {
                            Surface(
                                onClick = onBackClick,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_visitor_profile_back")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "رجوع",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.size(44.dp))
                        }
                        Spacer(modifier = Modifier.size(44.dp))
                    }
                }
            }

            // Circular Profile Picture with Halo Ring
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp)
                    .size(100.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OozoxAvatarImage(
                        model = user.avatarUri,
                        name = user.name,
                        size = 94.dp,
                        isTrader = user.type == UserType.GOLD_TRADER.name,
                        contentDescription = user.name,
                        modifier = Modifier
                            .testTag("profile_avatar_image")
                            .then(if (isOwnProfile) Modifier.clickable { showAvatarPickerModal = true } else Modifier)
                    )
                }

                // Camera Icon Badge for Quick Avatar Capture / Update (Owner Only)
                if (isOwnProfile) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .align(Alignment.BottomEnd)
                            .testTag("profile_avatar_camera_btn")
                            .clickable { showAvatarPickerModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "التقاط صورة شخصية بالكاميرا",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Small Icon-Only Edit Profile Button opposite Avatar (Owner Only)
            if (isOwnProfile) {
                Surface(
                    onClick = { showEditProfileModal = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 6.dp)
                        .size(38.dp)
                        .testTag("btn_hero_edit_profile")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل الملف الشخصي",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Feedback Banner on Avatar Updated
        AnimatedVisibility(
            visible = avatarUpdatedFeedback != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            avatarUpdatedFeedback?.let { msg ->
                Surface(
                    color = adaptiveStatusGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, adaptiveStatusGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = adaptiveStatusGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(msg, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = adaptiveStatusGreen)
                        }
                        IconButton(
                            onClick = { avatarUpdatedFeedback = null },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = adaptiveStatusGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // --- 2. User Info & Verification Status Header ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (user.isVerified || user.verificationStatus == "APPROVED") {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "حساب موثق",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    // Tappable Username Pill & Quiet Phone Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("username", user.username))
                                Toast.makeText(context, "تم نسخ اسم المستخدم", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                            modifier = Modifier.testTag("copy_profile_username_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ اسم المستخدم",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        if (isOwnProfile && user.phone.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = user.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val displayRole = when {
                        !user.jobTitle.isNullOrBlank() -> user.jobTitle
                        user.type == UserType.GOLD_TRADER.name -> "تاجر وصائغ ذهب معتمد"
                        else -> "عضو معتمد"
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (user.type == UserType.GOLD_TRADER.name) Icons.Default.Diamond else Icons.Default.BusinessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = displayRole,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Verification Action (Owner only, when pending or not yet verified)
                if (isOwnProfile && !user.isVerified && user.verificationStatus != "APPROVED") {
                    if (user.verificationStatus == "PENDING") {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "التوثيق قيد المراجعة",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = { showVerificationModal = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("توثيق الحساب", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- Visitor Action Buttons (Shown ONLY to visitors) ---
            if (!isOwnProfile) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onStartChatWithUser?.invoke() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_visitor_start_chat")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مراسلة", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }

                    if (user.isVerified || user.verificationStatus == "APPROVED" || user.type == UserType.GOLD_TRADER.name || !user.traderShopName.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = { onOpenMerchantStore(user.id, false) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_visitor_open_store")
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = user.traderShopName?.takeIf { it.isNotBlank() } ?: "عرض المتجر",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // --- Clean Unified Stats Card (Owner vs Visitor) ---
            val primaryStatColor = MaterialTheme.colorScheme.primary
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                Brush.verticalGradient(
                                    listOf(primaryColorOrDefault(primaryStatColor).copy(alpha = 0.08f), Color.Transparent)
                                )
                            )
                        }
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOwnProfile) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                            Text("رأس المال", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = GoldUtils.formatCurrency(totalCapitalSum),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(17.dp))
                            Text("حيازة الذهب", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = GoldUtils.formatWeight(totalGoldWeightSum),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                            Text("حالة العضوية", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (user.isVerified || user.verificationStatus == "APPROVED") "حساب موثق" else "عضو مسجل",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(17.dp))
                        Text("درجة الثقة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${businessEvaluation.trustScore}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StatusGreen
                        )
                    }
                }
            }

            // --- Professional Bio & Credentials Card ---
            if (!user.jobTitle.isNullOrBlank() || !user.education.isNullOrBlank() || !user.merchantBio.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!user.jobTitle.isNullOrBlank() || !user.education.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!user.jobTitle.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.WorkOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = user.jobTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (!user.education.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = user.education,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        if (!user.merchantBio.isNullOrBlank()) {
                            Text(
                                text = user.merchantBio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            // --- Reliability Summary (Clean, Decluttered) ---
            if (reliabilityRecords.isNotEmpty()) {
                val cvLayoutDirection = currentLanguage.direction
                CompositionLocalProvider(LocalLayoutDirection provides cvLayoutDirection) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = tr("cv_section_title", "سجل الموثوقية والالتزام"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        reliabilityRecords.forEach { record ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = record.period,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        val isIdeal = record.overallRating.contains("مثالي") || record.overallRating.contains("Ideal")
                                        val ratingLabel = if (isIdeal) tr("cv_ideal_rating", record.overallRating) else record.overallRating
                                        Surface(
                                            color = if (isIdeal) StatusGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = ratingLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isIdeal) StatusGreen else MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(tr("cv_attendance", "الحضور"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("${record.presenceRate}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(tr("cv_location", "الموقع الجغرافي"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                "${record.locationCompliance}%",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (record.locationCompliance >= 90) StatusGreen else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(tr("cv_punctuality", "الالتزام"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("${record.punctuality}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Owner Quick Shortcuts (All heavy settings moved to ProfileOptionsBottomSheet) ---
            if (isOwnProfile) {
                val subDetails = remember(user.subscriptionTier, user.subscriptionEndTimestamp, user.subscriptionStartTimestamp) {
                    SubscriptionFeatureGate.evaluateSubscription(user)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProfileSectionTitle("اختصارات سريعة")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (user.isVerified || user.verificationStatus == "APPROVED" || user.type == UserType.GOLD_TRADER.name || !user.traderShopName.isNullOrBlank()) {
                                val customStoreTitle = user.traderShopName?.takeIf { it.isNotBlank() } ?: "المتجر الخاص"
                                ProfileCleanSettingRow(
                                    icon = Icons.Default.Storefront,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    iconBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    title = customStoreTitle,
                                    subtitle = "إدارة المنتجات والخدمات والاسم التجاري",
                                    badge = "إدارة",
                                    onClick = { onOpenMerchantStore(user.id, true) }
                                )
                                ProfileInsetDivider()
                            }

                            ProfileCleanSettingRow(
                                icon = Icons.Default.WorkspacePremium,
                                iconTint = MaterialTheme.colorScheme.primary,
                                iconBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                title = "باقات الاشتراك والترقية",
                                subtitle = subDetails.tier.label,
                                badge = when (subDetails.phase) {
                                    SubscriptionPhase.PAID_ACTIVE -> tr("paid_active", "نشط")
                                    SubscriptionPhase.TRIAL_ACTIVE -> tr("free_trial", "تجربة")
                                    else -> tr("upgrade", "ترقية")
                                },
                                badgeColor = if (subDetails.phase == SubscriptionPhase.PAID_ACTIVE) MaterialTheme.colorScheme.primary else StatusGreen,
                                onClick = { showSubscriptionUpgradeModal = true }
                            )

                            ProfileInsetDivider()

                            ProfileCleanSettingRow(
                                icon = Icons.Default.Tune,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                iconBg = MaterialTheme.colorScheme.surfaceContainerHighest,
                                title = "المزيد من الخيارات والإعدادات",
                                subtitle = "المشاركة، التصدير، الأمان، والنسخ الاحتياطي",
                                onClick = { showProfileOptionsMenu = true }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Modal 1: Account Verification Request Form
    if (showVerificationModal) {
        ProfileVerificationDialog(
            initialShopAddress = user.traderShopAddress ?: "",
            onSubmit = onSubmitVerificationForm,
            onDismiss = { showVerificationModal = false }
        )
    }

    // Modal 2: Human-Crafted Responsive Account Cover Style Studio
    if (showCoverPickerModal) {
        ProfileCoverPickerDialog(
            userName = user.name,
            selectedCover = selectedCover,
            currentLanguage = currentLanguage,
            onApplyCover = { chosenCover ->
                selectedCover = chosenCover
                onUpdateProfileImages(selectedAvatar, chosenCover)
            },
            onPickFromGallery = {
                showCoverPickerModal = false
                coverPhotoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = { showCoverPickerModal = false }
        )
    }

    // Visual Cover Photo Editor Dialog (Zoom In, Zoom Out, Pan & Crop)
    if (pendingCoverEditUri != null) {
        CoverPhotoEditorDialog(
            imageUri = pendingCoverEditUri!!,
            onSaveCroppedCover = { savedPath ->
                selectedCover = savedPath
                onUpdateProfileImages(selectedAvatar, savedPath)
                pendingCoverEditUri = null
                Toast.makeText(context, "تم حفظ وضبط صورة الغلاف بنجاح ✨", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                pendingCoverEditUri = null
            }
        )
    }

    // Call Privacy Settings Dialog
    if (showCallPrivacyDialog) {
        ProfileCallPrivacyDialog(
            currentMode = callPrivacyMode,
            onSelectPrivacy = { option ->
                callPrivacyMode = option
                CallPrivacyManager.setCallPrivacy(context, option)
            },
            onDismiss = { showCallPrivacyDialog = false }
        )
    }

    // Modal 3: Change Profile Avatar (Camera Capture & Gallery & Presets)
    if (showAvatarPickerModal) {
        ProfileAvatarPickerDialog(
            hasCustomAvatar = !user.avatarUri.isNullOrBlank(),
            onCapturePhotoClick = onCapturePhotoClick,
            onPickGalleryClick = { galleryLauncher.launch("image/*") },
            onSelectPresetEmoji = { emoji ->
                selectedAvatar = emoji
                onUpdateProfileImages(selectedAvatar, selectedCover)
                avatarUpdatedFeedback = "تم اختيار رمز الأفاتار بنجاح ✨"
                showAvatarPickerModal = false
            },
            onRemoveAvatar = {
                selectedAvatar = ""
                onUpdateProfileImages(null, selectedCover)
                avatarUpdatedFeedback = "تمت إزالة الصورة الشخصية والعودة للافتراضي"
                showAvatarPickerModal = false
            },
            onDismiss = { showAvatarPickerModal = false }
        )
    }

    // Modal 3.5: Camera Permission Rationale
    if (showCameraPermissionRationale) {
        ProfileCameraPermissionRationaleDialog(
            onGrantClick = {
                showCameraPermissionRationale = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onDismiss = { showCameraPermissionRationale = false }
        )
    }

    // Modal 4: Secure Encrypted Backup Modal (AES-256-GCM + Phone Binding)
    if (showBackupModal) {
        ProfileCreateBackupDialog(
            userPhone = user.phone,
            onStartBackup = onCreateBackup,
            onDismiss = { showBackupModal = false }
        )
    }

    // Modal 4.2: Targeted Sub-Accounts & Secret Vaults Migration Modal
    if (showSubAccountsMigrationModal) {
        ProfileSubAccountsMigrationDialog(
            hiddenProfiles = hiddenProfiles,
            onCreateSubAccountsBackup = onCreateSubAccountsBackup,
            onDismiss = { showSubAccountsMigrationModal = false }
        )
    }

    // Modal 4.4: Cloud Backups Registry Modal (Google Drive & Oozox Cloud)
    if (showCloudBackupsVaultModal) {
        ProfileCloudBackupsVaultDialog(
            userPhone = user.phone,
            cloudBackups = cloudBackups,
            onRestoreBackup = { backup ->
                restoreFileContent = backup.encryptedPayloadBase64
                restoreFileName = backup.fileName
                backupMetadataPreview = SecureBackupVaultEngine.inspectBackupMetadata(backup.encryptedPayloadBase64, user.phone)
                showCloudBackupsVaultModal = false
                showRestoreModal = true
            },
            onDeleteCloudBackup = onDeleteCloudBackup,
            onDismiss = { showCloudBackupsVaultModal = false }
        )
    }

    // Modal 4.5: Secure Restore Modal (Phone Match Verification & Live Metadata Preview)
    if (showRestoreModal) {
        ProfileRestoreBackupDialog(
            userPhone = user.phone,
            restoreFileContent = restoreFileContent,
            restoreFileName = restoreFileName,
            restorePassphrase = restorePassphrase,
            onRestorePassphraseChange = { restorePassphrase = it },
            restoreValidationError = restoreValidationError,
            onRestoreValidationErrorChange = { restoreValidationError = it },
            backupMetadataPreview = backupMetadataPreview,
            onPickFileClick = { backupFilePickerLauncher.launch("*/*") },
            onConfirmRestore = onRestoreBackup,
            onDismiss = { showRestoreModal = false }
        )
    }

    // Modal 4.8: Backup/Restore Result or Progress Dialog
    ProfileBackupProgressAndResultDialogs(
        isBackupProcessing = isBackupProcessing,
        backupStatus = backupStatus,
        onClearBackupStatus = onClearBackupStatus
    )

    // Modal 5: Account Deletion (Google Play / App Store Compliance)
    if (showDeleteAccountDialog) {
        ProfileDeleteAccountDialog(
            onConfirmDelete = onDeleteAccount,
            onDismiss = { showDeleteAccountDialog = false }
        )
    }

    // Modal 6: Privacy Policy & Data Protection Disclosure
    if (showPrivacyPolicyDialog) {
        ProfilePrivacyPolicyDialog(
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }

    // Modal 7: Log Out Confirmation Dialog
    if (showLogoutDialog) {
        ProfileLogoutDialog(
            onConfirmLogout = onLogout,
            onDismiss = { showLogoutDialog = false }
        )
    }

    // Custom Message Notification Tones Dialog
    if (showNotificationToneDialog) {
        com.example.ui.components.MessageNotificationToneDialog(
            onDismiss = { showNotificationToneDialog = false }
        )
    }

    // Modal 8: App Lock & Biometric / PIN Configuration Dialog
    if (showAppLockDialog) {
        ProfileAppLockDialog(
            user = user,
            onSaveAppLock = onUpdateAppLock,
            onDismiss = { showAppLockDialog = false }
        )
    }

    // Subscription & Upgrade Popup Modal (نافذة منبثقة بجميع الباقات والعروض)
    if (showSubscriptionUpgradeModal) {
        SubscriptionUpgradeModal(
            currentUser = user,
            subscriptionPlans = subscriptionPlans,
            activeAnnouncements = activeAnnouncements,
            onDismiss = { showSubscriptionUpgradeModal = false },
            onSubmitSubscriptionRequest = { planId, planTitle, pricePaid ->
                onSubmitSubscriptionRequest(planId, planTitle, pricePaid)
            },
            onOpenFullSubscriptionsScreen = {
                showSubscriptionUpgradeModal = false
                onOpenSubscriptionsClick()
            }
        )
    }

    // Modal: Phone-to-Phone Migration & Device Transfer Dialog
    if (showDirectDeviceTransferModal) {
        DeviceTransferDialog(
            currentUser = user,
            onDismiss = { showDirectDeviceTransferModal = false },
            onMigrateCompleteOnNewPhone = { _, _, _, _ ->
                showDirectDeviceTransferModal = false
                appToast.showToast("تمت مزامنة وترحيل بيانات الجهاز بنجاح 📲✨", AppToastType.SUCCESS)
            }
        )
    }

    // Modal: 12-Word Mnemonic Vault Phrase Modal
    if (showMnemonicPhraseModal) {
        ProfileMnemonicPhraseDialog(
            userMnemonicSeed = userMnemonicSeed,
            onDismiss = { showMnemonicPhraseModal = false }
        )
    }

    // Modal: Create Parallel Hidden Sub-Account Dialog
    if (showCreateParallelSubAccountModal) {
        CreateParallelSubAccountDialog(
            onDismiss = { showCreateParallelSubAccountModal = false },
            onCreateSubAccount = { username, displayName, passwordSecret, mute, discreet ->
                showCreateParallelSubAccountModal = false
                onCreateParallelSubAccount?.invoke(username, displayName, passwordSecret, mute, discreet)
                appToast.showToast("تم إنشاء الحساب الفرعي المخفي بنجاح! اكتب كلمة المرور في البحث لفتحه 🔒✨", AppToastType.SUCCESS)
            }
        )
    }

    // Modal: Fast Google Cloud Backup (النسخ الاحتياطي السحابي السريع مع زر Google)
    if (showGoogleQuickBackupModal) {
        GoogleQuickBackupDialog(
            currentUser = user,
            cloudBackups = cloudBackups,
            isProcessing = isBackupProcessing,
            onDismiss = { showGoogleQuickBackupModal = false },
            onCreateGoogleBackup = { passphrase ->
                onCreateBackup(passphrase, true)
                appToast.showToast("جاري التشفير والرفع إلى Google Drive... ☁️", AppToastType.INFO)
            },
            onRestoreBackup = { backupRecord ->
                restoreFileContent = backupRecord.encryptedPayloadBase64
                restoreFileName = backupRecord.fileName
                backupMetadataPreview = SecureBackupVaultEngine.inspectBackupMetadata(backupRecord.encryptedPayloadBase64, user.phone)
                showRestoreModal = true
            },
            onDeleteBackup = { backupId ->
                onDeleteCloudBackup(backupId)
                appToast.showToast("تم حذف النسخة السحابية بنجاح", AppToastType.SUCCESS)
            }
        )
    }

    // Modal: Daily Financial Journal (دفتر اليومية المحاسبي المالي)
    if (showDailyJournalModal) {
        DailyFinancialJournalDialog(
            currentUser = user,
            dailyRecords = dailyRecords,
            onDismiss = { showDailyJournalModal = false },
            onAddRecord = { newRecord ->
                onAddDailyRecord?.invoke(newRecord)
                appToast.showToast("تم ترحيل وحفظ قيد اليومية بنجاح 📑", AppToastType.SUCCESS)
            },
            onDeleteRecord = { recordId ->
                onDeleteDailyRecord?.invoke(recordId)
                appToast.showToast("تم حذف قيد اليومية", AppToastType.INFO)
            }
        )
    }

    // Modal: Create Sub-Account Dialog
    if (showDirectSubAccountModal) {
        CreateSubAccountDialog(
            onDismiss = { showDirectSubAccountModal = false },
            onCreateSubAccount = { subName, subPin, subComp, subRole, subCap, subSilent ->
                showDirectSubAccountModal = false
                onCreateSubAccount?.invoke(subName, subPin, subComp, subRole, subCap, subSilent)
                appToast.showToast("تم إنشاء الحساب الفرعي والخزانة بنجاح 👥✨", AppToastType.SUCCESS)
            }
        )
    }

    // Modal: Database Security Audit Dialog
    if (showDatabaseSecurityAuditModal) {
        ProfileDatabaseSecurityAuditDialog(
            onDismiss = { showDatabaseSecurityAuditModal = false }
        )
    }

    // Broadcast Messaging Dialog
    if (showBroadcastModal) {
        BroadcastMessagingDialog(
            onSendBroadcast = { targetCategory, title, content, isUrgent, postAsAnnouncement, onComplete ->
                if (onSendBroadcast != null) {
                    onSendBroadcast(targetCategory, title, content, isUrgent, postAsAnnouncement, onComplete)
                } else {
                    onComplete(0)
                }
            },
            onDismiss = { showBroadcastModal = false }
        )
    }

    // Modal: CV Section & Template Customizer
    if (showCvEditorModal) {
        com.example.ui.components.CvSectionCustomizerDialog(
            user = user,
            currentLanguage = currentLanguage,
            selectedTemplate = selectedCvTemplate,
            onSelectTemplate = { selectedCvTemplate = it },
            reliabilityRecords = reliabilityRecords,
            trustScore = businessEvaluation.trustScore,
            onDismiss = { showCvEditorModal = false },
            onSaveSectionOrder = { newOrder ->
                onUpdateProfileInfo?.invoke(
                    user.name,
                    user.username,
                    if (user.type == UserType.GOLD_TRADER.name) UserType.GOLD_TRADER else UserType.BUSINESSMAN,
                    user.jobTitle,
                    user.merchantBio,
                    user.education,
                    newOrder,
                    user.traderShopName,
                    user.skills
                )
                appToast.showToast("تم حفظ تخصيص وترتيب السيرة الذاتية", AppToastType.SUCCESS)
            }
        )
    }

    // Modal: Professional CV Link, Client Card & 12 ATS Templates Hub
    if (showShareCvModal) {
        com.example.ui.components.ProfileShareAndCvHubDialog(
            user = user,
            reliabilityRecords = reliabilityRecords,
            trustScore = businessEvaluation.trustScore,
            currentLanguage = currentLanguage,
            selectedTemplate = selectedCvTemplate,
            onSelectTemplate = { selectedCvTemplate = it },
            projectsCount = projects.size,
            completedDealsCount = transactions.size + goldPurchases.size,
            onOpenSectionCustomizer = {
                showShareCvModal = false
                showCvEditorModal = true
            },
            onDismiss = { showShareCvModal = false }
        )
    }

    // ZOX admin backdoor switch removed in production.

    // Modal: Professional Edit User Profile
    if (showEditProfileModal) {
        com.example.ui.components.EditProfileProfessionalDialog(
            user = user,
            currentLanguage = currentLanguage,
            onDismiss = { showEditProfileModal = false },
            onSave = { newName, newUsername, newType, newJob, newBio, newEdu, newStoreName, newSkills ->
                onUpdateProfileInfo?.invoke(
                    newName,
                    newUsername,
                    newType,
                    newJob,
                    newBio,
                    newEdu,
                    user.cvSectionOrder,
                    newStoreName,
                    newSkills
                )
                appToast.showToast(
                    if (currentLanguage == AppLanguage.ARABIC) "تم تحديث بيانات الملف الشخصي بنجاح" else "Profile updated successfully",
                    AppToastType.SUCCESS
                )
                showEditProfileModal = false
            }
        )
    }

    // --- Executive Profile Options Bottom Sheet (3 Horizontal Bars Menu) ---
    if (showProfileOptionsMenu && isOwnProfile) {
        val isZoxAdmin = user.type == UserType.ADMIN.name
        ProfileOptionsBottomSheet(
            onDismiss = { showProfileOptionsMenu = false },
            onExportCvPdf = {
                ReportExporter.exportEmployeeCvToPdf(
                    context = context,
                    user = user,
                    reliabilityRecords = reliabilityRecords,
                    language = currentLanguage,
                    templateId = selectedCvTemplate.id,
                    customSectionOrder = user.cvSectionOrder?.split(",")
                )
            },
            onShareProfile = { showShareCvModal = true },
            onCustomizeCv = { showCvEditorModal = true },
            onEditProfile = { showEditProfileModal = true },
            onOpenSubscriptions = { showSubscriptionUpgradeModal = true },
            onOpenVerification = { showVerificationModal = true },
            isVerified = user.isVerified || user.verificationStatus == "APPROVED",
            onOpenSecuritySettings = {
                currentPinInput = user.appLockPin ?: ""
                isAppLockActive = user.appLockEnabled
                showAppLockDialog = true
            },
            onOpenCallPrivacy = { showCallPrivacyDialog = true },
            onImportContacts = {
                profileContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            onOpenNotificationTones = { showNotificationToneDialog = true },
            onOpenFloatingBubble = {
                val hasOverlayPermission = com.example.util.MessengerBubbleManager.canDrawOverlays(context)
                if (!hasOverlayPermission) {
                    com.example.util.MessengerBubbleManager.requestPermissionWithAutoLaunch(context)
                } else {
                    val nextState = !com.example.util.MessengerBubbleManager.isBubbleFeatureEnabled.value
                    com.example.util.MessengerBubbleManager.setBubbleFeatureEnabled(context, nextState)
                    appToast.showToast(
                        if (nextState) "تم تفعيل الفقاعة العائمة للمحادثات" else "تم إيقاف الفقاعة العائمة",
                        AppToastType.INFO
                    )
                }
            },
            onOpenCompanyAccounting = onOpenCompanyAccountingClick,
            onOpenNearby = onOpenNearbyClick,
            onOpenDeviceTransfer = { showDirectDeviceTransferModal = true },
            onOpenDailyJournal = { showDailyJournalModal = true },
            onOpenGoogleBackup = {
                onRefreshCloudBackups()
                showGoogleQuickBackupModal = true
            },
            onOpenCloudBackups = {
                onRefreshCloudBackups()
                showCloudBackupsVaultModal = true
            },
            onOpenLocalBackup = {
                showBackupModal = true
            },
            onOpenRestoreBackup = {
                restoreFileContent = null
                restoreFileName = null
                restorePassphrase = ""
                restoreValidationError = null
                backupMetadataPreview = null
                showRestoreModal = true
            },
            onOpenSubAccounts = { showCreateParallelSubAccountModal = true },
            onOpenRecoverySeed = { showMnemonicPhraseModal = true },
            onOpenSecurityAudit = { showDatabaseSecurityAuditModal = true },
            onOpenPrivacyPolicy = { showPrivacyPolicyDialog = true },
            onOpenHelpSupport = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://t.me/oozox_support"))
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "فريق الدعم الفني: support@oozox.com", Toast.LENGTH_LONG).show()
                }
            },
            onOpenStorefront = if (user.isVerified || user.verificationStatus == "APPROVED" || user.type == UserType.GOLD_TRADER.name || !user.traderShopName.isNullOrBlank()) {
                { onOpenMerchantStore(user.id, true) }
            } else null,
            storeName = user.traderShopName,
            onOpenBroadcast = if (isZoxAdmin) { { showBroadcastModal = true } } else null,
            onOpenAdmin = if (isZoxAdmin) onOpenAdminClick else null,
            onDeleteAccount = { showDeleteAccountDialog = true },
            onLogout = { showLogoutDialog = true }
        )
    }
}

@Composable
private fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
    )
}

private fun primaryColorOrDefault(color: Color): Color = color.copy(alpha = 0.06f)

@Composable
private fun ProfileCleanSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconBg: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitle: String? = null,
    badge: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBg,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!badge.isNullOrBlank()) {
                Surface(
                    color = badgeColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ProfileInsetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp, end = 16.dp),
        thickness = 0.6.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}

