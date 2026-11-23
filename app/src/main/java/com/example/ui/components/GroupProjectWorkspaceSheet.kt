package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GroupMemberEntity
import com.example.data.model.HiddenSubAccountEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectMemberEntity
import com.example.data.model.ProjectMemberRole
import com.example.data.model.ProjectTaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.WorkGroupEntity
import com.example.ui.theme.*
import com.example.util.GoldUtils
import com.example.util.LocalAppLanguage
import com.example.util.ProjectIndustryCatalog
import com.example.util.tr

/**
 * Unified Group-Chat <-> Project shared workspace bottom sheet.
 * Allows managing shared finances, tasks, and team roles/permissions directly inside the group chat,
 * backed by the exact same Room entities used by ProjectDetailScreen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupProjectWorkspaceSheet(
    project: ProjectEntity,
    workGroup: WorkGroupEntity?,
    projectTasks: List<ProjectTaskEntity>,
    projectMembers: List<ProjectMemberEntity>,
    groupMembers: List<GroupMemberEntity>,
    employees: List<HiddenSubAccountEntity> = emptyList(),
    initialTab: Int = 0,
    onAddAccountingEntry: (title: String, amount: Double, isIncome: Boolean, category: String) -> Unit,
    onOpenCreateTask: () -> Unit,
    onUpdateTaskStatus: (taskId: String, status: String) -> Unit,
    onSubmitTaskProof: (ProjectTaskEntity) -> Unit,
    onVerifyTask: (ProjectTaskEntity) -> Unit,
    onAddProjectMemberWithPerms: (name: String, phone: String, role: ProjectMemberRole, task: String, userId: String?, isAdmin: Boolean, canPost: Boolean, canEdit: Boolean, canExport: Boolean, canDirect: Boolean) -> Unit,
    onPromoteToManager: (ProjectMemberEntity) -> Unit,
    onRemoveMember: (ProjectMemberEntity) -> Unit,
    onOpenFullProjectDetail: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentLanguage = LocalAppLanguage.current
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    var showAccountingDialog by remember { mutableStateOf(false) }
    var accountingInitialTitle by remember { mutableStateOf("") }
    var accountingInitialIsIncome by remember { mutableStateOf(false) }
    var showRbacDialog by remember { mutableStateOf(false) }

    val accountingProfile = remember(project.type) {
        ProjectIndustryCatalog.resolveAccountingProfile(project.type)
    }
    val netBalance = project.totalIncome - project.totalExpenses

    val unifiedRoster = remember(projectMembers, groupMembers) {
        buildUnifiedProjectRoster(projectMembers, groupMembers)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.testTag("group_project_workspace_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(adaptiveGoldPrimary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = null,
                            tint = adaptiveGoldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = project.name,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = tr("proj_group.shared_sync_subtitle", "بيانات ومهام متزامنة مع صفحة المشروع"),
                            fontSize = 10.5.sp,
                            color = adaptiveGoldPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            onDismiss()
                            onOpenFullProjectDetail()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = adaptiveGoldPrimary.copy(alpha = 0.14f),
                            contentColor = adaptiveGoldPrimary
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("workspace_open_full_project_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(tr("proj_group.open_full_project", "صفحة المشروع"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OozoxCircleIconButton(
                        onClick = onDismiss,
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق"
                    )
                }
            }

            // Tabs
            val tabs = listOf(
                tr("proj_group.tab_finance", "المالية"),
                "${tr("proj_group.tab_tasks", "المهام")} (${projectTasks.size})",
                "${tr("proj_group.tab_team", "الفريق")} (${unifiedRoster.size})"
            )
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = adaptiveGoldPrimary
            ) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = {
                            Text(
                                text = label,
                                fontSize = 12.5.sp,
                                fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("workspace_tab_$idx")
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(accountingProfile.sectorNameAr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                                    Surface(
                                        color = adaptiveGoldPrimary.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = workGroup?.name ?: project.name,
                                            fontSize = 10.5.sp,
                                            color = adaptiveGoldPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    WorkspaceMetricTile("رأس المال", GoldUtils.formatCurrency(project.estimatedCapital), adaptiveGoldPrimary, Modifier.weight(1f))
                                    WorkspaceMetricTile("صافي الرصيد", GoldUtils.formatCurrency(netBalance), if (netBalance >= 0) StatusGreen else StatusRed, Modifier.weight(1f))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    WorkspaceMetricTile("الإيرادات", GoldUtils.formatCurrency(project.totalIncome), StatusGreen, Modifier.weight(1f))
                                    WorkspaceMetricTile("المصروفات", GoldUtils.formatCurrency(project.totalExpenses), StatusOrange, Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    item {
                        SingleProjectBudgetVisualizer(project = project)
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    accountingInitialTitle = ""
                                    accountingInitialIsIncome = false
                                    showAccountingDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("workspace_quick_entry_btn")
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("proj_group.quick_entry", "تسجيل قيد مالي"), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (selectedTab == 1) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("المهام المسندة للفريق (${projectTasks.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                            Button(
                                onClick = onOpenCreateTask,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("workspace_assign_task_btn")
                            ) {
                                Icon(Icons.Default.AssignmentInd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إسناد مهمة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (projectTasks.isEmpty()) {
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
                                    Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
                                    Text("لا توجد مهام مسجلة حالياً", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        items(projectTasks, key = { it.id }) { task ->
                            val isDone = task.status == TaskStatus.COMPLETED.name
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (isDone) StatusGreen.copy(alpha = 0.4f) else adaptiveGoldPrimary.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isDone,
                                            onCheckedChange = { checked ->
                                                onUpdateTaskStatus(
                                                    task.id,
                                                    if (checked) TaskStatus.COMPLETED.name else TaskStatus.IN_PROGRESS.name
                                                )
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = StatusGreen, uncheckedColor = adaptiveGoldPrimary)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.title,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            val meta = listOfNotNull(
                                                task.assignedToName?.takeIf { it.isNotBlank() },
                                                task.deadlineDurationText.takeIf { it.isNotBlank() }
                                            ).joinToString(" • ")
                                            if (meta.isNotBlank()) {
                                                Text(meta, fontSize = 11.sp, color = adaptiveGoldPrimary)
                                            }
                                        }
                                        Surface(
                                            color = when (task.priority) {
                                                TaskPriority.URGENT.name -> StatusRed.copy(alpha = 0.14f)
                                                TaskPriority.HIGH.name -> StatusOrange.copy(alpha = 0.14f)
                                                else -> adaptiveGoldPrimary.copy(alpha = 0.12f)
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = when (task.priority) {
                                                    TaskPriority.URGENT.name -> "عاجل"
                                                    TaskPriority.HIGH.name -> "مرتفع"
                                                    TaskPriority.LOW.name -> "منخفض"
                                                    else -> "متوسط"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (task.priority) {
                                                    TaskPriority.URGENT.name -> StatusRed
                                                    TaskPriority.HIGH.name -> StatusOrange
                                                    else -> adaptiveGoldPrimary
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (!task.isArchived && (!isDone || task.isSubmittedForReview)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (!isDone) {
                                                FilledTonalButton(
                                                    onClick = { onSubmitTaskProof(task) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                ) {
                                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("توثيق الإنجاز", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            if (task.isSubmittedForReview) {
                                                Button(
                                                    onClick = { onVerifyTask(task) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen, contentColor = Color.White),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                ) {
                                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("اعتماد وأرشفة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedTab == 2) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("فريق المشروع والمجموعة (${unifiedRoster.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                            Button(
                                onClick = { showRbacDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("workspace_add_member_btn")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tr("proj_group.add_member_cta", "إضافة موظف"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (unifiedRoster.isEmpty()) {
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
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
                                    Text(tr("proj_group.no_members_yet", "لا يوجد أعضاء مضافون بعد"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        items(unifiedRoster, key = { it.key }) { item ->
                            val roleEnum = item.projectMember?.let { pm ->
                                ProjectMemberRole.entries.firstOrNull { it.name == pm.role }
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(0.8.dp, adaptiveGoldPrimary.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(adaptiveGoldPrimary.copy(alpha = 0.14f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(18.dp))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.displayName, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                val sub = listOfNotNull(
                                                    item.jobTitle.takeIf { it.isNotBlank() },
                                                    item.phone.takeIf { it.isNotBlank() }
                                                ).joinToString(" • ")
                                                if (sub.isNotBlank()) {
                                                    Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                        Surface(
                                            color = adaptiveGoldPrimary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = roleEnum?.localizedLabel(currentLanguage) ?: item.roleBadge,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = adaptiveGoldPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    if (item.permissions.isNotEmpty()) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            item.permissions.forEach { perm ->
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(6.dp),
                                                    border = BorderStroke(0.6.dp, adaptiveGoldPrimary.copy(alpha = 0.3f))
                                                ) {
                                                    Text(
                                                        text = perm,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val pm = item.projectMember
                                    if (pm != null && roleEnum != ProjectMemberRole.OWNER) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (roleEnum != ProjectMemberRole.EXECUTIVE_MANAGER) {
                                                FilledTonalButton(
                                                    onClick = { onPromoteToManager(pm) },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = StatusGreen.copy(alpha = 0.14f),
                                                        contentColor = StatusGreen
                                                    )
                                                ) {
                                                    Icon(Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("ترقية لمدير", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            OozoxCircleIconButton(
                                                onClick = { onRemoveMember(pm) },
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "إزالة العضو",
                                                tint = MaterialTheme.colorScheme.error,
                                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
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

    if (showAccountingDialog) {
        ProjectAccountingEntryDialog(
            profile = accountingProfile,
            initialTitle = accountingInitialTitle,
            initialIsIncome = accountingInitialIsIncome,
            onDismiss = { showAccountingDialog = false },
            onConfirmEntry = { title, amount, isIncome, category ->
                onAddAccountingEntry(title, amount, isIncome, category)
                showAccountingDialog = false
            }
        )
    }

    if (showRbacDialog) {
        ProjectRbacDialog(
            projectId = project.id,
            members = projectMembers,
            groupMembers = groupMembers,
            employees = employees,
            onAddMember = { name, phone, role, task ->
                onAddProjectMemberWithPerms(
                    name, phone, role, task, null,
                    role.defaultIsAdmin(),
                    role.defaultCanPostAccounting(),
                    role.defaultCanEditRecords(),
                    role.defaultCanExportReports(),
                    true
                )
            },
            onAddMemberWithPermissions = onAddProjectMemberWithPerms,
            onPromoteToManager = onPromoteToManager,
            onRemoveMember = onRemoveMember,
            onDismiss = { showRbacDialog = false }
        )
    }
}

@Composable
private fun WorkspaceMetricTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

data class UnifiedProjectRosterItem(
    val key: String,
    val displayName: String,
    val phone: String,
    val jobTitle: String,
    val roleBadge: String,
    val permissions: List<String>,
    val projectMember: ProjectMemberEntity?,
    val groupMember: GroupMemberEntity?
)

fun buildUnifiedProjectRoster(
    projectMembers: List<ProjectMemberEntity>,
    groupMembers: List<GroupMemberEntity>
): List<UnifiedProjectRosterItem> {
    val result = mutableListOf<UnifiedProjectRosterItem>()
    val matchedGroupMemberIds = mutableSetOf<String>()

    projectMembers.forEach { pm ->
        val matchedGm = groupMembers.firstOrNull { gm ->
            gm.userId == pm.userId ||
                (gm.phone.isNotBlank() && gm.phone == pm.userPhone) ||
                gm.username.equals(pm.userName, ignoreCase = true)
        }
        if (matchedGm != null) matchedGroupMemberIds.add(matchedGm.id)
        val roleEnum = ProjectMemberRole.entries.firstOrNull { it.name == pm.role }
        val perms = buildList {
            if (matchedGm?.isGroupAdmin == true || roleEnum?.defaultIsAdmin() == true) add("مدير مجموعة")
            if (matchedGm?.canPostAccounting == true || roleEnum?.defaultCanPostAccounting() == true) add("قيود مالية")
            if (matchedGm?.canEditRecords == true || roleEnum?.defaultCanEditRecords() == true) add("تعديل")
            if (matchedGm?.canExportReports == true || roleEnum?.defaultCanExportReports() == true) add("تقارير")
            if (matchedGm?.canMessageDirectly != false) add("تراسل مباشر")
        }
        result.add(
            UnifiedProjectRosterItem(
                key = "pm_${pm.id}",
                displayName = pm.userName,
                phone = pm.userPhone.ifBlank { matchedGm?.phone.orEmpty() },
                jobTitle = pm.assignedTask?.takeIf { it.isNotBlank() } ?: matchedGm?.roleLabel.orEmpty(),
                roleBadge = roleEnum?.label ?: matchedGm?.roleLabel ?: "عضو فريق",
                permissions = perms,
                projectMember = pm,
                groupMember = matchedGm
            )
        )
    }

    groupMembers.filter { it.id !in matchedGroupMemberIds }.forEach { gm ->
        val perms = buildList {
            if (gm.isGroupAdmin) add("مدير مجموعة")
            if (gm.canPostAccounting) add("قيود مالية")
            if (gm.canEditRecords) add("تعديل")
            if (gm.canExportReports) add("تقارير")
            if (gm.canMessageDirectly) add("تراسل مباشر")
        }
        result.add(
            UnifiedProjectRosterItem(
                key = "gm_${gm.id}",
                displayName = gm.username,
                phone = gm.phone,
                jobTitle = gm.roleLabel,
                roleBadge = if (gm.isGroupAdmin) "مدير" else gm.roleLabel,
                permissions = perms,
                projectMember = null,
                groupMember = gm
            )
        )
    }

    return result
}
