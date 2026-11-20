package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.DailyAccountingRecordEntity
import com.example.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Confirmation & Audit Summary Dialog for Financial Transactions (Revenues & Expenses)
 * Shows a full summary review of the financial movement, treasury impact, staff responsible,
 * payment method, invoice number, and receipt attachments before final posting.
 */
@Composable
fun FinancialPostingConfirmationDialog(
    record: DailyAccountingRecordEntity,
    groupName: String,
    companyName: String?,
    previousCapital: Double,
    entryMode: Int, // 0: Revenue, 1: Expense, 2: Deficit/Settlement, 3: Full
    categoryLabel: String,
    paymentMethod: String,
    invoiceNumber: String?,
    onConfirmPost: () -> Unit,
    onBackToEdit: () -> Unit
) {
    val currencyFormatter = DecimalFormat("#,##0.00")
    val intCurrencyFormatter = DecimalFormat("#,##0")

    val isRevenue = record.dailyRevenue > 0 || record.incomingAmount > 0
    val isExpense = record.outgoingAmount > 0
    val isDeficit = record.deficitAmount > 0

    val totalAmount = when (entryMode) {
        0 -> record.dailyRevenue
        1 -> record.outgoingAmount
        2 -> record.deficitAmount
        else -> (record.dailyRevenue + record.incomingAmount) - (record.outgoingAmount + record.deficitAmount)
    }

    val netImpact = (record.dailyRevenue + record.incomingAmount) - (record.outgoingAmount + record.deficitAmount)
    val afterCapital = record.dailyCapital

    val formattedTime = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date(record.timestamp))

    Dialog(
        onDismissRequest = onBackToEdit,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header: Verification & Audit Shield
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = GoldPrimary.copy(alpha = 0.2f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, GoldPrimary),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FactCheck,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "تأكيد ومراجعة ترحيل القيد المالي",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                            Text(
                                text = "مراجعة شاملة لبيانات الحركة قبل الترحيل النهائي للخزينة",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = GoldDark.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.6.dp, GoldPrimary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "تدقيق مالي ️",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Summary Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Hero Transaction Amount Banner
                    Surface(
                        color = when (entryMode) {
                            0 -> StatusGreen.copy(alpha = 0.12f)
                            1 -> StatusRed.copy(alpha = 0.12f)
                            2 -> RealGoldAmber.copy(alpha = 0.12f)
                            else -> GoldDark.copy(alpha = 0.25f)
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.5.dp,
                            when (entryMode) {
                                0 -> StatusGreen
                                1 -> StatusRed
                                2 -> RealGoldAmber
                                else -> GoldPrimary
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Transaction Type Badge
                            Surface(
                                color = when (entryMode) {
                                    0 -> StatusGreen
                                    1 -> StatusRed
                                    2 -> RealGoldAmber
                                    else -> GoldPrimary
                                },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (entryMode) {
                                            0 -> Icons.Default.ArrowDownward
                                            1 -> Icons.Default.ArrowUpward
                                            2 -> Icons.Default.Balance
                                            else -> Icons.Default.AccountBalanceWallet
                                        },
                                        contentDescription = null,
                                        tint = if (entryMode == 1) Color.White else Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (entryMode) {
                                            0 -> "حركة إيراد / مقبوضات نقدية (+)"
                                            1 -> "حركة مصروفات / مدفوعات نقدية (-)"
                                            2 -> "حركة عجز / تسوية حسابات ️"
                                            else -> "قيد محاسبي يومي شامل"
                                        },
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (entryMode == 1) Color.White else Color.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Amount Display
                            val displayAmount = if (entryMode != 3) kotlin.math.abs(totalAmount) else kotlin.math.abs(netImpact)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = if (entryMode == 1 || (entryMode == 3 && netImpact < 0)) "- " else "+ ",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when (entryMode) {
                                        0 -> StatusGreen
                                        1 -> StatusRed
                                        2 -> RealGoldAmber
                                        else -> if (netImpact >= 0) StatusGreen else StatusRed
                                    }
                                )
                                Text(
                                    text = currencyFormatter.format(displayAmount),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when (entryMode) {
                                        0 -> StatusGreen
                                        1 -> StatusRed
                                        2 -> RealGoldAmber
                                        else -> if (netImpact >= 0) StatusGreen else StatusRed
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ج.م",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = categoryLabel,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // 1. Transaction Details Review Grid
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "تفاصيل وبيانات المعاملة:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )

                            // Target Group / Department
                            ReviewDataRow(
                                icon = Icons.Default.Business,
                                label = "القسم / الفرع المستهدف:",
                                value = "$groupName ${if (!companyName.isNullOrBlank()) "($companyName)" else ""}"
                            )

                            // Staff in Charge
                            ReviewDataRow(
                                icon = Icons.Default.PersonPin,
                                label = "المسؤول / المحاسب المسجل:",
                                value = "${record.staffName} • ${record.staffRole}"
                            )

                            // Date & Time
                            ReviewDataRow(
                                icon = Icons.Default.CalendarToday,
                                label = "تاريخ وتوقيت القيد:",
                                value = "${record.entryDate} ($formattedTime)"
                            )

                            // Payment Method
                            ReviewDataRow(
                                icon = Icons.Default.Payment,
                                label = "طريقة السداد / الدفع:",
                                value = paymentMethod
                            )

                            // Invoice Number (if any)
                            if (!invoiceNumber.isNullOrBlank()) {
                                ReviewDataRow(
                                    icon = Icons.Default.ReceiptLong,
                                    label = "رقم الفاتورة / المستند:",
                                    value = invoiceNumber
                                )
                            }
                        }
                    }

                    // 2. Full Entry Breakdown (if mode == 3)
                    if (entryMode == 3) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "تفصيل القيد المحاسبي الشامل:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الإيراد اليومي (+):", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${currencyFormatter.format(record.dailyRevenue)} ج.م", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الوارد النقدي (+):", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${currencyFormatter.format(record.incomingAmount)} ج.م", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المنصرف / المصروفات (-):", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${currencyFormatter.format(record.outgoingAmount)} ج.م", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("العجز / الفاقد (-):", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${currencyFormatter.format(record.deficitAmount)} ج.م", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = RealGoldAmber)
                                }

                                if (record.complaintsCount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("شكاوى العملاء المسجلة:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${record.complaintsCount} شكوى", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Treasury & Capital Impact Flow
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GoldDark.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("الأثر المالي المتوقع على رصيد الخزينة ورأس المال:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous Capital
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("الرصيد السابق", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${intCurrencyFormatter.format(previousCapital)} ج.م",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Arrow Impact
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        color = if (netImpact >= 0) StatusGreen.copy(alpha = 0.2f) else StatusRed.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (netImpact >= 0) "+${intCurrencyFormatter.format(netImpact)}" else intCurrencyFormatter.format(netImpact),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (netImpact >= 0) StatusGreen else StatusRed,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.East,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // New Expected Capital
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("الرصيد بعد الترحيل", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${intCurrencyFormatter.format(afterCapital)} ج.م",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldPrimary
                                    )
                                }
                            }
                        }
                    }

                    // 4. Notes & Screenshot Preview (if present)
                    val hasNotes = !record.notes.isNullOrBlank()
                    val hasScreenshot = record.screenshotUri != null
                    if (hasNotes || hasScreenshot) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (hasNotes) {
                                    Text("الملاحظات والبيان:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                                    Text(record.notes ?: "", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                }

                                if (hasScreenshot) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("مرفق إيصال / مستند بنكي مؤكد", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black.copy(alpha = 0.3f))
                                            .border(1.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    ) {
                                        AsyncImage(
                                            model = record.screenshotUri,
                                            contentDescription = "مرفق الإيصال",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Confirm & Post Final Entry
                    Button(
                        onClick = onConfirmPost,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "تأكيد وترحيل القيد نهائياً",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }

                    // Back to Edit
                    OutlinedButton(
                        onClick = onBackToEdit,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "تعديل البيانات ️",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewDataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
