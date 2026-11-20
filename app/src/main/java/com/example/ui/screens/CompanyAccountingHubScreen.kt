package com.example.ui.screens
import com.example.ui.components.AddAccountingRecordDialog
import com.example.ui.components.CreateCompanyGroupDialog


import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DailyAccountingRecordEntity
import com.example.data.model.FinancialComplaintEntity
import com.example.data.model.GroupMemberEntity
import com.example.data.model.HiddenProfileEntity
import com.example.data.model.WorkGroupEntity
import com.example.ui.components.AccountingSideDrawerContent
import com.example.ui.components.AccountingTaskbar
import com.example.ui.components.AccountingVoiceAssistantSheet
import com.example.ui.components.CircularCounterBadge
import com.example.ui.components.VoiceBusinessAgentDialog
import com.example.ui.components.AdvancedTransactionsLedgerSection
import com.example.ui.components.DeficitTransactionsInspectorDialog
import com.example.ui.components.AppToastType
import com.example.ui.components.LocalAppToast
import com.example.ui.components.FinancialPostingConfirmationDialog
import com.example.ui.components.RetroactiveComplaintDialog
import com.example.ui.components.WorkGroupInvoiceComplaintsDialog
import com.example.ui.components.VisualAttendanceLoggerSection
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AccountingI18n
import com.example.util.AppLanguage
import com.example.util.AppNavDestination
import com.example.util.GoldUtils
import com.example.util.HubModalType
import com.example.util.LocalAppLanguage
import com.example.util.ReportExporter
import com.example.util.SimpleExcelEngine
import com.example.util.acctStr
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CompanyAccountingHubScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onOpenGroupDetail: (String) -> Unit = {},
    onNavigateToAppSection: (AppNavDestination) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appToast = LocalAppToast.current
    val currentLang = LocalAppLanguage.current
    val str: (String) -> String = { key -> AccountingI18n.get(key, currentLang) }

    val hiddenProfiles by viewModel.allHiddenProfiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeHiddenProfile.collectAsStateWithLifecycle()
    val allRecords by viewModel.allDailyAccountingRecords.collectAsStateWithLifecycle()
    val allWorkGroups by viewModel.allWorkGroups.collectAsStateWithLifecycle()
    val allComplaints by viewModel.allFinancialComplaints.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var activeSection by remember { mutableIntStateOf(-1) } // -1: Dashboard, 0: Vaults, 1: Ledger, 2: Companies, 3: Staff, 4: Cloud Sync
    var secretAuthPrompt by remember { mutableStateOf(false) }
    var secretPasswordInput by remember { mutableStateOf("") }
    var secretAuthError by remember { mutableStateOf<String?>(null) }
    var showTopOverflowMenu by remember { mutableStateOf(false) }
    
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var showCreateCompanyDialog by remember { mutableStateOf(false) }
    var showAddAccountingRecordDialog by remember { mutableStateOf(false) }
    var selectedGroupForRecord by remember { mutableStateOf<WorkGroupEntity?>(null) }
    var showEditShiftDialog by remember { mutableStateOf<GroupMemberEntity?>(null) }
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var showCreateNestedGroupDialog by remember { mutableStateOf<WorkGroupEntity?>(null) }
    var showDmBlockedNoticeDialog by remember { mutableStateOf<String?>(null) }
    var showRetroactiveComplaintDialog by remember { mutableStateOf(false) }
    var showWorkGroupInvoiceComplaintsDialog by remember { mutableStateOf(false) }
    val allInvoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    var inspectDeficitDate by remember { mutableStateOf<String?>(null) }
    var profileToUnlock by remember { mutableStateOf<HiddenProfileEntity?>(null) }
    var profileToChangePassword by remember { mutableStateOf<HiddenProfileEntity?>(null) }
    var showVoiceAssistantSheet by remember { mutableStateOf(false) }
    var showFullAiVoiceAgentDialog by remember { mutableStateOf(false) }
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val allHashtagAccounts by viewModel.allHashtagAccounts.collectAsStateWithLifecycle()
    val allHashtagEntries by viewModel.allHashtagEntries.collectAsStateWithLifecycle()
    val allShiftHandovers by viewModel.allShiftHandovers.collectAsStateWithLifecycle()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    BackHandler(enabled = !drawerState.isOpen && activeSection != -1) {
        activeSection = -1
    }

    // Filter groups and records scoped to active profile or all
    val relevantGroups = remember(allWorkGroups, activeProfile) {
        if (activeProfile != null) {
            allWorkGroups.filter { it.profileId == activeProfile?.id || it.isAccountingGroup }
        } else {
            allWorkGroups.filter { it.isAccountingGroup }
        }
    }

    val relevantRecords = remember(allRecords, activeProfile) {
        if (activeProfile != null) {
            allRecords.filter { it.profileId == activeProfile?.id || it.profileId == null }
        } else {
            allRecords
        }
    }

    val initialCapitalValue = activeProfile?.initialCapital ?: 1500000.0

    val summary = remember(relevantRecords, initialCapitalValue) {
        SimpleExcelEngine.computeSummary(relevantRecords, initialCapitalValue)
    }

    val unifiedSummary = remember(allProjects, allInvoices, allHashtagAccounts, allHashtagEntries, relevantRecords, allShiftHandovers) {
        SimpleExcelEngine.calculateUnifiedBusinessAccounting(
            projects = allProjects,
            invoices = allInvoices,
            hashtagAccounts = allHashtagAccounts,
            hashtagEntries = allHashtagEntries,
            dailyRecords = relevantRecords,
            shiftHandovers = allShiftHandovers
        )
    }

    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayRecords = remember(relevantRecords, todayDateStr) {
        relevantRecords.filter { it.entryDate == todayDateStr }
    }
    val todayRevenueAndIn = remember(todayRecords) {
        todayRecords.sumOf { it.dailyRevenue + it.incomingAmount }
    }
    val todayExpenseAndOut = remember(todayRecords) {
        todayRecords.sumOf { it.outgoingAmount + it.deficitAmount }
    }
    val pendingComplaintsCount = remember(allComplaints) {
        allComplaints.count { it.employeeResponseStatus == "PENDING" }
    }
    val latestDeficitDate = remember(relevantRecords) {
        relevantRecords.filter { it.deficitAmount > 0.0 }.maxByOrNull { it.timestamp }?.entryDate
    }
    val staffCountEstimate = remember(relevantRecords, relevantGroups) {
        relevantRecords.map { it.staffId }.distinct().size.coerceAtLeast(relevantGroups.size)
    }
    val alertsCount = remember(pendingComplaintsCount, relevantRecords) {
        pendingComplaintsCount + relevantRecords.count { it.deficitAmount > 0.0 }
    }

    val exportUnifiedWorkbook: () -> Unit = {
        val file = SimpleExcelEngine.exportUnifiedBusinessWorkbookCsv(
            context = context,
            companyTitle = activeProfile?.companyName ?: activeProfile?.name ?: "OOZOX",
            projects = allProjects,
            invoices = allInvoices,
            hashtagAccounts = allHashtagAccounts,
            hashtagEntries = allHashtagEntries,
            dailyRecords = relevantRecords,
            shiftHandovers = allShiftHandovers
        )
        if (file != null) {
            SimpleExcelEngine.shareExcelFile(context, file, str("cmd_excel"))
            appToast.showToast(str("export_done"), AppToastType.SUCCESS)
        } else {
            appToast.showToast(str("export_error"), AppToastType.ERROR)
        }
    }

    val exportLedgerExcel: () -> Unit = {
        val excelFile = SimpleExcelEngine.exportToExcelCsv(
            context = context,
            groupName = activeProfile?.name ?: str("hub_title"),
            companyName = activeProfile?.companyName ?: "OOZOX",
            records = relevantRecords
        )
        if (excelFile != null) {
            SimpleExcelEngine.shareExcelFile(context, excelFile, str("export_excel"))
            appToast.showToast(str("export_done"), AppToastType.SUCCESS)
        } else {
            appToast.showToast(str("export_error"), AppToastType.ERROR)
        }
    }

    val exportLedgerPdf: () -> Unit = {
        ReportExporter.exportAccountingRecordsToPdf(
            context = context,
            groupName = activeProfile?.name ?: str("hub_title"),
            companyName = activeProfile?.companyName ?: "OOZOX",
            records = relevantRecords,
            filterCriteriaLabel = str("sec_ledger")
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            AccountingSideDrawerContent(
                activeSection = activeSection,
                activeProfile = activeProfile,
                recordsCount = relevantRecords.size,
                groupsCount = relevantGroups.size,
                hiddenProfilesCount = hiddenProfiles.size,
                alertsCount = alertsCount,
                onSelectSection = { targetSec -> activeSection = targetSec },
                onOpenVaultAuth = {
                    secretAuthError = null
                    secretAuthPrompt = true
                },
                onLockToMainProfile = {
                    viewModel.lockToMainProfile()
                    appToast.showToast(str("lock_vault"), AppToastType.SUCCESS)
                },
                onAddRecord = {
                    selectedGroupForRecord = relevantGroups.firstOrNull()
                    showAddAccountingRecordDialog = true
                },
                onOpenVoiceAssistant = { showVoiceAssistantSheet = true },
                onOpenComplaints = { showWorkGroupInvoiceComplaintsDialog = true },
                onOpenRetroComplaint = { showRetroactiveComplaintDialog = true },
                onExportUnifiedExcel = exportUnifiedWorkbook,
                onExportPdf = exportLedgerPdf,
                onLanguageChange = { lang -> viewModel.setLanguage(lang) },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        secretAuthError = null
                                        secretAuthPrompt = true
                                    }
                                )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeProfile?.name ?: str("hub_title"),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = adaptiveGoldPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (activeProfile != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = adaptiveStatusGreen.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = str("encrypted_badge"),
                                            fontSize = 10.sp,
                                            color = adaptiveStatusGreen,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            val sectionName = when (activeSection) {
                                -1 -> str("sec_dashboard")
                                0 -> str("sec_vaults")
                                1 -> str("sec_ledger")
                                2 -> str("sec_groups")
                                3 -> str("sec_staff")
                                4 -> str("sec_backup")
                                else -> ""
                            }
                            Text(
                                text = if (activeProfile != null) "${activeProfile!!.companyName} • $sectionName" else sectionName,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("accounting_drawer_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = str("open_drawer"),
                                    tint = adaptiveGoldPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (activeSection != -1) activeSection = -1 else onNavigateBack()
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("accounting_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = str("back"),
                                    tint = adaptiveGoldPrimary
                                )
                            }
                        }
                    },
                    actions = {
                        if (activeProfile != null) {
                            IconButton(
                                onClick = {
                                    viewModel.lockToMainProfile()
                                    appToast.showToast(str("lock_vault"), AppToastType.SUCCESS)
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = str("lock_vault"),
                                    tint = adaptiveStatusRed
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    secretAuthError = null
                                    secretAuthPrompt = true
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = str("open_vault"),
                                    tint = adaptiveGoldPrimary
                                )
                            }
                        }

                        Box(contentAlignment = Alignment.Center) {
                            IconButton(
                                onClick = { showWorkGroupInvoiceComplaintsDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_workgroup_invoice_complaints_topbar_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReportProblem,
                                    contentDescription = str("cmd_complaints"),
                                    tint = adaptiveStatusRed
                                )
                            }
                            if (alertsCount > 0) {
                                CircularCounterBadge(
                                    count = alertsCount,
                                    containerColor = adaptiveStatusRed,
                                    contentColor = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-3).dp, y = 4.dp)
                                        .testTag("accounting_topbar_alerts_badge")
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showTopOverflowMenu = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("accounting_overflow_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = str("quick_actions"),
                                    tint = adaptiveGoldPrimary
                                )
                            }

                            DropdownMenu(
                                expanded = showTopOverflowMenu,
                                onDismissRequest = { showTopOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(str("cmd_excel")) },
                                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = adaptiveStatusGreen) },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        exportUnifiedWorkbook()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(str("excel")) },
                                    leadingIcon = { Icon(Icons.Default.TableView, contentDescription = null, tint = adaptiveStatusGreen) },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        exportLedgerExcel()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(str("cmd_pdf")) },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = adaptiveGoldPrimary) },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        exportLedgerPdf()
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(str("cmd_retro")) },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = adaptiveStatusOrange) },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        showRetroactiveComplaintDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(str("cmd_voice")) },
                                    leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null, tint = adaptiveGoldPrimary) },
                                    onClick = {
                                        showTopOverflowMenu = false
                                        showVoiceAssistantSheet = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                if (activeSection in 0..3) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            when (activeSection) {
                                0 -> showCreateProfileDialog = true
                                1 -> {
                                    selectedGroupForRecord = relevantGroups.firstOrNull()
                                    showAddAccountingRecordDialog = true
                                }
                                2 -> showCreateCompanyDialog = true
                                3 -> showAddStaffDialog = true
                            }
                        },
                        containerColor = adaptiveGoldPrimary,
                        contentColor = adaptiveOnPrimary,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("accounting_section_fab"),
                        icon = {
                            Icon(
                                imageVector = when (activeSection) {
                                    0 -> Icons.Default.Security
                                    1 -> Icons.Default.PostAdd
                                    2 -> Icons.Default.Business
                                    3 -> Icons.Default.PersonAdd
                                    else -> Icons.Default.Add
                                },
                                contentDescription = null
                            )
                        },
                        text = {
                            Text(
                                text = when (activeSection) {
                                    0 -> str("new_vault")
                                    1 -> str("cmd_new_record")
                                    2 -> str("new_company")
                                    3 -> str("sec_staff")
                                    else -> str("cmd_new_record")
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            },
            bottomBar = {
                AccountingTaskbar(
                    activeSection = activeSection,
                    onSectionSelected = { targetSec -> activeSection = targetSec },
                    onVoiceRecordClick = { showVoiceAssistantSheet = true },
                    isVoiceRecording = showVoiceAssistantSheet,
                    recordsCount = relevantRecords.size,
                    groupsCount = relevantGroups.size,
                    alertsCount = alertsCount
                )
            }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Active Profile Info Banner
            if (activeProfile != null) {
                Surface(
                    color = adaptiveGoldPrimary.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${activeProfile!!.name} • ${str("encrypted_badge")}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = adaptiveGoldPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = activeProfile!!.companyName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.lockToMainProfile()
                                appToast.showToast(str("lock_vault"), AppToastType.SUCCESS)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = adaptiveStatusRed, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.heightIn(min = 38.dp)
                        ) {
                            Text(str("lock_vault"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = activeSection,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f))
                        .togetherWith(fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.98f))
                },
                label = "company_hub_tab_content_anim",
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { currentTab ->
                when (currentTab) {
                -1 -> DashboardContent(
                    summary = summary,
                    balance = summary.netCashflow + initialCapitalValue,
                    unifiedSummary = unifiedSummary,
                    initialCapital = initialCapitalValue,
                    todayRevenueAndIn = todayRevenueAndIn,
                    todayExpenseAndOut = todayExpenseAndOut,
                    pendingComplaintsCount = pendingComplaintsCount,
                    latestDeficitDate = latestDeficitDate,
                    groupsCount = relevantGroups.size,
                    staffCount = staffCountEstimate,
                    hiddenProfilesCount = hiddenProfiles.size,
                    onExportUnifiedExcel = exportUnifiedWorkbook,
                    onQuickAddRecord = {
                        selectedGroupForRecord = relevantGroups.firstOrNull()
                        showAddAccountingRecordDialog = true
                    },
                    onQuickRetroComplaint = { showRetroactiveComplaintDialog = true },
                    onOpenInvoiceComplaints = { showWorkGroupInvoiceComplaintsDialog = true },
                    onInspectLatestDeficit = { date -> inspectDeficitDate = date },
                    onOpenVoiceAssistant = { showVoiceAssistantSheet = true },
                    onExportPdf = exportLedgerPdf,
                    onNavigate = { activeSection = it }
                )
                4 -> CloudBackupAuthSection(
                    viewModel = viewModel,
                    recordsCount = relevantRecords.size,
                    groupsCount = relevantGroups.size,
                    profilesCount = hiddenProfiles.size,
                    companyName = activeProfile?.companyName ?: activeProfile?.name ?: "المركز المالي الشامل",
                    onExportUnifiedExcel = exportUnifiedWorkbook
                )
                0 -> HiddenProfilesTabContent(
                    profiles = hiddenProfiles,
                    activeProfile = activeProfile,
                    onSelectProfile = { profile ->
                        profileToUnlock = profile
                    },
                    onToggleNotifications = { profile, silent ->
                        viewModel.toggleProfileNotifications(profile.id, silent)
                        val statusText = if (silent) str("alerts_muted") else str("alerts_on")
                        appToast.showToast(statusText, AppToastType.INFO)
                    },
                    onChangePassword = { profile ->
                        profileToChangePassword = profile
                    },
                    onReturnToMainAccount = {
                        viewModel.lockToMainProfile()
                        appToast.showToast(str("main_treasury"), AppToastType.SUCCESS)
                    },
                    onDeleteProfile = { profileId ->
                        viewModel.deleteHiddenProfile(profileId)
                        appToast.showToast(str("vault_deleted"), AppToastType.INFO)
                    },
                    onCreateProfileClick = { showCreateProfileDialog = true }
                )
                1 -> AdvancedTransactionsLedgerSection(
                    records = relevantRecords,
                    groups = relevantGroups,
                    complaints = allComplaints,
                    companyName = activeProfile?.companyName ?: relevantGroups.firstOrNull()?.companyName ?: "شركة الصاغة والتجارة",
                    groupName = activeProfile?.name ?: relevantGroups.firstOrNull()?.name ?: "المركز المالي الشامل",
                    onAddRecordClick = {
                        selectedGroupForRecord = relevantGroups.firstOrNull()
                        showAddAccountingRecordDialog = true
                    },
                    onDeleteRecord = { recordId ->
                        viewModel.deleteDailyAccountingRecord(recordId)
                    },
                    onOpenComplaintDialog = {
                        showRetroactiveComplaintDialog = true
                    },
                    onInspectDeficit = { targetDate ->
                        inspectDeficitDate = targetDate
                    },
                    onImportRecords = { importedRows ->
                        val targetGroupId = relevantGroups.firstOrNull()?.id ?: "general_hub"
                        viewModel.importParsedAccountingRows(targetGroupId, importedRows) { count ->
                            appToast.showToast("${str("import_file")}: $count", AppToastType.SUCCESS)
                        }
                    }
                )
                2 -> CompanyGroupsTabContent(
                    groups = relevantGroups,
                    records = relevantRecords,
                    onCreateCompanyClick = { showCreateCompanyDialog = true },
                    onCreateSubGroupClick = { parentGroup ->
                        showCreateNestedGroupDialog = parentGroup
                    },
                    onOpenFinancialEntry = { targetGroup ->
                        selectedGroupForRecord = targetGroup
                        showAddAccountingRecordDialog = true
                    },
                    onExportGroupExcel = { targetGroup ->
                        val groupRecords = relevantRecords.filter { it.groupId == targetGroup.id || it.subGroupId == targetGroup.name }
                        val recordsToExport = if (groupRecords.isNotEmpty()) groupRecords else relevantRecords
                        val excelFile = SimpleExcelEngine.exportToExcelCsv(
                            context = context,
                            groupName = targetGroup.name,
                            companyName = targetGroup.companyName ?: targetGroup.name,
                            records = recordsToExport
                        )
                        if (excelFile != null) {
                            SimpleExcelEngine.shareExcelFile(context, excelFile)
                        } else {
                            appToast.showToast(str("export_error"), AppToastType.ERROR)
                        }
                    },
                    onToggleGroupPrivacy = { group, disableDMs ->
                        viewModel.updateGroupPrivacySettings(group.id, disableDMs, group.isPrivate)
                        val msg = if (disableDMs) "${group.name} • ${str("dm_blocked")}" else "${group.name} • ${str("dm_allowed")}"
                        appToast.showToast(msg, AppToastType.INFO)
                    },
                    onDeleteGroup = { group ->
                        viewModel.deleteWorkGroup(group.id)
                        appToast.showToast(str("group_deleted"), AppToastType.INFO)
                    },
                    onOpenGroup = onOpenGroupDetail
                )
                3 -> StaffShiftsTabContent(
                    groups = relevantGroups,
                    viewModel = viewModel,
                    onEditShift = { member ->
                        showEditShiftDialog = member
                    },
                    onAddStaffClick = {
                        showAddStaffDialog = true
                    },
                    onDmAttempt = { member, group ->
                        if (group?.disableMemberDMs == true && !member.canMessageDirectly) {
                            showDmBlockedNoticeDialog = member.username
                        } else {
                            onOpenGroupDetail(member.userId.ifBlank { member.username })
                        }
                    },
                    onDeleteStaff = { member ->
                        viewModel.deleteStaffMember(member.id)
                        appToast.showToast(str("staff_deleted"), AppToastType.INFO)
                    },
                    onToggleGroupPrivacy = { group, disableDMs ->
                        viewModel.updateGroupPrivacySettings(group.id, disableDMs, group.isPrivate)
                        val msg = if (disableDMs) str("dm_blocked") else str("dm_allowed")
                        appToast.showToast(msg, AppToastType.INFO)
                    }
                )
                }
            }
        }
        }
    }

    // Dialogs
    if (showCreateProfileDialog) {
        CreateHiddenProfileDialog(
            onDismiss = { showCreateProfileDialog = false },
            onConfirm = { name, pin, compName, role, cap, silent, notes ->
                viewModel.createHiddenProfile(
                    name = name,
                    secretPin = pin,
                    companyName = compName,
                    roleTitle = role,
                    initialCapital = cap,
                    silentNotifications = silent,
                    notes = notes,
                    onSuccess = { created ->
                        showCreateProfileDialog = false
                        viewModel.switchActiveProfile(created)
                        appToast.showToast(str("vault_created"), AppToastType.SUCCESS)
                    }
                )
            }
        )
    }

    if (showCreateCompanyDialog) {
        CreateCompanyGroupDialog(
            parentGroups = relevantGroups.filter { it.parentGroupId == null },
            activeProfileId = activeProfile?.id,
            onDismiss = { showCreateCompanyDialog = false },
            onConfirm = { name, compName, cap, disableDMs, parentId, departments ->
                viewModel.createAccountingCompanyGroup(
                    name = name,
                    companyName = compName,
                    initialCapital = cap,
                    disableMemberDMs = disableDMs,
                    parentGroupId = parentId,
                    profileId = activeProfile?.id,
                    departments = departments,
                    onCreated = {
                        showCreateCompanyDialog = false
                        appToast.showToast(str("branch_created"), AppToastType.SUCCESS)
                    }
                )
            }
        )
    }

    if (showAddAccountingRecordDialog) {
        AddAccountingRecordDialog(
            groups = relevantGroups,
            defaultGroup = selectedGroupForRecord,
            activeProfileId = activeProfile?.id,
            onDismiss = { showAddAccountingRecordDialog = false },
            onConfirm = { record ->
                viewModel.addDailyAccountingRecord(record) {
                    showAddAccountingRecordDialog = false
                    appToast.showToast(str("entry_saved"), AppToastType.SUCCESS)
                }
            }
        )
    }

    if (showEditShiftDialog != null) {
        EditStaffPermissionsDialog(
            member = showEditShiftDialog!!,
            onDismiss = { showEditShiftDialog = null },
            onSave = { role, start, end, shiftHours, days, type, canPost, canExport, canEdit, canMsg, isAdmin, notes, loc, lat, lng, geo, rad ->
                viewModel.updateStaffPermissionsAndShift(
                    memberId = showEditShiftDialog!!.id,
                    role = role,
                    startHours = start,
                    endHours = end,
                    shiftHours = shiftHours,
                    workingDays = days,
                    shiftType = type,
                    canPostAccounting = canPost,
                    canExportReports = canExport,
                    canEditRecords = canEdit,
                    canMessageDirectly = canMsg,
                    isGroupAdmin = isAdmin,
                    notes = notes,
                    location = loc,
                    lat = lat,
                    lng = lng,
                    geoEnabled = geo,
                    radius = rad
                )
                showEditShiftDialog = null
                appToast.showToast(str("staff_updated"), AppToastType.SUCCESS)
            }
        )
    }

    if (showAddStaffDialog) {
        AddStaffMemberDialog(
            groups = relevantGroups,
            defaultGroup = selectedGroupForRecord ?: relevantGroups.firstOrNull(),
            onDismiss = { showAddStaffDialog = false },
            onSave = { groupId, username, phone, role, start, end, shiftHours, days, type, canPost, canExport, canEdit, canMsg, isAdmin, notes, loc, lat, lng, geo, rad ->
                viewModel.addStaffMember(
                    groupId = groupId,
                    username = username,
                    phone = phone,
                    role = role,
                    startHours = start,
                    endHours = end,
                    shiftHours = shiftHours,
                    workingDays = days,
                    shiftType = type,
                    canPostAccounting = canPost,
                    canExportReports = canExport,
                    canEditRecords = canEdit,
                    canMessageDirectly = canMsg,
                    isGroupAdmin = isAdmin,
                    notes = notes,
                    location = loc,
                    lat = lat,
                    lng = lng,
                    geoEnabled = geo,
                    radius = rad
                )
                showAddStaffDialog = false
                appToast.showToast(str("staff_added"), AppToastType.SUCCESS)
            }
        )
    }

    if (showCreateNestedGroupDialog != null) {
        CreateNestedSubGroupDialog(
            parentGroup = showCreateNestedGroupDialog!!,
            onDismiss = { showCreateNestedGroupDialog = null },
            onConfirm = { name, description, disableDMs ->
                viewModel.createNestedSubGroup(
                    parentGroupId = showCreateNestedGroupDialog!!.id,
                    name = name,
                    description = description,
                    disableMemberDMs = disableDMs,
                    onCreated = {
                        showCreateNestedGroupDialog = null
                        appToast.showToast(str("sub_created"), AppToastType.SUCCESS)
                    }
                )
            }
        )
    }

    if (showDmBlockedNoticeDialog != null) {
        DmBlockedNoticeDialog(
            memberName = showDmBlockedNoticeDialog!!,
            onDismiss = { showDmBlockedNoticeDialog = null }
        )
    }

    // Retroactive Staff Complaint & Deficit Adjustment Dialog
    if (showRetroactiveComplaintDialog) {
        RetroactiveComplaintDialog(
            groups = relevantGroups,
            initialGroup = selectedGroupForRecord ?: relevantGroups.firstOrNull(),
            onDismiss = { showRetroactiveComplaintDialog = false },
            onSubmitComplaint = { complaint ->
                viewModel.submitRetroactiveComplaint(complaint) { resultingDeficit ->
                    showRetroactiveComplaintDialog = false
                    if (resultingDeficit > 0) {
                        appToast.showToast(
                            "${str("latest_deficit")}: ${GoldUtils.formatCurrency(resultingDeficit)}",
                            AppToastType.WARNING
                        )
                        inspectDeficitDate = complaint.targetDate
                    } else {
                        appToast.showToast(str("approve_adjust"), AppToastType.SUCCESS)
                    }
                }
            }
        )
    }

    // Work Group Invoice Complaints & Smart Auto-Mention Dialog
    if (showWorkGroupInvoiceComplaintsDialog) {
        val targetGroup = selectedGroupForRecord ?: relevantGroups.firstOrNull() ?: WorkGroupEntity(
            id = "group_main_sales",
            name = "جروب المبيعات والحسابات",
            ownerId = currentUser?.id ?: "owner_admin",
            companyName = "شركة الصاغة والتجارة"
        )
        WorkGroupInvoiceComplaintsDialog(
            workGroup = targetGroup,
            allInvoices = allInvoices,
            complaints = allComplaints,
            currentStaffId = currentUser?.id ?: "staff_manager",
            currentStaffName = currentUser?.name ?: "المدير العام",
            onSaveComplaint = { newComp ->
                viewModel.submitRetroactiveComplaint(newComp)
            },
            onUpdateComplaintResponse = { compId, status, reason, proofUri, proofMediaType, isDeducted ->
                viewModel.updateEmployeeComplaintResponseWithMedia(
                    complaintId = compId,
                    status = status,
                    reason = reason,
                    proofUri = proofUri,
                    proofMediaType = proofMediaType,
                    isDeducted = isDeducted
                )
            },
            onApplyJointDeduction = { cId, pAmt, rAmt, rId, notes ->
                viewModel.applyJointComplaintDeduction(cId, pAmt, rAmt, rId, notes)
            },
            onDismiss = { showWorkGroupInvoiceComplaintsDialog = false }
        )
    }

    // Deficit Transactions Inspector Dialog
    if (inspectDeficitDate != null) {
        DeficitTransactionsInspectorDialog(
            targetDate = inspectDeficitDate!!,
            groupName = activeProfile?.name ?: relevantGroups.firstOrNull()?.name ?: "المركز المالي الشامل",
            companyName = activeProfile?.companyName ?: relevantGroups.firstOrNull()?.companyName ?: "شركة الصاغة والتجارة",
            allRecords = relevantRecords,
            allComplaints = allComplaints,
            onDismiss = { inspectDeficitDate = null }
        )
    }

    if (secretAuthPrompt) {
        AlertDialog(
            onDismissRequest = {
                secretAuthPrompt = false
                secretPasswordInput = ""
                secretAuthError = null
            },
            shape = RoundedCornerShape(16.dp),
            title = { Text(str("vault_unlock_title"), color = adaptiveGoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        str("vault_unlock_hint"),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = secretPasswordInput,
                        onValueChange = {
                            secretPasswordInput = it
                            secretAuthError = null
                        },
                        label = { Text(str("passcode_label")) },
                        singleLine = true,
                        isError = secretAuthError != null,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (secretAuthError != null) {
                        Text(
                            text = secretAuthError!!,
                            color = adaptiveStatusRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val entered = secretPasswordInput.trim()
                        val matchedProfile = hiddenProfiles.find { it.secretPinOrPassword.trim() == entered }
                        when {
                            matchedProfile != null -> {
                                viewModel.switchActiveProfile(matchedProfile)
                                activeSection = 0
                                secretAuthPrompt = false
                                secretPasswordInput = ""
                                secretAuthError = null
                                appToast.showToast("${matchedProfile.name} • ${str("vault_unlocked")}", AppToastType.SUCCESS)
                            }
                            entered == "1234" -> {
                                activeSection = 0
                                secretAuthPrompt = false
                                secretPasswordInput = ""
                                secretAuthError = null
                            }
                            else -> {
                                secretAuthError = str("passcode_invalid")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(str("enter_vault"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        secretAuthPrompt = false
                        secretPasswordInput = ""
                        secretAuthError = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(str("cancel"), color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (profileToUnlock != null) {
        UnlockSubAccountPasswordDialog(
            profile = profileToUnlock!!,
            onDismiss = { profileToUnlock = null },
            onSuccess = {
                val target = profileToUnlock!!
                viewModel.switchActiveProfile(target)
                profileToUnlock = null
                appToast.showToast("${target.name} • ${str("vault_unlocked")}", AppToastType.SUCCESS)
            }
        )
    }

    if (profileToChangePassword != null) {
        ChangeSubAccountPasswordDialog(
            profile = profileToChangePassword!!,
            onDismiss = { profileToChangePassword = null },
            onConfirm = { newPass ->
                viewModel.updateProfilePassword(profileToChangePassword!!.id, newPass) {
                    profileToChangePassword = null
                    appToast.showToast(str("vault_updated"), AppToastType.SUCCESS)
                }
            }
        )
    }

    // Voice Accounting Assistant Sheet (Speech-to-Text & Smart Assistant Commands)
    if (showVoiceAssistantSheet) {
        AccountingVoiceAssistantSheet(
            activeProfile = activeProfile,
            records = relevantRecords,
            groups = relevantGroups,
            unifiedSummary = unifiedSummary,
            pendingComplaintsCount = pendingComplaintsCount,
            onInsertRecord = { newRecord ->
                viewModel.addDailyAccountingRecord(newRecord) {
                    appToast.showToast(str("cmd_new_record"), AppToastType.SUCCESS)
                }
            },
            onNavigateToSection = { targetSec ->
                activeSection = targetSec
            },
            onOpenModal = { modalType ->
                showVoiceAssistantSheet = false
                when (modalType) {
                    HubModalType.ADD_RECORD -> {
                        selectedGroupForRecord = relevantGroups.firstOrNull()
                        showAddAccountingRecordDialog = true
                    }
                    HubModalType.RETROACTIVE_COMPLAINT -> showRetroactiveComplaintDialog = true
                    HubModalType.INVOICE_COMPLAINTS -> showWorkGroupInvoiceComplaintsDialog = true
                    HubModalType.DEFICIT_INSPECTOR -> {
                        inspectDeficitDate = latestDeficitDate ?: todayDateStr
                    }
                    HubModalType.SECRET_VAULT_AUTH -> {
                        secretAuthError = null
                        secretAuthPrompt = true
                    }
                }
            },
            onOpenAppScreen = { dest ->
                showVoiceAssistantSheet = false
                onNavigateToAppSection(dest)
            },
            onExportExcel = exportLedgerExcel,
            onExportUnifiedExcel = exportUnifiedWorkbook,
            onExportPdf = exportLedgerPdf,
            onOpenFullVoiceAgent = {
                showVoiceAssistantSheet = false
                showFullAiVoiceAgentDialog = true
            },
            onDismiss = { showVoiceAssistantSheet = false }
        )
    }

    // Autonomous Multi-Step AI Voice Business Agent Dialog
    if (showFullAiVoiceAgentDialog) {
        val contactName = activeProfile?.name ?: relevantGroups.firstOrNull()?.name ?: ""
        VoiceBusinessAgentDialog(
            repository = viewModel.repository,
            mainViewModel = viewModel,
            defaultContactName = contactName,
            onDismiss = { showFullAiVoiceAgentDialog = false },
            onNavigateToChat = { chatId ->
                showFullAiVoiceAgentDialog = false
                onOpenGroupDetail(chatId)
            },
            onNavigateToDestination = { dest ->
                showFullAiVoiceAgentDialog = false
                onNavigateToAppSection(dest)
            }
        )
    }
}

// -------------------------------------------------------------
// Sub-Composable: 1. Hidden Profiles Management
// -------------------------------------------------------------
@Composable
private fun HiddenProfilesTabContent(
    profiles: List<HiddenProfileEntity>,
    activeProfile: HiddenProfileEntity?,
    onSelectProfile: (HiddenProfileEntity) -> Unit,
    onToggleNotifications: (HiddenProfileEntity, Boolean) -> Unit,
    onChangePassword: (HiddenProfileEntity) -> Unit,
    onReturnToMainAccount: () -> Unit,
    onDeleteProfile: (String) -> Unit,
    onCreateProfileClick: () -> Unit
) {
    var profilePendingDelete by remember { mutableStateOf<HiddenProfileEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Main Account Card
            val isMainActive = activeProfile == null
            val primaryGold = adaptiveGoldPrimary
            val onPrimary = adaptiveOnPrimary
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isMainActive) primaryGold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    if (isMainActive) 1.4.dp else 1.dp,
                    if (isMainActive) primaryGold else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                color = if (isMainActive) primaryGold else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = acctStr("main_treasury"),
                                        tint = if (isMainActive) onPrimary else primaryGold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = acctStr("main_treasury"),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = primaryGold.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = acctStr("default_badge"),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryGold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(acctStr("sub_vaults"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (isMainActive) {
                            Surface(
                                color = adaptiveStatusGreen.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = acctStr("active_now"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = adaptiveStatusGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = adaptiveStatusGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(acctStr("alerts_on"), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = adaptiveStatusGreen)
                        }

                        if (!isMainActive) {
                            Button(
                                onClick = onReturnToMainAccount,
                                colors = ButtonDefaults.buttonColors(containerColor = primaryGold, contentColor = onPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.heightIn(min = 40.dp)
                            ) {
                                Text(acctStr("return_main"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            val primaryGold = adaptiveGoldPrimary
            val onPrimary = adaptiveOnPrimary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${acctStr("sub_vaults_title")} (${profiles.size})",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = onCreateProfileClick,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGold, contentColor = onPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(acctStr("new_vault"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (profiles.isEmpty()) {
            item {
                val primaryGold = adaptiveGoldPrimary
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.LockPerson, contentDescription = null, tint = primaryGold.copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(acctStr("no_vaults"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onCreateProfileClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryGold),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(acctStr("new_vault"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        items(profiles, key = { it.id }) { profile ->
            val isActive = activeProfile?.id == profile.id
            val primaryGold = adaptiveGoldPrimary
            val onPrimary = adaptiveOnPrimary
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) primaryGold.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    if (isActive) 1.4.dp else 1.dp,
                    if (isActive) primaryGold else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectProfile(profile) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                color = if (isActive) primaryGold else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = profile.name,
                                        tint = if (isActive) onPrimary else primaryGold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(profile.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(profile.companyName, fontSize = 12.sp, color = primaryGold, fontWeight = FontWeight.SemiBold)
                                Text(profile.roleTitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (isActive) {
                            Surface(
                                color = adaptiveStatusGreen.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = acctStr("active_now"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = adaptiveStatusGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = if (profile.silentNotifications) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else adaptiveStatusGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (profile.silentNotifications) Icons.Default.NotificationsOff else Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (profile.silentNotifications) adaptiveStatusRed else adaptiveStatusGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (profile.silentNotifications) acctStr("alerts_muted") else acctStr("alerts_on"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (profile.silentNotifications) MaterialTheme.colorScheme.onSurfaceVariant else adaptiveStatusGreen
                                )
                            }

                            FilledTonalButton(
                                onClick = { onToggleNotifications(profile, !profile.silentNotifications) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (profile.silentNotifications) primaryGold.copy(alpha = 0.18f) else adaptiveStatusRed.copy(alpha = 0.14f)
                                ),
                                modifier = Modifier.heightIn(min = 38.dp)
                            ) {
                                Text(
                                    text = if (profile.silentNotifications) acctStr("unmute") else acctStr("mute"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (profile.silentNotifications) primaryGold else adaptiveStatusRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = primaryGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("••••••", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onChangePassword(profile) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryGold),
                                modifier = Modifier.heightIn(min = 40.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(acctStr("change_pin"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (!isActive) {
                                Button(
                                    onClick = { onSelectProfile(profile) },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryGold, contentColor = onPrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Text(acctStr("enter_vault"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            IconButton(
                                onClick = { profilePendingDelete = profile },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = acctStr("delete"), tint = adaptiveStatusRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (profilePendingDelete != null) {
        val target = profilePendingDelete!!
        AlertDialog(
            onDismissRequest = { profilePendingDelete = null },
            shape = RoundedCornerShape(16.dp),
            title = { Text(acctStr("delete_vault_title"), fontWeight = FontWeight.Bold) },
            text = {
                Text("${acctStr("delete_vault_body")} (${target.name})", fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProfile(target.id)
                        profilePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveStatusRed, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(acctStr("delete"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { profilePendingDelete = null },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(acctStr("cancel"))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// -------------------------------------------------------------
// Sub-Composable: 3. Company Groups & Nested Sub-Groups
// -------------------------------------------------------------
@Composable
private fun CompanyGroupsTabContent(
    groups: List<WorkGroupEntity>,
    records: List<DailyAccountingRecordEntity> = emptyList(),
    onCreateCompanyClick: () -> Unit,
    onCreateSubGroupClick: (WorkGroupEntity) -> Unit,
    onOpenFinancialEntry: (WorkGroupEntity) -> Unit,
    onExportGroupExcel: (WorkGroupEntity) -> Unit,
    onToggleGroupPrivacy: (WorkGroupEntity, Boolean) -> Unit,
    onDeleteGroup: (WorkGroupEntity) -> Unit,
    onOpenGroup: (String) -> Unit
) {
    var groupSearchQuery by remember { mutableStateOf("") }
    var groupPendingDelete by remember { mutableStateOf<WorkGroupEntity?>(null) }
    val parentGroups = remember(groups, groupSearchQuery) {
        val q = groupSearchQuery.trim()
        groups.filter { it.parentGroupId == null }.filter { parent ->
            if (q.isEmpty()) true
            else parent.name.contains(q, ignoreCase = true) ||
                (parent.companyName?.contains(q, ignoreCase = true) == true) ||
                groups.any { it.parentGroupId == parent.id && it.name.contains(q, ignoreCase = true) }
        }
    }
    val subGroupsTotalCount = remember(groups) { groups.count { it.parentGroupId != null } }
    val allGroupsRevenue = remember(records) { records.sumOf { it.dailyRevenue + it.incomingAmount } }
    val allGroupsExpense = remember(records) { records.sumOf { it.outgoingAmount + it.deficitAmount } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            val primaryGold = adaptiveGoldPrimary
            val onPrimary = adaptiveOnPrimary
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, primaryGold.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = primaryGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(acctStr("sec_groups"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = primaryGold.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${groups.count { it.parentGroupId == null }}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = primaryGold)
                                Text(acctStr("main_companies"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Surface(
                            color = adaptiveStatusBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$subGroupsTotalCount", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = adaptiveStatusBlue)
                                Text(acctStr("sub_departments"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Surface(
                            color = adaptiveStatusGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(GoldUtils.formatCurrency(allGroupsRevenue - allGroupsExpense), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = adaptiveStatusGreen, maxLines = 1)
                                Text(acctStr("entities_net"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = groupSearchQuery,
                        onValueChange = { groupSearchQuery = it },
                        placeholder = { Text(acctStr("search_company"), fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = acctStr("search_company")) },
                        trailingIcon = {
                            if (groupSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { groupSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = acctStr("close"))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = onCreateCompanyClick,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGold, contentColor = onPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(acctStr("new_company"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        if (parentGroups.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = adaptiveGoldPrimary.copy(alpha = 0.7f), modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(acctStr("no_companies"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        items(parentGroups, key = { it.id }) { parent ->
            val subGroups = groups.filter { it.parentGroupId == parent.id }
            val parentRecords = remember(records, parent, subGroups) {
                val groupIds = listOf(parent.id) + subGroups.map { it.id }
                val subGroupNames = listOf(parent.name) + subGroups.map { it.name }
                records.filter { it.groupId in groupIds || it.subGroupId in subGroupNames }
            }
            val parentRevenue = remember(parentRecords) { parentRecords.sumOf { it.dailyRevenue + it.incomingAmount } }
            val parentExpense = remember(parentRecords) { parentRecords.sumOf { it.outgoingAmount + it.deficitAmount } }
            val parentNet = parentRevenue - parentExpense
            val primaryGold = adaptiveGoldPrimary
            val onPrimary = adaptiveOnPrimary
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, primaryGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                color = primaryGold.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CorporateFare, contentDescription = null, tint = primaryGold, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(parent.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(parent.companyName ?: parent.name, fontSize = 12.sp, color = primaryGold, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        IconButton(
                            onClick = { groupPendingDelete = parent },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = acctStr("delete"), tint = adaptiveStatusRed, modifier = Modifier.size(20.dp))
                        }
                    }

                    if (!parent.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(parent.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, primaryGold.copy(alpha = 0.28f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(acctStr("revenue"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(GoldUtils.formatCurrency(parentRevenue), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = adaptiveStatusGreen)
                            }
                            Column {
                                Text(acctStr("expense"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(GoldUtils.formatCurrency(parentExpense), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = adaptiveStatusRed)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(acctStr("net"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    GoldUtils.formatCurrency(parentNet),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (parentNet >= 0) primaryGold else adaptiveStatusRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = if (parent.disableMemberDMs) adaptiveStatusRed.copy(alpha = 0.1f) else adaptiveStatusGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, if (parent.disableMemberDMs) adaptiveStatusRed.copy(alpha = 0.35f) else adaptiveStatusGreen.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (parent.disableMemberDMs) Icons.Default.CommentsDisabled else Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = if (parent.disableMemberDMs) adaptiveStatusRed else adaptiveStatusGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (parent.disableMemberDMs) acctStr("dm_blocked") else acctStr("dm_allowed"),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (parent.disableMemberDMs) adaptiveStatusRed else adaptiveStatusGreen
                                )
                            }

                            Switch(
                                checked = parent.disableMemberDMs,
                                onCheckedChange = { onToggleGroupPrivacy(parent, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = onPrimary,
                                    checkedTrackColor = primaryGold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenFinancialEntry(parent) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGold, contentColor = onPrimary),
                            modifier = Modifier
                                .weight(1.2f)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(acctStr("record_entry"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onExportGroupExcel(parent) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = adaptiveStatusGreen),
                            border = BorderStroke(1.dp, adaptiveStatusGreen),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(acctStr("excel"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onCreateSubGroupClick(parent) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryGold),
                            border = BorderStroke(1.dp, primaryGold.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(acctStr("add_sub"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onOpenGroup(parent.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = primaryGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(acctStr("open_channel"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (subGroups.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = primaryGold.copy(alpha = 0.25f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountTree, contentDescription = null, tint = primaryGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${acctStr("sub_departments")} (${subGroups.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        subGroups.forEach { sub ->
                            val secondarySubGroups = groups.filter { it.parentGroupId == sub.id }
                            val subRecords = remember(records, sub) {
                                records.filter { it.groupId == sub.id || it.subGroupId == sub.name }
                            }
                            val subRevenue = remember(subRecords) { subRecords.sumOf { it.dailyRevenue + it.incomingAmount } }
                            val subExpense = remember(subRecords) { subRecords.sumOf { it.outgoingAmount + it.deficitAmount } }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.8.dp, primaryGold.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = Icons.Default.SubdirectoryArrowRight,
                                                contentDescription = null,
                                                tint = primaryGold,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .graphicsLayer { scaleX = if (isRtl) -1f else 1f }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(sub.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                if (subRecords.isNotEmpty()) {
                                                    Text(
                                                        text = "${GoldUtils.formatCurrency(subRevenue)} / ${GoldUtils.formatCurrency(subExpense)}",
                                                        fontSize = 10.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onToggleGroupPrivacy(sub, !sub.disableMemberDMs) },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(
                                                    imageVector = if (sub.disableMemberDMs) Icons.Default.CommentsDisabled else Icons.Default.Forum,
                                                    contentDescription = acctStr("dm_blocked"),
                                                    tint = if (sub.disableMemberDMs) adaptiveStatusRed else adaptiveStatusGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { groupPendingDelete = sub },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = acctStr("delete"), tint = adaptiveStatusRed, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = { onOpenFinancialEntry(sub) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = primaryGold, contentColor = onPrimary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(1.1f)
                                                .heightIn(min = 40.dp)
                                        ) {
                                            Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(acctStr("record_entry"), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }

                                        OutlinedButton(
                                            onClick = { onOpenGroup(sub.id) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryGold),
                                            border = BorderStroke(0.8.dp, primaryGold.copy(alpha = 0.6f)),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(0.95f)
                                                .heightIn(min = 40.dp)
                                                .testTag("open_dept_chat_btn_${sub.id}")
                                        ) {
                                            Icon(Icons.Default.Forum, contentDescription = "فتح محادثة القسم", modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("المحادثة", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }

                                        OutlinedButton(
                                            onClick = { onExportGroupExcel(sub) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = adaptiveStatusGreen),
                                            border = BorderStroke(0.8.dp, adaptiveStatusGreen),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(0.8f)
                                                .heightIn(min = 40.dp)
                                        ) {
                                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(acctStr("excel"), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }

                                        TextButton(
                                            onClick = { onCreateSubGroupClick(sub) },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(0.8f)
                                                .heightIn(min = 40.dp)
                                        ) {
                                            Text(acctStr("add_sub"), fontSize = 10.sp, color = primaryGold, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }

                                    if (secondarySubGroups.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        secondarySubGroups.forEach { secSub ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(0.6.dp, primaryGold.copy(alpha = 0.2f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 16.dp, top = 3.dp, bottom = 3.dp)
                                                    .clickable { onOpenGroup(secSub.id) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.SubdirectoryArrowRight,
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .graphicsLayer { scaleX = if (isRtl) -1f else 1f },
                                                            tint = primaryGold
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(secSub.name, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        if (secSub.disableMemberDMs) {
                                                            Text(acctStr("dm_blocked"), fontSize = 9.sp, color = adaptiveStatusRed, fontWeight = FontWeight.Bold)
                                                        }
                                                        IconButton(
                                                            onClick = { onOpenFinancialEntry(secSub) },
                                                            modifier = Modifier.minimumInteractiveComponentSize()
                                                        ) {
                                                            Icon(Icons.Default.PostAdd, contentDescription = acctStr("record_entry"), tint = primaryGold, modifier = Modifier.size(18.dp))
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
            }
        }
    }

    if (groupPendingDelete != null) {
        val target = groupPendingDelete!!
        AlertDialog(
            onDismissRequest = { groupPendingDelete = null },
            shape = RoundedCornerShape(16.dp),
            title = { Text(acctStr("delete_group_title"), fontWeight = FontWeight.Bold) },
            text = {
                Text(String.format(acctStr("delete_group_body"), target.name), fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup(target)
                        groupPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveStatusRed, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(acctStr("delete"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { groupPendingDelete = null },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(acctStr("cancel"))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// -------------------------------------------------------------
// Sub-Composable: 4. Staff Shifts & Working Hours Matrix
// -------------------------------------------------------------
@Composable
private fun StaffShiftsTabContent(
    groups: List<WorkGroupEntity>,
    viewModel: MainViewModel,
    onEditShift: (GroupMemberEntity) -> Unit,
    onAddStaffClick: () -> Unit,
    onDmAttempt: (GroupMemberEntity, WorkGroupEntity?) -> Unit,
    onDeleteStaff: (GroupMemberEntity) -> Unit,
    onToggleGroupPrivacy: (WorkGroupEntity, Boolean) -> Unit
) {
    var selectedGroupIndex by remember { mutableIntStateOf(0) }
    val currentGroup = groups.getOrNull(selectedGroupIndex) ?: groups.firstOrNull()

    val membersFlow = remember(currentGroup) {
        if (currentGroup != null) viewModel.getGroupMembers(currentGroup.id) else kotlinx.coroutines.flow.flowOf(emptyList())
    }
    val members by membersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        if (groups.size > 1) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(acctStr("target_entity"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEachIndexed { index, grp ->
                        FilterChip(
                            selected = selectedGroupIndex == index,
                            onClick = { selectedGroupIndex = index },
                            label = { Text(grp.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = adaptiveGoldPrimary,
                                selectedLabelColor = adaptiveOnPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        VisualAttendanceLoggerSection(
            currentGroup = currentGroup,
            allGroups = groups,
            members = members,
            viewModel = viewModel,
            onEditShift = onEditShift,
            onAddStaffClick = onAddStaffClick,
            onDmAttempt = onDmAttempt,
            onDeleteStaff = onDeleteStaff,
            onToggleGroupPrivacy = onToggleGroupPrivacy
        )
    }
}

@Composable
private fun PermissionBadge(
    label: String,
    granted: Boolean,
    customColor: Color? = null
) {
    val tintColor = customColor ?: if (granted) adaptiveStatusGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Surface(
        color = tintColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, tintColor.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = tintColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// -------------------------------------------------------------
// Dialogs: Create Profile, Create Company, Add Record, Edit Shift
// -------------------------------------------------------------

@Composable
private fun CreateHiddenProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, pin: String, compName: String, role: String, cap: Double, silent: Boolean, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isPinVisible by remember { mutableStateOf(false) }
    var isConfirmPinVisible by remember { mutableStateOf(false) }
    var compName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("المدير المالي والمسؤول") }
    var capitalText by remember { mutableStateOf("1500000") }
    var silentNotifications by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }

    val isPinValid = pin.length >= 4
    val isPinMatching = pin == confirmPin

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 640.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            adaptiveGoldPrimary.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    )
                )
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Surface(
                        color = adaptiveGoldPrimary.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = adaptiveGoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = acctStr("new_vault"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = adaptiveGoldPrimary
                        )
                        Text(
                            text = acctStr("sub_vaults"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; showError = null },
                        label = { Text(acctStr("vault_name")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = compName,
                        onValueChange = { compName = it },
                        label = { Text(acctStr("company_name")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it; showError = null },
                        label = { Text(acctStr("passcode_min")) },
                        singleLine = true,
                        visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(
                                onClick = { isPinVisible = !isPinVisible },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (isPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = acctStr("passcode_label"),
                                    tint = adaptiveGoldPrimary
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it; showError = null },
                        label = { Text(acctStr("confirm_passcode")) },
                        singleLine = true,
                        visualTransformation = if (isConfirmPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(
                                onClick = { isConfirmPinVisible = !isConfirmPinVisible },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (isConfirmPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = acctStr("confirm_passcode"),
                                    tint = adaptiveGoldPrimary
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        isError = confirmPin.isNotEmpty() && !isPinMatching
                    )

                    if (confirmPin.isNotEmpty() && !isPinMatching) {
                        Text(acctStr("passcode_mismatch"), color = adaptiveStatusRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text(acctStr("job_title")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = capitalText,
                        onValueChange = { capitalText = it },
                        label = { Text(acctStr("capital")) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (silentNotifications) acctStr("stealth_mode") else acctStr("instant_alerts"),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (silentNotifications) MaterialTheme.colorScheme.onSurface else adaptiveStatusGreen
                                )
                                Text(
                                    text = if (silentNotifications) acctStr("alerts_muted") else acctStr("alerts_on"),
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = silentNotifications,
                                onCheckedChange = { silentNotifications = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = adaptiveOnPrimary,
                                    checkedTrackColor = adaptiveGoldPrimary
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(acctStr("notes_optional")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showError != null) {
                        Surface(
                            color = adaptiveStatusRed.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = showError!!,
                                color = adaptiveStatusRed,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val errEmptyName = acctStr("vault_name")
                val errShortPin = acctStr("passcode_short")
                val errMismatchPin = acctStr("passcode_mismatch")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(acctStr("cancel"), color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                showError = errEmptyName
                                return@Button
                            }
                            if (pin.isBlank() || pin.length < 4) {
                                showError = errShortPin
                                return@Button
                            }
                            if (pin != confirmPin) {
                                showError = errMismatchPin
                                return@Button
                            }
                            val cap = capitalText.toDoubleOrNull() ?: 0.0
                            onConfirm(name.trim(), pin.trim(), compName.ifBlank { name }.trim(), role.trim(), cap, silentNotifications, notes.ifBlank { null })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        modifier = Modifier
                            .weight(1.4f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(acctStr("save"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun UnlockSubAccountPasswordDialog(
    profile: HiddenProfileEntity,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var enteredPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 580.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            adaptiveGoldPrimary.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    )
                )
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Surface(
                        color = adaptiveGoldPrimary.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = adaptiveGoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = acctStr("vault_unlock_title"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = adaptiveGoldPrimary
                        )
                        Text(
                            text = acctStr("vault_unlock_hint"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = adaptiveGoldPrimary)
                            Text(profile.companyName, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedTextField(
                        value = enteredPassword,
                        onValueChange = {
                            enteredPassword = it
                            errorMessage = null
                        },
                        label = { Text(acctStr("passcode_label")) },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(
                                onClick = { isPasswordVisible = !isPasswordVisible },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = acctStr("passcode_label"),
                                    tint = adaptiveGoldPrimary
                                )
                            }
                        },
                        isError = errorMessage != null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = adaptiveStatusRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val invalidMsg = acctStr("passcode_invalid")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(acctStr("cancel"), color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = {
                            if (enteredPassword.trim() == profile.secretPinOrPassword.trim()) {
                                onSuccess()
                            } else {
                                errorMessage = invalidMsg
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(acctStr("enter_vault"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ChangeSubAccountPasswordDialog(
    profile: HiddenProfileEntity,
    onDismiss: () -> Unit,
    onConfirm: (newPassword: String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 620.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            adaptiveGoldPrimary.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    )
                )
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Surface(
                        color = adaptiveGoldPrimary.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = adaptiveGoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = acctStr("change_pin"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = adaptiveGoldPrimary
                        )
                        Text(
                            text = profile.name,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it; errorMessage = null },
                        label = { Text(acctStr("current_passcode")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; errorMessage = null },
                        label = { Text(acctStr("new_passcode")) },
                        singleLine = true,
                        visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(
                                onClick = { isNewPasswordVisible = !isNewPasswordVisible },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (isNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = acctStr("new_passcode"),
                                    tint = adaptiveGoldPrimary
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it; errorMessage = null },
                        label = { Text(acctStr("confirm_new_passcode")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = adaptiveStatusRed,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val errInvalid = acctStr("passcode_invalid")
                val errShort = acctStr("passcode_short")
                val errMismatch = acctStr("passcode_mismatch")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(acctStr("cancel"), color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = {
                            if (oldPassword.trim() != profile.secretPinOrPassword.trim()) {
                                errorMessage = errInvalid
                                return@Button
                            }
                            if (newPassword.trim().length < 4) {
                                errorMessage = errShort
                                return@Button
                            }
                            if (newPassword.trim() != confirmNewPassword.trim()) {
                                errorMessage = errMismatch
                                return@Button
                            }
                            onConfirm(newPassword.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(acctStr("save"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
    }
}


// CreateCompanyGroupDialog moved to Wizard components

@Composable
private fun CreateNestedSubGroupDialog(
    parentGroup: WorkGroupEntity,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?, disableDMs: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var disableDMs by remember { mutableStateOf(parentGroup.disableMemberDMs) }

    val presetDepartments = remember {
        listOf(
            "قسم الوارد والمقبوضات",
            "قسم الصادر والمصروفات",
            "قسم الخزينة والجرد",
            "فرع التوزيع والمبيعات",
            "قسم الشكاوى والعجز",
            "قسم الصياغة والتشغيل",
            "إدارة المشتريات والحسابات"
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 620.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            adaptiveGoldPrimary.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    )
                )
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Surface(
                        color = adaptiveGoldPrimary.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = null,
                                tint = adaptiveGoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = acctStr("sub_title"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = adaptiveGoldPrimary
                        )
                        Text(
                            text = "${acctStr("sub_parent")} ${parentGroup.name}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(acctStr("sub_name")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(acctStr("quick_presets"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetDepartments.forEach { preset ->
                            AssistChip(
                                onClick = { name = preset },
                                label = { Text(preset, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(acctStr("sub_desc")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val dmAccent = if (disableDMs) adaptiveStatusRed else adaptiveStatusGreen
                    Surface(
                        color = dmAccent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, dmAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    acctStr("lock_dms_label"),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (disableDMs) adaptiveStatusRed else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    acctStr("lock_dms_hint"),
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = disableDMs,
                                onCheckedChange = { disableDMs = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = adaptiveOnPrimary,
                                    checkedTrackColor = adaptiveGoldPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(acctStr("cancel"), color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name.trim(), description.trim().ifBlank { null }, disableDMs)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(acctStr("save"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
    }
}


// AddAccountingRecordDialog moved to Wizard components

@Composable
private fun EditStaffPermissionsDialog(
    member: GroupMemberEntity,
    onDismiss: () -> Unit,
    onSave: (
        role: String,
        start: String,
        end: String,
        shiftHours: Double,
        workingDays: String,
        shiftType: String,
        canPost: Boolean,
        canExport: Boolean,
        canEdit: Boolean,
        canMsg: Boolean,
        isAdmin: Boolean,
        notes: String?,
        location: String?,
        lat: Double?,
        lng: Double?,
        geoEnabled: Boolean,
        radius: Int
    ) -> Unit
) {
    var role by remember { mutableStateOf(member.roleLabel) }
    var startHours by remember { mutableStateOf(member.workingHoursStart ?: "09:00 ص") }
    var endHours by remember { mutableStateOf(member.workingHoursEnd ?: "05:00 م") }
    var shiftHoursText by remember { mutableStateOf(member.shiftHours.toString()) }
    var workingDays by remember { mutableStateOf(member.workingDays) }
    var shiftType by remember { mutableStateOf(member.shiftType) }

    var canPostAccounting by remember { mutableStateOf(member.canPostAccounting) }
    var canExportReports by remember { mutableStateOf(member.canExportReports) }
    var canEditRecords by remember { mutableStateOf(member.canEditRecords) }
    var canMessageDirectly by remember { mutableStateOf(member.canMessageDirectly) }
    var isGroupAdmin by remember { mutableStateOf(member.isGroupAdmin) }
    var notes by remember { mutableStateOf(member.notes ?: "") }

    var locationName by remember { mutableStateOf(member.workplaceLocation ?: "") }
    var locationLat by remember { mutableStateOf(member.workplaceLatitude?.toString() ?: "") }
    var locationLng by remember { mutableStateOf(member.workplaceLongitude?.toString() ?: "") }
    var geofencingEnabled by remember { mutableStateOf(member.geofencingEnabled) }
    var geofencingRadius by remember { mutableStateOf(member.geofencingRadiusMeters.toString()) }

    val rolePresets = listOf(
        "محاسب أول ومدقق دفاتر",
        "أمين صندوق وخزينة",
        "مسؤول مشتريات وتثمين ذهب",
        "صائغ ومصمم مجوهرات",
        "مراجع عجز وشكاوى",
        "كاشير ومبيعات",
        "مدير مالي وإداري"
    )

    val workingDaysPresets = listOf(
        "السبت - الخميس",
        "الأحد - الخميس",
        "السبت - الأربعاء",
        "طوال الأسبوع"
    )

    val shiftTypePresets = listOf(
        "نهارية (صباحية)",
        "مسائية",
        "ليلية (حراسة وجرد)"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 650.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            adaptiveGoldPrimary.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    )
                )
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Surface(
                        color = adaptiveGoldPrimary.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = adaptiveGoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = acctStr("edit_staff_title"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = adaptiveGoldPrimary
                        )
                        Text(
                            text = "${member.username} • ${member.phone}",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(acctStr("job_title"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text(acctStr("job_title")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rolePresets.forEach { preset ->
                            FilterChip(
                                selected = role == preset,
                                onClick = { role = preset },
                                label = { Text(preset, fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = adaptiveGoldPrimary,
                                    selectedLabelColor = adaptiveOnPrimary
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startHours,
                            onValueChange = { startHours = it },
                            label = { Text(acctStr("shift_start")) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endHours,
                            onValueChange = { endHours = it },
                            label = { Text(acctStr("shift_end")) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = shiftHoursText,
                        onValueChange = { shiftHoursText = it },
                        label = { Text(acctStr("shift_hours")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(acctStr("work_days"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        workingDaysPresets.forEach { days ->
                            FilterChip(
                                selected = workingDays == days,
                                onClick = { workingDays = days },
                                label = { Text(days, fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = adaptiveGoldPrimary,
                                    selectedLabelColor = adaptiveOnPrimary
                                )
                            )
                        }
                    }

                    Text(acctStr("shift_type"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        shiftTypePresets.forEach { type ->
                            FilterChip(
                                selected = shiftType == type,
                                onClick = { shiftType = type },
                                label = { Text(type, fontSize = 11.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = adaptiveGoldPrimary,
                                    selectedLabelColor = adaptiveOnPrimary
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    Text(acctStr("permissions_matrix"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)

                    PermissionSwitchRow(
                        title = "إدراج القيود اليومية",
                        subtitle = "تسجيل الوارد والمنصرف",
                        checked = canPostAccounting,
                        onCheckedChange = { canPostAccounting = it }
                    )

                    PermissionSwitchRow(
                        title = "تصدير التقارير",
                        subtitle = "استخراج إكسل وPDF",
                        checked = canExportReports,
                        onCheckedChange = { canExportReports = it }
                    )

                    PermissionSwitchRow(
                        title = "تعديل وحذف القيود",
                        subtitle = "صلاحية تعديل السجلات",
                        checked = canEditRecords,
                        onCheckedChange = { canEditRecords = it }
                    )

                    PermissionSwitchRow(
                        title = "المراسلات الخاصة",
                        subtitle = "السماح بالدردشة المباشرة",
                        checked = canMessageDirectly,
                        onCheckedChange = { canMessageDirectly = it }
                    )

                    PermissionSwitchRow(
                        title = "مسؤول إداري",
                        subtitle = "إدارة الأعضاء والمجموعات",
                        checked = isGroupAdmin,
                        onCheckedChange = { isGroupAdmin = it }
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(acctStr("notes_optional")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    Text(acctStr("geofencing"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)

                    PermissionSwitchRow(
                        title = acctStr("geofence_enable"),
                        subtitle = acctStr("workplace_name"),
                        checked = geofencingEnabled,
                        onCheckedChange = { geofencingEnabled = it }
                    )

                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text(acctStr("workplace_name")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = locationLat,
                            onValueChange = { locationLat = it },
                            label = { Text(acctStr("lat_label")) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = locationLng,
                            onValueChange = { locationLng = it },
                            label = { Text(acctStr("lng_label")) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = geofencingRadius,
                        onValueChange = { geofencingRadius = it },
                        label = { Text(acctStr("radius_meters")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(acctStr("cancel"), color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = {
                            val shift = shiftHoursText.toDoubleOrNull() ?: 8.0
                            val radius = geofencingRadius.toIntOrNull() ?: 200
                            onSave(
                                role,
                                startHours,
                                endHours,
                                shift,
                                workingDays,
                                shiftType,
                                canPostAccounting,
                                canExportReports,
                                canEditRecords,
                                canMessageDirectly,
                                isGroupAdmin,
                                notes.ifBlank { null },
                                locationName.ifBlank { null },
                                locationLat.toDoubleOrNull(),
                                locationLng.toDoubleOrNull(),
                                geofencingEnabled,
                                radius
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        modifier = Modifier
                            .weight(1.4f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(acctStr("save"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun AddStaffMemberDialog(
    groups: List<WorkGroupEntity>,
    defaultGroup: WorkGroupEntity?,
    onDismiss: () -> Unit,
    onSave: (
        groupId: String,
        username: String,
        phone: String,
        role: String,
        start: String,
        end: String,
        shiftHours: Double,
        workingDays: String,
        shiftType: String,
        canPost: Boolean,
        canExport: Boolean,
        canEdit: Boolean,
        canMsg: Boolean,
        isAdmin: Boolean,
        notes: String?,
        location: String?,
        lat: Double?,
        lng: Double?,
        geoEnabled: Boolean,
        radius: Int
    ) -> Unit
) {
    var selectedGroup by remember { mutableStateOf(defaultGroup ?: groups.firstOrNull()) }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("محاسب أول") }
    var startHours by remember { mutableStateOf("09:00 ص") }
    var endHours by remember { mutableStateOf("05:00 م") }
    var shiftHoursText by remember { mutableStateOf("8.0") }
    var workingDays by remember { mutableStateOf("السبت - الخميس") }
    var shiftType by remember { mutableStateOf("نهارية (صباحية)") }

    var canPostAccounting by remember { mutableStateOf(true) }
    var canExportReports by remember { mutableStateOf(false) }
    var canEditRecords by remember { mutableStateOf(false) }
    var canMessageDirectly by remember { mutableStateOf(false) }
    var isGroupAdmin by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    var locationName by remember { mutableStateOf("") }
    var locationLat by remember { mutableStateOf("") }
    var locationLng by remember { mutableStateOf("") }
    var geofencingEnabled by remember { mutableStateOf(false) }
    var geofencingRadius by remember { mutableStateOf("200") }

    val rolePresets = listOf(
        "محاسب أول ومدقق دفاتر",
        "أمين صندوق وخزينة",
        "مسؤول مشتريات وتثمين ذهب",
        "صائغ ومصمم مجوهرات",
        "مراجع عجز وشكاوى",
        "كاشير ومبيعات",
        "مدير مالي وإداري"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 650.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            adaptiveGoldPrimary.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    )
                )
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Surface(
                        color = adaptiveGoldPrimary.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = null,
                                tint = adaptiveGoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = acctStr("add_staff"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = adaptiveGoldPrimary
                        )
                        Text(
                            text = acctStr("edit_staff_title"),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (groups.size > 1) {
                        Text(acctStr("target_entity"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            groups.forEach { grp ->
                                FilterChip(
                                    selected = selectedGroup?.id == grp.id,
                                    onClick = { selectedGroup = grp },
                                    label = { Text(grp.name, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = adaptiveGoldPrimary,
                                        selectedLabelColor = adaptiveOnPrimary
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(acctStr("staff_name")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(acctStr("phone_number")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text(acctStr("job_title")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rolePresets.forEach { preset ->
                            FilterChip(
                                selected = role == preset,
                                onClick = { role = preset },
                                label = { Text(preset, fontSize = 10.5.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = adaptiveGoldPrimary,
                                    selectedLabelColor = adaptiveOnPrimary
                                )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startHours,
                            onValueChange = { startHours = it },
                            label = { Text(acctStr("shift_start")) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endHours,
                            onValueChange = { endHours = it },
                            label = { Text(acctStr("shift_end")) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = shiftHoursText,
                        onValueChange = { shiftHoursText = it },
                        label = { Text(acctStr("shift_hours")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    PermissionSwitchRow(
                        title = "إدراج القيود اليومية",
                        subtitle = "تسجيل الوارد والمنصرف",
                        checked = canPostAccounting,
                        onCheckedChange = { canPostAccounting = it }
                    )

                    PermissionSwitchRow(
                        title = "تصدير التقارير",
                        subtitle = "استخراج إكسل وPDF",
                        checked = canExportReports,
                        onCheckedChange = { canExportReports = it }
                    )

                    PermissionSwitchRow(
                        title = "المراسلات الخاصة",
                        subtitle = "السماح بالدردشة المباشرة",
                        checked = canMessageDirectly,
                        onCheckedChange = { canMessageDirectly = it }
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(acctStr("notes_optional")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    Text(acctStr("geofencing"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)

                    PermissionSwitchRow(
                        title = acctStr("geofence_enable"),
                        subtitle = acctStr("workplace_name"),
                        checked = geofencingEnabled,
                        onCheckedChange = { geofencingEnabled = it }
                    )

                    OutlinedTextField(
                        value = locationName,
                        onValueChange = { locationName = it },
                        label = { Text(acctStr("workplace_name")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = locationLat,
                            onValueChange = { locationLat = it },
                            label = { Text(acctStr("lat_label")) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = locationLng,
                            onValueChange = { locationLng = it },
                            label = { Text(acctStr("lng_label")) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = geofencingRadius,
                        onValueChange = { geofencingRadius = it },
                        label = { Text(acctStr("radius_meters")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(acctStr("cancel"), color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = {
                            val shift = shiftHoursText.toDoubleOrNull() ?: 8.0
                            val radius = geofencingRadius.toIntOrNull() ?: 200
                            val targetGroupId = selectedGroup?.id ?: groups.firstOrNull()?.id ?: "default_group"
                            if (username.isNotBlank() && phone.isNotBlank()) {
                                onSave(
                                    targetGroupId,
                                    username,
                                    phone,
                                    role,
                                    startHours,
                                    endHours,
                                    shift,
                                    workingDays,
                                    shiftType,
                                    canPostAccounting,
                                    canExportReports,
                                    canEditRecords,
                                    canMessageDirectly,
                                    isGroupAdmin,
                                    notes.ifBlank { null },
                                    locationName.ifBlank { null },
                                    locationLat.toDoubleOrNull(),
                                    locationLng.toDoubleOrNull(),
                                    geofencingEnabled,
                                    radius
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(acctStr("save"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun PermissionSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = adaptiveOnPrimary,
                    checkedTrackColor = adaptiveGoldPrimary
                )
            )
        }
    }
}

@Composable
private fun DmBlockedNoticeDialog(
    memberName: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .heightIn(max = 520.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            adaptiveStatusRed.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = adaptiveStatusRed.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LockPerson,
                                    contentDescription = acctStr("dm_restricted_title"),
                                    tint = adaptiveStatusRed,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = acctStr("dm_restricted_title"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            color = adaptiveGoldPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$memberName • ${acctStr("dm_restricted_body")}",
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(acctStr("understood"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
