package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import com.example.util.AuditVerificationReport
import com.example.util.ParsedVoiceAccountingRecord
import com.example.util.SmartAuditEngine
import com.example.util.SmartVoiceAccountingParser
import com.example.util.SpeechToTextManager
import com.example.util.TextToSpeechHelper
import java.util.Locale

/**
 * Minimalist, single-viewport (no-scroll) Voice Accounting panel:
 * Clean visual cues, sticky top input, live waveform, quick hashtag chips,
 * single-row parsed readout, and fixed bottom Cancel/Save actions.
 */
@Composable
fun VoiceAccountingDialog(
    contactName: String = "",
    existingHashtags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onInsertEntry: (record: ParsedVoiceAccountingRecord, report: AuditVerificationReport) -> Unit
) {
    val context = LocalContext.current
    var spokenText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var showAuditReview by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("ar-EG") }
    var showCustomTagInput by remember { mutableStateOf(false) }
    var customTagDraft by remember { mutableStateOf("") }

    val speechManager = remember { SpeechToTextManager(context) }
    val ttsHelper = remember { TextToSpeechHelper(context) }
    val rmsLevel by speechManager.rmsLevel.collectAsStateWithLifecycle()
    val speechError by speechManager.errorState.collectAsStateWithLifecycle()
    val isSpeechListening by speechManager.isListening.collectAsStateWithLifecycle()

    var isPlayingAudio by remember { mutableStateOf(false) }

    val availableHashtags = remember(existingHashtags) {
        val defaults = listOf("#فاتورة", "#توريد", "#دفعة", "#سداد", "#قبض", "#صرف")
        (existingHashtags.map { if (it.startsWith("#")) it else "#$it" } + defaults).distinct()
    }

    val parsedRecord = remember(spokenText) {
        if (spokenText.isNotBlank()) {
            SmartVoiceAccountingParser.parseVoiceAccountingText(spokenText, contactName)
        } else null
    }

    val auditReport = remember(parsedRecord) {
        parsedRecord?.let { SmartAuditEngine.auditEntry(it) }
    }

    fun applyHashtagToSpokenText(rawTag: String) {
        val normalizedTag = "#" + rawTag.trim().removePrefix("#").replace(" ", "_")
        if (normalizedTag == "#") return
        val clean = spokenText.replace(Regex("#[\\u0600-\\u06FF\\w]+"), "").trim()
        spokenText = if (clean.isBlank()) "$normalizedTag " else "$normalizedTag $clean"
    }

    fun playSpokenAudio() {
        if (parsedRecord != null && parsedRecord.spokenConfirmationText.isNotBlank()) {
            isPlayingAudio = true
            ttsHelper.speak(parsedRecord.spokenConfirmationText) { isPlayingAudio = false }
        }
    }

    fun stopSpokenAudio() {
        ttsHelper.stop()
        isPlayingAudio = false
    }

    fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(40L)
            }
        } catch (_: Exception) {}
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            triggerHaptic()
            stopSpokenAudio()
            isListening = true
            speechManager.startListening(
                languageCode = selectedLanguage,
                onPartial = { text ->
                    if (text.isNotBlank()) spokenText = SmartVoiceAccountingParser.normalizeSpokenHashtags(text)
                },
                onFinal = { text ->
                    if (text.isNotBlank()) {
                        spokenText = SmartVoiceAccountingParser.normalizeSpokenHashtags(text)
                    }
                    isListening = false
                }
            )
        } else {
            Toast.makeText(context, "يرجى منح صلاحية الميكروفون", Toast.LENGTH_SHORT).show()
        }
    }

    fun startListeningWithPermission() {
        stopSpokenAudio()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            triggerHaptic()
            isListening = true
            speechManager.startListening(
                languageCode = selectedLanguage,
                onPartial = { text ->
                    if (text.isNotBlank()) spokenText = SmartVoiceAccountingParser.normalizeSpokenHashtags(text)
                },
                onFinal = { text ->
                    if (text.isNotBlank()) {
                        spokenText = SmartVoiceAccountingParser.normalizeSpokenHashtags(text)
                    }
                    isListening = false
                }
            )
        }
    }

    fun stopListening() {
        triggerHaptic()
        speechManager.stopListening()
        isListening = false
    }

    LaunchedEffect(Unit) { startListeningWithPermission() }

    DisposableEffect(Unit) {
        onDispose {
            speechManager.destroy()
            ttsHelper.shutdown()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "voice_wave_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    if (showAuditReview && parsedRecord != null && auditReport != null) {
        PreCommitAuditDialog(
            record = parsedRecord,
            report = auditReport,
            onConfirmCommit = {
                showAuditReview = false
                speechManager.destroy()
                ttsHelper.speak("تم الحفظ")
                onInsertEntry(parsedRecord, auditReport)
                onDismiss()
            },
            onEditRecord = { showAuditReview = false },
            onDismiss = { showAuditReview = false }
        )
    }

    Dialog(
        onDismissRequest = {
            speechManager.destroy()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .testTag("voice_accounting_sheet"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 14.dp,
                border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Compact Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(adaptiveGoldPrimary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = adaptiveGoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = contactName.ifBlank { "المساعد الصوتي" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("ar-EG" to "EG", "ar-SA" to "SA", "en-US" to "EN").forEach { (lang, code) ->
                                val isSel = selectedLanguage == lang
                                Surface(
                                    onClick = {
                                        selectedLanguage = lang
                                        if (isListening || isSpeechListening) {
                                            stopListening()
                                            startListeningWithPermission()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) adaptiveGoldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = code,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) adaptiveOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    speechManager.destroy()
                                    onDismiss()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Single-line input + quick clear
                    OutlinedTextField(
                        value = spokenText,
                        onValueChange = { spokenText = it },
                        placeholder = {
                            Text(
                                text = "#فاتورة 2500",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        trailingIcon = {
                            if (spokenText.isNotBlank()) {
                                IconButton(onClick = {
                                    stopSpokenAudio()
                                    spokenText = ""
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = adaptiveGoldPrimary,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voice_accounting_top_input")
                    )

                    // Compact Hashtag Chips Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            FilterChip(
                                selected = showCustomTagInput,
                                onClick = { showCustomTagInput = !showCustomTagInput },
                                label = { Text("+ #", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("voice_custom_hashtag_toggle")
                            )
                        }
                        items(availableHashtags.size) { index ->
                            val tagChoice = availableHashtags[index]
                            val isSel = spokenText.contains(tagChoice, ignoreCase = true) ||
                                parsedRecord?.suggestedTag == tagChoice
                            FilterChip(
                                selected = isSel,
                                onClick = { applyHashtagToSpokenText(tagChoice) },
                                label = { Text(tagChoice, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }

                    AnimatedVisibility(visible = showCustomTagInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customTagDraft,
                                onValueChange = { customTagDraft = it },
                                placeholder = { Text("#...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (customTagDraft.isNotBlank()) {
                                        applyHashtagToSpokenText(customTagDraft)
                                        customTagDraft = ""
                                        showCustomTagInput = false
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = adaptiveGoldPrimary,
                                    contentColor = adaptiveOnPrimary
                                ),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "إضافة", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Centerpiece Mic + Waveform Strip (zero verbose paragraphs)
                    val listeningActive = isListening || isSpeechListening
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .scale(if (listeningActive) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    if (listeningActive) {
                                        Brush.radialGradient(listOf(StatusOrange, adaptiveGoldPrimary))
                                    } else {
                                        Brush.radialGradient(listOf(adaptiveGoldPrimary.copy(alpha = 0.35f), MaterialTheme.colorScheme.surfaceVariant))
                                    }
                                )
                                .clickable {
                                    if (listeningActive) stopListening() else startListeningWithPermission()
                                }
                                .testTag("voice_accounting_mic_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (listeningActive) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = if (listeningActive) "إيقاف" else "تسجيل",
                                tint = if (listeningActive) Color.White else adaptiveGoldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            val activeRms = if (listeningActive) rmsLevel.coerceIn(0.24f, 1.0f) else 0.14f
                            repeat(24) { i ->
                                val wave = if (listeningActive) {
                                    (kotlin.math.sin((wavePhase + i * 0.45f).toDouble()).toFloat() + 1f) * 0.5f
                                } else {
                                    ((i * 5) % 9) / 14f
                                }
                                val barHeight = (6f + (activeRms * 30f * (0.35f + 0.65f * wave))).dp
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (listeningActive) adaptiveGoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                        )
                                )
                            }
                        }

                        if (parsedRecord != null) {
                            IconButton(
                                onClick = { if (isPlayingAudio) stopSpokenAudio() else playSpokenAudio() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingAudio) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "استماع",
                                    tint = adaptiveGoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (!speechError.isNullOrBlank()) {
                        Text(
                            text = speechError.orEmpty(),
                            fontSize = 11.sp,
                            color = StatusRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Compact 1-Line Live Readout Chips (no paragraphs)
                    if (parsedRecord != null) {
                        val isCredit = parsedRecord.direction == "CREDIT"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCredit) StatusGreen.copy(alpha = 0.16f) else StatusRed.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = if (isCredit) "دائن" else "مدين",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = if (isCredit) StatusGreen else StatusRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (parsedRecord.amount > 0.0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%,.0f", parsedRecord.amount)} ج.م",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (parsedRecord.weightGrams > 0.0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${parsedRecord.weightGrams} جم",
                                        fontSize = 11.5.sp,
                                        color = adaptiveGoldPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (parsedRecord.suggestedTag.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = adaptiveGoldPrimary.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = parsedRecord.suggestedTag,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = adaptiveGoldPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Fixed Cancel & Save Footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                stopSpokenAudio()
                                speechManager.destroy()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("voice_accounting_cancel_btn")
                        ) {
                            Text("إلغاء", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (parsedRecord != null && auditReport != null) {
                                    if (auditReport.hasWarnings || !auditReport.isApproved) {
                                        stopSpokenAudio()
                                        showAuditReview = true
                                    } else {
                                        triggerHaptic()
                                        stopSpokenAudio()
                                        speechManager.destroy()
                                        ttsHelper.speak("تم الحفظ")
                                        onInsertEntry(parsedRecord, auditReport)
                                        onDismiss()
                                    }
                                } else {
                                    Toast.makeText(context, "أدخل تفاصيل القيد", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("voice_accounting_save_btn")
                        ) {
                            Text("حفظ", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
