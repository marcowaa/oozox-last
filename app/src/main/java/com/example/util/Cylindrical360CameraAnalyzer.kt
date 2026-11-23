package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Real-time CameraX ImageAnalysis analyzer for 360° cylindrical product capture.
 *
 * Behavior:
 * 1. Captures keyframes strictly when the product moves/rotates inside the center ROI.
 *    Stationary frames are ignored so angle/frame count never advances without movement.
 * 2. Automatically detects the end of a full 360° rotation via shift-tolerant loop closure
 *    against the starting frame signature (after traversing opposite views) and stitches
 *    captured center slices into an unwrapped cylindrical panoramic image.
 */
class Cylindrical360CameraAnalyzer(
    private val context: Context,
    private val onProgress: (angleDeg: Int, frameCount: Int, isMoving: Boolean) -> Unit,
    private val onCompleted: (frameCount: Int, unwrappedBitmap: Bitmap?, savedFilePath: String?) -> Unit
) : ImageAnalysis.Analyzer {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lock = Any()

    @Volatile
    private var scanningActive: Boolean = false

    @Volatile
    private var completed: Boolean = false

    private var prevDesc: FloatArray? = null
    private var startDesc: FloatArray? = null
    private var lastCapturedDesc: FloatArray? = null
    private var lastCaptureMs: Long = 0L
    private var maxDistanceFromStart: Float = 0f
    private var settleFramesLeft: Int = 2
    private val capturedSlices = mutableListOf<Bitmap>()

    fun startScan() {
        synchronized(lock) {
            clearSlicesLocked()
            prevDesc = null
            startDesc = null
            lastCapturedDesc = null
            lastCaptureMs = 0L
            maxDistanceFromStart = 0f
            settleFramesLeft = 2
            completed = false
            scanningActive = true
        }
        mainHandler.post { onProgress(0, 0, false) }
    }

    fun stopScan() {
        synchronized(lock) {
            scanningActive = false
        }
    }

    fun reset() {
        synchronized(lock) {
            scanningActive = false
            completed = false
            clearSlicesLocked()
            prevDesc = null
            startDesc = null
            lastCapturedDesc = null
            lastCaptureMs = 0L
            maxDistanceFromStart = 0f
            settleFramesLeft = 2
        }
        mainHandler.post { onProgress(0, 0, false) }
    }

    private fun clearSlicesLocked() {
        capturedSlices.forEach { bmp ->
            if (!bmp.isRecycled) bmp.recycle()
        }
        capturedSlices.clear()
    }

    override fun analyze(imageProxy: ImageProxy) {
        try {
            if (!scanningActive || completed) return

            val curr = extractCenterDescriptor(imageProxy) ?: return
            val nowMs = System.currentTimeMillis()

            var emitAngle = 0
            var emitCount = 0
            var emitMoving = false
            var finishNow = false
            var stitchedBitmap: Bitmap? = null
            var stitchedPath: String? = null
            var finalFrameCount = 0

            synchronized(lock) {
                if (!scanningActive || completed) return

                if (settleFramesLeft > 0) {
                    settleFramesLeft--
                    prevDesc = curr
                    return
                }

                val prev = prevDesc
                prevDesc = curr

                val start = startDesc
                val lastCap = lastCapturedDesc

                if (start == null || lastCap == null) {
                    // Lock 0° starting face frame
                    val slice = extractVerticalSliceBitmap(imageProxy)
                    if (slice != null) {
                        capturedSlices.add(slice)
                    }
                    startDesc = curr
                    lastCapturedDesc = curr
                    lastCaptureMs = nowMs
                    emitAngle = 0
                    emitCount = capturedSlices.size
                    emitMoving = true
                } else {
                    val instantMotion = if (prev != null) 1f - cosineSimilarity(curr, prev) else 0f
                    val viewDisplacement = 1f - cosineSimilarity(curr, lastCap)
                    val distFromStart = 1f - cosineSimilarity(curr, start)
                    maxDistanceFromStart = max(maxDistanceFromStart, distFromStart)

                    // Strictly require real product movement + angle change (ignore stationary scene)
                    val isProductMoving = instantMotion >= MOTION_MIN_DELTA || viewDisplacement >= MOTION_SHIFT_HINT
                    emitMoving = isProductMoving

                    val canCaptureAngle = isProductMoving &&
                            viewDisplacement >= KEYFRAME_VIEW_SHIFT &&
                            instantMotion < BLUR_REJECT_DELTA &&
                            (nowMs - lastCaptureMs) >= CAPTURE_COOLDOWN_MS

                    if (canCaptureAngle) {
                        val slice = extractVerticalSliceBitmap(imageProxy)
                        if (slice != null) {
                            capturedSlices.add(slice)
                        }
                        lastCapturedDesc = curr
                        lastCaptureMs = nowMs
                    }

                    // Automatic 360° end-of-rotation detection via shift-tolerant loop closure
                    val traversedOppositeSide = capturedSlices.size >= MIN_FRAMES_FOR_CLOSURE &&
                            maxDistanceFromStart >= MIN_OPPOSITE_DISSIMILARITY
                    val simToStart = if (traversedOppositeSide) {
                        maxShiftedCosineSimilarity(curr, start)
                    } else {
                        0f
                    }

                    val loopClosed = traversedOppositeSide && simToStart >= LOOP_CLOSURE_SIMILARITY
                    val capReached = capturedSlices.size >= MAX_KEYFRAMES

                    if (loopClosed || capReached) {
                        completed = true
                        scanningActive = false
                        finishNow = true
                        emitAngle = 360
                        emitCount = capturedSlices.size
                        finalFrameCount = capturedSlices.size
                        stitchedBitmap = stitchCylindricalSlicesLocked()
                        stitchedPath = stitchedBitmap?.let { saveBitmapToCache(it) }
                    } else {
                        val estimated = ((capturedSlices.size - 1).coerceAtLeast(0).toFloat() / TARGET_LOOP_FRAMES * 330f)
                            .toInt()
                            .coerceIn(0, 345)
                        emitAngle = estimated
                        emitCount = capturedSlices.size
                    }
                }
            }

            if (finishNow) {
                mainHandler.post {
                    onProgress(360, finalFrameCount, false)
                    onCompleted(finalFrameCount, stitchedBitmap, stitchedPath)
                }
            } else {
                mainHandler.post {
                    onProgress(emitAngle, emitCount, emitMoving)
                }
            }
        } catch (_: Exception) {
            // Keep pipeline resilient across camera lifecycle transitions
        } finally {
            imageProxy.close()
        }
    }

    private fun extractCenterDescriptor(imageProxy: ImageProxy): FloatArray? {
        val plane = imageProxy.planes.firstOrNull() ?: return null
        val w = imageProxy.width
        val h = imageProxy.height
        if (w <= 0 || h <= 0) return null

        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        val startX = (w * 0.22f).toInt()
        val endX = (w * 0.78f).toInt().coerceAtLeast(startX + 1)
        val startY = (h * 0.20f).toInt()
        val endY = (h * 0.80f).toInt().coerceAtLeast(startY + 1)

        val raw = FloatArray(GRID_SIZE * GRID_SIZE)
        var sum = 0f

        for (gy in 0 until GRID_SIZE) {
            val py = startY + ((endY - startY) * gy) / GRID_SIZE
            val rowOffset = py * rowStride
            for (gx in 0 until GRID_SIZE) {
                val px = startX + ((endX - startX) * gx) / GRID_SIZE
                val idx = rowOffset + px * pixelStride
                val luma = if (idx in 0 until buffer.limit()) {
                    (buffer.get(idx).toInt() and 0xFF).toFloat()
                } else {
                    0f
                }
                val cell = gy * GRID_SIZE + gx
                raw[cell] = luma
                sum += luma
            }
        }

        val mean = sum / raw.size
        var sqSum = 0f
        for (i in raw.indices) {
            val centered = raw[i] - mean
            raw[i] = centered
            sqSum += centered * centered
        }

        val norm = sqrt(sqSum)
        if (norm < 1e-3f) return null
        for (i in raw.indices) {
            raw[i] /= norm
        }
        return raw
    }

    private fun extractVerticalSliceBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val fullBmp = imageProxy.toBitmap()
            val rot = imageProxy.imageInfo.rotationDegrees
            val upright = if (rot != 0) {
                val matrix = Matrix().apply { postRotate(rot.toFloat()) }
                Bitmap.createBitmap(fullBmp, 0, 0, fullBmp.width, fullBmp.height, matrix, true).also {
                    if (it !== fullBmp) fullBmp.recycle()
                }
            } else {
                fullBmp
            }

            val cropW = (upright.width * 0.34f).toInt().coerceIn(24, upright.width)
            val cropX = ((upright.width - cropW) / 2).coerceAtLeast(0)
            val cropH = (upright.height * 0.78f).toInt().coerceIn(48, upright.height)
            val cropY = ((upright.height - cropH) / 2).coerceAtLeast(0)

            val cropped = Bitmap.createBitmap(upright, cropX, cropY, cropW, cropH)
            if (cropped !== upright) upright.recycle()

            val scaled = Bitmap.createScaledBitmap(cropped, SLICE_WIDTH_PX, SLICE_HEIGHT_PX, true)
            if (scaled !== cropped) cropped.recycle()
            scaled
        } catch (_: Exception) {
            null
        }
    }

    private fun stitchCylindricalSlicesLocked(): Bitmap? {
        if (capturedSlices.isEmpty()) return null
        val totalWidth = (capturedSlices.size * SLICE_WIDTH_PX).coerceAtLeast(SLICE_WIDTH_PX)
        val result = Bitmap.createBitmap(totalWidth, SLICE_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        var x = 0f
        for (slice in capturedSlices) {
            if (!slice.isRecycled) {
                canvas.drawBitmap(slice, x, 0f, null)
                x += SLICE_WIDTH_PX
            }
        }
        return result
    }

    private fun saveBitmapToCache(bitmap: Bitmap): String? {
        return try {
            val dir = File(context.cacheDir, "panoramic_360").apply { mkdirs() }
            val file = File(dir, "unwrap_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
        }
        return dot.coerceIn(-1f, 1f)
    }

    private fun maxShiftedCosineSimilarity(curr: FloatArray, start: FloatArray): Float {
        var best = cosineSimilarity(curr, start)
        for (shift in -2..2) {
            if (shift == 0) continue
            var dot = 0f
            var normA = 0f
            var normB = 0f
            for (y in 0 until GRID_SIZE) {
                for (x in 0 until GRID_SIZE) {
                    val sx = x + shift
                    if (sx in 0 until GRID_SIZE) {
                        val va = curr[y * GRID_SIZE + x]
                        val vb = start[y * GRID_SIZE + sx]
                        dot += va * vb
                        normA += va * va
                        normB += vb * vb
                    }
                }
            }
            val denom = sqrt(normA * normB)
            if (denom > 1e-4f) {
                best = max(best, (dot / denom).coerceIn(-1f, 1f))
            }
        }
        return best
    }

    companion object {
        private const val GRID_SIZE = 16
        private const val SLICE_WIDTH_PX = 84
        private const val SLICE_HEIGHT_PX = 210
        private const val MOTION_MIN_DELTA = 0.018f
        private const val MOTION_SHIFT_HINT = 0.065f
        private const val KEYFRAME_VIEW_SHIFT = 0.115f
        private const val BLUR_REJECT_DELTA = 0.45f
        private const val CAPTURE_COOLDOWN_MS = 230L
        private const val MIN_FRAMES_FOR_CLOSURE = 7
        private const val MIN_OPPOSITE_DISSIMILARITY = 0.16f
        private const val LOOP_CLOSURE_SIMILARITY = 0.84f
        private const val TARGET_LOOP_FRAMES = 12f
        private const val MAX_KEYFRAMES = 16
    }
}
