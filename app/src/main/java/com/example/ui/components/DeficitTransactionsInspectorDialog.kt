package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DailyAccountingRecordEntity
import com.example.data.model.FinancialComplaintEntity
import com.example.ui.theme.*
import com.example.util.GoldUtils
import com.example.util.NotificationHelper
import com.example.util.ReportExporter
import com.example.util.acctStr

/**
 * Executive modal auditing entries and retroactive adjustments contributing to a treasury deficit.
 */
@Composable
fun DeficitTransactionsInspectorDialog(
    targetDate: String,
    groupName: String,
    companyName: String,
    allRecords: List<DailyAccountingRecordEntity>,
    allComplaints: List<FinancialComplaintEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val dayRecords = remember(allRecords, targetDate) {
        allRecords.filter { it.entryDate == targetDate }
    }

    val dayComplaints = remember(allComplaints, targetDate) {
        allComplaints.filter { it.targetDate == targetDate }
    }

    val deficitCausingRecords = remember(dayRecords) {
        dayRecords.filter { it.deficitAmount > 0 || it.outgoingAmount > 0 || it.complaintsCount > 0 }
            .sortedByDescending { it.deficitAmount + it.outgoingAmount }
    }

    val totalDeficit = remember(dayRecords) { dayRecords.sumOf { it.deficitAmount } }
    val totalOutgoing = remember(dayRecords) { dayRecords.sumOf { it.outgoingAmount } }
    val totalIncoming = remember(dayRecords) { dayRecords.sumOf { it.incomingAmount } }
    val totalRevenue = remember(dayRecords) { dayRecords.sumOf { it.dailyRevenue } }
    val netCashFlow = (totalRevenue + totalIncoming) - (totalOutgoing + totalDeficit)

    val primaryGold = adaptiveGoldPrimary
    val onGold = adaptiveOnPrimary
    val greenColor = adaptiveStatusGreen
    val redColor = adaptiveStatusRed

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.88f)
                    .testTag("deficit_transactions_inspector_dialog"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            redColor.copy(alpha = 0.75f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    )
                )
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                color = redColor.copy(alpha = 0.14f),
                                shape = CircleShape,
                                modifier = Modifier.size(42.dp),
                                border = BorderStroke(1.dp, redColor.copy(alpha = 0.5f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = redColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    acctStr("deficit_inspector_title"),
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "$targetDate • $groupName",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Close, contentDescription = acctStr("close"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Deficit KPI Banner
                    Surface(
                        color = redColor.copy(alpha = 0.11f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, redColor.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(acctStr("deficit"), fontSize = 11.sp, color = redColor)
                                    Text(
                                        GoldUtils.formatCurrency(totalDeficit),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = redColor
                                    )
                                }
                                Surface(
                                    color = redColor.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, redColor.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = redColor, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "${deficitCausingRecords.size} ${acctStr("records_unit")}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = redColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = redColor.copy(alpha = 0.25f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(acctStr("outflow"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(GoldUtils.formatCurrency(totalOutgoing), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = redColor)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(acctStr("inflow"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(GoldUtils.formatCurrency(totalIncoming + totalRevenue), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = greenColor)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(acctStr("net"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        GoldUtils.formatCurrency(netCashFlow),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netCashFlow >= 0) greenColor else redColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // List Header & Export Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${acctStr("audit_details")} (${deficitCausingRecords.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryGold
                        )

                        Button(
                            onClick = {
                                ReportExporter.exportAccountingRecordsToPdf(
                                    context = context,
                                    groupName = groupName,
                                    companyName = companyName,
                                    records = if (deficitCausingRecords.isNotEmpty()) deficitCausingRecords else dayRecords,
                                    filterCriteriaLabel = targetDate
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGold, contentColor = onGold),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = onGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(acctStr("cmd_pdf"), fontSize = 11.5.sp, color = onGold, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (deficitCausingRecords.isEmpty() && dayComplaints.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = greenColor, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(acctStr("no_deficit_today"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (dayComplaints.isNotEmpty()) {
                                item {
                                    Text(
                                        "${acctStr("disputes_title")} (${dayComplaints.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = redColor
                                    )
                                }

                                items(dayComplaints) { complaint ->
                                    ComplaintAuditCard(complaint = complaint)
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${acctStr("sec_ledger")} (${deficitCausingRecords.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryGold
                                )
                            }

                            items(deficitCausingRecords) { record ->
                                DeficitRecordItemCard(record = record)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(acctStr("close"), color = MaterialTheme.colorScheme.onSurface)
                        }

                        val staffFallback = acctStr("sec_staff")
                        val inspectorTitle = acctStr("deficit_inspector_title")
                        Button(
                            onClick = {
                                NotificationHelper.showFinancialDeficitNotification(
                                    context = context,
                                    groupName = groupName,
                                    staffName = deficitCausingRecords.firstOrNull()?.staffName ?: staffFallback,
                                    deficitAmount = totalDeficit,
                                    entryDate = targetDate,
                                    notes = inspectorTitle
                                )
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = redColor, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(acctStr("alert_admin"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeficitRecordItemCard(record: DailyAccountingRecordEntity) {
    val redColor = adaptiveStatusRed
    val orangeColor = adaptiveStatusOrange
    val primaryGold = adaptiveGoldPrimary

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(
            1.dp,
            if (record.deficitAmount > 0) redColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = if (record.deficitAmount > 0) redColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp),
                        border = BorderStroke(1.dp, if (record.deficitAmount > 0) redColor.copy(alpha = 0.5f) else Color.Transparent)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (record.deficitAmount > 0) Icons.Default.TrendingDown else Icons.Default.Receipt,
                                contentDescription = null,
                                tint = if (record.deficitAmount > 0) redColor else primaryGold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(record.staffName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("${record.staffRole} • ${record.subGroupId ?: acctStr("main_treasury")}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (record.deficitAmount > 0) {
                    Surface(
                        color = redColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            GoldUtils.formatCurrency(record.deficitAmount),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = redColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (record.outgoingAmount > 0) {
                    Surface(
                        color = redColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            GoldUtils.formatCurrency(record.outgoingAmount),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = redColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (!record.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    record.notes,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (record.complaintsCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = orangeColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, orangeColor.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ReportProblem, contentDescription = null, tint = orangeColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            record.complaintsNotes ?: acctStr("cmd_complaints"),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComplaintAuditCard(complaint: FinancialComplaintEntity) {
    val redColor = adaptiveStatusRed
    val greenColor = adaptiveStatusGreen
    val accentColor = if (complaint.impactType == "DEDUCTION") redColor else greenColor
    Surface(
        color = accentColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (complaint.impactType == "DEDUCTION") Icons.Default.RemoveCircle else Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        complaint.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                Text(
                    "${if (complaint.impactType == "DEDUCTION") "-" else "+"} ${GoldUtils.formatCurrency(complaint.amount)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${complaint.submittedByStaffName} • ${complaint.category}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                complaint.details,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (complaint.adminAlertSent) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = redColor, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        acctStr("alert_admin"),
                        fontSize = 9.5.sp,
                        color = redColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
