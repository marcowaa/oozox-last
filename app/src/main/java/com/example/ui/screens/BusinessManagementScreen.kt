package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.WorkplaceLocationPolicyManager
import com.example.util.tr

/**
 * Enterprise Business Management & Company Hub Screen:
 * Comprehensive system for company sector setup, staff credentials, granular RBAC,
 * salaries, bonuses & deductions, leave scheduling, mandatory GPS geofenced check-in,
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
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onDeleteEmployee: (String) -> Unit = {},
    onAddPayrollAdjustment: (HiddenSubAccountEntity, String, Double, String, String) -> Unit = { _, _, _, _, _ -> },
    onAddEmployeeLeave: (HiddenSubAccountEntity, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onClockInMandatoryGps: suspend (HiddenSubAccountEntity) -> WorkplaceLocationPolicyManager.LocationPolicyCheckResult = {
        WorkplaceLocationPolicyManager.LocationPolicyCheckResult(true, "COMPLIANT_IN_BOUNDS", "تم استلام العمل بنجاح", 0f, true, false)
    },
    onOpenCompanyGroupChat: () -> Unit = {},
    onAssignTaskToCompanyGroup: (ProjectTaskEntity) -> Unit = {},
    onOpenProject: (String) -> Unit = {},
    onOpenSubscriptions: () -> Unit = {},
    onOpenCompanyAccounting: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Dialog states
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var employeeBeingEdited by remember { mutableStateOf<HiddenSubAccountEntity?>(null) }
    var employeeForAdjustment by remember { mutableStateOf<HiddenSubAccountEntity?>(null) }
    var employeeForLeave by remember { mutableStateOf<HiddenSubAccountEntity?>(null) }
    var employeePendingDelete by remember { mutableStateOf<HiddenSubAccountEntity?>(null) }
    var showAssignTaskModal by remember { mutableStateOf(false) }

    val activeCompanyName = remember(businessProfile) {
        businessProfile?.companyName?.ifBlank { "منظومة الشركة والمؤسسة" } ?: "منظومة الشركة والمؤسسة"
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(
                                onClick = onOpenDrawer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "القائمة الجانبية", tint = GoldPrimary)
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(OozoxTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(20.dp))
                            }

                            Column {
                                Text(
                                    text = activeCompanyName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = tr("biz.enterprise_sub"),
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = onOpenCompanyGroupChat,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(OozoxTeal.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = "مجموعة الموظفين", tint = OozoxTeal, modifier = Modifier.size(20.dp))
                            }

                            IconButton(
                                onClick = { showAddEmployeeDialog = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.PersonAddAlt1, contentDescription = "إضافة موظف", tint = GoldPrimary, modifier = Modifier.size(20.dp))
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
                            text = { Text(tr("biz.tab_overview"), fontSize = 11.5.sp, fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(17.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text(tr("biz.tab_company"), fontSize = 11.5.sp, fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(17.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text(tr("biz.tab_hr"), fontSize = 11.5.sp, fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(17.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 3,
                            onClick = { selectedTabIndex = 3 },
                            text = { Text(tr("biz.tab_geofence"), fontSize = 11.5.sp, fontWeight = if (selectedTabIndex == 3) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.ShareLocation, contentDescription = null, modifier = Modifier.size(17.dp)) }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> BusinessOverviewTab(
                    businessProfile = businessProfile,
                    allProjects = allProjects,
                    allHashtagAccounts = allHashtagAccounts,
                    allHashtagEntries = allHashtagEntries,
                    allInvoices = allInvoices,
                    allSubAccounts = allSubAccounts,
                    allPayrollAdjustments = allPayrollAdjustments,
                    allAttendanceRecords = allAttendanceRecords,
                    onOpenProject = onOpenProject,
                    onOpenCompanyAccounting = onOpenCompanyAccounting,
                    onOpenCompanyGroupChat = onOpenCompanyGroupChat,
                    onAssignTaskToGroup = { showAssignTaskModal = true },
                    onSwitchToTab = { selectedTabIndex = it }
                )
                1 -> BusinessSetupTab(
                    businessProfile = businessProfile,
                    onSaveProfile = onSaveBusinessProfile
                )
                2 -> BusinessHrPayrollTab(
                    allEmployees = allSubAccounts,
                    allPayrollAdjustments = allPayrollAdjustments,
                    allEmployeeLeaves = allEmployeeLeaves,
                    onAddEmployeeClick = { showAddEmployeeDialog = true },
                    onEditEmployeeClick = { employeeBeingEdited = it },
                    onAdjustPayrollClick = { employeeForAdjustment = it },
                    onAddLeaveClick = { employeeForLeave = it },
                    onDeleteEmployeeClick = { employeePendingDelete = it }
                )
                3 -> BusinessGeofenceGroupTab(
                    businessProfile = businessProfile,
                    allEmployees = allSubAccounts,
                    allAttendanceRecords = allAttendanceRecords,
                    activeSubAccount = activeSubAccount,
                    onClockInMandatoryGps = onClockInMandatoryGps,
                    onOpenCompanyGroupChat = onOpenCompanyGroupChat,
                    onAssignTaskToGroup = { showAssignTaskModal = true }
                )
            }
        }
    }

    // Modal: Add Employee
    if (showAddEmployeeDialog) {
        AddEditEmployeeDialog(
            employee = null,
            onDismiss = { showAddEmployeeDialog = false },
            onSave = { id, name, user, phone, pass, role, dept, sal, cur, geo, inv, rep, disc, mBiz, mStaff, mPay, mProj, aTask, mAcct, aDisc, dChat ->
                onCreateOrUpdateEmployee(id, name, user, phone, pass, role, dept, sal, cur, geo, inv, rep, disc, mBiz, mStaff, mPay, mProj, aTask, mAcct, aDisc, dChat)
                showAddEmployeeDialog = false
            }
        )
    }

    // Modal: Edit Employee
    val editTarget = employeeBeingEdited
    if (editTarget != null) {
        AddEditEmployeeDialog(
            employee = editTarget,
            onDismiss = { employeeBeingEdited = null },
            onSave = { id, name, user, phone, pass, role, dept, sal, cur, geo, inv, rep, disc, mBiz, mStaff, mPay, mProj, aTask, mAcct, aDisc, dChat ->
                onCreateOrUpdateEmployee(id, name, user, phone, pass, role, dept, sal, cur, geo, inv, rep, disc, mBiz, mStaff, mPay, mProj, aTask, mAcct, aDisc, dChat)
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
            title = { Text("حذف الموظف", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف حساب وصلاحيات الموظف «${deleteTarget.displayName}»؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEmployee(deleteTarget.id)
                        employeePendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الحذف", color = Color.White)
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
        val companyGroupId = businessProfile?.companyGroupId?.ifBlank { "wg_company_main" } ?: "wg_company_main"
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
