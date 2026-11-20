package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FinancialComplaintEntity
import com.example.data.model.HiddenSubAccountEntity
import com.example.data.model.ShiftHandoverEntity
import com.example.ui.theme.*
import com.example.util.GoldUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Share & export the retroactive balance & shift audit statement formatted in Arabic
 */
fun shareLedgerReport(
    context: Context,
    shifts: List<ShiftHandoverEntity>,
    complaints: List<FinancialComplaintEntity>,
    totalActualCash: Double,
    totalRevenue: Double,
    netBalance: Double,
    totalDeficits: Double
) {
    val reportDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    val sb = StringBuilder()
    sb.appendLine("═══════════════════════════════")
    sb.appendLine("💼 تقرير كشف الرصيد والورديات - OOZOX")
    sb.appendLine("═══════════════════════════════")
    sb.appendLine("📅 تاريخ التقرير: $reportDate")
    sb.appendLine("📊 ملخص التدقيق المالي العام:")
    sb.appendLine("• إجمالي النقد الفعلي بالخزينة: ${GoldUtils.formatCurrency(totalActualCash)}")
    sb.appendLine("• إجمالي المبيعات المحققة: ${GoldUtils.formatCurrency(totalRevenue)}")
    sb.appendLine("• إجمالي العجز والخصومات: ${GoldUtils.formatCurrency(totalDeficits)}")
    sb.appendLine("• صافي الرصيد المستحق: ${GoldUtils.formatCurrency(netBalance)}")
    sb.appendLine("• عدد الورديات: ${shifts.size} (${shifts.count { it.isReceived }} مستلمة، ${shifts.count { !it.isReceived }} بانتظار الاعتماد)")
    sb.appendLine("• عدد الشكاوى المالية: ${complaints.size}")
    sb.appendLine("───────────────────────────────")
    sb.appendLine("📋 تفاصيل الورديات الأخيرة:")
    shifts.take(20).forEachIndexed { idx, shift ->
        val sDate = if (shift.shiftDateText.isNotBlank()) shift.shiftDateText else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(shift.logoutTime))
        val status = if (shift.isReceived) "مستلم ومطابق ✓" else "بانتظار الاستلام ⏳"
        sb.appendLine("${idx + 1}. الكاشير: ${shift.cashierName} ($sDate)")
        sb.appendLine("   الحالة: $status | النقد: ${GoldUtils.formatCurrency(shift.actualCash)} | المبيعات: ${GoldUtils.formatCurrency(shift.totalRevenueEGP)}")
        if (shift.deficitAmount > 0) {
            sb.appendLine("   ⚠️ عجز مسجل: ${GoldUtils.formatCurrency(shift.deficitAmount)}")
        }
        if (shift.isReceived && !shift.receivedByCashierName.isNullOrBlank()) {
            sb.appendLine("   المستلم: ${shift.receivedByCashierName} (${shift.receivingNotes ?: "سليم"})")
        }
    }
    if (complaints.isNotEmpty()) {
        sb.appendLine("───────────────────────────────")
        sb.appendLine("⚠️ الشكاوى والخصومات المالية:")
        complaints.take(10).forEachIndexed { idx, c ->
            sb.appendLine("${idx + 1}. @${c.accusedEmployeeName ?: c.submittedByStaffName}: ${c.title} - ${GoldUtils.formatCurrency(c.claimAmount)} (${c.employeeResponseStatus})")
        }
    }
    sb.appendLine("═══════════════════════════════")
    sb.appendLine("منظومة OOZOX المحاسبية لإدارة الأعمال")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "كشف رصيد الورديات والعهد - OOZOX")
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة كشف الرصيد"))
}

/**
 * Dedicated, full-screen Dialog for Retroactive Employee Ledger and Audit Statement,
 * accessible directly from the Business Side Navigation Drawer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroactiveEmployeeLedgerDialog(
    allSubAccounts: List<HiddenSubAccountEntity>,
    allShiftHandovers: List<ShiftHandoverEntity>,
    allComplaints: List<FinancialComplaintEntity>,
    activeSubAccount: HiddenSubAccountEntity?,
    onReceiveShift: (handoverId: String, receiverId: String, receiverName: String, notes: String?) -> Unit,
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
    onRespondToComplaint: (complaintId: String, accepted: Boolean, reason: String?, proofUri: String?) -> Unit,
    onDecideOnComplaint: (complaintId: String, decision: String, notes: String?) -> Unit,
    onApplyJointDeduction: (complaintId: String, primaryAmount: Double, receiverAmount: Double, receiverId: String?, notes: String?) -> Unit = { _, _, _, _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val totalActualCash = remember(allShiftHandovers) { allShiftHandovers.sumOf { it.actualCash } }
    val totalRevenue = remember(allShiftHandovers) { allShiftHandovers.sumOf { it.totalRevenueEGP } }
    val totalDeficits = remember(allShiftHandovers) { allShiftHandovers.sumOf { it.deficitAmount } }
    val netBalance = remember(allShiftHandovers, allComplaints) {
        val compDeductions = allComplaints.filter { it.isDeductedFromBalance || it.employeeResponseStatus == "ACCEPTED" }.sumOf { it.claimAmount }
        totalActualCash - totalDeficits - compDeductions
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(
                                    text = "كشف الرصيد والعهد بأثر رجعي",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${allShiftHandovers.size} وردية مسجلة • ${allComplaints.size} شكوى ومطالبة",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                shareLedgerReport(
                                    context = context,
                                    shifts = allShiftHandovers,
                                    complaints = allComplaints,
                                    totalActualCash = totalActualCash,
                                    totalRevenue = totalRevenue,
                                    netBalance = netBalance,
                                    totalDeficits = totalDeficits
                                )
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.15f))
                                .testTag("share_retroactive_ledger_btn")
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "مشاركة الكشف",
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Main Ledger Scrollable Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    RetroactiveEmployeeLedgerSection(
                        allSubAccounts = allSubAccounts,
                        allShiftHandovers = allShiftHandovers,
                        allComplaints = allComplaints,
                        activeSubAccount = activeSubAccount,
                        onReceiveShift = onReceiveShift,
                        onReceiveShiftFull = onReceiveShiftFull,
                        onRespondToComplaint = onRespondToComplaint,
                        onDecideOnComplaint = onDecideOnComplaint,
                        onApplyJointDeduction = onApplyJointDeduction,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Section for viewing employee / agent balance retroactively by date and shift handover date,
 * shift receiving actions, and complaints with @mentions and responses.
 * Enhanced with modern Material 3 cards, filter tabs, executive metrics, and search.
 */
@Composable
fun RetroactiveEmployeeLedgerSection(
    allSubAccounts: List<HiddenSubAccountEntity>,
    allShiftHandovers: List<ShiftHandoverEntity>,
    allComplaints: List<FinancialComplaintEntity>,
    activeSubAccount: HiddenSubAccountEntity?,
    onReceiveShift: (handoverId: String, receiverId: String, receiverName: String, notes: String?) -> Unit,
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
    onRespondToComplaint: (complaintId: String, accepted: Boolean, reason: String?, proofUri: String?) -> Unit,
    onDecideOnComplaint: (complaintId: String, decision: String, notes: String?) -> Unit,
    onApplyJointDeduction: (complaintId: String, primaryAmount: Double, receiverAmount: Double, receiverId: String?, notes: String?) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: الكل, 1: بانتظار الاستلام ⏳, 2: مستلم ومطابق ✓, 3: شكاوى وخصومات ⚠️
    var searchQuery by remember { mutableStateOf("") }
    var selectedEmployeeFilter by remember { mutableStateOf<String?>("الكل") }
    var selectedDateFilter by remember { mutableStateOf("الكل") }

    var shiftToReceive by remember { mutableStateOf<ShiftHandoverEntity?>(null) }
    var complaintToRespond by remember { mutableStateOf<FinancialComplaintEntity?>(null) }
    var complaintToReviewByManager by remember { mutableStateOf<FinancialComplaintEntity?>(null) }
    var complaintForForensic by remember { mutableStateOf<FinancialComplaintEntity?>(null) }

    val distinctDates = remember(allShiftHandovers) {
        val dates = allShiftHandovers.map {
            if (it.shiftDateText.isNotBlank()) it.shiftDateText
            else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.logoutTime))
        }.distinct().sortedDescending()
        listOf("الكل") + dates
    }

    val pendingCount = remember(allShiftHandovers) { allShiftHandovers.count { !it.isReceived } }
    val receivedCount = remember(allShiftHandovers) { allShiftHandovers.count { it.isReceived } }

    // Filtered shifts
    val filteredShifts = remember(allShiftHandovers, selectedEmployeeFilter, selectedDateFilter, selectedTab, searchQuery) {
        allShiftHandovers.filter { shift ->
            val matchTab = when (selectedTab) {
                1 -> !shift.isReceived
                2 -> shift.isReceived
                3 -> false // in complaints tab we don't list shift cards
                else -> true
            }
            val matchEmp = selectedEmployeeFilter == null || selectedEmployeeFilter == "الكل" || shift.cashierName == selectedEmployeeFilter
            val shiftDate = if (shift.shiftDateText.isNotBlank()) shift.shiftDateText else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(shift.logoutTime))
            val matchDate = selectedDateFilter == "الكل" || shiftDate == selectedDateFilter
            val matchSearch = searchQuery.isBlank() || shift.cashierName.contains(searchQuery, ignoreCase = true) ||
                    (shift.notes?.contains(searchQuery, ignoreCase = true) == true) || shiftDate.contains(searchQuery)
            matchTab && matchEmp && matchDate && matchSearch
        }
    }

    // Filtered complaints
    val filteredComplaints = remember(allComplaints, selectedEmployeeFilter, selectedDateFilter, selectedTab, searchQuery) {
        if (selectedTab == 1 || selectedTab == 2) {
            emptyList()
        } else {
            allComplaints.filter { comp ->
                val matchEmp = selectedEmployeeFilter == null || selectedEmployeeFilter == "الكل" ||
                        comp.accusedEmployeeName == selectedEmployeeFilter || comp.submittedByStaffName == selectedEmployeeFilter
                val matchDate = selectedDateFilter == "الكل" || comp.targetDate == selectedDateFilter
                val matchSearch = searchQuery.isBlank() || comp.title.contains(searchQuery, ignoreCase = true) ||
                        comp.details.contains(searchQuery, ignoreCase = true) || (comp.accusedEmployeeName?.contains(searchQuery, ignoreCase = true) == true)
                matchEmp && matchDate && matchSearch
            }
        }
    }

    // Dynamic executive calculations
    val totalActualCash = remember(filteredShifts) { filteredShifts.sumOf { it.actualCash } }
    val totalRevenue = remember(filteredShifts) { filteredShifts.sumOf { it.totalRevenueEGP } }
    val totalDeficits = remember(filteredShifts) { filteredShifts.sumOf { it.deficitAmount } }
    val totalInvoices = remember(filteredShifts) { filteredShifts.sumOf { it.totalInvoicesCount } }
    val totalComplaintsDeductions = remember(filteredComplaints) {
        filteredComplaints.filter { it.isDeductedFromBalance || it.employeeResponseStatus == "ACCEPTED" }.sumOf { it.claimAmount }
    }
    val netTotalBalance = totalActualCash - totalDeficits - totalComplaintsDeductions

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Executive Summary Financial Matrix (4 M3 Tonal Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LedgerStatCard(
                        title = "النقد بالدرج",
                        value = GoldUtils.formatCurrency(totalActualCash),
                        subtitle = "النقد الفعلي بالخزينة",
                        icon = Icons.Default.Payments,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    LedgerStatCard(
                        title = "إجمالي المبيعات",
                        value = GoldUtils.formatCurrency(totalRevenue),
                        subtitle = "$totalInvoices فاتورة صادرة",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = GoldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LedgerStatCard(
                        title = "صافي الاستحقاق",
                        value = GoldUtils.formatCurrency(netTotalBalance),
                        subtitle = "الرصيد المالي المعتمد",
                        icon = Icons.Default.AccountBalance,
                        accentColor = if (netTotalBalance >= 0) adaptiveGoldPrimary else adaptiveStatusRed,
                        modifier = Modifier.weight(1f)
                    )
                    LedgerStatCard(
                        title = "العجز والخصومات",
                        value = GoldUtils.formatCurrency(totalDeficits + totalComplaintsDeductions),
                        subtitle = "عجز ورديات وشكاوى",
                        icon = Icons.Default.Warning,
                        accentColor = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Interactive Navigation Tabs (Pill Style)
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LedgerFilterTabItem(
                        title = "الكل",
                        badge = "${allShiftHandovers.size}",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    LedgerFilterTabItem(
                        title = "بانتظار ⏳",
                        badge = "$pendingCount",
                        badgeColor = Color(0xFFF59E0B),
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1.1f)
                    )
                    LedgerFilterTabItem(
                        title = "مستلم ✓",
                        badge = "$receivedCount",
                        badgeColor = Color(0xFF10B981),
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.weight(1.1f)
                    )
                    LedgerFilterTabItem(
                        title = "شكاوى ⚠️",
                        badge = "${allComplaints.size}",
                        badgeColor = Color(0xFFEF4444),
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }
        }

        // 3. Search & Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث باسم الكاشير أو الملاحظات أو التاريخ...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Employee Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الموظف:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    val employeeOptions = listOf("الكل") + allSubAccounts.map { it.displayName }
                    employeeOptions.forEach { emp ->
                        FilterChip(
                            selected = selectedEmployeeFilter == emp,
                            onClick = { selectedEmployeeFilter = emp },
                            label = { Text(emp, fontSize = 11.sp, fontWeight = if (selectedEmployeeFilter == emp) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Date Filter Chips
                if (distinctDates.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التاريخ:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        distinctDates.forEach { dt ->
                            FilterChip(
                                selected = selectedDateFilter == dt,
                                onClick = { selectedDateFilter = dt },
                                label = { Text(dt, fontSize = 10.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccent.copy(alpha = 0.85f),
                                    selectedLabelColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Shifts List
        if (selectedTab != 3) {
            if (filteredShifts.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                            Text(
                                text = "لا توجد ورديات تطابق الفلتر المحدد",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تأكد من اختيار اسم الموظف والتاريخ المناسب أو استلم شفت جديد.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredShifts, key = { it.id }) { shift ->
                    val shiftDate = if (shift.shiftDateText.isNotBlank()) shift.shiftDateText
                    else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(shift.logoutTime))

                    // Find complaints related to this shift / employee / date
                    val relatedComplaints = allComplaints.filter { comp ->
                        comp.targetDate == shiftDate && (comp.accusedEmployeeName == shift.cashierName || comp.submittedByStaffName == shift.cashierName)
                    }
                    val shiftComplaintsDeductions = relatedComplaints.filter { it.isDeductedFromBalance || it.employeeResponseStatus == "ACCEPTED" }
                        .sumOf { it.claimAmount }

                    val netShiftBalance = shift.totalPaidEGP - shiftComplaintsDeductions - shift.deficitAmount

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (shift.isFlaggedToOwner || shift.deficitAmount > 0) Color.Red.copy(alpha = 0.06f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (shift.isFlaggedToOwner || shift.deficitAmount > 0) Color.Red.copy(alpha = 0.45f)
                            else if (shift.isReceived) Color(0xFF10B981).copy(alpha = 0.35f)
                            else GoldPrimary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shift_ledger_card_${shift.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header Row: Avatar, Cashier Name, Date, Status Chip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (shift.isReceived) Color(0xFF10B981).copy(alpha = 0.18f)
                                                else GoldPrimary.copy(alpha = 0.2f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = shift.cashierName.take(1),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (shift.isReceived) Color(0xFF10B981) else GoldPrimary
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = shift.cashierName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "📅 $shiftDate",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (shift.isReceived) Color(0xFF10B981).copy(alpha = 0.15f)
                                    else Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    border = BorderStroke(
                                        0.8.dp,
                                        if (shift.isReceived) Color(0xFF10B981) else Color(0xFFF59E0B)
                                    )
                                ) {
                                    Text(
                                        text = if (shift.isReceived) "مستلم ومطابق ✓" else "بانتظار الاستلام ⏳",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (shift.isReceived) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Time Shift Badge
                            val loginFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(shift.loginTime))
                            val logoutFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(shift.logoutTime))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(13.dp))
                                        Text("وردية العمل:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = "$loginFormatted  -  $logoutFormatted",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // 4-Quadrant Metric Matrix
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("• النقد الفعلي بالدرج:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(GoldUtils.formatCurrency(shift.actualCash), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("• الفواتير المصدرة والمبيعات:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${shift.totalInvoicesCount} فواتير (${GoldUtils.formatCurrency(shift.totalRevenueEGP)})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("• حركة البضاعة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("أُضيف: +${shift.productsAddedCount} | بيع: -${shift.productsSoldCount}", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                        }
                                        if (shiftComplaintsDeductions > 0) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("• خصم شكاوى معتمدة:", fontSize = 11.sp, color = Color.Red)
                                                Text("-${GoldUtils.formatCurrency(shiftComplaintsDeductions)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }

                            // Payment Methods Channel Breakdown Pills if available
                            if (shift.instaPayCollected > 0 || shift.vodafoneCashCollected > 0 || shift.bankCollected > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (shift.instaPayCollected > 0) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF6366F1).copy(alpha = 0.15f)) {
                                            Text("إنستاباي: ${GoldUtils.formatCurrency(shift.instaPayCollected)}", fontSize = 10.sp, color = Color(0xFF6366F1), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    if (shift.vodafoneCashCollected > 0) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFEF4444).copy(alpha = 0.15f)) {
                                            Text("فودافون كاش: ${GoldUtils.formatCurrency(shift.vodafoneCashCollected)}", fontSize = 10.sp, color = Color(0xFFEF4444), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    if (shift.bankCollected > 0) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF0EA5E9).copy(alpha = 0.15f)) {
                                            Text("بنك/فيزا: ${GoldUtils.formatCurrency(shift.bankCollected)}", fontSize = 10.sp, color = Color(0xFF0EA5E9), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }

                            // Deficit warning box
                            if (shift.deficitAmount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Red.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                        Column {
                                            Text(
                                                text = "عجز بالخزينة: ${GoldUtils.formatCurrency(shift.deficitAmount)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Red
                                            )
                                            Text(
                                                text = "تم إشعار مالك المحل تلقائياً وإلزامية إرفاق بيان المطابقة.",
                                                fontSize = 9.5.sp,
                                                color = Color.Red.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Net Shift Balance Strip
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GoldPrimary.copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("صافي رصيد الوردية المالي:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        GoldUtils.formatCurrency(netShiftBalance),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netShiftBalance >= 0) adaptiveGoldPrimary else adaptiveStatusRed
                                    )
                                }
                            }

                            // Shift Receiving Info or Action Button
                            if (shift.isReceived) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.08f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "مستلم من: ${shift.receivedByCashierName ?: "الكاشير التالي"} • ${shift.receivingNotes ?: "سليم ومطابق"}",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF10B981),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { shiftToReceive = shift },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("receive_shift_btn_${shift.id}")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("استلام الوردية ومطابقة الخزينة 📥", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Financial Complaints Section (when Tab is 0 or 3)
        if (selectedTab == 0 || selectedTab == 3) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                                Text(
                                    text = "نظام الشكاوى المالية والخصومات الموجهة ⚠️",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF59E0B).copy(alpha = 0.15f)) {
                                Text(
                                    text = "${filteredComplaints.size} شكوى",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "شكاوى العهد والمبيعات المنشورة بالجروب مع منشن للموظف. القبول يخصم تلقائياً من الرصيد والرفض يتطلب مستند براءة لإشعار الإدارة.",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (filteredComplaints.isEmpty()) {
                            Text(
                                text = "لا توجد شكاوى مالية مسجلة حالياً.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            filteredComplaints.forEach { complaint ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (complaint.employeeResponseStatus) {
                                            "ACCEPTED" -> Color(0xFF10B981).copy(alpha = 0.08f)
                                            "REJECTED" -> Color.Red.copy(alpha = 0.08f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        when (complaint.employeeResponseStatus) {
                                            "ACCEPTED" -> Color(0xFF10B981).copy(alpha = 0.4f)
                                            "REJECTED" -> Color.Red.copy(alpha = 0.4f)
                                            else -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "المشكو في حقه: @${complaint.accusedEmployeeName ?: complaint.submittedByStaffName}",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldPrimary
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = when (complaint.employeeResponseStatus) {
                                                    "ACCEPTED" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                    "REJECTED" -> Color.Red.copy(alpha = 0.15f)
                                                    else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                }
                                            ) {
                                                Text(
                                                    text = when (complaint.employeeResponseStatus) {
                                                        "ACCEPTED" -> "تم القبول وخصم المبلغ ✓"
                                                        "REJECTED" -> "مرفوضة مع مستند ❌"
                                                        else -> "بانتظار رد الموظف ⏳"
                                                    },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (complaint.employeeResponseStatus) {
                                                        "ACCEPTED" -> Color(0xFF10B981)
                                                        "REJECTED" -> Color.Red
                                                        else -> Color(0xFFF59E0B)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("المبلغ المشكو فيه: ${GoldUtils.formatCurrency(complaint.claimAmount)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                            Text("تاريخ الواقعة: ${complaint.targetDate}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Text("سبب الشكوى: ${complaint.title} - ${complaint.details}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)

                                        // Rejection details if rejected
                                        if (complaint.employeeResponseStatus == "REJECTED" && !complaint.employeeRejectionReason.isNullOrBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.Red.copy(alpha = 0.08f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text("📝 سبب رفض الموظف للشكوى: ${complaint.employeeRejectionReason}", fontSize = 10.5.sp, color = Color.Red)
                                                    if (complaint.employeeRejectionProofUri != null) {
                                                        Text("📷 مرفق صورة إثبات الرفض ومستند البراءة ✓", fontSize = 10.sp, color = Color(0xFF10B981))
                                                    }
                                                }
                                            }
                                        }

                                        // Action buttons (>= 48dp minimumInteractiveComponentSize)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (complaint.employeeResponseStatus == "PENDING") {
                                                Button(
                                                    onClick = { complaintToRespond = complaint },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 44.dp)
                                                ) {
                                                    Text("الرد على الشكوى ✍️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = { complaintForForensic = complaint },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 44.dp)
                                            ) {
                                                Icon(Icons.Default.Balance, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("التحقيق الجنائي ⚖️", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = { complaintToReviewByManager = complaint },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 44.dp)
                                            ) {
                                                Text("قرار الإدارة 👨‍💼", fontSize = 11.sp)
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

    // Modal: Comprehensive Shift Custody Handover & Receiving Dialog
    shiftToReceive?.let { shift ->
        ShiftCustodyHandoverDialog(
            handover = shift,
            currentStaffName = activeSubAccount?.displayName ?: "كاشير الشفت المستلم",
            currentStaffId = activeSubAccount?.id ?: "cashier_next",
            onDismiss = { shiftToReceive = null },
            onConfirmReceiving = { hId, rId, rName, notes, merchStatus, stockChecked, cashConfirmed, invoicesCount, signature, acceptType, discNotes ->
                onReceiveShiftFull(
                    hId,
                    rId,
                    rName,
                    notes,
                    merchStatus,
                    stockChecked,
                    cashConfirmed,
                    invoicesCount,
                    signature,
                    acceptType,
                    discNotes
                )
                shiftToReceive = null
            }
        )
    }

    // Modal: Forensic Handover & Joint Liability Investigation
    complaintForForensic?.let { comp ->
        ForensicLiabilityDialog(
            complaint = comp,
            onApplyJointDeduction = { cId, pAmt, rAmt, rId, notes ->
                onApplyJointDeduction(cId, pAmt, rAmt, rId, notes)
                complaintForForensic = null
            },
            onDismiss = { complaintForForensic = null }
        )
    }

    // Modal: Employee Response to Complaint Dialog
    complaintToRespond?.let { comp ->
        ComplaintResponseDialog(
            complaint = comp,
            onDismiss = { complaintToRespond = null },
            onSubmitResponse = { accepted, reason, proofUri ->
                onRespondToComplaint(comp.id, accepted, reason, proofUri)
                complaintToRespond = null
            }
        )
    }

    // Modal: Manager Complaint Review Dialog
    complaintToReviewByManager?.let { comp ->
        ComplaintManagerReviewDialog(
            complaint = comp,
            onDismiss = { complaintToReviewByManager = null },
            onConfirmDecision = { decision, notes ->
                onDecideOnComplaint(comp.id, decision, notes)
                complaintToReviewByManager = null
            }
        )
    }
}

@Composable
private fun LedgerStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LedgerFilterTabItem(
    title: String,
    badge: String,
    badgeColor: Color = GoldPrimary,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) GoldPrimary else Color.Transparent,
        contentColor = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface,
        modifier = modifier.heightIn(min = 36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                shape = CircleShape,
                color = if (selected) Color.Black.copy(alpha = 0.18f) else badgeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = badge,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.Black else badgeColor,
                    modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Dialog for the incoming cashier to confirm receiving a handed-over shift
 */
@Composable
fun ShiftReceivingDialog(
    shift: ShiftHandoverEntity,
    currentCashierName: String,
    currentCashierId: String,
    onDismiss: () -> Unit,
    onConfirmReceipt: (receiverId: String, receiverName: String, notes: String?) -> Unit
) {
    var receiverName by remember { mutableStateOf(currentCashierName) }
    var receivingNotes by remember { mutableStateOf("تم الاستلام الفعلي ومطابقة النقد بالدرج وكافة البضائع.") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldPrimary)
                    Text("استلام الشفت ومطابقة الخزينة 📥", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• الموظف المسلم: ${shift.cashierName}", fontSize = 11.5.sp)
                        Text("• النقد المسلم بالدرج: ${GoldUtils.formatCurrency(shift.actualCash)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        Text("• الفواتير المصدرة: ${shift.totalInvoicesCount} فواتير", fontSize = 11.sp)
                        Text("• البضاعة المضافة: ${shift.productsAddedCount} أصناف", fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = receiverName,
                    onValueChange = { receiverName = it },
                    label = { Text("اسم الموظف المستلم (الطرف الآخر)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = receivingNotes,
                    onValueChange = { receivingNotes = it },
                    label = { Text("ملاحظات المطابقة والاستلام") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            onConfirmReceipt(currentCashierId, receiverName.trim(), receivingNotes.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        modifier = Modifier.weight(1.5f).heightIn(min = 48.dp)
                    ) {
                        Text("تأكيد الاستلام ✓", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog for the accused employee to respond to a financial complaint
 */
@Composable
fun ComplaintResponseDialog(
    complaint: FinancialComplaintEntity,
    onDismiss: () -> Unit,
    onSubmitResponse: (accepted: Boolean, reason: String?, proofUri: String?) -> Unit
) {
    var responseMode by remember { mutableStateOf("ACCEPT") } // "ACCEPT" or "REJECT"
    var rejectionReason by remember { mutableStateOf("") }
    var rejectionProofUri by remember { mutableStateOf<String?>("content://media/oozox/rejection_proof_${System.currentTimeMillis()}.jpg") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Gavel, contentDescription = null, tint = Color.Red)
                    Text("الرد على الشكوى المالية ⚖️", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Red.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("الشكوى: ${complaint.title}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        Text("المبلغ المشكو فيه: ${GoldUtils.formatCurrency(complaint.claimAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("التفاصيل: ${complaint.details}", fontSize = 11.sp)
                    }
                }

                Text("حدد موقفك من الشكوى:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = responseMode == "ACCEPT",
                        onClick = { responseMode = "ACCEPT" },
                        label = { Text("قبول الشكوى (خصم من الرصيد)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = responseMode == "REJECT",
                        onClick = { responseMode = "REJECT" },
                        label = { Text("رفض الشكوى (مع إرفاق سبب وصورة)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.Red,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (responseMode == "ACCEPT") {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ بموافقتك على الشكوى، سيتم خصم مبلغ ${GoldUtils.formatCurrency(complaint.claimAmount)} تلقائياً من رصيدك في كشف الحساب بأثر رجعي، وإشعار مدير العمل بذلك.",
                            fontSize = 10.5.sp,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("سبب الرفض والتوضيح الدقيق (إلزامي)") },
                        placeholder = { Text("اكتب بالتفصيل سبب عدم صحة الشكوى...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Text("صورة إثبات البراءة ومستند الرفض", fontSize = 10.5.sp)
                            }
                            Text("✓ مرفق", fontSize = 10.5.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            val accepted = responseMode == "ACCEPT"
                            onSubmitResponse(
                                accepted,
                                if (accepted) null else rejectionReason.trim(),
                                if (accepted) null else rejectionProofUri
                            )
                        },
                        enabled = responseMode == "ACCEPT" || rejectionReason.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (responseMode == "ACCEPT") Color(0xFF10B981) else Color.Red,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1.5f).heightIn(min = 48.dp)
                    ) {
                        Text(if (responseMode == "ACCEPT") "تأكيد القبول والخصم ✓" else "إرسال الرفض للمدير ❌", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog for Manager to review and decide on a complaint
 */
@Composable
fun ComplaintManagerReviewDialog(
    complaint: FinancialComplaintEntity,
    onDismiss: () -> Unit,
    onConfirmDecision: (decision: String, notes: String?) -> Unit
) {
    var decisionNotes by remember { mutableStateOf("تمت المراجعة من قبل الإدارة واتخاذ القرار المناسب.") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("مراجعة قرار الشكوى (صلاحية الإدارة) 👨‍💼", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• المشكو في حقه: @${complaint.accusedEmployeeName ?: complaint.submittedByStaffName}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text("• المبلغ: ${GoldUtils.formatCurrency(complaint.claimAmount)}", fontSize = 11.5.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        Text("• حالة رد الموظف: ${if (complaint.employeeResponseStatus == "ACCEPTED") "قبل الخصم ✓" else if (complaint.employeeResponseStatus == "REJECTED") "رفض الشكوى ❌" else "قيد الانتظار ⏳"}", fontSize = 11.sp)
                        if (!complaint.employeeRejectionReason.isNullOrBlank()) {
                            Text("• سبب رفض الموظف: ${complaint.employeeRejectionReason}", fontSize = 11.sp, color = Color.Red)
                        }
                    }
                }

                OutlinedTextField(
                    value = decisionNotes,
                    onValueChange = { decisionNotes = it },
                    label = { Text("ملاحظات قرار الإدارة") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onConfirmDecision("DISMISSED", decisionNotes) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("إسقاط الشكوى", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onConfirmDecision("CONFIRMED_DEDUCTION", decisionNotes) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("اعتماد الخصم ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
