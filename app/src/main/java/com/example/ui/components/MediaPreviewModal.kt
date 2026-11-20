package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class MediaPreviewItem(
    val uri: String,
    val caption: String = "",
    val senderName: String = "",
    val timestamp: Long = 0L,
    val isVideo: Boolean = false
)

@OptIn(UnstableApi::class)
@Composable
fun MediaPreviewModal(
    item: MediaPreviewItem,
    onDismiss: () -> Unit,
    onForward: (() -> Unit)? = null,
    useOverlayBox: Boolean = false
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isControlsVisible by remember { mutableStateOf(true) }

    val isYouTube = remember(item.uri) {
        item.uri.contains("youtube.com") || item.uri.contains("youtu.be")
    }

    val isVideo = remember(item.uri, item.isVideo) {
        isYouTube || item.isVideo || com.example.util.FileUtils.isVideoUriString(item.uri)
    }

    var isVideoPlaying by remember { mutableStateOf(false) }
    var isVideoBuffering by remember { mutableStateOf(false) }
    var videoError by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember(item.uri, isVideo, isYouTube) {
        if (isVideo && !isYouTube) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(item.uri)))
                prepare()
                playWhenReady = true
            }
        } else {
            null
        }
    }

    if (exoPlayer != null) {
        LaunchedEffect(isMuted) {
            exoPlayer.volume = if (isMuted) 0f else 1f
        }

        DisposableEffect(exoPlayer) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isVideoPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    isVideoBuffering = playbackState == Player.STATE_BUFFERING
                    if (exoPlayer.duration > 0L) {
                        totalDurationMs = exoPlayer.duration
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        isVideoPlaying = false
                        exoPlayer.seekTo(0L)
                        exoPlayer.pause()
                        currentPosMs = 0L
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    videoError = true
                    isVideoPlaying = false
                    isVideoBuffering = false
                }
            }
            exoPlayer.addListener(listener)
            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }

        LaunchedEffect(exoPlayer, isVideoPlaying) {
            while (true) {
                currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                if (exoPlayer.duration > 0L) {
                    totalDurationMs = exoPlayer.duration
                }
                delay(200L)
            }
        }
    }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            val maxOffsetX = 340f * (scale - 1f)
            val maxOffsetY = 460f * (scale - 1f)
            offset = Offset(
                x = (offset.x + offsetChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                y = (offset.y + offsetChange.y).coerceIn(-maxOffsetY, maxOffsetY)
            )
        } else {
            offset = Offset.Zero
        }
    }

    val animatedScale by animateFloatAsState(targetValue = scale, label = "media_zoom")

    val formattedTime = remember(item.timestamp) {
        if (item.timestamp > 0) {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
        } else ""
    }

    val previewContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
                .pointerInput(isVideo) {
                    detectTapGestures(
                        onTap = {
                            isControlsVisible = !isControlsVisible
                        },
                        onDoubleTap = {
                            if (!isVideo) {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.4f
                                }
                            }
                        }
                    )
                }
        ) {
            // Center Media Element (Zoomable full uncropped fit for images, native Media3 ExoPlayer for videos)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!isVideo) {
                            Modifier
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                                .transformable(state = state)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isYouTube) {
                    val uriString = item.uri
                    val videoId = if (uriString.contains("v=")) {
                        uriString.substringAfter("v=").substringBefore("&").substringBefore("?")
                    } else if (uriString.contains("youtu.be/")) {
                        uriString.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
                    } else if (uriString.contains("embed/")) {
                        uriString.substringAfter("embed/").substringBefore("?").substringBefore("&")
                    } else {
                        "dQw4w9WgXcQ"
                    }
                    val embedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1"

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                webViewClient = WebViewClient()
                                loadUrl(embedUrl)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else if (isVideo && exoPlayer != null) {
                    if (videoError) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "تعذر تشغيل الفيديو",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            update = { view ->
                                if (view.player != exoPlayer) view.player = exoPlayer
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.82f)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        if (isVideoBuffering) {
                            CircularProgressIndicator(
                                color = GoldPrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(42.dp)
                            )
                        } else if (!isVideoPlaying || isControlsVisible) {
                            IconButton(
                                onClick = {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.58f))
                            ) {
                                Icon(
                                    imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isVideoPlaying) "إيقاف" else "تشغيل",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                } else {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.uri)
                            .crossfade(250)
                            .build(),
                        contentDescription = item.caption.ifBlank { "معاينة الصورة" },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.88f),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = GoldPrimary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF141820)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = null,
                                        tint = adaptiveGoldPrimary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = "تعذر تحميل الصورة",
                                        color = OozoxPlatinum,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // Top Header Overlay
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (isVideo) "فيديو" else "صورة",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (item.senderName.isNotBlank() || formattedTime.isNotBlank()) {
                                    Text(
                                        text = listOfNotNull(
                                            item.senderName.takeIf { it.isNotBlank() },
                                            formattedTime.takeIf { it.isNotBlank() }
                                        ).joinToString(" • "),
                                        color = OozoxMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isVideo && exoPlayer != null) {
                                IconButton(
                                    onClick = { isMuted = !isMuted },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = if (isMuted) "تشغيل الصوت" else "كتم الصوت",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (!isVideo && scale > 1.05f) {
                                IconButton(
                                    onClick = {
                                        scale = 1f
                                        offset = Offset.Zero
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(GoldPrimary.copy(alpha = 0.2f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomOutMap,
                                        contentDescription = "إعادة ضبط التكبير",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (onForward != null) {
                                IconButton(
                                    onClick = {
                                        onDismiss()
                                        onForward()
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(GoldPrimary.copy(alpha = 0.25f))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "إعادة توجيه",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Timeline & Caption Overlay
            AnimatedVisibility(
                visible = isControlsVisible && (isVideo || item.caption.isNotBlank() || scale > 1f),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.82f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isVideo && !isYouTube && exoPlayer != null) {
                            val durMs = totalDurationMs.coerceAtLeast(1L)
                            val progress = (currentPosMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f)
                            val curSec = (currentPosMs / 1000L).toInt()
                            val totSec = (durMs / 1000L).toInt()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = String.format("%02d:%02d", curSec / 60, curSec % 60),
                                    color = Color.White,
                                    fontSize = 11.sp
                                )

                                Slider(
                                    value = progress,
                                    onValueChange = { p ->
                                        val seekTarget = (p * durMs).toLong()
                                        currentPosMs = seekTarget
                                        exoPlayer.seekTo(seekTarget)
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = GoldPrimary,
                                        activeTrackColor = GoldPrimary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = String.format("%02d:%02d", totSec / 60, totSec % 60),
                                    color = OozoxMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (item.caption.isNotBlank()) {
                            Text(
                                text = item.caption,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }

                        if (!isVideo && scale > 1f) {
                            Text(
                                text = String.format("%.1fx", scale),
                                color = GoldPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (useOverlayBox) {
        previewContent()
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            previewContent()
        }
    }
}
