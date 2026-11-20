package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TransactionEntity
import com.example.ui.theme.*
import com.example.util.GoldUtils

@Composable
fun ClearingHouseDialog(
    contactName: String,
    contactId: String,
    transactions: List<TransactionEntity>,
    onExecuteSettlement: (netGold24k: Double, netSilver999: Double, netCash: Double, totalMetalLoss: Double, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var notes by remember { mutableStateOf("عقد تقاص وتصفية آلي بين الطرفين") }

    val contactTxs = remember(transactions) {
        transactions.filter { it.contactId == contactId }
    }

    val totalGoldWeight24k: Double = remember(contactTxs) {
        contactTxs.fold(0.0) { acc, tx ->
            val purityMultiplier = when (tx.goldKarat) {
                "KARAT_24", "24k" -> 1.0
                "KARAT_21", "21k" -> 21.0 / 24.0
                "KARAT_18", "18k" -> 18.0 / 24.0
                "KARAT_14", "14k" -> 14.0 / 24.0
                else -> 21.0 / 24.0
            }
            acc + (tx.weight * purityMultiplier)
        }
    }

    val totalMetalLoss: Double = remember(contactTxs) {
        contactTxs.fold(0.0) { acc, tx -> acc + tx.metalLossGrams }
    }

    val netPureGoldAfterLoss = (totalGoldWeight24k - totalMetalLoss).coerceAtLeast(0.0)

    val totalCashPaid: Double = remember(contactTxs) {
        contactTxs.fold(0.0) { acc, tx -> acc + tx.totalPrice }
    }

    val totalMfgCosts: Double = remember(contactTxs) {
        contactTxs.fold(0.0) { acc, tx -> acc + tx.totalManufacturing }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Balance,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "غرفة التقاص والتصفية الآلية",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تسوية الحسابات والأرصدة المتبادلة",
                                fontSize = 11.sp,
                                color = GoldPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 0.8.dp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("الطرف المقابل: $contactName", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("إجمالي الصفقات المسجلة: ${contactTxs.size} صفقة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Summary Breakdown Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("نتائج حساب التقاص والخصم المتبادل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("وزن الذهب المتبادل (عيار 24):", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(GoldUtils.formatWeight(totalGoldWeight24k), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("خصم تآكل المعدن الصافي:", fontSize = 11.5.sp, color = Color(0xFFF59E0B))
                                Text("-${GoldUtils.formatWeight(totalMetalLoss)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("صافي رصيد الذهب بعد التصفية:", fontSize = 12.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
                                Text(GoldUtils.formatWeight(netPureGoldAfterLoss), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("إجمالي النقدية والمصنوعات:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(GoldUtils.formatCurrency(totalCashPaid + totalMfgCosts), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات تسوية التقاص", fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 0.8.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onExecuteSettlement(netPureGoldAfterLoss, 0.0, totalCashPaid + totalMfgCosts, totalMetalLoss, notes)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.background),
                        modifier = Modifier.weight(1.8f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إبرام عقد التقاص والتصفية", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
