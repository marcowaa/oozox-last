package com.example.ui.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.AuditSeverity
import com.example.util.AuditVerificationReport
import com.example.util.ParsedVoiceAccountingRecord
import com.example.util.TextToSpeechHelper
import java.util.Locale

/**
 * SmartPreCommitAuditDialog provides a rigorous, instant AI audit review
 * before any accounting entry or voice transaction is permanently committed to the database.
 * Detects anomalies, discrepancies, karat errors, and duplicate entries, warning the user clearly.
 */
@Composable
fun PreCommitAuditDialog(
    record: ParsedVoiceAccountingRecord,
    report: AuditVerificationReport,
    onConfirmCommit: () -> Unit,
    onEditRecord: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val ttsHelper = remember { TextToSpeechHelper(context) }
    val scrollState = rememberScrollState()
    var isPlayingAudio by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }

    fun playAudibleSummary() {
        val speechText = if (record.spokenConfirmationText.isNotBlank()) {
            record.spokenConfirmationText
        } else {
            buildString {
                append(report.summaryTitle)
                append(". ")
                append("الهاشتاج: ${record.suggestedTag}. ")
                if (record.amount > 0.0) {
                    append("المبلغ: ${record.amount.toInt()} جنيه. ")
                }
                if (record.weightGrams > 0.0) {
                    append("الوزن: ${record.weightGrams} جرام. ")
                }
                if (report.anomalies.isNotEmpty()) {
                    append("ملاحظات التدقيق: ")
                    report.anomalies.forEach { anomaly ->
                        append(anomaly.title)
                        append(". ")
                    }
                }
            }
        }
        isPlayingAudio = true
        ttsHelper.speak(speechText) {
            isPlayingAudio = false
        }
    }

    fun stopAudibleSummary() {
        ttsHelper.stop()
        isPlayingAudio = false
    }

    fun triggerHaptic(isError: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val effect = if (isError) VibrationEffect.EFFECT_DOUBLE_CLICK else VibrationEffect.EFFECT_CLICK
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(effect))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(if (isError) 100L else 40L)
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(report) {
        triggerHaptic(report.anomalies.any { it.severity == AuditSeverity.CRITICAL })
    }

    val headerColor = when {
        !report.isApproved -> StatusRed
        report.hasWarnings -> StatusOrange
        else -> StatusGreen
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("pre_commit_audit_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 20.dp,
            border = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(
                    listOf(
                        headerColor.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header with Audit Score Badge & Speech Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(headerColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    !report.isApproved -> "[تنبيه]"
                                    report.hasWarnings -> "️"
                                    else -> "[معتمد]"
                                },
                                fontSize = 22.sp
                            )
                        }
                        Column {
                            Text(
                                text = "المراجعة والتدقيق للعملية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "كود التدقيق: ${report.verificationAuditHash}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // TTS Audible Readout Button
                    IconButton(
                        onClick = {
                            val speechSummary = buildString {
                                append(report.summaryTitle)
                                append(". ")
                                append("الهاشتاج: ${record.suggestedTag}. ")
                                if (record.amount > 0.0) {
                                    append("المبلغ: ${record.amount.toInt()} جنيه. ")
                                }
                                if (record.weightGrams > 0.0) {
                                    append("الوزن: ${record.weightGrams} جرام. ")
                                }
                                if (report.anomalies.isNotEmpty()) {
                                    append("ملاحظات التدقيق: ")
                                    report.anomalies.forEach { anomaly ->
                                        append(anomaly.title)
                                        append(". ")
                                    }
                                }
                            }
                            ttsHelper.speak(speechSummary)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "قراءة صوتية للتدقيق",
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Score & Status Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = headerColor.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, headerColor.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Circular Score Indicator
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(headerColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${report.score}%",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = headerColor
                                    )
                                    Text(
                                        text = "تطابق",
                                        fontSize = 9.sp,
                                        color = headerColor
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = report.summaryTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = headerColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = report.summaryDescription,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Extracted Accounting Details Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "البيانات المحاسبية المستخرجة للقيد:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GoldPrimary
                            )

                            // Tag & Direction Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("الوسم (Hashtag):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = GoldPrimary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = record.suggestedTag,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = GoldPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("طبيعة القيد:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val isCredit = record.direction == "CREDIT"
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCredit) StatusGreen.copy(alpha = 0.2f) else StatusRed.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (isCredit) "دائن / سداد" else "مدين / مشتريات",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = if (isCredit) StatusGreen else StatusRed,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            // Amount & Weight
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("المبلغ الإجمالي:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (record.amount > 0.0) "${String.format(Locale.US, "%,.2f", record.amount)} ج.م" else "غير محدد",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (record.amount > 0.0) MaterialTheme.colorScheme.onSurface else StatusOrange
                                    )
                                }

                                if (record.weightGrams > 0.0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("الوزن / الكمية:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${record.weightGrams} ${if (record.suggestedTag.contains("حديد")) "طن" else "جرام"}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = GoldPrimary
                                        )
                                    }
                                }
                            }

                            // Karat & Unit Price if available
                            if (record.karat != null || (record.unitPrice != null && record.unitPrice > 0.0)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (record.karat != null) {
                                        Column {
                                            Text("العيار:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "عيار ${record.karat}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    if (record.unitPrice != null && record.unitPrice > 0.0) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("سعر الوحدة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "${String.format(Locale.US, "%,.0f", record.unitPrice)} ج.م",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            // Party & Statement
                            if (record.counterPartyName.isNotBlank()) {
                                Text(
                                    text = "• الطرف / العميل: ${record.counterPartyName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (record.notes.isNotBlank()) {
                                Text(
                                    text = "• البيان: ${record.notes}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // LIVE BALANCE IMPACT CARD (التأثير المباشر على الرصيد)
                    val isCredit = record.direction == "CREDIT"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCredit) StatusGreen.copy(alpha = 0.12f) else StatusRed.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, if (isCredit) StatusGreen.copy(alpha = 0.4f) else StatusRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = if (isCredit) "📈" else "📉", fontSize = 22.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "التأثير المحاسبي المباشر على الرصيد:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isCredit) StatusGreen else StatusRed
                                )
                                Text(
                                    text = record.balanceImpactText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "يتم تحديث دفتر الأستاذ، سجل الصندوق، وشيت الإكسل فورياً.",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Audio Confirmation & Spoken Text Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, if (isPlayingAudio) GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingAudio) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeMute,
                                        contentDescription = null,
                                        tint = if (isPlayingAudio) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "المعاينة الصوتية للقيد:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (isPlayingAudio) {
                                            stopAudibleSummary()
                                        } else {
                                            playAudibleSummary()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPlayingAudio) StatusOrange else adaptiveGoldPrimary,
                                        contentColor = adaptiveOnPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingAudio) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPlayingAudio) "إيقاف الصوت" else "استمع صوتياً",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "\"${record.spokenConfirmationText}\"",
                                fontSize = 11.5.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Hashtag Message Preview Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "رسالة الهاشتاج الحقيقية التي سيتم تسجيلها بالدردشة:",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldPrimary
                            )
                            Text(
                                text = record.formattedHashtagMessage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Anomaly / Warning Alert Cards
                    if (report.anomalies.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ملاحظات وتنبيهات التدقيق المحاسبي (${report.anomalies.size}):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (!report.isApproved) StatusRed else StatusOrange
                            )

                            report.anomalies.forEach { anomaly ->
                                val cardBg = when (anomaly.severity) {
                                    AuditSeverity.CRITICAL -> StatusRed.copy(alpha = 0.12f)
                                    AuditSeverity.WARNING -> StatusOrange.copy(alpha = 0.12f)
                                    AuditSeverity.INFO -> GoldPrimary.copy(alpha = 0.12f)
                                }
                                val cardBorder = when (anomaly.severity) {
                                    AuditSeverity.CRITICAL -> StatusRed.copy(alpha = 0.4f)
                                    AuditSeverity.WARNING -> StatusOrange.copy(alpha = 0.4f)
                                    AuditSeverity.INFO -> GoldPrimary.copy(alpha = 0.4f)
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = cardBg,
                                    border = BorderStroke(1.dp, cardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(anomaly.severity.icon, fontSize = 16.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = anomaly.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = anomaly.description,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (anomaly.suggestedFix != null) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "مقترح التصحيح: ${anomaly.suggestedFix}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = GoldPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Edit / Correct Button
                        OutlinedButton(
                            onClick = {
                                ttsHelper.stop()
                                onEditRecord()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تعديل وتصحيح", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Confirm / Commit Button
                        Button(
                            onClick = {
                                ttsHelper.stop()
                                onConfirmCommit()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!report.isApproved) StatusOrange else adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = if (!report.isApproved) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = adaptiveOnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (!report.isApproved) "اعتماد برغم التحذير" else "تأكيد وإدراج القيد",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Cancel text button
                    TextButton(
                        onClick = {
                            ttsHelper.stop()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    ) {
                        Text(
                            text = "إلغاء العملية والتراجع",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }
    }
}
