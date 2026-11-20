package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.ui.theme.adaptiveOnPrimary
import com.example.util.FileUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MediaChooserKind {
    PHOTO,
    VIDEO
}

/**
 * Minimal 2-Option Media Source Card: ONLY Camera or Gallery (الكاميرا / الاستديو).
 * Strictly uncluttered and contains zero format-list strings.
 */
@Composable
fun MediaSourceChooserCard(
    kind: MediaChooserKind,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDismiss: () -> Unit,
    useOverlayBox: Boolean = false
) {
    val isPhoto = kind == MediaChooserKind.PHOTO
    val accent = if (isPhoto) Color(0xFF10B981) else GoldPrimary

    val content: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .testTag(if (isPhoto) "photo_source_chooser_card" else "video_source_chooser_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPhoto) "إضافة صورة" else "إضافة فيديو",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Camera ONLY
                        Surface(
                            onClick = onCameraClick,
                            shape = RoundedCornerShape(18.dp),
                            color = accent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 104.dp)
                                .testTag("chooser_camera_btn")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 18.dp, horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPhoto) Icons.Default.PhotoCamera else Icons.Default.Videocam,
                                        contentDescription = "الكاميرا",
                                        tint = accent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "الكاميرا",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Gallery ONLY
                        Surface(
                            onClick = onGalleryClick,
                            shape = RoundedCornerShape(18.dp),
                            color = GoldPrimary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 104.dp)
                                .testTag("chooser_gallery_btn")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 18.dp, horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(GoldPrimary.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPhoto) Icons.Default.PhotoLibrary else Icons.Default.VideoLibrary,
                                        contentDescription = "الاستديو",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "الاستديو",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (useOverlayBox) {
        content()
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            content()
        }
    }
}

/**
 * Clean, human-crafted Video Editor Modal with ExoPlayer live preview,
 * trim range slider, audio mute toggle, optional caption, and internal export.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoEditorDialog(
    videoUri: Uri,
    initialCaption: String = "",
    showCaptionField: Boolean = true,
    targetFolder: String = "chat_videos",
    onConfirmSave: (editedUri: Uri, durationSec: Int, caption: String) -> Unit,
    onDismiss: () -> Unit,
    useOverlayBox: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val rawDurationMs = remember(videoUri) {
        FileUtils.getVideoDurationMs(context, videoUri).coerceAtLeast(1000L)
    }
    var trimRange by remember(rawDurationMs) {
        mutableStateOf(0f..rawDurationMs.toFloat())
    }
    var isMuted by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var caption by remember { mutableStateOf(initialCaption) }
    var isSaving by remember { mutableStateOf(false) }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(exoPlayer, trimRange, isPlaying) {
        while (true) {
            isPlaying = exoPlayer.isPlaying
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            currentPosMs = pos
            val startMs = trimRange.start.toLong()
            val endMs = trimRange.endInclusive.toLong().coerceAtLeast(startMs + 400L)
            if (pos < startMs || pos > endMs) {
                exoPlayer.seekTo(startMs)
            }
            delay(150L)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    val selectedDurationSec = remember(trimRange) {
        (((trimRange.endInclusive - trimRange.start) / 1000f).toInt()).coerceAtLeast(1)
    }

    val content: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(20.dp))
                            Text("تعديل الفيديو", fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { isMuted = !isMuted },
                                enabled = !isSaving,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isMuted) "إلغاء كتم الصوت" else "كتم الصوت",
                                    tint = if (isMuted) Color(0xFFEF4444) else adaptiveGoldPrimary
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                enabled = !isSaving,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }
                    }

                    // Live ExoPlayer Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0A0D14))
                            .clickable {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = FileUtils.formatDurationSeconds(selectedDurationSec),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Trim Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "بداية: ${FileUtils.formatDurationSeconds((trimRange.start / 1000f).toInt())}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "قص المقطع (${FileUtils.formatDurationSeconds(selectedDurationSec)})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = adaptiveGoldPrimary
                            )
                            Text(
                                text = "نهاية: ${FileUtils.formatDurationSeconds((trimRange.endInclusive / 1000f).toInt())}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RangeSlider(
                            value = trimRange,
                            onValueChange = { next ->
                                val minSpan = 800f.coerceAtMost(rawDurationMs.toFloat())
                                if (next.endInclusive - next.start >= minSpan) {
                                    trimRange = next
                                    exoPlayer.seekTo(next.start.toLong())
                                }
                            },
                            valueRange = 0f..rawDurationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = adaptiveGoldPrimary,
                                activeTrackColor = adaptiveGoldPrimary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }

                    if (showCaptionField) {
                        OutlinedTextField(
                            value = caption,
                            onValueChange = { caption = it },
                            label = { Text("تعليق (اختياري)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Save / Cancel Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("إلغاء", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                isSaving = true
                                exoPlayer.pause()
                                scope.launch {
                                    val result = FileUtils.trimOrMuteVideoToInternalStorage(
                                        context = context,
                                        sourceUri = videoUri,
                                        startMs = trimRange.start.toLong(),
                                        endMs = trimRange.endInclusive.toLong(),
                                        muteAudio = isMuted,
                                        targetFolder = targetFolder
                                    )
                                    isSaving = false
                                    val outUri = result?.uri?.let { Uri.parse(it) } ?: videoUri
                                    val outDur = result?.durationSec ?: selectedDurationSec
                                    onConfirmSave(outUri, outDur, caption.trim())
                                }
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = adaptiveOnPrimary, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حفظ", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (useOverlayBox) {
        content()
    } else {
        Dialog(
            onDismissRequest = { if (!isSaving) onDismiss() },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            content()
        }
    }
}
