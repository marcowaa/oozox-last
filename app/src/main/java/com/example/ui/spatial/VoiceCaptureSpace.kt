package com.example.ui.spatial

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.AudioRecorderManager
import com.example.util.SpeechToTextManager
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VoiceCaptureSpace(
    onCancel: () -> Unit,
    onSendVoiceNote: (durationSeconds: Int, audioPath: String, transcript: String) -> Unit,
    onSendAsText: ((text: String) -> Unit)? = null,
    onInsertIntoInput: ((text: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isRecording by remember { mutableStateOf(true) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var currentVolumeLevel by remember { mutableFloatStateOf(0.35f) }
    var liveTranscript by remember { mutableStateOf("") }
    var speechError by remember { mutableStateOf<String?>(null) }

    val speechManager = remember { SpeechToTextManager(context) }

    // Start Audio Recording & Speech Recognition on Mount
    DisposableEffect(Unit) {
        val file = AudioRecorderManager.startRecording(context)
        recordedFile = file

        // Trigger light haptic feedback
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(50L)
            }
        } catch (_: Exception) {}

        speechManager.startListening(
            languageCode = "ar-EG",
            onPartial = { text ->
                if (text.isNotBlank()) {
                    liveTranscript = com.example.util.SmartVoiceAccountingParser.normalizeSpokenHashtags(text)
                }
            },
            onFinal = { text ->
                if (text.isNotBlank()) {
                    liveTranscript = com.example.util.SmartVoiceAccountingParser.normalizeSpokenHashtags(text)
                }
            }
        )

        onDispose {
            speechManager.destroy()
            if (isRecording) {
                AudioRecorderManager.cancelRecording()
            }
        }
    }

    // Live timer
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000L)
            elapsedSeconds += 1
        }
    }

    // Live microphone amplitude polling
    LaunchedEffect(isRecording) {
        while (isRecording) {
            val maxAmp = AudioRecorderManager.getMaxAmplitude()
            val normalized = (maxAmp / 25000f).coerceIn(0.18f, 1.0f)
            currentVolumeLevel = normalized
            delay(70L)
        }
    }

    val displayTranscript = remember(liveTranscript, elapsedSeconds) {
        if (liveTranscript.isNotBlank()) {
            liveTranscript
        } else {
            when {
                elapsedSeconds < 2 -> "جاري الاستماع وتحويل الصوت إلى نص مباشرة..."
                elapsedSeconds in 2..4 -> "تحدث بوضوح، جاري كتابة ما تقوله باللغة العربية..."
                elapsedSeconds in 5..8 -> "استمر في الحديث، جاري معالجة الصوت والنص..."
                else -> "تفضل بإنهاء التسجيل أو الإرسال كنص أو تسجيل صوتي"
            }
        }
    }

    val formattedTime = remember(elapsedSeconds) {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Status Bar: Pulsing Mic & Live Timer & Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
                    val alphaPulse by infiniteTransition.animateFloat(
                        initialValue = 0.45f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(550, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "mic_alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StatusOrange.copy(alpha = alphaPulse)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "تسجيل مباشر وتحويل لنص",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "تسجيل وتحويل الصوت لنص",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = StatusGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "مباشر ⚡",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreen,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "المدة: $formattedTime",
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = GoldAccent
                        )
                    }
                }

                TextButton(
                    onClick = {
                        isRecording = false
                        speechManager.cancelListening()
                        AudioRecorderManager.cancelRecording()
                        onCancel()
                    }
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = "إلغاء", tint = StatusOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إلغاء", color = StatusOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Animated Audio Waveform Bars responsive to amplitude
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(24) { index ->
                    val factor = ((index * 17) % 7 + 3) / 10f
                    val animatedHeight = (currentVolumeLevel * factor).coerceIn(0.18f, 1.0f)

                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .fillMaxHeight(animatedHeight)
                            .clip(CircleShape)
                            .background(if (index % 2 == 0) GoldPrimary else GoldLight)
                    )
                }
            }

            // Live Transcript Bar
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "النص المحول تلقائياً:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayTranscript,
                        fontSize = 13.sp,
                        fontWeight = if (liveTranscript.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (liveTranscript.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            // Action Buttons: Send as Text | Send as Audio Voice Note | Insert into composer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Send Transcribed Text directly
                Button(
                    onClick = {
                        isRecording = false
                        speechManager.stopListening()
                        val textToSend = if (liveTranscript.isNotBlank()) liveTranscript else displayTranscript
                        if (onSendAsText != null) {
                            onSendAsText(textToSend)
                        } else {
                            val finalFile = AudioRecorderManager.stopRecording()
                            val filePath = finalFile?.absolutePath ?: recordedFile?.absolutePath ?: ""
                            val duration = elapsedSeconds.coerceAtLeast(1)
                            onSendVoiceNote(duration, filePath, textToSend)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = adaptiveGoldPrimary,
                        contentColor = adaptiveOnPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(40.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = adaptiveOnPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إرسال كنص 📝", color = adaptiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Button 2: Send as Full Voice Note (Audio + Transcript)
                OutlinedButton(
                    onClick = {
                        isRecording = false
                        speechManager.stopListening()
                        val finalFile = AudioRecorderManager.stopRecording()
                        val filePath = finalFile?.absolutePath ?: recordedFile?.absolutePath ?: ""
                        val duration = elapsedSeconds.coerceAtLeast(1)
                        val textToSend = if (liveTranscript.isNotBlank()) liveTranscript else displayTranscript
                        onSendVoiceNote(duration, filePath, textToSend)
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("صوت + نص 🎙️", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                // Button 3: Insert into text field to review/edit
                if (onInsertIntoInput != null) {
                    IconButton(
                        onClick = {
                            isRecording = false
                            speechManager.stopListening()
                            AudioRecorderManager.cancelRecording()
                            val textToInsert = if (liveTranscript.isNotBlank()) liveTranscript else displayTranscript
                            onInsertIntoInput(textToInsert)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "تعديل النص في المربع",
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

