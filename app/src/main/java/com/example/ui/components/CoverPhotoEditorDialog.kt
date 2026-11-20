package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.ui.theme.adaptiveOnPrimary
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ImageFrameAspectMode(
    val ratio: Float,
    val outWidth: Int,
    val outHeight: Int,
    val isCircleGuide: Boolean
) {
    BANNER(16f / 7f, 1280, 560, false),
    SQUARE_AVATAR(1f, 960, 960, true),
    PRODUCT_PHOTO(4f / 3f, 1280, 960, false)
}

/**
 * Interactive non-destructive Photo Editor:
 * - Shows 100% of the image uncropped at 1.0x via ContentScale.Fit (never auto-crops!).
 * - Supports pan, zoom, and 90-degree rotation across any image format.
 */
@Composable
fun CoverPhotoEditorDialog(
    imageUri: Uri,
    onSaveCroppedCover: (savedFilePath: String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "تعديل الصورة",
    frameMode: ImageFrameAspectMode = ImageFrameAspectMode.BANNER,
    useOverlayBox: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var viewportSize by remember { mutableStateOf(IntSize(900, 420)) }
    var isSaving by remember { mutableStateOf(false) }

    val editorContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 14.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with Rotate + Reset + Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = adaptiveGoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    rotationDegrees = (rotationDegrees + 90f) % 360f
                                },
                                enabled = !isSaving,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RotateRight,
                                    contentDescription = "تدوير الصورة",
                                    tint = adaptiveGoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    scale = 1.0f
                                    offsetX = 0f
                                    offsetY = 0f
                                    rotationDegrees = 0f
                                },
                                enabled = !isSaving,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "إعادة ضبط",
                                    tint = adaptiveGoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                enabled = !isSaving,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Uncropped Full-Fit Preview Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(frameMode.ratio)
                            .heightIn(max = 290.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0E1117))
                            .border(1.2.dp, adaptiveGoldPrimary.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            .clipToBounds()
                            .onSizeChanged { viewportSize = it }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val nextScale = (scale * zoom).coerceIn(0.65f, 4.5f)
                                    scale = nextScale
                                    val maxPanX = viewportSize.width * max(0.35f, nextScale * 0.5f)
                                    val maxPanY = viewportSize.height * max(0.35f, nextScale * 0.5f)
                                    offsetX = (offsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                                    offsetY = (offsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    rotationZ = rotationDegrees,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                        )

                        if (frameMode.isCircleGuide) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.88f)
                                    .aspectRatio(1f)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                            )
                        } else {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Spacer(modifier = Modifier.weight(1f).fillMaxHeight().border(0.3.dp, Color.White.copy(alpha = 0.14f)))
                                Spacer(modifier = Modifier.weight(1f).fillMaxHeight().border(0.3.dp, Color.White.copy(alpha = 0.14f)))
                                Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            color = Color.Black.copy(alpha = 0.62f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${(scale * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Compact Zoom Slider Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { scale = (scale - 0.15f).coerceAtLeast(0.65f) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "تصغير", tint = adaptiveGoldPrimary)
                        }

                        Slider(
                            value = scale,
                            onValueChange = { scale = it },
                            valueRange = 0.65f..4.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = adaptiveGoldPrimary,
                                activeTrackColor = adaptiveGoldPrimary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = { scale = (scale + 0.15f).coerceAtMost(4.0f) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "تكبير", tint = adaptiveGoldPrimary)
                        }
                    }

                    // Action Buttons
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
                                coroutineScope.launch {
                                    val savedPath = saveTransformedFrameImage(
                                        context = context,
                                        uri = imageUri,
                                        scale = scale,
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        rotationDegrees = rotationDegrees,
                                        viewportWidth = viewportSize.width,
                                        viewportHeight = viewportSize.height,
                                        frameMode = frameMode
                                    )
                                    isSaving = false
                                    onSaveCroppedCover(savedPath ?: imageUri.toString())
                                }
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = adaptiveOnPrimary,
                                    strokeWidth = 2.dp
                                )
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
        editorContent()
    } else {
        Dialog(
            onDismissRequest = { if (!isSaving) onDismiss() },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            editorContent()
        }
    }
}

private suspend fun saveTransformedFrameImage(
    context: Context,
    uri: Uri,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    rotationDegrees: Float,
    viewportWidth: Int,
    viewportHeight: Int,
    frameMode: ImageFrameAspectMode
): String? = withContext(Dispatchers.IO) {
    try {
        val decodedBitmap = FileUtils.decodeBitmapAnyFormat(context, uri, maxDimensionPx = 1800)
            ?: return@withContext FileUtils.copyMediaUriToInternalStorage(context, uri, isVideo = false, targetFolder = "covers")

        val originalBitmap = if (abs(rotationDegrees % 360f) > 0.1f) {
            val rotMatrix = Matrix().apply { postRotate(rotationDegrees) }
            Bitmap.createBitmap(
                decodedBitmap,
                0,
                0,
                decodedBitmap.width,
                decodedBitmap.height,
                rotMatrix,
                true
            ).also {
                if (it != decodedBitmap) decodedBitmap.recycle()
            }
        } else {
            decodedBitmap
        }

        val coversDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
        val outputFile = File(coversDir, "framed_${System.currentTimeMillis()}.jpg")

        // If user kept unzoomed unpanned 1.0x on a product photo, preserve natural full aspect ratio cleanly!
        val isUntouchedScaleAndPan = abs(scale - 1.0f) < 0.04f && abs(offsetX) < 4f && abs(offsetY) < 4f
        if (isUntouchedScaleAndPan && frameMode == ImageFrameAspectMode.PRODUCT_PHOTO) {
            val scaleCap = min(1f, 1600f / max(originalBitmap.width, originalBitmap.height).toFloat())
            val exportBitmap = if (scaleCap < 0.99f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width * scaleCap).roundToInt().coerceAtLeast(1),
                    (originalBitmap.height * scaleCap).roundToInt().coerceAtLeast(1),
                    true
                )
            } else {
                originalBitmap
            }
            FileOutputStream(outputFile).use { out ->
                exportBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            if (exportBitmap != originalBitmap) exportBitmap.recycle()
            originalBitmap.recycle()
            return@withContext Uri.fromFile(outputFile).toString()
        }

        val targetWidth = frameMode.outWidth
        val targetHeight = frameMode.outHeight
        val outputBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        canvas.drawColor(android.graphics.Color.rgb(14, 17, 23))

        val fitScale = min(
            targetWidth.toFloat() / originalBitmap.width.toFloat(),
            targetHeight.toFloat() / originalBitmap.height.toFloat()
        )
        val totalScale = fitScale * scale
        val scaledWidth = originalBitmap.width * totalScale
        val scaledHeight = originalBitmap.height * totalScale

        val vpW = viewportWidth.coerceAtLeast(1).toFloat()
        val vpH = viewportHeight.coerceAtLeast(1).toFloat()
        val mappedPanX = offsetX * (targetWidth.toFloat() / vpW)
        val mappedPanY = offsetY * (targetHeight.toFloat() / vpH)

        val drawLeft = (targetWidth - scaledWidth) / 2f + mappedPanX
        val drawTop = (targetHeight - scaledHeight) / 2f + mappedPanY

        val drawMatrix = Matrix().apply {
            postScale(totalScale, totalScale)
            postTranslate(drawLeft, drawTop)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(originalBitmap, drawMatrix, paint)

        FileOutputStream(outputFile).use { out ->
            outputBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        outputBitmap.recycle()
        originalBitmap.recycle()

        Uri.fromFile(outputFile).toString()
    } catch (e: Exception) {
        android.util.Log.e("CoverPhotoEditor", "Error processing framed photo", e)
        null
    }
}
