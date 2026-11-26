package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.AdminConfigEntity
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.ChatAccountEntity
import com.example.data.model.ChatEntity
import com.example.data.model.ClearingSettlementEntity
import com.example.data.model.CommentEntity
import com.example.data.model.ContactEntity
import com.example.data.model.DailyAccountingRecordEntity
import com.example.data.model.DynamicExcelCustomFieldEntity
import com.example.data.model.DynamicExcelRowRecordEntity
import com.example.data.model.DynamicExcelTemplateEntity
import com.example.data.model.EmployeeAttendanceEntity
import com.example.data.model.EmployeeLeaveEntity
import com.example.data.model.FinancialComplaintEntity
import com.example.data.model.GoldPurchaseEntity
import com.example.data.model.GroupMemberEntity
import com.example.data.model.HashtagAccountEntity
import com.example.data.model.HashtagEntryEntity
import com.example.data.model.HiddenProfileEntity
import com.example.data.model.HiddenSubAccountEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.LedgerEntryEntity
import com.example.data.model.MerchantDailyClosingReportEntity
import com.example.data.model.MerchantDailySaleEntity
import com.example.data.model.MerchantProductEntity
import com.example.data.model.MerchantReviewEntity
import com.example.data.model.MessageEntity
import com.example.data.model.PayrollAdjustmentEntity
import com.example.data.model.PriceAlertEntity
import com.example.data.model.ProjectAuditLogEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectMemberEntity
import com.example.data.model.ProjectTaskEntity
import com.example.data.model.PromoAnnouncementEntity
import com.example.data.model.ShiftHandoverEntity
import com.example.data.model.SubscriptionPlanEntity
import com.example.data.model.SubscriptionRequestEntity
import com.example.data.model.SyncQueueEntity
import com.example.data.model.TraderRequestEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.data.model.WorkGroupEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VaultSnapshotPackage(
    val rootJson: JSONObject,
    val totalRecords: Int,
    val chatsCount: Int,
    val messagesCount: Int,
    val recordsCount: Int,
    val complaintsCount: Int,
    val subProfilesCount: Int,
    val workGroupsCount: Int,
    val projectsCount: Int,
    val mediaFilesCount: Int,
    val omittedMediaCount: Int
)

data class VaultRestoreOutcome(
    val restoredRecordsCount: Int,
    val restoredMediaCount: Int,
    val failedMediaCount: Int,
    val totalEntitiesExpected: Int
)

/**
 * Full-database & local-media snapshot serializer/restorer across all 42 Room tables.
 * Used identically by Vault V3 (.oozox / /api/backups) and ECDH Cross-Device Transfer.
 */
object VaultSnapshotSerializer {

    private const val MAX_SINGLE_MEDIA_BYTES = 12 * 1024 * 1024L // 12 MB
    const val DEFAULT_CLOUD_MEDIA_BUDGET_BYTES = 7_500_000L // ~7.5 MB binary (~10 MB base64, within 15.5MB envelope cap)
    const val DEFAULT_LOCAL_OR_TRANSFER_MEDIA_BUDGET_BYTES = 48_000_000L

    private fun computeSha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun sanitizeFileName(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .takeLast(80)
            .ifBlank { "media_${System.currentTimeMillis()}" }
    }

    private fun extractCandidateFilePaths(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val found = LinkedHashSet<String>()
        val trimmed = raw.trim()
        if (trimmed.startsWith("/") || trimmed.startsWith("file://")) {
            found.add(trimmed.removePrefix("file://"))
        }
        // Handle JSON-embedded media URIs inside message content or metadata
        val regex = Regex("""(file://)?(/[a-zA-Z0-9_./\-]+\.[a-zA-Z0-9]{1,8})""")
        regex.findAll(trimmed).forEach { match ->
            val candidate = match.groupValues.getOrNull(2) ?: ""
            if (candidate.length > 4) found.add(candidate)
        }
        return found.toList()
    }

    suspend fun exportFullSnapshot(
        context: Context,
        db: AppDatabase,
        ownerPhone: String,
        ownerPhoneHash: String,
        migrationType: String = "FULL_VAULT_V3",
        providerLabel: String = "OOZOX_CLOUD",
        maxTotalMediaBytes: Long = DEFAULT_CLOUD_MEDIA_BUDGET_BYTES
    ): VaultSnapshotPackage = withContext(Dispatchers.IO) {
        val cleanPhone = ownerPhone.replace("[^0-9+]".toRegex(), "").trim()
        val now = System.currentTimeMillis()
        val nowText = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date(now))

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
        val transactions = db.transactionDao().getAllTransactionsSync()
        val purchases = db.goldPurchaseDao().getAllPurchasesSync()
        val traderRequests = db.traderRequestDao().getAllTraderRequestsSync()
        val adminConfig = db.adminConfigDao().getConfigSync()
        val projectMembers = db.projectMemberDao().getAllProjectMembersSync()
        val projectAuditLogs = db.projectAuditLogDao().getAllAuditLogsSync()
        val ledgerEntries = db.immutableLedgerDao().getAllLedgerEntriesSync()
        val settlements = db.clearingSettlementDao().getAllSettlementsSync()
        val syncQueue = db.syncQueueDao().getAllSyncItemsSync()
        val subscriptionPlans = db.subscriptionPlanDao().getAllPlansSync()
        val subscriptionRequests = db.subscriptionRequestDao().getAllRequestsSync()
        val promoAnnouncements = db.promoAnnouncementDao().getAllAnnouncementsSync()
        val products = db.merchantStoreDao().getAllProductsSync()
        val merchantSales = db.merchantStoreDao().getAllSalesSync()
        val closingReports = db.merchantStoreDao().getAllClosingReportsSync()
        val merchantReviews = db.merchantReviewDao().getAllReviewsSync()
        val priceAlerts = db.priceAlertDao().getAllAlertsSync()
        val hiddenProfiles = db.hiddenProfileDao().getAllHiddenProfilesSync()
        val hiddenSubAccounts = db.hiddenSubAccountDao().getAllSubAccountsSync()
        val dailyRecords = db.dailyAccountingRecordDao().getAllRecordsSync()
        val attendance = db.employeeAttendanceDao().getAllAttendanceSync()
        val complaints = db.financialComplaintDao().getAllComplaintsSync()
        val invoices = db.invoiceDao().getAllInvoicesSync()
        val projectTasks = db.projectTaskDao().getAllProjectTasksSync()
        val shiftHandovers = db.shiftHandoverDao().getAllShiftHandoversSync()
        val excelTemplates = db.dynamicExcelDao().getAllTemplatesSync()
        val excelCustomFields = db.dynamicExcelDao().getAllCustomFieldsSync()
        val excelRows = db.dynamicExcelDao().getAllRowsSync()
        val businessProfiles = db.businessHrDao().getAllBusinessProfilesSync()
        val payrollAdjustments = db.businessHrDao().getAllPayrollAdjustmentsSync()
        val employeeLeaves = db.businessHrDao().getAllEmployeeLeavesSync()

        // Collect all referenced local media files inside context.filesDir
        val candidatePaths = LinkedHashSet<String>()
        users.forEach { u ->
            candidatePaths.addAll(extractCandidateFilePaths(u.avatarUri))
            candidatePaths.addAll(extractCandidateFilePaths(u.coverUri))
        }
        messages.forEach { m -> candidatePaths.addAll(extractCandidateFilePaths(m.content)) }
        hashtagEntries.forEach { h -> candidatePaths.addAll(extractCandidateFilePaths(h.photoUri)) }
        hiddenProfiles.forEach { p -> candidatePaths.addAll(extractCandidateFilePaths(p.avatarUri)) }
        hiddenSubAccounts.forEach { s -> candidatePaths.addAll(extractCandidateFilePaths(s.avatarUri)) }
        dailyRecords.forEach { d -> candidatePaths.addAll(extractCandidateFilePaths(d.screenshotUri)) }
        complaints.forEach { c -> candidatePaths.addAll(extractCandidateFilePaths(c.employeeRejectionProofUri)) }
        transactions.forEach { t -> candidatePaths.addAll(extractCandidateFilePaths(t.photoUri)) }
        purchases.forEach { g ->
            candidatePaths.addAll(extractCandidateFilePaths(g.photoUri))
            candidatePaths.addAll(extractCandidateFilePaths(g.scalePhotoUri))
        }
        adminConfig?.let { a ->
            candidatePaths.addAll(extractCandidateFilePaths(a.appLogoUri))
        }
        subscriptionRequests.forEach { r -> candidatePaths.addAll(extractCandidateFilePaths(r.receiptPhotoUri)) }
        merchantReviews.forEach { r -> candidatePaths.addAll(extractCandidateFilePaths(r.reviewerAvatarUri)) }
        products.forEach { p -> candidatePaths.addAll(extractCandidateFilePaths(p.photoUri)) }
        projectTasks.forEach { t ->
            candidatePaths.addAll(extractCandidateFilePaths(t.mediaAttachmentUri))
            candidatePaths.addAll(extractCandidateFilePaths(t.workerResponseUri))
        }
        excelRows.forEach { r ->
            candidatePaths.addAll(extractCandidateFilePaths(r.photoUri))
            candidatePaths.addAll(extractCandidateFilePaths(r.videoUri))
        }

        // Also sweep local media folders (skipping backups/ to avoid recursive nesting)
        val mediaDirs = listOf("media", "voice", "chat_media", "attachments", "images", "documents", "restored_media")
        val filesRoot = context.filesDir.canonicalFile
        val filesRootPath = filesRoot.path
        mediaDirs.forEach { dirName ->
            val subDir = File(filesRoot, dirName)
            if (subDir.exists() && subDir.isDirectory) {
                subDir.walkTopDown().filter { it.isFile }.forEach { f ->
                    candidatePaths.add(f.absolutePath)
                }
            }
        }

        val mediaArray = JSONArray()
        val packedCanonicalPaths = HashSet<String>()
        var accumulatedMediaBytes = 0L
        var omittedMediaCount = 0

        for ((index, rawPath) in candidatePaths.withIndex()) {
            val file = try {
                val parsed = if (rawPath.startsWith("content://")) null else File(rawPath)
                parsed?.canonicalFile
            } catch (_: Exception) {
                null
            } ?: continue

            if (!file.exists() || !file.isFile) continue
            // Security boundary: never pack outside app's internal filesDir or backup archives
            if (!file.path.startsWith(filesRootPath)) continue
            if (file.path.contains("${File.separator}backups${File.separator}")) continue
            if (!packedCanonicalPaths.add(file.path)) continue

            val fileSize = file.length()
            if (fileSize <= 0L || fileSize > MAX_SINGLE_MEDIA_BYTES) {
                omittedMediaCount++
                continue
            }
            if (accumulatedMediaBytes + fileSize > maxTotalMediaBytes) {
                omittedMediaCount++
                continue
            }

            try {
                val bytes = file.readBytes()
                val sha = computeSha256Hex(bytes)
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val relName = file.relativeTo(filesRoot).path.replace(File.separatorChar, '_')
                mediaArray.put(JSONObject().apply {
                    put("id", "media_${index}_${sha.take(10)}")
                    put("originalPath", file.absolutePath)
                    put("rawPath", rawPath)
                    put("relativeName", sanitizeFileName(relName.ifBlank { file.name }))
                    put("sha256", sha)
                    put("sizeBytes", fileSize)
                    put("base64", b64)
                })
                accumulatedMediaBytes += fileSize
            } catch (_: Exception) {
                omittedMediaCount++
            }
        }

        val rootJson = JSONObject().apply {
            put("version", 3)
            put("migrationType", migrationType)
            put("createdAt", now)
            put("createdAtText", nowText)
            put("ownerPhoneHash", ownerPhoneHash)
            put("ownerPhoneMasked", if (cleanPhone.length > 4) "${cleanPhone.take(3)}****${cleanPhone.takeLast(3)}" else cleanPhone)
            put("provider", providerLabel)

            // All 42 Room tables
            put("users", JSONArray().apply { users.forEach { put(VaultEntityMappers.userToJson(it)) } })
            put("contacts", JSONArray().apply { contacts.forEach { put(VaultEntityMappers.contactToJson(it)) } })
            put("chats", JSONArray().apply { chats.forEach { put(VaultEntityMappers.chatToJson(it)) } })
            put("messages", JSONArray().apply { messages.forEach { put(VaultEntityMappers.messageToJson(it)) } })
            put("chatAccounts", JSONArray().apply { chatAccounts.forEach { put(VaultEntityMappers.chatAccountToJson(it)) } })
            put("hashtagAccounts", JSONArray().apply { hashtagAccounts.forEach { put(VaultEntityMappers.hashtagAccountToJson(it)) } })
            put("hashtagEntries", JSONArray().apply { hashtagEntries.forEach { put(VaultEntityMappers.hashtagEntryToJson(it)) } })
            put("comments", JSONArray().apply { comments.forEach { put(VaultEntityMappers.commentToJson(it)) } })
            put("workGroups", JSONArray().apply { workGroups.forEach { put(VaultEntityMappers.workGroupToJson(it)) } })
            put("groupMembers", JSONArray().apply { groupMembers.forEach { put(VaultEntityMappers.groupMemberToJson(it)) } })
            put("projects", JSONArray().apply { projects.forEach { put(VaultEntityMappers.projectToJson(it)) } })
            put("transactions", JSONArray().apply { transactions.forEach { put(VaultEntityMappers.transactionToJson(it)) } })
            put("purchases", JSONArray().apply { purchases.forEach { put(VaultEntityMappers.purchaseToJson(it)) } })
            put("traderRequests", JSONArray().apply { traderRequests.forEach { put(VaultEntityMappers.traderRequestToJson(it)) } })
            put("adminConfigs", JSONArray().apply { if (adminConfig != null) put(VaultEntityMappers.adminConfigToJson(adminConfig)) })
            put("projectMembers", JSONArray().apply { projectMembers.forEach { put(VaultEntityMappers.projectMemberToJson(it)) } })
            put("projectAuditLogs", JSONArray().apply { projectAuditLogs.forEach { put(VaultEntityMappers.projectAuditLogToJson(it)) } })
            put("ledgerEntries", JSONArray().apply { ledgerEntries.forEach { put(VaultEntityMappers.ledgerEntryToJson(it)) } })
            put("clearingSettlements", JSONArray().apply { settlements.forEach { put(VaultEntityMappers.settlementToJson(it)) } })
            put("syncQueue", JSONArray().apply { syncQueue.forEach { put(VaultEntityMappers.syncQueueToJson(it)) } })
            put("subscriptionPlans", JSONArray().apply { subscriptionPlans.forEach { put(VaultEntityMappers.subscriptionPlanToJson(it)) } })
            put("subscriptionRequests", JSONArray().apply { subscriptionRequests.forEach { put(VaultEntityMappers.subscriptionRequestToJson(it)) } })
            put("promoAnnouncements", JSONArray().apply { promoAnnouncements.forEach { put(VaultEntityMappers.promoAnnouncementToJson(it)) } })
            put("products", JSONArray().apply { products.forEach { put(VaultEntityMappers.productToJson(it)) } })
            put("merchantSales", JSONArray().apply { merchantSales.forEach { put(VaultEntityMappers.merchantSaleToJson(it)) } })
            put("closingReports", JSONArray().apply { closingReports.forEach { put(VaultEntityMappers.closingReportToJson(it)) } })
            put("merchantReviews", JSONArray().apply { merchantReviews.forEach { put(VaultEntityMappers.merchantReviewToJson(it)) } })
            put("priceAlerts", JSONArray().apply { priceAlerts.forEach { put(VaultEntityMappers.priceAlertToJson(it)) } })
            put("hiddenProfiles", JSONArray().apply { hiddenProfiles.forEach { put(VaultEntityMappers.hiddenProfileToJson(it)) } })
            put("hiddenSubAccounts", JSONArray().apply { hiddenSubAccounts.forEach { put(VaultEntityMappers.hiddenSubAccountToJson(it)) } })
            put("dailyRecords", JSONArray().apply { dailyRecords.forEach { put(VaultEntityMappers.dailyRecordToJson(it)) } })
            put("attendance", JSONArray().apply { attendance.forEach { put(VaultEntityMappers.attendanceToJson(it)) } })
            put("complaints", JSONArray().apply { complaints.forEach { put(VaultEntityMappers.complaintToJson(it)) } })
            put("invoices", JSONArray().apply { invoices.forEach { put(VaultEntityMappers.invoiceToJson(it)) } })
            put("projectTasks", JSONArray().apply { projectTasks.forEach { put(VaultEntityMappers.projectTaskToJson(it)) } })
            put("shiftHandovers", JSONArray().apply { shiftHandovers.forEach { put(VaultEntityMappers.shiftHandoverToJson(it)) } })
            put("excelTemplates", JSONArray().apply { excelTemplates.forEach { put(VaultEntityMappers.excelTemplateToJson(it)) } })
            put("excelCustomFields", JSONArray().apply { excelCustomFields.forEach { put(VaultEntityMappers.excelCustomFieldToJson(it)) } })
            put("excelRows", JSONArray().apply { excelRows.forEach { put(VaultEntityMappers.excelRowToJson(it)) } })
            put("businessProfiles", JSONArray().apply { businessProfiles.forEach { put(VaultEntityMappers.businessProfileToJson(it)) } })
            put("payrollAdjustments", JSONArray().apply { payrollAdjustments.forEach { put(VaultEntityMappers.payrollAdjustmentToJson(it)) } })
            put("employeeLeaves", JSONArray().apply { employeeLeaves.forEach { put(VaultEntityMappers.employeeLeaveToJson(it)) } })

            put("mediaFiles", mediaArray)
            put("mediaStats", JSONObject().apply {
                put("includedCount", mediaArray.length())
                put("omittedCount", omittedMediaCount)
                put("includedBytes", accumulatedMediaBytes)
            })
        }

        val totalRecords = users.size + contacts.size + chats.size + messages.size +
                chatAccounts.size + hashtagAccounts.size + hashtagEntries.size + comments.size +
                workGroups.size + groupMembers.size + projects.size + transactions.size +
                purchases.size + traderRequests.size + (if (adminConfig != null) 1 else 0) +
                projectMembers.size + projectAuditLogs.size + ledgerEntries.size + settlements.size +
                syncQueue.size + subscriptionPlans.size + subscriptionRequests.size +
                promoAnnouncements.size + products.size + merchantSales.size + closingReports.size +
                merchantReviews.size + priceAlerts.size + hiddenProfiles.size + hiddenSubAccounts.size +
                dailyRecords.size + attendance.size + complaints.size + invoices.size +
                projectTasks.size + shiftHandovers.size + excelTemplates.size + excelCustomFields.size +
                excelRows.size + businessProfiles.size + payrollAdjustments.size + employeeLeaves.size

        VaultSnapshotPackage(
            rootJson = rootJson,
            totalRecords = totalRecords,
            chatsCount = chats.size,
            messagesCount = messages.size,
            recordsCount = dailyRecords.size,
            complaintsCount = complaints.size,
            subProfilesCount = hiddenProfiles.size + hiddenSubAccounts.size,
            workGroupsCount = workGroups.size,
            projectsCount = projects.size,
            mediaFilesCount = mediaArray.length(),
            omittedMediaCount = omittedMediaCount
        )
    }

    /**
     * Extracts bundled media files (verifying SHA-256), builds URI rewrite map, and atomically restores
     * all Room tables inside db.withTransaction { ... }. Supports V1, V2, and V3 snapshots.
     */
    suspend fun restoreFullSnapshot(
        context: Context,
        db: AppDatabase,
        rootJson: JSONObject,
        preserveCurrentUser: Boolean = true
    ): VaultRestoreOutcome = withContext(Dispatchers.IO) {
        val uriRewriteMap = LinkedHashMap<String, String>()
        var restoredMedia = 0
        var failedMedia = 0

        val restoredMediaDir = File(context.filesDir, "restored_media").apply { mkdirs() }
        val mediaArr = rootJson.optJSONArray("mediaFiles")
        if (mediaArr != null) {
            for (i in 0 until mediaArr.length()) {
                try {
                    val mObj = mediaArr.getJSONObject(i)
                    val originalPath = mObj.optString("originalPath")
                    val rawPath = mObj.optString("rawPath")
                    val expectedSha = mObj.optString("sha256")
                    val base64Str = mObj.optString("base64")
                    val relName = sanitizeFileName(mObj.optString("relativeName", "media_$i.bin"))
                    if (base64Str.isBlank()) {
                        failedMedia++
                        continue
                    }
                    val bytes = Base64.decode(base64Str, Base64.NO_WRAP)
                    val actualSha = computeSha256Hex(bytes)
                    if (expectedSha.isNotBlank() && !expectedSha.equals(actualSha, ignoreCase = true)) {
                        failedMedia++
                        continue
                    }
                    val targetFile = File(restoredMediaDir, "${actualSha.take(12)}_$relName")
                    if (!targetFile.exists() || targetFile.length() != bytes.size.toLong()) {
                        FileOutputStream(targetFile).use { it.write(bytes) }
                    }
                    val destAbs = targetFile.absolutePath
                    if (originalPath.isNotBlank()) {
                        uriRewriteMap[originalPath] = destAbs
                        uriRewriteMap["file://$originalPath"] = destAbs
                    }
                    if (rawPath.isNotBlank()) {
                        uriRewriteMap[rawPath] = destAbs
                    }
                    restoredMedia++
                } catch (_: Exception) {
                    failedMedia++
                }
            }
        }

        val users = parseArray(rootJson, "users") { VaultEntityMappers.jsonToUser(it, uriRewriteMap) }
        val contacts = parseArray(rootJson, "contacts") { VaultEntityMappers.jsonToContact(it) }
        val chats = parseArray(rootJson, "chats") { VaultEntityMappers.jsonToChat(it) }
        val messages = parseArray(rootJson, "messages") { VaultEntityMappers.jsonToMessage(it, uriRewriteMap) }
        val chatAccounts = parseArray(rootJson, "chatAccounts") { VaultEntityMappers.jsonToChatAccount(it) }
        val hashtagAccounts = parseArray(rootJson, "hashtagAccounts") { VaultEntityMappers.jsonToHashtagAccount(it) }
        val hashtagEntries = parseArray(rootJson, "hashtagEntries") { VaultEntityMappers.jsonToHashtagEntry(it, uriRewriteMap) }
        val comments = parseArray(rootJson, "comments") { VaultEntityMappers.jsonToComment(it) }
        val workGroups = parseArray(rootJson, "workGroups") { VaultEntityMappers.jsonToWorkGroup(it) }
        val groupMembers = parseArray(rootJson, "groupMembers") { VaultEntityMappers.jsonToGroupMember(it) }
        val projects = parseArray(rootJson, "projects") { VaultEntityMappers.jsonToProject(it) }
        val transactions = parseArray(rootJson, "transactions") { VaultEntityMappers.jsonToTransaction(it, uriRewriteMap) }
        val purchases = parseArray(rootJson, "purchases") { VaultEntityMappers.jsonToPurchase(it, uriRewriteMap) }
        val traderRequests = parseArray(rootJson, "traderRequests") { VaultEntityMappers.jsonToTraderRequest(it) }
        val adminConfigs = parseArray(rootJson, "adminConfigs") { VaultEntityMappers.jsonToAdminConfig(it, uriRewriteMap) }
        val projectMembers = parseArray(rootJson, "projectMembers") { VaultEntityMappers.jsonToProjectMember(it) }
        val projectAuditLogs = parseArray(rootJson, "projectAuditLogs") { VaultEntityMappers.jsonToProjectAuditLog(it) }
        val ledgerEntries = parseArray(rootJson, "ledgerEntries") { VaultEntityMappers.jsonToLedgerEntry(it) }
        val settlements = parseArray(rootJson, "clearingSettlements") { VaultEntityMappers.jsonToSettlement(it) }
        val syncQueue = parseArray(rootJson, "syncQueue") { VaultEntityMappers.jsonToSyncQueue(it) }
        val subscriptionPlans = parseArray(rootJson, "subscriptionPlans") { VaultEntityMappers.jsonToSubscriptionPlan(it) }
        val subscriptionRequests = parseArray(rootJson, "subscriptionRequests") { VaultEntityMappers.jsonToSubscriptionRequest(it, uriRewriteMap) }
        val promoAnnouncements = parseArray(rootJson, "promoAnnouncements") { VaultEntityMappers.jsonToPromoAnnouncement(it) }
        val products = parseArray(rootJson, "products") { VaultEntityMappers.jsonToProduct(it, uriRewriteMap) }
        val merchantSales = parseArray(rootJson, "merchantSales") { VaultEntityMappers.jsonToMerchantSale(it) }
        val closingReports = parseArray(rootJson, "closingReports") { VaultEntityMappers.jsonToClosingReport(it) }
        val merchantReviews = parseArray(rootJson, "merchantReviews") { VaultEntityMappers.jsonToMerchantReview(it, uriRewriteMap) }
        val priceAlerts = parseArray(rootJson, "priceAlerts") { VaultEntityMappers.jsonToPriceAlert(it) }
        val hiddenProfiles = parseArray(rootJson, "hiddenProfiles") { VaultEntityMappers.jsonToHiddenProfile(it, uriRewriteMap) }
        val hiddenSubAccounts = parseArray(rootJson, "hiddenSubAccounts") { VaultEntityMappers.jsonToHiddenSubAccount(it, uriRewriteMap) }
        val dailyRecords = parseArray(rootJson, "dailyRecords") { VaultEntityMappers.jsonToDailyRecord(it, uriRewriteMap) }
        val attendance = parseArray(rootJson, "attendance") { VaultEntityMappers.jsonToAttendance(it) }
        val complaints = parseArray(rootJson, "complaints") { VaultEntityMappers.jsonToComplaint(it, uriRewriteMap) }
        val invoices = parseArray(rootJson, "invoices") { VaultEntityMappers.jsonToInvoice(it) }
        val projectTasks = parseArray(rootJson, "projectTasks") { VaultEntityMappers.jsonToProjectTask(it, uriRewriteMap) }
        val shiftHandovers = parseArray(rootJson, "shiftHandovers") { VaultEntityMappers.jsonToShiftHandover(it) }
        val excelTemplates = parseArray(rootJson, "excelTemplates") { VaultEntityMappers.jsonToExcelTemplate(it) }
        val excelCustomFields = parseArray(rootJson, "excelCustomFields") { VaultEntityMappers.jsonToExcelCustomField(it) }
        val excelRows = parseArray(rootJson, "excelRows") { VaultEntityMappers.jsonToExcelRow(it, uriRewriteMap) }
        val businessProfiles = parseArray(rootJson, "businessProfiles") { VaultEntityMappers.jsonToBusinessProfile(it) }
        val payrollAdjustments = parseArray(rootJson, "payrollAdjustments") { VaultEntityMappers.jsonToPayrollAdjustment(it) }
        val employeeLeaves = parseArray(rootJson, "employeeLeaves") { VaultEntityMappers.jsonToEmployeeLeave(it) }

        var expectedCount = 0
        var insertedCount = 0

        db.withTransaction {
            val activeCurrentUser = if (preserveCurrentUser) db.userDao().getCurrentUserSync() else null
            val usersToInsert = if (activeCurrentUser != null) {
                users.filter { it.id == activeCurrentUser.id }
            } else {
                users
            }
            if (usersToInsert.isNotEmpty()) {
                db.userDao().insertUsers(usersToInsert)
                insertedCount += usersToInsert.size
            }
            expectedCount += usersToInsert.size

            inlineRestore(contacts, db.contactDao()::insertContacts).let { expectedCount += it; insertedCount += it }
            inlineRestore(chats, db.chatDao()::insertChats).let { expectedCount += it; insertedCount += it }
            inlineRestore(messages, db.messageDao()::insertMessages).let { expectedCount += it; insertedCount += it }
            inlineRestore(chatAccounts, db.chatAccountDao()::insertAccounts).let { expectedCount += it; insertedCount += it }
            inlineRestore(hashtagAccounts, db.chatAccountDao()::insertHashtagAccounts).let { expectedCount += it; insertedCount += it }
            inlineRestore(hashtagEntries, db.chatAccountDao()::insertHashtagEntries).let { expectedCount += it; insertedCount += it }
            inlineRestore(comments, db.chatAccountDao()::insertComments).let { expectedCount += it; insertedCount += it }
            inlineRestore(workGroups, db.workGroupDao()::insertWorkGroups).let { expectedCount += it; insertedCount += it }
            inlineRestore(groupMembers, db.workGroupDao()::insertGroupMembers).let { expectedCount += it; insertedCount += it }
            inlineRestore(projects, db.projectDao()::insertProjects).let { expectedCount += it; insertedCount += it }
            inlineRestore(transactions, db.transactionDao()::insertTransactions).let { expectedCount += it; insertedCount += it }
            inlineRestore(purchases, db.goldPurchaseDao()::insertPurchases).let { expectedCount += it; insertedCount += it }
            inlineRestore(traderRequests, db.traderRequestDao()::insertTraderRequests).let { expectedCount += it; insertedCount += it }
            adminConfigs.lastOrNull()?.let { cfg ->
                db.adminConfigDao().saveConfig(cfg)
                expectedCount += 1
                insertedCount += 1
            }
            inlineRestore(projectMembers, db.projectMemberDao()::insertMembers).let { expectedCount += it; insertedCount += it }
            inlineRestore(projectAuditLogs, db.projectAuditLogDao()::insertAuditLogs).let { expectedCount += it; insertedCount += it }
            if (ledgerEntries.isNotEmpty()) {
                db.immutableLedgerDao().insertEntriesIgnore(ledgerEntries)
                expectedCount += ledgerEntries.size
                insertedCount += ledgerEntries.size
            }
            inlineRestore(settlements, db.clearingSettlementDao()::insertSettlements).let { expectedCount += it; insertedCount += it }
            inlineRestore(syncQueue, db.syncQueueDao()::insertSyncItems).let { expectedCount += it; insertedCount += it }
            inlineRestore(subscriptionPlans, db.subscriptionPlanDao()::insertPlans).let { expectedCount += it; insertedCount += it }
            inlineRestore(subscriptionRequests, db.subscriptionRequestDao()::insertRequests).let { expectedCount += it; insertedCount += it }
            inlineRestore(promoAnnouncements, db.promoAnnouncementDao()::insertAnnouncements).let { expectedCount += it; insertedCount += it }
            inlineRestore(products, db.merchantStoreDao()::insertProducts).let { expectedCount += it; insertedCount += it }
            inlineRestore(merchantSales, db.merchantStoreDao()::insertSales).let { expectedCount += it; insertedCount += it }
            inlineRestore(closingReports, db.merchantStoreDao()::insertClosingReports).let { expectedCount += it; insertedCount += it }
            inlineRestore(merchantReviews, db.merchantReviewDao()::insertReviews).let { expectedCount += it; insertedCount += it }
            inlineRestore(priceAlerts, db.priceAlertDao()::insertAlerts).let { expectedCount += it; insertedCount += it }
            inlineRestore(hiddenProfiles, db.hiddenProfileDao()::insertHiddenProfiles).let { expectedCount += it; insertedCount += it }
            inlineRestore(hiddenSubAccounts, db.hiddenSubAccountDao()::insertSubAccounts).let { expectedCount += it; insertedCount += it }
            inlineRestore(dailyRecords, db.dailyAccountingRecordDao()::insertRecords).let { expectedCount += it; insertedCount += it }
            inlineRestore(attendance, db.employeeAttendanceDao()::insertAll).let { expectedCount += it; insertedCount += it }
            inlineRestore(complaints, db.financialComplaintDao()::insertComplaints).let { expectedCount += it; insertedCount += it }
            inlineRestore(invoices, db.invoiceDao()::insertInvoices).let { expectedCount += it; insertedCount += it }
            inlineRestore(projectTasks, db.projectTaskDao()::insertTasks).let { expectedCount += it; insertedCount += it }
            inlineRestore(shiftHandovers, db.shiftHandoverDao()::insertShiftHandovers).let { expectedCount += it; insertedCount += it }
            inlineRestore(excelTemplates, db.dynamicExcelDao()::insertTemplates).let { expectedCount += it; insertedCount += it }
            inlineRestore(excelCustomFields, db.dynamicExcelDao()::insertCustomFields).let { expectedCount += it; insertedCount += it }
            inlineRestore(excelRows, db.dynamicExcelDao()::insertRows).let { expectedCount += it; insertedCount += it }
            inlineRestore(businessProfiles, db.businessHrDao()::insertBusinessProfiles).let { expectedCount += it; insertedCount += it }
            inlineRestore(payrollAdjustments, db.businessHrDao()::insertPayrollAdjustments).let { expectedCount += it; insertedCount += it }
            inlineRestore(employeeLeaves, db.businessHrDao()::insertEmployeeLeaves).let { expectedCount += it; insertedCount += it }
        }

        VaultRestoreOutcome(
            restoredRecordsCount = insertedCount,
            restoredMediaCount = restoredMedia,
            failedMediaCount = failedMedia,
            totalEntitiesExpected = expectedCount
        )
    }

    private inline fun <T> parseArray(
        rootJson: JSONObject,
        key: String,
        mapper: (JSONObject) -> T
    ): List<T> {
        val arr = rootJson.optJSONArray(key) ?: return emptyList()
        val list = ArrayList<T>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            list.add(mapper(obj))
        }
        return list
    }

    private suspend inline fun <T> inlineRestore(
        items: List<T>,
        crossinline inserter: suspend (List<T>) -> Unit
    ): Int {
        if (items.isEmpty()) return 0
        inserter(items)
        return items.size
    }
}
