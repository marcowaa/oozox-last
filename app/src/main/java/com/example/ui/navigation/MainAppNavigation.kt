package com.example.ui.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.navigation.toRoute
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.icons.AppIcons
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AppLanguage
import com.example.util.AppLocalizationProvider
import com.example.util.AppNavDestination
import com.example.util.CloudStorageProvider
import com.example.util.LocalAppLanguage
import com.example.util.NotificationHelper
import com.example.util.OfflineSyncManager
import com.example.util.Translations
import com.example.util.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allContacts by viewModel.allContacts.collectAsStateWithLifecycle()
    val allChats by viewModel.allChats.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allGoldPurchases by viewModel.allGoldPurchases.collectAsStateWithLifecycle()
    val allWorkGroups by viewModel.allWorkGroups.collectAsStateWithLifecycle()
    val allGroupMembers by viewModel.allGroupMembers.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val allTraderRequests by viewModel.allTraderRequests.collectAsStateWithLifecycle()
    val adminConfig by viewModel.adminConfig.collectAsStateWithLifecycle()
    val goldPrices by viewModel.goldPrices.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val selectedThemePalette by viewModel.selectedThemePalette.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val allNearbyUsers by viewModel.allNearbyUsers.collectAsStateWithLifecycle()
    val pendingSyncItems by viewModel.pendingSyncItems.collectAsStateWithLifecycle()
    val isRefreshingChats by viewModel.isRefreshingChats.collectAsStateWithLifecycle()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsStateWithLifecycle()
    val activeHiddenProfile by viewModel.activeHiddenProfile.collectAsStateWithLifecycle()
    val activeSubAccount by viewModel.activeSubAccount.collectAsStateWithLifecycle()
    val currentDisplayChats by viewModel.currentDisplayChats.collectAsStateWithLifecycle()

    val allHashtagEntries by viewModel.allHashtagEntries.collectAsStateWithLifecycle()
    val allLedgerEntries by viewModel.allLedgerEntries.collectAsStateWithLifecycle()
    val allSettlements by viewModel.allSettlements.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allPriceAlerts by viewModel.allPriceAlerts.collectAsStateWithLifecycle()
    val totalUnreadChats = remember(allChats) { allChats.sumOf { it.unreadCount } }
    val subscriptionPlans by viewModel.allSubscriptionPlans.collectAsStateWithLifecycle()
    val subscriptionRequests by viewModel.allSubscriptionRequests.collectAsStateWithLifecycle()
    val activePromoAnnouncements by viewModel.activePromoAnnouncements.collectAsStateWithLifecycle()
    val allPromoAnnouncements by viewModel.allPromoAnnouncements.collectAsStateWithLifecycle()
    val allHiddenProfiles by viewModel.allHiddenProfiles.collectAsStateWithLifecycle()
    val repeatUser by viewModel.repeatUser.collectAsStateWithLifecycle()
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val allMerchantProducts by viewModel.allMerchantProducts.collectAsStateWithLifecycle()
    val allProjectTasks by viewModel.allProjectTasks.collectAsStateWithLifecycle()
    val allProjectMembers by viewModel.allProjectMembers.collectAsStateWithLifecycle()
    val cloudBackups by viewModel.cloudBackups.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()
    val isBackupProcessing by viewModel.isBackupProcessing.collectAsStateWithLifecycle()

    val allDailyAccountingRecords by viewModel.allDailyAccountingRecords.collectAsStateWithLifecycle()
    val allAttendanceRecords by viewModel.allAttendanceRecords.collectAsStateWithLifecycle()
    val allSubAccounts by viewModel.allSubAccounts.collectAsStateWithLifecycle()
    val allShiftHandovers by viewModel.allShiftHandovers.collectAsStateWithLifecycle()
    val allFinancialComplaints by viewModel.allFinancialComplaints.collectAsStateWithLifecycle()
    val allChatAccounts by viewModel.allChatAccounts.collectAsStateWithLifecycle()
    val allHashtagAccounts by viewModel.allHashtagAccounts.collectAsStateWithLifecycle()

    val globalAccountingSnapshot by viewModel.globalAccountingSnapshot.collectAsStateWithLifecycle()
    val businessProfile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val allPayrollAdjustments by viewModel.allPayrollAdjustments.collectAsStateWithLifecycle()
    val allEmployeeLeaves by viewModel.allEmployeeLeaves.collectAsStateWithLifecycle()

    var showNewChatDialog by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showAddPurchaseDialog by remember { mutableStateOf(false) }
    var showFinancialAccountantDialog by remember { mutableStateOf(false) }
    var showVoiceBusinessAgentDialog by remember { mutableStateOf(false) }
    var showInvoiceVerificationModal by remember { mutableStateOf(false) }
    var showEncryptedVaultSheet by remember { mutableStateOf(false) }
    var activeChatForSmartAccountant by remember { mutableStateOf<ChatEntity?>(null) }
    var isAppUnlocked by remember { mutableStateOf(false) }
    var pendingMainTabIndex by remember { mutableStateOf<Int?>(null) }

    val navigateToAppDestination: (AppNavDestination) -> Unit = { dest ->
        when (dest) {
            AppNavDestination.CHATS -> {
                pendingMainTabIndex = BottomTab.CHATS.ordinal
                navController.popBackStack(Screen.MainTabs, inclusive = false)
            }
            AppNavDestination.PROJECTS -> {
                pendingMainTabIndex = BottomTab.PROJECTS.ordinal
                navController.popBackStack(Screen.MainTabs, inclusive = false)
            }
            AppNavDestination.BUSINESS -> {
                pendingMainTabIndex = BottomTab.BUSINESS.ordinal
                navController.popBackStack(Screen.MainTabs, inclusive = false)
            }
            AppNavDestination.INVOICES -> {
                pendingMainTabIndex = BottomTab.TRANSACTIONS.ordinal
                navController.popBackStack(Screen.MainTabs, inclusive = false)
            }
            AppNavDestination.PROFILE -> {
                pendingMainTabIndex = BottomTab.PROFILE.ordinal
                navController.popBackStack(Screen.MainTabs, inclusive = false)
            }
            AppNavDestination.GOLD -> navController.navigate(Screen.Gold)
            AppNavDestination.STORE -> navController.navigate(
                Screen.MerchantStore(
                    merchantId = currentUser?.id ?: "",
                    isManageMode = currentUser?.type == UserType.GOLD_TRADER.name || currentUser?.type == UserType.BUSINESSMAN.name
                )
            )
            AppNavDestination.ADMIN -> navController.navigate(Screen.Admin)
            AppNavDestination.ACCOUNTING_HUB -> navController.navigate(Screen.CompanyAccounting) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(currentUser?.id, currentUser?.appLockEnabled) {
        if (currentUser == null || currentUser?.appLockEnabled != true) {
            isAppUnlocked = true
        } else {
            isAppUnlocked = false
        }
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val pendingBubbleChatId by com.example.util.MessengerBubbleManager.pendingChatNavigation.collectAsStateWithLifecycle()
    val pendingCardHandle by com.example.util.ClientCardDeepLinkRouter.pendingCardHandle.collectAsStateWithLifecycle()

    LaunchedEffect(pendingBubbleChatId) {
        val targetChatId = pendingBubbleChatId
        if (!targetChatId.isNullOrBlank()) {
            navController.navigate(Screen.ChatDetail(targetChatId))
            com.example.util.MessengerBubbleManager.clearPendingChatNavigation()
        }
    }

    AppLocalizationProvider(currentLanguage = currentLanguage) {
        LaunchedEffect(isInitialized, currentUser) {
            if (isInitialized && currentUser == null) {
                navController.navigate(Screen.Onboarding) {
                    popUpTo(0) { inclusive = true }
                }
            } else if (isInitialized && currentUser != null) {
                // Check if opened from an FCM push notification with deep-link payload
                val destination = activity?.intent?.getStringExtra(NotificationHelper.EXTRA_DESTINATION)
                val chatId = activity?.intent?.getStringExtra(NotificationHelper.EXTRA_CHAT_ID)
                val projectId = activity?.intent?.getStringExtra(NotificationHelper.EXTRA_PROJECT_ID)
                val transactionId = activity?.intent?.getStringExtra(NotificationHelper.EXTRA_TRANSACTION_ID)

                if (!destination.isNullOrBlank()) {
                    when (destination) {
                        NotificationHelper.DESTINATION_CHAT -> {
                            if (!chatId.isNullOrBlank()) {
                                navController.navigate(Screen.ChatDetail(chatId))
                            }
                        }
                        NotificationHelper.DESTINATION_PROJECT -> {
                            if (!projectId.isNullOrBlank()) {
                                navController.navigate(Screen.ProjectDetail(projectId))
                            }
                        }
                        NotificationHelper.DESTINATION_TRANSACTION -> {
                            if (!transactionId.isNullOrBlank()) {
                                navController.navigate(Screen.TransactionDetail(transactionId))
                            }
                        }
                    }
                    // Clear intent extras to avoid repeating navigation on configuration change
                    activity.intent?.removeExtra(NotificationHelper.EXTRA_DESTINATION)
                    activity.intent?.removeExtra(NotificationHelper.EXTRA_CHAT_ID)
                    activity.intent?.removeExtra(NotificationHelper.EXTRA_PROJECT_ID)
                    activity.intent?.removeExtra(NotificationHelper.EXTRA_TRANSACTION_ID)
                }
            }
        }

        val toastController = remember { AppToastController() }

        CompositionLocalProvider(
            LocalAppToast provides toastController
        ) {
            GoldChatTheme(darkTheme = isDarkTheme, palette = selectedThemePalette) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (currentUser != null && currentUser?.appLockEnabled == true && !currentUser?.appLockPin.isNullOrBlank() && !isAppUnlocked) {
                        BiometricLockScreen(
                            correctPin = currentUser?.appLockPin ?: "",
                            userName = currentUser?.name ?: tr("lock.default_user"),
                            userTypeLabel = if (currentUser?.type == UserType.GOLD_TRADER.name) tr("drawer.verified_trader") else tr("drawer.role_investor"),
                            isBiometricEnabled = currentUser?.biometricAuthEnabled ?: true,
                            onUnlocked = {
                                isAppUnlocked = true
                            }
                        )
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.MainTabs
                        ) {
                composable<Screen.Onboarding> {
                    OnboardingScreen(
                        repeatUser = repeatUser,
                        allHiddenProfiles = allHiddenProfiles,
                        onLoginSuccess = { name, phone, username, type ->
                            viewModel.loginOrRegister(name, phone, username, type)
                            navController.navigate(Screen.MainTabs) {
                                popUpTo(Screen.Onboarding) { inclusive = true }
                            }
                        },
                        onCreateSubAccountDirectly = { sName, sPin, sComp, sRole, sCap, sSilent ->
                            viewModel.createHiddenProfile(sName, sPin, sComp, sRole, sCap, sSilent)
                        },
                        onSwitchProfile = { prof ->
                            viewModel.switchActiveProfile(prof)
                        }
                    )
                }

                composable<Screen.MainTabs> {
                    val pagerState = rememberPagerState(initialPage = 0, pageCount = { BottomTab.entries.size })
                    val coroutineScope = rememberCoroutineScope()
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    var isDesktopSidebarOpen by remember { mutableStateOf(true) }
                    var isFocusMode by remember { mutableStateOf(false) }

                    BackHandler(enabled = drawerState.isOpen) {
                        coroutineScope.launch { drawerState.close() }
                    }

                    LaunchedEffect(pendingMainTabIndex) {
                        val targetPage = pendingMainTabIndex
                        if (targetPage != null && targetPage in 0 until BottomTab.entries.size) {
                            pagerState.scrollToPage(targetPage)
                            pendingMainTabIndex = null
                        }
                    }

                    val onTabSelected = remember(coroutineScope, pagerState) {
                        { index: Int ->
                            coroutineScope.launch {
                                if (kotlin.math.abs(pagerState.currentPage - index) > 1) {
                                    pagerState.scrollToPage(index)
                                } else {
                                    pagerState.animateScrollToPage(
                                        page = index,
                                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                                    )
                                }
                            }
                            Unit
                        }
                    }

                    val onOpenDrawerAction = remember(coroutineScope, drawerState) {
                        {
                            coroutineScope.launch { drawerState.open() }
                            Unit
                        }
                    }
                    val onCloseDrawerAction = remember(coroutineScope, drawerState) {
                        {
                            coroutineScope.launch { drawerState.close() }
                            Unit
                        }
                    }
                    val onForceSyncAction = remember(viewModel) {
                        { viewModel.forceSyncChats() }
                    }
                    val onUnlockHiddenProfileAction = remember(viewModel) {
                        { query: String -> viewModel.checkAndUnlockHiddenProfile(query) }
                    }
                    val onLockHiddenProfileAction = remember(viewModel) {
                        { viewModel.lockToMainProfile() }
                    }
                    val onUnlockSubAccountAction = remember(viewModel) {
                        { query: String -> viewModel.checkAndUnlockSubAccount(query) }
                    }
                    val onLockSubAccountAction = remember(viewModel) {
                        { viewModel.lockToMainAccount() }
                    }
                    val onCreateParallelSubAccountAction = remember(viewModel) {
                        { username: String, displayName: String, passwordSecret: String, mute: Boolean, discreet: Boolean ->
                            viewModel.createSubAccount(
                                username = username,
                                displayName = displayName,
                                secretPassword = passwordSecret,
                                muteNotifications = mute,
                                discreetNotifications = discreet
                            )
                        }
                    }
                    val onOpenCompanyAccountingAction = remember(navController) {
                        { navController.navigate(Screen.CompanyAccounting) }
                    }
                    val onOpenSubscriptionsAction = remember(navController) {
                        { navController.navigate(Screen.Subscriptions) }
                    }
                    val onOpenNearbyAction = remember(navController) {
                        { navController.navigate(Screen.Nearby) }
                    }
                    val onOpenAdminAction = remember(navController) {
                        { navController.navigate(Screen.Admin) }
                    }
                    val onOpenGoldAction = remember(navController) {
                        { navController.navigate(Screen.Gold) }
                    }
                    val onChatClickAction = remember(navController) {
                        { chatId: String ->
                            if (chatId.isNotBlank()) navController.navigate(Screen.ChatDetail(chatId)) else navController.navigate(Screen.ChatDetail("unknown"))
                        }
                    }
                    val onProjectClickAction = remember(navController) {
                        { projectId: String ->
                            navController.navigate(Screen.ProjectDetail(projectId))
                        }
                    }
                    val onTransactionClickAction = remember(navController) {
                        { txId: String ->
                            navController.navigate(Screen.TransactionDetail(txId))
                        }
                    }
                    val onOpenMerchantStoreAction = remember(navController) {
                        { mId: String?, isManage: Boolean ->
                            navController.navigate(Screen.MerchantStore(merchantId = mId, isManageMode = isManage))
                        }
                    }
                    val onTogglePinChatAction = remember(viewModel) {
                        { chatId: String, isPinned: Boolean -> viewModel.togglePinChat(chatId, isPinned) }
                    }
                    val onArchiveChatAction = remember(viewModel) {
                        { chatId: String, isArchived: Boolean -> viewModel.archiveChat(chatId, isArchived) }
                    }
                    val onDeleteChatAction = remember(viewModel) {
                        { chatId: String -> viewModel.deleteChat(chatId) }
                    }
                    val onMarkChatAsReadAction = remember(viewModel) {
                        { chatId: String -> viewModel.markChatAsRead(chatId) }
                    }
                    val onCreateGroupFolderAction = remember(viewModel) {
                        { groupName: String, selectedChatIds: List<String> -> viewModel.createGroupChatFolder(groupName, selectedChatIds) }
                    }
                    val onRemoveGroupFolderAction = remember(viewModel) {
                        { groupName: String -> viewModel.removeChatGroupFolder(groupName) }
                    }
                    val onRenameGroupFolderAction = remember(viewModel) {
                        { oldName: String, newName: String, selectedIds: List<String>? ->
                            viewModel.renameChatGroupFolder(oldName, newName, selectedIds)
                        }
                    }
                    val onDeleteProjectAction = remember(viewModel) {
                        { projectId: String -> viewModel.deleteProject(projectId) }
                    }
                    val onUpdatePricesAction = remember(viewModel) {
                        { k21: Double -> viewModel.updateGoldPrices(k21) }
                    }
                    val onSubmitTraderRequestAction = remember(viewModel) {
                        { shop: String, addr: String, lic: String -> viewModel.submitTraderRequest(shop, addr, lic) }
                    }
                    val onAddAlertAction = remember(viewModel) {
                        { metal: String, karat: String, targetPrice: Double, direction: String, note: String? ->
                            viewModel.addPriceAlert(metal, karat, targetPrice, direction, note ?: "")
                        }
                    }
                    val onDeleteAlertAction = remember(viewModel) {
                        { alertId: String -> viewModel.deletePriceAlert(alertId) }
                    }
                    val onToggleAlertAction = remember(viewModel) {
                        { alertId: String, isActive: Boolean -> viewModel.togglePriceAlert(alertId, isActive) }
                    }
                    val onTestTriggerAlertAction = remember(viewModel) {
                        { alert: PriceAlertEntity -> viewModel.testTriggerPriceAlert(alert) }
                    }
                    val onResetAlertTriggerAction = remember(viewModel) {
                        { alertId: String -> viewModel.resetPriceAlertTrigger(alertId) }
                    }
                    val onDeleteTransactionAction = remember(viewModel) {
                        { txId: String -> viewModel.deleteTransaction(txId) }
                    }
                    val onDeleteInvoiceAction = remember(viewModel) {
                        { invoiceId: String -> viewModel.deleteInvoice(invoiceId) }
                    }
                    val onThemeToggleAction = remember(viewModel) {
                        { isDark: Boolean -> viewModel.toggleTheme(isDark) }
                    }
                    val onSelectPaletteAction = remember(viewModel) {
                        { pal: AppThemePalette -> viewModel.setThemePalette(pal) }
                    }
                    val onLanguageChangeAction = remember(viewModel) {
                        { lang: AppLanguage -> viewModel.setLanguage(lang) }
                    }
                    val onUpdateProfileImagesAction = remember(viewModel) {
                        { avatar: String?, cover: String? -> viewModel.updateProfileImages(avatar, cover) }
                    }
                    val onSubmitVerificationFormAction = remember(viewModel) {
                        { natId: String, commReg: String, shopAddr: String, notes: String ->
                            viewModel.submitVerificationForm(natId, commReg, shopAddr, notes)
                        }
                    }
                    val onSubmitSubscriptionRequestAction = remember(viewModel) {
                        { planId: String, planTitle: String, pricePaid: Double ->
                            viewModel.submitSubscriptionRequest(planId, planTitle, pricePaid)
                        }
                    }
                    val onUpdateAppLockAction = remember(viewModel) {
                        { enabled: Boolean, pin: String, bioEnabled: Boolean ->
                            viewModel.updateAppLock(enabled, pin, bioEnabled)
                        }
                    }
                    val onCreateBackupAction = remember(viewModel) {
                        { passphrase: String, isGoogleDrive: Boolean ->
                            viewModel.createEncryptedBackup(passphrase, isGoogleDrive)
                        }
                    }
                    val onCreateSubAccountsBackupAction = remember(viewModel) {
                        { passphrase: String, profileIds: List<String>?, provider: CloudStorageProvider ->
                            viewModel.createSubAccountsMigrationBackup(passphrase, profileIds, provider)
                        }
                    }
                    val onRestoreBackupAction = remember(viewModel) {
                        { fileContent: String, passphrase: String ->
                            viewModel.restoreEncryptedBackup(fileContent, passphrase)
                        }
                    }
                    val onDeleteCloudBackupAction = remember(viewModel) {
                        { backupId: String -> viewModel.deleteCloudBackup(backupId) }
                    }
                    val onRefreshCloudBackupsAction = remember(viewModel) {
                        { viewModel.refreshCloudBackups() }
                    }
                    val onClearBackupStatusAction = remember(viewModel) {
                        { viewModel.clearBackupStatus() }
                    }
                    val onDeleteAccountAction = remember(viewModel) {
                        { viewModel.deleteCurrentUserAccount() }
                    }
                    val onCreateSubAccountAction = remember(viewModel) {
                        { sName: String, sPin: String, sComp: String, sRole: String, sCap: Double, sSilent: Boolean ->
                            viewModel.createHiddenProfile(sName, sPin, sComp, sRole, sCap, sSilent)
                        }
                    }
                    val onAddDailyRecordAction = remember(viewModel) {
                        { record: DailyAccountingRecordEntity -> viewModel.addDailyAccountingRecord(record) }
                    }
                    val onDeleteDailyRecordAction = remember(viewModel) {
                        { recordId: String -> viewModel.deleteDailyAccountingRecord(recordId) }
                    }
                    val onSwitchProfileAction = remember(viewModel) {
                        { prof: HiddenProfileEntity -> viewModel.switchActiveProfile(prof) }
                    }
                    val onLogoutAction = remember(viewModel, navController) {
                        {
                            viewModel.logout()
                            navController.navigate(Screen.Onboarding) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                    val onUpdateProfileInfoAction = remember(viewModel) {
                        { name: String, username: String, type: UserType, jobTitle: String?, bio: String?, edu: String?, order: String?, storeName: String?, skills: String? ->
                            viewModel.updateUserProfileInfo(name, username, type, jobTitle, bio, edu, order, storeName, skills)
                        }
                    }
                    val onSendBroadcastAction = remember(viewModel) {
                        { targetCategory: BroadcastTargetCategory, title: String, content: String, isUrgent: Boolean, postAsAnnouncement: Boolean, onComplete: (Int) -> Unit ->
                            viewModel.sendBroadcastMessage(targetCategory, title, content, isUrgent, postAsAnnouncement, onComplete)
                        }
                    }

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        gesturesEnabled = drawerState.isOpen,
                        drawerContent = {
                            GlobalSideDrawerContent(
                                currentUser = currentUser,
                                currentLanguage = currentLanguage,
                                onLanguageChange = onLanguageChangeAction,
                                onCloseDrawer = onCloseDrawerAction,
                                activeSection = when (BottomTab.entries.getOrNull(pagerState.currentPage)) {
                                    BottomTab.PROJECTS -> DrawerSection.PROJECTS
                                    BottomTab.BUSINESS -> DrawerSection.COMPANY_ACCOUNTING
                                    else -> DrawerSection.HOME
                                },
                                onOpenHome = { onTabSelected(BottomTab.CHATS.ordinal) },
                                onOpenAIAccountant = {
                                    activeChatForSmartAccountant = null
                                    showFinancialAccountantDialog = true
                                },
                                onOpenVoiceBusinessAgent = {
                                    activeChatForSmartAccountant = null
                                    showVoiceBusinessAgentDialog = true
                                },
                                onOpenCompanyAccounting = onOpenCompanyAccountingAction,
                                onOpenInvoiceVerification = { showInvoiceVerificationModal = true },
                                onOpenSubscriptions = onOpenSubscriptionsAction,
                                onOpenNearby = onOpenNearbyAction,
                                onOpenGold = onOpenGoldAction,
                                onOpenGoldDeal = onOpenGoldAction,
                                onOpenCommandTerminal = { onTabSelected(BottomTab.CHATS.ordinal) },
                                onOpenBackupsVault = {
                                    showEncryptedVaultSheet = true
                                },
                                onOpenBiometricSettings = { onTabSelected(BottomTab.PROFILE.ordinal) },
                                onOpenAdmin = onOpenAdminAction,
                                onForceSync = onForceSyncAction,
                                onLogout = onLogoutAction,
                                goldPriceKarat21 = goldPrices.karat21,
                                activeChatsCount = currentDisplayChats.count { !it.isArchived }
                            )
                        }
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            val isDesktop = maxWidth >= 700.dp

                        Row(modifier = Modifier.fillMaxSize()) {
                            // Desktop Hidden/Collapsible Luxury Sidebar (Hidden in Focus Mode)
                            if (isDesktop && !isFocusMode) {
                                AnimatedVisibility(
                                    visible = isDesktopSidebarOpen,
                                    enter = expandHorizontally(animationSpec = tween(250)) + fadeIn(animationSpec = tween(200)),
                                    exit = shrinkHorizontally(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .width(260.dp)
                                            .fillMaxHeight(),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                                        ),
                                        tonalElevation = 4.dp
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .windowInsetsPadding(WindowInsets.statusBars)
                                                .windowInsetsPadding(WindowInsets.navigationBars)
                                                .padding(horizontal = 16.dp, vertical = 20.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                // Luxury Header
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(MaterialTheme.colorScheme.primary),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "O",
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 18.sp,
                                                                color = MaterialTheme.colorScheme.onPrimary
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text(
                                                                text = "OOZOX",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 16.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = tr("nav.luxury_edition"),
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = { isDesktopSidebarOpen = false },
                                                        modifier = Modifier.minimumInteractiveComponentSize()
                                                    ) {
                                                        Icon(
                                                            imageVector = AppIcons.MenuClose,
                                                            contentDescription = tr("nav.collapse_sidebar"),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(28.dp))

                                                // Navigation Items
                                                BottomTab.entries.forEachIndexed { index, tab ->
                                                    val isSelected = pagerState.currentPage == index
                                                    val icon = when (tab) {
                                                        BottomTab.CHATS -> AppIcons.Chats
                                                        BottomTab.PROJECTS -> AppIcons.Projects
                                                        BottomTab.BUSINESS -> AppIcons.Business
                                                        BottomTab.TRANSACTIONS -> AppIcons.Transactions
                                                        BottomTab.PROFILE -> AppIcons.Profile
                                                    }

                                                    Surface(
                                                        onClick = { onTabSelected(index) },
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
                                                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(
                                                                    imageVector = icon,
                                                                    contentDescription = Translations.get(tab.labelKey, currentLanguage),
                                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(12.dp))
                                                                Text(
                                                                    text = Translations.get(tab.labelKey, currentLanguage),
                                                                    fontSize = 13.sp,
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                                )
                                                            }

                                                            if (tab == BottomTab.CHATS && totalUnreadChats > 0) {
                                                                val sidebarBadgeSize = if (totalUnreadChats > 99) 24.dp else 20.dp
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(sidebarBadgeSize)
                                                                        .clip(CircleShape)
                                                                        .background(MaterialTheme.colorScheme.primary),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = if (totalUnreadChats > 99) "99+" else totalUnreadChats.toString(),
                                                                        fontSize = if (totalUnreadChats > 99) 8.sp else 10.sp,
                                                                        lineHeight = 10.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                                        textAlign = TextAlign.Center,
                                                                        maxLines = 1,
                                                                        style = LocalTextStyle.current.copy(
                                                                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Bottom User Info in Sidebar
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = currentUser?.name ?: tr("lock.default_user"),
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1
                                                        )
                                                        Text(
                                                            text = if (currentUser?.type == UserType.GOLD_TRADER.name) tr("drawer.certified_merchant") else tr("drawer.investor_label"),
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Main Pager View (Gesture-Based Swipeable Canvas) + Luxury Bottom Bar
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize(),
                                        key = { BottomTab.entries[it].name },
                                        userScrollEnabled = true
                                    ) { page ->
                                    when (BottomTab.entries[page]) {
                                        BottomTab.CHATS -> ChatListScreen(
                                            chats = currentDisplayChats,
                                            currentUser = currentUser,
                                            adminConfig = adminConfig,
                                            goldPrices = goldPrices,
                                            isRefreshing = isRefreshingChats,
                                            onRefresh = onForceSyncAction,
                                            lastSyncTime = lastSyncTimestamp,
                                            activeHiddenProfile = activeHiddenProfile,
                                            onUnlockHiddenProfile = onUnlockHiddenProfileAction,
                                            onLockHiddenProfile = onLockHiddenProfileAction,
                                            activeSubAccount = activeSubAccount,
                                            onUnlockSubAccount = onUnlockSubAccountAction,
                                            onLockSubAccount = onLockSubAccountAction,
                                            onCreateParallelSubAccount = onCreateParallelSubAccountAction,
                                            onOpenCompanyAccounting = onOpenCompanyAccountingAction,
                                            onOpenSubscriptions = onOpenSubscriptionsAction,
                                            onOpenDrawer = onOpenDrawerAction,
                                            hashtagEntries = allHashtagEntries,
                                            ledgerEntries = allLedgerEntries,
                                            settlements = allSettlements,
                                            onChatClick = onChatClickAction,
                                            onNewChatClick = { showNewChatDialog = true },
                                            onCreateGroupFolder = onCreateGroupFolderAction,
                                            onRemoveGroupFolder = onRemoveGroupFolderAction,
                                            onRenameGroupFolder = onRenameGroupFolderAction,
                                            onOpenAIAccountant = {
                                                activeChatForSmartAccountant = null
                                                showFinancialAccountantDialog = true
                                            },
                                            onOpenGoldDealClick = onOpenGoldAction,
                                            onOpenNewProjectClick = { showNewProjectDialog = true },
                                            onTogglePinChat = onTogglePinChatAction,
                                            onArchiveChat = onArchiveChatAction,
                                            onDeleteChat = onDeleteChatAction,
                                            onMarkChatAsRead = onMarkChatAsReadAction,
                                            workGroups = allWorkGroups,
                                            projects = allProjects,
                                            onAssignToProjectWorkGroup = { chat, wg, proj, roleLabel, projRole, canPost, canEdit, canExport, canMsg, isAdmin ->
                                                viewModel.assignChatContactToProjectWorkGroup(
                                                    chat = chat,
                                                    workGroup = wg,
                                                    project = proj,
                                                    roleLabel = roleLabel,
                                                    projectRole = projRole,
                                                    canPostAccounting = canPost,
                                                    canEditRecords = canEdit,
                                                    canExportReports = canExport,
                                                    canMessageDirectly = canMsg,
                                                    isGroupAdmin = isAdmin
                                                )
                                            },
                                            contacts = allContacts,
                                            onCreateStandaloneWorkGroup = { groupName, description, selectedContacts, disableMemberDMs, onCreated ->
                                                viewModel.createStandaloneWorkGroup(
                                                    groupName = groupName,
                                                    description = description,
                                                    selectedContacts = selectedContacts,
                                                    disableMemberDMs = disableMemberDMs,
                                                    onCreated = { newChatId ->
                                                        onCreated(newChatId)
                                                        navController.navigate(Screen.ChatDetail(newChatId))
                                                    }
                                                )
                                            }
                                        )

                                        BottomTab.PROJECTS -> ProjectsScreen(
                                            workGroups = allWorkGroups,
                                            projects = allProjects,
                                            isRefreshing = isRefreshingChats,
                                            onRefresh = onForceSyncAction,
                                            onProjectClick = onProjectClickAction,
                                            onOpenProjectChat = { proj ->
                                                viewModel.ensureWorkGroupChatAndOpen(proj.workGroupId) { chatId ->
                                                    navController.navigate(Screen.ChatDetail(chatId))
                                                }
                                            },
                                            onCreateProjectClick = { showNewProjectDialog = true },
                                            onDeleteProject = onDeleteProjectAction
                                        )

                                        BottomTab.BUSINESS -> BusinessManagementScreen(
                                            currentUser = currentUser,
                                            allInvoices = allInvoices,
                                            merchantProducts = allMerchantProducts,
                                            activeSubAccount = activeSubAccount,
                                            allSubAccounts = allSubAccounts,
                                            allShiftHandovers = allShiftHandovers,
                                            allComplaints = allFinancialComplaints,
                                            allProjects = allProjects,
                                            allProjectTasks = allProjectTasks,
                                            allHashtagAccounts = allHashtagAccounts,
                                            allHashtagEntries = allHashtagEntries,
                                            allDailyRecords = allDailyAccountingRecords,
                                            businessProfile = businessProfile,
                                            allPayrollAdjustments = allPayrollAdjustments,
                                            allEmployeeLeaves = allEmployeeLeaves,
                                            allAttendanceRecords = allAttendanceRecords,
                                            allWorkGroups = allWorkGroups,
                                            allGroupMembers = allGroupMembers,
                                            onLoginSubAccount = { user, pin -> viewModel.loginEmployee(user, pin) },
                                            onLoginEmployeeWithMandatoryGps = { user, pass ->
                                                viewModel.loginEmployeeWithMandatoryGps(user, pass)
                                            },
                                            onLogoutSubAccount = { viewModel.lockToMainAccount() },
                                            onAddShiftHandover = { handover -> viewModel.addShiftHandover(handover) },
                                            onReceiveShift = { id, rId, rName, notes -> viewModel.receiveShiftHandover(id, rId, rName, notes) },
                                            onReceiveShiftFull = { id, rId, rName, notes, merchStatus, stockChecked, cashConfirmed, invoicesCount, signature, acceptType, discNotes ->
                                                viewModel.receiveShiftHandoverFull(id, rId, rName, notes, merchStatus, stockChecked, cashConfirmed, invoicesCount, signature, acceptType, discNotes)
                                            },
                                            onRespondToComplaint = { id, accepted, reason, uri -> viewModel.respondToComplaint(id, accepted, reason, uri) },
                                            onDecideOnComplaint = { id, decision, notes -> viewModel.decideOnComplaintByManager(id, decision, notes) },
                                            onApplyJointDeduction = { id, pAmt, rAmt, rId, notes -> viewModel.applyJointComplaintDeduction(id, pAmt, rAmt, rId, notes) },
                                            onUpdateProfession = { prof -> viewModel.updateUserProfession(prof) },
                                            onCreateInvoice = { inv -> viewModel.addInvoice(inv) },
                                            onAddMerchantProduct = { prod -> viewModel.addMerchantProduct(prod) },
                                            onAddSubAccount = { name, phone, pin ->
                                                viewModel.createSubAccount(
                                                    username = phone,
                                                    displayName = name,
                                                    secretPassword = pin,
                                                    phone = phone,
                                                    muteNotifications = true,
                                                    discreetNotifications = false
                                                )
                                            },
                                            onSaveBusinessProfile = { profile -> viewModel.saveBusinessProfile(profile) },
                                            onCreateOrUpdateEmployee = { id, name, user, phone, pass, role, dept, sal, cur, geo, isActive, inv, rep, disc, mBiz, mStaff, mPay, mProj, aTask, mAcct, aDisc, dChat ->
                                                viewModel.createOrUpdateEnterpriseEmployee(
                                                    existingId = id,
                                                    fullName = name,
                                                    username = user,
                                                    phone = phone,
                                                    password = pass,
                                                    roleTitle = role,
                                                    department = dept,
                                                    baseSalary = sal,
                                                    salaryCurrency = cur,
                                                    geofenceEnforced = geo,
                                                    isActiveEmployee = isActive,
                                                    canCreateInvoices = inv,
                                                    canViewReports = rep,
                                                    canSetProductPrices = disc,
                                                    permManageBusiness = mBiz,
                                                    permManageStaff = mStaff,
                                                    permManagePayroll = mPay,
                                                    permManageProjects = mProj,
                                                    permAssignTasks = aTask,
                                                    permManageAccounting = mAcct,
                                                    permApproveDiscounts = aDisc,
                                                    permDirectChat = dChat
                                                )
                                            },
                                            onDeleteEmployee = { empId -> viewModel.deleteSubAccount(empId) },
                                            onAddPayrollAdjustment = { emp, type, amt, reason, date ->
                                                viewModel.addEmployeePayrollAdjustment(emp, type, amt, reason, date)
                                            },
                                            onDeletePayrollAdjustment = { adjId ->
                                                viewModel.deletePayrollAdjustment(adjId)
                                            },
                                            onAddEmployeeLeave = { emp, type, start, end, notes ->
                                                viewModel.addEmployeeLeaveRecord(emp, type, start, end, notes)
                                            },
                                            onDeleteEmployeeLeave = { leaveId ->
                                                viewModel.deleteEmployeeLeave(leaveId)
                                            },
                                            onClockInMandatoryGps = { emp -> viewModel.clockInEmployeeWithMandatoryGps(emp) },
                                            onOpenCompanyGroupChat = {
                                                viewModel.openCompanyStaffGroupChat { chatId ->
                                                    navController.navigate(Screen.ChatDetail(chatId))
                                                }
                                            },
                                            onAssignTaskToCompanyGroup = { task ->
                                                viewModel.insertProjectTask(task)
                                            },
                                            onUpdateTaskStatus = { taskId, status ->
                                                viewModel.updateProjectTaskStatus(taskId, status)
                                            },
                                            onOpenProject = onProjectClickAction,
                                            onOpenSubscriptions = onOpenSubscriptionsAction,
                                            onOpenCompanyAccounting = onOpenCompanyAccountingAction,
                                            onOpenDrawer = onOpenDrawerAction
                                        )

                                        BottomTab.TRANSACTIONS -> TransactionsScreen(
                                            transactions = allTransactions,
                                            goldPurchases = allGoldPurchases,
                                            settlements = allSettlements,
                                            hashtagEntries = allHashtagEntries,
                                            ledgerEntries = allLedgerEntries,
                                            invoices = allInvoices,
                                            projects = allProjects,
                                            dailyRecords = allDailyAccountingRecords,
                                            adminConfig = adminConfig,
                                            hashtagAccounts = allHashtagAccounts,
                                            globalSnapshot = globalAccountingSnapshot,
                                            onPayInvoice = { invId, amt, method, notes ->
                                                viewModel.payInvoiceWithSync(invId, amt, method, notes)
                                            },
                                            onTransactionClick = onTransactionClickAction,
                                            onOpenProjectClick = onProjectClickAction,
                                            onDeleteTransaction = onDeleteTransactionAction,
                                            onDeleteInvoice = onDeleteInvoiceAction
                                        )

                                        BottomTab.PROFILE -> ProfileScreen(
                                            currentUser = currentUser,
                                            adminConfig = adminConfig,
                                            workGroups = allWorkGroups,
                                            projects = allProjects,
                                            goldPurchases = allGoldPurchases,
                                            transactions = allTransactions,
                                            hiddenProfiles = allHiddenProfiles,
                                            cloudBackups = cloudBackups,
                                            dailyRecords = allDailyAccountingRecords,
                                            subscriptionPlans = subscriptionPlans,
                                            activeAnnouncements = activePromoAnnouncements,
                                            attendanceRecords = allAttendanceRecords,
                                            isDarkTheme = isDarkTheme,
                                            selectedPalette = selectedThemePalette,
                                            currentLanguage = currentLanguage,
                                            onThemeToggle = onThemeToggleAction,
                                            onSelectPalette = onSelectPaletteAction,
                                            onLanguageChange = onLanguageChangeAction,
                                            onUpdateProfileImages = onUpdateProfileImagesAction,
                                            onSubmitVerificationForm = onSubmitVerificationFormAction,
                                            onSubmitSubscriptionRequest = onSubmitSubscriptionRequestAction,
                                            onOpenNearbyClick = onOpenNearbyAction,
                                            onOpenSubscriptionsClick = onOpenSubscriptionsAction,
                                            onOpenAdminClick = onOpenAdminAction,
                                            onOpenCompanyAccountingClick = onOpenCompanyAccountingAction,
                                            onOpenMerchantStore = onOpenMerchantStoreAction,
                                            onUpdateAppLock = onUpdateAppLockAction,
                                            onCreateBackup = onCreateBackupAction,
                                            onCreateSubAccountsBackup = onCreateSubAccountsBackupAction,
                                            onRestoreBackup = onRestoreBackupAction,
                                            onDeleteCloudBackup = onDeleteCloudBackupAction,
                                            onRefreshCloudBackups = onRefreshCloudBackupsAction,
                                            backupStatus = backupStatus,
                                            isBackupProcessing = isBackupProcessing,
                                            onClearBackupStatus = onClearBackupStatusAction,
                                            onDeleteAccount = onDeleteAccountAction,
                                            onCreateSubAccount = onCreateSubAccountAction,
                                            onCreateParallelSubAccount = onCreateParallelSubAccountAction,
                                            onAddDailyRecord = onAddDailyRecordAction,
                                            onDeleteDailyRecord = onDeleteDailyRecordAction,
                                            onSwitchProfile = onSwitchProfileAction,
                                            onLogout = onLogoutAction,
                                            onUpdateProfileInfo = onUpdateProfileInfoAction,
                                            onSendBroadcast = onSendBroadcastAction
                                        )
                                    }
                                }

                                // Edge-only Side Drawer Swipe Zone (28dp outer edge) — leaves center body 100% free for HorizontalPager section swipes
                                if (!isDesktop && !isFocusMode && !drawerState.isOpen) {
                                    val isRtlLayout = LocalLayoutDirection.current == LayoutDirection.Rtl
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .width(28.dp)
                                            .fillMaxHeight()
                                            .pointerInput(isRtlLayout) {
                                                var totalEdgeDrag = 0f
                                                var opened = false
                                                detectHorizontalDragGestures(
                                                    onDragStart = {
                                                        totalEdgeDrag = 0f
                                                        opened = false
                                                    },
                                                    onHorizontalDrag = { _, dragAmount ->
                                                        if (!opened) {
                                                            totalEdgeDrag += dragAmount
                                                            val inwardDelta = if (isRtlLayout) -totalEdgeDrag else totalEdgeDrag
                                                            if (inwardDelta > 22f) {
                                                                opened = true
                                                                coroutineScope.launch { drawerState.open() }
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                            .testTag("drawer_edge_swipe_zone")
                                    )
                                }

                                // Desktop Toggle Button when sidebar is hidden (Only on wide screens)
                                if (isDesktop && !isDesktopSidebarOpen) {
                                    Surface(
                                        onClick = { isDesktopSidebarOpen = true },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .align(Alignment.TopStart)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = tr("drawer.open_sidebar"),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(8.dp).size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Luxury Bottom Section Bar for Mobile Screens
                            if (!isDesktop || isFocusMode) {
                                LuxuryBottomSectionBar(
                                    pagerState = pagerState,
                                    totalUnreadChats = totalUnreadChats,
                                    currentLanguage = currentLanguage,
                                    onTabSelected = onTabSelected
                                )
                            }
                        }
                    }
                }
            }
        }

                composable<Screen.ChatDetail> { backStackEntry ->
                    
                    val route: Screen.ChatDetail = backStackEntry.toRoute()
                    val chat = allChats.find { it.id == route.chatId }
                        ?: allContacts.find { it.id == route.chatId || it.phone == route.chatId }?.let { contact ->
                            ChatEntity(
                                id = route.chatId,
                                contactId = contact.id,
                                contactName = contact.name,
                                contactType = contact.type,
                                contactVerified = contact.isVerified
                            )
                        }
                        ?: allNearbyUsers.find { it.id == route.chatId }?.let { nearby ->
                            ChatEntity(
                                id = route.chatId,
                                contactId = nearby.id,
                                contactName = nearby.name,
                                contactType = nearby.userType,
                                contactVerified = nearby.isVerified
                            )
                        }
                        ?: if (route.chatId.contains("ai", ignoreCase = true) || route.chatId.contains("bot", ignoreCase = true)) {
                            ChatEntity(
                                id = route.chatId,
                                contactId = "ai_bot",
                                contactName = tr("drawer.ai_title"),
                                contactType = UserType.GOLD_TRADER.name,
                                contactVerified = true
                            )
                        } else {
                            ChatEntity(
                                id = route.chatId,
                                contactId = route.chatId,
                                contactName = tr("drawer.default_chat_name"),
                                contactType = "",
                                contactVerified = false
                            )
                        }

                    val chatHistoryState by remember(route.chatId) {
                        viewModel.observePagedChatHistory(route.chatId)
                    }.collectAsStateWithLifecycle(
                        initialValue = com.example.ui.viewmodel.ChatHistoryUiState(isInitialLoading = false)
                    )
                    val messages = chatHistoryState.messages
                    val accounts by remember(route.chatId) { viewModel.getAccountsForChat(route.chatId) }.collectAsStateWithLifecycle(initialValue = emptyList())
                    val hashtags by remember(route.chatId) { viewModel.getHashtagsForChat(route.chatId) }.collectAsStateWithLifecycle(initialValue = emptyList())
                    val allHashtagEntries by viewModel.allHashtagEntries.collectAsStateWithLifecycle()
                    val translatedMessages by viewModel.translatedMessages.collectAsStateWithLifecycle()
                    val translationFontSize by viewModel.translationFontSize.collectAsStateWithLifecycle()
                    val translationColorStyle by viewModel.translationColorStyle.collectAsStateWithLifecycle()

                    LaunchedEffect(route.chatId) {
                        viewModel.markChatAsRead(route.chatId)
                    }

                    var showGoldDealDialog by remember { mutableStateOf(false) }

                    ChatDetailScreen(
                        chat = chat,
                        messages = messages,
                        isInitialLoading = chatHistoryState.isInitialLoading,
                        isLoadingOlderMessages = chatHistoryState.isLoadingOlder,
                        hasMoreOlderMessages = chatHistoryState.hasMoreOlder,
                        totalMessagesCount = chatHistoryState.totalCount,
                        onLoadOlderMessages = { viewModel.loadOlderMessagesForChat(route.chatId) },
                        onExportFullChatPdf = { viewModel.exportFullChatToPdf(context, chat) },
                        accounts = accounts,
                        hashtags = hashtags,
                        allEntries = allHashtagEntries,
                        allTransactions = allTransactions,
                        goldPrices = goldPrices,
                        currentUser = currentUser,
                        contactUser = allUsers.find { it.id == chat?.contactId },
                        availableChats = allChats,
                        onNavigateToProfile = { contactId ->
                            navController.navigate(Screen.MerchantStore(merchantId = contactId, isManageMode = false))
                        },
                        translatedMessages = translatedMessages,
                        translationFontSize = translationFontSize,
                        translationColorStyle = translationColorStyle,
                        onUpdateTranslationFontSize = { viewModel.setTranslationFontSize(it) },
                        onUpdateTranslationColorStyle = { viewModel.setTranslationColorStyle(it) },
                        allProjects = allProjects,
                        allInvoices = allInvoices,
                        invoicesForChat = remember(allInvoices, route.chatId, chat.contactId) {
                            allInvoices.filter {
                                it.chatId == route.chatId ||
                                    (chat.contactId.isNotBlank() && (it.receiverId == chat.contactId || it.senderId == chat.contactId))
                            }
                        },
                        onPayInvoice = { invId, amt, method, notes ->
                            viewModel.payInvoiceWithSync(invId, amt, method, notes)
                        },
                        onRecordHashtagOperation = { tag, entryType, desc, amt, dir, pId, pName, method, isExt ->
                            viewModel.recordChatHashtagOperation(
                                chatId = route.chatId,
                                tag = tag,
                                entryType = entryType,
                                description = desc,
                                amount = amt,
                                direction = dir,
                                projectId = pId,
                                projectName = pName,
                                paymentMethod = method,
                                isExternalBusiness = isExt
                            )
                        },
                        onTranslateMessage = { msg ->
                            viewModel.translateMessageContextually(msg, messages)
                        },
                        onDismissTranslation = { msgId ->
                            viewModel.dismissTranslation(msgId)
                        },
                        onBackClick = {
                            if (activeSubAccount != null) {
                                viewModel.lockToMainAccount()
                            }
                            navController.popBackStack()
                        },
                        onSendMessage = { text, type ->
                            viewModel.sendMessage(route.chatId, text, type)
                        },
                        onForwardMessage = { targetChatId, text, type ->
                            viewModel.sendMessage(targetChatId, text, type)
                        },
                        onToggleReaction = { messageId, currentReaction, emoji ->
                            viewModel.toggleMessageReaction(messageId, currentReaction, emoji)
                        },
                        onCreateAccount = { accName ->
                            viewModel.createAccount(route.chatId, accName)
                        },
                        onCreateHashtagAccount = { chatId, accountId, tag ->
                            viewModel.createHashtagAccount(chatId, accountId, tag)
                        },
                        onAddEntry = { hashtagId, accountId, type, desc, amt, dir ->
                            viewModel.addHashtagEntry(hashtagId, accountId, type, desc, amt, dir)
                        },
                        onOpenGoldDeal = { showGoldDealDialog = true },
                        onOpenAIAccountant = {
                            activeChatForSmartAccountant = chat
                            showFinancialAccountantDialog = true
                        },
                        onOpenVoiceBusinessAgent = {
                            activeChatForSmartAccountant = chat
                            showVoiceBusinessAgentDialog = true
                        },
                        onExecuteClearing = { netGold, netSilver, netCash, loss, notes ->
                            if (chat != null) {
                                viewModel.executeClearingSettlement(
                                    contactId = chat.contactId,
                                    contactName = chat.contactName,
                                    netGold24k = netGold,
                                    netSilver999 = netSilver,
                                    netCash = netCash,
                                    metalLoss = loss,
                                    notes = notes
                                )
                            }
                        },
                        workGroups = allWorkGroups,
                        allGroupMembers = allGroupMembers,
                        allProjectMembers = allProjectMembers,
                        allEmployees = allSubAccounts,
                        allContacts = allContacts,
                        projectTasks = allProjectTasks,
                        onInsertFieldTask = { task ->
                            viewModel.insertProjectTask(task, targetChatId = route.chatId)
                        },
                        onSubmitTaskProof = { taskId, uri, type, notes, uid, uname ->
                            viewModel.submitFieldTaskWorkerResponse(taskId, uri, type, notes, uid, uname)
                        },
                        onConfirmAndArchiveTask = { taskId, amount, notes ->
                            viewModel.confirmAndArchiveFieldTask(taskId, amount, notes)
                        },
                        onUpdateProjectTaskStatus = { taskId, status ->
                            viewModel.updateProjectTaskStatus(taskId, status)
                        },
                        onOpenGroupChatById = { targetGroupId ->
                            val existingGroupChat = allChats.firstOrNull {
                                it.groupId == targetGroupId || it.id == "wg_chat_$targetGroupId" || it.id == "chat_wg_$targetGroupId"
                            }
                            if (existingGroupChat != null) {
                                navController.navigate(Screen.ChatDetail(existingGroupChat.id))
                            } else {
                                viewModel.ensureWorkGroupChatAndOpen(targetGroupId) { resolvedChatId ->
                                    navController.navigate(Screen.ChatDetail(resolvedChatId))
                                }
                            }
                        },
                        onAddGroupMembers = { chosenContacts, manualName, manualPhone, roleLabel, isAdmin ->
                            val resolvedGroupId = chat.groupId ?: allWorkGroups.firstOrNull {
                                it.id == chat.contactId || chat.id == "wg_chat_${it.id}" || chat.id == "chat_wg_${it.id}"
                            }?.id
                            viewModel.addMembersToGroupChat(
                                chatId = route.chatId,
                                groupId = resolvedGroupId,
                                contacts = chosenContacts,
                                manualName = manualName,
                                manualPhone = manualPhone,
                                roleLabel = roleLabel,
                                isGroupAdmin = isAdmin
                            )
                        },
                        onRemoveGroupMember = { member ->
                            viewModel.removeGroupMemberFromChat(route.chatId, member)
                        },
                        onToggleGroupMemberAdmin = { member ->
                            viewModel.toggleGroupMemberAdmin(member)
                        },
                        onOpenProjectDetail = { projId ->
                            navController.navigate(Screen.ProjectDetail(projId))
                        },
                        onAddProjectAccountingEntry = { pId, pName, title, amount, isIncome, category ->
                            viewModel.addProjectAccountingEntry(pId, pName, title, amount, isIncome, category)
                        },
                        onAddProjectMemberWithPerms = { pId, name, phone, role, task, uId, isAdmin, canPost, canEdit, canExport, canDirect ->
                            viewModel.addProjectMember(pId, name, phone, role, task, uId, isAdmin, canPost, canEdit, canExport, canDirect)
                        },
                        onPromoteProjectMember = { pId, member ->
                            viewModel.promoteProjectMemberToManager(pId, member)
                        },
                        onRemoveProjectMember = { pId, member ->
                            viewModel.removeProjectMember(pId, member)
                        }
                    )

                    if (showGoldDealDialog && chat != null) {
                        GoldDealDialog(
                            contactName = chat.contactName,
                            onAddTransaction = { karat, weight, price, mfg, notes, lossRate, lossGrams ->
                                viewModel.addGoldTransaction(
                                    chatId = chat.id,
                                    contactId = chat.contactId,
                                    contactName = chat.contactName,
                                    karat = karat,
                                    weight = weight,
                                    pricePerGram = price,
                                    mfgCostPerGram = mfg,
                                    notes = notes,
                                    metalLossRatePercent = lossRate,
                                    metalLossGrams = lossGrams
                                )
                                showGoldDealDialog = false
                            },
                            onDismiss = { showGoldDealDialog = false }
                        )
                    }
                }

                composable<Screen.ProjectDetail> { backStackEntry ->
                    val route: Screen.ProjectDetail = backStackEntry.toRoute()
                    val project = allProjects.find { it.id == route.projectId }
                    val workGroup = allWorkGroups.find { it.id == project?.workGroupId }
                    val members by remember(project?.workGroupId) { viewModel.getGroupMembers(project?.workGroupId ?: "") }.collectAsStateWithLifecycle(initialValue = emptyList())
                    val projectMembers by remember(route.projectId) { viewModel.getProjectMembers(route.projectId) }.collectAsStateWithLifecycle(initialValue = emptyList())
                    val projectAuditLogs by remember(route.projectId) { viewModel.getProjectAuditLogs(route.projectId) }.collectAsStateWithLifecycle(initialValue = emptyList())
                    val projectTasks by remember(route.projectId) { viewModel.getTasksForProject(route.projectId) }.collectAsStateWithLifecycle(initialValue = emptyList())

                    ProjectDetailScreen(
                        project = project,
                        workGroup = workGroup,
                        members = members,
                        projectMembers = projectMembers,
                        projectAuditLogs = projectAuditLogs,
                        projectTasks = projectTasks,
                        shiftHandovers = allShiftHandovers,
                        employees = allSubAccounts,
                        onAddProjectMember = { name, phone, role, task ->
                            viewModel.addProjectMember(route.projectId, name, phone, role, task)
                        },
                        onAddProjectMemberWithPerms = { name, phone, role, task, uId, isAdmin, canPost, canEdit, canExport, canDirect ->
                            viewModel.addProjectMember(route.projectId, name, phone, role, task, uId, isAdmin, canPost, canEdit, canExport, canDirect)
                        },
                        onPromoteToManager = { member ->
                            viewModel.promoteProjectMemberToManager(route.projectId, member)
                        },
                        onRemoveProjectMember = { member ->
                            viewModel.removeProjectMember(route.projectId, member)
                        },
                        onOpenGroupChat = {
                            project?.let { p ->
                                viewModel.ensureWorkGroupChatAndOpen(p.workGroupId) { chatId ->
                                    navController.navigate(Screen.ChatDetail(chatId))
                                }
                            }
                        },
                        onAddAccountingEntry = { title, amount, isIncome, category ->
                            viewModel.addProjectAccountingEntry(
                                projectId = route.projectId,
                                projectName = project?.name ?: Translations.get("drawer.project_default_name", currentLanguage),
                                title = title,
                                amount = amount,
                                isIncome = isIncome,
                                accountCategory = category
                            )
                        },
                        onAddShiftHandover = { handover ->
                            viewModel.addShiftHandover(handover)
                        },
                        onReceiveShiftHandoverFull = { id, rId, rName, notes, merchStatus, stockChecked, cashConfirmed, invoicesCount, signature, acceptType, discNotes ->
                            viewModel.receiveShiftHandoverFull(
                                id, rId, rName, notes, merchStatus, stockChecked, cashConfirmed, invoicesCount, signature, acceptType, discNotes
                            )
                        },
                        onAddTask = { title, desc, assignee, priority, dueDate, cost ->
                            viewModel.addProjectTask(
                                projectId = route.projectId,
                                projectName = project?.name ?: Translations.get("drawer.project_default_name", currentLanguage),
                                title = title,
                                description = desc,
                                assignedToName = assignee,
                                priority = priority,
                                dueDate = dueDate,
                                costEstimate = cost
                            )
                        },
                        onAddFieldTask = { task ->
                            viewModel.insertProjectTask(task)
                        },
                        onSubmitTaskProof = { taskId, proofUri, proofType, notes, uid, uname ->
                            viewModel.submitFieldTaskWorkerResponse(taskId, proofUri, proofType, notes, uid, uname)
                        },
                        onConfirmAndArchiveTask = { taskId, amount, notes ->
                            viewModel.confirmAndArchiveFieldTask(taskId, amount, notes)
                        },
                        onUpdateTaskStatus = { taskId, status ->
                            viewModel.updateProjectTaskStatus(taskId, status)
                        },
                        onDeleteTask = { taskId ->
                            viewModel.deleteProjectTask(taskId)
                        },
                        onDeleteProject = { projId ->
                            viewModel.deleteProject(projId) {
                                navController.popBackStack()
                            }
                        },
                        onLoadExcelSheet = { pId, pType ->
                            viewModel.loadProjectExcelSheet(pId, pType)
                        },
                        onSaveExcelSheet = { pId, tpl, rows ->
                            viewModel.saveProjectExcelSheet(pId, tpl, rows)
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<Screen.TransactionDetail> { backStackEntry ->
                    val route: Screen.TransactionDetail = backStackEntry.toRoute()
                    val transaction = allTransactions.find { it.id == route.transactionId }

                    TransactionDetailScreen(
                        transaction = transaction,
                        onDeleteTransaction = { txId ->
                            viewModel.deleteTransaction(txId) {
                                navController.popBackStack()
                            }
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<Screen.Nearby> {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(tr("drawer.nearby_radar_title"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("drawer.back"), tint = adaptiveGoldPrimary)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.background
                    ) { paddingValues ->
                        Box(modifier = Modifier.padding(paddingValues)) {
                            NearbyUsersScreen(
                                nearbyUsers = allNearbyUsers,
                                onStartChat = { nearbyUser ->
                                    val contact = ContactEntity(
                                        id = nearbyUser.id,
                                        name = nearbyUser.name,
                                        phone = nearbyUser.phone,
                                        username = nearbyUser.username,
                                        type = nearbyUser.userType,
                                        isVerified = nearbyUser.isVerified
                                    )
                                    viewModel.startChatWithContact(contact) { newChatId ->
                                        navController.navigate(Screen.ChatDetail(newChatId))
                                    }
                                },
                                onOpenMerchantStore = { merchantId ->
                                    navController.navigate(Screen.MerchantStore(merchantId = merchantId))
                                }
                            )
                        }
                    }
                }

                composable<Screen.Admin> {
                    AdminScreen(
                        adminConfig = adminConfig,
                        traderRequests = allTraderRequests,
                        allUsers = allUsers,
                        subscriptionPlans = subscriptionPlans,
                        subscriptionRequests = subscriptionRequests,
                        promoAnnouncements = allPromoAnnouncements,
                        goldPrices = goldPrices,
                        allTransactions = allTransactions,
                        allGoldPurchases = allGoldPurchases,
                        allProjects = allProjects,
                        allInvoices = allInvoices,
                        isLoading = adminConfig == null,
                        onBackClick = { navController.popBackStack() },
                        onApproveTrader = { reqId ->
                            viewModel.updateTraderRequestStatus(reqId, TraderStatus.APPROVED, Translations.get("drawer.admin_approved_note", currentLanguage))
                        },
                        onRejectTrader = { reqId ->
                            viewModel.updateTraderRequestStatus(reqId, TraderStatus.REJECTED, Translations.get("drawer.admin_rejected_note", currentLanguage))
                        },
                        onSaveConfig = { appName, appLogoUri, adminPin, geminiKey, geminiModel, aiPrompt, notifUrl, notifKey, news, goldUrl, goldKey, intervalSec, enc, voice, prompt, bio ->
                            viewModel.updateAdminConfig(appName, appLogoUri, adminPin, geminiKey, geminiModel, aiPrompt, notifUrl, notifKey, news, goldUrl, goldKey, intervalSec, enc, voice, prompt, bio)
                        },
                        onUpdatePrices = { k21 ->
                            viewModel.updateGoldPrices(k21)
                        },
                        onUpdatePlanPriceAndOffer = { planId, monthlyPrice, discountPercent, offerTitle, isOfferActive ->
                            viewModel.updatePlanPriceAndOffer(planId, monthlyPrice, discountPercent, offerTitle, isOfferActive)
                        },
                        onUpdateSubscriptionRequestStatus = { requestId, status, notes, userId, planId ->
                            viewModel.updateSubscriptionRequestStatus(requestId, status, notes, userId, planId)
                        },
                        onExtendUserTrial = { userId, days ->
                            viewModel.extendUserTrial(userId, days)
                        },
                        onUpdateUserTierDirectly = { userId, tierName, days ->
                            viewModel.updateUserTierDirectly(userId, tierName, days)
                        },
                        onPostPromoAnnouncement = { title, content, promoCode, discountPercent, targetPlanId ->
                            viewModel.postPromoAnnouncement(title, content, promoCode, discountPercent, targetPlanId)
                        },
                        onDeletePromoAnnouncement = { id ->
                            viewModel.deletePromoAnnouncement(id)
                        },
                        onTogglePromoStatus = { ids, isActive ->
                            viewModel.setPromoAnnouncementsActive(ids, isActive)
                        },
                        onDeleteUsers = { userIds ->
                            viewModel.deleteUsers(userIds)
                        },
                        onSendBroadcast = { targetCategory, title, content, isUrgent, postAsAnnouncement, onComplete ->
                            viewModel.sendBroadcastMessage(targetCategory, title, content, isUrgent, postAsAnnouncement, onComplete)
                        }
                    )
                }

                composable<Screen.Subscriptions> {
                    SubscriptionsScreen(
                        currentUser = currentUser,
                        subscriptionPlans = subscriptionPlans,
                        activeAnnouncements = activePromoAnnouncements,
                        onBackClick = { navController.popBackStack() },
                        onSubmitSubscriptionRequest = { planId, planTitle, pricePaid ->
                            viewModel.submitSubscriptionRequest(planId, planTitle, pricePaid)
                        }
                    )
                }

                composable<Screen.CompanyAccounting> {
                    CompanyAccountingHubScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenGroupDetail = { targetId ->
                            val directChat = allChats.find {
                                it.id == targetId || it.groupId == targetId || it.id == "chat_wg_$targetId" || it.contactId == targetId
                            }
                            if (directChat != null) {
                                navController.navigate(Screen.ChatDetail(directChat.id))
                            } else {
                                viewModel.ensureWorkGroupChatAndOpen(targetId) { newChatId ->
                                    navController.navigate(Screen.ChatDetail(newChatId))
                                }
                            }
                        },
                        onNavigateToAppSection = navigateToAppDestination
                    )
                }

                composable<Screen.MerchantStore> { backStackEntry ->
                    val route: Screen.MerchantStore = backStackEntry.toRoute()
                    MerchantStoreScreen(
                        viewModel = viewModel,
                        merchantId = route.merchantId,
                        isManageModeRequested = route.isManageMode,
                        onBack = { navController.popBackStack() },
                        onOpenChat = { merchantId ->
                            val existingChat = allChats.find { it.contactId == merchantId }
                            if (existingChat != null) {
                                navController.navigate(Screen.ChatDetail(existingChat.id))
                            } else {
                                val targetContact = allContacts.find { it.id == merchantId } ?: ContactEntity(
                                    id = merchantId,
                                    name = Translations.get("drawer.verified_gold_shop", currentLanguage),
                                    phone = "",
                                    type = UserType.GOLD_TRADER.name,
                                    isVerified = true
                                )
                                viewModel.startChatWithContact(targetContact) { newChatId ->
                                    navController.navigate(Screen.ChatDetail(newChatId))
                                }
                            }
                        }
                    )
                }

                composable<Screen.Gold> {
                    GoldScreen(
                        goldPrices = goldPrices,
                        purchases = allGoldPurchases,
                        alerts = allPriceAlerts,
                        currentUser = currentUser,
                        isLoading = false,
                        isRefreshing = false,
                        onRefresh = { viewModel.forceSyncChats() },
                        lastSyncTime = lastSyncTimestamp,
                        onUpdatePricesClick = { price -> viewModel.updateGoldPrices(price) },
                        onAddPurchaseClick = { showAddPurchaseDialog = true },
                        onSubmitTraderRequest = { shop, cr, tax -> viewModel.submitTraderRequest(shop, cr, tax) },
                        onOpenMerchantStore = { mId, isManage ->
                            navController.navigate(Screen.MerchantStore(merchantId = mId, isManageMode = isManage))
                        },
                        onAddAlert = { metal, karat, targetPrice, direction, note ->
                            viewModel.addPriceAlert(metal, karat, targetPrice, direction, note)
                        },
                        onDeleteAlert = { alertId -> viewModel.deletePriceAlert(alertId) },
                        onToggleAlert = { alertId, isActive -> viewModel.togglePriceAlert(alertId, isActive) },
                        onTestTriggerAlert = { alert -> viewModel.testTriggerPriceAlert(alert) },
                        onResetAlertTrigger = { alertId -> viewModel.resetPriceAlertTrigger(alertId) },
                        onOpenFinancialAccountant = {
                            activeChatForSmartAccountant = null
                            showFinancialAccountantDialog = true
                        },
                        onAddScannedPurchase = { metalType, weight, karat, pricePerGram, mfg, mfgCost, shop, notes ->
                            viewModel.addMetalPurchaseToPortfolio(
                                metalType = metalType,
                                weight = weight,
                                karat = karat,
                                pricePerGram = pricePerGram,
                                manufacturer = mfg,
                                manufacturingCost = mfgCost,
                                mfgType = "PER_GRAM",
                                stampDuty = 0.0,
                                cashbackRefund = 0.0,
                                traderHashtag = shop,
                                sellerShopName = shop,
                                sellerLocation = Translations.get("drawer.recorded_via_app", currentLanguage),
                                scalePhotoUri = null
                            )
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }

        // Global Dialogs
        if (showNewChatDialog) {
            NewChatDialog(
                contacts = allContacts,
                onSelectContact = { contact ->
                    viewModel.startChatWithContact(contact) { newChatId ->
                        showNewChatDialog = false
                        navController.navigate(Screen.ChatDetail(newChatId))
                    }
                },
                onImportContacts = {
                    viewModel.importDeviceContacts(context) { count ->
                        Toast.makeText(
                            context,
                            Translations.format("drawer.imported_contacts_fmt", currentLanguage, "count" to count),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onDismiss = { showNewChatDialog = false }
            )
        }

        if (showNewProjectDialog) {
            NewProjectDialog(
                contacts = allContacts,
                employees = allSubAccounts,
                onCreateProject = { gName, gDesc, pName, cap, type, loc, coPhone, coName, membersInfo, customTypeName, memberInputs ->
                    viewModel.createWorkGroupAndProject(
                        groupName = gName,
                        groupDesc = gDesc,
                        projectName = pName,
                        capital = cap,
                        projectType = type,
                        location = loc,
                        coManagerPhone = coPhone,
                        coManagerName = coName,
                        membersInfo = membersInfo,
                        customProjectTypeName = customTypeName,
                        memberInputs = memberInputs
                    )
                },
                onDismiss = { showNewProjectDialog = false }
            )
        }

        if (showAddPurchaseDialog) {
            AddGoldPurchaseDialog(
                contacts = allContacts,
                onAddPurchase = { metal, weight, karat, price, mfg, mfgCost, mfgType, stamp, cashback, hashtag, shop, loc, scalePhoto, lossRate, lossGrams ->
                    viewModel.addMetalPurchaseToPortfolio(
                        metalType = metal,
                        weight = weight,
                        karat = karat,
                        pricePerGram = price,
                        manufacturer = mfg,
                        manufacturingCost = mfgCost,
                        mfgType = mfgType,
                        stampDuty = stamp,
                        cashbackRefund = cashback,
                        traderHashtag = hashtag,
                        sellerShopName = shop,
                        sellerLocation = loc,
                        scalePhotoUri = scalePhoto,
                        metalLossRatePercent = lossRate,
                        metalLossGrams = lossGrams
                    )
                },
                onDismiss = { showAddPurchaseDialog = false }
            )
        }

        if (showFinancialAccountantDialog) {
            val targetChatId = activeChatForSmartAccountant?.id
            val chatName = activeChatForSmartAccountant?.contactName ?: tr("drawer.all_accounts_scope")
            FinancialAccountantDialog(
                chatName = chatName,
                accounts = remember(allChatAccounts, targetChatId) {
                    if (targetChatId == null) allChatAccounts else allChatAccounts.filter { it.chatId == targetChatId }
                },
                hashtags = remember(allHashtagAccounts, targetChatId) {
                    if (targetChatId == null) allHashtagAccounts else allHashtagAccounts.filter { it.chatId == targetChatId }
                },
                entries = allHashtagEntries,
                transactions = allTransactions,
                ledgerEntries = allLedgerEntries,
                dailyRecords = allDailyAccountingRecords,
                globalSnapshot = globalAccountingSnapshot,
                onOpenVoiceBusinessAgent = {
                    showFinancialAccountantDialog = false
                    showVoiceBusinessAgentDialog = true
                },
                onDismiss = { showFinancialAccountantDialog = false }
            )
        }

        if (showVoiceBusinessAgentDialog) {
            val contactName = activeChatForSmartAccountant?.contactName ?: ""
            VoiceBusinessAgentDialog(
                repository = viewModel.repository,
                mainViewModel = viewModel,
                defaultContactName = contactName,
                onDismiss = { showVoiceBusinessAgentDialog = false },
                onNavigateToChat = { chatId ->
                    showVoiceBusinessAgentDialog = false
                    navController.navigate(Screen.ChatDetail(chatId))
                },
                onNavigateToDestination = { dest ->
                    showVoiceBusinessAgentDialog = false
                    navigateToAppDestination(dest)
                }
            )
        }

        if (showInvoiceVerificationModal) {
            InvoiceVerificationDialog(
                allInvoices = allInvoices,
                onDismiss = { showInvoiceVerificationModal = false }
            )
        }

        if (showEncryptedVaultSheet) {
            EncryptedVaultModalSheet(
                currentUser = currentUser,
                cloudBackups = cloudBackups,
                isProcessing = isBackupProcessing,
                onCreateBackup = { passphrase, isGoogleDrive ->
                    viewModel.createEncryptedBackup(passphrase, isGoogleDrive)
                },
                onDeleteCloudBackup = { backupId ->
                    viewModel.deleteCloudBackup(backupId)
                },
                onRefreshCloudBackups = {
                    viewModel.refreshCloudBackups()
                },
                onOpenRestorePicker = {
                    showEncryptedVaultSheet = false
                    navController.navigate(Screen.MainTabs)
                },
                onDismiss = { showEncryptedVaultSheet = false }
            )
        }

        // Global Toast Notification Host
        ToastNotificationHost(toastController = toastController)

        // Floating Facebook Messenger Style Chat Bubble Overlay
        MessengerBubbleOverlay(
            viewModel = viewModel,
            onNavigateToFullChat = { targetChatId ->
                navController.navigate(Screen.ChatDetail(targetChatId))
            }
        )

        // Incoming Client Card Deep Link / Domain Link Dialog (oozox://card/... or https://oozox.com/card/...)
        val activeDeepLinkHandle = pendingCardHandle
        if (!activeDeepLinkHandle.isNullOrBlank()) {
            val matchedUser = remember(activeDeepLinkHandle, currentUser, allUsers, allContacts) {
                if (currentUser?.username.equals(activeDeepLinkHandle, ignoreCase = true) ||
                    currentUser?.id?.startsWith(activeDeepLinkHandle) == true
                ) {
                    currentUser
                } else {
                    allUsers.firstOrNull {
                        it.username.equals(activeDeepLinkHandle, ignoreCase = true) ||
                            it.id.startsWith(activeDeepLinkHandle)
                    } ?: allContacts.firstOrNull {
                        it.username.equals(activeDeepLinkHandle, ignoreCase = true)
                    }?.let { c ->
                        UserEntity(
                            id = c.id,
                            name = c.name,
                            phone = c.phone,
                            username = c.username.orEmpty().ifBlank { activeDeepLinkHandle },
                            type = c.type,
                            isVerified = c.isVerified
                        )
                    } ?: UserEntity(
                        id = "ext_$activeDeepLinkHandle",
                        name = activeDeepLinkHandle,
                        phone = "",
                        username = activeDeepLinkHandle,
                        isVerified = true
                    )
                }
            } ?: UserEntity(
                id = "ext_$activeDeepLinkHandle",
                name = activeDeepLinkHandle,
                phone = "",
                username = activeDeepLinkHandle,
                isVerified = true
            )
            val cardConfig = remember(matchedUser.id) {
                com.example.util.ClientCardPrefsStore.load(context, matchedUser)
            }
            VisitedDomainCardDialog(
                user = matchedUser,
                config = cardConfig,
                trustScore = if (matchedUser.id == currentUser?.id) 98 else 96,
                reliabilitySummary = tr("drawer.domain_card_verified"),
                onDismiss = {
                    com.example.util.ClientCardDeepLinkRouter.clearPendingHandle()
                }
            )
        }
            }
        }
    }
}
}

/**
 * Ultra-Futuristic Kinetic Island Bottom Dock for OOZOX
 * Dynamic, fluid, pure-icon interface with floating frosted glass, magnetic golden glow,
 * spring-physics animations, and zero text clutter for lightning-fast responsiveness.
 */
@Composable
fun LuxuryBottomSectionBar(
    pagerState: PagerState,
    totalUnreadChats: Int,
    currentLanguage: AppLanguage,
    onTabSelected: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val primaryAccent = adaptiveGoldPrimary
    val onPrimaryAccent = adaptiveOnPrimary

    // Sync state for animated background synchronization indicator
    val isSyncing by OfflineSyncManager.isSyncing.collectAsStateWithLifecycle()
    val syncRotationTransition = rememberInfiniteTransition(label = "sync_rotation")
    val syncRotationAngle by syncRotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_spin"
    )

    val syncPulseAlpha by syncRotationTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sync_pulse"
    )

    val borderBrush = remember(primaryAccent, isSyncing, syncPulseAlpha) {
        if (isSyncing) {
            Brush.horizontalGradient(
                colors = listOf(
                    primaryAccent.copy(alpha = syncPulseAlpha),
                    RealGoldAmber.copy(alpha = 0.8f),
                    primaryAccent.copy(alpha = syncPulseAlpha)
                )
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(
                    primaryAccent.copy(alpha = 0.45f),
                    primaryAccent.copy(alpha = 0.20f),
                    primaryAccent.copy(alpha = 0.55f),
                    primaryAccent.copy(alpha = 0.45f)
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Subtle animated sync pill indicator docked directly above the navigation bar
            AnimatedVisibility(
                visible = isSyncing,
                enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 }
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = tr("common.syncing"),
                            tint = GoldPrimary,
                            modifier = Modifier
                                .size(14.dp)
                                .graphicsLayer(rotationZ = syncRotationAngle)
                        )
                        Text(
                            text = tr("common.syncing"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 12.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = borderBrush
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomTab.entries.forEachIndexed { index, tab ->
                    val isSelected = pagerState.currentPage == index

                    // Smooth calm scale animation (no jitter or shaking)
                    val animatedScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1.0f,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        label = "tab_scale_$index"
                    )

                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) primaryAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        animationSpec = tween(durationMillis = 180),
                        label = "icon_color_$index"
                    )

                    val icon = when (tab) {
                        BottomTab.CHATS -> AppIcons.Chats
                        BottomTab.PROJECTS -> AppIcons.Projects
                        BottomTab.BUSINESS -> AppIcons.Business
                        BottomTab.TRANSACTIONS -> AppIcons.Transactions
                        BottomTab.PROFILE -> AppIcons.Profile
                    }

                    val tabDescription = Translations.get(tab.labelKey, currentLanguage)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onTabSelected(index)
                            }
                            .testTag("bottom_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient magnetic glow for active tab
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                primaryAccent.copy(alpha = 0.22f),
                                                primaryAccent.copy(alpha = 0.06f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                contentAlignment = Alignment.TopEnd,
                                modifier = Modifier.scale(animatedScale)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tabDescription,
                                    tint = iconTint,
                                    modifier = Modifier.size(24.dp)
                                )

                                // Real-time notification badge on chats icon
                                if (tab == BottomTab.CHATS && totalUnreadChats > 0) {
                                    val badgeSize = when {
                                        totalUnreadChats > 99 -> 22.dp
                                        totalUnreadChats > 9 -> 20.dp
                                        else -> 18.dp
                                    }
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 6.dp, y = (-4).dp)
                                            .size(badgeSize)
                                            .clip(CircleShape)
                                            .background(primaryAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (totalUnreadChats > 99) "99+" else totalUnreadChats.toString(),
                                            fontSize = when {
                                                totalUnreadChats > 99 -> 7.5.sp
                                                totalUnreadChats > 9 -> 8.5.sp
                                                else -> 9.sp
                                            },
                                            lineHeight = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = onPrimaryAccent,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            style = LocalTextStyle.current.copy(
                                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                            )
                                        )
                                    }
                                }
                            }

                            // Dynamic micro active dot indicator
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 3.dp)
                                        .width(14.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(primaryAccent, primaryAccent.copy(alpha = 0.6f), primaryAccent)
                                            )
                                        )
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
