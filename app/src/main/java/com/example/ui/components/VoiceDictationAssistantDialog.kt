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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.MessageType
import com.example.ui.theme.*
import com.example.util.GoldUtils
import com.example.util.SpeechToTextManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dictation modes supported by the Voice-to-Text assistant.
 */
enum class VoiceDictationMode(val title: String, val icon: String, val subtitle: String) {
    ACCOUNTING(
        title = "إدخال قيد محاسبي صوتي",
        icon = "",
        subtitle = "إملاء القيود بالصوت، المبالغ، الذهب، الهاشتاجات، والمطابقات"
    ),
    INVOICE(
        title = "إملاء تفاصيل فاتورة",
        icon = "",
        subtitle = "إملاء أرقام المبالغ، الذهب، اسم العميل، والبيان المالي"
    ),
    PROJECT_NOTES(
        title = "إملاء ملاحظات مشروع",
        icon = "",
        subtitle = "إملاء التحديثات الميدانية، المهام، والملاحظات الإشرافية"
    ),
    FREE_TEXT(
        title = "إملاء نصي حر",
        icon = "",
        subtitle = "تحويل الكلمات المنطوقة إلى نص عادي في المحادثة"
    )
}

/**
 * VoiceDictationAssistantDialog allows users to quickly dictate invoice details,
 * project notes, or free-form text with real-time speech recognition, live waveform feedback,
 * language switching, and intelligent structured draft generation.
 */
@Composable
fun VoiceDictationAssistantDialog(
    initialMode: VoiceDictationMode = VoiceDictationMode.INVOICE,
    contactName: String = "",
    onDismiss: () -> Unit,
    onInsertText: (formattedText: String) -> Unit,
    onDirectSend: (content: String, messageType: String) -> Unit
) {
    val context = LocalContext.current
    var currentMode by remember { mutableStateOf(initialMode) }
    var selectedLanguage by remember { mutableStateOf("ar-EG") }
    var transcribedText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    val speechManager = remember { SpeechToTextManager(context) }
    val rmsLevel by speechManager.rmsLevel.collectAsStateWithLifecycle()
    val speechError by speechManager.errorState.collectAsStateWithLifecycle()
    val isSpeechListening by speechManager.isListening.collectAsStateWithLifecycle()

    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(40L)
            }
        } catch (_: Exception) {}
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            triggerHapticFeedback()
            isListening = true
            speechManager.startListening(
                languageCode = selectedLanguage,
                onPartial = { text -> if (text.isNotBlank()) transcribedText = text },
                onFinal = { text ->
                    if (text.isNotBlank()) transcribedText = text
                    isListening = false
                }
            )
        } else {
            showPermissionRationale = true
        }
    }

    fun startListeningWithPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            triggerHapticFeedback()
            isListening = true
            speechManager.startListening(
                languageCode = selectedLanguage,
                onPartial = { text -> if (text.isNotBlank()) transcribedText = text },
                onFinal = { text ->
                    if (text.isNotBlank()) transcribedText = text
                    isListening = false
                }
            )
        }
    }

    fun stopListening() {
        triggerHapticFeedback()
        speechManager.stopListening()
        isListening = false
    }

    // Auto-start dictation upon opening
    LaunchedEffect(Unit) {
        startListeningWithPermission()
    }

    DisposableEffect(Unit) {
        onDispose {
            speechManager.destroy()
        }
    }

    // Formatter logic according to mode
    fun formatOutput(rawText: String, mode: VoiceDictationMode): String {
        if (rawText.isBlank()) return ""
        val nowFormatted = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(Date())
        return when (mode) {
            VoiceDictationMode.ACCOUNTING -> {
                val parsed = com.example.util.SmartVoiceAccountingParser.parseVoiceAccountingText(rawText, contactName)
                parsed.formattedHashtagMessage
            }
            VoiceDictationMode.INVOICE -> {
                buildString {
                    appendLine("تفاصيل فاتورة معتمدة")
                    if (contactName.isNotBlank()) {
                        appendLine("• العميل / الطرف: $contactName")
                    }
                    appendLine("• التاريخ: $nowFormatted")
                    appendLine("• تفاصيل الفاتورة المُملات: $rawText")
                    appendLine("• الحالة: مسودة جاهزة للمطابقة والاعتماد ️")
                }.trim()
            }
            VoiceDictationMode.PROJECT_NOTES -> {
                buildString {
                    appendLine("ملاحظات ومتابعة مشروع")
                    if (contactName.isNotBlank()) {
                        appendLine("• المشروع / الجهة: $contactName")
                    }
                    appendLine("• توقيت التسجيل: $nowFormatted")
                    appendLine("• الملاحظة الميدانية: $rawText")
                    appendLine("• التوثيق: إملاء صوتي موثق ️")
                }.trim()
            }
            VoiceDictationMode.FREE_TEXT -> rawText.trim()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "dict_dialog_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse"
    )

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
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = 640.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(
                    1.5.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            GoldPrimary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
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
                                    .background(GoldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = "الإملاء الصوتي الذكي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = {
                                speechManager.destroy()
                                onDismiss()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
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

                // Mode Selection Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VoiceDictationMode.values().forEach { mode ->
                        val isSelected = currentMode == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    currentMode = mode
                                    triggerHapticFeedback()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(mode.icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (mode) {
                                        VoiceDictationMode.ACCOUNTING -> "قيد محاسبي"
                                        VoiceDictationMode.INVOICE -> "تفاصيل فاتورة"
                                        VoiceDictationMode.PROJECT_NOTES -> "ملاحظات مشروع"
                                        VoiceDictationMode.FREE_TEXT -> "إملاء حر"
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) adaptiveOnPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Center Microphone & Live Waveform Animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Mic Button
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .scale(if (isListening || isSpeechListening) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    if (isListening || isSpeechListening) {
                                        Brush.radialGradient(listOf(StatusOrange, GoldPrimary))
                                    } else {
                                        Brush.radialGradient(listOf(GoldDark, MaterialTheme.colorScheme.surfaceVariant))
                                    }
                                )
                                .clickable {
                                    if (isListening || isSpeechListening) {
                                        stopListening()
                                    } else {
                                        startListeningWithPermission()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening || isSpeechListening) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = if (isListening) "إيقاف الاستماع" else "بدء الاستماع",
                                tint = if (isListening || isSpeechListening) Color.White else GoldLight,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Text(
                            text = if (isListening || isSpeechListening) {
                                "جاري الاستماع لصوتك الآن... تحدث بوضوح ️"
                            } else {
                                "اضغط على الميكروفون للبدء بالتسجيل"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isListening || isSpeechListening) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Live Waveform Visualizer
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(28.dp)
                        ) {
                            val activeRms = if (isListening || isSpeechListening) rmsLevel.coerceAtLeast(0.2f) else 0.1f
                            repeat(18) { i ->
                                val multiplier = ((i * 7) % 11 + 3) / 14f
                                val barHeight = (6 + (activeRms * 20 * multiplier)).dp
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (isListening || isSpeechListening) GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }

                        // Language Toggle Bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("اللغة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            listOf(
                                "ar-EG" to "مصري / عام",
                                "ar-SA" to "فصحى / خليجي",
                                "en-US" to "English"
                            ).forEach { (langCode, label) ->
                                val isLang = selectedLanguage == langCode
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isLang) GoldPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                    border = BorderStroke(1.dp, if (isLang) GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable {
                                        selectedLanguage = langCode
                                        if (isListening || isSpeechListening) {
                                            stopListening()
                                            startListeningWithPermission()
                                        }
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isLang) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isLang) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Error message if any
                if (speechError != null && !isListening) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(speechError ?: "", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Transcribed Text Display & Live Editing Field
                OutlinedTextField(
                    value = transcribedText,
                    onValueChange = { transcribedText = it },
                    label = {
                        Text(
                            when (currentMode) {
                                VoiceDictationMode.ACCOUNTING -> "تفاصيل القيد المحاسبي بالصوت (قابلة للتعديل)"
                                VoiceDictationMode.INVOICE -> "تفاصيل الفاتورة المُملاة (قابلة للتعديل)"
                                VoiceDictationMode.PROJECT_NOTES -> "ملاحظات المشروع المُملاة (قابلة للتعديل)"
                                VoiceDictationMode.FREE_TEXT -> "النص المُملی (قابل للتعديل)"
                            },
                            fontSize = 11.5.sp
                        )
                    },
                    placeholder = {
                        Text(
                            when (currentMode) {
                                VoiceDictationMode.ACCOUNTING -> "مثال: اكتب أو انطق: دفعة خمسين ألف جنيه لحساب الذهب عيار 21 للمهندس خالد..."
                                VoiceDictationMode.INVOICE -> "مثال: فاتورة توريد 50 جرام ذهب عيار 21 بمبلغ 175,000 جنيه للعميل أحمد..."
                                VoiceDictationMode.PROJECT_NOTES -> "مثال: متابعة مشروع التشطيبات تم الانتهاء من الدهانات وجاري تركيب الإضاءة..."
                                VoiceDictationMode.FREE_TEXT -> "تحدث لبدء تحويل الصوت إلى نص..."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        if (transcribedText.isNotBlank()) {
                            IconButton(onClick = { transcribedText = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        focusedLabelColor = GoldPrimary
                    ),
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Pinned Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Insert into textInput
                    OutlinedButton(
                        onClick = {
                            val formatted = formatOutput(transcribedText, currentMode)
                            if (formatted.isNotBlank()) {
                                speechManager.destroy()
                                onInsertText(formatted)
                                onDismiss()
                            } else {
                                Toast.makeText(context, "يرجى التحدث أولاً لتسجيل النص", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إدراج بالرسالة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Direct Send
                    Button(
                        onClick = {
                            val formatted = formatOutput(transcribedText, currentMode)
                            if (formatted.isNotBlank()) {
                                speechManager.destroy()
                                val msgType = when (currentMode) {
                                    VoiceDictationMode.INVOICE -> MessageType.INVOICE.name
                                    else -> MessageType.TEXT.name
                                }
                                onDirectSend(formatted, msgType)
                                onDismiss()
                            } else {
                                Toast.makeText(context, "يرجى التحدث أولاً لتسجيل النص", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = adaptiveGoldPrimary,
                            contentColor = adaptiveOnPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = adaptiveOnPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال فوري", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
    }

    // Permission Rationale Dialog if user denied
    if (showPermissionRationale) {
        Dialog(
            onDismissRequest = { showPermissionRationale = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(26.dp))
                        }

                        Text(
                            text = "مطلوب إذن الميكروفون",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "يرجى منح إذن الميكروفون للاستماع.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showPermissionRationale = false },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Text("إلغاء")
                            }

                            Button(
                                onClick = {
                                    showPermissionRationale = false
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = adaptiveGoldPrimary,
                                    contentColor = adaptiveOnPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Text("منح الإذن", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
