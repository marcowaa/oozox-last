package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.Base64
import com.example.data.local.AppDatabase
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class TransferQrPayload(
    val transferId: String,
    val qrNonce: String,
    val targetEcdhPubKey: String
)

data class TransferProgressState(
    val phase: String = "",
    val progressPercent: Int = 0,
    val currentChunk: Int = 0,
    val totalChunks: Int = 0,
    val message: String = ""
)

data class TransferCompletionStats(
    val totalRecords: Int = 0,
    val mediaFilesCount: Int = 0,
    val restoredRecords: Int = totalRecords,
    val restoredMedia: Int = mediaFilesCount
)

data class EncryptedTransferChunk(
    val chunkIndex: Int,
    val totalChunks: Int,
    val cipherChunkBase64: String,
    val ivBase64: String,
    val chunkSha256: String
)

/**
 * End-to-End Encrypted Cross-Device Transfer Engine.
 * Security model:
 * - Ephemeral ECDH P-256 (secp256r1) keypair per transfer session on both new & old phones.
 * - HKDF-SHA256 key derivation bound to `OOZOX_XFER|<transferId>` + `oozox-transfer-e2e-v1`.
 * - 6-digit Short Authentication String (SAS) identical on both phones for MITM visual verification.
 * - AES-256-GCM encrypted 512KB chunks with per-chunk SHA-256 and resumable upload/download.
 * - Server relays ciphertext only and revokes the old device session strictly after `POST /api/transfer/:id/complete`.
 */
object CrossDeviceTransferEngine {

    private const val CURVE_NAME = "secp256r1"
    private const val HKDF_INFO = "oozox-transfer-e2e-v1"
    private const val AES_GCM_IV_BYTES = 12
    private const val AES_GCM_TAG_BITS = 128
    private const val CHUNK_SIZE_BYTES = 512 * 1024

    fun generateEcdhKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec(CURVE_NAME), SecureRandom())
        return kpg.generateKeyPair()
    }

    fun generateEphemeralKeyPair(): KeyPair = generateEcdhKeyPair()

    fun encodePublicKey(key: PublicKey): String =
        Base64.encodeToString(key.encoded, Base64.NO_WRAP)

    fun encodePublicKeyBase64(key: PublicKey): String = encodePublicKey(key)

    fun decodePublicKey(base64: String): PublicKey {
        val bytes = Base64.decode(base64.trim(), Base64.NO_WRAP)
        return KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes))
    }

    /**
     * Derives a 256-bit symmetric AES key from ephemeral ECDH shared secret using HKDF-SHA256
     * bound symmetrically to the transferId so both phones derive the exact same key.
     */
    fun deriveAesSessionKey(
        localPrivateKey: PrivateKey,
        remotePublicKeyBase64: String,
        transferId: String
    ): ByteArray {
        val remotePub = decodePublicKey(remotePublicKeyBase64)
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(localPrivateKey)
        agreement.doPhase(remotePub, true)
        val sharedSecret = agreement.generateSecret()

        val salt = MessageDigest.getInstance("SHA-256")
            .digest("OOZOX_XFER|$transferId".toByteArray(StandardCharsets.UTF_8))

        val extractMac = Mac.getInstance("HmacSHA256")
        extractMac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = extractMac.doFinal(sharedSecret)

        val expandMac = Mac.getInstance("HmacSHA256")
        expandMac.init(SecretKeySpec(prk, "HmacSHA256"))
        expandMac.update(HKDF_INFO.toByteArray(StandardCharsets.UTF_8))
        expandMac.update(0x01.toByte())
        return expandMac.doFinal().copyOf(32)
    }

    fun deriveAesKey(localKeyPair: KeyPair, remotePublicKeyBase64: String, transferId: String = "default"): ByteArray =
        deriveAesSessionKey(localKeyPair.private, remotePublicKeyBase64, transferId)

    /**
     * Computes a symmetric 6-digit Short Authentication String (SAS) from both public keys, transferId, and derived AES key.
     */
    fun computeSasCode(
        aesKey: ByteArray,
        transferId: String,
        localPublicKeyBase64: String,
        remotePublicKeyBase64: String
    ): String {
        val sorted = listOf(localPublicKeyBase64.trim(), remotePublicKeyBase64.trim()).sorted()
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(aesKey, "HmacSHA256"))
        mac.update("SAS|$transferId|${sorted[0]}|${sorted[1]}".toByteArray(StandardCharsets.UTF_8))
        val digest = mac.doFinal()
        val num = ((digest[0].toInt() and 0x7F) shl 24) or
            ((digest[1].toInt() and 0xFF) shl 16) or
            ((digest[2].toInt() and 0xFF) shl 8) or
            (digest[3].toInt() and 0xFF)
        return "%06d".format(num % 1_000_000)
    }

    fun computeSasCode(
        localPublicKeyBase64: String,
        remotePublicKeyBase64: String,
        aesKey: ByteArray
    ): String = computeSasCode(aesKey, "default", localPublicKeyBase64, remotePublicKeyBase64)

    fun encryptAesGcmRaw(plainBytes: ByteArray, aesKey: ByteArray, aad: String = ""): Pair<ByteArray, ByteArray> {
        val iv = ByteArray(AES_GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(AES_GCM_TAG_BITS, iv))
        if (aad.isNotEmpty()) cipher.updateAAD(aad.toByteArray(StandardCharsets.UTF_8))
        val cipherBytes = cipher.doFinal(plainBytes)
        return iv to cipherBytes
    }

    fun encryptAesGcm(aesKey: ByteArray, plainBytes: ByteArray, aad: String = ""): Pair<String, String> {
        val (iv, cipherBytes) = encryptAesGcmRaw(plainBytes, aesKey, aad)
        return Base64.encodeToString(cipherBytes, Base64.NO_WRAP) to Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    fun decryptAesGcm(cipherBytes: ByteArray, iv: ByteArray, aesKey: ByteArray, aad: String = ""): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(AES_GCM_TAG_BITS, iv))
        if (aad.isNotEmpty()) cipher.updateAAD(aad.toByteArray(StandardCharsets.UTF_8))
        return cipher.doFinal(cipherBytes)
    }

    fun decryptAesGcm(aesKey: ByteArray, cipherBase64: String, ivBase64: String, aad: String = ""): ByteArray =
        decryptAesGcm(
            cipherBytes = Base64.decode(cipherBase64, Base64.NO_WRAP),
            iv = Base64.decode(ivBase64, Base64.NO_WRAP),
            aesKey = aesKey,
            aad = aad
        )

    fun splitAndEncryptChunks(plainJson: String, aesKey: ByteArray, transferId: String): List<EncryptedTransferChunk> {
        val rawBytes = plainJson.toByteArray(StandardCharsets.UTF_8)
        val slices = if (rawBytes.isEmpty()) {
            listOf(ByteArray(0))
        } else {
            val list = mutableListOf<ByteArray>()
            var offset = 0
            while (offset < rawBytes.size) {
                val end = minOf(offset + CHUNK_SIZE_BYTES, rawBytes.size)
                list.add(rawBytes.copyOfRange(offset, end))
                offset = end
            }
            list
        }
        val total = slices.size
        return slices.mapIndexed { idx, slice ->
            val (iv, cipherBytes) = encryptAesGcmRaw(slice, aesKey, aad = "$transferId:$idx")
            EncryptedTransferChunk(
                chunkIndex = idx,
                totalChunks = total,
                cipherChunkBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP),
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
                chunkSha256 = SecureBackupVaultEngine.sha256Hex(cipherBytes)
            )
        }
    }

    fun decryptAndReassembleChunks(
        chunks: List<ApiTransferDownloadChunkData>,
        aesKey: ByteArray,
        transferId: String
    ): String {
        val ordered = chunks.sortedBy { it.chunkIndex }
        val output = java.io.ByteArrayOutputStream()
        for (chunk in ordered) {
            val cipherBytes = Base64.decode(chunk.cipherChunkBase64, Base64.NO_WRAP)
            val actualSha = SecureBackupVaultEngine.sha256Hex(cipherBytes)
            if (!actualSha.equals(chunk.chunkSha256, ignoreCase = true)) {
                throw SecurityException("تحذير سلامة البيانات: بصمة الحزمة رقم ${chunk.chunkIndex} غير متطابقة!")
            }
            val iv = Base64.decode(chunk.ivBase64, Base64.NO_WRAP)
            val plainSlice = decryptAesGcm(cipherBytes, iv, aesKey, aad = "$transferId:${chunk.chunkIndex}")
            output.write(plainSlice)
        }
        return String(output.toByteArray(), StandardCharsets.UTF_8)
    }

    fun parseQrPayload(rawQr: String): TransferQrPayload? {
        return try {
            val uri = Uri.parse(rawQr.trim())
            val transferId = uri.getQueryParameter("transferId")?.trim().orEmpty()
            val qrNonce = uri.getQueryParameter("nonce")?.trim().orEmpty()
            val pub = uri.getQueryParameter("pub")?.trim().orEmpty()
            if (transferId.isBlank() || pub.isBlank()) null
            else TransferQrPayload(transferId = transferId, qrNonce = qrNonce, targetEcdhPubKey = pub)
        } catch (_: Exception) {
            null
        }
    }

    fun parseTransferQrPayload(rawQr: String): TransferQrPayload? = parseQrPayload(rawQr)

    fun generateQrBitmap(content: String, sizePx: Int = 560): Bitmap? {
        return try {
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Source (Old Phone) end-to-end execution used by DeviceTransferDialog & IncomingTransferApprovalDialog.
     */
    suspend fun approveAndUploadFullAccount(
        context: Context,
        transferId: String,
        targetEcdhPubKey: String,
        ownerPhone: String,
        approvalMethod: String,
        qrNonce: String? = null,
        onSasReady: (String) -> Unit = {},
        onProgress: (TransferProgressState) -> Unit = {}
    ): Result<TransferCompletionStats> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val effectivePhone = ownerPhone.ifBlank {
                db.userDao().getCurrentUserSync()?.phone
                    ?: SecureCredentialVault.getTrustedPhone(context).orEmpty()
            }
            onProgress(TransferProgressState("KEY_EXCHANGE", 10, 0, 0, "توليد مفاتيح التشفير ECDH..."))
            val sourceKeyPair = generateEcdhKeyPair()
            val sourcePubB64 = encodePublicKey(sourceKeyPair.public)
            val aesKey = deriveAesSessionKey(sourceKeyPair.private, targetEcdhPubKey, transferId)
            val sas = computeSasCode(aesKey, transferId, sourcePubB64, targetEcdhPubKey)
            withContext(Dispatchers.Main) { onSasReady(sas) }

            onProgress(TransferProgressState("APPROVING", 20, 0, 0, "تأكيد تفويض النقل على الخادم (SAS: $sas)..."))
            val approveResp = RetrofitClient.apiService().approveDeviceTransfer(
                transferId = transferId,
                request = ApiTransferApproveRequest(
                    sourceEcdhPubKey = sourcePubB64,
                    approvalMethod = approvalMethod,
                    qrNonce = qrNonce
                )
            )
            if (!approveResp.isSuccessful) {
                return@withContext Result.failure(
                    IllegalStateException(
                        AppErrorMapper.fromHttpStatus(
                            approveResp.code(),
                            fallbackAr = "تعذر تفويض النقل",
                            fallbackEn = "Couldn't approve transfer"
                        )
                    )
                )
            }

            onProgress(TransferProgressState("EXPORTING", 35, 0, 0, "تجهيز كافة المحادثات والوسائط وسجلات الخزينة..."))
            val rawExport = SecureBackupVaultEngine.buildRawVaultV3Snapshot(
                context = context,
                db = db,
                currentUserPhone = effectivePhone,
                includeMedia = true
            )

            onProgress(TransferProgressState("ENCRYPTING", 50, 0, 0, "تشفير الحزم بطبقة AES-256-GCM..."))
            val chunks = splitAndEncryptChunks(rawExport.snapshotJson, aesKey, transferId)
            chunks.forEachIndexed { idx, chunk ->
                val chunkResp = RetrofitClient.apiService().uploadTransferChunk(
                    transferId = transferId,
                    request = ApiTransferUploadChunkRequest(
                        chunkIndex = chunk.chunkIndex,
                        totalChunks = chunk.totalChunks,
                        cipherChunkBase64 = chunk.cipherChunkBase64,
                        ivBase64 = chunk.ivBase64,
                        chunkSha256 = chunk.chunkSha256
                    )
                )
                if (!chunkResp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException(
                            AppErrorMapper.fromHttpStatus(
                                chunkResp.code(),
                                fallbackAr = "تعذر رفع بيانات النقل، حاول مجدداً",
                                fallbackEn = "Couldn't upload transfer data"
                            )
                        )
                    )
                }
                val pct = 50 + (((idx + 1) * 40) / chunks.size.coerceAtLeast(1))
                onProgress(
                    TransferProgressState(
                        phase = "UPLOADING",
                        progressPercent = pct,
                        currentChunk = idx + 1,
                        totalChunks = chunks.size,
                        message = "رفع الحزمة المشفرة ${idx + 1} من ${chunks.size}..."
                    )
                )
            }

            val manifestJson = "{\"totalRecords\":${rawExport.stats.totalRecordsCount},\"mediaFilesCount\":${rawExport.stats.mediaFilesCount}}"
            val (manifestIv, manifestCipher) = encryptAesGcmRaw(manifestJson.toByteArray(StandardCharsets.UTF_8), aesKey, aad = "$transferId:manifest")
            val finResp = RetrofitClient.apiService().finalizeTransferUpload(
                transferId = transferId,
                request = ApiTransferFinalizeUploadRequest(
                    totalChunks = chunks.size,
                    encryptedManifestBase64 = Base64.encodeToString(manifestCipher, Base64.NO_WRAP),
                    manifestIvBase64 = Base64.encodeToString(manifestIv, Base64.NO_WRAP),
                    manifestSha256 = SecureBackupVaultEngine.sha256Hex(manifestCipher),
                    summary = ApiTransferSummaryDto(
                        totalRecords = rawExport.stats.totalRecordsCount,
                        mediaFilesCount = rawExport.stats.mediaFilesCount,
                        totalBytes = rawExport.snapshotJson.toByteArray(StandardCharsets.UTF_8).size.toLong()
                    )
                )
            )
            if (!finResp.isSuccessful) {
                return@withContext Result.failure(
                    IllegalStateException(
                        AppErrorMapper.fromHttpStatus(
                            finResp.code(),
                            fallbackAr = "تعذر إتمام رفع البيانات",
                            fallbackEn = "Couldn't finish uploading transfer"
                        )
                    )
                )
            }

            onProgress(
                TransferProgressState(
                    phase = "UPLOADED",
                    progressPercent = 100,
                    currentChunk = chunks.size,
                    totalChunks = chunks.size,
                    message = "تم الرفع المشفر بنجاح — بانتظار إتمام الهاتف الجديد وتسجيل الخروج التلقائي."
                )
            )
            Result.success(
                TransferCompletionStats(
                    totalRecords = rawExport.stats.totalRecordsCount,
                    mediaFilesCount = rawExport.stats.mediaFilesCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Source (Old Phone) end-to-end execution (legacy signature preserved).
     */
    suspend fun executeSourceUpload(
        context: Context,
        db: AppDatabase,
        currentUserPhone: String,
        transferId: String,
        targetEcdhPubKey: String,
        approvalMethod: String,
        qrNonce: String? = null,
        onProgress: (stage: String, pct: Int, sasCode: String?) -> Unit
    ): Result<String> {
        var latestSas = ""
        val res = approveAndUploadFullAccount(
            context = context,
            transferId = transferId,
            targetEcdhPubKey = targetEcdhPubKey,
            ownerPhone = currentUserPhone,
            approvalMethod = approvalMethod,
            qrNonce = qrNonce,
            onSasReady = { latestSas = it },
            onProgress = { st -> onProgress(st.message, st.progressPercent, latestSas.ifBlank { null }) }
        )
        return res.map { latestSas }
    }

    /**
     * Target (New Phone) end-to-end download + atomic restore + server completion (which revokes old device).
     */
    suspend fun downloadDecryptAndRestore(
        context: Context,
        statusData: ApiTransferStatusData,
        targetKeyPair: KeyPair,
        onProgress: (TransferProgressState) -> Unit = {}
    ): Result<TransferCompletionStats> = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val currentUserPhone = db.userDao().getCurrentUserSync()?.phone
            ?: SecureCredentialVault.getTrustedPhone(context).orEmpty()
        val outcome = downloadRestoreAndComplete(
            context = context,
            db = db,
            currentUserPhone = currentUserPhone,
            transferId = statusData.transferId,
            targetKeyPair = targetKeyPair,
            statusData = statusData,
            onProgress = { stage, pct ->
                onProgress(
                    TransferProgressState(
                        phase = if (pct >= 100) "COMPLETED" else "RESTORING",
                        progressPercent = pct,
                        totalChunks = statusData.totalChunks,
                        message = stage
                    )
                )
            }
        )
        when (outcome) {
            is BackupResult.Success -> Result.success(
                TransferCompletionStats(
                    totalRecords = outcome.recordsCount,
                    mediaFilesCount = outcome.backupStats?.mediaFilesCount ?: 0,
                    restoredRecords = outcome.recordsCount,
                    restoredMedia = outcome.backupStats?.mediaFilesCount ?: 0
                )
            )
            is BackupResult.Error -> Result.failure(IllegalStateException(outcome.errorMessage))
        }
    }

    /**
     * Target (New Phone) end-to-end download + atomic restore + server completion (which revokes old device).
     */
    suspend fun downloadRestoreAndComplete(
        context: Context,
        db: AppDatabase,
        currentUserPhone: String,
        transferId: String,
        targetKeyPair: KeyPair,
        statusData: ApiTransferStatusData,
        onProgress: (stage: String, pct: Int) -> Unit
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val sourcePub = statusData.sourceEcdhPubKey
            if (sourcePub.isNullOrBlank()) {
                return@withContext BackupResult.Error("لم يتم استلام المفتاح العام المؤقت من الهاتف القديم.")
            }
            val aesKey = deriveAesSessionKey(targetKeyPair.private, sourcePub, transferId)
            val totalChunks = statusData.totalChunks.coerceAtLeast(1)
            val downloaded = mutableListOf<ApiTransferDownloadChunkData>()

            for (i in 0 until totalChunks) {
                onProgress("تنزيل الحزمة المشفرة ${i + 1} من $totalChunks...", 15 + ((i * 55) / totalChunks))
                val resp = RetrofitClient.apiService().downloadTransferChunk(transferId, i)
                val chunk = resp.body()?.data
                if (!resp.isSuccessful || chunk == null) {
                    return@withContext BackupResult.Error(
                        AppErrorMapper.fromHttpStatus(
                            resp.code(),
                            fallbackAr = "تعذر تنزيل بيانات النقل، حاول مجدداً",
                            fallbackEn = "Couldn't download transfer data"
                        )
                    )
                }
                downloaded.add(chunk)
            }

            onProgress("فك التشفير والتحقق من سلامة السجلات والوسائط...", 78)
            val plainSnapshotJson = decryptAndReassembleChunks(downloaded, aesKey, transferId)

            onProgress("استعادة قاعدة البيانات والملفات ذرياً...", 88)
            val restoreOutcome = SecureBackupVaultEngine.restoreRawVaultV3Json(
                context = context,
                db = db,
                rawSnapshotJson = plainSnapshotJson,
                currentUserPhone = currentUserPhone
            )
            if (restoreOutcome is BackupResult.Error) {
                return@withContext restoreOutcome
            }

            onProgress("تأكيد النقل وإغلاق الجلسة على الهاتف القديم...", 96)
            val completeResp = RetrofitClient.apiService().completeDeviceTransfer(transferId)
            if (!completeResp.isSuccessful) {
                return@withContext BackupResult.Error("تمت الاستعادة بنجاح، لكن تعذر إغلاق جلسة الهاتف القديم تلقائياً")
            }

            onProgress("اكتمل نقل الحساب والوسائط بالكامل!", 100)
            restoreOutcome
        } catch (e: Exception) {
            BackupResult.Error(
                AppErrorMapper.fromThrowable(
                    e,
                    fallbackAr = "تعذر نقل الحساب، تحقق من الاتصال وأعد المحاولة",
                    fallbackEn = "Account transfer failed. Check connection"
                )
            )
        }
    }
}
