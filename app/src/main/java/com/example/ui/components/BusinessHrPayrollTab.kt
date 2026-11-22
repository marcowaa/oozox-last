package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Business HR & Payroll Tab: Manage employees, credentials, granular RBAC permissions,
 * base salaries, financial bonuses & deductions, and employee leave date ranges.
 */
@Composable
fun BusinessHrPayrollTab(
    allEmployees: List<HiddenSubAccountEntity>,
    allPayrollAdjustments: List<PayrollAdjustmentEntity>,
    allEmployeeLeaves: List<EmployeeLeaveEntity>,
    onAddEmployeeClick: () -> Unit,
    onEditEmployeeClick: (HiddenSubAccountEntity) -> Unit,
    onAdjustPayrollClick: (HiddenSubAccountEntity) -> Unit,
    onAddLeaveClick: (HiddenSubAccountEntity) -> Unit,
    onDeleteEmployeeClick: (HiddenSubAccountEntity) -> Unit
) {
    val activeEmployees = remember(allEmployees) {
        allEmployees.filter { it.isActiveEmployee }
    }
    val totalBase = remember(activeEmployees) {
        activeEmployees.sumOf { it.baseSalary }
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
                                Text("مسير رواتب الشركة (شهرياً)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${activeEmployees.size} موظف مسجل", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("الرواتب الأساسية: ${GoldUtils.formatCurrency(totalBase)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+ حوافز: ${GoldUtils.formatCurrency(totalBonuses)}", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        Text("- خصومات: ${GoldUtils.formatCurrency(totalDeductions)}", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Add Employee Button
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

        // 3. Employees List
        if (activeEmployees.isEmpty()) {
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
                        Text("لا يوجد موظفون مضافون بعد", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("أضف موظفيك وحدد بياناتهم وصلاحياتهم ورواتبهم ومواقع عملهم.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(activeEmployees) { employee ->
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

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_card_${employee.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                        .background(OozoxTeal.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(22.dp))
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
                                    text = GoldUtils.formatCurrency(empNet),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                                Text(
                                    text = "أساسي: ${GoldUtils.formatCurrency(employee.baseSalary)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Status badges (Leave & Geofence)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (activeLeave != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "🏖️ في إجازة معتمدة حتى ${activeLeave.endDate}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (employee.geofenceEnforced) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = OozoxTeal.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "📍 ملزم بنطاق الـ GPS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OozoxTeal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onEditEmployeeClick(employee) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 36.dp)
                            ) {
                                Text("تعديل", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = { onAdjustPayrollClick(employee) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1.2f).heightIn(min = 36.dp)
                            ) {
                                Text(tr("hr.bonus_deduction_btn"), fontSize = 11.sp, color = GoldAccent)
                            }

                            OutlinedButton(
                                onClick = { onAddLeaveClick(employee) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1.1f).heightIn(min = 36.dp)
                            ) {
                                Text(tr("hr.leave_btn"), fontSize = 11.sp, color = OozoxTeal)
                            }

                            IconButton(
                                onClick = { onDeleteEmployeeClick(employee) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
