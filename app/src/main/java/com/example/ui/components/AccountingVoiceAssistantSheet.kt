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
import com.example.data.model.DailyAccountingRecordEntity
import com.example.data.model.HiddenProfileEntity
import com.example.data.model.WorkGroupEntity
import com.example.ui.theme.*
import com.example.util.AccountingSpeechToTextEngine
import com.example.util.AccountingVoiceCommand
import com.example.util.AppNavDestination
import com.example.util.HubModalType
import com.example.util.LocalAppLanguage
import com.example.util.ParsedVoiceAccountingRecord
import com.example.util.SmartVoiceAccountingParser
import com.example.util.SuggestedAssistantAction
import com.example.util.TextToSpeechHelper
import com.example.util.UnifiedBusinessAccountingSummary
import com.example.util.acctStr
import java.util.Locale

/**
 * Minimalist single-viewport (no-scroll) Accounting Voice & Smart Assistant dialog.
 * Instant mic + live waveform, quick command/#hashtag chips, live financial Q&A,
 * feature explanations, and one-tap navigation across hub & app modules.
 */
@Composable
fun AccountingVoiceAssistantSheet(
    activeProfile: HiddenProfileEntity?,
    records: List<DailyAccountingRecordEntity>,
    groups: List<WorkGroupEntity>,
    unifiedSummary: UnifiedBusinessAccountingSummary? = null,
    pendingComplaintsCount: Int = 0,
    onInsertRecord: (DailyAccountingRecordEntity) -> Unit,
    onNavigateToSection: (Int) -> Unit,
    onOpenModal: (HubModalType) -> Unit = {},
    onOpenAppScreen: (AppNavDestination) -> Unit = {},
    onExportExcel: () -> Unit,
    onExportUnifiedExcel: () -> Unit = onExportExcel,
    onExportPdf: () -> Unit,
    onOpenFullVoiceAgent: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val engine = remember { AccountingSpeechToTextEngine(context) }
    val ttsHelper = remember { TextToSpeechHelper(context) }

    val isListening by engine.isListening.collectAsStateWithLifecycle()
    val spokenTextState by engine.spokenText.collectAsStateWithLifecycle()
    val partialTextState by engine.partialText.collectAsStateWithLifecycle()
    val rmsLevel by engine.rmsLevel.collectAsStateWithLifecycle()
    val errorMessage by engine.errorMessage.collectAsStateWithLifecycle()

    var manualText by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("ar-EG") }
    var isPlayingAudio by remember { mutableStateOf(false) }

    val appLanguage = LocalAppLanguage.current
    val activeText = when {
        manualText.isNotBlank() -> manualText
        spokenTextState.isNotBlank() -> SmartVoiceAccountingParser.normalizeSpokenHashtags(spokenTextState)
        else -> SmartVoiceAccountingParser.normalizeSpokenHashtags(partialTextState)
    }

    val detectedCommand = remember(activeText, activeProfile, records, groups, unifiedSummary, pendingComplaintsCount, appLanguage) {
        if (activeText.isNotBlank()) {
            engine.interpretAccountingVoiceCommand(
                rawText = activeText,
                activeProfile = activeProfile,
                records = records,
                groups = groups,
                unifiedSummary = unifiedSummary,
                pendingComplaintsCount = pendingComplaintsCount,
                language = appLanguage
            )
        } else {
            null
        }
    }

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

    fun speak(text: String) {
        if (text.isBlank()) return
        ttsHelper.stop()
        isPlayingAudio = true
        ttsHelper.speak(text) { isPlayingAudio = false }
    }

    fun applyHashtag(tag: String) {
        val cleanTag = "#" + tag.trim().removePrefix("#").replace(" ", "_")
        val base = activeText.replace(Regex("#[\\u0600-\\u06FF\\w]+"), "").trim()
        manualText = if (base.isBlank()) "$cleanTag " else "$cleanTag $base"
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            triggerHaptic()
            engine.startListening(languageCode = selectedLanguage, onFinal = { triggerHaptic() })
        } else {
            Toast.makeText(context, "يرجى تفعيل الميكروفون", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleListening() {
        triggerHaptic()
        ttsHelper.stop()
        isPlayingAudio = false
        if (isListening) {
            engine.stopListening()
        } else {
            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                engine.startListening(languageCode = selectedLanguage, onFinal = { triggerHaptic() })
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            toggleListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.stop()
            engine.destroy()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "acct_voice_wave")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.14f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(580, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
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

    var showFullReviewDialog by remember { mutableStateOf<Pair<DailyAccountingRecordEntity, ParsedVoiceAccountingRecord>?>(null) }

    if (showFullReviewDialog != null) {
        val (candidate, parsed) = showFullReviewDialog!!
        VoiceTransactionReviewDialog(
            candidateRecord = candidate,
            parsedVoiceRecord = parsed,
            onConfirmDatabaseUpdate = { finalRecord ->
                onInsertRecord(finalRecord)
                showFullReviewDialog = null
                onDismiss()
            },
            onDismiss = { showFullReviewDialog = null }
        )
    }

    Dialog(
        onDismissRequest = {
            ttsHelper.stop()
            engine.stopListening()
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
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 14.dp,
                border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Compact Header + Dialect Switch
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
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Text(
                                text = activeProfile?.name ?: acctStr("assistant_title"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            listOf("ar-EG" to "EG", "ar-SA" to "SA", "en-US" to "EN").forEach { (code, label) ->
                                val isSel = selectedLanguage == code
                                Surface(
                                    onClick = {
                                        selectedLanguage = code
                                        if (isListening) {
                                            toggleListening()
                                            toggleListening()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) adaptiveGoldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) adaptiveOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    ttsHelper.stop()
                                    engine.stopListening()
                                    onDismiss()
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.Close, contentDescription = acctStr("close"), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // 2. Single-line Input Bar
                    OutlinedTextField(
                        value = if (manualText.isNotBlank()) manualText else activeText,
                        onValueChange = { manualText = it },
                        placeholder = { Text(acctStr("assistant_hint"), fontSize = 12.5.sp) },
                        trailingIcon = {
                            if (activeText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        manualText = ""
                                        engine.cancelListening()
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = acctStr("close"), modifier = Modifier.size(17.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("accounting_voice_input")
                    )

                    // 3. Quick Commands & #Hashtag Strip
                    val tags = remember(appLanguage) {
                        if (appLanguage == com.example.util.AppLanguage.ENGLISH) {
                            listOf("Open Journal", "Explain Balance", "Open Gold", "Unified Excel", "#Expense", "#Revenue")
                        } else {
                            listOf("افتح اليومية", "اشرح الرصيد", "افتح الذهب", "شيت إكسل", "#مصروفات", "#توريد", "#سداد")
                        }
                    }
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(tags.size) { idx ->
                            val t = tags[idx]
                            FilterChip(
                                selected = activeText == t || (t.startsWith("#") && activeText.contains(t)),
                                onClick = {
                                    if (t.startsWith("#")) applyHashtag(t) else manualText = t
                                },
                                shape = RoundedCornerShape(12.dp),
                                label = { Text(t, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }

                    // 4. Centerpiece Mic + Live Waveform
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isListening) adaptiveStatusRed else adaptiveGoldPrimary,
                            modifier = Modifier
                                .size(58.dp)
                                .scale(pulseScale)
                                .clickable { toggleListening() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                                    contentDescription = "تسجيل صوتي",
                                    tint = if (isListening) Color.White else adaptiveOnPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            val activeRms = if (isListening) rmsLevel.coerceIn(0.24f, 1.0f) else 0.14f
                            repeat(24) { i ->
                                val wave = if (isListening) {
                                    (kotlin.math.sin((wavePhase + i * 0.45f).toDouble()).toFloat() + 1f) * 0.5f
                                } else {
                                    ((i * 5) % 8) / 14f
                                }
                                val barHeight = (5f + (activeRms * 27f * (0.35f + 0.65f * wave))).dp
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isListening) adaptiveGoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                ttsHelper.stop()
                                engine.stopListening()
                                onOpenFullVoiceAgent()
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = acctStr("cmd_voice"),
                                tint = adaptiveGoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = adaptiveStatusRed,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 5. Compact Live Readout + Action Row
                    val executeSuggested: (SuggestedAssistantAction) -> Unit = { action ->
                        triggerHaptic()
                        when (action) {
                            is SuggestedAssistantAction.OpenHubSection -> {
                                onNavigateToSection(action.sectionIndex)
                                onDismiss()
                            }
                            is SuggestedAssistantAction.OpenHubModal -> {
                                onOpenModal(action.modalType)
                                onDismiss()
                            }
                            is SuggestedAssistantAction.OpenAppScreen -> {
                                onOpenAppScreen(action.destination)
                                onDismiss()
                            }
                            is SuggestedAssistantAction.ExportReport -> {
                                when (action.format) {
                                    "EXCEL_UNIFIED" -> onExportUnifiedExcel()
                                    "EXCEL" -> onExportExcel()
                                    else -> onExportPdf()
                                }
                                onDismiss()
                            }
                        }
                    }

                    AnimatedVisibility(visible = detectedCommand != null) {
                        when (val cmd = detectedCommand) {
                            is AccountingVoiceCommand.RecordTransaction -> {
                                val isCredit = cmd.candidateEntity.dailyRevenue > 0 || cmd.candidateEntity.incomingAmount > 0
                                val amt = if (isCredit) (cmd.candidateEntity.dailyRevenue + cmd.candidateEntity.incomingAmount) else cmd.candidateEntity.outgoingAmount
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = (if (isCredit) adaptiveStatusGreen else adaptiveStatusRed).copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = if (isCredit) "دائن" else "مدين",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = if (isCredit) adaptiveStatusGreen else adaptiveStatusRed,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        if (amt > 0.0) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "${String.format(Locale.US, "%,.0f", amt)} ج.م",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = adaptiveGoldPrimary.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = cmd.parsedVoiceRecord.suggestedTag,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = adaptiveGoldPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                if (isPlayingAudio) {
                                                    ttsHelper.stop()
                                                    isPlayingAudio = false
                                                } else {
                                                    speak(cmd.spokenConfirmation)
                                                }
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = if (isPlayingAudio) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = acctStr("listening"),
                                                tint = adaptiveGoldPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                triggerHaptic()
                                                ttsHelper.stop()
                                                showFullReviewDialog = Pair(cmd.candidateEntity, cmd.parsedVoiceRecord)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("open_voice_review_dialog_btn")
                                        ) {
                                            Text(acctStr("review_action"), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        }

                                        val savedFeedback = acctStr("entry_saved")
                                        Button(
                                            onClick = {
                                                triggerHaptic()
                                                onInsertRecord(cmd.candidateEntity)
                                                speak(savedFeedback)
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("confirm_voice_record_btn")
                                        ) {
                                            Text(acctStr("save"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                            is AccountingVoiceCommand.BalanceQuery -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(adaptiveStatusGreen.copy(alpha = 0.12f))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(cmd.title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                        Text(cmd.answer, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = adaptiveStatusGreen)
                                    }
                                    Text(cmd.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            is AccountingVoiceCommand.DeficitQuery -> {
                                val accent = if (cmd.totalDeficit > 0) adaptiveStatusRed else adaptiveStatusGreen
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(accent.copy(alpha = 0.12f))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(cmd.title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    Text(cmd.answer, fontSize = 12.sp, color = accent, fontWeight = FontWeight.SemiBold)
                                    cmd.suggestedAction?.let { action ->
                                        Button(
                                            onClick = { executeSuggested(action) },
                                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                        ) {
                                            Text(action.label, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            is AccountingVoiceCommand.SectionNavigation -> {
                                Button(
                                    onClick = {
                                        triggerHaptic()
                                        onNavigateToSection(cmd.sectionIndex)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("voice_nav_section_btn")
                                ) {
                                    Text("${acctStr("open_section_action")}: ${cmd.sectionTitle}", fontWeight = FontWeight.Bold)
                                }
                            }
                            is AccountingVoiceCommand.OpenHubModal -> {
                                Button(
                                    onClick = {
                                        triggerHaptic()
                                        onOpenModal(cmd.modalType)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("voice_open_modal_btn")
                                ) {
                                    Text("${acctStr("open_section_action")}: ${cmd.title}", fontWeight = FontWeight.Bold)
                                }
                            }
                            is AccountingVoiceCommand.OpenAppScreen -> {
                                Button(
                                    onClick = {
                                        triggerHaptic()
                                        onOpenAppScreen(cmd.destination)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("voice_open_app_screen_btn")
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${acctStr("open_section_action")}: ${cmd.title}", fontWeight = FontWeight.Bold)
                                }
                            }
                            is AccountingVoiceCommand.ExportReport -> {
                                Button(
                                    onClick = {
                                        triggerHaptic()
                                        when (cmd.format) {
                                            "EXCEL_UNIFIED" -> onExportUnifiedExcel()
                                            "EXCEL" -> onExportExcel()
                                            else -> onExportPdf()
                                        }
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveStatusGreen, contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(cmd.message, fontWeight = FontWeight.Bold)
                                }
                            }
                            is AccountingVoiceCommand.ExplainOrAnswer -> {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = adaptiveGoldPrimary.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(cmd.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = adaptiveGoldPrimary)
                                            IconButton(
                                                onClick = {
                                                    if (isPlayingAudio) {
                                                        ttsHelper.stop()
                                                        isPlayingAudio = false
                                                    } else {
                                                        speak(cmd.spokenReply)
                                                    }
                                                },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlayingAudio) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = acctStr("listening"),
                                                    tint = adaptiveGoldPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = cmd.answer,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        cmd.suggestedAction?.let { action ->
                                            Button(
                                                onClick = { executeSuggested(action) },
                                                colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                            ) {
                                                Text(action.label, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                            }
                                        }
                                    }
                                }
                            }
                            is AccountingVoiceCommand.SmartAssistantAdvice -> {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = cmd.answer,
                                            fontSize = 12.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        cmd.suggestedAction?.let { action ->
                                            OutlinedButton(
                                                onClick = { executeSuggested(action) },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                            ) {
                                                Text(action.label, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }
    }
}
