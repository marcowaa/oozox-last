package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    GOOGLE_DRIVE("سحابة OOZOX وتصدير مشفر (.oozox)", "share_vault"),
    OOZOX_CLOUD("نسخة سحابية مشفرة على خادم OOZOX", "local_vault"),
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
    val createdAtText: String,
    val mediaFilesCount: Int = 0,
    val formatVersion: Int = 3,
    val deviceName: String = "",
    val chatsCount: Int = 0,
    val messagesCount: Int = 0
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
    val errorMessage: String? = null,
    val mediaFilesCount: Int = 0,
    val sizeFormatted: String = "",
    val chatsCount: Int = 0,
    val messagesCount: Int = 0
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
    val backupDateText: String = "",
    val mediaFilesCount: Int = 0,
    val totalRecordsCount: Int = 0
)

@Serializable
data class VaultMediaFileDto(
    val relativePath: String,
    val sha256: String,
    val sizeBytes: Long,
    val contentBase64: String
)

@Serializable
data class VaultV3Snapshot(
    val version: Int = 3,
    val createdAt: Long = System.currentTimeMillis(),
    val createdAtText: String = "",
    val ownerPhoneHash: String = "",
    val ownerPhoneMasked: String = "",
    val users: List<UserEntity> = emptyList(),
    val contacts: List<ContactEntity> = emptyList(),
    val chats: List<ChatEntity> = emptyList(),
    val messages: List<MessageEntity> = emptyList(),
    val chatAccounts: List<ChatAccountEntity> = emptyList(),
    val hashtagAccounts: List<HashtagAccountEntity> = emptyList(),
    val hashtagEntries: List<HashtagEntryEntity> = emptyList(),
    val comments: List<CommentEntity> = emptyList(),
    val workGroups: List<WorkGroupEntity> = emptyList(),
    val groupMembers: List<GroupMemberEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val projectMembers: List<ProjectMemberEntity> = emptyList(),
    val projectAuditLogs: List<ProjectAuditLogEntity> = emptyList(),
    val ledgerEntries: List<LedgerEntryEntity> = emptyList(),
    val clearingSettlements: List<ClearingSettlementEntity> = emptyList(),
    val syncQueue: List<SyncQueueEntity> = emptyList(),
    val subscriptionPlans: List<SubscriptionPlanEntity> = emptyList(),
    val subscriptionRequests: List<SubscriptionRequestEntity> = emptyList(),
    val promoAnnouncements: List<PromoAnnouncementEntity> = emptyList(),
    val merchantReviews: List<MerchantReviewEntity> = emptyList(),
    val merchantProducts: List<MerchantProductEntity> = emptyList(),
    val merchantDailySales: List<MerchantDailySaleEntity> = emptyList(),
    val merchantClosingReports: List<MerchantDailyClosingReportEntity> = emptyList(),
    val priceAlerts: List<PriceAlertEntity> = emptyList(),
    val hiddenProfiles: List<HiddenProfileEntity> = emptyList(),
    val hiddenSubAccounts: List<HiddenSubAccountEntity> = emptyList(),
    val dailyRecords: List<DailyAccountingRecordEntity> = emptyList(),
    val attendance: List<EmployeeAttendanceEntity> = emptyList(),
    val complaints: List<FinancialComplaintEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val purchases: List<GoldPurchaseEntity> = emptyList(),
    val traderRequests: List<TraderRequestEntity> = emptyList(),
    val adminConfigs: List<AdminConfigEntity> = emptyList(),
    val invoices: List<InvoiceEntity> = emptyList(),
    val projectTasks: List<ProjectTaskEntity> = emptyList(),
    val shiftHandovers: List<ShiftHandoverEntity> = emptyList(),
    val dynamicTemplates: List<DynamicExcelTemplateEntity> = emptyList(),
    val dynamicCustomFields: List<DynamicExcelCustomFieldEntity> = emptyList(),
    val dynamicRowRecords: List<DynamicExcelRowRecordEntity> = emptyList(),
    val businessProfiles: List<BusinessProfileEntity> = emptyList(),
    val payrollAdjustments: List<PayrollAdjustmentEntity> = emptyList(),
    val employeeLeaves: List<EmployeeLeaveEntity> = emptyList(),
    val mediaFiles: List<VaultMediaFileDto> = emptyList()
)

data class VaultExportPayload(
    val snapshotJson: String,
    val stats: BackupStats,
    val phoneHash: String
)

object SecureBackupVaultEngine {

    const val MAGIC_HEADER_V1 = "OOZOX_ENCRYPTED_VAULT_V1"
    const val MAGIC_HEADER_V2 = "OOZOX_ENCRYPTED_VAULT_V2"
    const val MAGIC_HEADER_V3 = "OOZOX_ENCRYPTED_VAULT_V3"

    private const val ITERATIONS = 65536
    private const val KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val MAX_SINGLE_MEDIA_BYTES = 8L * 1024L * 1024L
    private const val MAX_TOTAL_MEDIA_BYTES = 32L * 1024L * 1024L

    private val vaultJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun hashPhoneNumber(phone: String): String {
        val cleanPhone = phone.replace("[^0-9+]".toRegex(), "").trim()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(cleanPhone.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private fun maskPhone(cleanPhone: String): String =
        if (cleanPhone.length > 4) "${cleanPhone.take(3)}****${cleanPhone.takeLast(3)}" else cleanPhone

    /**
     * Inspects an encrypted V1/V2/V3 .oozox backup envelope without requiring decryption.
     */
    fun inspectBackupMetadata(backupContent: String, currentPhone: String): BackupMetadataPreview {
        return try {
            val json = JSONObject(backupContent)
            val magic = json.optString("magic")
            if (magic != MAGIC_HEADER_V3 && magic != MAGIC_HEADER_V2 && magic != MAGIC_HEADER_V1) {
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
                    errorMessage = "الملف ليس نسخة احتياطية صالحة من نظام OOZOX."
                )
            }

            val phoneHash = json.optString("phoneHash")
            val cleanPhone = currentPhone.replace("[^0-9+]".toRegex(), "").trim()
            val currentHash = hashPhoneNumber(cleanPhone)
            val isMatched = phoneHash.isBlank() || phoneHash == currentHash

            val timestamp = json.optLong("timestamp", System.currentTimeMillis())
            val dateText = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))
            val backupType = json.optString("backupType", "FULL_VAULT_V3")
            val statsObj = json.optJSONObject("stats")
            val inferredVersion = when (magic) {
                MAGIC_HEADER_V3 -> 3
                MAGIC_HEADER_V2 -> 2
                else -> 1
            }

            val bytesLen = backupContent.toByteArray(StandardCharsets.UTF_8).size.toDouble()
            val kb = bytesLen / 1024.0
            val formattedSize = if (kb >= 1024.0) "${"%.2f".format(kb / 1024.0)} MB" else "${"%.1f".format(kb.coerceAtLeast(0.1))} KB"

            BackupMetadataPreview(
                isValid = true,
                version = json.optInt("version", inferredVersion),
                createdAtText = dateText,
                ownerPhoneMasked = json.optString("ownerPhoneMasked", maskPhone(cleanPhone)),
                isPhoneMatched = isMatched,
                backupType = backupType,
                totalRecordsCount = statsObj?.optInt("totalRecords") ?: 0,
                profilesCount = statsObj?.optInt("subProfiles") ?: statsObj?.optInt("subProfilesCount") ?: 0,
                recordsCount = statsObj?.optInt("dailyRecords") ?: statsObj?.optInt("recordsCount") ?: 0,
                mediaFilesCount = statsObj?.optInt("mediaFilesCount") ?: 0,
                sizeFormatted = formattedSize,
                chatsCount = statsObj?.optInt("chatsCount") ?: 0,
                messagesCount = statsObj?.optInt("messagesCount") ?: 0
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
     * Collects all 42 Room tables and local app media files into an uncompressed Vault V3 snapshot.
     * Used by both `.oozox` encrypted vault backups and E2E Cross-Device Transfer.
     */
    suspend fun buildRawVaultV3Snapshot(
        context: Context,
        db: AppDatabase,
        currentUserPhone: String,
        includeMedia: Boolean = true
    ): VaultExportPayload = withContext(Dispatchers.IO) {
        val cleanPhone = currentUserPhone.replace("[^0-9+]".toRegex(), "").trim()
        val phoneHash = hashPhoneNumber(cleanPhone)
        val nowText = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())

        val users = db.userDao().getAllUsersSync()
        val contacts = db.contactDao().getAllContactsSync()
        val chats = db.chatDao().getAllChatsSync()
        val messages = db.messageDao().getAllMessagesSync()
        val chatAccounts = db.chatAccountDao().getAllChatAccountsSync()
        val hashtagAccounts = db.chatAccountDao().getAllHashtagAccountsSync()
        val hashtagEntries = db.chatAccountDao().getAllHashtagEntriesSync()
        val comments = db.chatAccountDao().getAllCommentsSync()
        val workGroups = db.workGroupDao().getAllWorkGroupsSync()
        val groupMembers = db.workGroupDao().getAllGroupMembersSync()
        val projects = db.projectDao().getAllProjectsSync()
        val projectMembers = db.projectMemberDao().getAllProjectMembersSync()
        val projectAuditLogs = db.projectAuditLogDao().getAllAuditLogsSync()
        val ledgerEntries = db.immutableLedgerDao().getAllLedgerEntriesSync()
        val clearingSettlements = db.clearingSettlementDao().getAllSettlementsSync()
        val syncQueue = db.syncQueueDao().getAllSyncItemsSync()
        val subscriptionPlans = db.subscriptionPlanDao().getAllPlansSync()
        val subscriptionRequests = db.subscriptionRequestDao().getAllRequestsSync()
        val promoAnnouncements = db.promoAnnouncementDao().getAllAnnouncementsSync()
        val merchantReviews = db.merchantReviewDao().getAllReviewsSync()
        val merchantProducts = db.merchantStoreDao().getAllProductsSync()
        val merchantDailySales = db.merchantStoreDao().getAllSalesSync()
        val merchantClosingReports = db.merchantStoreDao().getAllClosingReportsSync()
        val priceAlerts = db.priceAlertDao().getAllAlertsSync()
        val hiddenProfiles = db.hiddenProfileDao().getAllHiddenProfilesSync()
        val hiddenSubAccounts = db.hiddenSubAccountDao().getAllSubAccountsSync()
        val dailyRecords = db.dailyAccountingRecordDao().getAllRecordsSync()
        val attendance = db.employeeAttendanceDao().getAllAttendanceSync()
        val complaints = db.financialComplaintDao().getAllComplaintsSync()
        val transactions = db.transactionDao().getAllTransactionsSync()
        val purchases = db.goldPurchaseDao().getAllPurchasesSync()
        val traderRequests = db.traderRequestDao().getAllTraderRequestsSync()
        val adminConfig = db.adminConfigDao().getConfigSync()?.let { listOf(it) } ?: emptyList()
        val invoices = db.invoiceDao().getAllInvoicesSync()
        val projectTasks = db.projectTaskDao().getAllProjectTasksSync()
        val shiftHandovers = db.shiftHandoverDao().getAllShiftHandoversSync()
        val dynamicTemplates = db.dynamicExcelDao().getAllTemplatesSync()
        val dynamicCustomFields = db.dynamicExcelDao().getAllCustomFieldsSync()
        val dynamicRowRecords = db.dynamicExcelDao().getAllRowsSync()
        val businessProfiles = db.businessHrDao().getAllBusinessProfilesSync()
        val payrollAdjustments = db.businessHrDao().getAllPayrollAdjustmentsSync()
        val employeeLeaves = db.businessHrDao().getAllEmployeeLeavesSync()

        val mediaFiles = if (includeMedia) collectLocalMediaFiles(context) else emptyList()

        val totalRecordsCount = users.size + contacts.size + chats.size + messages.size +
            chatAccounts.size + hashtagAccounts.size + hashtagEntries.size + comments.size +
            workGroups.size + groupMembers.size + projects.size + projectMembers.size +
            projectAuditLogs.size + ledgerEntries.size + clearingSettlements.size +
            subscriptionPlans.size + subscriptionRequests.size + promoAnnouncements.size +
            merchantReviews.size + merchantProducts.size + merchantDailySales.size +
            merchantClosingReports.size + priceAlerts.size + hiddenProfiles.size +
            hiddenSubAccounts.size + dailyRecords.size + attendance.size + complaints.size +
            transactions.size + purchases.size + traderRequests.size + invoices.size +
            projectTasks.size + shiftHandovers.size + dynamicTemplates.size +
            dynamicCustomFields.size + dynamicRowRecords.size + businessProfiles.size +
            payrollAdjustments.size + employeeLeaves.size

        val snapshot = VaultV3Snapshot(
            version = 3,
            createdAt = System.currentTimeMillis(),
            createdAtText = nowText,
            ownerPhoneHash = phoneHash,
            ownerPhoneMasked = maskPhone(cleanPhone),
            users = users,
            contacts = contacts,
            chats = chats,
            messages = messages,
            chatAccounts = chatAccounts,
            hashtagAccounts = hashtagAccounts,
            hashtagEntries = hashtagEntries,
            comments = comments,
            workGroups = workGroups,
            groupMembers = groupMembers,
            projects = projects,
            projectMembers = projectMembers,
            projectAuditLogs = projectAuditLogs,
            ledgerEntries = ledgerEntries,
            clearingSettlements = clearingSettlements,
            syncQueue = syncQueue,
            subscriptionPlans = subscriptionPlans,
            subscriptionRequests = subscriptionRequests,
            promoAnnouncements = promoAnnouncements,
            merchantReviews = merchantReviews,
            merchantProducts = merchantProducts,
            merchantDailySales = merchantDailySales,
            merchantClosingReports = merchantClosingReports,
            priceAlerts = priceAlerts,
            hiddenProfiles = hiddenProfiles,
            hiddenSubAccounts = hiddenSubAccounts,
            dailyRecords = dailyRecords,
            attendance = attendance,
            complaints = complaints,
            transactions = transactions,
            purchases = purchases,
            traderRequests = traderRequests,
            adminConfigs = adminConfig,
            invoices = invoices,
            projectTasks = projectTasks,
            shiftHandovers = shiftHandovers,
            dynamicTemplates = dynamicTemplates,
            dynamicCustomFields = dynamicCustomFields,
            dynamicRowRecords = dynamicRowRecords,
            businessProfiles = businessProfiles,
            payrollAdjustments = payrollAdjustments,
            employeeLeaves = employeeLeaves,
            mediaFiles = mediaFiles
        )

        val stats = BackupStats(
            usersCount = users.size,
            chatsCount = chats.size,
            messagesCount = messages.size,
            recordsCount = dailyRecords.size,
            complaintsCount = complaints.size,
            subProfilesCount = hiddenProfiles.size + hiddenSubAccounts.size,
            workGroupsCount = workGroups.size,
            projectsCount = projects.size,
            backupDateText = nowText,
            mediaFilesCount = mediaFiles.size,
            totalRecordsCount = totalRecordsCount
        )

        VaultExportPayload(
            snapshotJson = vaultJson.encodeToString(snapshot),
            stats = stats,
            phoneHash = phoneHash
        )
    }

    private fun collectLocalMediaFiles(context: Context): List<VaultMediaFileDto> {
        val result = mutableListOf<VaultMediaFileDto>()
        val root = context.filesDir ?: return emptyList()
        var accumulatedBytes = 0L
        val excludedDirs = setOf("backups", "transfer_tmp", "datastore", "profileInstalled")
        root.walkTopDown()
            .filter { file ->
                file.isFile && !excludedDirs.any { dir ->
                    file.relativeTo(root).path.startsWith(dir)
                }
            }
            .forEach { file ->
                val len = file.length()
                if (len in 1..MAX_SINGLE_MEDIA_BYTES && (accumulatedBytes + len) <= MAX_TOTAL_MEDIA_BYTES) {
                    val bytes = file.readBytes()
                    accumulatedBytes += bytes.size
                    result.add(
                        VaultMediaFileDto(
                            relativePath = file.relativeTo(root).path.replace('\\', '/'),
                            sha256 = sha256Hex(bytes),
                            sizeBytes = bytes.size.toLong(),
                            contentBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        )
                    )
                }
            }
        return result
    }

    /**
     * Encrypts a raw Vault V3 snapshot JSON into a `.oozox` V3 JSON envelope with PBKDF2 + AES-256-GCM + HMAC-SHA256.
     */
    private fun sealEncryptedEnvelope(
        plainJsonString: String,
        phoneHash: String,
        cleanPhone: String,
        encryptionPassphrase: String,
        backupType: String,
        stats: BackupStats
    ): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }

        val keySpec = PBEKeySpec(encryptionPassphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        cipher.updateAAD(phoneHash.toByteArray(StandardCharsets.UTF_8))
        val cipherText = cipher.doFinal(plainJsonString.toByteArray(StandardCharsets.UTF_8))

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
        val hmacSignature = mac.doFinal(cipherText)

        val packageEnvelope = JSONObject().apply {
            put("magic", MAGIC_HEADER_V3)
            put("version", 3)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("phoneHash", phoneHash)
            put("ownerPhoneMasked", maskPhone(cleanPhone))
            put("hmac", Base64.encodeToString(hmacSignature, Base64.NO_WRAP))
            put("payload", Base64.encodeToString(cipherText, Base64.NO_WRAP))
            put("timestamp", System.currentTimeMillis())
            put("backupType", backupType)
            put("stats", JSONObject().apply {
                put("totalRecords", stats.totalRecordsCount)
                put("chatsCount", stats.chatsCount)
                put("messagesCount", stats.messagesCount)
                put("mediaFilesCount", stats.mediaFilesCount)
                put("dailyRecords", stats.recordsCount)
                put("complaintsCount", stats.complaintsCount)
                put("subProfiles", stats.subProfilesCount)
                put("workGroups", stats.workGroupsCount)
                put("projects", stats.projectsCount)
            })
        }
        return packageEnvelope.toString()
    }

    /**
     * Targeted migration export for sub-accounts / vaults.
     * Uploads to real `/api/backups` when cloudProvider != LOCAL_DEVICE (never uses fake SharedPreferences).
     */
    suspend fun createSubAccountsMigrationBackup(
        context: Context,
        db: AppDatabase,
        currentUserPhone: String,
        encryptionPassphrase: String,
        selectedProfileIds: List<String>? = null,
        cloudProvider: CloudStorageProvider = CloudStorageProvider.LOCAL_DEVICE
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            if (encryptionPassphrase.length < 4) {
                return@withContext BackupResult.Error("يجب أن تتكون كلمة مرور النسخ الاحتياطي من 4 أحرف أو أرقام على الأقل لضمان التشفير.")
            }

            val cleanPhone = currentUserPhone.replace("[^0-9+]".toRegex(), "").trim()
            val phoneHash = hashPhoneNumber(cleanPhone)

            val allProfiles = db.hiddenProfileDao().getAllHiddenProfilesSync()
            val filteredProfiles = if (selectedProfileIds.isNullOrEmpty()) allProfiles else allProfiles.filter { selectedProfileIds.contains(it.id) }
            val profileIdSet = filteredProfiles.map { it.id }.toSet()

            val allWorkGroups = db.workGroupDao().getAllWorkGroupsSync()
            val filteredWorkGroups = if (profileIdSet.isEmpty()) allWorkGroups else allWorkGroups.filter { it.profileId in profileIdSet || it.profileId == null }
            val filteredGroupIds = filteredWorkGroups.map { it.id }.toSet()
            val filteredMembers = db.workGroupDao().getAllGroupMembersSync().filter { it.groupId in filteredGroupIds }
            val filteredDailyRecords = db.dailyAccountingRecordDao().getAllRecordsSync().filter {
                (profileIdSet.isEmpty() || it.profileId in profileIdSet) || it.groupId in filteredGroupIds
            }
            val filteredComplaints = db.financialComplaintDao().getAllComplaintsSync().filter {
                (profileIdSet.isEmpty() || it.profileId in profileIdSet) || it.groupId in filteredGroupIds
            }
            val filteredAttendance = db.employeeAttendanceDao().getAllAttendanceSync().filter { it.groupId in filteredGroupIds }

            val nowText = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
            val totalCount = filteredProfiles.size + filteredWorkGroups.size + filteredMembers.size +
                filteredDailyRecords.size + filteredComplaints.size + filteredAttendance.size

            val snapshot = VaultV3Snapshot(
                version = 3,
                createdAt = System.currentTimeMillis(),
                createdAtText = nowText,
                ownerPhoneHash = phoneHash,
                ownerPhoneMasked = maskPhone(cleanPhone),
                hiddenProfiles = filteredProfiles,
                workGroups = filteredWorkGroups,
                groupMembers = filteredMembers,
                dailyRecords = filteredDailyRecords,
                complaints = filteredComplaints,
                attendance = filteredAttendance
            )
            val stats = BackupStats(
                recordsCount = filteredDailyRecords.size,
                complaintsCount = filteredComplaints.size,
                subProfilesCount = filteredProfiles.size,
                workGroupsCount = filteredWorkGroups.size,
                backupDateText = nowText,
                totalRecordsCount = totalCount
            )
            val envelopeString = sealEncryptedEnvelope(
                plainJsonString = vaultJson.encodeToString(snapshot),
                phoneHash = phoneHash,
                cleanPhone = cleanPhone,
                encryptionPassphrase = encryptionPassphrase,
                backupType = "SUB_ACCOUNTS_MIGRATION",
                stats = stats
            )

            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "oozox_subaccounts_vault_$timestampStr.oozox"
            val backupsDir = File(context.filesDir, "backups").apply { mkdirs() }
            val backupFile = File(backupsDir, backupFileName)
            FileOutputStream(backupFile).use { it.write(envelopeString.toByteArray(StandardCharsets.UTF_8)) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", backupFile)

            var uploadedBackupId: String? = null
            if (cloudProvider != CloudStorageProvider.LOCAL_DEVICE && RetrofitClient.hasActiveSession()) {
                val uploadRes = uploadBackupEnvelopeToServer(
                    fileName = backupFileName,
                    backupType = "SUB_ACCOUNTS_MIGRATION",
                    phoneHash = phoneHash,
                    envelopeString = envelopeString,
                    stats = stats
                )
                if (uploadRes.isFailure) {
                    return@withContext BackupResult.Error("تم حفظ الملف محلياً لكن فشل الرفع إلى السحابة: ${uploadRes.exceptionOrNull()?.message}")
                }
                uploadedBackupId = uploadRes.getOrNull()?.backupId
            }

            BackupResult.Success(
                message = if (uploadedBackupId != null) {
                    "تم تصدير ورفع خزينة الحسابات الفرعية ($totalCount سجل) إلى سحابة OOZOX المشفرة بنجاح!"
                } else {
                    "تم تصدير خزينة الحسابات الفرعية ($totalCount سجل) محلياً بتشفير AES-256 بنجاح."
                },
                filePath = backupFile.absolutePath,
                uri = uri,
                recordsCount = totalCount,
                cloudBackupId = uploadedBackupId,
                backupStats = stats
            )
        } catch (e: Exception) {
            BackupResult.Error("فشل في ترحيل الحسابات الفرعية: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Exports a complete end-to-end encrypted Vault V3 archive (all 42 Room tables + media files).
     * When `isGoogleDriveCloudExport == true`, uploads genuinely to POST /api/backups and fails honestly if upload fails.
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
            val export = buildRawVaultV3Snapshot(context, db, cleanPhone, includeMedia = true)
            val envelopeString = sealEncryptedEnvelope(
                plainJsonString = export.snapshotJson,
                phoneHash = export.phoneHash,
                cleanPhone = cleanPhone,
                encryptionPassphrase = encryptionPassphrase,
                backupType = "FULL_VAULT_V3",
                stats = export.stats
            )

            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "oozox_vault_v3_$timestampStr.oozox"

            val backupsDir = File(context.filesDir, "backups").apply { mkdirs() }
            val backupFile = File(backupsDir, backupFileName)
            FileOutputStream(backupFile).use { output ->
                output.write(envelopeString.toByteArray(StandardCharsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", backupFile)
            var cloudBackupId: String? = null

            if (isGoogleDriveCloudExport) {
                if (!RetrofitClient.hasActiveSession()) {
                    return@withContext BackupResult.Error("يلزم تسجيل الدخول بجلسة خادم نشطة لرفع النسخة السحابية إلى سحابة OOZOX.")
                }
                val uploadRes = uploadBackupEnvelopeToServer(
                    fileName = backupFileName,
                    backupType = "FULL_VAULT_V3",
                    phoneHash = export.phoneHash,
                    envelopeString = envelopeString,
                    stats = export.stats
                )
                if (uploadRes.isFailure) {
                    return@withContext BackupResult.Error(
                        "تعذر رفع النسخة إلى سحابة OOZOX: ${uploadRes.exceptionOrNull()?.message ?: "خطأ في الشبكة"}"
                    )
                }
                cloudBackupId = uploadRes.getOrNull()?.backupId
                OfflineSyncManager.markServerBackupSucceeded(context)
            }

            BackupResult.Success(
                message = if (cloudBackupId != null) {
                    "تم رفع النسخة السحابية المشفرة V3 (${export.stats.totalRecordsCount} سجل و ${export.stats.mediaFilesCount} ملف وسائط) بنجاح!"
                } else {
                    "تم إنشاء النسخة الاحتياطية المشفرة V3 (.oozox) محلياً (${export.stats.totalRecordsCount} سجل و ${export.stats.mediaFilesCount} ملف وسائط)!"
                },
                filePath = backupFile.absolutePath,
                uri = uri,
                recordsCount = export.stats.totalRecordsCount,
                cloudBackupId = cloudBackupId,
                backupStats = export.stats
            )
        } catch (e: Exception) {
            BackupResult.Error("فشل في إنشاء النسخة الاحتياطية: ${e.localizedMessage ?: e.message}")
        }
    }

    private suspend fun uploadBackupEnvelopeToServer(
        fileName: String,
        backupType: String,
        phoneHash: String,
        envelopeString: String,
        stats: BackupStats
    ): Result<ApiCloudBackupItemDto> {
        return try {
            val resp = RetrofitClient.apiService().uploadCloudBackup(
                ApiUploadBackupRequest(
                    fileName = fileName,
                    backupType = backupType,
                    formatVersion = 3,
                    phoneHash = phoneHash,
                    vaultContent = envelopeString,
                    deviceName = DeviceTrustManager.getDeviceDisplayName(),
                    stats = ApiBackupStatsDto(
                        usersCount = stats.usersCount,
                        chatsCount = stats.chatsCount,
                        messagesCount = stats.messagesCount,
                        recordsCount = stats.recordsCount,
                        subProfilesCount = stats.subProfilesCount,
                        workGroupsCount = stats.workGroupsCount,
                        projectsCount = stats.projectsCount,
                        mediaFilesCount = stats.mediaFilesCount,
                        totalRecords = stats.totalRecordsCount
                    )
                )
            )
            val created = resp.body()?.data?.backup
            if (!resp.isSuccessful || resp.body()?.success != true || created == null) {
                val errMsg = resp.errorBody()?.string()?.let { raw ->
                    try { JSONObject(raw).optString("message") } catch (_: Exception) { null }
                } ?: "فشل الرفع (${resp.code()})"
                Result.failure(IllegalStateException(errMsg))
            } else {
                Result.success(created)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches authentic cloud backups owned by the currently authenticated account via GET /api/backups.
     */
    suspend fun listServerCloudBackups(ownerPhone: String): Result<List<CloudBackupRecord>> = withContext(Dispatchers.IO) {
        if (!RetrofitClient.hasActiveSession()) {
            return@withContext Result.failure(IllegalStateException("يرجى تسجيل الدخول أولاً لعرض النسخ السحابية."))
        }
        try {
            val resp = RetrofitClient.apiService().listCloudBackups()
            if (!resp.isSuccessful || resp.body()?.success != true) {
                return@withContext Result.failure(IllegalStateException("تعذر جلب النسخ السحابية (${resp.code()})."))
            }
            val phoneHash = hashPhoneNumber(ownerPhone)
            val mapped = resp.body()?.data?.backups.orEmpty().map { dto ->
                val kb = dto.sizeBytes / 1024.0
                val formattedSize = if (kb >= 1024.0) "${"%.2f".format(kb / 1024.0)} MB" else "${"%.1f".format(kb.coerceAtLeast(0.1))} KB"
                CloudBackupRecord(
                    id = dto.backupId,
                    ownerPhone = ownerPhone,
                    ownerPhoneHash = phoneHash,
                    provider = CloudStorageProvider.OOZOX_CLOUD,
                    fileName = dto.fileName,
                    encryptedPayloadBase64 = dto.vaultContent.orEmpty(),
                    fileSizeFormatted = formattedSize,
                    totalRecords = dto.stats.totalRecords,
                    subProfilesCount = dto.stats.subProfilesCount,
                    createdAt = System.currentTimeMillis(),
                    createdAtText = dto.createdAt.take(16).replace("T", " "),
                    mediaFilesCount = dto.stats.mediaFilesCount,
                    formatVersion = dto.formatVersion,
                    deviceName = dto.deviceName,
                    chatsCount = dto.stats.chatsCount,
                    messagesCount = dto.stats.messagesCount
                )
            }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchCloudBackupsFromServer(ownerPhone: String): List<CloudBackupRecord> =
        listServerCloudBackups(ownerPhone).getOrDefault(emptyList())

    suspend fun downloadServerCloudBackup(backupId: String): Result<ApiCloudBackupItemDto> = withContext(Dispatchers.IO) {
        if (!RetrofitClient.hasActiveSession()) {
            return@withContext Result.failure(IllegalStateException("يرجى تسجيل الدخول أولاً لتحميل النسخة السحابية."))
        }
        try {
            val resp = RetrofitClient.apiService().downloadCloudBackup(backupId)
            val item = resp.body()?.data
            val content = item?.vaultContent
            if (!resp.isSuccessful || resp.body()?.success != true || item == null || content.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("تعذر تحميل النسخة السحابية من الخادم (${resp.code()})."))
            }
            if (item.payloadSha256.isNotBlank()) {
                val actualSha = sha256Hex(content.toByteArray(StandardCharsets.UTF_8))
                if (!actualSha.equals(item.payloadSha256, ignoreCase = true)) {
                    return@withContext Result.failure(SecurityException("❌ فشل التحقق من بصمة SHA-256 للنسخة المحمّلة من السحابة."))
                }
            }
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads a verified cloud backup via GET /api/backups/:backupId/download and restores it atomically.
     */
    suspend fun downloadAndRestoreCloudBackup(
        context: Context,
        db: AppDatabase,
        backupId: String,
        currentUserPhone: String,
        enteredPassphrase: String
    ): BackupResult = withContext(Dispatchers.IO) {
        val dl = downloadServerCloudBackup(backupId)
        if (dl.isFailure) {
            return@withContext BackupResult.Error(dl.exceptionOrNull()?.message ?: "تعذر تحميل النسخة السحابية.")
        }
        val content = dl.getOrNull()?.vaultContent.orEmpty()
        restoreEncryptedBackup(context, db, content, currentUserPhone, enteredPassphrase)
    }

    suspend fun deleteServerCloudBackup(backupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!RetrofitClient.hasActiveSession()) {
            return@withContext Result.failure(IllegalStateException("لا توجد جلسة نشطة."))
        }
        try {
            val resp = RetrofitClient.apiService().deleteCloudBackup(backupId)
            if (resp.isSuccessful) Result.success(Unit)
            else Result.failure(IllegalStateException("فشل الحذف (${resp.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCloudBackupFromServer(backupId: String): Boolean =
        deleteServerCloudBackup(backupId).isSuccess

    /**
     * Validates, decrypts, and atomically restores a V1, V2, or V3 encrypted `.oozox` vault package.
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
            } catch (_: Exception) {
                return@withContext BackupResult.Error("صيغة الملف غير صالحة. تأكد من اختيار ملف (.oozox) معتمد.")
            }

            val magic = envelope.optString("magic")
            if (magic != MAGIC_HEADER_V3 && magic != MAGIC_HEADER_V2 && magic != MAGIC_HEADER_V1) {
                return@withContext BackupResult.Error("ملف النسخة غير صالح أو غير معتمد من منصة OOZOX.")
            }

            val backupPhoneHash = envelope.optString("phoneHash")
            val cleanCurrentPhone = currentUserPhone.replace("[^0-9+]".toRegex(), "").trim()
            val currentPhoneHash = hashPhoneNumber(cleanCurrentPhone)

            if (backupPhoneHash.isNotBlank() && backupPhoneHash != currentPhoneHash) {
                return@withContext BackupResult.Error(
                    "❌ خطأ أمني: رقم الهاتف الحالي ($cleanCurrentPhone) لا يطابق رقم مالك النسخة الاحتياطية الأصلي!"
                )
            }

            val salt = Base64.decode(envelope.getString("salt"), Base64.NO_WRAP)
            val iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)
            val cipherText = Base64.decode(envelope.getString("payload"), Base64.NO_WRAP)
            val hmacBase64 = envelope.optString("hmac")

            val keySpec = PBEKeySpec(enteredPassphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(keyBytes, "AES")

            if (hmacBase64.isNotBlank()) {
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
                val calculatedHmac = mac.doFinal(cipherText)
                val expectedHmac = Base64.decode(hmacBase64, Base64.NO_WRAP)
                if (!calculatedHmac.contentEquals(expectedHmac)) {
                    return@withContext BackupResult.Error("❌ كلمة المرور غير صحيحة أو تم العبث ببيانات النسخة الاحتياطية!")
                }
            }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            if (backupPhoneHash.isNotBlank()) {
                cipher.updateAAD(backupPhoneHash.toByteArray(StandardCharsets.UTF_8))
            }

            val decryptedBytes = try {
                cipher.doFinal(cipherText)
            } catch (_: Exception) {
                return@withContext BackupResult.Error("❌ فشل فك التشفير: كلمة المرور غير صحيحة أو البيانات تالفة.")
            }

            val decryptedJsonString = String(decryptedBytes, StandardCharsets.UTF_8)
            val rootObj = JSONObject(decryptedJsonString)
            val payloadVersion = rootObj.optInt("version", if (magic == MAGIC_HEADER_V3) 3 else 2)

            if (payloadVersion >= 3 && magic == MAGIC_HEADER_V3) {
                restoreRawVaultV3Json(context, db, decryptedJsonString, cleanCurrentPhone)
            } else {
                restoreLegacyV1V2Json(db, rootObj)
            }
        } catch (e: Exception) {
            BackupResult.Error("فشل في استعادة النسخة الاحتياطية: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Atomically restores a VaultV3Snapshot JSON string inside a single Room transaction,
     * verifying ownership and media SHA-256 digests before committing.
     * Used by both `.oozox` V3 restore and Cross-Device E2E Transfer.
     */
    suspend fun restoreRawVaultV3Json(
        context: Context,
        db: AppDatabase,
        rawSnapshotJson: String,
        currentUserPhone: String
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val snapshot = vaultJson.decodeFromString<VaultV3Snapshot>(rawSnapshotJson)
            val cleanCurrentPhone = currentUserPhone.replace("[^0-9+]".toRegex(), "").trim()
            val expectedHash = hashPhoneNumber(cleanCurrentPhone)
            if (snapshot.ownerPhoneHash.isNotBlank() && cleanCurrentPhone.isNotBlank() && snapshot.ownerPhoneHash != expectedHash) {
                return@withContext BackupResult.Error("❌ فشل التحقق من ملكية الهاتف أثناء الاستعادة.")
            }

            // Verify & stage media files BEFORE mutating Room
            val stagedMedia = mutableListOf<Pair<File, ByteArray>>()
            val rootDir = context.filesDir
            val canonicalRoot = rootDir.canonicalPath + File.separator
            for (media in snapshot.mediaFiles) {
                val safeRel = media.relativePath.replace("\\", "/").trimStart('/')
                val targetFile = File(rootDir, safeRel)
                if (!targetFile.canonicalPath.startsWith(canonicalRoot)) {
                    return@withContext BackupResult.Error("❌ مسار ملف غير آمن داخل النسخة الاحتياطية: $safeRel")
                }
                val bytes = Base64.decode(media.contentBase64, Base64.NO_WRAP)
                if (!sha256Hex(bytes).equals(media.sha256, ignoreCase = true)) {
                    return@withContext BackupResult.Error("❌ تلف في بصمة SHA-256 للملف: $safeRel — تم إلغاء الاستعادة حفاظاً على بياناتك.")
                }
                stagedMedia.add(targetFile to bytes)
            }

            var restoredCount = 0
            db.withTransaction {
                if (snapshot.contacts.isNotEmpty()) { db.contactDao().insertContacts(snapshot.contacts); restoredCount += snapshot.contacts.size }
                if (snapshot.chats.isNotEmpty()) { db.chatDao().insertChats(snapshot.chats); restoredCount += snapshot.chats.size }
                if (snapshot.messages.isNotEmpty()) { db.messageDao().insertMessages(snapshot.messages); restoredCount += snapshot.messages.size }
                if (snapshot.chatAccounts.isNotEmpty()) { db.chatAccountDao().insertAccounts(snapshot.chatAccounts); restoredCount += snapshot.chatAccounts.size }
                if (snapshot.hashtagAccounts.isNotEmpty()) { db.chatAccountDao().insertHashtagAccounts(snapshot.hashtagAccounts); restoredCount += snapshot.hashtagAccounts.size }
                if (snapshot.hashtagEntries.isNotEmpty()) { db.chatAccountDao().insertHashtagEntries(snapshot.hashtagEntries); restoredCount += snapshot.hashtagEntries.size }
                if (snapshot.comments.isNotEmpty()) { db.chatAccountDao().insertComments(snapshot.comments); restoredCount += snapshot.comments.size }
                if (snapshot.workGroups.isNotEmpty()) { db.workGroupDao().insertWorkGroups(snapshot.workGroups); restoredCount += snapshot.workGroups.size }
                if (snapshot.groupMembers.isNotEmpty()) { db.workGroupDao().insertGroupMembers(snapshot.groupMembers); restoredCount += snapshot.groupMembers.size }
                if (snapshot.projects.isNotEmpty()) { db.projectDao().insertProjects(snapshot.projects); restoredCount += snapshot.projects.size }
                if (snapshot.projectMembers.isNotEmpty()) { db.projectMemberDao().insertMembers(snapshot.projectMembers); restoredCount += snapshot.projectMembers.size }
                if (snapshot.projectAuditLogs.isNotEmpty()) { db.projectAuditLogDao().insertAuditLogs(snapshot.projectAuditLogs); restoredCount += snapshot.projectAuditLogs.size }
                if (snapshot.ledgerEntries.isNotEmpty()) { db.immutableLedgerDao().insertEntriesIgnore(snapshot.ledgerEntries); restoredCount += snapshot.ledgerEntries.size }
                if (snapshot.clearingSettlements.isNotEmpty()) { db.clearingSettlementDao().insertSettlements(snapshot.clearingSettlements); restoredCount += snapshot.clearingSettlements.size }
                if (snapshot.subscriptionPlans.isNotEmpty()) { db.subscriptionPlanDao().insertPlans(snapshot.subscriptionPlans); restoredCount += snapshot.subscriptionPlans.size }
                if (snapshot.subscriptionRequests.isNotEmpty()) { db.subscriptionRequestDao().insertRequests(snapshot.subscriptionRequests); restoredCount += snapshot.subscriptionRequests.size }
                if (snapshot.promoAnnouncements.isNotEmpty()) { db.promoAnnouncementDao().insertAnnouncements(snapshot.promoAnnouncements); restoredCount += snapshot.promoAnnouncements.size }
                if (snapshot.merchantReviews.isNotEmpty()) { db.merchantReviewDao().insertReviews(snapshot.merchantReviews); restoredCount += snapshot.merchantReviews.size }
                if (snapshot.merchantProducts.isNotEmpty()) { db.merchantStoreDao().insertProducts(snapshot.merchantProducts); restoredCount += snapshot.merchantProducts.size }
                if (snapshot.merchantDailySales.isNotEmpty()) { db.merchantStoreDao().insertSales(snapshot.merchantDailySales); restoredCount += snapshot.merchantDailySales.size }
                if (snapshot.merchantClosingReports.isNotEmpty()) { db.merchantStoreDao().insertClosingReports(snapshot.merchantClosingReports); restoredCount += snapshot.merchantClosingReports.size }
                if (snapshot.priceAlerts.isNotEmpty()) { db.priceAlertDao().insertAlerts(snapshot.priceAlerts); restoredCount += snapshot.priceAlerts.size }
                if (snapshot.hiddenProfiles.isNotEmpty()) { db.hiddenProfileDao().insertHiddenProfiles(snapshot.hiddenProfiles); restoredCount += snapshot.hiddenProfiles.size }
                if (snapshot.hiddenSubAccounts.isNotEmpty()) { db.hiddenSubAccountDao().insertSubAccounts(snapshot.hiddenSubAccounts); restoredCount += snapshot.hiddenSubAccounts.size }
                if (snapshot.dailyRecords.isNotEmpty()) { db.dailyAccountingRecordDao().insertRecords(snapshot.dailyRecords); restoredCount += snapshot.dailyRecords.size }
                if (snapshot.attendance.isNotEmpty()) { db.employeeAttendanceDao().insertAll(snapshot.attendance); restoredCount += snapshot.attendance.size }
                if (snapshot.complaints.isNotEmpty()) { db.financialComplaintDao().insertComplaints(snapshot.complaints); restoredCount += snapshot.complaints.size }
                if (snapshot.transactions.isNotEmpty()) { db.transactionDao().insertTransactions(snapshot.transactions); restoredCount += snapshot.transactions.size }
                if (snapshot.purchases.isNotEmpty()) { db.goldPurchaseDao().insertPurchases(snapshot.purchases); restoredCount += snapshot.purchases.size }
                if (snapshot.traderRequests.isNotEmpty()) { db.traderRequestDao().insertTraderRequests(snapshot.traderRequests); restoredCount += snapshot.traderRequests.size }
                snapshot.adminConfigs.firstOrNull()?.let { db.adminConfigDao().saveConfig(it); restoredCount++ }
                if (snapshot.invoices.isNotEmpty()) { db.invoiceDao().insertInvoices(snapshot.invoices); restoredCount += snapshot.invoices.size }
                if (snapshot.projectTasks.isNotEmpty()) { db.projectTaskDao().insertTasks(snapshot.projectTasks); restoredCount += snapshot.projectTasks.size }
                if (snapshot.shiftHandovers.isNotEmpty()) { db.shiftHandoverDao().insertShiftHandovers(snapshot.shiftHandovers); restoredCount += snapshot.shiftHandovers.size }
                if (snapshot.dynamicTemplates.isNotEmpty()) { db.dynamicExcelDao().insertTemplates(snapshot.dynamicTemplates); restoredCount += snapshot.dynamicTemplates.size }
                if (snapshot.dynamicCustomFields.isNotEmpty()) { db.dynamicExcelDao().insertCustomFields(snapshot.dynamicCustomFields); restoredCount += snapshot.dynamicCustomFields.size }
                if (snapshot.dynamicRowRecords.isNotEmpty()) { db.dynamicExcelDao().insertRows(snapshot.dynamicRowRecords); restoredCount += snapshot.dynamicRowRecords.size }
                if (snapshot.businessProfiles.isNotEmpty()) { db.businessHrDao().insertBusinessProfiles(snapshot.businessProfiles); restoredCount += snapshot.businessProfiles.size }
                if (snapshot.payrollAdjustments.isNotEmpty()) { db.businessHrDao().insertPayrollAdjustments(snapshot.payrollAdjustments); restoredCount += snapshot.payrollAdjustments.size }
                if (snapshot.employeeLeaves.isNotEmpty()) { db.businessHrDao().insertEmployeeLeaves(snapshot.employeeLeaves); restoredCount += snapshot.employeeLeaves.size }
            }

            // Write verified media files after transaction commits
            for ((file, bytes) in stagedMedia) {
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { it.write(bytes) }
            }

            BackupResult.Success(
                message = "تمت الاستعادة والتحقق من السلامة بنجاح: $restoredCount سجل و ${stagedMedia.size} ملف وسائط!",
                recordsCount = restoredCount,
                backupStats = BackupStats(
                    chatsCount = snapshot.chats.size,
                    messagesCount = snapshot.messages.size,
                    recordsCount = snapshot.dailyRecords.size,
                    subProfilesCount = snapshot.hiddenProfiles.size + snapshot.hiddenSubAccounts.size,
                    workGroupsCount = snapshot.workGroups.size,
                    projectsCount = snapshot.projects.size,
                    mediaFilesCount = stagedMedia.size,
                    totalRecordsCount = restoredCount
                )
            )
        } catch (e: Exception) {
            BackupResult.Error("فشلت الاستعادة الذرية للنسخة V3: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Backward-compatible atomic restoration for V1/V2 .oozox archives inside a single Room transaction.
     */
    private suspend fun restoreLegacyV1V2Json(db: AppDatabase, rootJson: JSONObject): BackupResult {
        var restoredRecords = 0
        db.withTransaction {
            rootJson.optJSONArray("hiddenProfiles")?.let { arr ->
                for (i in 0 until arr.length()) { db.hiddenProfileDao().insertHiddenProfile(jsonToHiddenProfile(arr.getJSONObject(i))); restoredRecords++ }
            }
            rootJson.optJSONArray("workGroups")?.let { arr ->
                for (i in 0 until arr.length()) { db.workGroupDao().insertWorkGroup(jsonToWorkGroup(arr.getJSONObject(i))); restoredRecords++ }
            }
            rootJson.optJSONArray("groupMembers")?.let { arr ->
                for (i in 0 until arr.length()) { db.workGroupDao().insertGroupMember(jsonToGroupMember(arr.getJSONObject(i))); restoredRecords++ }
            }
            rootJson.optJSONArray("dailyRecords")?.let { arr ->
                for (i in 0 until arr.length()) { db.dailyAccountingRecordDao().insertRecord(jsonToDailyRecord(arr.getJSONObject(i))); restoredRecords++ }
            }
            rootJson.optJSONArray("complaints")?.let { arr ->
                for (i in 0 until arr.length()) { db.financialComplaintDao().insertComplaint(jsonToComplaint(arr.getJSONObject(i))); restoredRecords++ }
            }
            rootJson.optJSONArray("attendance")?.let { arr ->
                for (i in 0 until arr.length()) { db.employeeAttendanceDao().insertAttendance(jsonToAttendance(arr.getJSONObject(i))); restoredRecords++ }
            }
            rootJson.optJSONArray("chats")?.let { arr ->
                for (i in 0 until arr.length()) { db.chatDao().insertChat(jsonToChat(arr.getJSONObject(i))); restoredRecords++ }
            }
            rootJson.optJSONArray("messages")?.let { arr ->
                for (i in 0 until arr.length()) { db.messageDao().insertMessage(jsonToMessage(arr.getJSONObject(i))); restoredRecords++ }
            }
            rootJson.optJSONArray("hashtagAccounts")?.let { arr ->
                for (i in 0 until arr.length()) { db.chatAccountDao().insertHashtagAccount(jsonToHashtagAccount(arr.getJSONObject(i))); restoredRecords++ }
            }
            rootJson.optJSONArray("hashtagEntries")?.let { arr ->
                for (i in 0 until arr.length()) { db.chatAccountDao().insertHashtagEntry(jsonToHashtagEntry(arr.getJSONObject(i))); restoredRecords++ }
            }
            rootJson.optJSONArray("projects")?.let { arr ->
                for (i in 0 until arr.length()) { db.projectDao().insertProject(jsonToProject(arr.getJSONObject(i))); restoredRecords++ }
            }
            rootJson.optJSONArray("transactions")?.let { arr ->
                for (i in 0 until arr.length()) { db.transactionDao().insertTransaction(jsonToTransaction(arr.getJSONObject(i))); restoredRecords++ }
            }
            rootJson.optJSONArray("purchases")?.let { arr ->
                for (i in 0 until arr.length()) { db.goldPurchaseDao().insertPurchase(jsonToPurchase(arr.getJSONObject(i))); restoredRecords++ }
            }
            rootJson.optJSONArray("products")?.let { arr ->
                for (i in 0 until arr.length()) { db.merchantStoreDao().insertProduct(jsonToProduct(arr.getJSONObject(i))); restoredRecords++ }
            }
        }
        return BackupResult.Success(
            message = "تمت استعادة النسخة الاحتياطية المتوافقة بنجاح ($restoredRecords سجل).",
            recordsCount = restoredRecords
        )
    }

    fun shareBackupFile(context: Context, fileUri: Uri, customTitle: String = "نسخة احتياطية مشفرة - OOZOX") {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, customTitle)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "تصدير النسخة الاحتياطية المشفرة (.oozox)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
    }

    // Legacy V1/V2 JSON Readers preserved for backward compatibility
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

    private fun jsonToHashtagAccount(j: JSONObject) = HashtagAccountEntity(
        id = j.getString("id"),
        chatId = j.getString("chatId"),
        accountId = j.getString("accountId"),
        tag = j.getString("tag"),
        creatorId = j.getString("creatorId"),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        status = j.optString("status", HashtagStatus.ACTIVE.name)
    )

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
