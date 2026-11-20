package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.InvoiceEntity
import com.example.ui.theme.*
import com.example.util.SmartProductScannerEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GranularPaymentBreakdownDialog(
    allInvoices: List<InvoiceEntity>,
    shopName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val report = remember(allInvoices) {
        SmartProductScannerEngine.generatePaymentBreakdownReport(allInvoices)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
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
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = GoldPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("تقرير تفصيل المبيعات بوسائل الدفع للأدمن", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                    Text("تفصيص حسابات الموظفين: إنستاباي، فودافون كاش، كاش، مدفوعات موردين", fontSize = 10.5.sp, color = GoldPrimary)
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    val file = SmartProductScannerEngine.exportPaymentReportToCsv(
                                        context = context,
                                        report = report,
                                        shopName = shopName
                                    )
                                    if (file != null) {
                                        com.example.util.SimpleExcelEngine.shareExcelFile(
                                            context = context,
                                            file = file,
                                            title = "تقرير وسائل الدفع وحسابات الموظفين - Excel"
                                        )
                                    } else {
                                        Toast.makeText(context, "تعذر تصدير الملف", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("export_payment_breakdown_excel_btn")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "تصدير Excel", tint = Color(0xFF10B981))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Overall Summary Metric Cards
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📊 إجماليات المتجر حسب وسيلة الدفع اليوم:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "إجمالي: ${report.grandTotalSales.toInt()} ج.م",
                                            color = Color(0xFF10B981),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PaymentMetricCard("إنستا باي (InstaPay)", "${report.grandTotalInstaPay.toInt()} ج.م", Color(0xFF8B5CF6), Modifier.weight(1f))
                                    PaymentMetricCard("فودافون كاش", "${report.grandTotalVodafoneCash.toInt()} ج.م", Color(0xFFEF4444), Modifier.weight(1f))
                                    PaymentMetricCard("كاش الدرج", "${report.grandTotalCash.toInt()} ج.م", Color(0xFF10B981), Modifier.weight(1f))
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PaymentMetricCard("تحويلات بنكية و فيزا", "${(report.grandTotalBankTransfer + report.grandTotalVisa).toInt()} ج.م", Color(0xFF3B82F6), Modifier.weight(1f))
                                    PaymentMetricCard("مدفوع للموردين", "${report.grandTotalSupplierPayout.toInt()} ج.م", Color(0xFFF59E0B), Modifier.weight(1f))
                                    PaymentMetricCard("صافي نقدية الدرج", "${(report.grandTotalCash - report.grandTotalSupplierPayout).toInt()} ج.م", GoldPrimary, Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // 2. Granular Cashier Breakdown Table (تفصيص حسابات الموظفين)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("جدول تفصيص الحسابات لكل موظف:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                        Text("مرر أفقياً للاطلاع على مبيعات كل وسيلة ومدفوعات الموردين", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val scrollState = rememberScrollState()

                                Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                                    Column {
                                        // Header Row
                                        Row(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                                .padding(vertical = 8.dp, horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("اسم الموظف", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(130.dp))
                                            Text("الفواتير", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                            Text("إجمالي المبيعات", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp), textAlign = TextAlign.Center)
                                            Text("إنستا باي", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
                                            Text("فودافون كاش", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
                                            Text("كاش بالدرج", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
                                            Text("دفع لموردين", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
                                            Text("صافي العهدة", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Data Rows per Cashier
                                        report.cashierBreakdowns.forEachIndexed { idx, c ->
                                            Row(
                                                modifier = Modifier
                                                    .padding(vertical = 6.dp, horizontal = 6.dp)
                                                    .background(
                                                        if (idx % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                        RoundedCornerShape(6.dp)
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(c.cashierName, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(130.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${c.invoicesCount}", fontSize = 11.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                                                Text("${c.totalSales.toInt()} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp), textAlign = TextAlign.Center, color = Color(0xFF10B981))
                                                Text("${c.instaPayTotal.toInt()}", fontSize = 11.sp, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center, color = Color(0xFF8B5CF6))
                                                Text("${c.vodafoneCashTotal.toInt()}", fontSize = 11.sp, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center, color = Color(0xFFEF4444))
                                                Text("${c.cashInDrawerTotal.toInt()}", fontSize = 11.sp, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center, color = Color(0xFF10B981))
                                                Text("${c.supplierPayoutTotal.toInt()}", fontSize = 11.sp, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center, color = Color(0xFFF59E0B))
                                                Text("${c.netCashInHand.toInt()} ج.م", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp), textAlign = TextAlign.Center, color = GoldPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Admin Decision & Audit Confirmation
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مطابقة وتدقيق الحسابات مع البنوك والمحافظ:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                }
                                Text(
                                    "تم تفصيص الحسابات بالكامل. يستطيع الأدمن مطابقة إجمالي إنستاباي (${report.grandTotalInstaPay.toInt()} ج.م) مع إشعار البنك، ومطابقة فودافون كاش (${report.grandTotalVodafoneCash.toInt()} ج.م) مع رصيد المحفظة، واستلام نقدية الدرج الفعلية (${(report.grandTotalCash - report.grandTotalSupplierPayout).toInt()} ج.م) بعد خصم مدفوعات الموردين.",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMetricCard(
    title: String,
    amount: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(2.dp))
            Text(amount, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}
