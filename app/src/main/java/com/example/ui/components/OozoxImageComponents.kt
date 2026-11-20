package com.example.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.adaptiveGoldDark
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.util.GoldUtils

/**
 * High-performance Avatar Composable with Coil Inexact Downsampling & Memory Caching.
 */
@Composable
fun OozoxAvatarImage(
    model: Any?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isGroup: Boolean = false,
    isTrader: Boolean = false,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = remember(size, density) { with(density) { size.roundToPx() } }

    val isEmoji = remember(model) {
        model is String && model.length in 1..4 && model.any { it.code > 255 || Character.isSurrogate(it) }
    }

    val hasValidImage = remember(model, isEmoji) {
        !isEmoji && model != null && (model !is String || model.isNotBlank())
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val containerBg = when {
        isGroup -> Color(0xFF1E3A8A)
        isTrader -> if (isDark) GoldDark else Color(0xFFFEF3C7)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = when {
        isGroup -> Color(0xFF60A5FA).copy(alpha = 0.5f)
        isTrader -> if (isDark) GoldPrimary.copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerBg)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (hasValidImage) {
            val request = remember(model, sizePx) {
                ImageRequest.Builder(context)
                    .data(model)
                    .size(Size(sizePx, sizePx))
                    .precision(Precision.INEXACT)
                    .scale(Scale.FIT)
                    .crossfade(200)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    .allowRgb565(false)
                    .build()
            }

            SubcomposeAsyncImage(
                model = request,
                contentDescription = contentDescription ?: name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Success -> {
                        SubcomposeAsyncImageContent()
                    }
                    is AsyncImagePainter.State.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = GoldUtils.getInitials(name),
                                fontSize = (size.value * 0.35f).sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isTrader -> if (isDark) GoldLight else Color(0xFF92400E)
                                    isDark -> GoldPrimary
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                    else -> {
                        AvatarFallback(isGroup = isGroup, name = name, isTrader = isTrader, size = size)
                    }
                }
            }
        } else if (isEmoji && model is String) {
            Text(
                text = model,
                fontSize = (size.value * 0.45f).sp
            )
        } else {
            AvatarFallback(isGroup = isGroup, name = name, isTrader = isTrader, size = size)
        }
    }
}

@Composable
private fun AvatarFallback(
    isGroup: Boolean,
    name: String,
    isTrader: Boolean,
    size: Dp
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    if (isGroup) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = "مجموعة",
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f)
        )
    } else {
        Text(
            text = GoldUtils.getInitials(name),
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Bold,
            color = when {
                isTrader -> if (isDark) GoldLight else Color(0xFF92400E)
                isDark -> GoldPrimary
                else -> MaterialTheme.colorScheme.primary
            }
        )
    }
}

/**
 * Thumbnail Composable defaulting to ContentScale.Fit so photos render uncropped
 * while preserving aspect ratio on a clean neutral backdrop.
 */
@Composable
fun OozoxThumbnailImage(
    model: Any?,
    modifier: Modifier = Modifier,
    width: Dp = 80.dp,
    height: Dp = 80.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val widthPx = remember(width, density) { with(density) { width.roundToPx() } }
    val heightPx = remember(height, density) { with(density) { height.roundToPx() } }

    val request = remember(model, widthPx, heightPx, contentScale) {
        ImageRequest.Builder(context)
            .data(model)
            .size(Size(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1)))
            .precision(Precision.INEXACT)
            .scale(if (contentScale == ContentScale.Crop) Scale.FILL else Scale.FIT)
            .crossfade(220)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            .allowRgb565(false)
            .build()
    }

    val modelStr = remember(model) { model?.toString().orEmpty() }
    val isVideoModel = remember(modelStr) {
        com.example.util.FileUtils.isVideoUriString(modelStr)
    }
    val videoFrame by produceState<Bitmap?>(initialValue = null, key1 = modelStr, key2 = isVideoModel) {
        if (!isVideoModel || modelStr.isBlank()) {
            value = null
        } else {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    if (modelStr.startsWith("content://") || modelStr.startsWith("file://")) {
                        retriever.setDataSource(context, Uri.parse(modelStr))
                    } else {
                        retriever.setDataSource(modelStr)
                    }
                    retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (_: Exception) {
                    null
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }
            }
        }
    }

    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    val containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        Color(0xFF0E131D)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    }

    Surface(
        shape = shape,
        color = containerColor,
        border = BorderStroke(0.8.dp, adaptiveGoldDark.copy(alpha = 0.25f)),
        modifier = modifier
            .size(width = width, height = height)
            .then(clickModifier)
    ) {
        if (isVideoModel) {
            if (videoFrame != null) {
                Image(
                    bitmap = videoFrame!!.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF111827)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = contentDescription,
                        tint = adaptiveGoldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        } else {
            SubcomposeAsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Success -> {
                        SubcomposeAsyncImageContent()
                    }
                    is AsyncImagePainter.State.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = GoldPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    is AsyncImagePainter.State.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = "تعذر تحميل الصورة",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = adaptiveGoldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * In-chat & In-store inline ExoPlayer video player.
 * Shows full thumbnail frame while idle, and plays natively right inside the message bubble on tap,
 * with sound toggle and fullscreen expansion to MediaPreviewModal.
 */
@OptIn(UnstableApi::class)
@Composable
fun InlineChatVideoPlayer(
    videoUri: String,
    width: Dp,
    height: Dp,
    shape: Shape = RoundedCornerShape(12.dp),
    onExpandFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaybackActive by remember(videoUri) { mutableStateOf(false) }
    var isPlaying by remember(videoUri) { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    val parsedUri = remember(videoUri) { Uri.parse(videoUri) }

    Surface(
        shape = shape,
        color = Color(0xFF0A0D14),
        border = BorderStroke(0.8.dp, adaptiveGoldPrimary.copy(alpha = 0.3f)),
        modifier = modifier.size(width = width, height = height)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isPlaybackActive) {
                OozoxThumbnailImage(
                    model = videoUri,
                    width = width,
                    height = height,
                    shape = shape,
                    contentScale = ContentScale.Fit,
                    contentDescription = "فيديو مرفق",
                    onClick = { isPlaybackActive = true }
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { isPlaybackActive = true },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.68f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "تشغيل الفيديو داخل المحادثة",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onExpandFullscreen,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "ملء الشاشة",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                val exoPlayer = remember(videoUri) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(parsedUri))
                        repeatMode = Player.REPEAT_MODE_OFF
                        prepare()
                        playWhenReady = true
                    }
                }

                LaunchedEffect(isMuted) {
                    exoPlayer.volume = if (isMuted) 0f else 1f
                }

                DisposableEffect(exoPlayer) {
                    val listener = object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            isPlaying = playing
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0L)
                                exoPlayer.pause()
                                isPlaying = false
                            }
                        }
                    }
                    exoPlayer.addListener(listener)
                    onDispose {
                        exoPlayer.removeListener(listener)
                        exoPlayer.release()
                    }
                }

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
                        .fillMaxSize()
                        .clip(shape)
                )

                // Overlay mini controls
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.58f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "إلغاء الكتم" else "كتم",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                exoPlayer.pause()
                                onExpandFullscreen()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "تكبير الفيديو",
                                tint = GoldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-performance Hero/Banner Image Composable with downsampling to screen dimensions.
 */
@Composable
fun OozoxBannerImage(
    model: Any?,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val heightPx = remember(height, density) { with(density) { height.roundToPx() } }

    val request = remember(model, heightPx, contentScale) {
        ImageRequest.Builder(context)
            .data(model)
            .size(Size(1080, heightPx))
            .precision(Precision.INEXACT)
            .scale(if (contentScale == ContentScale.Crop) Scale.FILL else Scale.FIT)
            .crossfade(300)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            .allowRgb565(false)
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Success -> {
                SubcomposeAsyncImageContent()
            }
            is AsyncImagePainter.State.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GoldDark.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = GoldPrimary,
                        strokeWidth = 2.dp
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GoldDark.copy(alpha = 0.15f))
                )
            }
        }
    }
}
