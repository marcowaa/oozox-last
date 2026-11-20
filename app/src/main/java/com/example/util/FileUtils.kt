package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.roundToInt

data class DocumentInfo(
    val uri: Uri,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val isImage: Boolean
)

data class ProductMediaItem(
    val uri: String,
    val isVideo: Boolean = false,
    val durationSec: Int = 0
)

data class SmartMediaProcessResult(
    val uri: String,
    val isVideo: Boolean,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val durationSec: Int = 0,
    val originalBytes: Long = 0L
)

object FileUtils {

    const val MAX_SHORT_VIDEO_MS = 30_500L

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mov", "mkv", "webm", "3gp", "3gpp", "avi", "m4v", "flv", "ts", "mts", "wmv", "mpeg", "mpg"
    )

    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "avif", "bmp", "wbmp", "dng", "raw", "tiff", "tif", "ico"
    )

    fun isVideoUriString(raw: String): Boolean {
        val low = raw.lowercase().substringBefore('?')
        val ext = low.substringAfterLast('.', "")
        return ext in VIDEO_EXTENSIONS ||
            low.contains("chat_videos") ||
            low.contains("vid_") ||
            low.contains("video/")
    }

    fun isImageNameOrMime(fileName: String, mime: String): Boolean {
        if (mime.startsWith("image/", ignoreCase = true)) return true
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in IMAGE_EXTENSIONS
    }

    fun isVideoNameOrMime(fileName: String, mime: String): Boolean {
        if (mime.startsWith("video/", ignoreCase = true)) return true
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in VIDEO_EXTENSIONS
    }

    fun getFileNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
        var name = "ملف_${System.currentTimeMillis()}"
        var size = 0L
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val retrievedName = it.getString(nameIndex)
                        if (!retrievedName.isNullOrBlank()) {
                            name = retrievedName
                        }
                    }
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = it.getLong(sizeIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(name, size)
    }

    fun getMimeType(context: Context, uri: Uri, fileName: String): String {
        var mime = try { context.contentResolver.getType(uri) } catch (_: Exception) { null }
        if (mime.isNullOrBlank() || mime == "*/*" || mime == "application/octet-stream") {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            if (extension.isNotBlank()) {
                val mapMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (!mapMime.isNullOrBlank()) {
                    mime = mapMime
                }
            }
        }
        return mime ?: "application/octet-stream"
    }

    fun isVideoUri(context: Context, uri: Uri): Boolean {
        val type = try { context.contentResolver.getType(uri) } catch (_: Exception) { null }
        if (type?.startsWith("video/", ignoreCase = true) == true) return true
        if (type?.startsWith("image/", ignoreCase = true) == true) return false
        val str = uri.toString()
        if (isVideoUriString(str)) return true
        val (name, _) = getFileNameAndSize(context, uri)
        if (isVideoNameOrMime(name, type ?: "")) return true
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            retriever.release()
            hasVideo.equals("yes", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun getVideoDurationMs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    fun formatDurationSeconds(seconds: Int): String {
        val safeSec = seconds.coerceAtLeast(0)
        val mins = safeSec / 60
        val secs = safeSec % 60
        return String.format("%d:%02d", mins, secs)
    }

    /**
     * Decodes any image format supported by the OS (JPEG, PNG, WEBP, GIF, HEIC, HEIF, AVIF, BMP, etc.)
     * while applying EXIF orientation and preserving full aspect ratio without cropping.
     */
    fun decodeBitmapAnyFormat(context: Context, uri: Uri, maxDimensionPx: Int = 1800): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width.coerceAtLeast(1)
                    val h = info.size.height.coerceAtLeast(1)
                    val longest = max(w, h)
                    if (longest > maxDimensionPx) {
                        val scale = maxDimensionPx.toFloat() / longest.toFloat()
                        decoder.setTargetSize(
                            (w * scale).roundToInt().coerceAtLeast(1),
                            (h * scale).roundToInt().coerceAtLeast(1)
                        )
                    }
                }
            } catch (_: Exception) {
                // Fall back to BitmapFactory path below
            }
        }

        return try {
            val orientation = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (_: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val srcW = boundsOpts.outWidth
            val srcH = boundsOpts.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            var inSampleSize = 1
            val longestEdge = max(srcW, srcH)
            while (longestEdge / (inSampleSize * 2) >= maxDimensionPx) {
                inSampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampled = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val exifMatrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> exifMatrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> exifMatrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> exifMatrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> exifMatrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> exifMatrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    exifMatrix.postRotate(90f)
                    exifMatrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    exifMatrix.postRotate(270f)
                    exifMatrix.postScale(-1f, 1f)
                }
            }
            if (!exifMatrix.isIdentity) {
                Bitmap.createBitmap(sampled, 0, 0, sampled.width, sampled.height, exifMatrix, true).also {
                    if (it != sampled) sampled.recycle()
                }
            } else {
                sampled
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reads EXIF rotation/flip orientation and normalizes the image WITHOUT cropping any pixel,
     * supporting any image or video format via ImageDecoder / BitmapFactory / raw stream copy fallback.
     */
    suspend fun smartProcessAndStoreMedia(
        context: Context,
        sourceUri: Uri,
        isVideo: Boolean,
        targetFolder: String = "product_media",
        maxDimensionPx: Int = 1600,
        jpegQuality: Int = 88,
        onProgress: (Float) -> Unit = {}
    ): SmartMediaProcessResult? = withContext(Dispatchers.IO) {
        try {
            onProgress(0.08f)
            val mediaDir = File(context.filesDir, targetFolder).apply { mkdirs() }
            val (origName, origSize) = getFileNameAndSize(context, sourceUri)

            if (isVideo) {
                val mime = try { context.contentResolver.getType(sourceUri) } catch (_: Exception) { null }
                val extFromMime = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                val extFromName = origName.substringAfterLast('.', "").takeIf { it.length in 2..5 }
                val ext = extFromMime ?: extFromName ?: "mp4"
                val targetFile = File(mediaDir, "VID_${System.currentTimeMillis()}_${(100..999).random()}.$ext")

                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var copied = 0L
                        val expectedTotal = origSize.coerceAtLeast(1L)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            if (origSize > 0L) {
                                onProgress((0.1f + 0.85f * (copied.toFloat() / expectedTotal.toFloat())).coerceIn(0.1f, 0.95f))
                            }
                        }
                    }
                } ?: return@withContext null

                val durMs = getVideoDurationMs(context, Uri.fromFile(targetFile))
                val durSec = ((durMs / 1000L).toInt()).coerceAtLeast(1)
                onProgress(1f)
                return@withContext SmartMediaProcessResult(
                    uri = Uri.fromFile(targetFile).toString(),
                    isVideo = true,
                    width = 0,
                    height = 0,
                    sizeBytes = targetFile.length(),
                    durationSec = durSec,
                    originalBytes = origSize
                )
            }

            // Image smart processing across all formats
            onProgress(0.32f)
            val orientedBitmap = decodeBitmapAnyFormat(context, sourceUri, maxDimensionPx)
            if (orientedBitmap == null) {
                val fallbackUri = copyMediaUriToInternalStorage(context, sourceUri, isVideo = false, targetFolder = targetFolder)
                    ?: return@withContext null
                onProgress(1f)
                return@withContext SmartMediaProcessResult(
                    uri = fallbackUri,
                    isVideo = false,
                    width = 0,
                    height = 0,
                    sizeBytes = origSize,
                    originalBytes = origSize
                )
            }

            onProgress(0.74f)
            val curMax = max(orientedBitmap.width, orientedBitmap.height)
            val finalBitmap = if (curMax > maxDimensionPx && curMax > 0) {
                val ratio = maxDimensionPx.toFloat() / curMax.toFloat()
                val dstW = (orientedBitmap.width * ratio).roundToInt().coerceAtLeast(1)
                val dstH = (orientedBitmap.height * ratio).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(orientedBitmap, dstW, dstH, true).also {
                    if (it != orientedBitmap) orientedBitmap.recycle()
                }
            } else {
                orientedBitmap
            }

            val preserveAlpha = finalBitmap.hasAlpha()
            val ext = if (preserveAlpha) "png" else "jpg"
            val targetFile = File(mediaDir, "IMG_${System.currentTimeMillis()}_${(100..999).random()}.$ext")
            FileOutputStream(targetFile).use { out ->
                if (preserveAlpha) {
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } else {
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(74, 96), out)
                }
            }

            val finalW = finalBitmap.width
            val finalH = finalBitmap.height
            finalBitmap.recycle()
            onProgress(1f)

            SmartMediaProcessResult(
                uri = Uri.fromFile(targetFile).toString(),
                isVideo = false,
                width = finalW,
                height = finalH,
                sizeBytes = targetFile.length(),
                originalBytes = origSize
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Losslessly trims and/or mutes any compatible video into internal MP4 storage,
     * falling back to direct stream copy if an exotic container is not supported by MediaMuxer.
     */
    suspend fun trimOrMuteVideoToInternalStorage(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        endMs: Long,
        muteAudio: Boolean,
        targetFolder: String = "chat_videos"
    ): SmartMediaProcessResult? = withContext(Dispatchers.IO) {
        val totalMs = getVideoDurationMs(context, sourceUri)
        val isFullLength = startMs <= 250L && (totalMs <= 0L || endMs >= totalMs - 250L)
        if (isFullLength && !muteAudio) {
            return@withContext smartProcessAndStoreMedia(
                context = context,
                sourceUri = sourceUri,
                isVideo = true,
                targetFolder = targetFolder
            )
        }

        val mediaDir = File(context.filesDir, targetFolder).apply { mkdirs() }
        val targetFile = File(mediaDir, "VID_EDIT_${System.currentTimeMillis()}.mp4")
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(context, sourceUri, null)
            muxer = MediaMuxer(targetFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackIndexMap = HashMap<Int, Int>()
            var maxBufferSize = 256 * 1024

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                val isAudio = mime.startsWith("audio/")
                val isVideo = mime.startsWith("video/")
                if (!isVideo && !(isAudio && !muteAudio)) continue

                extractor.selectTrack(i)
                val dstTrack = muxer.addTrack(format)
                trackIndexMap[i] = dstTrack
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    val reqSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                    if (reqSize > maxBufferSize) maxBufferSize = reqSize
                }
            }

            if (trackIndexMap.isEmpty()) {
                return@withContext smartProcessAndStoreMedia(context, sourceUri, isVideo = true, targetFolder = targetFolder)
            }

            // Preserve orientation rotation metadata if present
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, sourceUri)
                val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
                if (rot != null) muxer.setOrientationHint(rot)
                retriever.release()
            } catch (_: Exception) {}

            muxer.start()
            val startUs = (startMs.coerceAtLeast(0L)) * 1000L
            val endUs = if (endMs > startMs) endMs * 1000L else Long.MAX_VALUE
            if (startUs > 0L) {
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }
                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) break
                val srcTrack = extractor.sampleTrackIndex
                val dstTrack = trackIndexMap[srcTrack]
                if (dstTrack != null && sampleTimeUs >= startUs) {
                    bufferInfo.presentationTimeUs = (sampleTimeUs - startUs).coerceAtLeast(0L)
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(dstTrack, buffer, bufferInfo)
                }
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()

            if (!targetFile.exists() || targetFile.length() == 0L) {
                targetFile.delete()
                return@withContext smartProcessAndStoreMedia(context, sourceUri, isVideo = true, targetFolder = targetFolder)
            }

            val durMs = getVideoDurationMs(context, Uri.fromFile(targetFile))
            val durSec = ((durMs / 1000L).toInt()).coerceAtLeast(1)
            SmartMediaProcessResult(
                uri = Uri.fromFile(targetFile).toString(),
                isVideo = true,
                width = 0,
                height = 0,
                sizeBytes = targetFile.length(),
                durationSec = durSec,
                originalBytes = targetFile.length()
            )
        } catch (_: Exception) {
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
            try { if (targetFile.exists()) targetFile.delete() } catch (_: Exception) {}
            smartProcessAndStoreMedia(context, sourceUri, isVideo = true, targetFolder = targetFolder)
        }
    }

    fun copyMediaUriToInternalStorage(
        context: Context,
        sourceUri: Uri,
        isVideo: Boolean,
        targetFolder: String = "product_media"
    ): String? {
        return try {
            val mediaDir = File(context.filesDir, targetFolder).apply { mkdirs() }
            val (origName, _) = getFileNameAndSize(context, sourceUri)
            val mime = try { context.contentResolver.getType(sourceUri) } catch (_: Exception) { null }
            val extFromMime = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            val extFromName = origName.substringAfterLast('.', "").takeIf { it.length in 2..5 }
            val ext = extFromMime ?: extFromName ?: if (isVideo) "mp4" else "jpg"
            val prefix = if (isVideo) "VID" else "IMG"
            val targetFile = File(mediaDir, "${prefix}_${System.currentTimeMillis()}_${(100..999).random()}.$ext")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            Uri.fromFile(targetFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseProductMedia(rawPhotoUri: String?): List<ProductMediaItem> {
        if (rawPhotoUri.isNullOrBlank()) return emptyList()
        return rawPhotoUri.split("||")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { token ->
                when {
                    token.startsWith("VID|") -> {
                        val rest = token.removePrefix("VID|")
                        val durPart = rest.substringBefore("|", "0").toIntOrNull() ?: 0
                        val uriPart = rest.substringAfter("|", rest)
                        ProductMediaItem(uri = uriPart, isVideo = true, durationSec = durPart)
                    }
                    token.startsWith("IMG|") -> {
                        ProductMediaItem(uri = token.removePrefix("IMG|"), isVideo = false, durationSec = 0)
                    }
                    else -> {
                        val vid = isVideoUriString(token)
                        ProductMediaItem(uri = token, isVideo = vid, durationSec = 0)
                    }
                }
            }
            .filter { it.uri.isNotBlank() }
    }

    fun serializeProductMedia(items: List<ProductMediaItem>): String? {
        if (items.isEmpty()) return null
        return items.joinToString("||") { item ->
            if (item.isVideo) "VID|${item.durationSec}|${item.uri}" else "IMG|${item.uri}"
        }
    }

    fun copyUriToLocalStorage(context: Context, sourceUri: Uri, originalName: String): Uri {
        return try {
            val docsDir = File(context.filesDir, "shared_documents").apply { mkdirs() }
            val cleanName = originalName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(docsDir, "${System.currentTimeMillis()}_$cleanName")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            FileProvider.getUriForFile(context, "${context.packageName}.provider", targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUri
        }
    }

    fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "مستند"
        val df = DecimalFormat("#.##")
        return when {
            sizeInBytes < 1024 -> "$sizeInBytes B"
            sizeInBytes < 1024 * 1024 -> "${df.format(sizeInBytes / 1024.0)} KB"
            sizeInBytes < 1024 * 1024 * 1024 -> "${df.format(sizeInBytes / (1024.0 * 1024.0))} MB"
            else -> "${df.format(sizeInBytes / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }

    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    fun openDocument(context: Context, uriString: String, fileName: String? = null, mimeType: String? = null) {
        try {
            val uri = Uri.parse(uriString)
            val resolvedMime = mimeType ?: if (!fileName.isNullOrBlank()) {
                val ext = getFileExtension(fileName)
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/*"
            } else "application/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, resolvedMime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "فتح المستند"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر فتح الملف، لا يوجد تطبيق متوافق: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareDocument(context: Context, uriString: String, fileName: String, mimeType: String? = null) {
        try {
            val uri = Uri.parse(uriString)
            val resolvedMime = mimeType ?: "application/*"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = resolvedMime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_TEXT, "مستند مرفق: $fileName")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة الملف عبر..."))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر مشاركة الملف: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
