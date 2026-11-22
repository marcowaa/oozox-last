package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.GoldUtils
import com.example.util.tr

/**
 * Business Overview Tab: Executive Dashboard covering All Projects,
 * All Accounts/Hashtags, Staff Headcount, and Company Staff Group.
 */
@Composable
fun BusinessOverviewTab(
    businessProfile: BusinessProfileEntity?,
    allProjects: List<ProjectEntity>,
    allHashtagAccounts: List<HashtagAccountEntity>,
    allHashtagEntries: List<HashtagEntryEntity> = emptyList(),
    allInvoices: List<InvoiceEntity>,
    allSubAccounts: List<HiddenSubAccountEntity>,
    allPayrollAdjustments: List<PayrollAdjustmentEntity>,
    allAttendanceRecords: List<EmployeeAttendanceEntity>,
    onOpenProject: (String) -> Unit,
    onOpenCompanyAccounting: () -> Unit,
    onOpenCompanyGroupChat: () -> Unit,
    onAssignTaskToGroup: () -> Unit,
    onSwitchToTab: (Int) -> Unit
) {
    val activeStaffCount = remember(allSubAccounts) {
        allSubAccounts.count { it.isActiveEmployee }
    }
    val totalProjectsCapital = remember(allProjects) {
        allProjects.sumOf { it.estimatedCapital }
    }
    val totalInvoicesRevenue = remember(allInvoices) {
        allInvoices.sumOf { it.totalAmount }
    }
    val totalHashtagsBalance = remember(allHashtagEntries) {
        allHashtagEntries.sumOf { if (it.direction == "IN") it.amount else -it.amount }
    }
    val totalBaseSalaries = remember(allSubAccounts) {
        allSubAccounts.filter { it.isActiveEmployee }.sumOf { it.baseSalary }
    }
    val totalBonuses = remember(allPayrollAdjustments) {
        allPayrollAdjustments.filter { it.type == "BONUS" }.sumOf { it.amount }
    }
    val totalDeductions = remember(allPayrollAdjustments) {
        allPayrollAdjustments.filter { it.type == "DEDUCTION" }.sumOf { it.amount }
    }
    val netPayroll = (totalBaseSalaries + totalBonuses - totalDeductions).coerceAtLeast(0.0)

    val presentTodayCount = remember(allAttendanceRecords) {
        allAttendanceRecords.count { it.status == "CLOCKED_IN" && it.isLocationVerified }
    }
    val violationsTodayCount = remember(allAttendanceRecords) {
        allAttendanceRecords.count { it.status == "OUT_OF_BOUNDS" || it.status == "GPS_DISABLED" || it.locationPolicyViolated }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("business_overview_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Executive Hero KPIs Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = businessProfile?.companyName?.ifBlank { "منظومة الشركة والمؤسسة" } ?: "منظومة الشركة والمؤسسة",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = businessProfile?.customActivityLabel?.ifBlank {
                                    businessProfile.activityType
                                } ?: "إدارة المشاريع والحسابات والكوادر البشرية",
                                fontSize = 11.5.sp,
                                color = OozoxTeal
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldPrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "مؤسسة رسمية 🏢",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // KPIs Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExecutiveKpiChip(
                            icon = Icons.Default.WorkOutline,
                            iconTint = OozoxTeal,
                            label = "المشاريع الجارية",
                            value = "${allProjects.size} مشاريع",
                            subValue = GoldUtils.formatCurrency(totalProjectsCapital),
                            modifier = Modifier.weight(1f)
                        )
                        ExecutiveKpiChip(
                            icon = Icons.Default.AccountBalanceWallet,
                            iconTint = GoldPrimary,
                            label = "إجمالي الفواتير",
                            value = "${allInvoices.size} فاتورة",
                            subValue = GoldUtils.formatCurrency(totalInvoicesRevenue),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExecutiveKpiChip(
                            icon = Icons.Default.People,
                            iconTint = Color(0xFF3B82F6),
                            label = "الموظفون والرواتب",
                            value = "$activeStaffCount موظف",
                            subValue = "صافي: ${GoldUtils.formatCurrency(netPayroll)}",
                            modifier = Modifier.weight(1f)
                        )
                        ExecutiveKpiChip(
                            icon = Icons.Default.MyLocation,
                            iconTint = if (violationsTodayCount > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                            label = "حضور الـ GPS",
                            value = "$presentTodayCount حاضر",
                            subValue = if (violationsTodayCount > 0) "🚨 $violationsTodayCount مخالفة" else "✓ النطاق ملتزم",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Company Staff Group Quick Banner
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = OozoxTeal.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(OozoxTeal.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(
                                text = businessProfile?.companyGroupName?.ifBlank { "مجموعة كل الموظفين" } ?: "مجموعة كل الموظفين",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مجموعة موحدة تشمل $activeStaffCount موظف لإسناد المهام والتنبيهات المباشرة",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onOpenCompanyGroupChat,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 38.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المجموعة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = onAssignTaskToGroup,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.AddTask, contentDescription = "إسناد مهمة", tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // 3. All Projects & Operations Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.AccountTree, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                    Text(
                        text = tr("biz.all_projects_title"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${allProjects.size} مشاريع مسجلة",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (allProjects.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                        Text("لا توجد مشاريع مضافة حالياً", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(allProjects) { project ->
                Surface(
                    onClick = { onOpenProject(project.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("project_item_${project.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
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
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GoldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Apartment, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                            }

                            Column {
                                Text(
                                    text = project.name,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${project.type} • رأس المال: ${GoldUtils.formatCurrency(project.estimatedCapital)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // 4. All Accounts, Ledgers & Hashtags Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(18.dp))
                    Text(
                        text = tr("biz.all_accounts_title"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = onOpenCompanyAccounting,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("فتح المركز المحاسبي ↗", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = OozoxTeal)
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("رصيد حسابات الهاشتاج (#):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = GoldUtils.formatCurrency(totalHashtagsBalance),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalHashtagsBalance >= 0.0) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إجمالي الفواتير الصادرة:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = GoldUtils.formatCurrency(totalInvoicesRevenue),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مسير الرواتب الصافي (شهرياً):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = GoldUtils.formatCurrency(netPayroll),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                    }
                }
            }
        }

        // Quick Navigation Tiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onSwitchToTab(1) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                ) {
                    Text("بيانات النشاط 🏢", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onSwitchToTab(2) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.White),
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                ) {
                    Text("إدارة الموظفين 👥", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExecutiveKpiChip(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    subValue: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.8.dp, iconTint.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                Text(label, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subValue, fontSize = 10.sp, color = iconTint, fontWeight = FontWeight.Medium)
        }
    }
}
