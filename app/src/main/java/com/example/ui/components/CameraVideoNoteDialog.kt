package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.VideoView
import androidx.camera.core.CameraSelector
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StatusRed
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.ui.theme.adaptiveOnPrimary
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

private enum class VideoNoteStage {
    RECORDING,
    REVIEW
}

/**
 * Instant Front-First CameraX Video Recorder Overlay.
 * Opens directly in the Front Camera while recording continuously.
 * Supports switching Front <-> Back camera seamlessly mid-recording via asPersistentRecording().
 * After stopping, allows playback, typing a caption, and sending or cancelling.
 */
@Composable
fun CameraVideoNoteDialog(
    onDismiss: () -> Unit,
    onSendVideo: (videoUri: String, caption: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var stage by remember { mutableStateOf(VideoNoteStage.RECORDING) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isRecordingStarted by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var captionText by remember { mutableStateOf("") }
    var cameraError by remember { mutableStateOf<String?>(null) }

    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var videoCaptureRef by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var isReviewPlaying by remember { mutableStateOf(false) }
    var reviewVideoViewRef by remember { mutableStateOf<VideoView?>(null) }

    val outputFile = remember {
        val dir = File(context.filesDir, "chat_videos").apply { mkdirs() }
        File(dir, "VID_${System.currentTimeMillis()}.mp4")
    }

    val hasMicPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun cleanupAndDismiss() {
        try {
            activeRecording?.stop()
        } catch (_: Exception) {}
        try {
            cameraProviderRef?.unbindAll()
        } catch (_: Exception) {}
        try {
            if (outputFile.exists()) outputFile.delete()
        } catch (_: Exception) {}
        onDismiss()
    }

    fun finishRecordingToReview() {
        val rec = activeRecording
        if (rec != null) {
            try {
                rec.stop()
            } catch (_: Exception) {
                if (outputFile.exists() && outputFile.length() > 0L) {
                    recordedFile = outputFile
                    stage = VideoNoteStage.REVIEW
                } else {
                    cleanupAndDismiss()
                }
            }
        } else if (outputFile.exists() && outputFile.length() > 0L) {
            recordedFile = outputFile
            stage = VideoNoteStage.REVIEW
        } else {
            cleanupAndDismiss()
        }
    }

    // Bind / Rebind camera use cases when switching Front <-> Back while keeping persistent recording alive
    LaunchedEffect(stage, isFrontCamera, previewViewRef) {
        if (stage != VideoNoteStage.RECORDING) return@LaunchedEffect
        val previewView = previewViewRef ?: return@LaunchedEffect

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProviderRef = provider

                val selector = if (isFrontCamera) {
                    if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                } else {
                    if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }
                }

                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }

                val videoCapture = videoCaptureRef ?: run {
                    val qualitySelector = QualitySelector.fromOrderedList(
                        listOf(Quality.HD, Quality.SD),
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                    )
                    val recorder = Recorder.Builder()
                        .setQualitySelector(qualitySelector)
                        .build()
                    VideoCapture.Builder(recorder)
                        .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
                        .build()
                        .also { videoCaptureRef = it }
                }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    videoCapture
                )

                // Auto-start persistent recording on initial bind so switching camera later keeps recording smoothly
                if (activeRecording == null && !isRecordingStarted) {
                    val fileOptions = FileOutputOptions.Builder(outputFile).build()
                    var pending = videoCapture.output
                        .prepareRecording(context, fileOptions)
                    if (hasMicPermission) {
                        @Suppress("MissingPermission")
                        pending = pending.withAudioEnabled()
                    }
                    pending = pending.asPersistentRecording()

                    val mainExecutor = ContextCompat.getMainExecutor(context)
                    activeRecording = pending.start(mainExecutor) { event ->
                        when (event) {
                            is VideoRecordEvent.Start -> {
                                isRecordingStarted = true
                            }
                            is VideoRecordEvent.Finalize -> {
                                activeRecording = null
                                isRecordingStarted = false
                                if (!event.hasError() || (outputFile.exists() && outputFile.length() > 1024L)) {
                                    recordedFile = outputFile
                                    stage = VideoNoteStage.REVIEW
                                } else {
                                    cameraError = "تعذر حفظ الفيديو"
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                cameraError = "الكاميرا غير متاحة"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Recording duration timer (30s cap)
    LaunchedEffect(stage, isRecordingStarted) {
        if (stage == VideoNoteStage.RECORDING && isRecordingStarted) {
            while (stage == VideoNoteStage.RECORDING && elapsedSeconds < 30) {
                delay(1000L)
                elapsedSeconds += 1
            }
            if (stage == VideoNoteStage.RECORDING && elapsedSeconds >= 30) {
                finishRecordingToReview()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                activeRecording?.stop()
            } catch (_: Exception) {}
            try {
                cameraProviderRef?.unbindAll()
            } catch (_: Exception) {}
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "rec_dot_pulse")
    val dotAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_dot_alpha"
    )

    Dialog(
        onDismissRequest = { cleanupAndDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp)
                .testTag("camera_video_note_dialog")
        ) {
            if (stage == VideoNoteStage.RECORDING) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top minimalist timer badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .alpha(dotAlpha)
                                    .clip(CircleShape)
                                    .background(StatusRed)
                            )
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60),
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Live CameraX Preview Card
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(2.dp, adaptiveGoldPrimary.copy(alpha = 0.7f)),
                        color = Color(0xFF111827),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AndroidView(
                                factory = { ctx ->
                                    PreviewView(ctx).apply {
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    }.also { previewViewRef = it }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            if (cameraError != null) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.Black.copy(alpha = 0.75f),
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    Text(
                                        text = cameraError.orEmpty(),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Recording Controls: Cancel | Stop & Review | Flip Camera (while recording continues)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { cleanupAndDismiss() },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .testTag("video_note_cancel_rec_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إلغاء",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        val stopBtnColor by animateColorAsState(
                            targetValue = if (isRecordingStarted) StatusRed else GoldPrimary,
                            label = "stop_btn_color"
                        )
                        IconButton(
                            onClick = { finishRecordingToReview() },
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(stopBtnColor)
                                .testTag("video_note_stop_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "إنهاء التسجيل",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = { isFrontCamera = !isFrontCamera },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .testTag("video_note_flip_camera_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "تبديل الكاميرا",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            } else {
                // REVIEW STAGE: Replay video + type optional caption ("شرح...") + Send or Cancel
                val videoFile = recordedFile ?: outputFile
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.2.dp, adaptiveGoldPrimary.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setVideoURI(Uri.fromFile(videoFile))
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                            isReviewPlaying = true
                                        }
                                        setOnErrorListener { _, _, _ ->
                                            isReviewPlaying = false
                                            true
                                        }
                                    }.also { reviewVideoViewRef = it }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                                    .clip(RoundedCornerShape(24.dp))
                            )

                            IconButton(
                                onClick = {
                                    val vv = reviewVideoViewRef
                                    if (vv != null) {
                                        if (vv.isPlaying) {
                                            vv.pause()
                                            isReviewPlaying = false
                                        } else {
                                            vv.start()
                                            isReviewPlaying = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = if (isReviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isReviewPlaying) "إيقاف مؤقت" else "تشغيل",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = captionText,
                        onValueChange = { captionText = it },
                        placeholder = { Text("شرح...", fontSize = 13.sp) },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = adaptiveGoldPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_note_caption_input")
                    )

                    // Equal-size, symbol-free Cancel & Send buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { cleanupAndDismiss() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("video_note_cancel_btn")
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                val finalUri = Uri.fromFile(videoFile).toString()
                                try {
                                    cameraProviderRef?.unbindAll()
                                } catch (_: Exception) {}
                                onSendVideo(finalUri, captionText.trim())
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("video_note_send_btn")
                        ) {
                            Text("إرسال", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
