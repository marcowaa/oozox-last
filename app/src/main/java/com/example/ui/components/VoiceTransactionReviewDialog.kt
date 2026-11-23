package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DailyAccountingRecordEntity
import com.example.ui.theme.*
import com.example.util.AccountingSpeechToTextEngine
import com.example.util.ParsedVoiceAccountingRecord
import com.example.util.TextToSpeechHelper
import java.util.Locale

/**
 * Voice-enabled Transaction Review Dialog for the Accounting Dashboard.
 * The AI synthesizes and reads back recorded transaction details (amount, hashtag category,
 * counterparty, balance impact) aloud via Text-to-Speech before the user confirms the final database update.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTransactionReviewDialog(
    candidateRecord: DailyAccountingRecordEntity,
    parsedVoiceRecord: ParsedVoiceAccountingRecord? = null,
    onConfirmDatabaseUpdate: (DailyAccountingRecordEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val ttsHelper = remember { TextToSpeechHelper(context) }
    val speechEngine = remember { AccountingSpeechToTextEngine(context) }

    val isCredit = candidateRecord.incomingAmount > 0 || candidateRecord.dailyRevenue > 0
    val rawAmount = if (isCredit) (candidateRecord.incomingAmount + candidateRecord.dailyRevenue) else candidateRecord.outgoingAmount

    var amountText by remember { mutableStateOf(if (rawAmount > 0) rawAmount.toInt().toString() else "0") }
    var hashtagText by remember {
        mutableStateOf(
            parsedVoiceRecord?.suggestedTag?.takeIf { it.isNotBlank() }
                ?: if (isCredit) "#مقبوضات" else "#مصروفات"
        )
    }
    var partyNameText by remember { mutableStateOf(candidateRecord.staffName) }
    var notesText by remember { mutableStateOf(candidateRecord.notes) }
    var isEditMode by remember { mutableStateOf(false) }

    var isAiSpeaking by remember { mutableStateOf(false) }
    var isListeningForVoiceConfirm by remember { mutableStateOf(false) }
    var autoSpokenTriggered by remember { mutableStateOf(false) }

    fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(40)
            }
        } catch (_: Exception) {}
    }

    val spokenReviewScript = remember(amountText, hashtagText, partyNameText, isCredit) {
        val currentAmt = amountText.toDoubleOrNull() ?: rawAmount
        val opType = if (isCredit) "إيراد وقبض" else "مصروف ومدفوعات"
        val partyClause = if (partyNameText.isNotBlank() && partyNameText != "المساعد الصوتي") "، الطرف: $partyNameText" else ""
        val goldClause = if ((parsedVoiceRecord?.weightGrams ?: 0.0) > 0) {
            "، الوزن: ${parsedVoiceRecord?.weightGrams} جرام" + (parsedVoiceRecord?.karat?.let { " عيار $it" } ?: "")
        } else ""
        val tagClause = if (hashtagText.isNotBlank()) "، تصنيف الهاشتاج: $hashtagText" else ""

        "مراجعة تفاصيل القيد قبل الحفظ: تم رصد قيد $opType، بمبلغ ${currentAmt.toInt()} جنيه مصري$tagClause$partyClause$goldClause. هل تؤكد التحديث النهائي في قاعدة البيانات؟"
    }

    fun startAiReadback() {
        ttsHelper.stop()
        isAiSpeaking = true
        triggerHaptic()
        ttsHelper.speak(spokenReviewScript) {
            isAiSpeaking = false
        }
    }

    fun stopAiReadback() {
        ttsHelper.stop()
        isAiSpeaking = false
    }

    // Auto-read back transaction details when the dialog first appears
    LaunchedEffect(Unit) {
        if (!autoSpokenTriggered) {
            autoSpokenTriggered = true
            // Slight delay so the dialog animation finishes gracefully
            kotlinx.coroutines.delay(450)
            startAiReadback()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.stop()
            speechEngine.destroy()
        }
    }

    // Audio equalizer animation bars when AI is reading aloud
    val infiniteTransition = rememberInfiniteTransition(label = "ai_readback_bars")
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = 34f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 18f, targetValue = 30f,
        animationSpec = infiniteRepeatable(tween(290, easing = FastOutLinearInEasing), RepeatMode.Reverse),
        label = "bar3"
    )
    val bar4Height by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = 32f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "bar4"
    )

    fun commitFinalRecord() {
        triggerHaptic()
        stopAiReadback()
        val parsedAmt = amountText.toDoubleOrNull() ?: rawAmount
        val finalRecord = candidateRecord.copy(
            incomingAmount = if (isCredit) parsedAmt else 0.0,
            dailyRevenue = if (isCredit && candidateRecord.dailyRevenue > 0) parsedAmt else 0.0,
            outgoingAmount = if (!isCredit) parsedAmt else 0.0,
            staffName = partyNameText,
            notes = "[مراجعة صوتية معتمدة] $notesText ($hashtagText)",
            timestamp = System.currentTimeMillis()
        )
        onConfirmDatabaseUpdate(finalRecord)
        ttsHelper.speak("تم اعتماد القيد وتحديث قاعدة البيانات بنجاح.")
        Toast.makeText(context, "تم تأكيد التحديث النهائي في قاعدة البيانات بنجاح!", Toast.LENGTH_SHORT).show()
        onDismiss()
    }

    Dialog(
        onDismissRequest = {
            stopAiReadback()
            onDismiss()
        },
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
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.5f)),
                shadowElevation = 18.dp,
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = 650.dp)
                    .testTag("voice_transaction_review_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with AI Voice Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(GoldPrimary.copy(alpha = 0.25f), GoldLight.copy(alpha = 0.15f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Text(
                                text = "المراجعة الصوتية للقيد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = {
                                stopAiReadback()
                                onDismiss()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                // AI Voice Readback Player Card (Audio Waveform & Playback Controls)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isAiSpeaking) GoldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(
                        1.dp,
                        if (isAiSpeaking) GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Animated Audio Wave Equalizer Bars
                            if (isAiSpeaking) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Box(Modifier.width(4.dp).height(bar1Height.dp).clip(RoundedCornerShape(2.dp)).background(GoldPrimary))
                                    Box(Modifier.width(4.dp).height(bar2Height.dp).clip(RoundedCornerShape(2.dp)).background(GoldPrimary))
                                    Box(Modifier.width(4.dp).height(bar3Height.dp).clip(RoundedCornerShape(2.dp)).background(GoldPrimary))
                                    Box(Modifier.width(4.dp).height(bar4Height.dp).clip(RoundedCornerShape(2.dp)).background(GoldPrimary))
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (isAiSpeaking) "🎙️ الذكاء الاصطناعي يقرأ القيد الآن..." else "استمع لمراجعة القيد صوتياً",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isAiSpeaking) GoldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تدقيق آلي للمبلغ، الهاشتاج، والطرف",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Playback Control Button
                        OozoxCircleIconButton(
                            onClick = {
                                if (isAiSpeaking) {
                                    stopAiReadback()
                                } else {
                                    startAiReadback()
                                }
                            },
                            imageVector = if (isAiSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isAiSpeaking) "إيقاف الصوت" else "إعادة القراءة الصوتية",
                            tint = if (isAiSpeaking) StatusOrange else GoldPrimary,
                            containerColor = if (isAiSpeaking) StatusOrange.copy(alpha = 0.2f) else GoldPrimary.copy(alpha = 0.2f),
                            modifier = Modifier.testTag("toggle_voice_readback_btn")
                        )
                    }
                }

                // Transaction Review Details Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Amount & Direction
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Direction Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCredit) StatusGreen.copy(alpha = 0.12f) else StatusRed.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, if (isCredit) StatusGreen.copy(alpha = 0.4f) else StatusRed.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("نوع الحركة", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isCredit) "🟢 إيراد / مقبوضات" else "🔴 مصروف / مدفوعات",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isCredit) StatusGreen else StatusRed
                                )
                            }
                        }

                        // Amount Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("المبلغ المالي", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                val displayAmt = amountText.toDoubleOrNull() ?: rawAmount
                                Text(
                                    text = "${String.format(Locale.US, "%,.2f", displayAmt)} ج.م",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = if (isCredit) StatusGreen else GoldPrimary
                                )
                            }
                        }
                    }

                    // Row 2: Hashtag Category & Counterparty
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Hashtag Category Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = GoldPrimary.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("تصنيف الهاشتاج", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tag, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = hashtagText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = GoldPrimary
                                    )
                                }
                            }
                        }

                        // Counterparty Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("الطرف / الموظف", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (partyNameText.isNotBlank()) partyNameText else "خزينة المركز",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Optional Gold Weights Card
                    if ((parsedVoiceRecord?.weightGrams ?: 0.0) > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusGreen.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Scale, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "الوزن المقيد: ${parsedVoiceRecord?.weightGrams} جرام" + (parsedVoiceRecord?.karat?.let { " عيار $it" } ?: ""),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreen
                                )
                            }
                        }
                    }

                    // Balance Impact Summary Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isCredit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isCredit) StatusGreen else StatusOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            val displayAmt = amountText.toDoubleOrNull() ?: rawAmount
                            Text(
                                text = if (isCredit) {
                                    "الأثر المالي: زيادة رصيد السيولة بالصندوق بمقدار ${displayAmt.toInt()} ج.م"
                                } else {
                                    "الأثر المالي: خصم ${displayAmt.toInt()} ج.م من رصيد الصندوق وتصنيفها كمصروف"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Quick Edit Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isEditMode = !isEditMode }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEditMode) "إخفاء خيارات التعديل اليدوي ▲" else "تعديل المبلغ أو الهاشتاج يدوياً ▼",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldPrimary
                    )
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.ExpandLess else Icons.Default.Edit,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Quick Editable Inputs Section
                AnimatedVisibility(
                    visible = isEditMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("المبلغ (ج.م)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = hashtagText,
                            onValueChange = { hashtagText = it },
                            label = { Text("تصنيف الهاشتاج (مثال: #مصروفات)") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = partyNameText,
                            onValueChange = { partyNameText = it },
                            label = { Text("اسم الطرف المتعامل") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Hands-free Voice Confirmation Listener Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isListeningForVoiceConfirm) StatusRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(
                        1.dp,
                        if (isListeningForVoiceConfirm) StatusRed else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            triggerHaptic()
                            if (isListeningForVoiceConfirm) {
                                speechEngine.stopListening()
                                isListeningForVoiceConfirm = false
                            } else {
                                stopAiReadback()
                                speechEngine.startListening(
                                    onFinal = { spokenText ->
                                        val norm = spokenText.trim().lowercase(Locale.getDefault())
                                        if (norm.contains("نعم") || norm.contains("تاكيد") || norm.contains("تأكيد") ||
                                            norm.contains("اعتمد") || norm.contains("سجل") || norm.contains("تمام") || norm.contains("موافق")
                                        ) {
                                            commitFinalRecord()
                                        }
                                        isListeningForVoiceConfirm = false
                                    }
                                )
                                isListeningForVoiceConfirm = true
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isListeningForVoiceConfirm) Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = "تأكيد صوتي",
                            tint = if (isListeningForVoiceConfirm) StatusRed else GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isListeningForVoiceConfirm) "🎙️ قل (تأكيد) أو (اعتمد) للحفظ الصوتي المباشر..." else "تأكيد صوتي بدون لمس الشاشة",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isListeningForVoiceConfirm) StatusRed else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "يمكنك النطق بكلمة «تأكيد» أو «نعم» للاعتماد فوراً",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }

                // Pinned Action Buttons (side-by-side compact 48dp touch targets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            stopAiReadback()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.8f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { commitFinalRecord() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = adaptiveGoldPrimary,
                            contentColor = adaptiveOnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .heightIn(min = 48.dp)
                            .testTag("confirm_database_update_btn")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تأكيد واعتماد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
        }
    }
}
