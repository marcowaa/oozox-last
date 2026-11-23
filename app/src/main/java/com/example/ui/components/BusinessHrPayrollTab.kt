package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmployeeLeaveEntity
import com.example.data.model.HiddenSubAccountEntity
import com.example.data.model.PayrollAdjustmentEntity
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OozoxTeal
import com.example.util.GoldUtils
import com.example.util.tr
import com.example.util.trFmt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Business HR & Payroll Tab: Manage employees, credentials, granular RBAC permissions,
 * base salaries, bonuses/deductions ledger, and scheduled leave history.
 */
@Composable
fun BusinessHrPayrollTab(
    allEmployees: List<HiddenSubAccountEntity>,
    allPayrollAdjustments: List<PayrollAdjustmentEntity>,
    allEmployeeLeaves: List<EmployeeLeaveEntity>,
    canManageStaff: Boolean = true,
    canManagePayroll: Boolean = true,
    onAddEmployeeClick: () -> Unit,
    onEditEmployeeClick: (HiddenSubAccountEntity) -> Unit,
    onAdjustPayrollClick: (HiddenSubAccountEntity) -> Unit,
    onAddLeaveClick: (HiddenSubAccountEntity) -> Unit,
    onDeleteEmployeeClick: (HiddenSubAccountEntity) -> Unit,
    onDeletePayrollAdjustment: (String) -> Unit = {},
    onDeleteEmployeeLeave: (String) -> Unit = {}
) {
    val totalBase = remember(allEmployees) {
        allEmployees.filter { it.isActiveEmployee }.sumOf { it.baseSalary }
    }
    val totalBonuses = remember(allPayrollAdjustments) {
        allPayrollAdjustments.filter { it.type == "BONUS" }.sumOf { it.amount }
    }
    val totalDeductions = remember(allPayrollAdjustments) {
        allPayrollAdjustments.filter { it.type == "DEDUCTION" }.sumOf { it.amount }
    }
    val netPayable = (totalBase + totalBonuses - totalDeductions).coerceAtLeast(0.0)

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("business_hr_payroll_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Monthly Payroll Summary Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(tr("hr.monthly_payroll_title"), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = trFmt("hr.staff_count_fmt", "count" to allEmployees.count { it.isActiveEmployee }),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = GoldUtils.formatCurrency(netPayable),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(trFmt("hr.base_salaries_fmt", "amount" to GoldUtils.formatCurrency(totalBase)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(trFmt("hr.bonuses_fmt", "amount" to GoldUtils.formatCurrency(totalBonuses)), fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        Text(trFmt("hr.deductions_fmt", "amount" to GoldUtils.formatCurrency(totalDeductions)), fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Add Employee Button
        if (canManageStaff) {
            item {
                Button(
                    onClick = onAddEmployeeClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("add_employee_btn")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("hr.add_employee"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Employees Directory
        if (allEmployees.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                        Text(tr("hr.no_employees"), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(tr("hr.no_employees_desc"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(allEmployees) { employee ->
                val empBonuses = remember(allPayrollAdjustments, employee.id) {
                    allPayrollAdjustments.filter { it.employeeId == employee.id && it.type == "BONUS" }.sumOf { it.amount }
                }
                val empDeductions = remember(allPayrollAdjustments, employee.id) {
                    allPayrollAdjustments.filter { it.employeeId == employee.id && it.type == "DEDUCTION" }.sumOf { it.amount }
                }
                val empNet = (employee.baseSalary + empBonuses - empDeductions).coerceAtLeast(0.0)

                val activeLeave = remember(allEmployeeLeaves, employee.id, todayStr) {
                    allEmployeeLeaves.firstOrNull {
                        it.employeeId == employee.id && it.status == "APPROVED" &&
                                todayStr >= it.startDate && todayStr <= it.endDate
                    }
                }

                val permBizLabel = tr("hr.perm_manage_business")
                val permStaffLabel = tr("hr.perm_manage_staff")
                val permPayLabel = tr("hr.perm_manage_payroll")
                val permProjLabel = tr("hr.perm_manage_projects")
                val permTaskLabel = tr("hr.perm_assign_tasks")
                val permAcctLabel = tr("hr.perm_manage_accounting")
                val permInvLabel = tr("hr.perm_create_invoices")
                val grantedPerms = remember(
                    employee, permBizLabel, permStaffLabel, permPayLabel,
                    permProjLabel, permTaskLabel, permAcctLabel, permInvLabel
                ) {
                    buildList {
                        if (employee.permManageBusiness) add(permBizLabel)
                        if (employee.permManageStaff) add(permStaffLabel)
                        if (employee.permManagePayroll) add(permPayLabel)
                        if (employee.permManageProjects) add(permProjLabel)
                        if (employee.permAssignTasks) add(permTaskLabel)
                        if (employee.permManageAccounting) add(permAcctLabel)
                        if (employee.canCreateInvoices) add(permInvLabel)
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = BorderStroke(
                        1.dp,
                        if (employee.isActiveEmployee) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_card_${employee.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        .clip(CircleShape)
                                        .background(if (employee.isActiveEmployee) OozoxTeal.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = if (employee.isActiveEmployee) OozoxTeal else Color.Gray, modifier = Modifier.size(22.dp))
                                }

                                Column {
                                    Text(
                                        text = employee.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${employee.roleTitle} • ${employee.department} • @${employee.username}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${GoldUtils.formatCurrency(empNet)} ${employee.salaryCurrency}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                                Text(
                                    text = trFmt("hr.base_salaries_fmt", "amount" to GoldUtils.formatCurrency(employee.baseSalary)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Status & RBAC badges
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (activeLeave != null) {
                                BadgePill(trFmt("hr.on_leave_until_fmt", "date" to activeLeave.endDate), Color(0xFFF59E0B))
                            }
                            if (employee.geofenceEnforced) {
                                BadgePill(tr("hr.gps_enforced_badge"), OozoxTeal)
                            }
                            grantedPerms.take(4).forEach { permLabel ->
                                BadgePill(permLabel, GoldPrimary)
                            }
                        }

                        if (canManageStaff || canManagePayroll) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (canManageStaff) {
                                    OutlinedButton(
                                        onClick = { onEditEmployeeClick(employee) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).heightIn(min = 38.dp)
                                    ) {
                                        Text(tr("hr.edit_btn"), fontSize = 11.sp)
                                    }
                                }

                                if (canManagePayroll) {
                                    OutlinedButton(
                                        onClick = { onAdjustPayrollClick(employee) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1.2f).heightIn(min = 38.dp)
                                    ) {
                                        Text(tr("hr.bonus_deduction_btn"), fontSize = 11.sp, color = GoldAccent)
                                    }
                                }

                                if (canManageStaff) {
                                    OutlinedButton(
                                        onClick = { onAddLeaveClick(employee) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1.1f).heightIn(min = 38.dp)
                                    ) {
                                        Text(tr("hr.leave_btn"), fontSize = 11.sp, color = OozoxTeal)
                                    }

                                    IconButton(
                                        onClick = { onDeleteEmployeeClick(employee) },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = tr("common.delete"), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Payroll Adjustments History Ledger
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                Text(tr("hr.payroll_history_title"), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (allPayrollAdjustments.isEmpty()) {
            item {
                Text(tr("hr.no_payroll_history"), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(allPayrollAdjustments.take(25)) { adj ->
                val isBonus = adj.type == "BONUS"
                val tint = if (isBonus) Color(0xFF10B981) else Color(0xFFEF4444)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.6.dp, tint.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${adj.employeeName} • ${if (isBonus) tr("hr.bonus_tag") else tr("hr.deduction_tag")}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = tint
                            )
                            Text(
                                text = "${adj.reason} • ${adj.effectiveDate}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${if (isBonus) "+" else "-"}${GoldUtils.formatCurrency(adj.amount)}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = tint
                            )
                            if (canManagePayroll) {
                                IconButton(
                                    onClick = { onDeletePayrollAdjustment(adj.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = tr("common.delete"), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Scheduled Employee Leaves Ledger
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(18.dp))
                Text(tr("hr.leaves_history_title"), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (allEmployeeLeaves.isEmpty()) {
            item {
                Text(tr("hr.no_leaves_history"), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(allEmployeeLeaves.take(25)) { lv ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.6.dp, OozoxTeal.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${lv.employeeName} (${trFmt("hr.days_count_fmt", "days" to lv.daysCount)})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OozoxTeal
                            )
                            Text(
                                text = "${lv.startDate} → ${lv.endDate}${if (lv.notes.isNotBlank()) " • ${lv.notes}" else ""}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (canManageStaff) {
                            IconButton(
                                onClick = { onDeleteEmployeeLeave(lv.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = tr("common.delete"), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}
