package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OozoxTeal
import com.example.util.tr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Add / Edit Enterprise Employee Dialog with Username, Password, Role,
 * Base Salary, Mandatory Geofence toggle, and 10 Granular RBAC Permissions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEmployeeDialog(
    employee: HiddenSubAccountEntity? = null,
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
    var roleTitle by remember { mutableStateOf(employee?.roleTitle?.ifBlank { "موظف" } ?: "موظف") }
    var department by remember { mutableStateOf(employee?.department?.ifBlank { "العمليات والمشاريع" } ?: "العمليات والمشاريع") }
    var baseSalaryStr by remember { mutableStateOf(if ((employee?.baseSalary ?: 0.0) > 0) employee?.baseSalary?.toInt().toString() else "6000") }
    var salaryCurrency by remember { mutableStateOf(employee?.salaryCurrency?.ifBlank { "EGP" } ?: "EGP") }
    var geofenceEnforced by remember { mutableStateOf(employee?.geofenceEnforced ?: true) }

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
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PersonAddAlt1, contentDescription = null, tint = GoldPrimary)
                        Text(
                            text = if (employee == null) tr("hr.add_employee") else "تعديل بيانات وصلاحيات الموظف",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = tr("common.close"))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text(tr("hr.full_name")) },
                        placeholder = { Text("مثال: م/ كريم عبد العزيز") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("employee_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it.filter { c -> c.isLetterOrDigit() || c == '_' }.lowercase() },
                            label = { Text(tr("hr.username")) },
                            placeholder = { Text("kareem_2026") },
                            leadingIcon = { Text("@", fontWeight = FontWeight.Bold, color = GoldPrimary) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("employee_username_input")
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("الهاتف") },
                            placeholder = { Text("010XXXXXXXX") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(tr("hr.password")) },
                        placeholder = { Text("كلمة المرور الخاصة بدخول الموظف") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("employee_password_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = roleTitle,
                            onValueChange = { roleTitle = it },
                            label = { Text(tr("hr.role_title")) },
                            placeholder = { Text("مهندس تنفيذ / محاسب") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text(tr("hr.department")) },
                            placeholder = { Text("إدارة المشاريع") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = baseSalaryStr,
                            onValueChange = { baseSalaryStr = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(tr("hr.base_salary")) },
                            placeholder = { Text("6000") },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f).testTag("employee_salary_input")
                        )
                        OutlinedTextField(
                            value = salaryCurrency,
                            onValueChange = { salaryCurrency = it.uppercase() },
                            label = { Text("العملة") },
                            singleLine = true,
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    // Mandatory Geofence Check-in Toggle
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
                                Text("إلزام الموظف بموقع العمل (Geofencing)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("يجبر الموظف على تفعيل GPS والبقاء داخل نطاق المقر", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = geofenceEnforced,
                                onCheckedChange = { geofenceEnforced = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = OozoxTeal, checkedTrackColor = OozoxTeal.copy(alpha = 0.3f))
                            )
                        }
                    }

                    // Granular RBAC Permissions Section
                    Text(
                        text = tr("hr.permissions_title"),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )

                    PermissionToggleRow("إدارة الشركة وإعدادات النشاط", permManageBusiness) { permManageBusiness = it }
                    PermissionToggleRow("إدارة الموظفين والكوادر البشرية", permManageStaff) { permManageStaff = it }
                    PermissionToggleRow("إدارة مسير الرواتب والحوافز والخصومات", permManagePayroll) { permManagePayroll = it }
                    PermissionToggleRow("إدارة المشاريع وإسناد العمل الميداني", permManageProjects) { permManageProjects = it }
                    PermissionToggleRow("إسناد المهام في مجموعات العمل", permAssignTasks) { permAssignTasks = it }
                    PermissionToggleRow("إدارة الدفاتر والقيود المحاسبية", permManageAccounting) { permManageAccounting = it }
                    PermissionToggleRow("إصدار وطباعة الفواتير الرسمية", canCreateInvoices) { canCreateInvoices = it }
                    PermissionToggleRow("اعتماد الخصومات والأسعار المخصصة", permApproveDiscounts) { permApproveDiscounts = it }
                    PermissionToggleRow("عرض التقارير المالية واليومية", canViewReports) { canViewReports = it }
                    PermissionToggleRow("التواصل والمراسلات المباشرة", permDirectChat) { permDirectChat = it }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 46.dp)
                    ) {
                        Text(tr("common.cancel"))
                    }

                    Button(
                        onClick = {
                            val salary = baseSalaryStr.toDoubleOrNull() ?: 0.0
                            onSave(
                                employee?.id,
                                fullName.trim(),
                                username.trim(),
                                phone.trim(),
                                password.trim(),
                                roleTitle.trim(),
                                department.trim(),
                                salary,
                                salaryCurrency.trim(),
                                geofenceEnforced,
                                canCreateInvoices,
                                canViewReports,
                                permApproveDiscounts,
                                permManageBusiness,
                                permManageStaff,
                                permManagePayroll,
                                permManageProjects,
                                permAssignTasks,
                                permManageAccounting,
                                permApproveDiscounts,
                                permDirectChat
                            )
                            onDismiss()
                        },
                        enabled = fullName.isNotBlank() && username.isNotBlank() && (password.isNotBlank() || employee != null),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        modifier = Modifier.weight(1.8f).heightIn(min = 46.dp).testTag("confirm_save_employee_btn")
                    ) {
                        Text(if (employee == null) "حفظ الموظف وتفعيل الصلاحيات ✓" else "تحديث البيانات والصلاحيات ✓", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 11.5.sp)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.height(28.dp),
                colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldPrimary.copy(alpha = 0.3f))
            )
        }
    }
}

/**
 * Bonus or Deduction Adjustment Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayrollAdjustmentDialog(
    employee: HiddenSubAccountEntity,
    onDismiss: () -> Unit,
    onConfirm: (type: String, amount: Double, reason: String, date: String) -> Unit
) {
    var adjType by remember { mutableStateOf("BONUS") } // BONUS or DEDUCTION
    var amountStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("حافز / خصم مالي للموظف", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الموظف: ${employee.displayName} (${employee.roleTitle})", fontSize = 12.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = adjType == "BONUS",
                        onClick = { adjType = "BONUS" },
                        label = { Text("حافز ومكافأة (+)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF10B981), selectedLabelColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = adjType == "DEDUCTION",
                        onClick = { adjType = "DEDUCTION" },
                        label = { Text("خصم وجزاء (-)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFEF4444), selectedLabelColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("المبلغ المالي (${employee.salaryCurrency})") },
                    placeholder = { Text("500") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("adjustment_amount_input")
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب الحافز أو الخصم") },
                    placeholder = { Text("مثال: مكافأة إنجاز مشروع / تأخير متكرر") },
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (adjType == "BONUS") Color(0xFF10B981) else Color(0xFFEF4444),
                    contentColor = Color.White
                )
            ) {
                Text("اعتماد الحركة المالية", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(tr("common.cancel"))
            }
        }
    )
}

/**
 * Employee Leave Date Range Dialog
 */
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
        title = {
            Text("تسجيل وتحديد إجازة للموظف", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الموظف: ${employee.displayName}", fontSize = 12.sp, color = OozoxTeal, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "ANNUAL" to "سنوية",
                        "SICK" to "مرضية",
                        "EMERGENCY" to "عارضة",
                        "UNPAID" to "بدون راتب"
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
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("تاريخ البدء") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("تاريخ الانتهاء") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الإجازة") },
                    placeholder = { Text("موافقة الإدارة...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(leaveType, startDate.trim(), endDate.trim(), notes.trim())
                    onDismiss()
                },
                enabled = startDate.isNotBlank() && endDate.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
            ) {
                Text("اعتماد الإجازة ✓", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(tr("common.cancel"))
            }
        }
    )
}
