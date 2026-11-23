package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.WorkplaceLocationPolicyManager
import com.example.util.tr
import com.example.util.trFmt

/**
 * Enterprise Business Management & Company Hub Screen:
 * Comprehensive system for company sector setup, staff credentials, granular RBAC,
 * salaries, bonuses & deductions, leave scheduling, mandatory GPS geofenced login/check-in,
 * all projects & accounts management, and company-wide staff group task assignments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessManagementScreen(
    currentUser: UserEntity?,
    allInvoices: List<InvoiceEntity>,
    merchantProducts: List<MerchantProductEntity> = emptyList(),
    activeSubAccount: HiddenSubAccountEntity? = null,
    allSubAccounts: List<HiddenSubAccountEntity> = emptyList(),
    allShiftHandovers: List<ShiftHandoverEntity> = emptyList(),
    allComplaints: List<FinancialComplaintEntity> = emptyList(),
    allProjects: List<ProjectEntity> = emptyList(),
    allProjectTasks: List<ProjectTaskEntity> = emptyList(),
    allHashtagAccounts: List<HashtagAccountEntity> = emptyList(),
    allHashtagEntries: List<HashtagEntryEntity> = emptyList(),
    allDailyRecords: List<DailyAccountingRecordEntity> = emptyList(),
    businessProfile: BusinessProfileEntity? = null,
    allPayrollAdjustments: List<PayrollAdjustmentEntity> = emptyList(),
    allEmployeeLeaves: List<EmployeeLeaveEntity> = emptyList(),
    allAttendanceRecords: List<EmployeeAttendanceEntity> = emptyList(),
    allWorkGroups: List<WorkGroupEntity> = emptyList(),
    allGroupMembers: List<GroupMemberEntity> = emptyList(),
    onLoginSubAccount: (String, String) -> Boolean = { _, _ -> false },
    onLoginEmployeeWithMandatoryGps: suspend (String, String) -> Pair<Boolean, WorkplaceLocationPolicyManager.LocationPolicyCheckResult?> = { _, _ -> false to null },
    onLogoutSubAccount: () -> Unit = {},
    onAddShiftHandover: (ShiftHandoverEntity) -> Unit = {},
    onReceiveShift: (handoverId: String, receiverId: String, receiverName: String, notes: String?) -> Unit = { _, _, _, _ -> },
    onReceiveShiftFull: (
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
    ) -> Unit = { id, rId, rName, notes, _, _, _, _, _, _, _ -> onReceiveShift(id, rId, rName, notes) },
    onRespondToComplaint: (complaintId: String, accepted: Boolean, reason: String?, proofUri: String?) -> Unit = { _, _, _, _ -> },
    onDecideOnComplaint: (complaintId: String, decision: String, notes: String?) -> Unit = { _, _, _ -> },
    onApplyJointDeduction: (complaintId: String, primaryAmount: Double, receiverAmount: Double, receiverId: String?, notes: String?) -> Unit = { _, _, _, _, _ -> },
    onUpdateProfession: (String) -> Unit = {},
    onCreateInvoice: (InvoiceEntity) -> Unit = {},
    onAddMerchantProduct: (MerchantProductEntity) -> Unit = {},
    onAddSubAccount: (name: String, phone: String, pin: String) -> Unit = { _, _, _ -> },
    onSaveBusinessProfile: (BusinessProfileEntity) -> Unit = {},
    onCreateOrUpdateEmployee: (
        existingId: String?,
        fullName: String,
        username: String,
        phone: String,
        password: String,
        roleTitle: String,
        department: String,
        baseSalary: Double,
        salaryCurrency: String,
        geofenceEnforced: Boolean,
        isActiveEmployee: Boolean,
        canCreateInvoices: Boolean,
        canViewReports: Boolean,
        canSetProductPrices: Boolean,
        permManageBusiness: Boolean,
        permManageStaff: Boolean,
        permManagePayroll: Boolean,
        permManageProjects: Boolean,
        permAssignTasks: Boolean,
        permManageAccounting: Boolean,
        permApproveDiscounts: Boolean,
        permDirectChat: Boolean
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onDeleteEmployee: (String) -> Unit = {},
    onAddPayrollAdjustment: (HiddenSubAccountEntity, String, Double, String, String) -> Unit = { _, _, _, _, _ -> },
    onDeletePayrollAdjustment: (String) -> Unit = {},
    onAddEmployeeLeave: (HiddenSubAccountEntity, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onDeleteEmployeeLeave: (String) -> Unit = {},
    onClockInMandatoryGps: suspend (HiddenSubAccountEntity) -> WorkplaceLocationPolicyManager.LocationPolicyCheckResult = {
        WorkplaceLocationPolicyManager.LocationPolicyCheckResult(true, "COMPLIANT_IN_BOUNDS", "✓", 0f, true, false)
    },
    onOpenCompanyGroupChat: () -> Unit = {},
    onAssignTaskToCompanyGroup: (ProjectTaskEntity) -> Unit = {},
    onUpdateTaskStatus: (String, String) -> Unit = { _, _ -> },
    onOpenProject: (String) -> Unit = {},
    onOpenSubscriptions: () -> Unit = {},
    onOpenCompanyAccounting: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // RBAC permissions derived from activeSubAccount (Owner mode has full access)
    val isOwnerMode = activeSubAccount == null
    val canManageBusiness = isOwnerMode || activeSubAccount?.permManageBusiness == true
    val canManageStaff = isOwnerMode || activeSubAccount?.permManageStaff == true
    val canManagePayroll = isOwnerMode || activeSubAccount?.permManagePayroll == true
    val canAssignTasks = isOwnerMode || activeSubAccount?.permAssignTasks == true
    val canManageAccounting = isOwnerMode || activeSubAccount?.permManageAccounting == true

    // Dialog states
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var showEmployeeLoginDialog by remember { mutableStateOf(false) }
    var employeeBeingEdited by remember { mutableStateOf<HiddenSubAccountEntity?>(null) }
    var employeeForAdjustment by remember { mutableStateOf<HiddenSubAccountEntity?>(null) }
    var employeeForLeave by remember { mutableStateOf<HiddenSubAccountEntity?>(null) }
    var employeePendingDelete by remember { mutableStateOf<HiddenSubAccountEntity?>(null) }
    var showAssignTaskModal by remember { mutableStateOf(false) }

    val defaultCompanyTitle = tr("biz.default_company_name")
    val activeCompanyName = remember(businessProfile, defaultCompanyTitle) {
        businessProfile?.companyName?.ifBlank { defaultCompanyTitle } ?: defaultCompanyTitle
    }
    val companyGroupId = remember(businessProfile) {
        businessProfile?.companyGroupId?.ifBlank { "wg_company_main" } ?: "wg_company_main"
    }
    val companyGroupTasks = remember(allProjectTasks, companyGroupId) {
        allProjectTasks.filter { it.workGroupId == companyGroupId || it.projectId == "company_main_tasks" }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onOpenDrawer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = tr("biz.enterprise_title"), tint = GoldPrimary)
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(OozoxTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(18.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeCompanyName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tr("biz.enterprise_sub"),
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            OozoxCircleIconButton(
                                onClick = { showEmployeeLoginDialog = true },
                                imageVector = Icons.Default.Badge,
                                contentDescription = tr("hr.login_portal_btn"),
                                tint = GoldPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.testTag("topbar_employee_login_btn")
                            )

                            OozoxCircleIconButton(
                                onClick = onOpenCompanyGroupChat,
                                imageVector = Icons.Default.Groups,
                                contentDescription = tr("hr.staff_group_title"),
                                tint = OozoxTeal,
                                containerColor = OozoxTeal.copy(alpha = 0.15f),
                                modifier = Modifier.testTag("topbar_company_group_btn")
                            )

                            if (canManageStaff) {
                                OozoxCircleIconButton(
                                    onClick = { showAddEmployeeDialog = true },
                                    imageVector = Icons.Default.PersonAddAlt1,
                                    contentDescription = tr("hr.add_employee"),
                                    tint = GoldPrimary,
                                    containerColor = GoldPrimary.copy(alpha = 0.2f),
                                    modifier = Modifier.testTag("topbar_add_employee_btn")
                                )
                            }
                        }
                    }

                    // 4 Main Tabs
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = GoldPrimary,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text(tr("biz.tab_overview"), fontSize = 11.5.sp, fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(17.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text(tr("biz.tab_company"), fontSize = 11.5.sp, fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            icon = { Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(17.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text(tr("biz.tab_hr"), fontSize = 11.5.sp, fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(17.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 3,
                            onClick = { selectedTabIndex = 3 },
                            text = { Text(tr("biz.tab_geofence"), fontSize = 11.5.sp, fontWeight = if (selectedTabIndex == 3) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            icon = { Icon(Icons.Default.ShareLocation, contentDescription = null, modifier = Modifier.size(17.dp)) }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Active Logged-In Employee Session Banner
            val loggedEmployee = activeSubAccount
            if (loggedEmployee != null) {
                Surface(
                    color = OozoxTeal.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, OozoxTeal.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(18.dp))
                            Text(
                                text = trFmt(
                                    "hr.active_session_banner_fmt",
                                    "name" to loggedEmployee.displayName,
                                    "role" to loggedEmployee.roleTitle
                                ),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = OozoxTeal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = onLogoutSubAccount,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("switch_to_manager_btn")
                        ) {
                            Text(tr("hr.switch_to_manager"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTabIndex) {
                    0 -> BusinessOverviewTab(
                        businessProfile = businessProfile,
                        allProjects = allProjects,
                        allHashtagAccounts = allHashtagAccounts,
                        allHashtagEntries = allHashtagEntries,
                        allDailyRecords = allDailyRecords,
                        allInvoices = allInvoices,
                        allSubAccounts = allSubAccounts,
                        allPayrollAdjustments = allPayrollAdjustments,
                        allAttendanceRecords = allAttendanceRecords,
                        canManageAccounting = canManageAccounting,
                        canAssignTasks = canAssignTasks,
                        onOpenProject = onOpenProject,
                        onOpenCompanyAccounting = onOpenCompanyAccounting,
                        onOpenCompanyGroupChat = onOpenCompanyGroupChat,
                        onAssignTaskToGroup = { showAssignTaskModal = true },
                        onSwitchToTab = { selectedTabIndex = it }
                    )
                    1 -> BusinessSetupTab(
                        businessProfile = businessProfile,
                        canEditBusiness = canManageBusiness,
                        onSaveProfile = onSaveBusinessProfile
                    )
                    2 -> BusinessHrPayrollTab(
                        allEmployees = allSubAccounts,
                        allPayrollAdjustments = allPayrollAdjustments,
                        allEmployeeLeaves = allEmployeeLeaves,
                        canManageStaff = canManageStaff,
                        canManagePayroll = canManagePayroll,
                        onAddEmployeeClick = { showAddEmployeeDialog = true },
                        onEditEmployeeClick = { employeeBeingEdited = it },
                        onAdjustPayrollClick = { employeeForAdjustment = it },
                        onAddLeaveClick = { employeeForLeave = it },
                        onDeleteEmployeeClick = { employeePendingDelete = it },
                        onDeletePayrollAdjustment = onDeletePayrollAdjustment,
                        onDeleteEmployeeLeave = onDeleteEmployeeLeave
                    )
                    3 -> BusinessGeofenceGroupTab(
                        businessProfile = businessProfile,
                        allEmployees = allSubAccounts,
                        allAttendanceRecords = allAttendanceRecords,
                        companyTasks = companyGroupTasks,
                        activeSubAccount = activeSubAccount,
                        canAssignTasks = canAssignTasks,
                        onClockInMandatoryGps = onClockInMandatoryGps,
                        onOpenEmployeeLoginClick = { showEmployeeLoginDialog = true },
                        onOpenCompanyGroupChat = onOpenCompanyGroupChat,
                        onAssignTaskToGroup = { showAssignTaskModal = true },
                        onUpdateTaskStatus = onUpdateTaskStatus
                    )
                }
            }
        }
    }

    // Modal: Mandatory Employee Login + GPS Verification
    if (showEmployeeLoginDialog) {
        EmployeeLoginMandatoryGpsDialog(
            onDismiss = { showEmployeeLoginDialog = false },
            onLoginWithMandatoryGps = onLoginEmployeeWithMandatoryGps
        )
    }

    // Modal: Add Employee
    if (showAddEmployeeDialog) {
        AddEditEmployeeDialog(
            employee = null,
            defaultCurrency = businessProfile?.currency?.ifBlank { "EGP" } ?: "EGP",
            onDismiss = { showAddEmployeeDialog = false },
            onSave = { id, name, user, phone, pass, role, dept, sal, cur, geo, activeEmp, inv, rep, disc, mBiz, mStaff, mPay, mProj, aTask, mAcct, aDisc, dChat ->
                onCreateOrUpdateEmployee(id, name, user, phone, pass, role, dept, sal, cur, geo, activeEmp, inv, rep, disc, mBiz, mStaff, mPay, mProj, aTask, mAcct, aDisc, dChat)
                showAddEmployeeDialog = false
            }
        )
    }

    // Modal: Edit Employee
    val editTarget = employeeBeingEdited
    if (editTarget != null) {
        AddEditEmployeeDialog(
            employee = editTarget,
            defaultCurrency = businessProfile?.currency?.ifBlank { "EGP" } ?: "EGP",
            onDismiss = { employeeBeingEdited = null },
            onSave = { id, name, user, phone, pass, role, dept, sal, cur, geo, activeEmp, inv, rep, disc, mBiz, mStaff, mPay, mProj, aTask, mAcct, aDisc, dChat ->
                onCreateOrUpdateEmployee(id, name, user, phone, pass, role, dept, sal, cur, geo, activeEmp, inv, rep, disc, mBiz, mStaff, mPay, mProj, aTask, mAcct, aDisc, dChat)
                employeeBeingEdited = null
            }
        )
    }

    // Modal: Payroll Adjustment (Bonus / Deduction)
    val adjTarget = employeeForAdjustment
    if (adjTarget != null) {
        PayrollAdjustmentDialog(
            employee = adjTarget,
            onDismiss = { employeeForAdjustment = null },
            onConfirm = { type, amount, reason, date ->
                onAddPayrollAdjustment(adjTarget, type, amount, reason, date)
                employeeForAdjustment = null
            }
        )
    }

    // Modal: Employee Leave Date Range
    val leaveTarget = employeeForLeave
    if (leaveTarget != null) {
        EmployeeLeaveDialog(
            employee = leaveTarget,
            onDismiss = { employeeForLeave = null },
            onConfirm = { leaveType, start, end, notes ->
                onAddEmployeeLeave(leaveTarget, leaveType, start, end, notes)
                employeeForLeave = null
            }
        )
    }

    // Modal: Delete Employee Confirmation
    val deleteTarget = employeePendingDelete
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { employeePendingDelete = null },
            title = { Text(tr("hr.delete_emp_title"), fontWeight = FontWeight.Bold) },
            text = { Text(trFmt("hr.delete_emp_confirm_fmt", "name" to deleteTarget.displayName)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEmployee(deleteTarget.id)
                        employeePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(tr("hr.confirm_delete"), color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { employeePendingDelete = null }) {
                    Text(tr("common.cancel"))
                }
            }
        )
    }

    // Modal: Assign Task to Company Staff Group
    if (showAssignTaskModal) {
        val groupMembersMentions = remember(allSubAccounts) {
            allSubAccounts.filter { it.isActiveEmployee }.map { "@${it.displayName} (@${it.username})" }
        }

        CreatePhysicalFieldTaskDialog(
            projectId = "company_main_tasks",
            projectName = activeCompanyName,
            workGroupId = companyGroupId,
            availableMembers = groupMembersMentions,
            onDismiss = { showAssignTaskModal = false },
            onCreateTask = { task ->
                onAssignTaskToCompanyGroup(task)
                showAssignTaskModal = false
            }
        )
    }
}
