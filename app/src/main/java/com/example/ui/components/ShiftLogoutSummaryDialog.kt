package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AppDatabase
import com.example.service.ShiftAggregationSummary
import com.example.service.ShiftHandoverBackendService
import com.example.ui.theme.GoldPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftLogoutSummaryDialog(
    cashierId: String,
    cashierName: String,
    cashierPhone: String? = null,
    loginTime: Long,
    onShiftClosedAndLogout: (ShiftAggregationSummary) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = remember { AppDatabase.getInstance(context) }

    var actualCashText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var isCalculating by remember { mutableStateOf(false) }
    var aggregatedSummary by remember { mutableStateOf<ShiftAggregationSummary?>(null) }

    // Initial preliminary calculation
    LaunchedEffect(Unit) {
        isCalculating = true
        try {
            val initial = ShiftHandoverBackendService.aggregateAndCloseShift(
                context = context,
                database = database,
                cashierId = cashierId,
                cashierName = cashierName,
                cashierPhone = cashierPhone,
                loginTime = loginTime,
                logoutTime = System.currentTimeMillis(),
                actualCashCount = 0.0,
                notes = "حساب مبدئي"
            )
            aggregatedSummary = initial
            actualCashText = initial.expectedCashInDrawer.toInt().toString()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isCalculating = false
        }
    }

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
                                Icon(Icons.Default.LockClock, contentDescription = null, tint = GoldPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("تسجيل خروج وتقفيل الشفت المالي 🛑", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                    Text("تجميع تلقائي لبيانات الشفت ورصد العجز", fontSize = 11.sp, color = GoldPrimary)
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "إلغاء")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { padding ->
                if (isCalculating && aggregatedSummary == null) {
                    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GoldPrimary)
                    }
                } else {
                    val summary = aggregatedSummary
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Employee & Shift Duration Header Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("👤 الموظف: $cashierName", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = GoldPrimary)
                                    Text("المدة: ${summary?.shiftDurationHours ?: 8.0} ساعة", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("كود الموظف: $cashierId • إجمالي الفواتير الصادرة: ${summary?.totalInvoicesCount ?: 0} فاتورة", fontSize = 11.5.sp)
                            }
                        }

                        // 2. Collected Amounts by Payment Method (تجميع مبالغ وسائل الدفع)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("المبالغ المحصلة بالشفت بحسب وسيلة الدفع 💳:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ShiftMetricPill("إنستا باي", "${summary?.instaPayCollected?.toInt() ?: 0} ج.م", Color(0xFF8B5CF6), Modifier.weight(1f))
                                    ShiftMetricPill("فودافون كاش", "${summary?.vodafoneCashCollected?.toInt() ?: 0} ج.م", Color(0xFFEF4444), Modifier.weight(1f))
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ShiftMetricPill("كاش نقدي بالدرج", "${summary?.cashCollected?.toInt() ?: 0} ج.م", Color(0xFF10B981), Modifier.weight(1f))
                                    ShiftMetricPill("تحويلات بنكية/فيزا", "${summary?.bankCollected?.toInt() ?: 0} ج.م", Color(0xFF3B82F6), Modifier.weight(1f))
                                }

                                if ((summary?.supplierPayoutsTotal ?: 0.0) > 0) {
                                    Surface(
                                        color = Color(0xFFF59E0B).copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "• مدفوعات للموردين تم خصمها من الدرج: ${summary?.supplierPayoutsTotal?.toInt()} ج.م",
                                            fontSize = 11.sp,
                                            color = Color(0xFFD97706),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("إجمالي المبيعات المحصلة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${summary?.totalRevenueEGP?.toInt() ?: 0} ج.م", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                            }
                        }

                        // 3. Cash Treasury Count & Deficit Verification (صافي الخزينة والعجز)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("جرد الخزينة والنقدية الفعلية بالدرج 💰:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("النقدية المتوقعة دفترياً بالخزينة:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${summary?.expectedCashInDrawer?.toInt() ?: 0} ج.م", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                }

                                OutlinedTextField(
                                    value = actualCashText,
                                    onValueChange = { actualCashText = it.filter { c -> c.isDigit() || c == '.' } },
                                    label = { Text("المبلغ الفعلي الموجود بالدرج نقداً (ج.م) *") },
                                    placeholder = { Text("أدخل الجرد النقدي الفعلي") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("actual_cash_drawer_input")
                                )

                                val parsedActual = actualCashText.toDoubleOrNull() ?: 0.0
                                val expCash = summary?.expectedCashInDrawer ?: 0.0
                                val currentDeficit = if (parsedActual < expCash) expCash - parsedActual else 0.0
                                val currentSurplus = if (parsedActual > expCash) parsedActual - expCash else 0.0

                                if (currentDeficit > 0) {
                                    Surface(
                                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "يوجد عجز مالي بالخزينة: ${currentDeficit.toInt()} ج.م ⚠️",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFEF4444)
                                                )
                                            }
                                            Text(
                                                "سيقوم النظام تلقائياً بإرسال إشعار فوري وتنبيه طوارئ للمدير العام ببيانات العجز والموظف.",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                } else if (currentSurplus > 0) {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "🟢 يوجد زيادة/فائض نقدي بالدرج: ${currentSurplus.toInt()} ج.م",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "✓ الحسابات مطابقة تماماً بدون أي عجز.",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = notesText,
                                    onValueChange = { notesText = it },
                                    label = { Text("ملاحظات تقفيل الشفت (اختياري)") },
                                    placeholder = { Text("سبب أي فارق أو توضيحات الشفت...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // 4. Confirm & Finalize Shift Logout Button
                        Button(
                            onClick = {
                                val actualCashVal = actualCashText.toDoubleOrNull() ?: 0.0
                                isCalculating = true

                                coroutineScope.launch {
                                    try {
                                        val finalSummary = ShiftHandoverBackendService.aggregateAndCloseShift(
                                            context = context,
                                            database = database,
                                            cashierId = cashierId,
                                            cashierName = cashierName,
                                            cashierPhone = cashierPhone,
                                            loginTime = loginTime,
                                            logoutTime = System.currentTimeMillis(),
                                            actualCashCount = actualCashVal,
                                            notes = notesText.ifBlank { null }
                                        )

                                        if (finalSummary.hasDeficit) {
                                            Toast.makeText(
                                                context,
                                                "تم تسجيل الشفت وإرسال تنبيه عجز مالي (${finalSummary.deficitAmount.toInt()} ج.م) للمدير ⚠️",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(context, "تم إغلاق الشفت المالي وتسجيل الخروج بنجاح ✓", Toast.LENGTH_SHORT).show()
                                        }

                                        onShiftClosedAndLogout(finalSummary)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "حدث خطأ أثناء تقفيل الشفت: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isCalculating = false
                                    }
                                }
                            },
                            enabled = !isCalculating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("confirm_shift_logout_btn")
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تأكيد إغلاق الشفت وتسجيل الخروج", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftMetricPill(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        }
    }
}
