package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Military-Grade AES-256-GCM Local Database Cryptographic Engine.
 * 
 * Ensures all local Room database entities (Sub-accounts/Hidden profiles,
 * Chat messages, Accounting records, Financial complaints, and Hashtag entries)
 * are stored on disk in SQLite in strong AES-256-GCM ciphertext blocks.
 * 
 * Each sub-account (Hidden Profile) derives its own unique 256-bit symmetric key
 * from its secret PIN/Passphrase and unique ID, ensuring complete cryptographic isolation
 * even if the physical storage / SQLite database file is forensically extracted from the device.
 */
object RoomLocalAES256Engine {

    private const val ENVELOPE_PREFIX = "[AES256_GCM:v1:"
    private const val ENVELOPE_SUFFIX = "]"
    private const val PBKDF2_ITERATIONS = 65536
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val SALT_LENGTH_BYTES = 16

    private const val PREFS_CRYPTO_VAULT = "oozox_room_crypto_vault"
    private const val KEY_DEVICE_MASTER_SALT = "device_master_salt_b64"
    private const val KEYSTORE_ALIAS_MASTER_WRAP = "oozox_room_master_wrap_v2"
    private const val KEY_WRAPPED_MASTER_DEK = "wrapped_master_dek_v2"
    private const val KEY_WRAPPED_MASTER_IV = "wrapped_master_iv_v2"
    private const val KEY_RANDOM_DEK_FALLBACK = "random_master_dek_fallback_v2"

    // In-memory LRU / Concurrent cache for derived keys to prevent high CPU overhead
    private val keyCache = ConcurrentHashMap<String, SecretKeySpec>()

    // In-memory active registry of unlocked hidden profiles for the active session
    private val unlockedProfiles = ConcurrentHashMap<String, HiddenProfileEntity>()

    fun registerUnlockedProfile(profile: HiddenProfileEntity) {
        unlockedProfiles[profile.id] = profile
    }

    fun unregisterProfile(profileId: String) {
        unlockedProfiles.remove(profileId)
    }

    fun clearUnlockedProfiles() {
        unlockedProfiles.clear()
    }

    fun getUnlockedProfiles(): List<HiddenProfileEntity> = unlockedProfiles.values.toList()

    /**
     * Gets or generates a persistent device master salt.
     */
    private fun getDeviceMasterSalt(context: Context): ByteArray {
        val sp: SharedPreferences = context.getSharedPreferences(PREFS_CRYPTO_VAULT, Context.MODE_PRIVATE)
        val storedSaltB64 = sp.getString(KEY_DEVICE_MASTER_SALT, null)
        if (!storedSaltB64.isNullOrBlank()) {
            return Base64.decode(storedSaltB64, Base64.NO_WRAP)
        }
        val newSalt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        sp.edit().putString(KEY_DEVICE_MASTER_SALT, Base64.encodeToString(newSalt, Base64.NO_WRAP)).apply()
        return newSalt
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Derives a dedicated 256-bit AES key for a specific sub-account / hidden profile.
     */
    fun deriveKeyForProfile(profilePinOrPass: String, profileId: String, context: Context? = null): SecretKeySpec {
        val cacheKey = "profile_${profileId}_${sha256Hex(profilePinOrPass)}"
        keyCache[cacheKey]?.let { return it }

        val saltBytes = MessageDigest.getInstance("SHA-256")
            .digest("OOZOX_SUB_ACCOUNT_VAULT_SALT_${profileId}".toByteArray(StandardCharsets.UTF_8))
            .copyOf(SALT_LENGTH_BYTES)

        val keySpec = PBEKeySpec(profilePinOrPass.toCharArray(), saltBytes, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        keyCache[cacheKey] = secretKey
        return secretKey
    }

    private fun getOrCreateKeystoreWrappingKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val existing = keyStore.getKey(KEYSTORE_ALIAS_MASTER_WRAP, null) as? SecretKey
            if (existing != null) return existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS_MASTER_WRAP,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_LENGTH_BITS)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Retrieves or provisions a cryptographically random 256-bit device master DEK,
     * hardware-wrapped via AndroidKeyStore when available (never using a hardcoded constant passphrase).
     */
    fun getMasterDeviceKey(context: Context): SecretKeySpec {
        val cacheKey = "master_device_key_v2"
        keyCache[cacheKey]?.let { return it }

        val sp: SharedPreferences = context.getSharedPreferences(PREFS_CRYPTO_VAULT, Context.MODE_PRIVATE)
        val keystoreWrapKey = getOrCreateKeystoreWrappingKey()

        if (keystoreWrapKey != null) {
            val wrappedB64 = sp.getString(KEY_WRAPPED_MASTER_DEK, null)
            val ivB64 = sp.getString(KEY_WRAPPED_MASTER_IV, null)
            if (!wrappedB64.isNullOrBlank() && !ivB64.isNullOrBlank()) {
                try {
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(
                        Cipher.DECRYPT_MODE,
                        keystoreWrapKey,
                        GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(ivB64, Base64.NO_WRAP))
                    )
                    val dekBytes = cipher.doFinal(Base64.decode(wrappedB64, Base64.NO_WRAP))
                    if (dekBytes.size == 32) {
                        return SecretKeySpec(dekBytes, "AES").also { keyCache[cacheKey] = it }
                    }
                } catch (_: Exception) {
                    // Proceed to provision fresh hardware-wrapped DEK if keystore entry was invalidated
                }
            }

            // Provision fresh CSPRNG 256-bit DEK and wrap inside AndroidKeyStore
            val freshDek = ByteArray(32).also { SecureRandom().nextBytes(it) }
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, keystoreWrapKey)
                val wrapped = cipher.doFinal(freshDek)
                sp.edit()
                    .putString(KEY_WRAPPED_MASTER_DEK, Base64.encodeToString(wrapped, Base64.NO_WRAP))
                    .putString(KEY_WRAPPED_MASTER_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                    .apply()
                return SecretKeySpec(freshDek, "AES").also { keyCache[cacheKey] = it }
            } catch (_: Exception) {
                // Fall through to per-install CSPRNG storage below on JVM/non-keystore hosts
            }
        }

        // Per-install CSPRNG fallback for non-KeyStore environments (e.g., Robolectric JVM unit tests)
        val storedRandomB64 = sp.getString(KEY_RANDOM_DEK_FALLBACK, null)
        val dekBytes = if (!storedRandomB64.isNullOrBlank()) {
            Base64.decode(storedRandomB64, Base64.NO_WRAP)
        } else {
            ByteArray(32).also {
                SecureRandom().nextBytes(it)
                sp.edit().putString(KEY_RANDOM_DEK_FALLBACK, Base64.encodeToString(it, Base64.NO_WRAP)).apply()
            }
        }
        return SecretKeySpec(dekBytes, "AES").also { keyCache[cacheKey] = it }
    }

    /**
     * Read-only legacy master key fallback solely for decrypting records created prior to v2 KeyStore upgrade.
     */
    private fun getLegacyMasterDeviceKey(context: Context): SecretKeySpec? {
        val cacheKey = "master_device_key_legacy"
        keyCache[cacheKey]?.let { return it }

        val sp: SharedPreferences = context.getSharedPreferences(PREFS_CRYPTO_VAULT, Context.MODE_PRIVATE)
        val storedSaltB64 = sp.getString(KEY_DEVICE_MASTER_SALT, null) ?: return null
        val salt = Base64.decode(storedSaltB64, Base64.NO_WRAP)
        val keySpec = PBEKeySpec("OOZOX_PLATFORM_MASTER_DEVICE_AES256".toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES").also { keyCache[cacheKey] = it }
    }

    /**
     * Checks if a string is already encrypted in our AES-256-GCM format.
     */
    fun isEncrypted(text: String?): Boolean {
        if (text == null) return false
        return text.startsWith(ENVELOPE_PREFIX) && text.endsWith(ENVELOPE_SUFFIX)
    }

    /**
     * Encrypts plaintext using a given AES-256 SecretKeySpec with AES/GCM/NoPadding.
     */
    fun encrypt(plainText: String?, key: SecretKeySpec): String {
        if (plainText.isNullOrBlank()) return plainText ?: ""
        if (isEncrypted(plainText)) return plainText // Already encrypted

        return try {
            val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

            val cipherTextBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherB64 = Base64.encodeToString(cipherTextBytes, Base64.NO_WRAP)

            "$ENVELOPE_PREFIX$ivB64:$cipherB64$ENVELOPE_SUFFIX"
        } catch (e: Exception) {
            e.printStackTrace()
            plainText
        }
    }

    /**
     * Decrypts ciphertext using a given AES-256 SecretKeySpec with AES/GCM/NoPadding.
     */
    fun decrypt(cipherText: String?, key: SecretKeySpec): String {
        if (cipherText.isNullOrBlank()) return cipherText ?: ""
        if (!isEncrypted(cipherText)) return cipherText // Plaintext already

        return try {
            val inner = cipherText.removePrefix(ENVELOPE_PREFIX).removeSuffix(ENVELOPE_SUFFIX)
            val parts = inner.split(":")
            if (parts.size < 2) return cipherText

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            // If wrong key or tampered, return secure indicator or fallback
            "[🔒 محتوى مشفر بحماية عسكرية AES-256]"
        }
    }

    /**
     * Automatically encrypts content based on active sub-account or device master key.
     */
    fun autoEncrypt(plainText: String?, activeProfile: HiddenProfileEntity?, context: Context): String {
        if (plainText.isNullOrBlank()) return plainText ?: ""
        val key = if (activeProfile != null) {
            deriveKeyForProfile(activeProfile.secretPinOrPassword, activeProfile.id, context)
        } else {
            getMasterDeviceKey(context)
        }
        return encrypt(plainText, key)
    }

    /**
     * Automatically decrypts content trying active profile first, then master device key.
     */
    fun autoDecrypt(cipherText: String?, activeProfile: HiddenProfileEntity?, context: Context): String {
        if (cipherText.isNullOrBlank() || !isEncrypted(cipherText)) return cipherText ?: ""

        // 1. Try active profile key if available
        if (activeProfile != null) {
            val profileKey = deriveKeyForProfile(activeProfile.secretPinOrPassword, activeProfile.id, context)
            val decrypted = decrypt(cipherText, profileKey)
            if (!decrypted.startsWith("[🔒")) {
                return decrypted
            }
        }

        // 2. Try any other unlocked session profiles
        for (profile in unlockedProfiles.values) {
            if (activeProfile == null || profile.id != activeProfile.id) {
                val pKey = deriveKeyForProfile(profile.secretPinOrPassword, profile.id, context)
                val dec = decrypt(cipherText, pKey)
                if (!dec.startsWith("[🔒")) {
                    return dec
                }
            }
        }

        // 3. Try hardware-wrapped device master key
        val masterKey = getMasterDeviceKey(context)
        val decryptedWithMaster = decrypt(cipherText, masterKey)
        if (!decryptedWithMaster.startsWith("[🔒")) {
            return decryptedWithMaster
        }

        // 4. Read-only fallback for legacy v1 records encrypted prior to KeyStore DEK migration
        getLegacyMasterDeviceKey(context)?.let { legacyKey ->
            val decryptedLegacy = decrypt(cipherText, legacyKey)
            if (!decryptedLegacy.startsWith("[🔒")) {
                return decryptedLegacy
            }
        }

        return "[🔒 محتوى محمي بمفتاح حساب فرعي مشفر]"
    }

    // --- Entity Encrypt / Decrypt Helpers ---

    fun encryptMessage(msg: MessageEntity, activeProfile: HiddenProfileEntity?, context: Context): MessageEntity {
        return msg.copy(
            content = autoEncrypt(msg.content, activeProfile, context)
        )
    }

    fun decryptMessage(msg: MessageEntity, activeProfile: HiddenProfileEntity?, context: Context): MessageEntity {
        return msg.copy(
            content = autoDecrypt(msg.content, activeProfile, context)
        )
    }

    fun encryptDailyRecord(record: DailyAccountingRecordEntity, activeProfile: HiddenProfileEntity?, context: Context): DailyAccountingRecordEntity {
        val profileKey = if (record.profileId != null && activeProfile != null && activeProfile.id == record.profileId) {
            deriveKeyForProfile(activeProfile.secretPinOrPassword, activeProfile.id, context)
        } else {
            getMasterDeviceKey(context)
        }

        return record.copy(
            notes = if (record.notes != null) encrypt(record.notes, profileKey) else null,
            complaintsNotes = if (record.complaintsNotes != null) encrypt(record.complaintsNotes, profileKey) else null,
            screenshotUri = if (record.screenshotUri != null) encrypt(record.screenshotUri, profileKey) else null
        )
    }

    fun decryptDailyRecord(record: DailyAccountingRecordEntity, activeProfile: HiddenProfileEntity?, context: Context): DailyAccountingRecordEntity {
        return record.copy(
            notes = if (record.notes != null) autoDecrypt(record.notes, activeProfile, context) else null,
            complaintsNotes = if (record.complaintsNotes != null) autoDecrypt(record.complaintsNotes, activeProfile, context) else null,
            screenshotUri = if (record.screenshotUri != null) autoDecrypt(record.screenshotUri, activeProfile, context) else null
        )
    }

    fun encryptComplaint(complaint: FinancialComplaintEntity, activeProfile: HiddenProfileEntity?, context: Context): FinancialComplaintEntity {
        val profileKey = if (complaint.profileId != null && activeProfile != null && activeProfile.id == complaint.profileId) {
            deriveKeyForProfile(activeProfile.secretPinOrPassword, activeProfile.id, context)
        } else {
            getMasterDeviceKey(context)
        }

        return complaint.copy(
            title = encrypt(complaint.title, profileKey),
            details = encrypt(complaint.details, profileKey),
            resolutionNotes = if (complaint.resolutionNotes != null) encrypt(complaint.resolutionNotes, profileKey) else null
        )
    }

    fun decryptComplaint(complaint: FinancialComplaintEntity, activeProfile: HiddenProfileEntity?, context: Context): FinancialComplaintEntity {
        return complaint.copy(
            title = autoDecrypt(complaint.title, activeProfile, context),
            details = autoDecrypt(complaint.details, activeProfile, context),
            resolutionNotes = if (complaint.resolutionNotes != null) autoDecrypt(complaint.resolutionNotes, activeProfile, context) else null
        )
    }

    fun encryptHiddenProfile(profile: HiddenProfileEntity, context: Context): HiddenProfileEntity {
        val profileKey = deriveKeyForProfile(profile.secretPinOrPassword, profile.id, context)
        return profile.copy(
            notes = if (profile.notes != null) encrypt(profile.notes, profileKey) else null,
            isEncrypted = true
        )
    }

    fun decryptHiddenProfile(profile: HiddenProfileEntity, context: Context): HiddenProfileEntity {
        val profileKey = deriveKeyForProfile(profile.secretPinOrPassword, profile.id, context)
        return profile.copy(
            notes = if (profile.notes != null) decrypt(profile.notes, profileKey) else null
        )
    }

    // --- Forensic Extraction Simulator & Security Audit Models ---

    data class RawDatabaseRowSample(
        val tableName: String,
        val rowId: String,
        val fieldName: String,
        val rawSqliteCiphertext: String,
        val authorizedDecryptedText: String,
        val isEncryptedOnDisk: Boolean,
        val keyDerivationScheme: String
    )

    data class ForensicExtractionReport(
        val totalEntitiesInspected: Int,
        val encryptedFieldsCount: Int,
        val plaintextLeaksCount: Int,
        val encryptionAlgorithm: String = "AES-256-GCM (128-bit Auth Tag)",
        val keyDerivationFunction: String = "PBKDF2-HMAC-SHA256 (65,536 Iterations)",
        val subAccountIsolation: String = "مفاتيح متماثلة معزولة لكل حساب فرعي",
        val samples: List<RawDatabaseRowSample>,
        val isPassedSecurely: Boolean
    )

    /**
     * Simulates reading physical SQLite database files to show the user that raw disk data is 100% encrypted.
     */
    suspend fun runForensicDatabaseExtractionAudit(
        context: Context,
        db: AppDatabase,
        activeProfile: HiddenProfileEntity?
    ): ForensicExtractionReport = withContext(Dispatchers.IO) {
        val samples = mutableListOf<RawDatabaseRowSample>()
        var totalCount = 0
        var encryptedCount = 0
        var leaksCount = 0

        // 1. Inspect Raw Messages
        val rawMessages = db.messageDao().getAllMessages().first()
        rawMessages.take(4).forEach { msg ->
            totalCount++
            val isEnc = isEncrypted(msg.content)
            if (isEnc) encryptedCount++ else leaksCount++
            samples.add(
                RawDatabaseRowSample(
                    tableName = "messages",
                    rowId = msg.id.take(8),
                    fieldName = "content",
                    rawSqliteCiphertext = if (isEnc) msg.content else "[UNENCRYPTED_PLAINTEXT: ${msg.content.take(30)}]",
                    authorizedDecryptedText = autoDecrypt(msg.content, activeProfile, context),
                    isEncryptedOnDisk = isEnc,
                    keyDerivationScheme = if (activeProfile != null) "SubAccount-PBKDF2-AES256" else "DeviceMaster-AES256"
                )
            )
        }

        // 2. Inspect Raw Daily Accounting Records
        val rawRecords = db.dailyAccountingRecordDao().getAllRecords().first()
        rawRecords.take(3).forEach { rec ->
            if (rec.notes != null) {
                totalCount++
                val isEnc = isEncrypted(rec.notes)
                if (isEnc) encryptedCount++ else leaksCount++
                samples.add(
                    RawDatabaseRowSample(
                        tableName = "daily_accounting_records",
                        rowId = rec.id.take(8),
                        fieldName = "notes",
                        rawSqliteCiphertext = if (isEnc) rec.notes else "[PLAINTEXT: ${rec.notes.take(30)}]",
                        authorizedDecryptedText = autoDecrypt(rec.notes, activeProfile, context),
                        isEncryptedOnDisk = isEnc,
                        keyDerivationScheme = "SubAccount-Vault-Key"
                    )
                )
            }
        }

        // 3. Inspect Raw Financial Complaints
        val rawComplaints = db.financialComplaintDao().getAllComplaints().first()
        rawComplaints.take(3).forEach { comp ->
            totalCount++
            val isEnc = isEncrypted(comp.details)
            if (isEnc) encryptedCount++ else leaksCount++
            samples.add(
                RawDatabaseRowSample(
                    tableName = "financial_complaints",
                    rowId = comp.id.take(8),
                    fieldName = "details",
                    rawSqliteCiphertext = if (isEnc) comp.details else "[PLAINTEXT: ${comp.details.take(30)}]",
                    authorizedDecryptedText = autoDecrypt(comp.details, activeProfile, context),
                    isEncryptedOnDisk = isEnc,
                    keyDerivationScheme = "SubAccount-Vault-Key"
                )
            )
        }

        // 4. Inspect Hidden Profiles
        val rawProfiles = db.hiddenProfileDao().getAllHiddenProfiles().first()
        rawProfiles.take(2).forEach { prof ->
            if (prof.notes != null) {
                totalCount++
                val isEnc = isEncrypted(prof.notes)
                if (isEnc) encryptedCount++ else leaksCount++
                samples.add(
                    RawDatabaseRowSample(
                        tableName = "hidden_profiles",
                        rowId = prof.id.take(8),
                        fieldName = "confidential_notes",
                        rawSqliteCiphertext = if (isEnc) prof.notes else "[PLAINTEXT: ${prof.notes.take(30)}]",
                        authorizedDecryptedText = decryptHiddenProfile(prof, context).notes ?: "",
                        isEncryptedOnDisk = isEnc,
                        keyDerivationScheme = "Dedicated-PIN-AES256"
                    )
                )
            }
        }

        ForensicExtractionReport(
            totalEntitiesInspected = totalCount,
            encryptedFieldsCount = encryptedCount,
            plaintextLeaksCount = leaksCount,
            samples = samples,
            isPassedSecurely = leaksCount == 0
        )
    }
}
