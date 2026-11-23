package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.HiddenSubAccountEntity
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OozoxTeal
import com.example.util.tr
import com.example.util.trFmt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Add / Edit Enterprise Employee Dialog with Username, Password, Role Presets,
 * Base Salary, Mandatory Geofence toggle, Active toggle, and 10 Granular RBAC Permissions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEmployeeDialog(
    employee: HiddenSubAccountEntity? = null,
    defaultCurrency: String = "EGP",
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    var fullName by remember { mutableStateOf(employee?.displayName.orEmpty()) }
    var username by remember { mutableStateOf(employee?.username.orEmpty()) }
    var phone by remember { mutableStateOf(employee?.phone.orEmpty()) }
    var password by remember { mutableStateOf(employee?.rawSecretTrigger.orEmpty()) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val defaultSalesRole = tr("hr.preset_sales")
    var roleTitle by remember { mutableStateOf(employee?.roleTitle?.ifBlank { defaultSalesRole } ?: defaultSalesRole) }
    var department by remember { mutableStateOf(employee?.department.orEmpty()) }
    var baseSalaryStr by remember { mutableStateOf(if ((employee?.baseSalary ?: 0.0) > 0) employee?.baseSalary?.toInt().toString() else "6000") }
    var salaryCurrency by remember { mutableStateOf(employee?.salaryCurrency?.ifBlank { defaultCurrency } ?: defaultCurrency) }
    var geofenceEnforced by remember { mutableStateOf(employee?.geofenceEnforced ?: true) }
    var isActiveEmployee by remember { mutableStateOf(employee?.isActiveEmployee ?: true) }

    // 10 Granular RBAC Permissions
    var permManageBusiness by remember { mutableStateOf(employee?.permManageBusiness ?: false) }
    var permManageStaff by remember { mutableStateOf(employee?.permManageStaff ?: false) }
    var permManagePayroll by remember { mutableStateOf(employee?.permManagePayroll ?: false) }
    var permManageProjects by remember { mutableStateOf(employee?.permManageProjects ?: true) }
    var permAssignTasks by remember { mutableStateOf(employee?.permAssignTasks ?: true) }
    var permManageAccounting by remember { mutableStateOf(employee?.permManageAccounting ?: false) }
    var canCreateInvoices by remember { mutableStateOf(employee?.canCreateInvoices ?: true) }
    var permApproveDiscounts by remember { mutableStateOf(employee?.permApproveDiscounts ?: false) }
    var canViewReports by remember { mutableStateOf(employee?.canViewReports ?: true) }
    var permDirectChat by remember { mutableStateOf(employee?.permDirectChat ?: true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PersonAddAlt1, contentDescription = null, tint = GoldPrimary)
                        Text(
                            text = if (employee == null) tr("hr.add_employee") else tr("hr.edit_employee_title"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = tr("common.close"))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text(tr("hr.full_name")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("employee_name_input")
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it.filter { c -> c.isLetterOrDigit() || c == '_' }.lowercase() },
                            label = { Text(tr("hr.username")) },
                            leadingIcon = { Text("@", fontWeight = FontWeight.Bold, color = GoldPrimary) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("employee_username_input")
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text(tr("hr.phone_label")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(tr("hr.password")) },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("employee_password_input")
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = roleTitle,
                            onValueChange = { roleTitle = it },
                            label = { Text(tr("hr.role_title")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text(tr("hr.department")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = baseSalaryStr,
                            onValueChange = { baseSalaryStr = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(tr("hr.base_salary")) },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f).testTag("employee_salary_input")
                        )
                        OutlinedTextField(
                            value = salaryCurrency,
                            onValueChange = { salaryCurrency = it.uppercase() },
                            label = { Text(tr("hr.currency_label")) },
                            singleLine = true,
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    // Mandatory Geofence Toggle
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, if (geofenceEnforced) OozoxTeal.copy(alpha = 0.5f) else Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tr("hr.geofence_enforce_title"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(tr("hr.geofence_enforce_desc"), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = geofenceEnforced,
                                onCheckedChange = { geofenceEnforced = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = OozoxTeal, checkedTrackColor = OozoxTeal.copy(alpha = 0.3f))
                            )
                        }
                    }

                    if (employee != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tr("hr.active_employee_title"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(tr("hr.active_employee_desc"), fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = isActiveEmployee, onCheckedChange = { isActiveEmployee = it })
                            }
                        }
                    }

                    // Role Presets
                    Text(tr("hr.role_presets"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = OozoxTeal)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val execLabel = tr("hr.preset_exec")
                        val acctLabel = tr("hr.preset_acct")
                        val projLabel = tr("hr.preset_proj")
                        val salesLabel = tr("hr.preset_sales")

                        AssistChip(onClick = {
                            roleTitle = execLabel
                            permManageBusiness = true; permManageStaff = true; permManagePayroll = true
                            permManageProjects = true; permAssignTasks = true; permManageAccounting = true
                            canCreateInvoices = true; permApproveDiscounts = true; canViewReports = true; permDirectChat = true
                        }, label = { Text(execLabel, fontSize = 10.5.sp) })

                        AssistChip(onClick = {
                            roleTitle = acctLabel
                            permManageBusiness = false; permManageStaff = false; permManagePayroll = true
                            permManageProjects = false; permAssignTasks = false; permManageAccounting = true
                            canCreateInvoices = true; permApproveDiscounts = true; canViewReports = true; permDirectChat = true
                        }, label = { Text(acctLabel, fontSize = 10.5.sp) })

                        AssistChip(onClick = {
                            roleTitle = projLabel
                            permManageBusiness = false; permManageStaff = false; permManagePayroll = false
                            permManageProjects = true; permAssignTasks = true; permManageAccounting = false
                            canCreateInvoices = true; permApproveDiscounts = false; canViewReports = true; permDirectChat = true
                        }, label = { Text(projLabel, fontSize = 10.5.sp) })

                        AssistChip(onClick = {
                            roleTitle = salesLabel
                            permManageBusiness = false; permManageStaff = false; permManagePayroll = false
                            permManageProjects = false; permAssignTasks = false; permManageAccounting = false
                            canCreateInvoices = true; permApproveDiscounts = false; canViewReports = false; permDirectChat = true
                        }, label = { Text(salesLabel, fontSize = 10.5.sp) })
                    }

                    Text(tr("hr.permissions_title"), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                    PermissionToggleRow(tr("hr.perm_manage_business"), permManageBusiness) { permManageBusiness = it }
                    PermissionToggleRow(tr("hr.perm_manage_staff"), permManageStaff) { permManageStaff = it }
                    PermissionToggleRow(tr("hr.perm_manage_payroll"), permManagePayroll) { permManagePayroll = it }
                    PermissionToggleRow(tr("hr.perm_manage_projects"), permManageProjects) { permManageProjects = it }
                    PermissionToggleRow(tr("hr.perm_assign_tasks"), permAssignTasks) { permAssignTasks = it }
                    PermissionToggleRow(tr("hr.perm_manage_accounting"), permManageAccounting) { permManageAccounting = it }
                    PermissionToggleRow(tr("hr.perm_create_invoices"), canCreateInvoices) { canCreateInvoices = it }
                    PermissionToggleRow(tr("hr.perm_approve_discounts"), permApproveDiscounts) { permApproveDiscounts = it }
                    PermissionToggleRow(tr("hr.perm_view_reports"), canViewReports) { canViewReports = it }
                    PermissionToggleRow(tr("hr.perm_direct_chat"), permDirectChat) { permDirectChat = it }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).heightIn(min = 46.dp)) {
                        Text(tr("common.cancel"))
                    }
                    Button(
                        onClick = {
                            val salary = baseSalaryStr.toDoubleOrNull() ?: 0.0
                            onSave(
                                employee?.id, fullName.trim(), username.trim(), phone.trim(), password.trim(),
                                roleTitle.trim(), department.trim(), salary, salaryCurrency.trim(),
                                geofenceEnforced, isActiveEmployee, canCreateInvoices, canViewReports,
                                permApproveDiscounts, permManageBusiness, permManageStaff, permManagePayroll,
                                permManageProjects, permAssignTasks, permManageAccounting, permApproveDiscounts, permDirectChat
                            )
                            onDismiss()
                        },
                        enabled = fullName.isNotBlank() && username.isNotBlank() && (password.isNotBlank() || employee != null),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        modifier = Modifier.weight(1.8f).heightIn(min = 46.dp).testTag("confirm_save_employee_btn")
                    ) {
                        Text(if (employee == null) tr("hr.save_employee_btn") else tr("hr.update_employee_btn"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.height(28.dp), colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldPrimary.copy(alpha = 0.3f)))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayrollAdjustmentDialog(
    employee: HiddenSubAccountEntity,
    onDismiss: () -> Unit,
    onConfirm: (type: String, amount: Double, reason: String, date: String) -> Unit
) {
    var adjType by remember { mutableStateOf("BONUS") }
    var amountStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("hr.adj_dialog_title"), fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${employee.displayName} (${employee.roleTitle})", fontSize = 12.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = adjType == "BONUS",
                        onClick = { adjType = "BONUS" },
                        label = { Text(tr("hr.adj_bonus"), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF10B981), selectedLabelColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = adjType == "DEDUCTION",
                        onClick = { adjType = "DEDUCTION" },
                        label = { Text(tr("hr.adj_deduction"), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFEF4444), selectedLabelColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(trFmt("hr.adj_amount_fmt", "currency" to employee.salaryCurrency)) },
                    placeholder = { Text("500") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("adjustment_amount_input")
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(tr("hr.adj_reason")) },
                    placeholder = { Text(tr("hr.adj_reason_ph")) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onConfirm(adjType, amt, reason.trim(), today)
                        onDismiss()
                    }
                },
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0.0 && reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = if (adjType == "BONUS") Color(0xFF10B981) else Color(0xFFEF4444), contentColor = Color.White)
            ) {
                Text(tr("hr.adj_confirm_btn"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(tr("common.cancel")) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeLeaveDialog(
    employee: HiddenSubAccountEntity,
    onDismiss: () -> Unit,
    onConfirm: (leaveType: String, startDate: String, endDate: String, notes: String) -> Unit
) {
    var leaveType by remember { mutableStateOf("ANNUAL") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("hr.leave_dialog_title"), fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(employee.displayName, fontSize = 12.sp, color = OozoxTeal, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "ANNUAL" to tr("hr.leave_annual"),
                        "SICK" to tr("hr.leave_sick"),
                        "EMERGENCY" to tr("hr.leave_emergency"),
                        "UNPAID" to tr("hr.leave_unpaid")
                    ).forEach { (lKey, lLabel) ->
                        FilterChip(
                            selected = leaveType == lKey,
                            onClick = { leaveType = lKey },
                            label = { Text(lLabel, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text(tr("hr.leave_start")) }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text(tr("hr.leave_end")) }, placeholder = { Text("YYYY-MM-DD") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(tr("hr.leave_notes")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(leaveType, startDate.trim(), endDate.trim(), notes.trim()); onDismiss() },
                enabled = startDate.isNotBlank() && endDate.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
            ) {
                Text(tr("hr.leave_confirm_btn"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(tr("common.cancel")) } }
    )
}
