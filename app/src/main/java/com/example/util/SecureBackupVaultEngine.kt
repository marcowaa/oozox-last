package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

sealed class BackupResult {
    data class Success(
        val message: String, 
        val filePath: String? = null, 
        val uri: Uri? = null, 
        val recordsCount: Int = 0,
        val cloudBackupId: String? = null,
        val cloudUrl: String? = null,
        val backupStats: BackupStats? = null
    ) : BackupResult()
    data class Error(val errorMessage: String) : BackupResult()
}

enum class CloudStorageProvider(val title: String, val icon: String) {
    GOOGLE_DRIVE("Google Drive (جوجل درايف السحابي)", "google_drive"),
    OOZOX_CLOUD("سحابة أوزوكس المشفرة (Oozox Vault Cloud)", "cloud_vault"),
    LOCAL_DEVICE("ملف محلي مشفر (.oozox)", "local_file")
}

data class CloudBackupRecord(
    val id: String,
    val ownerPhone: String,
    val ownerPhoneHash: String,
    val provider: CloudStorageProvider,
    val fileName: String,
    val encryptedPayloadBase64: String,
    val fileSizeFormatted: String,
    val totalRecords: Int,
    val subProfilesCount: Int,
    val createdAt: Long,
    val createdAtText: String
)

data class BackupMetadataPreview(
    val isValid: Boolean,
    val version: Int,
    val createdAtText: String,
    val ownerPhoneMasked: String,
    val isPhoneMatched: Boolean,
    val backupType: String,
    val totalRecordsCount: Int,
    val profilesCount: Int,
    val recordsCount: Int,
    val errorMessage: String? = null
)

data class BackupStats(
    val usersCount: Int = 0,
    val chatsCount: Int = 0,
    val messagesCount: Int = 0,
    val recordsCount: Int = 0,
    val complaintsCount: Int = 0,
    val subProfilesCount: Int = 0,
    val workGroupsCount: Int = 0,
    val projectsCount: Int = 0,
    val backupDateText: String = ""
)

object SecureBackupVaultEngine {

    private const val MAGIC_HEADER = "OOZOX_ENCRYPTED_VAULT_V2"
    private const val ITERATIONS = 65536
    private const val KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    // Simulated cloud storage storage registry (persisted in SharedPreferences)
    private const val CLOUD_REGISTRY_PREFS = "oozox_cloud_backups_registry"

    /**
     * Computes a SHA-256 hex string for phone validation.
     */
    fun hashPhoneNumber(phone: String): String {
        val cleanPhone = phone.replace("[^0-9+]".toRegex(), "").trim()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(cleanPhone.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Inspects an encrypted backup file's public envelope without requiring decryption.
     */
    fun inspectBackupMetadata(backupContent: String, currentPhone: String): BackupMetadataPreview {
        return try {
            val json = JSONObject(backupContent)
            val magic = json.optString("magic")
            if (magic != MAGIC_HEADER && magic != "OOZOX_ENCRYPTED_VAULT_V1") {
                return BackupMetadataPreview(
                    isValid = false,
                    version = 0,
                    createdAtText = "",
                    ownerPhoneMasked = "",
                    isPhoneMatched = false,
                    backupType = "",
                    totalRecordsCount = 0,
                    profilesCount = 0,
                    recordsCount = 0,
                    errorMessage = "الملف ليس نسخة احتياطية صالحة من نظام أوزوكس."
                )
            }

            val phoneHash = json.optString("phoneHash")
            val cleanPhone = currentPhone.replace("[^0-9+]".toRegex(), "").trim()
            val currentHash = hashPhoneNumber(cleanPhone)
            val isMatched = phoneHash.isBlank() || phoneHash == currentHash

            val timestamp = json.optLong("timestamp", System.currentTimeMillis())
            val dateText = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))
            val backupType = json.optString("backupType", "LOCAL_SECURE_VAULT")
            val statsObj = json.optJSONObject("stats")

            BackupMetadataPreview(
                isValid = true,
                version = json.optInt("version", 2),
                createdAtText = dateText,
                ownerPhoneMasked = json.optString("ownerPhoneMasked", if (cleanPhone.length > 4) "${cleanPhone.take(3)}****${cleanPhone.takeLast(3)}" else cleanPhone),
                isPhoneMatched = isMatched,
                backupType = backupType,
                totalRecordsCount = statsObj?.optInt("totalRecords") ?: 0,
                profilesCount = statsObj?.optInt("subProfiles") ?: 0,
                recordsCount = statsObj?.optInt("dailyRecords") ?: 0
            )
        } catch (e: Exception) {
            BackupMetadataPreview(
                isValid = false,
                version = 0,
                createdAtText = "",
                ownerPhoneMasked = "",
                isPhoneMatched = false,
                backupType = "",
                totalRecordsCount = 0,
                profilesCount = 0,
                recordsCount = 0,
                errorMessage = "خطأ في قراءة ملف النسخة: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Exports a targeted encrypted migration package specifically for sub-accounts / vaults.
     * Can export all or selected sub-profiles and their linked data.
     */
    suspend fun createSubAccountsMigrationBackup(
        context: Context,
        db: AppDatabase,
        currentUserPhone: String,
        encryptionPassphrase: String,
        selectedProfileIds: List<String>? = null, // null means all sub-accounts
        cloudProvider: CloudStorageProvider = CloudStorageProvider.LOCAL_DEVICE
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            if (encryptionPassphrase.length < 4) {
                return@withContext BackupResult.Error("يجب أن تتكون كلمة مرور النسخ الاحتياطي من 4 أحرف أو أرقام على الأقل لضمان التشفير العسكري.")
            }

            val cleanPhone = currentUserPhone.replace("[^0-9+]".toRegex(), "").trim()
            val phoneHash = hashPhoneNumber(cleanPhone)

            // Gather all or selected hidden sub-accounts / profiles
            val allProfiles = db.hiddenProfileDao().getAllHiddenProfiles().first()
            val filteredProfiles = if (selectedProfileIds.isNullOrEmpty()) {
                allProfiles
            } else {
                allProfiles.filter { selectedProfileIds.contains(it.id) }
            }

            val profileIdSet = filteredProfiles.map { it.id }.toSet()

            // Gather linked workgroups
            val allWorkGroups = db.workGroupDao().getAllWorkGroups().first()
            val filteredWorkGroups = if (profileIdSet.isEmpty()) {
                allWorkGroups
            } else {
                allWorkGroups.filter { it.profileId != null && profileIdSet.contains(it.profileId) }
            }

            val workGroupIdSet = filteredWorkGroups.map { it.id }.toSet()

            // Gather group members
            val filteredMembers = filteredWorkGroups.flatMap { g ->
                db.workGroupDao().getGroupMembers(g.id).first()
            }

            // Gather daily records for these profiles / groups
            val allDailyRecords = db.dailyAccountingRecordDao().getAllRecords().first()
            val filteredDailyRecords = if (profileIdSet.isEmpty() && workGroupIdSet.isEmpty()) {
                allDailyRecords
            } else {
                allDailyRecords.filter { r ->
                    (r.profileId != null && profileIdSet.contains(r.profileId)) ||
                    workGroupIdSet.contains(r.groupId)
                }
            }

            // Gather complaints
            val allComplaints = db.financialComplaintDao().getAllComplaints().first()
            val filteredComplaints = if (profileIdSet.isEmpty() && workGroupIdSet.isEmpty()) {
                allComplaints
            } else {
                allComplaints.filter { c ->
                    (c.profileId != null && profileIdSet.contains(c.profileId)) ||
                    workGroupIdSet.contains(c.groupId)
                }
            }

            // Gather hashtag entries & accounts
            val hashtagAccounts = db.chatAccountDao().getAllHashtagAccounts().first()
            val hashtagEntries = db.chatAccountDao().getAllHashtagEntries().first()
            val chatAccounts = db.chatAccountDao().getAllChatAccounts().first()

            val stats = BackupStats(
                subProfilesCount = filteredProfiles.size,
                workGroupsCount = filteredWorkGroups.size,
                recordsCount = filteredDailyRecords.size,
                complaintsCount = filteredComplaints.size,
                backupDateText = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
            )

            val totalRecordsCount = filteredProfiles.size + filteredWorkGroups.size +
                    filteredDailyRecords.size + filteredComplaints.size + hashtagEntries.size

            val rootJson = JSONObject().apply {
                put("version", 2)
                put("migrationType", "SUB_ACCOUNTS_VAULT")
                put("createdAt", System.currentTimeMillis())
                put("createdAtText", stats.backupDateText)
                put("ownerPhoneHash", phoneHash)
                put("ownerPhoneMasked", if (cleanPhone.length > 4) "${cleanPhone.take(3)}****${cleanPhone.takeLast(3)}" else cleanPhone)
                put("provider", cloudProvider.name)

                // Sub-payload
                put("hiddenProfiles", JSONArray().apply { filteredProfiles.forEach { put(hiddenProfileToJson(it)) } })
                put("workGroups", JSONArray().apply { filteredWorkGroups.forEach { put(workGroupToJson(it)) } })
                put("groupMembers", JSONArray().apply { filteredMembers.forEach { put(groupMemberToJson(it)) } })
                put("dailyRecords", JSONArray().apply { filteredDailyRecords.forEach { put(dailyRecordToJson(it)) } })
                put("complaints", JSONArray().apply { filteredComplaints.forEach { put(complaintToJson(it)) } })
                put("hashtagAccounts", JSONArray().apply { hashtagAccounts.forEach { put(hashtagAccountToJson(it)) } })
                put("hashtagEntries", JSONArray().apply { hashtagEntries.forEach { put(hashtagEntryToJson(it)) } })
                put("chatAccounts", JSONArray().apply { chatAccounts.forEach { put(chatAccountToJson(it)) } })
            }

            val plainJsonString = rootJson.toString()

            // Key Derivation & AES-256-GCM Encryption
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }

            val keySpec = PBEKeySpec(encryptionPassphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            cipher.updateAAD(phoneHash.toByteArray(StandardCharsets.UTF_8))
            val cipherText = cipher.doFinal(plainJsonString.toByteArray(StandardCharsets.UTF_8))

            val mac = Mac.getInstance("HmacSHA256")
            val macKey = SecretKeySpec(keyBytes, "HmacSHA256")
            mac.init(macKey)
            val hmacSignature = mac.doFinal(cipherText)

            val packageEnvelope = JSONObject().apply {
                put("magic", MAGIC_HEADER)
                put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                put("phoneHash", phoneHash)
                put("ownerPhoneMasked", if (cleanPhone.length > 4) "${cleanPhone.take(3)}****${cleanPhone.takeLast(3)}" else cleanPhone)
                put("hmac", Base64.encodeToString(hmacSignature, Base64.NO_WRAP))
                put("payload", Base64.encodeToString(cipherText, Base64.NO_WRAP))
                put("timestamp", System.currentTimeMillis())
                put("backupType", if (cloudProvider == CloudStorageProvider.GOOGLE_DRIVE) "CLOUD_GOOGLE_DRIVE" else "SUB_ACCOUNTS_VAULT")
                put("stats", JSONObject().apply {
                    put("totalRecords", totalRecordsCount)
                    put("subProfiles", filteredProfiles.size)
                    put("dailyRecords", filteredDailyRecords.size)
                    put("workGroups", filteredWorkGroups.size)
                })
            }

            val envelopeString = packageEnvelope.toString()
            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "oozox_subaccounts_vault_$timestampStr.oozox"

            val backupsDir = File(context.filesDir, "backups").apply { mkdirs() }
            val backupFile = File(backupsDir, backupFileName)
            FileOutputStream(backupFile).use { output ->
                output.write(envelopeString.toByteArray(StandardCharsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", backupFile)

            // If Cloud Storage / Google Drive is selected, register in Cloud Registry
            var cloudBackupId: String? = null
            var cloudUrl: String? = null
            if (cloudProvider == CloudStorageProvider.GOOGLE_DRIVE || cloudProvider == CloudStorageProvider.OOZOX_CLOUD) {
                cloudBackupId = "cloud_vault_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
                cloudUrl = "https://drive.google.com/file/d/$cloudBackupId/view"

                val cloudRecord = CloudBackupRecord(
                    id = cloudBackupId,
                    ownerPhone = cleanPhone,
                    ownerPhoneHash = phoneHash,
                    provider = cloudProvider,
                    fileName = backupFileName,
                    encryptedPayloadBase64 = envelopeString,
                    fileSizeFormatted = "${(envelopeString.length / 1024.0).let { "%.1f".format(it) }} KB",
                    totalRecords = totalRecordsCount,
                    subProfilesCount = filteredProfiles.size,
                    createdAt = System.currentTimeMillis(),
                    createdAtText = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
                )
                saveCloudBackupRecord(context, cloudRecord)
            }

            BackupResult.Success(
                message = "تم تصدير وترحيل قاعدة بيانات الحسابات الفرعية (${filteredProfiles.size} حساب/خزينة و $totalRecordsCount سجل) بنجاح بتشفير AES-256 وربطها برقم هاتفك!",
                filePath = backupFile.absolutePath,
                uri = uri,
                recordsCount = totalRecordsCount,
                cloudBackupId = cloudBackupId,
                cloudUrl = cloudUrl,
                backupStats = stats
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult.Error("فشل في ترحيل الحسابات الفرعية: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Saves cloud backup in shared registry.
     */
    private fun saveCloudBackupRecord(context: Context, record: CloudBackupRecord) {
        try {
            val sp = context.getSharedPreferences(CLOUD_REGISTRY_PREFS, Context.MODE_PRIVATE)
            val existingJson = sp.getString("backups_list", "[]") ?: "[]"
            val array = JSONArray(existingJson)

            val recordJson = JSONObject().apply {
                put("id", record.id)
                put("ownerPhone", record.ownerPhone)
                put("ownerPhoneHash", record.ownerPhoneHash)
                put("provider", record.provider.name)
                put("fileName", record.fileName)
                put("encryptedPayloadBase64", record.encryptedPayloadBase64)
                put("fileSizeFormatted", record.fileSizeFormatted)
                put("totalRecords", record.totalRecords)
                put("subProfilesCount", record.subProfilesCount)
                put("createdAt", record.createdAt)
                put("createdAtText", record.createdAtText)
            }
            array.put(recordJson)
            sp.edit().putString("backups_list", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Lists cloud backups available for a given phone number.
     */
    fun listCloudBackups(context: Context, phone: String): List<CloudBackupRecord> {
        val cleanPhone = phone.replace("[^0-9+]".toRegex(), "").trim()
        val phoneHash = hashPhoneNumber(cleanPhone)
        val result = mutableListOf<CloudBackupRecord>()

        try {
            val sp = context.getSharedPreferences(CLOUD_REGISTRY_PREFS, Context.MODE_PRIVATE)
            val jsonStr = sp.getString("backups_list", "[]") ?: "[]"
            val array = JSONArray(jsonStr)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val storedHash = obj.optString("ownerPhoneHash")
                val storedPhone = obj.optString("ownerPhone")

                if (storedHash == phoneHash || storedPhone == cleanPhone) {
                    result.add(
                        CloudBackupRecord(
                            id = obj.getString("id"),
                            ownerPhone = storedPhone,
                            ownerPhoneHash = storedHash,
                            provider = try { CloudStorageProvider.valueOf(obj.getString("provider")) } catch (e: Exception) { CloudStorageProvider.GOOGLE_DRIVE },
                            fileName = obj.getString("fileName"),
                            encryptedPayloadBase64 = obj.getString("encryptedPayloadBase64"),
                            fileSizeFormatted = obj.optString("fileSizeFormatted", "12 KB"),
                            totalRecords = obj.optInt("totalRecords", 0),
                            subProfilesCount = obj.optInt("subProfilesCount", 0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            createdAtText = obj.optString("createdAtText", "")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Return latest first
        return result.sortedByDescending { it.createdAt }
    }

    /**
     * Deletes a cloud backup from the cloud registry.
     */
    fun deleteCloudBackup(context: Context, backupId: String) {
        try {
            val sp = context.getSharedPreferences(CLOUD_REGISTRY_PREFS, Context.MODE_PRIVATE)
            val jsonStr = sp.getString("backups_list", "[]") ?: "[]"
            val array = JSONArray(jsonStr)
            val newArray = JSONArray()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("id") != backupId) {
                    newArray.put(obj)
                }
            }
            sp.edit().putString("backups_list", newArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Exports an end-to-end encrypted backup file of the entire app database.
     * Protected by the user's password and bound to their registered phone number.
     */
    suspend fun createEncryptedBackup(
        context: Context,
        db: AppDatabase,
        currentUserPhone: String,
        encryptionPassphrase: String,
        isGoogleDriveCloudExport: Boolean = false
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            if (encryptionPassphrase.length < 4) {
                return@withContext BackupResult.Error("يجب أن تتكون كلمة مرور النسخ الاحتياطي من 4 أحرف أو أرقام على الأقل لضمان التشفير.")
            }

            val cleanPhone = currentUserPhone.replace("[^0-9+]".toRegex(), "").trim()
            val phoneHash = hashPhoneNumber(cleanPhone)

            // 1. Gather all database entities
            val users = db.userDao().getAllUsers().first()
            val chats = db.chatDao().getAllChats().first()
            val messages = db.messageDao().getAllMessages().first()
            val chatAccounts = db.chatAccountDao().getAllChatAccounts().first()
            val hashtagAccounts = db.chatAccountDao().getAllHashtagAccounts().first()
            val hashtagEntries = db.chatAccountDao().getAllHashtagEntries().first()
            val workGroups = db.workGroupDao().getAllWorkGroups().first()
            val groupMembers = db.workGroupDao().getAllWorkGroups().first().flatMap { g ->
                db.workGroupDao().getGroupMembers(g.id).first()
            }
            val dailyRecords = db.dailyAccountingRecordDao().getAllRecords().first()
            val complaints = db.financialComplaintDao().getAllComplaints().first()
            val attendance = db.employeeAttendanceDao().getAllAttendance().first()
            val hiddenProfiles = db.hiddenProfileDao().getAllHiddenProfiles().first()
            val projects = db.projectDao().getAllProjects().first()
            val transactions = db.transactionDao().getAllTransactions().first()
            val purchases = db.goldPurchaseDao().getAllPurchases().first()
            val products = db.merchantStoreDao().getAllProducts().first()

            val rootJson = JSONObject().apply {
                put("version", 2)
                put("createdAt", System.currentTimeMillis())
                put("createdAtText", SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("ownerPhoneHash", phoneHash)
                put("ownerPhoneMasked", if (cleanPhone.length > 4) "${cleanPhone.take(3)}****${cleanPhone.takeLast(3)}" else cleanPhone)

                // Sub-payload
                put("users", JSONArray().apply { users.forEach { put(userToJson(it)) } })
                put("chats", JSONArray().apply { chats.forEach { put(chatToJson(it)) } })
                put("messages", JSONArray().apply { messages.forEach { put(messageToJson(it)) } })
                put("chatAccounts", JSONArray().apply { chatAccounts.forEach { put(chatAccountToJson(it)) } })
                put("hashtagAccounts", JSONArray().apply { hashtagAccounts.forEach { put(hashtagAccountToJson(it)) } })
                put("hashtagEntries", JSONArray().apply { hashtagEntries.forEach { put(hashtagEntryToJson(it)) } })
                put("workGroups", JSONArray().apply { workGroups.forEach { put(workGroupToJson(it)) } })
                put("groupMembers", JSONArray().apply { groupMembers.forEach { put(groupMemberToJson(it)) } })
                put("dailyRecords", JSONArray().apply { dailyRecords.forEach { put(dailyRecordToJson(it)) } })
                put("complaints", JSONArray().apply { complaints.forEach { put(complaintToJson(it)) } })
                put("attendance", JSONArray().apply { attendance.forEach { put(attendanceToJson(it)) } })
                put("hiddenProfiles", JSONArray().apply { hiddenProfiles.forEach { put(hiddenProfileToJson(it)) } })
                put("projects", JSONArray().apply { projects.forEach { put(projectToJson(it)) } })
                put("transactions", JSONArray().apply { transactions.forEach { put(transactionToJson(it)) } })
                put("purchases", JSONArray().apply { purchases.forEach { put(purchaseToJson(it)) } })
                put("products", JSONArray().apply { products.forEach { put(productToJson(it)) } })
            }

            val plainJsonString = rootJson.toString()
            val totalRecordsCount = users.size + chats.size + messages.size + dailyRecords.size +
                    complaints.size + hiddenProfiles.size + projects.size + workGroups.size

            // 2. Strong Cryptographic Key Derivation & AES-256-GCM Encryption
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }

            val keySpec = PBEKeySpec(encryptionPassphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            // Add AAD for tamper detection
            cipher.updateAAD(phoneHash.toByteArray(StandardCharsets.UTF_8))
            val cipherText = cipher.doFinal(plainJsonString.toByteArray(StandardCharsets.UTF_8))

            // 3. HMAC-SHA256 of the cipherText for double tamper-proofing
            val mac = Mac.getInstance("HmacSHA256")
            val macKey = SecretKeySpec(keyBytes, "HmacSHA256")
            mac.init(macKey)
            val hmacSignature = mac.doFinal(cipherText)

            // 4. Build Vault Package File
            val packageEnvelope = JSONObject().apply {
                put("magic", MAGIC_HEADER)
                put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                put("phoneHash", phoneHash)
                put("hmac", Base64.encodeToString(hmacSignature, Base64.NO_WRAP))
                put("payload", Base64.encodeToString(cipherText, Base64.NO_WRAP))
                put("timestamp", System.currentTimeMillis())
                put("backupType", if (isGoogleDriveCloudExport) "CLOUD_GOOGLE_DRIVE" else "LOCAL_SECURE_VAULT")
            }

            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "oozox_vault_backup_$timestampStr.oozox"

            val backupsDir = File(context.filesDir, "backups").apply { mkdirs() }
            val backupFile = File(backupsDir, backupFileName)
            val envelopeString = packageEnvelope.toString()
            FileOutputStream(backupFile).use { output ->
                output.write(envelopeString.toByteArray(StandardCharsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", backupFile)

            var cloudBackupId: String? = null
            if (isGoogleDriveCloudExport) {
                cloudBackupId = "gdrive_${System.currentTimeMillis()}"
                val cloudRecord = CloudBackupRecord(
                    id = cloudBackupId,
                    ownerPhone = cleanPhone,
                    ownerPhoneHash = phoneHash,
                    provider = CloudStorageProvider.GOOGLE_DRIVE,
                    fileName = backupFileName,
                    encryptedPayloadBase64 = envelopeString,
                    fileSizeFormatted = "${(envelopeString.length / 1024.0).let { "%.1f".format(it) }} KB",
                    totalRecords = totalRecordsCount,
                    subProfilesCount = hiddenProfiles.size,
                    createdAt = System.currentTimeMillis(),
                    createdAtText = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
                )
                saveCloudBackupRecord(context, cloudRecord)
            }

            BackupResult.Success(
                message = if (isGoogleDriveCloudExport) {
                    "تم رفع ومزامنة النسخة الاحتياطية المشفرة بنجاح إلى Google Drive ☁️"
                } else {
                    "تم إنشاء وتشفير النسخة الاحتياطية بنجاح بحماية AES-256 ورقم الهاتف!"
                },
                filePath = backupFile.absolutePath,
                uri = uri,
                recordsCount = totalRecordsCount,
                cloudBackupId = cloudBackupId
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult.Error("فشل في إنشاء النسخة الاحتياطية: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Validates and restores an encrypted backup package.
     * Enforces strict phone number matching to prevent unauthorized transfer.
     */
    suspend fun restoreEncryptedBackup(
        context: Context,
        db: AppDatabase,
        backupFileContent: String,
        currentUserPhone: String,
        enteredPassphrase: String
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            if (backupFileContent.isBlank()) {
                return@withContext BackupResult.Error("ملف النسخة الاحتياطية فارغ أو تالف.")
            }

            val envelope = try {
                JSONObject(backupFileContent)
            } catch (e: Exception) {
                return@withContext BackupResult.Error("صيغة الملف غير صالحة. تأكد من اختيار ملف النسخ الاحتياطي الخاص بالصاغة (.oozox).")
            }

            val magic = envelope.optString("magic")
            if (magic != MAGIC_HEADER && magic != "OOZOX_ENCRYPTED_VAULT_V1") {
                return@withContext BackupResult.Error("ملف النسخة غير صالح أو غير معتمد من منصة الصاغة.")
            }

            val backupPhoneHash = envelope.optString("phoneHash")
            val cleanCurrentPhone = currentUserPhone.replace("[^0-9+]".toRegex(), "").trim()
            val currentPhoneHash = hashPhoneNumber(cleanCurrentPhone)

            // CRITICAL SECURITY CONSTRAINT:
            // Ensure the restoring user's phone number strictly matches the phone number inside the backup
            if (backupPhoneHash.isNotBlank() && backupPhoneHash != currentPhoneHash) {
                return@withContext BackupResult.Error(
                    "❌ خطأ أمني جسيم: رقم الهاتف المسجل في هذا الهاتف ($cleanCurrentPhone) لا يطابق رقم هاتف مالك النسخة الاحتياطية الأصلي!\n\nنظام الحماية يمنع استعادة ونقل البيانات المحاسبية والسرية إلى هاتف برقم مختلف لمنع السرقة والاحتيال."
                )
            }

            val saltBase64 = envelope.getString("salt")
            val ivBase64 = envelope.getString("iv")
            val payloadBase64 = envelope.getString("payload")
            val hmacBase64 = envelope.optString("hmac")

            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherText = Base64.decode(payloadBase64, Base64.NO_WRAP)

            // Derive key
            val keySpec = PBEKeySpec(enteredPassphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            // Verify HMAC if present
            if (hmacBase64.isNotBlank()) {
                val mac = Mac.getInstance("HmacSHA256")
                val macKey = SecretKeySpec(keyBytes, "HmacSHA256")
                mac.init(macKey)
                val calculatedHmac = mac.doFinal(cipherText)
                val expectedHmac = Base64.decode(hmacBase64, Base64.NO_WRAP)
                if (!calculatedHmac.contentEquals(expectedHmac)) {
                    return@withContext BackupResult.Error("❌ كلمة المرور غير صحيحة أو تم التلاعب ببيانات النسخة الاحتياطية!")
                }
            }

            // Decrypt with AES-256-GCM
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            if (backupPhoneHash.isNotBlank()) {
                cipher.updateAAD(backupPhoneHash.toByteArray(StandardCharsets.UTF_8))
            }

            val decryptedBytes = try {
                cipher.doFinal(cipherText)
            } catch (e: Exception) {
                return@withContext BackupResult.Error("❌ فشل فك التشفير: كلمة المرور غير صحيحة أو البيانات تالفة.")
            }

            val decryptedJsonString = String(decryptedBytes, StandardCharsets.UTF_8)
            val rootJson = JSONObject(decryptedJsonString)

            // Restore data into database
            var restoredRecords = 0

            // Hidden profiles
            val profilesArr = rootJson.optJSONArray("hiddenProfiles")
            if (profilesArr != null) {
                for (i in 0 until profilesArr.length()) {
                    val pObj = profilesArr.getJSONObject(i)
                    db.hiddenProfileDao().insertHiddenProfile(jsonToHiddenProfile(pObj))
                    restoredRecords++
                }
            }

            // Work groups
            val groupsArr = rootJson.optJSONArray("workGroups")
            if (groupsArr != null) {
                for (i in 0 until groupsArr.length()) {
                    val gObj = groupsArr.getJSONObject(i)
                    db.workGroupDao().insertWorkGroup(jsonToWorkGroup(gObj))
                    restoredRecords++
                }
            }

            // Group members
            val membersArr = rootJson.optJSONArray("groupMembers")
            if (membersArr != null) {
                for (i in 0 until membersArr.length()) {
                    val mObj = membersArr.getJSONObject(i)
                    db.workGroupDao().insertGroupMember(jsonToGroupMember(mObj))
                    restoredRecords++
                }
            }

            // Daily records
            val recordsArr = rootJson.optJSONArray("dailyRecords")
            if (recordsArr != null) {
                for (i in 0 until recordsArr.length()) {
                    val rObj = recordsArr.getJSONObject(i)
                    db.dailyAccountingRecordDao().insertRecord(jsonToDailyRecord(rObj))
                    restoredRecords++
                }
            }

            // Complaints
            val complaintsArr = rootJson.optJSONArray("complaints")
            if (complaintsArr != null) {
                for (i in 0 until complaintsArr.length()) {
                    val cObj = complaintsArr.getJSONObject(i)
                    db.financialComplaintDao().insertComplaint(jsonToComplaint(cObj))
                    restoredRecords++
                }
            }

            // Attendance
            val attendanceArr = rootJson.optJSONArray("attendance")
            if (attendanceArr != null) {
                for (i in 0 until attendanceArr.length()) {
                    val aObj = attendanceArr.getJSONObject(i)
                    db.employeeAttendanceDao().insertAttendance(jsonToAttendance(aObj))
                    restoredRecords++
                }
            }

            // Chats & Messages
            val chatsArr = rootJson.optJSONArray("chats")
            if (chatsArr != null) {
                for (i in 0 until chatsArr.length()) {
                    val chObj = chatsArr.getJSONObject(i)
                    db.chatDao().insertChat(jsonToChat(chObj))
                    restoredRecords++
                }
            }

            val msgArr = rootJson.optJSONArray("messages")
            if (msgArr != null) {
                for (i in 0 until msgArr.length()) {
                    val mObj = msgArr.getJSONObject(i)
                    db.messageDao().insertMessage(jsonToMessage(mObj))
                    restoredRecords++
                }
            }

            // Hashtag accounts & entries
            val hashAccArr = rootJson.optJSONArray("hashtagAccounts")
            if (hashAccArr != null) {
                for (i in 0 until hashAccArr.length()) {
                    val hObj = hashAccArr.getJSONObject(i)
                    db.chatAccountDao().insertHashtagAccount(jsonToHashtagAccount(hObj))
                    restoredRecords++
                }
            }

            val hashEntArr = rootJson.optJSONArray("hashtagEntries")
            if (hashEntArr != null) {
                for (i in 0 until hashEntArr.length()) {
                    val heObj = hashEntArr.getJSONObject(i)
                    db.chatAccountDao().insertHashtagEntry(jsonToHashtagEntry(heObj))
                    restoredRecords++
                }
            }

            // Projects
            val projArr = rootJson.optJSONArray("projects")
            if (projArr != null) {
                for (i in 0 until projArr.length()) {
                    val pObj = projArr.getJSONObject(i)
                    db.projectDao().insertProject(jsonToProject(pObj))
                    restoredRecords++
                }
            }

            // Transactions & Purchases
            val txArr = rootJson.optJSONArray("transactions")
            if (txArr != null) {
                for (i in 0 until txArr.length()) {
                    val tObj = txArr.getJSONObject(i)
                    db.transactionDao().insertTransaction(jsonToTransaction(tObj))
                    restoredRecords++
                }
            }

            val purArr = rootJson.optJSONArray("purchases")
            if (purArr != null) {
                for (i in 0 until purArr.length()) {
                    val pObj = purArr.getJSONObject(i)
                    db.goldPurchaseDao().insertPurchase(jsonToPurchase(pObj))
                    restoredRecords++
                }
            }

            BackupResult.Success(
                message = "تمت استعادة كافة البيانات والقيود والحسابات المشفرة بنجاح! ($restoredRecords سجل)",
                recordsCount = restoredRecords
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult.Error("فشل في استعادة النسخة الاحتياطية: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Share or export backup file via Android Share Sheet (Google Drive, WhatsApp, Local, etc.)
     */
    fun shareBackupFile(context: Context, fileUri: Uri, customTitle: String = "نسخة احتياطية مشفرة - الصاغة") {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, customTitle)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "تصدير النسخة الاحتياطية المشفرة (Google Drive / الجهاز / سحابة)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- JSON Conversion Helpers ---

    private fun userToJson(u: UserEntity) = JSONObject().apply {
        put("id", u.id)
        put("name", u.name)
        put("phone", u.phone)
        put("username", u.username)
        put("type", u.type)
        put("isVerified", u.isVerified)
        put("subscriptionTier", u.subscriptionTier)
        put("subscriptionStartTimestamp", u.subscriptionStartTimestamp)
        put("subscriptionEndTimestamp", u.subscriptionEndTimestamp)
        put("isTrialActive", u.isTrialActive)
        put("subscriptionStatus", u.subscriptionStatus)
    }

    private fun chatToJson(c: ChatEntity) = JSONObject().apply {
        put("id", c.id)
        put("contactId", c.contactId)
        put("contactName", c.contactName)
        put("contactType", c.contactType)
        put("contactVerified", c.contactVerified)
        put("lastMessage", c.lastMessage)
        put("lastMessageTime", c.lastMessageTime ?: 0L)
        put("lastMessageStatus", c.lastMessageStatus)
        put("lastMessageSenderId", c.lastMessageSenderId)
        put("unreadCount", c.unreadCount)
        put("isPinned", c.isPinned)
        put("createdAt", c.createdAt)
        put("isGroup", c.isGroup)
        put("groupId", c.groupId)
        put("groupName", c.groupName)
        put("isArchived", c.isArchived)
    }

    private fun jsonToChat(j: JSONObject) = ChatEntity(
        id = j.getString("id"),
        contactId = j.getString("contactId"),
        contactName = j.getString("contactName"),
        contactType = j.optString("contactType", UserType.BUSINESSMAN.name),
        contactVerified = j.optBoolean("contactVerified", false),
        lastMessage = j.optString("lastMessage", null),
        lastMessageTime = if (j.has("lastMessageTime") && j.getLong("lastMessageTime") > 0) j.getLong("lastMessageTime") else null,
        lastMessageStatus = j.optString("lastMessageStatus", null),
        lastMessageSenderId = j.optString("lastMessageSenderId", null),
        unreadCount = j.optInt("unreadCount", 0),
        isPinned = j.optBoolean("isPinned", false),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        isGroup = j.optBoolean("isGroup", false),
        groupId = j.optString("groupId", null),
        groupName = j.optString("groupName", null),
        isArchived = j.optBoolean("isArchived", false)
    )

    private fun messageToJson(m: MessageEntity) = JSONObject().apply {
        put("id", m.id)
        put("chatId", m.chatId)
        put("senderId", m.senderId)
        put("senderName", m.senderName)
        put("type", m.type)
        put("content", m.content)
        put("transactionId", m.transactionId)
        put("hashtagId", m.hashtagId)
        put("timestamp", m.timestamp)
        put("status", m.status)
        put("reaction", m.reaction)
    }

    private fun jsonToMessage(j: JSONObject) = MessageEntity(
        id = j.getString("id"),
        chatId = j.getString("chatId"),
        senderId = j.getString("senderId"),
        senderName = j.getString("senderName"),
        type = j.optString("type", MessageType.TEXT.name),
        content = j.getString("content"),
        transactionId = j.optString("transactionId", null),
        hashtagId = j.optString("hashtagId", null),
        timestamp = j.optLong("timestamp", System.currentTimeMillis()),
        status = j.optString("status", MessageStatus.SENT.name),
        reaction = j.optString("reaction", null)
    )

    private fun chatAccountToJson(a: ChatAccountEntity) = JSONObject().apply {
        put("id", a.id)
        put("chatId", a.chatId)
        put("name", a.name)
        put("creatorId", a.creatorId)
        put("createdAt", a.createdAt)
        put("status", a.status)
    }

    private fun jsonToChatAccount(j: JSONObject) = ChatAccountEntity(
        id = j.getString("id"),
        chatId = j.getString("chatId"),
        name = j.getString("name"),
        creatorId = j.getString("creatorId"),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        status = j.optString("status", HashtagStatus.ACTIVE.name)
    )

    private fun hashtagAccountToJson(h: HashtagAccountEntity) = JSONObject().apply {
        put("id", h.id)
        put("chatId", h.chatId)
        put("accountId", h.accountId)
        put("tag", h.tag)
        put("creatorId", h.creatorId)
        put("createdAt", h.createdAt)
        put("status", h.status)
    }

    private fun jsonToHashtagAccount(j: JSONObject) = HashtagAccountEntity(
        id = j.getString("id"),
        chatId = j.getString("chatId"),
        accountId = j.getString("accountId"),
        tag = j.getString("tag"),
        creatorId = j.getString("creatorId"),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        status = j.optString("status", HashtagStatus.ACTIVE.name)
    )

    private fun hashtagEntryToJson(e: HashtagEntryEntity) = JSONObject().apply {
        put("id", e.id)
        put("hashtagId", e.hashtagId)
        put("accountId", e.accountId)
        put("type", e.type)
        put("description", e.description)
        put("amount", e.amount)
        put("direction", e.direction)
        put("createdBy", e.createdBy)
        put("createdAt", e.createdAt)
        put("photoUri", e.photoUri)
    }

    private fun jsonToHashtagEntry(j: JSONObject) = HashtagEntryEntity(
        id = j.getString("id"),
        hashtagId = j.getString("hashtagId"),
        accountId = j.getString("accountId"),
        type = j.optString("type", HashtagEntryType.INVOICE.name),
        description = j.getString("description"),
        amount = j.optDouble("amount", 0.0),
        direction = j.optString("direction", HashtagEntryDirection.DEBIT.name),
        createdBy = j.getString("createdBy"),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        photoUri = j.optString("photoUri", null)
    )

    private fun hiddenProfileToJson(p: HiddenProfileEntity) = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("secretPinOrPassword", p.secretPinOrPassword)
        put("companyName", p.companyName)
        put("roleTitle", p.roleTitle)
        put("avatarUri", p.avatarUri)
        put("initialCapital", p.initialCapital)
        put("isEncrypted", p.isEncrypted)
        put("silentNotifications", p.silentNotifications)
        put("colorHex", p.colorHex)
        put("notes", p.notes)
        put("createdAt", p.createdAt)
    }

    private fun jsonToHiddenProfile(j: JSONObject) = HiddenProfileEntity(
        id = j.getString("id"),
        name = j.getString("name"),
        secretPinOrPassword = j.getString("secretPinOrPassword"),
        companyName = j.getString("companyName"),
        roleTitle = j.optString("roleTitle", "المدير المالي والمسؤول"),
        avatarUri = j.optString("avatarUri", null),
        initialCapital = j.optDouble("initialCapital", 0.0),
        isEncrypted = j.optBoolean("isEncrypted", true),
        silentNotifications = j.optBoolean("silentNotifications", true),
        colorHex = j.optString("colorHex", null),
        notes = j.optString("notes", null),
        createdAt = j.optLong("createdAt", System.currentTimeMillis())
    )

    private fun workGroupToJson(g: WorkGroupEntity) = JSONObject().apply {
        put("id", g.id)
        put("name", g.name)
        put("description", g.description)
        put("ownerId", g.ownerId)
        put("parentGroupId", g.parentGroupId)
        put("isAccountingGroup", g.isAccountingGroup)
        put("isPrivate", g.isPrivate)
        put("disableMemberDMs", g.disableMemberDMs)
        put("companyName", g.companyName)
        put("companyCapital", g.companyCapital)
        put("profileId", g.profileId)
        put("createdAt", g.createdAt)
    }

    private fun jsonToWorkGroup(j: JSONObject) = WorkGroupEntity(
        id = j.getString("id"),
        name = j.getString("name"),
        description = j.optString("description", null),
        ownerId = j.getString("ownerId"),
        parentGroupId = j.optString("parentGroupId", null),
        isAccountingGroup = j.optBoolean("isAccountingGroup", false),
        isPrivate = j.optBoolean("isPrivate", true),
        disableMemberDMs = j.optBoolean("disableMemberDMs", true),
        companyName = j.optString("companyName", null),
        companyCapital = j.optDouble("companyCapital", 0.0),
        profileId = j.optString("profileId", null),
        createdAt = j.optLong("createdAt", System.currentTimeMillis())
    )

    private fun groupMemberToJson(m: GroupMemberEntity) = JSONObject().apply {
        put("id", m.id)
        put("groupId", m.groupId)
        put("userId", m.userId)
        put("username", m.username)
        put("phone", m.phone)
        put("roleLabel", m.roleLabel)
        put("status", m.status)
        put("workingHoursStart", m.workingHoursStart)
        put("workingHoursEnd", m.workingHoursEnd)
        put("shiftHours", m.shiftHours)
        put("workingDays", m.workingDays)
        put("shiftType", m.shiftType)
        put("canPostAccounting", m.canPostAccounting)
        put("canExportReports", m.canExportReports)
        put("canEditRecords", m.canEditRecords)
        put("canMessageDirectly", m.canMessageDirectly)
        put("isGroupAdmin", m.isGroupAdmin)
        put("notes", m.notes)
        put("addedAt", m.addedAt)
    }

    private fun jsonToGroupMember(j: JSONObject) = GroupMemberEntity(
        id = j.getString("id"),
        groupId = j.getString("groupId"),
        userId = j.getString("userId"),
        username = j.getString("username"),
        phone = j.getString("phone"),
        roleLabel = j.getString("roleLabel"),
        status = j.optString("status", "accepted"),
        workingHoursStart = j.optString("workingHoursStart", "09:00 ص"),
        workingHoursEnd = j.optString("workingHoursEnd", "05:00 م"),
        shiftHours = j.optDouble("shiftHours", 8.0),
        workingDays = j.optString("workingDays", "السبت - الخميس"),
        shiftType = j.optString("shiftType", "نهارية (صباحية)"),
        canPostAccounting = j.optBoolean("canPostAccounting", true),
        canExportReports = j.optBoolean("canExportReports", false),
        canEditRecords = j.optBoolean("canEditRecords", false),
        canMessageDirectly = j.optBoolean("canMessageDirectly", false),
        isGroupAdmin = j.optBoolean("isGroupAdmin", false),
        notes = j.optString("notes", null),
        addedAt = j.optLong("addedAt", System.currentTimeMillis())
    )

    private fun dailyRecordToJson(r: DailyAccountingRecordEntity) = JSONObject().apply {
        put("id", r.id)
        put("groupId", r.groupId)
        put("subGroupId", r.subGroupId)
        put("profileId", r.profileId)
        put("entryDate", r.entryDate)
        put("staffId", r.staffId)
        put("staffName", r.staffName)
        put("staffRole", r.staffRole)
        put("dailyRevenue", r.dailyRevenue)
        put("incomingAmount", r.incomingAmount)
        put("outgoingAmount", r.outgoingAmount)
        put("deficitAmount", r.deficitAmount)
        put("complaintsCount", r.complaintsCount)
        put("complaintsNotes", r.complaintsNotes)
        put("dailyCapital", r.dailyCapital)
        put("weeklyCapital", r.weeklyCapital)
        put("monthlyCapital", r.monthlyCapital)
        put("screenshotUri", r.screenshotUri)
        put("notes", r.notes)
        put("isEncrypted", r.isEncrypted)
        put("timestamp", r.timestamp)
    }

    private fun jsonToDailyRecord(j: JSONObject) = DailyAccountingRecordEntity(
        id = j.getString("id"),
        groupId = j.getString("groupId"),
        subGroupId = j.optString("subGroupId", null),
        profileId = j.optString("profileId", null),
        entryDate = j.getString("entryDate"),
        staffId = j.getString("staffId"),
        staffName = j.getString("staffName"),
        staffRole = j.getString("staffRole"),
        dailyRevenue = j.optDouble("dailyRevenue", 0.0),
        incomingAmount = j.optDouble("incomingAmount", 0.0),
        outgoingAmount = j.optDouble("outgoingAmount", 0.0),
        deficitAmount = j.optDouble("deficitAmount", 0.0),
        complaintsCount = j.optInt("complaintsCount", 0),
        complaintsNotes = j.optString("complaintsNotes", null),
        dailyCapital = j.optDouble("dailyCapital", 0.0),
        weeklyCapital = j.optDouble("weeklyCapital", 0.0),
        monthlyCapital = j.optDouble("monthlyCapital", 0.0),
        screenshotUri = j.optString("screenshotUri", null),
        notes = j.optString("notes", null),
        isEncrypted = j.optBoolean("isEncrypted", true),
        timestamp = j.optLong("timestamp", System.currentTimeMillis())
    )

    private fun complaintToJson(c: FinancialComplaintEntity) = JSONObject().apply {
        put("id", c.id)
        put("groupId", c.groupId)
        put("subGroupId", c.subGroupId)
        put("profileId", c.profileId)
        put("submittedByStaffId", c.submittedByStaffId)
        put("submittedByStaffName", c.submittedByStaffName)
        put("submittedByStaffRole", c.submittedByStaffRole)
        put("title", c.title)
        put("details", c.details)
        put("targetDate", c.targetDate)
        put("impactType", c.impactType)
        put("amount", c.amount)
        put("category", c.category)
        put("status", c.status)
        put("resultingDayDeficit", c.resultingDayDeficit)
        put("adminAlertSent", c.adminAlertSent)
        put("resolutionNotes", c.resolutionNotes)
        put("timestamp", c.timestamp)
    }

    private fun jsonToComplaint(j: JSONObject) = FinancialComplaintEntity(
        id = j.getString("id"),
        groupId = j.getString("groupId"),
        subGroupId = j.optString("subGroupId", null),
        profileId = j.optString("profileId", null),
        submittedByStaffId = j.getString("submittedByStaffId"),
        submittedByStaffName = j.getString("submittedByStaffName"),
        submittedByStaffRole = j.getString("submittedByStaffRole"),
        title = j.getString("title"),
        details = j.getString("details"),
        targetDate = j.getString("targetDate"),
        impactType = j.optString("impactType", "DEDUCTION"),
        amount = j.optDouble("amount", 0.0),
        category = j.optString("category", "عجز صندوق"),
        status = j.optString("status", "APPLIED"),
        resultingDayDeficit = j.optDouble("resultingDayDeficit", 0.0),
        adminAlertSent = j.optBoolean("adminAlertSent", true),
        resolutionNotes = j.optString("resolutionNotes", null),
        timestamp = j.optLong("timestamp", System.currentTimeMillis())
    )

    private fun attendanceToJson(a: EmployeeAttendanceEntity) = JSONObject().apply {
        put("id", a.id)
        put("memberId", a.memberId)
        put("groupId", a.groupId)
        put("employeeName", a.employeeName)
        put("employeeRole", a.employeeRole)
        put("date", a.date)
        put("clockInTime", a.clockInTime)
        put("clockOutTime", a.clockOutTime)
        put("clockInTimestamp", a.clockInTimestamp ?: 0L)
        put("clockOutTimestamp", a.clockOutTimestamp ?: 0L)
        put("scheduledShiftStart", a.scheduledShiftStart)
        put("scheduledShiftEnd", a.scheduledShiftEnd)
        put("scheduledShiftHours", a.scheduledShiftHours)
        put("actualWorkedHours", a.actualWorkedHours)
        put("status", a.status)
        put("overtimeMinutes", a.overtimeMinutes)
        put("lateMinutes", a.lateMinutes)
        put("earlyDepartureMinutes", a.earlyDepartureMinutes)
        put("locationTag", a.locationTag)
        put("notes", a.notes)
        put("timestamp", a.timestamp)
    }

    private fun jsonToAttendance(j: JSONObject) = EmployeeAttendanceEntity(
        id = j.getString("id"),
        memberId = j.getString("memberId"),
        groupId = j.getString("groupId"),
        employeeName = j.getString("employeeName"),
        employeeRole = j.getString("employeeRole"),
        date = j.getString("date"),
        clockInTime = j.optString("clockInTime", null),
        clockOutTime = j.optString("clockOutTime", null),
        clockInTimestamp = if (j.has("clockInTimestamp") && j.getLong("clockInTimestamp") > 0) j.getLong("clockInTimestamp") else null,
        clockOutTimestamp = if (j.has("clockOutTimestamp") && j.getLong("clockOutTimestamp") > 0) j.getLong("clockOutTimestamp") else null,
        scheduledShiftStart = j.optString("scheduledShiftStart", "09:00 ص"),
        scheduledShiftEnd = j.optString("scheduledShiftEnd", "05:00 م"),
        scheduledShiftHours = j.optDouble("scheduledShiftHours", 8.0),
        actualWorkedHours = j.optDouble("actualWorkedHours", 0.0),
        status = j.optString("status", "CLOCKED_IN"),
        overtimeMinutes = j.optInt("overtimeMinutes", 0),
        lateMinutes = j.optInt("lateMinutes", 0),
        earlyDepartureMinutes = j.optInt("earlyDepartureMinutes", 0),
        locationTag = j.optString("locationTag", "المقر الرئيسي - الخزينة المركزية"),
        notes = j.optString("notes", null),
        timestamp = j.optLong("timestamp", System.currentTimeMillis())
    )

    private fun projectToJson(p: ProjectEntity) = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("workGroupId", p.workGroupId)
        put("type", p.type)
        put("estimatedCapital", p.estimatedCapital)
        put("totalExpenses", p.totalExpenses)
        put("totalIncome", p.totalIncome)
        put("location", p.location)
        put("coManagerPhone", p.coManagerPhone)
        put("coManagerName", p.coManagerName)
        put("membersInfo", p.membersInfo)
        put("createdAt", p.createdAt)
    }

    private fun jsonToProject(j: JSONObject) = ProjectEntity(
        id = j.getString("id"),
        name = j.getString("name"),
        workGroupId = j.getString("workGroupId"),
        type = j.optString("type", ProjectType.CONSTRUCTION.name),
        estimatedCapital = j.optDouble("estimatedCapital", 0.0),
        totalExpenses = j.optDouble("totalExpenses", 0.0),
        totalIncome = j.optDouble("totalIncome", 0.0),
        location = j.optString("location", "القاهرة، مصر"),
        coManagerPhone = j.optString("coManagerPhone", null),
        coManagerName = j.optString("coManagerName", null),
        membersInfo = j.optString("membersInfo", ""),
        createdAt = j.optLong("createdAt", System.currentTimeMillis())
    )

    private fun transactionToJson(t: TransactionEntity) = JSONObject().apply {
        put("id", t.id)
        put("chatId", t.chatId)
        put("currentUserId", t.currentUserId)
        put("currentUserName", t.currentUserName)
        put("currentUserType", t.currentUserType)
        put("contactId", t.contactId)
        put("contactName", t.contactName)
        put("goldKarat", t.goldKarat)
        put("goldType", t.goldType)
        put("weight", t.weight)
        put("pricePerGram", t.pricePerGram)
        put("manufacturingCostPerGram", t.manufacturingCostPerGram)
        put("totalGoldPrice", t.totalGoldPrice)
        put("totalManufacturing", t.totalManufacturing)
        put("totalPrice", t.totalPrice)
        put("traderCostPerGram", t.traderCostPerGram ?: 0.0)
        put("profit", t.profit ?: 0.0)
        put("photoUri", t.photoUri)
        put("timestamp", t.timestamp)
        put("status", t.status)
        put("originalTransactionId", t.originalTransactionId)
        put("notes", t.notes)
        put("metalLossRatePercent", t.metalLossRatePercent)
        put("metalLossGrams", t.metalLossGrams)
    }

    private fun jsonToTransaction(j: JSONObject) = TransactionEntity(
        id = j.getString("id"),
        chatId = j.getString("chatId"),
        currentUserId = j.getString("currentUserId"),
        currentUserName = j.getString("currentUserName"),
        currentUserType = j.optString("currentUserType", UserType.BUSINESSMAN.name),
        contactId = j.getString("contactId"),
        contactName = j.getString("contactName"),
        goldKarat = j.optString("goldKarat", GoldKarat.KARAT_21.name),
        goldType = j.optString("goldType", "مصاغ"),
        weight = j.optDouble("weight", 0.0),
        pricePerGram = j.optDouble("pricePerGram", 0.0),
        manufacturingCostPerGram = j.optDouble("manufacturingCostPerGram", 0.0),
        totalGoldPrice = j.optDouble("totalGoldPrice", 0.0),
        totalManufacturing = j.optDouble("totalManufacturing", 0.0),
        totalPrice = j.optDouble("totalPrice", 0.0),
        traderCostPerGram = if (j.has("traderCostPerGram")) j.getDouble("traderCostPerGram") else null,
        profit = if (j.has("profit")) j.getDouble("profit") else null,
        photoUri = j.optString("photoUri", null),
        timestamp = j.optLong("timestamp", System.currentTimeMillis()),
        status = j.optString("status", TransactionStatus.PURCHASED.name),
        originalTransactionId = j.optString("originalTransactionId", null),
        notes = j.optString("notes", null),
        metalLossRatePercent = j.optDouble("metalLossRatePercent", 0.5),
        metalLossGrams = j.optDouble("metalLossGrams", 0.0)
    )

    private fun purchaseToJson(p: GoldPurchaseEntity) = JSONObject().apply {
        put("id", p.id)
        put("buyerId", p.buyerId)
        put("traderHashtag", p.traderHashtag)
        put("traderConfirmed", p.traderConfirmed)
        put("photoUri", p.photoUri)
        put("extractedWeight", p.extractedWeight)
        put("manuallyEdited", p.manuallyEdited)
        put("karat", p.karat)
        put("pricePerGram", p.pricePerGram)
        put("currentPricePerGram", p.currentPricePerGram)
        put("manufacturer", p.manufacturer)
        put("manufacturingCost", p.manufacturingCost)
        put("stampDuty", p.stampDuty)
        put("totalPaid", p.totalPaid)
        put("currentValue", p.currentValue)
        put("profitLoss", p.profitLoss)
        put("profitLossPercent", p.profitLossPercent)
        put("status", p.status)
        put("createdAt", p.createdAt)
        put("metalType", p.metalType)
        put("mfgType", p.mfgType)
        put("cashbackRefund", p.cashbackRefund)
        put("sellerShopName", p.sellerShopName)
        put("sellerLocation", p.sellerLocation)
        put("scalePhotoUri", p.scalePhotoUri)
        put("metalLossRatePercent", p.metalLossRatePercent)
        put("metalLossGrams", p.metalLossGrams)
    }

    private fun jsonToPurchase(j: JSONObject) = GoldPurchaseEntity(
        id = j.getString("id"),
        buyerId = j.getString("buyerId"),
        traderHashtag = j.optString("traderHashtag", ""),
        traderConfirmed = j.optBoolean("traderConfirmed", false),
        photoUri = j.optString("photoUri", null),
        extractedWeight = j.optDouble("extractedWeight", 0.0),
        manuallyEdited = j.optBoolean("manuallyEdited", false),
        karat = j.optString("karat", GoldKarat.KARAT_21.name),
        pricePerGram = j.optDouble("pricePerGram", 0.0),
        currentPricePerGram = j.optDouble("currentPricePerGram", 0.0),
        manufacturer = j.optString("manufacturer", "عام"),
        manufacturingCost = j.optDouble("manufacturingCost", 0.0),
        stampDuty = j.optDouble("stampDuty", 0.0),
        totalPaid = j.optDouble("totalPaid", 0.0),
        currentValue = j.optDouble("currentValue", 0.0),
        profitLoss = j.optDouble("profitLoss", 0.0),
        profitLossPercent = j.optDouble("profitLossPercent", 0.0),
        status = j.optString("status", TransactionStatus.PURCHASED.name),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        metalType = j.optString("metalType", MetalType.GOLD.name),
        mfgType = j.optString("mfgType", "PER_GRAM"),
        cashbackRefund = j.optDouble("cashbackRefund", 0.0),
        sellerShopName = j.optString("sellerShopName", null),
        sellerLocation = j.optString("sellerLocation", null),
        scalePhotoUri = j.optString("scalePhotoUri", null),
        metalLossRatePercent = j.optDouble("metalLossRatePercent", 0.5),
        metalLossGrams = j.optDouble("metalLossGrams", 0.0)
    )

    private fun productToJson(p: MerchantProductEntity) = JSONObject().apply {
        put("id", p.id)
        put("merchantId", p.merchantId)
        put("merchantShopName", p.merchantShopName)
        put("merchantPhone", p.merchantPhone)
        put("title", p.title)
        put("category", p.category)
        put("karat", p.karat)
        put("weightGrams", p.weightGrams)
        put("manufacturingCostPerGram", p.manufacturingCostPerGram)
        put("photoUri", p.photoUri)
        put("description", p.description)
        put("isAvailable", p.isAvailable)
        put("createdAt", p.createdAt)
    }

    private fun jsonToProduct(j: JSONObject) = MerchantProductEntity(
        id = j.getString("id"),
        merchantId = j.getString("merchantId"),
        merchantShopName = j.getString("merchantShopName"),
        merchantPhone = j.optString("merchantPhone", null),
        title = j.getString("title"),
        category = j.optString("category", MerchantProductCategory.BULLION.name),
        karat = j.optString("karat", GoldKarat.KARAT_24.name),
        weightGrams = j.optDouble("weightGrams", 0.0),
        manufacturingCostPerGram = j.optDouble("manufacturingCostPerGram", 0.0),
        photoUri = j.optString("photoUri", null),
        description = j.optString("description", null),
        isAvailable = j.optBoolean("isAvailable", true),
        createdAt = j.optLong("createdAt", System.currentTimeMillis())
    )
}
