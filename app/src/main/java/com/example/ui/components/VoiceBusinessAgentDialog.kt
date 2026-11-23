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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.repository.GoldChatRepository
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * State machine for the Autonomous Voice Business Action Agent.
 */
enum class AgentSessionState {
    IDLE,
    LISTENING_COMMAND,
    ANALYZING,
    CONFIRMING_ACTION,
    LISTENING_CONFIRMATION,
    EXECUTING,
    SUCCESS_RESULT,
    CANCELLED
}

/**
 * Minimalist, single-viewport (no-scroll) Voice Business Action Agent dialog.
 * Clean audio waveform, sticky top command bar, hashtag pills, compact visual readout,
 * and anchored bottom confirmation controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceBusinessAgentDialog(
    repository: GoldChatRepository,
    mainViewModel: MainViewModel,
    defaultContactName: String = "",
    onDismiss: () -> Unit,
    onNavigateToChat: (chatId: String) -> Unit = {},
    onNavigateToDestination: (AppNavDestination) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appLanguage = LocalAppLanguage.current
    val coroutineScope = rememberCoroutineScope()

    var sessionState by remember { mutableStateOf(AgentSessionState.IDLE) }
    var spokenInput by remember { mutableStateOf("") }
    var manualInput by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<AiBusinessAction?>(null) }
    var lastActionResult by remember { mutableStateOf<AiVoiceBusinessAgentEngine.ActionResult.Success?>(null) }
    var detectedNavDestination by remember { mutableStateOf<AppNavDestination?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isSpeechConfirmationEnabled by remember {
        mutableStateOf(AiVoiceAgentPreferences.isSpeechConfirmationEnabled(context))
    }
    val isAutoListenConfirmationEnabled = remember {
        AiVoiceAgentPreferences.isAutoListenConfirmationEnabled(context)
    }

    val speechManager = remember { SpeechToTextManager(context) }
    val ttsHelper = remember { TextToSpeechHelper(context) }

    val rmsLevel by speechManager.rmsLevel.collectAsStateWithLifecycle()
    val isSpeechListening by speechManager.isListening.collectAsStateWithLifecycle()

    var consultationAnswer by remember { mutableStateOf<String?>(null) }
    var showCustomTagField by remember { mutableStateOf(false) }
    var customTagInput by remember { mutableStateOf("") }
    val allHashtags by repository.allHashtagAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val quickTags = remember(allHashtags) {
        val defaults = listOf("#فاتورة", "#دفعة", "#سداد", "#توريد", "#خصم", "#مصروفات")
        (allHashtags.map { if (it.tag.startsWith("#")) it.tag else "#${it.tag}" } + defaults).distinct().take(12)
    }

    fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(45)
            }
        } catch (_: Exception) {}
    }

    fun applyHashtagToCommand(rawTag: String) {
        val clean = "#" + rawTag.trim().removePrefix("#").replace(" ", "_")
        if (clean == "#") return
        val stripped = manualInput.replace(Regex("#[\\u0600-\\u06FF\\w]+"), "").trim()
        manualInput = if (stripped.isBlank()) "$clean " else "$clean $stripped"
    }

    fun cancelPendingAction() {
        ttsHelper.stop()
        speechManager.stopListening()
        pendingAction = null
        sessionState = AgentSessionState.CANCELLED
    }

    fun executeConfirmedActionNow() {
        val action = pendingAction ?: return
        sessionState = AgentSessionState.EXECUTING
        ttsHelper.stop()
        speechManager.stopListening()

        coroutineScope.launch {
            val result = AiVoiceBusinessAgentEngine.executeConfirmedAction(
                context = context,
                repository = repository,
                mainViewModel = mainViewModel,
                action = action
            )

            when (result) {
                is AiVoiceBusinessAgentEngine.ActionResult.Success -> {
                    lastActionResult = result
                    sessionState = AgentSessionState.SUCCESS_RESULT
                    triggerVibration()
                    if (isSpeechConfirmationEnabled && result.message.isNotBlank()) {
                        ttsHelper.speak(result.message)
                    }
                }
                is AiVoiceBusinessAgentEngine.ActionResult.Failure -> {
                    errorMessage = result.error
                    sessionState = AgentSessionState.IDLE
                }
            }
        }
    }

    fun startListeningForConfirmation() {
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            sessionState = AgentSessionState.LISTENING_CONFIRMATION
            speechManager.startListening(
                languageCode = "ar-EG",
                onPartial = { text ->
                    if (AiVoiceBusinessAgentEngine.isConfirmationAffirmative(text)) {
                        speechManager.stopListening()
                        executeConfirmedActionNow()
                    } else if (AiVoiceBusinessAgentEngine.isConfirmationNegative(text)) {
                        speechManager.stopListening()
                        cancelPendingAction()
                    }
                },
                onFinal = { text ->
                    if (AiVoiceBusinessAgentEngine.isConfirmationAffirmative(text)) {
                        executeConfirmedActionNow()
                    } else if (AiVoiceBusinessAgentEngine.isConfirmationNegative(text)) {
                        cancelPendingAction()
                    }
                }
            )
        }
    }

    suspend fun processCommandText(text: String) {
        sessionState = AgentSessionState.ANALYZING
        consultationAnswer = null
        detectedNavDestination = null
        errorMessage = null
        try {
            val navTarget = SmartAccountantEngine.detectGlobalDestination(text)
            if (navTarget != null) {
                detectedNavDestination = navTarget
                val label = navTarget.label(appLanguage)
                val msg = if (appLanguage == AppLanguage.ENGLISH) "Ready to open $label" else "جاهز لفتح قسم $label"
                consultationAnswer = msg
                sessionState = AgentSessionState.SUCCESS_RESULT
                if (isSpeechConfirmationEnabled) {
                    ttsHelper.speak(msg)
                }
                return
            }

            val action = AiVoiceBusinessAgentEngine.analyzeUserVoiceCommand(
                context = context,
                rawInput = text,
                defaultContact = defaultContactName
            )
            pendingAction = action

            if (action.type == AiActionType.GENERAL_CONSULTATION) {
                val snapshot = mainViewModel.buildGlobalAccountingSnapshot()
                val answer = SmartAccountantEngine.askGlobalAccountant(text, snapshot)
                consultationAnswer = answer
                sessionState = AgentSessionState.SUCCESS_RESULT
                if (isSpeechConfirmationEnabled && answer.isNotBlank()) {
                    ttsHelper.speak(answer.take(220))
                }
            } else if (action.requiresConfirmation && action.confirmationQuestionSpeech.isNotBlank()) {
                sessionState = AgentSessionState.CONFIRMING_ACTION
                if (isSpeechConfirmationEnabled) {
                    ttsHelper.speak(action.confirmationQuestionSpeech) {
                        if (isAutoListenConfirmationEnabled && sessionState == AgentSessionState.CONFIRMING_ACTION) {
                            coroutineScope.launch {
                                delay(280)
                                startListeningForConfirmation()
                            }
                        }
                    }
                }
            } else {
                executeConfirmedActionNow()
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "تعذر التنفيذ"
            sessionState = AgentSessionState.IDLE
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            triggerVibration()
            sessionState = AgentSessionState.LISTENING_COMMAND
            speechManager.startListening(
                languageCode = "ar-EG",
                onPartial = { text ->
                    val normalized = SmartVoiceAccountingParser.normalizeSpokenHashtags(text)
                    spokenInput = normalized
                    manualInput = normalized
                },
                onFinal = { text ->
                    val normalized = SmartVoiceAccountingParser.normalizeSpokenHashtags(text)
                    spokenInput = normalized
                    manualInput = normalized
                    if (normalized.isNotBlank()) {
                        coroutineScope.launch { processCommandText(normalized) }
                    }
                }
            )
        } else {
            Toast.makeText(context, "يرجى تفعيل الميكروفون", Toast.LENGTH_SHORT).show()
        }
    }

    fun startListeningForCommand() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            triggerVibration()
            spokenInput = ""
            errorMessage = null
            consultationAnswer = null
            sessionState = AgentSessionState.LISTENING_COMMAND
            speechManager.startListening(
                languageCode = "ar-EG",
                onPartial = { text ->
                    val normalized = SmartVoiceAccountingParser.normalizeSpokenHashtags(text)
                    spokenInput = normalized
                    manualInput = normalized
                },
                onFinal = { text ->
                    val normalized = SmartVoiceAccountingParser.normalizeSpokenHashtags(text)
                    spokenInput = normalized
                    manualInput = normalized
                    if (normalized.isNotBlank()) {
                        coroutineScope.launch { processCommandText(normalized) }
                    }
                }
            )
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
            speechManager.destroy()
        }
    }

    val waveTransition = rememberInfiniteTransition(label = "agent_wave_transition")
    val wavePhase by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "agent_wave_phase"
    )
    val pulseScale by waveTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(580, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "agent_pulse_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
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
                    .testTag("voice_business_agent_sheet"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 14.dp,
                border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Minimal Icon-First Header (no descriptive paragraphs)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
                                    .background(Brush.linearGradient(listOf(OozoxTeal, GoldPrimary))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Text(
                                text = defaultContactName.ifBlank { "المساعد الصوتي" },
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OozoxCircleIconButton(
                                onClick = {
                                    val newVal = !isSpeechConfirmationEnabled
                                    isSpeechConfirmationEnabled = newVal
                                    AiVoiceAgentPreferences.setSpeechConfirmationEnabled(context, newVal)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                imageVector = if (isSpeechConfirmationEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = "الصوت",
                                tint = if (isSpeechConfirmationEnabled) OozoxTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                containerColor = if (isSpeechConfirmationEnabled) OozoxTeal.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                            )

                            OozoxCircleIconButton(
                                onClick = onDismiss,
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // 2. Top Command Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualInput,
                            onValueChange = { manualInput = it },
                            placeholder = {
                                Text("#دفعة 500 ...", fontSize = 13.sp)
                            },
                            trailingIcon = {
                                if (manualInput.isNotBlank()) {
                                    IconButton(onClick = { manualInput = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(17.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                focusedBorderColor = OozoxTeal
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_voice_manual_text_input")
                        )

                        IconButton(
                            onClick = {
                                val text = manualInput.trim()
                                if (text.isNotBlank()) {
                                    spokenInput = text
                                    coroutineScope.launch { processCommandText(text) }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(OozoxTeal)
                                .testTag("send_manual_ai_command_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "تنفيذ",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 3. Hashtag Pills Strip
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            FilterChip(
                                selected = showCustomTagField,
                                onClick = { showCustomTagField = !showCustomTagField },
                                label = { Text("+ #", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                        items(quickTags, key = { it }) { tag ->
                            val isApplied = manualInput.contains(tag, ignoreCase = true)
                            FilterChip(
                                selected = isApplied,
                                onClick = { applyHashtagToCommand(tag) },
                                label = { Text(tag, fontSize = 11.5.sp, fontWeight = FontWeight.Medium) }
                            )
                        }
                    }

                    AnimatedVisibility(visible = showCustomTagField) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customTagInput,
                                onValueChange = { customTagInput = it },
                                placeholder = { Text("#...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (customTagInput.isNotBlank()) {
                                        applyHashtagToCommand(customTagInput)
                                        customTagInput = ""
                                        showCustomTagField = false
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "إدراج", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // 4. Centerpiece Mic Button + Live Audio Waveform
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(58.dp)
                                .scale(if (isSpeechListening) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(if (isSpeechListening) Color(0xFFEF4444) else OozoxTeal)
                                .clickable {
                                    if (isSpeechListening) speechManager.stopListening() else startListeningForCommand()
                                }
                                .testTag("voice_agent_mic_button")
                        ) {
                            Icon(
                                imageVector = if (isSpeechListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "تسجيل صوتي",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activeRms = if (isSpeechListening) rmsLevel.coerceIn(0.24f, 1.0f) else 0.14f
                            repeat(24) { i ->
                                val wave = if (isSpeechListening) {
                                    (kotlin.math.sin((wavePhase + i * 0.45f).toDouble()).toFloat() + 1f) * 0.5f
                                } else {
                                    ((i * 5) % 8) / 14f
                                }
                                val barHeight = (5f + (activeRms * 27f * (0.3f + 0.7f * wave))).dp
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isSpeechListening) OozoxTeal else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                                )
                            }
                        }

                        if (sessionState == AgentSessionState.ANALYZING || sessionState == AgentSessionState.EXECUTING) {
                            CircularProgressIndicator(
                                color = OozoxTeal,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage.orEmpty(),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 5. Compact Contextual Result / Confirmation Strip (zero bloat)
                    AnimatedVisibility(visible = !consultationAnswer.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = adaptiveGoldPrimary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_agent_consultation_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = consultationAnswer.orEmpty(),
                                    fontSize = 12.5.sp,
                                    lineHeight = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .heightIn(max = 120.dp)
                                        .verticalScroll(rememberScrollState())
                                )
                                detectedNavDestination?.let { dest ->
                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onNavigateToDestination(dest)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("voice_agent_open_dest_btn")
                                    ) {
                                        Text("${acctStr("open_section_action")}: ${dest.label(appLanguage)}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = pendingAction != null && (sessionState == AgentSessionState.CONFIRMING_ACTION || sessionState == AgentSessionState.LISTENING_CONFIRMATION)
                    ) {
                        val action = pendingAction
                        if (action != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(action.type.badgeColorHex).copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = action.type.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(action.type.badgeColorHex),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = action.targetContactName,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (action.amount > 0.0) {
                                        Text(
                                            text = "${String.format(Locale.US, "%,.0f", action.amount)} ج.م",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (action.type == AiActionType.DEDUCT_BALANCE) Color(0xFFEF4444) else Color(0xFF10B981)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            cancelPendingAction()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Text("إلغاء", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            executeConfirmedActionNow()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("confirm_action_button")
                                    ) {
                                        Text("حفظ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = lastActionResult != null && sessionState == AgentSessionState.SUCCESS_RESULT && consultationAnswer.isNullOrBlank()
                    ) {
                        val res = lastActionResult
                        if (res != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        lastActionResult = null
                                        manualInput = ""
                                        sessionState = AgentSessionState.IDLE
                                        startListeningForCommand()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("جديد", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onNavigateToChat(res.chatId)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text("فتح المحادثة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
