package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.util.trFmt

/**
 * Business Overview Tab: Executive Dashboard covering All Projects,
 * All Accounts/Hashtags, Daily Ledger, Invoices, Payroll, Staff Headcount, and Company Staff Group.
 */
@Composable
fun BusinessOverviewTab(
    businessProfile: BusinessProfileEntity?,
    allProjects: List<ProjectEntity>,
    allHashtagAccounts: List<HashtagAccountEntity>,
    allHashtagEntries: List<HashtagEntryEntity> = emptyList(),
    allDailyRecords: List<DailyAccountingRecordEntity> = emptyList(),
    allInvoices: List<InvoiceEntity>,
    allSubAccounts: List<HiddenSubAccountEntity>,
    allPayrollAdjustments: List<PayrollAdjustmentEntity>,
    allAttendanceRecords: List<EmployeeAttendanceEntity>,
    canManageAccounting: Boolean = true,
    canAssignTasks: Boolean = true,
    onOpenProject: (String) -> Unit,
    onOpenCompanyAccounting: () -> Unit,
    onOpenCompanyGroupChat: () -> Unit,
    onAssignTaskToGroup: () -> Unit,
    onSwitchToTab: (Int) -> Unit
) {
    val activeEmployees = remember(allSubAccounts) { allSubAccounts.filter { it.isActiveEmployee } }
    val activeStaffCount = activeEmployees.size

    val totalProjectsCapital = remember(allProjects) { allProjects.sumOf { it.estimatedCapital } }
    val totalInvoicesRevenue = remember(allInvoices) { allInvoices.sumOf { it.totalAmount } }
    val paidInvoices = remember(allInvoices) { allInvoices.count { it.status == InvoiceStatus.PAID.name } }
    val pendingInvoices = remember(allInvoices) { allInvoices.count { it.status == InvoiceStatus.PENDING.name } }
    val overdueInvoices = remember(allInvoices) { allInvoices.count { it.status == InvoiceStatus.OVERDUE.name } }

    val totalHashtagsBalance = remember(allHashtagEntries) {
        allHashtagEntries.sumOf { if (it.direction == "IN") it.amount else -it.amount }
    }
    val hashtagBalances = remember(allHashtagAccounts, allHashtagEntries) {
        allHashtagAccounts.map { acct ->
            val net = allHashtagEntries
                .filter { it.hashtagId == acct.id }
                .sumOf { if (it.direction == "IN") it.amount else -it.amount }
            acct to net
        }
    }
    val dailyInflow = remember(allDailyRecords) {
        allDailyRecords.sumOf { it.incomingAmount }
    }
    val dailyOutflow = remember(allDailyRecords) {
        allDailyRecords.sumOf { it.outgoingAmount }
    }

    val totalBaseSalaries = remember(activeEmployees) { activeEmployees.sumOf { it.baseSalary } }
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

    val activeLang = com.example.util.LocalAppLanguage.current
    val defaultSubtitle = tr("biz.default_subtitle")
    val resolvedSectorLabel = remember(businessProfile, activeLang, defaultSubtitle) {
        val sectorEnum = BusinessActivitySector.entries.firstOrNull { it.name == businessProfile?.activityType }
        if (sectorEnum == BusinessActivitySector.CUSTOM) {
            businessProfile?.customActivityLabel?.ifBlank { sectorEnum.localizedLabel(activeLang) } ?: sectorEnum.localizedLabel(activeLang)
        } else {
            sectorEnum?.localizedLabel(activeLang) ?: defaultSubtitle
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("business_overview_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Prompt user to complete Company & HQ GPS Setup if not configured yet
        if (businessProfile == null || !businessProfile.isSetupCompleted) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.14f)),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = tr("biz.setup_incomplete_title"),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = tr("biz.setup_incomplete_desc"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSwitchToTab(1) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 42.dp).testTag("complete_setup_cta_btn")
                        ) {
                            Text(tr("biz.complete_setup_btn"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = businessProfile?.companyName?.ifBlank { tr("biz.default_company_name") }
                                    ?: tr("biz.default_company_name"),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = resolvedSectorLabel,
                                fontSize = 11.5.sp,
                                color = OozoxTeal,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldPrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = tr("biz.official_enterprise_badge"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExecutiveKpiChip(
                            icon = Icons.Default.WorkOutline,
                            iconTint = OozoxTeal,
                            label = tr("biz.kpi_projects"),
                            value = trFmt("biz.kpi_projects_count_fmt", "count" to allProjects.size),
                            subValue = GoldUtils.formatCurrency(totalProjectsCapital),
                            modifier = Modifier.weight(1f)
                        )
                        ExecutiveKpiChip(
                            icon = Icons.Default.AccountBalanceWallet,
                            iconTint = GoldPrimary,
                            label = tr("biz.kpi_invoices"),
                            value = trFmt("biz.kpi_invoices_count_fmt", "count" to allInvoices.size),
                            subValue = GoldUtils.formatCurrency(totalInvoicesRevenue),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExecutiveKpiChip(
                            icon = Icons.Default.People,
                            iconTint = Color(0xFF3B82F6),
                            label = tr("biz.kpi_staff"),
                            value = trFmt("biz.kpi_staff_count_fmt", "count" to activeStaffCount),
                            subValue = trFmt("biz.kpi_net_payroll_fmt", "amount" to GoldUtils.formatCurrency(netPayroll)),
                            modifier = Modifier.weight(1f)
                        )
                        ExecutiveKpiChip(
                            icon = Icons.Default.MyLocation,
                            iconTint = if (violationsTodayCount > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                            label = tr("biz.kpi_gps_attendance"),
                            value = trFmt("biz.kpi_present_fmt", "count" to presentTodayCount),
                            subValue = if (violationsTodayCount > 0) {
                                trFmt("biz.kpi_violations_fmt", "count" to violationsTodayCount)
                            } else {
                                tr("biz.kpi_compliant_ok")
                            },
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
                                text = businessProfile?.companyGroupName?.ifBlank { tr("hr.staff_group_title") }
                                    ?: tr("hr.staff_group_title"),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = trFmt("biz.group_banner_desc_fmt", "count" to activeStaffCount),
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onOpenCompanyGroupChat,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 38.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("biz.group_open_short"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        if (canAssignTasks) {
                            OozoxCircleIconButton(
                                onClick = onAssignTaskToGroup,
                                imageVector = Icons.Default.AddTask,
                                contentDescription = tr("hr.assign_task_group"),
                                tint = GoldPrimary,
                                containerColor = GoldPrimary.copy(alpha = 0.2f)
                            )
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
                    text = trFmt("biz.projects_count_fmt", "count" to allProjects.size),
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
                        Text(tr("biz.no_projects"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(allProjects) { project ->
                val projNet = project.totalIncome - project.totalExpenses
                Surface(
                    onClick = { onOpenProject(project.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
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
                                    text = trFmt(
                                        "biz.project_finance_fmt",
                                        "cap" to GoldUtils.formatCurrency(project.estimatedCapital),
                                        "inc" to GoldUtils.formatCurrency(project.totalIncome),
                                        "exp" to GoldUtils.formatCurrency(project.totalExpenses),
                                        "net" to GoldUtils.formatCurrency(projNet)
                                    ),
                                    fontSize = 10.5.sp,
                                    color = if (projNet >= 0.0) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFEF4444),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // 4. All Accounts, Ledgers, Hashtags & Invoices Hub
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

                if (canManageAccounting) {
                    TextButton(
                        onClick = onOpenCompanyAccounting,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(tr("biz.open_accounting_hub"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OozoxTeal)
                    }
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
                    AccountMetricRow(
                        label = tr("biz.hashtags_balance"),
                        value = GoldUtils.formatCurrency(totalHashtagsBalance),
                        valueColor = if (totalHashtagsBalance >= 0.0) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                    AccountMetricRow(
                        label = tr("biz.daily_inflow"),
                        value = "+ ${GoldUtils.formatCurrency(dailyInflow)}",
                        valueColor = Color(0xFF10B981)
                    )
                    AccountMetricRow(
                        label = tr("biz.daily_outflow"),
                        value = "- ${GoldUtils.formatCurrency(dailyOutflow)}",
                        valueColor = Color(0xFFEF4444)
                    )
                    AccountMetricRow(
                        label = tr("biz.invoices_total_label"),
                        value = GoldUtils.formatCurrency(totalInvoicesRevenue),
                        valueColor = GoldPrimary
                    )
                    Text(
                        text = trFmt("biz.invoices_status_fmt", "paid" to paidInvoices, "pending" to pendingInvoices, "overdue" to overdueInvoices),
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    AccountMetricRow(
                        label = tr("biz.net_payroll_label"),
                        value = GoldUtils.formatCurrency(netPayroll),
                        valueColor = Color(0xFF3B82F6)
                    )

                    if (hashtagBalances.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        Text(
                            text = tr("biz.hashtag_accounts_list"),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        hashtagBalances.take(6).forEach { (acct, bal) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${acct.tag}",
                                    fontSize = 11.5.sp,
                                    color = OozoxTeal,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = GoldUtils.formatCurrency(bal),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bal >= 0.0) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Navigation Tiles
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onSwitchToTab(1) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                ) {
                    Text(tr("biz.tab_company"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onSwitchToTab(2) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.White),
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                ) {
                    Text(tr("biz.tab_hr"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AccountMetricRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
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
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                Text(label, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subValue, fontSize = 10.sp, color = iconTint, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
