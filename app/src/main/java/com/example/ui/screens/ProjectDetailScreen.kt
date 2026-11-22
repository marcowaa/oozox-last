package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GroupMemberEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectMemberRole
import com.example.data.model.ProjectTaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.WorkGroupEntity
import com.example.ui.components.DeleteProjectConfirmationDialog
import com.example.ui.components.DeleteRecordType
import com.example.ui.components.ProjectDetailSkeleton
import com.example.ui.components.RecordDeleteConfirmationDialog
import com.example.ui.components.RecordDetailItem
import com.example.ui.components.SingleProjectBudgetVisualizer
import com.example.ui.components.ToastNotificationBus
import com.example.ui.components.AppToastType
import com.example.ui.components.CreatePhysicalFieldTaskDialog
import com.example.ui.components.WorkerSubmitProofDialog
import com.example.ui.components.AdminVerifyTaskDialog
import com.example.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.GoldUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.PrimaryScrollableTabRow
import com.example.data.model.ShiftHandoverEntity
import com.example.ui.components.CreateProjectShiftHandoverDialog
import com.example.ui.components.ProjectAccountingEntryDialog
import com.example.ui.components.ProjectDetailSideDrawerContent
import com.example.ui.components.ShiftCustodyHandoverDialog
import com.example.util.ProjectIndustryCatalog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    project: ProjectEntity?,
    workGroup: WorkGroupEntity?,
    members: List<GroupMemberEntity>,
    projectMembers: List<com.example.data.model.ProjectMemberEntity> = emptyList(),
    projectAuditLogs: List<com.example.data.model.ProjectAuditLogEntity> = emptyList(),
    projectTasks: List<ProjectTaskEntity> = emptyList(),
    shiftHandovers: List<ShiftHandoverEntity> = emptyList(),
    onLoadExcelSheet: (suspend (String, String?) -> Pair<com.example.util.SmartSpreadsheetTemplate, MutableList<com.example.util.SmartSpreadsheetRow>>)? = null,
    onSaveExcelSheet: ((String, com.example.util.SmartSpreadsheetTemplate, List<com.example.util.SmartSpreadsheetRow>) -> Unit)? = null,
    onAddProjectMember: (name: String, phone: String, role: com.example.data.model.ProjectMemberRole, task: String) -> Unit = { _, _, _, _ -> },
    onPromoteToManager: (com.example.data.model.ProjectMemberEntity) -> Unit = {},
    onAddAccountingEntry: (title: String, amount: Double, isIncome: Boolean, accountCategory: String) -> Unit = { _, _, _, _ -> },
    onAddShiftHandover: (ShiftHandoverEntity) -> Unit = {},
    onReceiveShiftHandoverFull: (
        handoverId: String,
        receiverId: String,
        receiverName: String,
        notes: String?,
        merchandiseStatus: String,
        physicalStockChecked: Boolean,
        cashDrawerConfirmed: Boolean,
        invoicesAuditedCount: Int,
        receiverSignature: String?,
        acceptanceType: String,
        goodsDiscrepancyNotes: String?
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    onAddTask: (title: String, desc: String?, assignedToName: String?, priority: String, dueDate: String?, cost: Double) -> Unit = { _, _, _, _, _, _ -> },
    onAddFieldTask: (ProjectTaskEntity) -> Unit = {},
    onSubmitTaskProof: (taskId: String, proofUri: String?, proofType: String, notes: String, userId: String?, userName: String?) -> Unit = { _, _, _, _, _, _ -> },
    onConfirmAndArchiveTask: (taskId: String, financialAmount: Double, notes: String?) -> Unit = { _, _, _ -> },
    onUpdateTaskStatus: (taskId: String, status: String) -> Unit = { _, _ -> },
    onDeleteTask: (taskId: String) -> Unit = {},
    onDeleteProject: (projectId: String) -> Unit = {},
    onBackClick: () -> Unit
) {
    if (project == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("تفاصيل المشروع...", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = GoldPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                ProjectDetailSkeleton()
            }
        }
        return
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showRbacDialog by remember { mutableStateOf(false) }
    var showShareModal by remember { mutableStateOf(false) }
    var showExcelStudio by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showCreateFieldTaskDialog by remember { mutableStateOf(false) }
    var taskToSubmitProof by remember { mutableStateOf<ProjectTaskEntity?>(null) }
    var taskToVerifyAndArchive by remember { mutableStateOf<ProjectTaskEntity?>(null) }
    var showDeleteProjectConfirmDialog by remember { mutableStateOf(false) }
    var taskPendingDelete by remember { mutableStateOf<ProjectTaskEntity?>(null) }

    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDesc by remember { mutableStateOf("") }
    var newTaskAssignee by remember { mutableStateOf("") }
    var newTaskPriority by remember { mutableStateOf(TaskPriority.MEDIUM.name) }
    var newTaskDueDate by remember { mutableStateOf("") }
    var newTaskCost by remember { mutableStateOf("") }

    val projectLink = "https://oozox.app/projects/${project.id}"
    val shareContent = remember(project) {
        buildString {
            appendLine("🏗️ مشروع: ${project.name} (${project.type})")
            appendLine("💰 رأس المال التقديري: ${GoldUtils.formatCurrency(project.estimatedCapital)}")
            appendLine("📈 إجمالي الإيرادات: ${GoldUtils.formatCurrency(project.totalIncome)}")
            appendLine("📉 إجمالي المصروفات: ${GoldUtils.formatCurrency(project.totalExpenses)}")
            val netBalance = project.totalIncome - project.totalExpenses
            appendLine("💵 صافي الرصيد: ${GoldUtils.formatCurrency(netBalance)}")
            if (project.location.isNotBlank()) {
                appendLine("📍 الموقع: ${project.location}")
            }
            if (!project.coManagerName.isNullOrBlank()) {
                appendLine("👤 الشريك الإداري: ${project.coManagerName}")
            }
            appendLine("🔗 رابط المشروع: $projectLink")
        }
    }

    val triggerNativeShare = {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_SUBJECT, "تفاصيل مشروع: ${project.name}")
                putExtra(Intent.EXTRA_TEXT, shareContent)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "مشاركة تفاصيل المشروع عبر:")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح نافذة المشاركة", Toast.LENGTH_SHORT).show()
        }
    }

    var showAccountingEntryDialog by remember { mutableStateOf(false) }
    var accountingInitialTitle by remember { mutableStateOf("") }
    var accountingInitialIsIncome by remember { mutableStateOf(false) }
    var showCreateShiftHandoverDialog by remember { mutableStateOf(false) }
    var handoverPendingReceive by remember { mutableStateOf<ShiftHandoverEntity?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val accountingProfile = remember(project.type) {
        ProjectIndustryCatalog.resolveAccountingProfile(project.type)
    }
    val displayTypeLabel = remember(project.type) {
        ProjectIndustryCatalog.getDisplayLabel(project.type)
    }
    val projectShiftHandovers = remember(shiftHandovers, project.name) {
        val projectTagged = shiftHandovers.filter { it.notes?.contains("[مشروع: ${project.name}]") == true }
        projectTagged.ifEmpty { shiftHandovers }
    }

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ProjectDetailSideDrawerContent(
                project = project,
                profile = accountingProfile,
                onSelectTab = { selectedTabIndex = it },
                onAssignFieldTask = { showCreateFieldTaskDialog = true },
                onOpenSmartExcel = { showExcelStudio = true },
                onOpenRbacAndPromotion = { showRbacDialog = true },
                onExportPdf = {
                    com.example.util.ReportExporter.exportProjectSummaryToPdf(
                        context = context,
                        project = project,
                        tasks = projectTasks,
                        members = null
                    )
                },
                onShareProject = { showShareModal = true },
                onTriggerSync = { com.example.util.OfflineSyncManager.triggerImmediateSync(context) },
                onDeleteProject = { showDeleteProjectConfirmDialog = true },
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = project.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "$displayTypeLabel • ${accountingProfile.sectorNameAr}",
                                color = GoldPrimary,
                                fontSize = 10.5.sp,
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("project_detail_drawer_btn")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "قائمة أدوات المشروع", tint = adaptiveGoldPrimary)
                            }
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = adaptiveGoldPrimary)
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showRbacDialog = true },
                            modifier = Modifier.testTag("project_detail_rbac_btn")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "الفريق والصلاحيات", tint = GoldPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val tabTitles = listOf("نظرة عامة", "محاسبة المشروع", "المهام والتوثيق", "تسليم الشفتات")
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = GoldPrimary,
                    edgePadding = 12.dp
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("project_detail_tab_$index")
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            if (selectedTabIndex == 0) {
            // Offline Persistence Status Bar
            item {
                val isNetworkOnline by com.example.util.OfflineSyncManager.isNetworkOnline.collectAsStateWithLifecycle()
                val isSyncing by com.example.util.OfflineSyncManager.isSyncing.collectAsStateWithLifecycle()

                Surface(
                    color = if (isNetworkOnline) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else Color(0xFFD97706).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isNetworkOnline) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color(0xFFD97706).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            com.example.util.OfflineSyncManager.triggerImmediateSync(context)
                        }
                        .testTag("project_detail_offline_cache_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isNetworkOnline) Icons.Default.Storage else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isNetworkOnline) GoldPrimary else Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isNetworkOnline) "بيانات هذا المشروع محفوظة ومتاحة أوفلاين" else "وضع عدم الاتصال: تُحفظ التغييرات محلياً حتى تتوفر الشبكة",
                                fontSize = 11.5.sp,
                                color = if (isNetworkOnline) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFD97706),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSyncing) "مزامنة..." else "مزامنة ⟳",
                            fontSize = 11.sp,
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Capital Overview Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ملخص مالية المشروع", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                            Surface(color = GoldPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text(project.type, fontSize = 11.sp, color = GoldAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("رأس المال: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(GoldUtils.formatCurrency(project.estimatedCapital), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("المصروفات: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(GoldUtils.formatCurrency(project.totalExpenses), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StatusOrange)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("الإيرادات: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(GoldUtils.formatCurrency(project.totalIncome), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val net = project.totalIncome - project.totalExpenses
                                Text("الرصيد: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(GoldUtils.formatCurrency(net), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (net >= 0) StatusGreen else StatusRed)
                            }
                        }
                    }
                }
            }

            // Quick Assign Task Card (إسناد مهمة مع منشن)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateFieldTaskDialog = true }
                        .testTag("overview_assign_field_task_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AssignmentInd,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "إسناد مهمة للموظف (@منشن)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تحديد المكلف والمدة ومشاركة المهمة في مجموعة العمل",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                        Button(
                            onClick = { showCreateFieldTaskDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("إسناد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Excel & Data Table Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showExcelStudio = true }
                        .testTag("open_excel_studio_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.TableChart,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "جدول البيانات والتحليل المالي",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "تقارير مالية وتتبع التكاليف",
                                        fontSize = 11.sp,
                                        color = GoldPrimary
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF10B981)
                            )
                        }

                        Button(
                            onClick = { showExcelStudio = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("عرض جدول البيانات 📊", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Interactive Budget & Expense Visualizer
            item {
                SingleProjectBudgetVisualizer(project = project)
            }

            // Budget Exceeded Alert History View
            item {
                ProjectBudgetAlertHistoryCard(
                    project = project,
                    auditLogs = projectAuditLogs
                )
            }

            // Location & Management Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("موقع وإدارة المشروع", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("الموقع: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(project.location, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        if (!project.coManagerName.isNullOrBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("الشريك: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${project.coManagerName} (${project.coManagerPhone ?: ""})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                            }
                        }
                    }
                }
            }

            // Mentions & Tasks Assigned
            if (project.membersInfo.isNotBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("المينشن وتوزيع المهام للمشاركين", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)

                            val splitMembers = project.membersInfo.split(" | ")
                            splitMembers.forEach { memItem ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AssignmentInd, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(memItem, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Sector KPIs Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("مؤشرات الأداء المخصصة للقطاع:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            accountingProfile.kpiLabels.forEach { kpi ->
                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = kpi,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GoldPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            } // End Tab 0 (Overview)

            // Tab 1: Tailored Project Accounting
            if (selectedTabIndex == 1) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(accountingProfile.sectorNameAr, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                    Text(
                                        text = if (accountingProfile.tracksPhysicalGoods) "نظام محاسبي يدعم المخزون والبضائع والعهدة" else "نظام محاسبي للمشاريع والخدمات والعقود",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        accountingInitialTitle = ""
                                        accountingInitialIsIncome = false
                                        showAccountingEntryDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.background),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("add_accounting_entry_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("قيد جديد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Text("قوالب الإدخال السريع لهذا النشاط:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                accountingProfile.quickEntryTemplates.forEach { (label, type) ->
                                    val isInc = type == "INCOME"
                                    AssistChip(
                                        onClick = {
                                            accountingInitialTitle = label
                                            accountingInitialIsIncome = isInc
                                            showAccountingEntryDialog = true
                                        },
                                        label = { Text(label, fontSize = 11.sp) },
                                        leadingIcon = {
                                            Icon(
                                                if (isInc) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                contentDescription = null,
                                                tint = if (isInc) StatusGreen else StatusOrange,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }

                            Text("حسابات الإيرادات المعتمدة:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                            Text(accountingProfile.revenueAccounts.joinToString(" • "), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Text("حسابات المصروفات والتكاليف:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = StatusOrange)
                            Text(accountingProfile.expenseAccounts.joinToString(" • "), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } // End Tab 1

            if (selectedTabIndex == 2) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Checklist, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مهام وبنود التنفيذ (مزامنة Offline)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showCreateFieldTaskDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("create_field_task_btn")
                        ) {
                            Icon(Icons.Default.AssignmentInd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إسناد مهمة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showAddTaskDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = adaptiveOnPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("بند سريع", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = adaptiveOnPrimary)
                        }
                    }
                }
            }

            if (projectTasks.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("لا توجد مهام مسجلة لهذا المشروع حالياً", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(projectTasks, key = { it.id }) { task ->
                    val isDone = task.status == TaskStatus.COMPLETED.name
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDone) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDone) Color(0xFF4CAF50).copy(alpha = 0.4f) else GoldPrimary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isDone,
                                        onCheckedChange = { checked ->
                                            val newStatus = if (checked) TaskStatus.COMPLETED.name else TaskStatus.IN_PROGRESS.name
                                            onUpdateTaskStatus(task.id, newStatus)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF4CAF50),
                                            uncheckedColor = GoldPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = task.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                            textDecoration = if (isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                        )
                                        if (!task.description.isNullOrBlank()) {
                                            Text(
                                                text = task.description,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { taskPendingDelete = task },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("delete_task_btn_${task.id}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف المهمة", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    // Status Badge
                                    Surface(
                                        color = if (isDone) Color(0xFF4CAF50).copy(alpha = 0.15f) else GoldPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isDone) "مكتملة ✅" else if (task.status == TaskStatus.IN_PROGRESS.name) "قيد التنفيذ ⏳" else "قيد الانتظار 📌",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDone) Color(0xFF4CAF50) else GoldAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Priority Badge
                                    Surface(
                                        color = when (task.priority) {
                                            TaskPriority.URGENT.name -> Color.Red.copy(alpha = 0.15f)
                                            TaskPriority.HIGH.name -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = when (task.priority) {
                                                TaskPriority.URGENT.name -> "عاجل جداً"
                                                TaskPriority.HIGH.name -> "عالي"
                                                TaskPriority.LOW.name -> "منخفض"
                                                else -> "متوسط"
                                            },
                                            fontSize = 10.sp,
                                            color = when (task.priority) {
                                                TaskPriority.URGENT.name -> Color.Red
                                                TaskPriority.HIGH.name -> Color(0xFFFF9800)
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Offline / Sync Status Badge
                                    Surface(
                                        color = if (task.isSynced) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFD97706).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (task.isSynced) "مزامَن ☁️" else "محلي ⏱️",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (task.isSynced) Color(0xFF4CAF50) else Color(0xFFD97706),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (task.costEstimate > 0) {
                                    Text(
                                        text = "الميزانية: ${GoldUtils.formatCurrency(task.costEstimate)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                }
                            }

                            if (!task.assignedToName.isNullOrBlank() || !task.dueDate.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (!task.assignedToName.isNullOrBlank()) {
                                        Text("المكلف: ${task.assignedToName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (!task.dueDate.isNullOrBlank()) {
                                        Text("تاريخ التسليم: ${task.dueDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // Physical Field Operations Details & Escalating Reminders
                            if (task.mediaAttachmentUri != null || task.deadlineTimestamp > 0 || task.assignedMembersList.isNotBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                if (task.assignedMembersList.isNotBlank()) {
                                    Text("👥 المكلفون بالمنشن: ${task.assignedMembersList}", fontSize = 11.sp, color = GoldAccent, fontWeight = FontWeight.Bold)
                                }

                                if (task.mediaAttachmentUri != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            if (task.mediaAttachmentType == "VIDEO") Icons.Default.Videocam else Icons.Default.PhotoCamera,
                                            contentDescription = null,
                                            tint = if (task.mediaAttachmentType == "VIDEO") Color(0xFFEF4444) else Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (task.mediaAttachmentType == "VIDEO") "مرفق فيديو موقع العمل المطلوب من الإدارة 🎥" else "مرفق صورة موقع العمل المطلوب من الإدارة 📷",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Escalating Reminders / Urgency Alert
                                if (task.deadlineTimestamp > 0 && !isDone && !task.isArchived) {
                                    val now = System.currentTimeMillis()
                                    val diffMs = task.deadlineTimestamp - now
                                    val isOverdue = diffMs <= 0
                                    val isVeryUrgent = diffMs in 1..(3600 * 1000)

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isOverdue) Color.Red.copy(alpha = 0.15f) else if (isVeryUrgent) Color(0xFFF59E0B).copy(alpha = 0.15f) else GoldPrimary.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, if (isOverdue) Color.Red else if (isVeryUrgent) Color(0xFFF59E0B) else GoldPrimary.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                if (isOverdue || isVeryUrgent) Icons.Default.NotificationsActive else Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = if (isOverdue) Color.Red else if (isVeryUrgent) Color(0xFFF59E0B) else GoldPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            val totalHoursRem = diffMs / 3600000L
                                            val daysRem = totalHoursRem / 24L
                                            val remHoursPart = totalHoursRem % 24L
                                            val remainingLabel = when {
                                                daysRem >= 1L -> "${daysRem} يوم و ${remHoursPart} س"
                                                else -> "${totalHoursRem} س و ${(diffMs % 3600000L) / 60000L} د"
                                            }
                                            Text(
                                                text = if (isOverdue) "🛑 تجاوزت المهمة موعد التسليم المحدد!"
                                                else if (isVeryUrgent) "🚨 تنبيه متصاعد: اقترب موعد التسليم جداً (${diffMs / 60000} دقيقة متبقية)!"
                                                else "⏳ المدة: ${task.deadlineDurationText} (متبقي $remainingLabel)",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOverdue) Color.Red else if (isVeryUrgent) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                if (task.isSubmittedForReview) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("✓ قام المكلف برفع إثبات الإنجاز الميداني وبانتظار فحص وتأكيد الأدمن", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                            if (!task.workerResponseText.isNullOrBlank()) {
                                                Text("إفادة الإنجاز: ${task.workerResponseText}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }

                                if (task.isArchived) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF3B82F6).copy(alpha = 0.12f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.Archive, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                            Text("تم تأكيد الإنجاز بنجاح وأرشفة المهمة بالسجلات الرسمية ✓", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                                        }
                                    }
                                }

                                // Field Task Action Buttons
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!task.isArchived && !isDone) {
                                        Button(
                                            onClick = { taskToSubmitProof = task },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("توثيق الرد والإنجاز 📸", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (task.isSubmittedForReview && !task.isArchived) {
                                        Button(
                                            onClick = { taskToVerifyAndArchive = task },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("فحص وتأكيد الأرشفة 📋", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Workgroup Members List
            item {
                Text("فريق العمل والأعضاء المسجلين", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
            }

            items(members, key = { it.id }) { m ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(m.username, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(m.roleLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Audit Trail Log Section
            if (projectAuditLogs.isNotEmpty()) {
                item {
                    Text("سجل التدقيق الرقابي والمتابعة 📜", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                }

                items(projectAuditLogs, key = { it.id }) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(log.action, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                Text(GoldUtils.formatShortTime(log.timestamp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("بواسطة: ${log.actorName} (${log.actorPhone})", fontSize = 11.sp, color = GoldAccent)
                            if (!log.details.isNullOrBlank()) {
                                Text(log.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            } // End Tab 2

            // Tab 3: Smart Shift Handover (Financials & Physical Goods)
            if (selectedTabIndex == 3) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("تسليم الشفتات والعهدة", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                    Text("توثيق الماليات (كاش، إنستاباي، فودافون كاش) وجرد البضائع", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { showCreateShiftHandoverDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.background),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("new_shift_handover_btn")
                                ) {
                                    Icon(Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تسليم شفت", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Text("قائمة التحقق المعتمدة لهذا القطاع:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            accountingProfile.shiftHandoverChecklist.forEach { checkItem ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                                    Text(checkItem, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                if (projectShiftHandovers.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(32.dp))
                                Text("لا توجد محاضر تسليم شفت مسجلة بعد", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("استخدم زر (تسليم شفت) لتوثيق الجرد المالي والسلعي.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(projectShiftHandovers, key = { it.id }) { handover ->
                        val isReceived = handover.isReceived || !handover.receivedByCashierName.isNullOrBlank()
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isReceived) StatusGreen.copy(alpha = 0.4f) else GoldPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("المسلّم: ${handover.cashierName}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    Surface(
                                        color = if (isReceived) adaptiveStatusGreen.copy(alpha = 0.14f) else adaptiveStatusOrange.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isReceived) "تم الاستلام والمطابقة ✓" else "بانتظار الاستلام ⏳",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isReceived) adaptiveStatusGreen else adaptiveStatusOrange,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("كاش: ${GoldUtils.formatCurrency(handover.cashCollected)}", fontSize = 11.sp)
                                    Text("إنستاباي: ${GoldUtils.formatCurrency(handover.instaPayCollected)}", fontSize = 11.sp)
                                    Text("فودافون: ${GoldUtils.formatCurrency(handover.vodafoneCashCollected)}", fontSize = 11.sp)
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الإجمالي الفعلي: ${GoldUtils.formatCurrency(handover.actualCash)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                                    if (handover.deficitAmount > 0) {
                                        Text("عجز: ${GoldUtils.formatCurrency(handover.deficitAmount)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = adaptiveStatusRed)
                                    } else if (handover.surplusAmount > 0) {
                                        Text("فائض: ${GoldUtils.formatCurrency(handover.surplusAmount)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = adaptiveStatusGreen)
                                    }
                                }

                                if (handover.productsAddedCount > 0 || handover.productsSoldCount > 0) {
                                    Text(
                                        text = "📦 حركة البضائع: مضاف (${handover.productsAddedCount}) • منصرف/مباع (${handover.productsSoldCount})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (!isReceived) {
                                    Button(
                                        onClick = { handoverPendingReceive = handover },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669), contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("استلام وفحص العهدة والماليات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(
                                        text = "تم الاستلام بواسطة: ${handover.receivedByCashierName ?: "المستلم"}",
                                        fontSize = 11.sp,
                                        color = adaptiveStatusGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            } // End Tab 3
                }
            }
        }
    }

    if (showCreateFieldTaskDialog) {
        val groupMemberLabels = members.map {
            val phonePart = if (it.phone.isNotBlank()) " (${it.phone})" else ""
            "@${it.username}$phonePart - ${it.roleLabel}"
        }
        val projectMemberLabels = projectMembers.map {
            val phonePart = if (it.userPhone.isNotBlank()) " (${it.userPhone})" else ""
            val roleText = ProjectMemberRole.entries.firstOrNull { r -> r.name == it.role }?.label ?: it.assignedTask ?: "عضو"
            "@${it.userName}$phonePart - $roleText"
        }
        val infoLabels = project.membersInfo
            .split("|")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { if (it.startsWith("@")) it else "@$it" }
        val coManagerLabel = project.coManagerName?.takeIf { it.isNotBlank() }?.let { name ->
            val phonePart = project.coManagerPhone?.takeIf { p -> p.isNotBlank() }?.let { " ($it)" } ?: ""
            listOf("@$name$phonePart - المدير التنفيذي المشارك")
        } ?: emptyList()

        val combinedMemberNames = (groupMemberLabels + projectMemberLabels + coManagerLabel + infoLabels)
            .distinctBy { it.substringBefore(" - ").substringBefore(" (").trim().removePrefix("@") }

        CreatePhysicalFieldTaskDialog(
            projectId = project.id,
            projectName = project.name,
            workGroupId = project.workGroupId,
            availableMembers = combinedMemberNames,
            onDismiss = { showCreateFieldTaskDialog = false },
            onCreateTask = { task ->
                onAddFieldTask(task)
                showCreateFieldTaskDialog = false
            }
        )
    }

    if (showExcelStudio) {
        com.example.ui.components.ExcelStudioDialog(
            project = project,
            onLoadSheet = onLoadExcelSheet,
            onSaveSheet = onSaveExcelSheet,
            onDismiss = { showExcelStudio = false }
        )
    }

    taskToSubmitProof?.let { task ->
        WorkerSubmitProofDialog(
            task = task,
            currentUserName = members.firstOrNull()?.username ?: "الموظف المكلف",
            currentUserId = members.firstOrNull()?.userId ?: "worker_current",
            onDismiss = { taskToSubmitProof = null },
            onSubmitProof = { uri, type, notes, uid, uname ->
                onSubmitTaskProof(task.id, uri, type, notes, uid, uname)
                taskToSubmitProof = null
            }
        )
    }

    taskToVerifyAndArchive?.let { task ->
        AdminVerifyTaskDialog(
            task = task,
            onDismiss = { taskToVerifyAndArchive = null },
            onConfirmAndArchive = { amount, notes ->
                onConfirmAndArchiveTask(task.id, amount, notes)
                taskToVerifyAndArchive = null
            }
        )
    }

    if (showRbacDialog) {
        com.example.ui.components.ProjectRbacDialog(
            projectId = project.id,
            members = projectMembers,
            onAddMember = { name, phone, role, task ->
                onAddProjectMember(name, phone, role, task)
            },
            onPromoteToManager = onPromoteToManager,
            onDismiss = { showRbacDialog = false }
        )
    }

    if (showAccountingEntryDialog) {
        ProjectAccountingEntryDialog(
            profile = accountingProfile,
            initialTitle = accountingInitialTitle,
            initialIsIncome = accountingInitialIsIncome,
            onDismiss = { showAccountingEntryDialog = false },
            onConfirmEntry = { title, amount, isIncome, category ->
                onAddAccountingEntry(title, amount, isIncome, category)
            }
        )
    }

    if (showCreateShiftHandoverDialog) {
        CreateProjectShiftHandoverDialog(
            project = project,
            profile = accountingProfile,
            onDismiss = { showCreateShiftHandoverDialog = false },
            onCreateHandover = { handover ->
                onAddShiftHandover(handover)
            }
        )
    }

    handoverPendingReceive?.let { handover ->
        ShiftCustodyHandoverDialog(
            handover = handover,
            currentStaffId = members.firstOrNull()?.userId ?: "manager_current",
            currentStaffName = members.firstOrNull()?.username ?: "المدير المستلم",
            onConfirmReceiving = { hId, rId, rName, notes, merchStatus, stockChecked, cashConfirmed, invCount, sig, acceptType, discNotes ->
                onReceiveShiftHandoverFull(
                    hId, rId, rName, notes, merchStatus, stockChecked, cashConfirmed, invCount, sig, acceptType, discNotes
                )
                handoverPendingReceive = null
            },
            onDismiss = { handoverPendingReceive = null }
        )
    }

    if (showShareModal) {
        ModalBottomSheet(
            onDismissRequest = { showShareModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "مشاركة المشروع والتقارير",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { showShareModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Project Preview Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(project.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GoldPrimary)
                            Surface(color = GoldPrimary.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text(project.type, fontSize = 11.sp, color = GoldAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }

                        Text(
                            text = "رأس المال: ${GoldUtils.formatCurrency(project.estimatedCapital)} • صافي الرصيد: ${GoldUtils.formatCurrency(project.totalIncome - project.totalExpenses)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Link Container
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = projectLink,
                                    fontSize = 12.sp,
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(projectLink))
                                        Toast.makeText(context, "تم نسخ رابط المشروع بنجاح 📋", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "نسخ الرابط",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Primary Sharing Action (Native Sheet)
                Button(
                    onClick = {
                        showShareModal = false
                        triggerNativeShare()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = adaptiveOnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "مشاركة مباشرة عبر تطبيقات التواصل",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = adaptiveOnPrimary
                    )
                }

                // Secondary Copy Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(projectLink))
                            Toast.makeText(context, "تم نسخ الرابط 📋", Toast.LENGTH_SHORT).show()
                            showShareModal = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ الرابط", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(shareContent))
                            Toast.makeText(context, "تم نسخ ملخص المشروع بالكامل 📋", Toast.LENGTH_SHORT).show()
                            showShareModal = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ الملخص", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        var taskTimingMode by remember { mutableStateOf("DAYS") } // NOW, HOURS, DAYS
        var taskHoursText by remember { mutableStateOf("6") }
        var taskDaysText by remember { mutableStateOf("3") }
        val scrollStateDialog = rememberScrollState()

        val calculatedDurationMs: Long = when (taskTimingMode) {
            "NOW" -> 15L * 60L * 1000L
            "HOURS" -> ((taskHoursText.toIntOrNull() ?: 1).coerceIn(1, 720)).toLong() * 3600L * 1000L
            else -> ((taskDaysText.toIntOrNull() ?: 1).coerceIn(1, 365)).toLong() * 24L * 3600L * 1000L
        }
        val calculatedDurationNum: Int = when (taskTimingMode) {
            "NOW" -> 0
            "HOURS" -> (taskHoursText.toIntOrNull() ?: 1).coerceIn(1, 720)
            else -> (taskDaysText.toIntOrNull() ?: 1).coerceIn(1, 365)
        }
        val calculatedDurationText: String = when (taskTimingMode) {
            "NOW" -> com.example.util.tr("task.mode_now")
            "HOURS" -> "$calculatedDurationNum ساعة"
            else -> "$calculatedDurationNum يوم"
        }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddTask, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إسناد مهمة جديدة للمشروع", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollStateDialog)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("عنوان المهمة / البند") },
                        placeholder = { Text("توريد خامات / مراجعة هندسية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTaskDesc,
                        onValueChange = { newTaskDesc = it },
                        label = { Text("تفاصيل ومواصفات التنفيذ (اختياري)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTaskAssignee,
                        onValueChange = { newTaskAssignee = it },
                        label = { Text("اسم المكلف / الموظف المسؤول") },
                        placeholder = { Text("م. أحمد") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = com.example.util.tr("task.timing_mode_label"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "NOW" to com.example.util.tr("task.mode_now"),
                            "HOURS" to com.example.util.tr("task.mode_hours"),
                            "DAYS" to com.example.util.tr("task.mode_days")
                        ).forEach { (mKey, mLabel) ->
                            FilterChip(
                                selected = taskTimingMode == mKey,
                                onClick = {
                                    taskTimingMode = mKey
                                    if (mKey == "NOW") newTaskPriority = TaskPriority.URGENT.name
                                },
                                label = { Text(mLabel, fontSize = 10.sp, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (taskTimingMode == "HOURS") {
                        OutlinedTextField(
                            value = taskHoursText,
                            onValueChange = { taskHoursText = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text(com.example.util.tr("task.custom_hours_label")) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (taskTimingMode == "DAYS") {
                        OutlinedTextField(
                            value = taskDaysText,
                            onValueChange = { taskDaysText = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text(com.example.util.tr("task.custom_days_label")) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = newTaskCost,
                        onValueChange = { newTaskCost = it },
                        label = { Text("التكلفة التقديرية أو الميزانية") },
                        placeholder = { Text("0.0") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الأولوية:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        listOf(
                            TaskPriority.LOW.name to "منخفضة",
                            TaskPriority.MEDIUM.name to "متوسطة",
                            TaskPriority.HIGH.name to "عالية",
                            TaskPriority.URGENT.name to "طوارئ"
                        ).forEach { (pKey, pLabel) ->
                            FilterChip(
                                selected = newTaskPriority == pKey,
                                onClick = { newTaskPriority = pKey },
                                label = { Text(pLabel, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddTaskDialog = false },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(com.example.util.tr("common.cancel"), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                val cost = newTaskCost.toDoubleOrNull() ?: 0.0
                                val deadlineTs = System.currentTimeMillis() + calculatedDurationMs
                                val dueFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(deadlineTs))
                                val assigneeName = newTaskAssignee.trim().ifBlank { "فريق المشروع" }
                                val task = ProjectTaskEntity(
                                    id = "proj_task_${System.currentTimeMillis()}",
                                    projectId = project.id,
                                    projectName = project.name,
                                    title = newTaskTitle.trim(),
                                    description = newTaskDesc.trim().ifBlank { null },
                                    assignedToUserId = null,
                                    assignedToName = assigneeName,
                                    assignedToPhone = null,
                                    priority = if (taskTimingMode == "NOW") TaskPriority.URGENT.name else newTaskPriority,
                                    status = TaskStatus.PENDING.name,
                                    dueDate = dueFmt,
                                    costEstimate = cost,
                                    isSynced = true,
                                    workGroupId = workGroup?.id,
                                    assignedMembersList = assigneeName,
                                    deadlineDurationText = calculatedDurationText,
                                    deadlineTimestamp = deadlineTs,
                                    urgencyAlertLevel = if (taskTimingMode == "NOW") "CRITICAL" else "NORMAL",
                                    financialAmount = cost,
                                    timingMode = taskTimingMode,
                                    durationValue = calculatedDurationNum
                                )
                                onAddFieldTask(task)
                                newTaskTitle = ""
                                newTaskDesc = ""
                                newTaskAssignee = ""
                                newTaskCost = ""
                                newTaskDueDate = ""
                                showAddTaskDialog = false
                            } else {
                                Toast.makeText(context, "يرجى كتابة عنوان المهمة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إسناد المهمة", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {}
        )
    }

    // Delete Project Confirmation Dialog Guard
    if (showDeleteProjectConfirmDialog) {
        DeleteProjectConfirmationDialog(
            project = project,
            onDismiss = { showDeleteProjectConfirmDialog = false },
            onConfirm = {
                showDeleteProjectConfirmDialog = false
                onDeleteProject(project.id)
            }
        )
    }

    // Delete Task Confirmation Dialog Guard
    if (taskPendingDelete != null) {
        val targetTask = taskPendingDelete!!
        RecordDeleteConfirmationDialog(
            recordType = DeleteRecordType.PROJECT_TASK,
            title = "تأكيد حذف المهمة 📌",
            subtitle = "المشروع: ${project.name}",
            itemName = targetTask.title,
            details = listOf(
                RecordDetailItem("عنوان المهمة:", targetTask.title),
                RecordDetailItem("الحالة:", if (targetTask.status == TaskStatus.COMPLETED.name) "مكتملة ✅" else "قيد التنفيذ ⏳"),
                RecordDetailItem("الأولوية:", targetTask.priority),
                RecordDetailItem("المسؤول:", targetTask.assignedToName ?: "غير محدد")
            ),
            warningMessage = "سيتم حذف هذه المهمة وسجل تكاليفها من قاعدة البيانات المحلية بشكل نهائي.",
            onDismiss = { taskPendingDelete = null },
            onConfirm = {
                onDeleteTask(targetTask.id)
                taskPendingDelete = null
            }
        )
    }
}

/**
 * Dedicated History View listing past toast notification alerts triggered when expenses exceeded
 * the defined project budget, with live retriggering and timeline indicators.
 */
@Composable
fun ProjectBudgetAlertHistoryCard(
    project: ProjectEntity,
    auditLogs: List<com.example.data.model.ProjectAuditLogEntity>,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val isOverBudget = project.totalExpenses > project.estimatedCapital
    val excessAmount = (project.totalExpenses - project.estimatedCapital).coerceAtLeast(0.0)
    val burnPercent = if (project.estimatedCapital > 0) ((project.totalExpenses / project.estimatedCapital) * 100).toInt() else 0

    // Build historical alert list from real project audit logs and active budget status
    val allAlerts = remember(project, auditLogs) {
        val list = mutableListOf<BudgetAlertRecord>()

        // 1. Current active alert if budget exceeded
        if (isOverBudget) {
            list.add(
                BudgetAlertRecord(
                    id = "active_${project.id}",
                    timestamp = project.createdAt,
                    title = "تجاوز الميزانية المرصودة بنسبة ${burnPercent}% ⚠️",
                    toastMessage = "⚠️ تنبيه فوري: تجاوزت مصروفات المشروع (${GoldUtils.formatCurrency(project.totalExpenses)}) سقف الميزانية (${GoldUtils.formatCurrency(project.estimatedCapital)}) بمقدار عجز ${GoldUtils.formatCurrency(excessAmount)}!",
                    budgetAmount = project.estimatedCapital,
                    expenseAmount = project.totalExpenses,
                    deficitAmount = excessAmount,
                    percentage = burnPercent,
                    isCritical = burnPercent >= 115
                )
            )
        }

        // 2. Extracted from project audit logs
        auditLogs.filter { log ->
            log.action.contains("BUDGET", ignoreCase = true) ||
            log.action.contains("عجز", ignoreCase = true) ||
            log.action.contains("تجاوز", ignoreCase = true) ||
            log.action.contains("مصروفات", ignoreCase = true) ||
            (log.details?.contains("تجاوز", ignoreCase = true) == true)
        }.forEach { log ->
            list.add(
                BudgetAlertRecord(
                    id = "audit_${log.id}",
                    timestamp = log.timestamp,
                    title = "سجل رقابي: تنبيه تخطي الموازنة",
                    toastMessage = log.details ?: "تم رصد تسجيل مصروفات تجاوزت السقف المالي للمشروع بنجاح",
                    budgetAmount = project.estimatedCapital,
                    expenseAmount = project.totalExpenses,
                    deficitAmount = excessAmount,
                    percentage = burnPercent,
                    isCritical = false
                )
            )
        }

        list.distinctBy { it.id }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget) StatusRed.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isOverBudget) StatusRed.copy(alpha = 0.5f) else GoldPrimary.copy(alpha = 0.25f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_project_budget_alert_history")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = (if (isOverBudget) StatusRed else GoldPrimary).copy(alpha = 0.15f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isOverBudget) Icons.Default.Warning else Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (isOverBudget) StatusRed else GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "سجل تنبيهات تجاوز الميزانية (Toast Alerts)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "أرشيف التنبيهات المنبثقة الصادرة عند تخطي سقف المصروفات",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = (if (allAlerts.isNotEmpty()) StatusRed else StatusGreen).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${allAlerts.size} تنبيه مسجل",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (allAlerts.isNotEmpty()) StatusRed else StatusGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Budget Verification Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                OutlinedButton(
                    onClick = {
                        val msg = "فحص الرصيد: ${GoldUtils.formatCurrency(project.totalExpenses)} من أصل ${GoldUtils.formatCurrency(project.estimatedCapital)}"
                        ToastNotificationBus.showToast(
                            message = msg,
                            type = if (isOverBudget) AppToastType.WARNING else AppToastType.SUCCESS,
                            durationMillis = 2500L
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("btn_verify_budget_status")
                ) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("فحص الرصيد 🔍", fontSize = 11.5.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
            }

            // Alerts List or Empty State
            if (allAlerts.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "الميزانية منضبطة - لا توجد تجاوزات",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "معدل الاستهلاك الحالي ${burnPercent}% من إجمالي الميزانية المرصودة.",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allAlerts.forEach { alert ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (alert.isCritical) StatusRed.copy(alpha = 0.4f) else StatusOrange.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("budget_alert_item_${alert.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Top status bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = (if (alert.isCritical) StatusRed else StatusOrange).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (alert.isCritical) "🔴 تجاوز حرج (${alert.percentage}%)" else "🟠 تحذير تجاوز (${alert.percentage}%)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (alert.isCritical) StatusRed else StatusOrange,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = alert.title,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = dateFormat.format(Date(alert.timestamp)),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Toast bubble preview
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = StatusRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = alert.toastMessage,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Financial figures & action buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الميزانية: ${GoldUtils.formatCurrency(alert.budgetAmount)} • المنصرف: ${GoldUtils.formatCurrency(alert.expenseAmount)}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Retrigger toast notification
                                        IconButton(
                                            onClick = {
                                                ToastNotificationBus.showToast(
                                                    message = alert.toastMessage,
                                                    type = AppToastType.ERROR,
                                                    durationMillis = 4000L
                                                )
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("btn_retrigger_toast_alert_${alert.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Replay,
                                                contentDescription = "إعادة إطلاق التنبيه",
                                                tint = StatusRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Copy to clipboard
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(alert.toastMessage))
                                                ToastNotificationBus.showToast(
                                                    message = "تم نسخ نص التنبيه إلى الحافظة 📋",
                                                    type = AppToastType.SUCCESS,
                                                    durationMillis = 2000L
                                                )
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("btn_copy_toast_alert_${alert.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "نسخ التنبيه",
                                                tint = GoldPrimary,
                                                modifier = Modifier.size(15.dp)
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

data class BudgetAlertRecord(
    val id: String,
    val timestamp: Long,
    val title: String,
    val toastMessage: String,
    val budgetAmount: Double,
    val expenseAmount: Double,
    val deficitAmount: Double,
    val percentage: Int,
    val isCritical: Boolean
)
