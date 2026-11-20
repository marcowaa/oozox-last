package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FinancialComplaintEntity
import com.example.ui.theme.*
import com.example.util.ForensicHandoverAnalysisEngine
import com.example.util.GoldUtils
import kotlinx.coroutines.launch

/**
 * ForensicLiabilityDialog
 * Displays the AI Forensic Audit results analyzing how the incoming employee confirmed
 * receiving funds & goods despite the confirmed complaint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForensicLiabilityDialog(
    complaint: FinancialComplaintEntity,
    onApplyJointDeduction: (complaintId: String, primaryAmount: Double, receiverAmount: Double, receiverId: String?, notes: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var forensicResult by remember { mutableStateOf<ForensicHandoverAnalysisEngine.ForensicAnalysisResult?>(null) }

    val primaryGold = adaptiveGoldPrimary
    val redColor = adaptiveStatusRed
    val orangeColor = adaptiveStatusOrange
    val greenColor = adaptiveStatusGreen

    LaunchedEffect(complaint) {
        scope.launch {
            isLoading = true
            forensicResult = ForensicHandoverAnalysisEngine.analyzeComplaintHandoverLiability(context, complaint)
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, primaryGold.copy(alpha = 0.35f))
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(redColor.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Balance, contentDescription = null, tint = redColor, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("التحقيق الجنائي في استلام الشفت", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                        Text("تحليل إقرار استلام العهدة ومسؤولية الأطراف", fontSize = 11.sp, color = primaryGold)
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("إغلاق", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                }

                                if (forensicResult != null && forensicResult!!.receiverDeductionAmount > 0) {
                                    Button(
                                        onClick = {
                                            val res = forensicResult!!
                                            onApplyJointDeduction(
                                                res.complaintId,
                                                res.primaryDeductionAmount,
                                                res.receiverDeductionAmount,
                                                res.receiverId,
                                                "تم تطبيق الخصم المشترك بناءً على التحقيق الجنائي (${res.liabilityTypeAr}): ${GoldUtils.formatCurrency(res.primaryDeductionAmount)} على ${res.primaryAccusedName} و ${GoldUtils.formatCurrency(res.receiverDeductionAmount)} على ${res.receiverName}"
                                            )
                                            Toast.makeText(context, "تم اعتماد قرار الخصم المشترك وتحديث أرصدة العهدة", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = redColor, contentColor = Color.White),
                                        modifier = Modifier
                                            .weight(2f)
                                            .heightIn(min = 48.dp)
                                            .testTag("apply_joint_deduction_btn"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("اعتماد وتطبيق الخصم المشترك", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { padding ->
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(color = primaryGold)
                                Text("جارٍ مطابقة محاضر تسليم الشفت وسجلات الخزينة...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else if (forensicResult != null) {
                        val result = forensicResult!!
                        val verdictColor = when (result.liabilityType) {
                            "SUSPECTED_COLLUSION" -> redColor
                            "GROSS_NEGLIGENCE_RECEIPT" -> orangeColor
                            else -> greenColor
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = verdictColor.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, verdictColor.copy(alpha = 0.65f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("تشخيص النظام الرقابي:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Surface(
                                            color = verdictColor,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                result.liabilityTypeAr,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        "توزيع المسؤولية: ${result.liabilityDistribution}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("أطراف العملية محل النزاع:", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("1. الموظف المحرر (المشكو في حقه):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(result.primaryAccusedName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Surface(
                                            color = redColor.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                "خصم ${GoldUtils.formatCurrency(result.primaryDeductionAmount)}",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = redColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("2. الموظف المستلم (المعتمد للعهدة):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(result.receiverName ?: "لم يستلم أحد الشفت رسمياً", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        if (result.receiverDeductionAmount > 0) {
                                            Surface(
                                                color = orangeColor.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    "خصم ${GoldUtils.formatCurrency(result.receiverDeductionAmount)}",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = orangeColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        } else {
                                            Text("لا يوجد خصم", fontSize = 11.5.sp, color = greenColor, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Analytics, contentDescription = null, tint = primaryGold, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("كيف أكد الموظف الآخر الاستلام رغم وجود الشكوى؟", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        result.detailedReportAr,
                                        fontSize = 12.5.sp,
                                        lineHeight = 19.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, primaryGold.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = primaryGold, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("توصية النظام للمدير المسؤول:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                                    }
                                    Text(
                                        result.recommendedActionAr,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
