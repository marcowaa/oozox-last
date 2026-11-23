package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DailyAccountingRecordEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyFinancialJournalDialog(
    currentUser: UserEntity,
    dailyRecords: List<DailyAccountingRecordEntity>,
    onDismiss: () -> Unit,
    onAddRecord: (DailyAccountingRecordEntity) -> Unit,
    onDeleteRecord: (String) -> Unit
) {
    val context = LocalContext.current
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val yesterdayStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
    val dayBeforeStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -2)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    var selectedDate by remember { mutableStateOf(todayStr) }
    var selectedFilterType by remember { mutableStateOf("ALL") } // ALL, REVENUE, EXPENSE, DEFICIT
    var showAddEntryModal by remember { mutableStateOf(false) }

    // Filtered records for selected date
    val dayRecords = remember(dailyRecords, selectedDate, selectedFilterType) {
        val forDay = dailyRecords.filter { it.entryDate == selectedDate || (selectedDate == todayStr && it.entryDate.isBlank()) }
        when (selectedFilterType) {
            "REVENUE" -> forDay.filter { it.incomingAmount > 0 || it.dailyRevenue > 0 }
            "EXPENSE" -> forDay.filter { it.outgoingAmount > 0 }
            "DEFICIT" -> forDay.filter { it.deficitAmount > 0 }
            else -> forDay
        }
    }

    // Totals
    val totalRevenue = remember(dayRecords) { dayRecords.sumOf { it.incomingAmount + it.dailyRevenue } }
    val totalExpenses = remember(dayRecords) { dayRecords.sumOf { it.outgoingAmount } }
    val totalDeficit = remember(dayRecords) { dayRecords.sumOf { it.deficitAmount } }
    val netCashflow = totalRevenue - totalExpenses

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("daily_financial_journal_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.4f))
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
                    OozoxCircleIconButton(
                        onClick = onDismiss,
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurface,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "دفتر اليومية",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "سجل القيود والمصروفات",
                                fontSize = 11.5.sp,
                                color = GoldPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // Date Selection Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedDate == todayStr,
                        onClick = { selectedDate = todayStr },
                        label = { Text("اليوم ($todayStr)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )

                    FilterChip(
                        selected = selectedDate == yesterdayStr,
                        onClick = { selectedDate = yesterdayStr },
                        label = { Text("أمس ($yesterdayStr)", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )

                    FilterChip(
                        selected = selectedDate == dayBeforeStr,
                        onClick = { selectedDate = dayBeforeStr },
                        label = { Text("أول أمس ($dayBeforeStr)", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Summary Dashboard Deck
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ملخص اليوم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                            Text("${dayRecords.size} قيود", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Inflow / Revenue
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("الإيرادات", fontSize = 10.5.sp, color = Color(0xFF10B981))
                                    Text(
                                        "${"%,.0f".format(totalRevenue)} ج.م",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }

                            // Outflow / Expenses
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("المصروفات", fontSize = 10.5.sp, color = Color(0xFFEF4444))
                                    Text(
                                        "${"%,.0f".format(totalExpenses)} ج.م",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }

                            // Net Balance
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (netCashflow >= 0) GoldPrimary.copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("الصافي", fontSize = 10.5.sp, color = if (netCashflow >= 0) GoldLight else Color(0xFFEF4444))
                                    Text(
                                        "${"%,.0f".format(netCashflow)} ج.م",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netCashflow >= 0) GoldLight else Color(0xFFEF4444)
                                    )
                                }
                            }
                        }

                        if (totalDeficit > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "عجز الصندوق: ${"%,.0f".format(totalDeficit)} ج.م",
                                    fontSize = 11.sp,
                                    color = Color(0xFFF59E0B),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: + Add Entry & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddEntryModal = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة قيد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareBody = buildString {
                                appendLine("تقرير كشف اليومية المحاسبي - Oozox Financial Journal")
                                appendLine("التاريخ: $selectedDate")
                                appendLine("المسؤول: ${currentUser.name} (${currentUser.phone})")
                                appendLine("─────────────────────────")
                                appendLine("إجمالي المقبوضات/الوارد: ${"%,.0f".format(totalRevenue)} ج.م")
                                appendLine("إجمالي المصروفات/المنصرف: ${"%,.0f".format(totalExpenses)} ج.م")
                                appendLine("صافي حركة الصندوق: ${"%,.0f".format(netCashflow)} ج.م")
                                if (totalDeficit > 0) {
                                    appendLine("️ عجز الصندوق: ${"%,.0f".format(totalDeficit)} ج.م")
                                }
                                appendLine("─────────────────────────")
                                appendLine("تفاصيل القيود المسجلة (${dayRecords.size} قيد):")
                                dayRecords.forEachIndexed { i, r ->
                                    appendLine("${i + 1}. [${r.staffRole}] ${r.staffName}: وارد ${r.incomingAmount} ج.م | صادر ${r.outgoingAmount} ج.م ${if (!r.notes.isNullOrBlank()) "(${r.notes})" else ""}")
                                }
                                appendLine("─────────────────────────")
                                appendLine("كشف معتمد ومشفر عبر تطبيق Oozox")
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "مشاركة تقرير دفتر اليومية"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Records List
                if (dayRecords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد قيود مسجلة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("اضغط على \"إضافة قيد\" لتسجيل حركة مالية", fontSize = 11.sp, color = GoldLight)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(dayRecords, key = { it.id }) { record ->
                            JournalEntryCard(record = record, onDelete = { onDeleteRecord(record.id) })
                        }
                    }
                }
            }
        }
    }

    // Modal: Add Fast Journal Entry
    if (showAddEntryModal) {
        FastAddJournalRecordDialog(
            targetDate = selectedDate,
            currentUserName = currentUser.name,
            onDismiss = { showAddEntryModal = false },
            onSave = { newRecord ->
                onAddRecord(newRecord)
                showAddEntryModal = false
            }
        )
    }
}

@Composable
fun JournalEntryCard(
    record: DailyAccountingRecordEntity,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = record.staffName.ifBlank { "قيد يومية معتمد" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${record.staffRole} • ${record.subGroupId ?: "الخزينة العامة"}",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "مرحّل ومطابق",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (record.incomingAmount > 0 || record.dailyRevenue > 0) {
                    Text(
                        text = "وارد: +${"%,.0f".format(record.incomingAmount.takeIf { it > 0 } ?: record.dailyRevenue)} ج.م",
                        color = Color(0xFF10B981),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (record.outgoingAmount > 0) {
                    Text(
                        text = "صادر: -${"%,.0f".format(record.outgoingAmount)} ج.م",
                        color = Color(0xFFEF4444),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (record.deficitAmount > 0) {
                    Text(
                        text = "عجز: ${"%,.0f".format(record.deficitAmount)} ج.م",
                        color = Color(0xFFF59E0B),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!record.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ملاحظات: ${record.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FastAddJournalRecordDialog(
    targetDate: String,
    currentUserName: String,
    onDismiss: () -> Unit,
    onSave: (DailyAccountingRecordEntity) -> Unit
) {
    var entryType by remember { mutableStateOf("REVENUE") } // REVENUE or EXPENSE
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var staffRoleText by remember { mutableStateOf("أمين الخزينة") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PostAdd, contentDescription = null, tint = GoldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إضافة قيد ($targetDate)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { entryType = "REVENUE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (entryType == "REVENUE") Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (entryType == "REVENUE") Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("إيراد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { entryType = "EXPENSE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (entryType == "EXPENSE") Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (entryType == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("مصروف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("المبلغ (ج.م)") },
                    placeholder = { Text("مثال: 5000") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = GoldPrimary) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("البيان / ملاحظات") },
                    placeholder = { Text("تفاصيل القيد...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val newEntity = DailyAccountingRecordEntity(
                        id = "journal_${System.currentTimeMillis()}",
                        groupId = "general_journal",
                        subGroupId = "دفتر اليومية",
                        entryDate = targetDate,
                        staffId = "current_staff",
                        staffName = currentUserName,
                        staffRole = staffRoleText,
                        incomingAmount = if (entryType == "REVENUE") amount else 0.0,
                        outgoingAmount = if (entryType == "EXPENSE") amount else 0.0,
                        dailyRevenue = if (entryType == "REVENUE") amount else 0.0,
                        deficitAmount = 0.0,
                        notes = notesText,
                        isEncrypted = true,
                        timestamp = System.currentTimeMillis()
                    )
                    onSave(newEntity)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("حفظ القيد", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
