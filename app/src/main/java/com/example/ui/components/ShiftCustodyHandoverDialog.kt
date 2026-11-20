package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.model.ShiftHandoverEntity
import com.example.ui.theme.GoldPrimary
import com.example.util.NotificationHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * ShiftCustodyHandoverDialog
 * Comprehensive Shift Handover & Custody Verification System.
 * Enables full custody transfer between employees covering:
 * 1. Cash & Treasury Net (Cash, InstaPay, Vodafone Cash, Bank)
 * 2. Physical Merchandise & Inventory Stock Piece Count
 * 3. Invoices, Operations & Dual Digital Confirmation Signatures.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftCustodyHandoverDialog(
    handover: ShiftHandoverEntity,
    currentStaffId: String,
    currentStaffName: String,
    onConfirmReceiving: (
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
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var cashDrawerConfirmed by remember { mutableStateOf(true) }
    var physicalStockChecked by remember { mutableStateOf(true) }
    var invoicesAudited by remember { mutableStateOf(true) }
    var acceptanceType by remember { mutableStateOf("ACCEPTED_FULL_MATCH") } // ACCEPTED_FULL_MATCH, ACCEPTED_WITH_DISCREPANCY
    var merchandiseStatus by remember { mutableStateOf("مطابقة البضاعة والمخزون بالكامل (100%)") }
    var receivingNotes by remember { mutableStateOf("") }
    var discrepancyNotes by remember { mutableStateOf("") }
    var signatureCode by remember { mutableStateOf(currentStaffName) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(GoldPrimary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Handshake, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("محضر تسليم واستلام العهدة والشفت 🤝", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                    Text("المسلّم: ${handover.cashierName} • المستلم: $currentStaffName", fontSize = 10.5.sp, color = GoldPrimary)
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("إلغاء", fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    if (!cashDrawerConfirmed || !physicalStockChecked || !invoicesAudited) {
                                        Toast.makeText(context, "يرجى تأكيد بنود الاستلام (الخزينة، البضاعة، الفواتير) للمتابعة", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    if (acceptanceType == "ACCEPTED_WITH_DISCREPANCY" && discrepancyNotes.isBlank()) {
                                        Toast.makeText(context, "يرجى توضيح الفروقات أو النواقص في خانة الملاحظات", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    showConfirmDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (acceptanceType == "ACCEPTED_FULL_MATCH") Color(0xFF10B981) else Color(0xFFF59E0B),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(2f)
                                    .testTag("confirm_shift_custody_receipt_btn"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (acceptanceType == "ACCEPTED_FULL_MATCH") "تأكيد استلام العهدة (مطابقة 100%)" else "تأكيد استلام العهدة (مع تسجيل فروقات)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Status Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("تفاصيل الشفت المسلم", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("تاريخ: ${handover.shiftDateText.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(handover.logoutTime)) }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("الموظف المسلّم للعهدة: ${handover.cashierName} (ID: ${handover.cashierId.take(8).uppercase()})", fontSize = 12.sp, color = GoldPrimary, fontWeight = FontWeight.SemiBold)
                                if (!handover.notes.isNullOrBlank()) {
                                    Text("ملاحظات المسلّم: ${handover.notes}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    // Pillar 1: Cash & Treasury Breakdown
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("1. جرد الخزينة والنقدية المسلمة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Checkbox(
                                        checked = cashDrawerConfirmed,
                                        onCheckedChange = { cashDrawerConfirmed = it },
                                        modifier = Modifier.testTag("checkbox_cash_confirmed")
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // Detailed Metrics Grid
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FinancialRowItem("النقد الفعلي بالدرج (Cash):", "${handover.actualCash.toInt()} ج.م", isBold = true)
                                    FinancialRowItem("إنستا باي (InstaPay):", "${handover.instaPayCollected.toInt()} ج.م")
                                    FinancialRowItem("فودافون كاش والمحافظ الإلكترونية:", "${handover.vodafoneCashCollected.toInt()} ج.م")
                                    FinancialRowItem("التحويلات البنكية والفيزا:", "${handover.bankCollected.toInt()} ج.م")
                                    FinancialRowItem("مدفوعات الموردين أثناء الشفت:", "- ${handover.supplierPayoutsDuringShift.toInt()} ج.م", isNegative = true)
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    FinancialRowItem("صافي الخزينة الإجمالي:", "${handover.netTreasuryCash.toInt()} ج.م", isPrimary = true)
                                }

                                if (handover.hasDeficit) {
                                    Surface(
                                        color = Color(0xFFEF4444).copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تنبيه: مسجل عجز خزينة بالشفت قدره ${handover.deficitAmount.toInt()} ج.م", fontSize = 11.5.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pillar 2: Physical Merchandise & Stock Pieces Count
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("2. جرد واستلام البضاعة والمخزون", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Checkbox(
                                        checked = physicalStockChecked,
                                        onCheckedChange = { physicalStockChecked = it },
                                        modifier = Modifier.testTag("checkbox_stock_confirmed")
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("البضائع المضافة للعهدة:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${handover.productsAddedCount} صنف / قطعة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("البضائع المباعة أثناء الشفت:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${handover.productsSoldCount} صنف / قطعة", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }
                                }

                                Text("حالة البضاعة والمطابقة الميدانية:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = merchandiseStatus.contains("مطابقة"),
                                        onClick = { merchandiseStatus = "مطابقة البضاعة والمخزون بالكامل (100%)" },
                                        label = { Text("مطابقة 100% ✓", fontSize = 11.5.sp) }
                                    )
                                    FilterChip(
                                        selected = merchandiseStatus.contains("فروقات"),
                                        onClick = {
                                            merchandiseStatus = "وجود فروقات أو نواقص بالبضاعة ⚠️"
                                            acceptanceType = "ACCEPTED_WITH_DISCREPANCY"
                                        },
                                        label = { Text("يوجد نواقص/فروقات ⚠️", fontSize = 11.5.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // Pillar 3: Invoices & Accounting Records
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("3. مطابقة الفواتير والعمليات", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Checkbox(
                                        checked = invoicesAudited,
                                        onCheckedChange = { invoicesAudited = it },
                                        modifier = Modifier.testTag("checkbox_invoices_confirmed")
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("إجمالي عدد الفواتير المسجلة:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${handover.totalInvoicesCount} فاتورة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("إجمالي عدد العمليات المحاسبية:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${handover.operationsCount} عملية", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Pillar 4: Digital Confirmation & Discrepancy Notes
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("إقرار مستلم العهدة والملاحظات الرقابية ✍️", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = acceptanceType == "ACCEPTED_FULL_MATCH",
                                        onClick = { acceptanceType = "ACCEPTED_FULL_MATCH" },
                                        label = { Text("استلام كامل مطابق 100% ✓", fontSize = 11.5.sp) }
                                    )
                                    FilterChip(
                                        selected = acceptanceType == "ACCEPTED_WITH_DISCREPANCY",
                                        onClick = { acceptanceType = "ACCEPTED_WITH_DISCREPANCY" },
                                        label = { Text("استلام مع توثيق فروقات ⚠️", fontSize = 11.5.sp) }
                                    )
                                }

                                if (acceptanceType == "ACCEPTED_WITH_DISCREPANCY") {
                                    OutlinedTextField(
                                        value = discrepancyNotes,
                                        onValueChange = { discrepancyNotes = it },
                                        label = { Text("بيان الفروقات والنواقص بالتفصيل (مبالغ/أصناف/فواتير) *") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                OutlinedTextField(
                                    value = receivingNotes,
                                    onValueChange = { receivingNotes = it },
                                    label = { Text("ملاحظات إضافية على الاستلام (اختياري)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = signatureCode,
                                    onValueChange = { signatureCode = it },
                                    label = { Text("اسم/توقيع المستلم الرقمي للعهدة *") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text("تأكيد اعتماد استلام العهدة والشفت 🤝", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "هل تؤكد بصفتك الموظف المستلم ($currentStaffName) فحص واستلام كامل النقدية (${handover.netTreasuryCash.toInt()} ج.م) والبضاعة والفواتير وفق البنود المحددة؟ سيتم تسجيل توقيعك كإقرار رسمي في النظام.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onConfirmReceiving(
                            handover.id,
                            currentStaffId,
                            currentStaffName,
                            receivingNotes.ifBlank { null },
                            merchandiseStatus,
                            physicalStockChecked,
                            cashDrawerConfirmed,
                            handover.totalInvoicesCount,
                            signatureCode.ifBlank { currentStaffName },
                            acceptanceType,
                            if (acceptanceType == "ACCEPTED_WITH_DISCREPANCY") discrepancyNotes.ifBlank { null } else null
                        )
                        NotificationHelper.showShiftHandoverReceivedAlert(
                            context = context,
                            senderName = handover.cashierName,
                            receiverName = currentStaffName,
                            totalCash = handover.netTreasuryCash
                        )
                        Toast.makeText(context, "تم تأكيد استلام الشفت والعهدة وتوثيق المحضر بنجاح ✓", Toast.LENGTH_LONG).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("نعم، اعتمد الاستلام", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("مراجعة ثانية")
                }
            }
        )
    }
}

@Composable
private fun FinancialRowItem(
    label: String,
    value: String,
    isBold: Boolean = false,
    isPrimary: Boolean = false,
    isNegative: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontSize = if (isPrimary) 13.5.sp else 12.5.sp,
            fontWeight = if (isBold || isPrimary) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isPrimary -> GoldPrimary
                isNegative -> Color(0xFFEF4444)
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
