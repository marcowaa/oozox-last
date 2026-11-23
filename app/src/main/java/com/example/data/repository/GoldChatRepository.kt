package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.util.GoldUtils
import com.example.util.HashtagUtils
import com.example.util.NotificationHelper
import com.example.util.OfflineSyncManager
import com.example.util.RoomLocalAES256Engine
import com.example.util.SmartExcelTemplateEngine
import com.example.util.SmartSpreadsheetRow
import com.example.util.SmartSpreadsheetTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class GoldChatRepository(context: Context) {
    private val appContext = context.applicationContext
    val db = AppDatabase.getInstance(context)

    val currentUser: Flow<UserEntity?> = db.userDao().getCurrentUser()
    val allUsers: Flow<List<UserEntity>> = db.userDao().getAllUsers()
    val allContacts: Flow<List<ContactEntity>> = db.contactDao().getAllContacts()
    val allChats: Flow<List<ChatEntity>> = db.chatDao().getAllChats()
    val allTransactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactions()
    val allGoldPurchases: Flow<List<GoldPurchaseEntity>> = db.goldPurchaseDao().getAllPurchases()
    val allWorkGroups: Flow<List<WorkGroupEntity>> = db.workGroupDao().getAllWorkGroups()
    val allGroupMembers: Flow<List<GroupMemberEntity>> = db.workGroupDao().getAllGroupMembers()
    val allProjects: Flow<List<ProjectEntity>> = db.projectDao().getAllProjects()
    val allProjectMembers: Flow<List<ProjectMemberEntity>> = db.projectMemberDao().getAllProjectMembers()
    val allTraderRequests: Flow<List<TraderRequestEntity>> = db.traderRequestDao().getAllTraderRequests()
    val adminConfig: Flow<AdminConfigEntity?> = db.adminConfigDao().getConfig()
    val allLedgerEntries: Flow<List<LedgerEntryEntity>> = db.immutableLedgerDao().getAllLedgerEntries()
    val allSettlements: Flow<List<ClearingSettlementEntity>> = db.clearingSettlementDao().getAllSettlements()
    val pendingSyncItems: Flow<List<SyncQueueEntity>> = db.syncQueueDao().getPendingItems()
    val allSubscriptionPlans: Flow<List<SubscriptionPlanEntity>> = db.subscriptionPlanDao().getAllPlans()
    val allSubscriptionRequests: Flow<List<SubscriptionRequestEntity>> = db.subscriptionRequestDao().getAllRequests()
    val activePromoAnnouncements: Flow<List<PromoAnnouncementEntity>> = db.promoAnnouncementDao().getActiveAnnouncements()
    val allPromoAnnouncements: Flow<List<PromoAnnouncementEntity>> = db.promoAnnouncementDao().getAllAnnouncements()
    val allMerchantProducts: Flow<List<MerchantProductEntity>> = db.merchantStoreDao().getAllProducts()
    val allChatAccounts: Flow<List<ChatAccountEntity>> = db.chatAccountDao().getAllChatAccounts()
    val allHashtagAccounts: Flow<List<HashtagAccountEntity>> = db.chatAccountDao().getAllHashtagAccounts()
    val allHashtagEntries: Flow<List<HashtagEntryEntity>> = db.chatAccountDao().getAllHashtagEntries()
    val allPriceAlerts: Flow<List<PriceAlertEntity>> = db.priceAlertDao().getAllAlerts()
    val allHiddenProfiles: Flow<List<HiddenProfileEntity>> = db.hiddenProfileDao().getAllHiddenProfiles().map { list ->
        list.map { RoomLocalAES256Engine.decryptHiddenProfile(it, appContext) }
    }.flowOn(Dispatchers.IO)
    val allSubAccounts: Flow<List<HiddenSubAccountEntity>> = db.hiddenSubAccountDao().getAllSubAccounts()
    fun getMainChats(): Flow<List<ChatEntity>> = db.chatDao().getMainChats()
    fun getSubAccountChats(subAccountId: String): Flow<List<ChatEntity>> = db.chatDao().getSubAccountChats(subAccountId)
    val allDailyAccountingRecords: Flow<List<DailyAccountingRecordEntity>> = db.dailyAccountingRecordDao().getAllRecords().map { list ->
        list.map { RoomLocalAES256Engine.decryptDailyRecord(it, null, appContext) }
    }.flowOn(Dispatchers.IO)
    val allFinancialComplaints: Flow<List<FinancialComplaintEntity>> = db.financialComplaintDao().getAllComplaints().map { list ->
        list.map { RoomLocalAES256Engine.decryptComplaint(it, null, appContext) }
    }.flowOn(Dispatchers.IO)
    val allInvoices: Flow<List<InvoiceEntity>> = db.invoiceDao().getAllInvoices()
    val allProjectTasks: Flow<List<ProjectTaskEntity>> = db.projectTaskDao().getAllProjectTasks()
    val allShiftHandovers: Flow<List<ShiftHandoverEntity>> = db.shiftHandoverDao().getAllShiftHandovers()
    val businessProfile: Flow<BusinessProfileEntity?> = db.businessHrDao().observeBusinessProfile()
    val allPayrollAdjustments: Flow<List<PayrollAdjustmentEntity>> = db.businessHrDao().getAllPayrollAdjustments()
    val allEmployeeLeaves: Flow<List<EmployeeLeaveEntity>> = db.businessHrDao().getAllEmployeeLeaves()

    fun getTasksForProject(projectId: String): Flow<List<ProjectTaskEntity>> = db.projectTaskDao().getTasksForProject(projectId)
    fun getTaskById(id: String): Flow<ProjectTaskEntity?> = db.projectTaskDao().getTaskById(id)
    fun getInvoicesForProject(projectId: String): Flow<List<InvoiceEntity>> = db.invoiceDao().getInvoicesForProject(projectId)
    fun getInvoiceById(id: String): Flow<InvoiceEntity?> = db.invoiceDao().getInvoiceById(id)
    fun getInvoicesForUser(userId: String): Flow<List<InvoiceEntity>> = db.invoiceDao().getInvoicesForUser(userId)

    fun getComplaintsForGroup(groupId: String): Flow<List<FinancialComplaintEntity>> =
        db.financialComplaintDao().getComplaintsForGroup(groupId).map { list ->
            list.map { RoomLocalAES256Engine.decryptComplaint(it, null, appContext) }
        }.flowOn(Dispatchers.IO)

    fun getComplaintsForDate(date: String): Flow<List<FinancialComplaintEntity>> =
        db.financialComplaintDao().getComplaintsForTargetDate(date).map { list ->
            list.map { RoomLocalAES256Engine.decryptComplaint(it, null, appContext) }
        }.flowOn(Dispatchers.IO)

    fun getProductsByMerchant(merchantId: String): Flow<List<MerchantProductEntity>> = db.merchantStoreDao().getProductsByMerchant(merchantId)
    fun getUnclosedSalesForMerchant(merchantId: String): Flow<List<MerchantDailySaleEntity>> = db.merchantStoreDao().getUnclosedSalesForMerchant(merchantId)
    fun getAllSalesForMerchant(merchantId: String): Flow<List<MerchantDailySaleEntity>> = db.merchantStoreDao().getAllSalesForMerchant(merchantId)
    fun getClosingReportsForMerchant(merchantId: String): Flow<List<MerchantDailyClosingReportEntity>> = db.merchantStoreDao().getClosingReportsForMerchant(merchantId)

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> =
        db.messageDao().getMessagesForChat(chatId).map { list ->
            list.map { RoomLocalAES256Engine.decryptMessage(it, null, appContext) }
        }.flowOn(Dispatchers.IO)

    fun getRecentMessagesForChat(chatId: String, limit: Int = 35): Flow<List<MessageEntity>> =
        db.messageDao().getRecentMessagesForChat(chatId, limit).map { list ->
            list.asReversed().map { RoomLocalAES256Engine.decryptMessage(it, null, appContext) }
        }.flowOn(Dispatchers.IO)

    fun getRecentMessagesForChatDesc(chatId: String, limit: Int = 35): Flow<List<MessageEntity>> =
        db.messageDao().getRecentMessagesForChat(chatId, limit).map { list ->
            list.map { RoomLocalAES256Engine.decryptMessage(it, null, appContext) }
        }.flowOn(Dispatchers.IO)

    suspend fun getOlderMessagesPageKeyset(
        chatId: String,
        beforeTimestamp: Long,
        beforeId: String,
        limit: Int = 30
    ): List<MessageEntity> = withContext(Dispatchers.IO) {
        db.messageDao().getOlderMessagesKeyset(chatId, beforeTimestamp, beforeId, limit)
            .map { RoomLocalAES256Engine.decryptMessage(it, null, appContext) }
    }

    fun getMessageCountForChat(chatId: String): Flow<Int> =
        db.messageDao().getMessageCountForChat(chatId)

    suspend fun getAllMessagesForChatDecrypted(chatId: String): List<MessageEntity> = withContext(Dispatchers.IO) {
        db.messageDao().getAllMessagesForChatSync(chatId)
            .map { RoomLocalAES256Engine.decryptMessage(it, null, appContext) }
    }
    fun getAccountsForChat(chatId: String): Flow<List<ChatAccountEntity>> = db.chatAccountDao().getAccountsForChat(chatId)
    fun getHashtagsForChat(chatId: String): Flow<List<HashtagAccountEntity>> = db.chatAccountDao().getHashtagsForChat(chatId)
    fun getEntriesForHashtag(hashtagId: String): Flow<List<HashtagEntryEntity>> = db.chatAccountDao().getEntriesForHashtag(hashtagId)
    fun getGroupMembers(groupId: String): Flow<List<GroupMemberEntity>> = db.workGroupDao().getGroupMembers(groupId)
    fun getSubGroups(parentGroupId: String): Flow<List<WorkGroupEntity>> = db.workGroupDao().getSubGroups(parentGroupId)
    fun getAccountingRecordsForGroup(groupId: String): Flow<List<DailyAccountingRecordEntity>> =
        db.dailyAccountingRecordDao().getRecordsForGroup(groupId).map { list ->
            list.map { RoomLocalAES256Engine.decryptDailyRecord(it, null, appContext) }
        }.flowOn(Dispatchers.IO)
    fun getAccountingRecordsForProfile(profileId: String): Flow<List<DailyAccountingRecordEntity>> =
        db.dailyAccountingRecordDao().getRecordsForProfile(profileId).map { list ->
            list.map { RoomLocalAES256Engine.decryptDailyRecord(it, null, appContext) }
        }.flowOn(Dispatchers.IO)
    fun getProjectMembers(projectId: String): Flow<List<ProjectMemberEntity>> = db.projectMemberDao().getMembersForProject(projectId)
    fun getProjectAuditLogs(projectId: String): Flow<List<ProjectAuditLogEntity>> = db.projectAuditLogDao().getAuditLogsForProject(projectId)

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingConfig = db.adminConfigDao().getConfig().first()
        if (existingConfig == null) {
            db.adminConfigDao().saveConfig(
                AdminConfigEntity(
                    id = 1,
                    encryptionEnabled = true
                )
            )
        }
    }


    suspend fun clearChatUnread(chatId: String) = withContext(Dispatchers.IO) {
        db.chatDao().clearUnread(chatId)
    }

    suspend fun setChatArchived(chatId: String, isArchived: Boolean) = withContext(Dispatchers.IO) {
        db.chatDao().setChatArchived(chatId, isArchived)
    }

    suspend fun deleteChat(chatId: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.messageDao().deleteMessagesForChat(chatId)
            db.chatDao().deleteChat(chatId)
        }
    }

    suspend fun setChatPinned(chatId: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        db.chatDao().setChatPinned(chatId, isPinned)
    }

    suspend fun updateChatGroup(chatId: String, groupName: String?) = withContext(Dispatchers.IO) {
        val chat = db.chatDao().getChatById(chatId) ?: return@withContext
        val updated = chat.copy(
            groupName = groupName,
            groupId = if (groupName.isNullOrBlank() && !chat.isGroup) null else chat.groupId
        )
        db.chatDao().insertChat(updated)
    }

    suspend fun createGroupChatFolder(groupName: String, selectedChatIds: List<String>) = withContext(Dispatchers.IO) {
        val cleanFolderName = groupName.trim()
        if (cleanFolderName.isEmpty()) return@withContext
        val newFolderId = UUID.randomUUID().toString()
        selectedChatIds.forEach { chatId ->
            val chat = db.chatDao().getChatById(chatId)
            if (chat != null) {
                val updated = chat.copy(
                    groupName = cleanFolderName,
                    groupId = if (chat.isGroup && !chat.groupId.isNullOrBlank()) chat.groupId else newFolderId
                )
                db.chatDao().insertChat(updated)
            }
        }
    }

    suspend fun saveUser(user: UserEntity) = db.userDao().insertUser(user)

    suspend fun getUserByPhone(phone: String): UserEntity? = withContext(Dispatchers.IO) {
        db.userDao().getUserByPhone(phone)
    }

    suspend fun deleteUsers(userIds: List<String>) = withContext(Dispatchers.IO) {
        db.userDao().deleteUsers(userIds)
    }

    suspend fun clearUserSession() = withContext(Dispatchers.IO) {
        db.userDao().clearUsers()
    }

    suspend fun createChat(contact: ContactEntity): ChatEntity = withContext(Dispatchers.IO) {
        val existing = db.chatDao().getAllChats().first().find { it.contactId == contact.id }
        if (existing != null) return@withContext existing

        val newChat = ChatEntity(
            id = UUID.randomUUID().toString(),
            contactId = contact.id,
            contactName = contact.name,
            contactType = contact.type,
            contactVerified = contact.isVerified,
            lastMessage = "بداية المحادثة",
            lastMessageTime = System.currentTimeMillis()
        )
        db.chatDao().insertChat(newChat)
        newChat
    }

    suspend fun insertContacts(contacts: List<ContactEntity>) = withContext(Dispatchers.IO) {
        db.contactDao().insertContacts(contacts)
    }

    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        content: String,
        type: String = MessageType.TEXT.name
    ): MessageEntity = withContext(Dispatchers.IO) {
        val isOnline = OfflineSyncManager.isNetworkAvailable(appContext)
        val initialStatus = if (isOnline) MessageStatus.SENT.name else MessageStatus.PENDING.name
        val plainMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            type = type,
            content = content,
            timestamp = System.currentTimeMillis(),
            status = initialStatus
        )
        db.withTransaction {
            // Store AES-256 encrypted ciphertext in Room SQLite database
            val encryptedMsg = RoomLocalAES256Engine.encryptMessage(plainMsg, null, appContext)
            db.messageDao().insertMessage(encryptedMsg)

            // Queue for synchronization if offline or spotty network
            if (!isOnline) {
                val payload = JSONObject().apply {
                    put("messageId", plainMsg.id)
                    put("chatId", chatId)
                }.toString()
                val syncItem = SyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    actionType = "SEND_MESSAGE",
                    payloadJson = payload,
                    status = "PENDING"
                )
                db.syncQueueDao().insertSyncItem(syncItem)
            }

            val previewText = when (type) {
                MessageType.IMAGE.name -> {
                    val caption = if (content.contains("||")) content.substringAfter("||").trim() else ""
                    if (caption.isNotBlank()) caption else "صورة ملتقطة"
                }
                MessageType.DOCUMENT.name -> {
                    val parts = content.split("||")
                    val fileName = if (parts.size > 1) parts[1].trim() else "مستند"
                    fileName
                }
                MessageType.VOICE.name -> "تسجيل صوتي"
                MessageType.MISSED_CALL.name -> "مكالمة فائتة"
                MessageType.CALL.name -> "مكالمة صوتية"
                else -> content
            }
            val currentUser = db.userDao().getCurrentUserSync()
            val isFromMe = (currentUser != null && currentUser.id == senderId)
            val existingChat = db.chatDao().getChatById(chatId)
            val newUnread = if (isFromMe) 0 else ((existingChat?.unreadCount ?: 0) + 1)
            db.chatDao().updateLastMessage(chatId, previewText, System.currentTimeMillis(), senderId, initialStatus, newUnread)
        }
        plainMsg
    }

    suspend fun markChatAsRead(chatId: String, currentUserId: String) = withContext(Dispatchers.IO) {
        db.chatDao().clearUnread(chatId)
        db.messageDao().markIncomingMessagesAsRead(chatId, currentUserId, MessageStatus.READ.name)
    }

    suspend fun updateMessageStatus(messageId: String, status: String, chatId: String? = null) = withContext(Dispatchers.IO) {
        db.messageDao().updateMessageStatus(messageId, status)
        if (!chatId.isNullOrBlank()) {
            db.chatDao().updateLastMessageStatus(chatId, status)
        }
    }

    suspend fun updateMessageReaction(messageId: String, reaction: String?) = withContext(Dispatchers.IO) {
        db.messageDao().updateMessageReaction(messageId, reaction)
    }

    suspend fun createChatAccount(chatId: String, name: String, creatorId: String): ChatAccountEntity = withContext(Dispatchers.IO) {
        val acc = ChatAccountEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            name = name,
            creatorId = creatorId
        )
        db.chatAccountDao().insertAccount(acc)
        acc
    }

    suspend fun createHashtagAccount(
        chatId: String,
        accountId: String,
        tag: String,
        creatorId: String,
        projectId: String? = null,
        projectName: String? = null
    ): HashtagAccountEntity = withContext(Dispatchers.IO) {
        val cleanTag = tag.trim().removePrefix("#").ifBlank { "عام" }
        val resolvedProject = projectId?.let { db.projectDao().getProjectById(it) }
        val hashAcc = HashtagAccountEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            accountId = accountId,
            tag = cleanTag,
            creatorId = creatorId,
            projectId = resolvedProject?.id ?: projectId,
            projectName = resolvedProject?.name ?: projectName
        )
        db.chatAccountDao().insertHashtagAccount(hashAcc)
        hashAcc
    }

    suspend fun addHashtagEntry(
        hashtagId: String,
        accountId: String,
        type: String,
        description: String,
        amount: Double,
        direction: String,
        createdBy: String,
        projectId: String? = null,
        projectName: String? = null,
        isExternalBusiness: Boolean = false,
        paymentMethod: String = "كاش"
    ) = withContext(Dispatchers.IO) {
        val safeAmount = if (amount.isFinite() && amount > 0.0) amount else 0.0
        if (safeAmount <= 0.0) {
            throw IllegalArgumentException("المبلغ يجب أن يكون أكبر من الصفر")
        }
        val hashtag = db.chatAccountDao().getHashtagByIdSync(hashtagId)
        if (hashtag != null) {
            val user = db.userDao().getCurrentUserSync()
            recordChatHashtagOperation(
                chatId = hashtag.chatId,
                tag = hashtag.tag,
                entryType = if (type == HashtagEntryType.PAYMENT.name) HashtagEntryType.PAYMENT else HashtagEntryType.INVOICE,
                description = description,
                amount = safeAmount,
                direction = if (direction == HashtagEntryDirection.CREDIT.name) HashtagEntryDirection.CREDIT else HashtagEntryDirection.DEBIT,
                actorId = user?.id ?: "me",
                actorName = createdBy.ifBlank { user?.name ?: "المستخدم" },
                projectId = projectId ?: hashtag.projectId,
                projectName = projectName ?: hashtag.projectName,
                paymentMethod = paymentMethod,
                isExternalBusiness = isExternalBusiness,
                postChatMessage = true
            )
            return@withContext
        }
        val entry = HashtagEntryEntity(
            id = UUID.randomUUID().toString(),
            hashtagId = hashtagId,
            accountId = accountId,
            type = type,
            description = description,
            amount = safeAmount,
            direction = direction,
            createdBy = createdBy,
            projectId = projectId,
            projectName = projectName
        )
        db.chatAccountDao().insertHashtagEntry(entry)
    }

    data class ChatHashtagSyncResult(
        val hashtagAccount: HashtagAccountEntity,
        val entry: HashtagEntryEntity,
        val invoice: InvoiceEntity?,
        val remainingBalance: Double,
        val isSettled: Boolean
    )

    suspend fun recordChatHashtagOperation(
        chatId: String,
        tag: String,
        entryType: HashtagEntryType,
        description: String,
        amount: Double,
        direction: HashtagEntryDirection,
        actorId: String,
        actorName: String,
        projectId: String? = null,
        projectName: String? = null,
        paymentMethod: String = "كاش",
        isExternalBusiness: Boolean = false,
        photoUri: String? = null,
        linkedInvoiceId: String? = null,
        postChatMessage: Boolean = true
    ): ChatHashtagSyncResult = withContext(Dispatchers.IO) {
        if (!amount.isFinite() || amount <= 0.0) {
            throw IllegalArgumentException("المبلغ غير صالح: يجب إدخال قيمة أكبر من الصفر")
        }
        val cleanTag = tag.trim().removePrefix("#").ifBlank { "عام" }
        val roundedAmount = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toDouble()
        val now = System.currentTimeMillis()
        val isOnline = OfflineSyncManager.isNetworkAvailable(appContext)

        db.withTransaction {
            val existingChat = db.chatDao().getChatById(chatId)
            val clientId = existingChat?.contactId ?: chatId
            val clientName = existingChat?.contactName ?: actorName
            val resolvedProject = projectId?.takeIf { it.isNotBlank() }?.let { db.projectDao().getProjectById(it) }
            val resolvedProjectId = resolvedProject?.id ?: projectId?.takeIf { it.isNotBlank() }
            val resolvedProjectName = resolvedProject?.name ?: projectName?.takeIf { it.isNotBlank() }

            val existingAccounts = db.chatAccountDao().getAccountsForChatSync(chatId)
            val account = existingAccounts.firstOrNull() ?: ChatAccountEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                name = "حساب القيود والمطابقات الرئيسية",
                creatorId = actorId
            ).also { db.chatAccountDao().insertAccount(it) }

            val existingHashtags = db.chatAccountDao().getHashtagsForChatSync(chatId)
            val matchedHashtag = existingHashtags.firstOrNull { it.tag.equals(cleanTag, ignoreCase = true) }
            val hashtagAcc = if (matchedHashtag != null) {
                if (resolvedProjectId != null && matchedHashtag.projectId == null) {
                    matchedHashtag.copy(projectId = resolvedProjectId, projectName = resolvedProjectName).also {
                        db.chatAccountDao().insertHashtagAccount(it)
                    }
                } else matchedHashtag
            } else {
                HashtagAccountEntity(
                    id = UUID.randomUUID().toString(),
                    chatId = chatId,
                    accountId = account.id,
                    tag = cleanTag,
                    creatorId = actorId,
                    projectId = resolvedProjectId,
                    projectName = resolvedProjectName
                ).also { db.chatAccountDao().insertHashtagAccount(it) }
            }

            val isCredit = direction == HashtagEntryDirection.CREDIT || entryType == HashtagEntryType.PAYMENT
            var syncedInvoice: InvoiceEntity? = null
            var effectiveLinkedInvoiceId = linkedInvoiceId

            if (!isCredit) {
                // Create formal InvoiceEntity synchronized with Invoices Ledger & Projects
                val invoiceId = linkedInvoiceId ?: UUID.randomUUID().toString()
                effectiveLinkedInvoiceId = invoiceId
                val existingNumbers = db.invoiceDao().getAllInvoicesSync()
                    .map { it.invoiceNumber.uppercase(Locale.US) }
                    .toSet()
                val shortInvoiceNumber = com.example.util.ShortCodeGenerator.generateInvoiceCode(existingNumbers)
                val isGold = cleanTag.contains("ذهب") || cleanTag.contains("كسر")
                val invCategory = when {
                    resolvedProjectId != null -> InvoiceCategory.PROJECT_SUPPLY.name
                    isGold -> InvoiceCategory.GOLD_DEAL.name
                    else -> InvoiceCategory.GENERAL.name
                }
                val newInvoice = InvoiceEntity(
                    id = invoiceId,
                    invoiceNumber = shortInvoiceNumber,
                    title = description.ifBlank { "فاتورة #$cleanTag" },
                    category = invCategory,
                    senderId = actorId,
                    senderName = actorName,
                    receiverId = clientId,
                    receiverName = clientName,
                    totalAmount = roundedAmount,
                    paymentMethod = paymentMethod,
                    status = InvoiceStatus.PENDING.name,
                    issueDate = now,
                    notes = "مربوطة بهاشتاج #$cleanTag من محادثة $clientName",
                    projectId = resolvedProjectId,
                    projectName = resolvedProjectName,
                    isDeliveredToOozoxUser = true,
                    isSynced = isOnline,
                    createdAt = now,
                    updatedAt = now,
                    hashtagId = hashtagAcc.id,
                    hashtagTag = cleanTag,
                    chatId = chatId,
                    paidAmount = 0.0,
                    isExternalBusiness = isExternalBusiness,
                    sourceType = if (isExternalBusiness) "EXTERNAL" else "CHAT_HASHTAG"
                )
                db.invoiceDao().insertInvoice(newInvoice)
                syncedInvoice = newInvoice

                if (resolvedProject != null) {
                    db.projectDao().addExpenseDelta(resolvedProject.id, roundedAmount)
                    db.projectAuditLogDao().insertAuditLog(
                        ProjectAuditLogEntity(
                            id = UUID.randomUUID().toString(),
                            projectId = resolvedProject.id,
                            action = "فاتورة هاشتاج (#$cleanTag)",
                            actorName = actorName,
                            actorPhone = "",
                            details = "إصدار فاتورة ${newInvoice.invoiceNumber} بقيمة ${GoldUtils.formatCurrency(roundedAmount)} لـ $clientName"
                        )
                    )
                }
            } else {
                // Allocate client payment across open invoices (linked invoice -> same hashtag -> same chat -> same client)
                var remainingPayBD = BigDecimal.valueOf(roundedAmount)
                val candidateInvoices = buildList {
                    if (!linkedInvoiceId.isNullOrBlank()) {
                        db.invoiceDao().getInvoiceByIdSync(linkedInvoiceId)?.let { add(it) }
                    }
                    addAll(db.invoiceDao().getInvoicesByHashtagIdSync(hashtagAcc.id))
                    addAll(db.invoiceDao().getInvoicesByChatIdSync(chatId))
                    addAll(db.invoiceDao().getUnpaidInvoicesByClientSync(clientId, clientName))
                }.distinctBy { it.id }
                    .filter { it.status != InvoiceStatus.PAID.name && it.status != InvoiceStatus.CANCELLED.name }

                for (inv in candidateInvoices) {
                    if (remainingPayBD <= BigDecimal("0.01")) break
                    val totalBD = BigDecimal.valueOf(inv.totalAmount)
                    val alreadyPaidBD = BigDecimal.valueOf(inv.paidAmount)
                    val owedBD = totalBD.subtract(alreadyPaidBD).coerceAtLeast(BigDecimal.ZERO)
                    if (owedBD <= BigDecimal("0.01")) continue
                    val applyBD = if (remainingPayBD < owedBD) remainingPayBD else owedBD
                    val newPaidBD = alreadyPaidBD.add(applyBD).setScale(2, RoundingMode.HALF_UP)
                    val newStatus = if (newPaidBD.add(BigDecimal("0.01")) >= totalBD) {
                        InvoiceStatus.PAID.name
                    } else {
                        InvoiceStatus.PARTIAL.name
                    }
                    db.invoiceDao().updateInvoicePayment(inv.id, newPaidBD.toDouble(), newStatus, now)
                    if (syncedInvoice == null) {
                        syncedInvoice = inv.copy(paidAmount = newPaidBD.toDouble(), status = newStatus, updatedAt = now)
                        effectiveLinkedInvoiceId = inv.id
                    }
                    remainingPayBD = remainingPayBD.subtract(applyBD).coerceAtLeast(BigDecimal.ZERO)
                }

                if (resolvedProject != null) {
                    db.projectDao().addIncomeDelta(resolvedProject.id, roundedAmount)
                    db.projectAuditLogDao().insertAuditLog(
                        ProjectAuditLogEntity(
                            id = UUID.randomUUID().toString(),
                            projectId = resolvedProject.id,
                            action = "تحصيل / سداد (#$cleanTag)",
                            actorName = actorName,
                            actorPhone = "",
                            details = "سداد بقيمة ${GoldUtils.formatCurrency(roundedAmount)} من $clientName"
                        )
                    )
                }
            }

            val entry = HashtagEntryEntity(
                id = UUID.randomUUID().toString(),
                hashtagId = hashtagAcc.id,
                accountId = account.id,
                type = if (isCredit) HashtagEntryType.PAYMENT.name else HashtagEntryType.INVOICE.name,
                description = description.ifBlank { if (isCredit) "دفعة سداد #$cleanTag" else "فاتورة #$cleanTag" },
                amount = roundedAmount,
                direction = if (isCredit) HashtagEntryDirection.CREDIT.name else HashtagEntryDirection.DEBIT.name,
                createdBy = actorName,
                createdAt = now,
                photoUri = photoUri,
                linkedInvoiceId = effectiveLinkedInvoiceId,
                projectId = resolvedProjectId ?: hashtagAcc.projectId,
                projectName = resolvedProjectName ?: hashtagAcc.projectName
            )
            db.chatAccountDao().insertHashtagEntry(entry)

            // Recompute hashtag balance & status atomically
            val allEntries = db.chatAccountDao().getEntriesForHashtagSync(hashtagAcc.id)
            val totals = HashtagUtils.calculateHashtagTotals(allEntries)
            val isSettled = totals.remaining <= 0.01 && totals.totalDebit > 0.0
            val newHashStatus = if (isSettled) HashtagStatus.SETTLED.name else HashtagStatus.ACTIVE.name
            db.chatAccountDao().updateHashtagStatus(hashtagAcc.id, newHashStatus)

            // Audit & Immutable Ledger entry
            val auditChecksum = Integer.toHexString("${entry.id}|$roundedAmount|$now".hashCode()).uppercase(Locale.US)
            val isGoldTag = cleanTag.contains("ذهب") || cleanTag.contains("كسر")
            val ledger = LedgerEntryEntity(
                id = UUID.randomUUID().toString(),
                accountOwnerId = actorId,
                counterPartyId = clientId,
                counterPartyName = clientName,
                assetType = if (isGoldTag) LedgerAssetType.GOLD_24K.name else LedgerAssetType.CASH_EGP.name,
                entryType = if (isCredit) LedgerEntryType.CREDIT.name else LedgerEntryType.DEBIT.name,
                weightGrams = 0.0,
                cashAmount = roundedAmount,
                metalLossRate = 0.0,
                checksumHash = auditChecksum,
                isReversal = false,
                notes = "قيد متزامن #$cleanTag | ${entry.description} | الفاتورة: ${syncedInvoice?.invoiceNumber ?: "-"}",
                timestamp = now
            )
            db.immutableLedgerDao().insertEntry(ledger)

            // Encrypted Daily Accounting Record (AES-256)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
            val isRevenue = isCredit && (cleanTag.contains("مبيعات") || cleanTag.contains("إيراد") || cleanTag.contains("ايراد") || syncedInvoice != null)
            val dailyRecord = DailyAccountingRecordEntity(
                id = UUID.randomUUID().toString(),
                groupId = resolvedProjectId ?: chatId,
                subGroupId = "#$cleanTag",
                profileId = null,
                entryDate = todayStr,
                staffId = actorId,
                staffName = actorName,
                staffRole = if (isExternalBusiness) "أعمال خارجية" else "محاسبة الشات",
                dailyRevenue = if (isRevenue) roundedAmount else 0.0,
                incomingAmount = if (isCredit && !isRevenue) roundedAmount else 0.0,
                outgoingAmount = if (!isCredit) roundedAmount else 0.0,
                deficitAmount = 0.0,
                complaintsCount = 0,
                complaintsNotes = null,
                notes = "${if (isCredit) "سداد" else "فاتورة"} #$cleanTag - ${entry.description} (${syncedInvoice?.invoiceNumber ?: ""})".trim(),
                timestamp = now
            )
            db.dailyAccountingRecordDao().insertRecord(
                RoomLocalAES256Engine.encryptDailyRecord(dailyRecord, null, appContext)
            )

            // Safe JSON payload in SyncQueue
            val syncPayload = JSONObject().apply {
                put("entryId", entry.id)
                put("hashtagId", hashtagAcc.id)
                put("tag", cleanTag)
                put("invoiceId", effectiveLinkedInvoiceId ?: "")
                put("invoiceNumber", syncedInvoice?.invoiceNumber ?: "")
                put("projectId", resolvedProjectId ?: "")
                put("amount", roundedAmount)
                put("direction", entry.direction)
            }.toString()
            db.syncQueueDao().insertSyncItem(
                SyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    actionType = if (isCredit) "HASHTAG_PAYMENT_SYNC" else "HASHTAG_INVOICE_SYNC",
                    payloadJson = syncPayload,
                    status = if (isOnline) "SYNCED" else "PENDING",
                    timestamp = now
                )
            )

            if (postChatMessage) {
                val msgType = if (isCredit) MessageType.PAYMENT.name else MessageType.INVOICE.name
                val invLabel = syncedInvoice?.invoiceNumber?.let { " | رقم $it" } ?: ""
                val projLabel = resolvedProjectName?.let { " | مشروع: $it" } ?: ""
                val msgText = if (isCredit) {
                    "سداد #$cleanTag بمبلغ ${GoldUtils.formatCurrency(roundedAmount)} ($paymentMethod)$invLabel$projLabel — ${entry.description}"
                } else {
                    "فاتورة #$cleanTag بمبلغ ${GoldUtils.formatCurrency(roundedAmount)}$invLabel$projLabel — ${entry.description}"
                }
                val chatMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    chatId = chatId,
                    senderId = actorId,
                    senderName = actorName,
                    type = msgType,
                    content = msgText,
                    transactionId = syncedInvoice?.id,
                    hashtagId = hashtagAcc.id,
                    timestamp = now,
                    status = if (isOnline) MessageStatus.SENT.name else MessageStatus.PENDING.name
                )
                db.messageDao().insertMessage(RoomLocalAES256Engine.encryptMessage(chatMsg, null, appContext))
                db.chatDao().updateLastMessage(chatId, msgText, now, actorId, chatMsg.status, 0)
            }

            ChatHashtagSyncResult(
                hashtagAccount = hashtagAcc.copy(status = newHashStatus),
                entry = entry,
                invoice = syncedInvoice,
                remainingBalance = totals.remaining,
                isSettled = isSettled
            )
        }
    }

    suspend fun payInvoiceWithSync(
        invoiceId: String,
        paymentAmount: Double,
        paymentMethod: String = "كاش",
        actorId: String,
        actorName: String,
        notes: String? = null,
        postChatNotice: Boolean = true
    ): InvoiceEntity = withContext(Dispatchers.IO) {
        if (!paymentAmount.isFinite() || paymentAmount <= 0.0) {
            throw IllegalArgumentException("مبلغ السداد يجب أن يكون أكبر من الصفر")
        }
        val roundedPay = BigDecimal.valueOf(paymentAmount).setScale(2, RoundingMode.HALF_UP).toDouble()
        val now = System.currentTimeMillis()
        val isOnline = OfflineSyncManager.isNetworkAvailable(appContext)

        db.withTransaction {
            val inv = db.invoiceDao().getInvoiceByIdSync(invoiceId)
                ?: throw IllegalStateException("الفاتورة غير موجودة")
            val totalBD = BigDecimal.valueOf(inv.totalAmount)
            val paidBD = BigDecimal.valueOf(inv.paidAmount)
            val newPaidBD = paidBD.add(BigDecimal.valueOf(roundedPay)).setScale(2, RoundingMode.HALF_UP)
            val clampedPaid = if (newPaidBD > totalBD) totalBD else newPaidBD
            val newStatus = if (clampedPaid.add(BigDecimal("0.01")) >= totalBD) {
                InvoiceStatus.PAID.name
            } else {
                InvoiceStatus.PARTIAL.name
            }

            db.invoiceDao().updateInvoicePayment(inv.id, clampedPaid.toDouble(), newStatus, now)

            // Sync back to linked Hashtag if present (or create in linked chat)
            val hashtagId = inv.hashtagId
            if (!hashtagId.isNullOrBlank()) {
                val hashtag = db.chatAccountDao().getHashtagByIdSync(hashtagId)
                if (hashtag != null) {
                    val payEntry = HashtagEntryEntity(
                        id = UUID.randomUUID().toString(),
                        hashtagId = hashtag.id,
                        accountId = hashtag.accountId,
                        type = HashtagEntryType.PAYMENT.name,
                        description = notes?.takeIf { it.isNotBlank() }
                            ?: "سداد فاتورة ${inv.invoiceNumber} ($paymentMethod)",
                        amount = roundedPay,
                        direction = HashtagEntryDirection.CREDIT.name,
                        createdBy = actorName,
                        createdAt = now,
                        linkedInvoiceId = inv.id,
                        projectId = inv.projectId,
                        projectName = inv.projectName
                    )
                    db.chatAccountDao().insertHashtagEntry(payEntry)
                    val entries = db.chatAccountDao().getEntriesForHashtagSync(hashtag.id)
                    val totals = HashtagUtils.calculateHashtagTotals(entries)
                    val newHashStatus = if (totals.remaining <= 0.01 && totals.totalDebit > 0.0) {
                        HashtagStatus.SETTLED.name
                    } else {
                        HashtagStatus.ACTIVE.name
                    }
                    db.chatAccountDao().updateHashtagStatus(hashtag.id, newHashStatus)
                }
            }

            // Sync Project Income
            if (!inv.projectId.isNullOrBlank()) {
                db.projectDao().addIncomeDelta(inv.projectId, roundedPay)
                db.projectAuditLogDao().insertAuditLog(
                    ProjectAuditLogEntity(
                        id = UUID.randomUUID().toString(),
                        projectId = inv.projectId,
                        action = "سداد فاتورة ${inv.invoiceNumber}",
                        actorName = actorName,
                        actorPhone = "",
                        details = "تحصيل ${GoldUtils.formatCurrency(roundedPay)} ($paymentMethod) — الحالة: $newStatus"
                    )
                )
            }

            // Immutable ledger
            val checksum = Integer.toHexString("PAY|${inv.id}|$roundedPay|$now".hashCode()).uppercase(Locale.US)
            db.immutableLedgerDao().insertEntry(
                LedgerEntryEntity(
                    id = UUID.randomUUID().toString(),
                    accountOwnerId = actorId,
                    counterPartyId = inv.receiverId,
                    counterPartyName = inv.receiverName,
                    assetType = LedgerAssetType.CASH_EGP.name,
                    entryType = LedgerEntryType.CREDIT.name,
                    weightGrams = 0.0,
                    cashAmount = roundedPay,
                    checksumHash = checksum,
                    notes = "سداد فاتورة ${inv.invoiceNumber}: ${inv.title} ($paymentMethod)",
                    timestamp = now
                )
            )

            // Encrypted Daily Accounting Record
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
            val rec = DailyAccountingRecordEntity(
                id = UUID.randomUUID().toString(),
                groupId = inv.projectId ?: inv.chatId ?: "invoices_hub",
                subGroupId = inv.hashtagTag?.let { "#$it" } ?: inv.invoiceNumber,
                profileId = null,
                entryDate = todayStr,
                staffId = actorId,
                staffName = actorName,
                staffRole = "تحصيل الفواتير",
                dailyRevenue = roundedPay,
                incomingAmount = 0.0,
                outgoingAmount = 0.0,
                deficitAmount = 0.0,
                complaintsCount = 0,
                notes = "سداد فاتورة ${inv.invoiceNumber} - ${inv.receiverName} ($paymentMethod)",
                timestamp = now
            )
            db.dailyAccountingRecordDao().insertRecord(
                RoomLocalAES256Engine.encryptDailyRecord(rec, null, appContext)
            )

            // Post confirmation message into linked chat if available
            if (postChatNotice && !inv.chatId.isNullOrBlank()) {
                val statusLabel = if (newStatus == InvoiceStatus.PAID.name) "مدفوعة بالكامل ✅" else "مدفوعة جزئياً ⏳"
                val tagSuffix = inv.hashtagTag?.let { " (#$it)" } ?: ""
                val msgText = "تم تسجيل سداد ${GoldUtils.formatCurrency(roundedPay)} للفاتورة ${inv.invoiceNumber}$tagSuffix عبر $paymentMethod — الحالة: $statusLabel"
                val chatMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    chatId = inv.chatId,
                    senderId = actorId,
                    senderName = actorName,
                    type = MessageType.PAYMENT.name,
                    content = msgText,
                    transactionId = inv.id,
                    hashtagId = inv.hashtagId,
                    timestamp = now,
                    status = if (isOnline) MessageStatus.SENT.name else MessageStatus.PENDING.name
                )
                db.messageDao().insertMessage(RoomLocalAES256Engine.encryptMessage(chatMsg, null, appContext))
                db.chatDao().updateLastMessage(inv.chatId, msgText, now, actorId, chatMsg.status, 0)
            }

            // Queue sync
            val syncPayload = JSONObject().apply {
                put("invoiceId", inv.id)
                put("invoiceNumber", inv.invoiceNumber)
                put("paidAmount", clampedPaid.toDouble())
                put("paymentDelta", roundedPay)
                put("status", newStatus)
                put("method", paymentMethod)
            }.toString()
            db.syncQueueDao().insertSyncItem(
                SyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    actionType = "PAY_INVOICE",
                    payloadJson = syncPayload,
                    status = if (isOnline) "SYNCED" else "PENDING",
                    timestamp = now
                )
            )

            inv.copy(paidAmount = clampedPaid.toDouble(), status = newStatus, updatedAt = now)
        }
    }

    suspend fun autoProcessHashtagFromMessage(
        chatId: String,
        content: String,
        senderId: String,
        senderName: String
    ): HashtagEntryEntity? = withContext(Dispatchers.IO) {
        val trimmed = content.trim()
        // CRITICAL: Never process casual chat messages that do not contain an explicit #hashtag
        if (!trimmed.contains("#")) return@withContext null

        val explicitTag = HashtagUtils.parseHashtag(trimmed)?.takeIf { it.isNotBlank() } ?: return@withContext null
        val existingChat = db.chatDao().getChatById(chatId)
        val contactName = existingChat?.contactName ?: senderName
        val parsed = com.example.util.SmartVoiceAccountingParser.parseVoiceAccountingText(trimmed, contactName)

        // Only record accounting entry & invoice when a valid financial amount > 0 is detected
        if (!parsed.amount.isFinite() || parsed.amount <= 0.0) return@withContext null

        val isCredit = parsed.direction == "CREDIT" ||
            explicitTag.contains("دفعة") || explicitTag.contains("سداد") || explicitTag.contains("تحويل")
        val direction = if (isCredit) HashtagEntryDirection.CREDIT else HashtagEntryDirection.DEBIT
        val entryType = if (isCredit) HashtagEntryType.PAYMENT else HashtagEntryType.INVOICE

        val result = recordChatHashtagOperation(
            chatId = chatId,
            tag = explicitTag,
            entryType = entryType,
            description = trimmed,
            amount = parsed.amount,
            direction = direction,
            actorId = senderId,
            actorName = senderName,
            postChatMessage = false // Message already sent by user!
        )
        result.entry
    }

    suspend fun createWorkGroup(name: String, description: String?, ownerId: String): WorkGroupEntity = withContext(Dispatchers.IO) {
        val wg = WorkGroupEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            ownerId = ownerId
        )
        db.workGroupDao().insertWorkGroup(wg)
        wg
    }

    suspend fun createProject(
        name: String,
        workGroupId: String,
        capital: Double,
        type: String,
        location: String = "القاهرة، مصر",
        coManagerPhone: String? = null,
        coManagerName: String? = null,
        membersInfo: String = ""
    ): ProjectEntity = withContext(Dispatchers.IO) {
        val safeCapital = if (capital.isFinite() && capital >= 0.0) capital else 0.0
        val proj = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            workGroupId = workGroupId,
            type = type,
            estimatedCapital = safeCapital,
            location = location,
            coManagerPhone = coManagerPhone,
            coManagerName = coManagerName,
            membersInfo = membersInfo
        )
        db.withTransaction {
            db.projectDao().insertProject(proj)
            val isOnline = OfflineSyncManager.isNetworkAvailable(appContext)
            val payload = JSONObject().apply {
                put("projectId", proj.id)
                put("name", name)
                put("capital", safeCapital)
            }.toString()
            val syncItem = SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                actionType = "ADD_PROJECT",
                payloadJson = payload,
                status = if (isOnline) "SYNCED" else "PENDING"
            )
            db.syncQueueDao().insertSyncItem(syncItem)
        }
        proj
    }

    suspend fun addGoldTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        val normalizedId = if (transaction.id.isBlank() || transaction.id.contains("-")) {
            val existingIds = db.transactionDao().getAllTransactions().first().map { it.id.uppercase(Locale.US) }.toSet()
            com.example.util.ShortCodeGenerator.generateOperationCode(existingIds)
        } else {
            transaction.id.uppercase(Locale.US)
        }
        db.transactionDao().insertTransaction(transaction.copy(id = normalizedId))
    }

    suspend fun getInvoiceByNumberOrIdSync(code: String): InvoiceEntity? = withContext(Dispatchers.IO) {
        val clean = code.trim().removePrefix("#")
        db.invoiceDao().getInvoiceByNumberSync(clean) ?: db.invoiceDao().getInvoiceByIdSync(clean)
    }

    suspend fun getTransactionByIdSync(code: String): TransactionEntity? = withContext(Dispatchers.IO) {
        val clean = code.trim().removePrefix("#").uppercase(Locale.US)
        db.transactionDao().getTransactionById(clean) ?: db.transactionDao().getTransactionById(code.trim())
    }

    suspend fun addGoldPurchase(purchase: GoldPurchaseEntity) = withContext(Dispatchers.IO) {
        db.goldPurchaseDao().insertPurchase(purchase)
    }

    suspend fun submitTraderRequest(request: TraderRequestEntity) = withContext(Dispatchers.IO) {
        db.traderRequestDao().insertTraderRequest(request)
    }

    suspend fun updateTraderRequestStatus(id: String, status: String, adminNotes: String?) = withContext(Dispatchers.IO) {
        db.traderRequestDao().updateStatus(id, status, System.currentTimeMillis(), adminNotes)
    }

    suspend fun updateAdminConfig(config: AdminConfigEntity) = withContext(Dispatchers.IO) {
        db.adminConfigDao().saveConfig(config)
    }

    suspend fun addLedgerEntry(entry: LedgerEntryEntity) = withContext(Dispatchers.IO) {
        db.immutableLedgerDao().insertEntry(entry)
    }

    suspend fun addClearingSettlement(settlement: ClearingSettlementEntity) = withContext(Dispatchers.IO) {
        db.clearingSettlementDao().insertSettlement(settlement)
    }

    suspend fun getProjectByIdSync(projectId: String): ProjectEntity? = withContext(Dispatchers.IO) {
        db.projectDao().getProjectById(projectId)
    }

    suspend fun getProjectByWorkGroupIdSync(workGroupId: String): ProjectEntity? = withContext(Dispatchers.IO) {
        db.projectDao().getProjectByWorkGroupIdSync(workGroupId)
    }

    suspend fun getProjectMembersSync(projectId: String): List<ProjectMemberEntity> = withContext(Dispatchers.IO) {
        db.projectMemberDao().getMembersForProjectSync(projectId)
    }

    suspend fun addProjectMember(member: ProjectMemberEntity) = withContext(Dispatchers.IO) {
        db.projectMemberDao().insertMember(member)
    }

    suspend fun updateProjectMemberRole(memberId: String, newRole: String) = withContext(Dispatchers.IO) {
        db.projectMemberDao().updateMemberRole(memberId, newRole)
    }

    suspend fun getProjectTaskByIdSync(taskId: String): ProjectTaskEntity? = withContext(Dispatchers.IO) {
        db.projectTaskDao().getTaskByIdSync(taskId)
    }

    suspend fun recordProjectAccountingEntry(
        projectId: String,
        amount: Double,
        isIncome: Boolean
    ) = withContext(Dispatchers.IO) {
        val project = db.projectDao().getProjectById(projectId) ?: return@withContext
        val updated = if (isIncome) {
            project.copy(totalIncome = project.totalIncome + amount)
        } else {
            project.copy(totalExpenses = project.totalExpenses + amount)
        }
        db.projectDao().insertProject(updated)
    }

    suspend fun addProjectAuditLog(log: ProjectAuditLogEntity) = withContext(Dispatchers.IO) {
        db.projectAuditLogDao().insertAuditLog(log)
    }

    suspend fun queueSyncItem(item: SyncQueueEntity) = withContext(Dispatchers.IO) {
        db.syncQueueDao().insertSyncItem(item)
    }

    suspend fun clearSyncedItems() = withContext(Dispatchers.IO) {
        db.syncQueueDao().clearSynced()
    }

    suspend fun seedSubscriptionPlansIfEmpty() = withContext(Dispatchers.IO) {
        val existingPlans = db.subscriptionPlanDao().getAllPlans().first()
        if (existingPlans.isNotEmpty()) return@withContext

        val defaultPlans = listOf(
            SubscriptionPlanEntity(
                id = SubscriptionTier.FREE_TRIAL.name,
                title = SubscriptionTier.FREE_TRIAL.label,
                description = SubscriptionTier.FREE_TRIAL.description,
                monthlyPrice = 0.0,
                annualPrice = 0.0,
                discountPercent = 0,
                offerTitle = "هدية الانضمام: شهران كاملان (60 يوماً) مجاناً بكل المزايا",
                isOfferActive = true,
                featuresListText = "وصول كامل وشامل لجميع المزايا لمدة 60 يوماً\nالترجمة السياقية الفورية والنطق الصوتي بالذكاء الاصطناعي (Gemini AI)\nالمحاسب المالي الذكي والوكيل الصوتي AI\nالمركز المالي المحاسبي وإدارة اليوميات والشركات\nنظام الشكاوى المالية وتدقيق عجز الصندوق\nخزائن وحسابات فرعية مشفرة وسرية\nتصدير كشوف الحسابات والتقارير PDF و Excel\nالنسخ الاحتياطي المشفر والنقل السحابي والمحلي للأجهزة\nمحادثات الأعمال اللحظية والمجموعات المشفرة",
                badgeText = SubscriptionTier.FREE_TRIAL.badgeText,
                isPopular = false,
                isEnabled = true
            ),
            SubscriptionPlanEntity(
                id = SubscriptionTier.SILVER_TRADER.name,
                title = SubscriptionTier.SILVER_TRADER.label,
                description = SubscriptionTier.SILVER_TRADER.description,
                monthlyPrice = 499.0,
                annualPrice = 4990.0,
                discountPercent = 15,
                offerTitle = "عرض خاص للأعمال والمحلات والأنشطة التجارية",
                isOfferActive = true,
                featuresListText = "الترجمة السياقية الفورية والنطق الصوتي للمحادثات (Gemini AI)\nقيود محاسبية يومية غير محدودة\nكشوف حسابات الهاشتاج والمحادثات التجارية\nتصدير تقارير اليومية وكشوف الحسابات PDF و Excel\nإدارة حساب فرعي سري مشفر واحد (1 Vault)\nإدارة الفواتير والمدفوعات والمستحقات\nإدارة ورديات الموظفين وحضورهم\nدعم فني عبر الشات المباشر",
                badgeText = SubscriptionTier.SILVER_TRADER.badgeText,
                isPopular = false,
                isEnabled = true
            ),
            SubscriptionPlanEntity(
                id = SubscriptionTier.GOLD_ENTERPRISE.name,
                title = SubscriptionTier.GOLD_ENTERPRISE.label,
                description = SubscriptionTier.GOLD_ENTERPRISE.description,
                monthlyPrice = 999.0,
                annualPrice = 9990.0,
                discountPercent = 20,
                offerTitle = "الأكثر طلباً للشركات والمؤسسات والمقاولات",
                isOfferActive = true,
                featuresListText = "كل مميزات الباقة الفضية بالكامل\nحسابات فرعية مشفرة سرية بلا حدود (خزائن مخفية 100%)\nالمساعد والوكيل الصوتي الذكي لتسجيل القيود صوتياً\nإدارة مشاريع الأعمال بصلاحيات الفريق (RBAC)\nسجل التدقيق والرقابة غير القابل للتعديل\nدفتر الأستاذ المزدوج الموثق بتشفير SHA256\nالنسخ الاحتياطي المشفر والنقل الآمن بين الهواتف برقم الهاتف",
                badgeText = SubscriptionTier.GOLD_ENTERPRISE.badgeText,
                isPopular = true,
                isEnabled = true
            ),
            SubscriptionPlanEntity(
                id = SubscriptionTier.DIAMOND_VIP.name,
                title = SubscriptionTier.DIAMOND_VIP.label,
                description = SubscriptionTier.DIAMOND_VIP.description,
                monthlyPrice = 1999.0,
                annualPrice = 19990.0,
                discountPercent = 30,
                offerTitle = "عرض المؤسسات الشاملة: خصم 30% لفترة محدودة",
                isOfferActive = true,
                featuresListText = "كل مميزات المنصة بالكامل بلا أي حدود أو قيود\nالمحاسب المالي الذكي والتحليل التلقائي بالذكاء الاصطناعي\nالوكيل الصوتي التفاعلي لإدارة الحسابات صوتياً\nالنسخ الاحتياطي التلقائي السحابي اليومي المجدول\nتأمين وحظر رسائل الخاص بالمجموعات لحماية الأعمال\nمدير حساب مخصص ودعم VIP مباشر على مدار الساعة",
                badgeText = SubscriptionTier.DIAMOND_VIP.badgeText,
                isPopular = false,
                isEnabled = true
            )
        )
        db.subscriptionPlanDao().insertPlans(defaultPlans)
    }

    suspend fun updatePlanPriceAndOffer(
        planId: String,
        monthlyPrice: Double,
        discountPercent: Int,
        offerTitle: String?,
        isOfferActive: Boolean
    ) = withContext(Dispatchers.IO) {
        db.subscriptionPlanDao().updatePlanPriceAndOffer(planId, monthlyPrice, discountPercent, offerTitle, isOfferActive)
    }

    suspend fun submitSubscriptionRequest(request: SubscriptionRequestEntity) = withContext(Dispatchers.IO) {
        db.subscriptionRequestDao().insertRequest(request)
    }

    suspend fun updateSubscriptionRequestStatus(id: String, status: String, adminNotes: String?) = withContext(Dispatchers.IO) {
        db.subscriptionRequestDao().updateRequestStatus(id, status, System.currentTimeMillis(), adminNotes)
    }

    suspend fun updateUserSubscription(
        userId: String,
        tier: String,
        start: Long = System.currentTimeMillis(),
        end: Long = System.currentTimeMillis() + (30L * 24 * 3600 * 1000),
        isTrial: Boolean = false,
        status: String = "ACTIVE"
    ) = withContext(Dispatchers.IO) {
        db.userDao().updateUserSubscription(userId, tier, start, end, isTrial, status)
    }

    suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
        db.userDao().insertUser(user)
    }

    suspend fun postPromoAnnouncement(announcement: PromoAnnouncementEntity) = withContext(Dispatchers.IO) {
        db.promoAnnouncementDao().insertAnnouncement(announcement)
    }

    suspend fun setPromoAnnouncementsActive(ids: List<String>, isActive: Boolean) = withContext(Dispatchers.IO) {
        db.promoAnnouncementDao().setAnnouncementsActive(ids, isActive)
    }

    suspend fun deletePromoAnnouncement(id: String) = withContext(Dispatchers.IO) {
        db.promoAnnouncementDao().deleteAnnouncement(id)
    }

    suspend fun addMerchantProduct(product: MerchantProductEntity) = withContext(Dispatchers.IO) {
        db.merchantStoreDao().insertProduct(product)
    }

    suspend fun deleteMerchantProduct(productId: String) = withContext(Dispatchers.IO) {
        db.merchantStoreDao().deleteProduct(productId)
    }

    suspend fun incrementMerchantProductViews(productId: String) = withContext(Dispatchers.IO) {
        db.merchantStoreDao().incrementProductViews(productId)
    }

    fun getReviewsForMerchant(merchantId: String): Flow<List<MerchantReviewEntity>> {
        return db.merchantReviewDao().getReviewsForMerchant(merchantId)
    }

    fun getAllMerchantReviews(): Flow<List<MerchantReviewEntity>> {
        return db.merchantReviewDao().getAllReviews()
    }

    suspend fun addMerchantReview(review: MerchantReviewEntity) = withContext(Dispatchers.IO) {
        db.merchantReviewDao().insertReview(review)
    }

    suspend fun updateMerchantProfileInfo(
        userId: String,
        jobTitle: String?,
        skills: String?,
        services: String?,
        showSkills: Boolean,
        exp: Int,
        bio: String?,
        education: String?,
        cvSectionOrder: String?,
        shopName: String?,
        address: String?,
        ratingScore: Float,
        reviewsCount: Int
    ) = withContext(Dispatchers.IO) {
        db.userDao().updateMerchantProfileInfo(
            userId, jobTitle, skills, services, showSkills, exp, bio, education, cvSectionOrder, shopName, address, ratingScore, reviewsCount
        )
    }

    suspend fun recordMerchantSale(sale: MerchantDailySaleEntity) = withContext(Dispatchers.IO) {
        db.merchantStoreDao().insertSale(sale)
    }

    suspend fun markMerchantSalesAsClosed(merchantId: String) = withContext(Dispatchers.IO) {
        db.merchantStoreDao().markSalesAsClosed(merchantId)
    }

    suspend fun insertMerchantClosingReport(report: MerchantDailyClosingReportEntity) = withContext(Dispatchers.IO) {
        db.merchantStoreDao().insertClosingReport(report)
    }

    suspend fun togglePinChat(chatId: String, currentPinned: Boolean) = withContext(Dispatchers.IO) {
        db.chatDao().setChatPinned(chatId, !currentPinned)
    }

    suspend fun addPriceAlert(alert: PriceAlertEntity) = withContext(Dispatchers.IO) {
        db.priceAlertDao().insertAlert(alert)
    }

    suspend fun deletePriceAlert(alertId: String) = withContext(Dispatchers.IO) {
        db.priceAlertDao().deleteAlert(alertId)
    }

    suspend fun togglePriceAlert(alertId: String, isActive: Boolean) = withContext(Dispatchers.IO) {
        db.priceAlertDao().setAlertActive(alertId, isActive)
    }

    suspend fun setPriceAlertTriggered(alertId: String, isTriggered: Boolean) = withContext(Dispatchers.IO) {
        db.priceAlertDao().setAlertTriggered(alertId, isTriggered)
    }

    suspend fun addHiddenProfile(profile: HiddenProfileEntity) = withContext(Dispatchers.IO) {
        val encrypted = RoomLocalAES256Engine.encryptHiddenProfile(profile, appContext)
        db.hiddenProfileDao().insertHiddenProfile(encrypted)
    }

    suspend fun updateProfileNotificationSetting(profileId: String, silent: Boolean) = withContext(Dispatchers.IO) {
        db.hiddenProfileDao().updateProfileNotificationSetting(profileId, silent)
    }

    suspend fun updateProfilePassword(profileId: String, newPassword: String) = withContext(Dispatchers.IO) {
        db.hiddenProfileDao().updateProfilePassword(profileId, newPassword)
    }

    suspend fun deleteHiddenProfile(profileId: String) = withContext(Dispatchers.IO) {
        db.hiddenProfileDao().deleteHiddenProfile(profileId)
    }

    suspend fun addHiddenSubAccount(account: HiddenSubAccountEntity) = withContext(Dispatchers.IO) {
        db.hiddenSubAccountDao().insertSubAccount(account)
    }

    suspend fun updateSubAccount(account: HiddenSubAccountEntity) = withContext(Dispatchers.IO) {
        db.hiddenSubAccountDao().updateSubAccount(account)
    }

    suspend fun updateSubAccountNotifications(id: String, mute: Boolean, discreet: Boolean) = withContext(Dispatchers.IO) {
        db.hiddenSubAccountDao().updateNotificationSettings(id, mute, discreet)
    }

    suspend fun updateSubAccountPassword(id: String, newPassword: String) = withContext(Dispatchers.IO) {
        val salt = com.example.util.HiddenAccountSecurityEngine.generateSalt()
        val hash = com.example.util.HiddenAccountSecurityEngine.hashPassword(newPassword, salt)
        db.hiddenSubAccountDao().updatePassword(id, hash, salt, newPassword)
    }

    suspend fun deleteSubAccount(id: String) = withContext(Dispatchers.IO) {
        db.hiddenSubAccountDao().deleteSubAccount(id)
    }

    suspend fun addDailyAccountingRecord(record: DailyAccountingRecordEntity) = withContext(Dispatchers.IO) {
        val encryptedRecord = RoomLocalAES256Engine.encryptDailyRecord(record, null, appContext)
        db.dailyAccountingRecordDao().insertRecord(encryptedRecord)

        try {
            // Check if sub-account has muted notifications
            if (record.profileId != null) {
                val profile = db.hiddenProfileDao().getProfileById(record.profileId)
                if (profile?.silentNotifications == true) {
                    // Notifications are muted for this sub-account
                    return@withContext
                }
            }

            val group = db.workGroupDao().getWorkGroupById(record.groupId)
            val groupName = group?.name ?: "الخزينة العامة"

            // 1. Alert manager automatically if there is a deficit
            if (record.deficitAmount > 0) {
                NotificationHelper.showFinancialDeficitNotification(
                    context = appContext,
                    groupName = groupName,
                    staffName = record.staffName,
                    deficitAmount = record.deficitAmount,
                    entryDate = record.entryDate,
                    notes = record.notes ?: record.complaintsNotes
                )
            }

            // 2. Alert manager if there is an extraordinary surplus or high inflow
            val netInflow = if (record.dailyRevenue > 0) record.dailyRevenue else (record.incomingAmount - record.outgoingAmount)
            if (netInflow >= 50000.0) {
                NotificationHelper.showFinancialSurplusNotification(
                    context = appContext,
                    groupName = groupName,
                    staffName = record.staffName,
                    surplusAmount = netInflow,
                    entryDate = record.entryDate,
                    notes = record.notes
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteDailyAccountingRecord(recordId: String) = withContext(Dispatchers.IO) {
        db.dailyAccountingRecordDao().deleteRecord(recordId)
    }

    suspend fun submitRetroactiveComplaintAndApply(complaint: FinancialComplaintEntity): Double = withContext(Dispatchers.IO) {
        // 1. Insert the encrypted financial complaint
        val encComplaint = RoomLocalAES256Engine.encryptComplaint(complaint, null, appContext)
        db.financialComplaintDao().insertComplaint(encComplaint)

        // 2. Create the retroactive financial adjustment entry on targetDate
        val isDeduction = complaint.impactType == "DEDUCTION"
        val adjRecord = DailyAccountingRecordEntity(
            id = "adj_${complaint.id}",
            groupId = complaint.groupId,
            subGroupId = complaint.subGroupId ?: "قسم الشكاوى والتسويات الرجعية",
            profileId = complaint.profileId,
            entryDate = complaint.targetDate,
            staffId = complaint.submittedByStaffId,
            staffName = complaint.submittedByStaffName,
            staffRole = complaint.submittedByStaffRole,
            dailyRevenue = 0.0,
            incomingAmount = if (!isDeduction) complaint.amount else 0.0,
            outgoingAmount = if (isDeduction) complaint.amount else 0.0,
            deficitAmount = if (isDeduction) complaint.amount else 0.0,
            complaintsCount = 1,
            complaintsNotes = "شكوى وتسوية بأثر رجعي [${complaint.category}]: ${complaint.title} (${if (isDeduction) "خصم" else "إضافة"} ${GoldUtils.formatCurrency(complaint.amount)})",
            notes = "أثر مالي رجعي: ${complaint.details}",
            timestamp = System.currentTimeMillis()
        )
        val encAdjRecord = RoomLocalAES256Engine.encryptDailyRecord(adjRecord, null, appContext)
        db.dailyAccountingRecordDao().insertRecord(encAdjRecord)

        // 3. Query all records for that date and group to calculate retroactive net deficit
        val recordsOnDate = db.dailyAccountingRecordDao().getRecordsForGroupAndDate(complaint.groupId, complaint.targetDate).first()
        val totalDayDeficit = recordsOnDate.sumOf { it.deficitAmount }

        // Update complaint with resulting deficit
        val updatedComplaint = complaint.copy(
            resultingDayDeficit = totalDayDeficit,
            adminAlertSent = totalDayDeficit > 0
        )
        db.financialComplaintDao().insertComplaint(updatedComplaint)

        // 4. Notify admin immediately if there is a deficit on that target day (if not muted for this sub-account)
        if (totalDayDeficit > 0) {
            try {
                var isSilent = false
                if (complaint.profileId != null) {
                    val profile = db.hiddenProfileDao().getProfileById(complaint.profileId)
                    if (profile?.silentNotifications == true) {
                        isSilent = true
                    }
                }

                if (!isSilent) {
                    val group = db.workGroupDao().getWorkGroupById(complaint.groupId)
                    val groupName = group?.name ?: "الخزينة العامة"
                    NotificationHelper.showRetroactiveComplaintDeficitNotification(
                        context = appContext,
                        groupName = groupName,
                        staffName = complaint.submittedByStaffName,
                        complaintTitle = complaint.title,
                        targetDate = complaint.targetDate,
                        adjustmentAmount = complaint.amount,
                        netDayDeficit = totalDayDeficit
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        totalDayDeficit
    }

    suspend fun deleteFinancialComplaint(complaintId: String) = withContext(Dispatchers.IO) {
        db.financialComplaintDao().deleteComplaint(complaintId)
        db.dailyAccountingRecordDao().deleteRecord("adj_$complaintId")
    }

    suspend fun updateComplaintResolution(complaintId: String, status: String, notes: String?) = withContext(Dispatchers.IO) {
        db.financialComplaintDao().updateComplaintStatus(complaintId, status, notes)
    }

    suspend fun updateMemberPermissionsAndShift(
        memberId: String,
        role: String,
        startHours: String?,
        endHours: String?,
        shiftHours: Double,
        workingDays: String,
        shiftType: String,
        canPostAccounting: Boolean,
        canExportReports: Boolean,
        canEditRecords: Boolean,
        canMessageDirectly: Boolean,
        isGroupAdmin: Boolean,
        notes: String? = null,
        location: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        geoEnabled: Boolean = false,
        radius: Int = 200
    ) = withContext(Dispatchers.IO) {
        db.workGroupDao().updateMemberPermissionsAndShift(
            memberId = memberId,
            role = role,
            start = startHours,
            end = endHours,
            shift = shiftHours,
            workingDays = workingDays,
            shiftType = shiftType,
            canPost = canPostAccounting,
            canExport = canExportReports,
            canEdit = canEditRecords,
            canMessage = canMessageDirectly,
            isAdmin = isGroupAdmin,
            notes = notes,
            location = location,
            lat = lat,
            lng = lng,
            geoEnabled = geoEnabled,
            radius = radius
        )
    }

    suspend fun updateGroupPrivacySettings(groupId: String, disableMemberDMs: Boolean, isPrivate: Boolean) = withContext(Dispatchers.IO) {
        db.workGroupDao().updateGroupPrivacySettings(groupId, disableMemberDMs, isPrivate)
    }

    suspend fun saveWorkGroup(workGroup: WorkGroupEntity) = withContext(Dispatchers.IO) {
        db.workGroupDao().insertWorkGroup(workGroup)
    }

    suspend fun saveGroupMember(member: GroupMemberEntity) = withContext(Dispatchers.IO) {
        db.workGroupDao().insertGroupMember(member)
    }

    suspend fun deleteGroupMember(memberId: String) = withContext(Dispatchers.IO) {
        db.workGroupDao().deleteGroupMember(memberId)
    }

    suspend fun addMembersToWorkGroupChat(
        chatId: String,
        existingGroupId: String? = null,
        contacts: List<ContactEntity> = emptyList(),
        manualName: String? = null,
        manualPhone: String? = null,
        roleLabel: String = "عضو فريق العمل",
        isGroupAdmin: Boolean = false,
        actorUser: UserEntity? = null
    ): Int = withContext(Dispatchers.IO) {
        val chat = db.chatDao().getChatById(chatId)
        val resolvedGroupId = existingGroupId?.takeIf { it.isNotBlank() }
            ?: chat?.groupId?.takeIf { it.isNotBlank() }
            ?: chat?.contactId?.takeIf { it.isNotBlank() }
            ?: "wg_$chatId"

        var workGroup = db.workGroupDao().getWorkGroupById(resolvedGroupId)
        if (workGroup == null) {
            val actor = actorUser ?: db.userDao().getCurrentUserSync()
            val ownerId = actor?.id ?: "me"
            workGroup = WorkGroupEntity(
                id = resolvedGroupId,
                name = chat?.contactName?.ifBlank { "مجموعة عمل" } ?: "مجموعة عمل",
                description = "مجموعة عمل وتنفيذ مهام",
                ownerId = ownerId,
                isPrivate = false,
                disableMemberDMs = false
            )
            db.workGroupDao().insertWorkGroup(workGroup)

            val creatorMember = GroupMemberEntity(
                id = UUID.randomUUID().toString(),
                groupId = workGroup.id,
                userId = ownerId,
                username = actor?.name ?: "مدير المجموعة",
                phone = actor?.phone ?: "",
                roleLabel = "مدير المجموعة",
                canPostAccounting = true,
                canExportReports = true,
                canEditRecords = true,
                canMessageDirectly = true,
                isGroupAdmin = true
            )
            db.workGroupDao().insertGroupMember(creatorMember)
        }

        if (chat != null && chat.groupId != workGroup.id) {
            db.chatDao().insertChat(
                chat.copy(
                    isGroup = true,
                    groupId = workGroup.id,
                    groupName = chat.groupName ?: workGroup.name
                )
            )
        }

        val currentMembers = db.workGroupDao().getGroupMembersSync(workGroup.id).toMutableList()
        val linkedProject = db.projectDao().getAllProjects().first().firstOrNull { it.workGroupId == workGroup.id }
        val existingProjMembers = if (linkedProject != null) {
            db.projectMemberDao().getMembersForProject(linkedProject.id).first().toMutableList()
        } else mutableListOf()

        val cleanRole = roleLabel.trim().ifBlank { "عضو فريق العمل" }
        val addedMentions = mutableListOf<String>()

        fun alreadyExists(userId: String, name: String, phone: String): Boolean {
            return currentMembers.any { m ->
                (userId.isNotBlank() && m.userId == userId) ||
                    (phone.isNotBlank() && m.phone == phone) ||
                    m.username.trim().equals(name.trim(), ignoreCase = true)
            }
        }

        val toInsert = mutableListOf<GroupMemberEntity>()
        contacts.distinctBy { it.id }.forEach { contact ->
            val cleanName = contact.name.trim()
            if (cleanName.isNotBlank() && !alreadyExists(contact.id, cleanName, contact.phone.trim())) {
                val member = GroupMemberEntity(
                    id = UUID.randomUUID().toString(),
                    groupId = workGroup.id,
                    userId = contact.id,
                    username = cleanName,
                    phone = contact.phone.trim(),
                    roleLabel = cleanRole,
                    canPostAccounting = isGroupAdmin,
                    canExportReports = isGroupAdmin,
                    canEditRecords = isGroupAdmin,
                    canMessageDirectly = isGroupAdmin || !workGroup.disableMemberDMs,
                    isGroupAdmin = isGroupAdmin
                )
                toInsert.add(member)
                currentMembers.add(member)
            }
        }

        val trimmedManualName = manualName?.trim().orEmpty().removePrefix("@").trim()
        val trimmedManualPhone = manualPhone?.trim().orEmpty()
        if (trimmedManualName.isNotBlank() && !alreadyExists("", trimmedManualName, trimmedManualPhone)) {
            val member = GroupMemberEntity(
                id = UUID.randomUUID().toString(),
                groupId = workGroup.id,
                userId = "member_${UUID.randomUUID().toString().take(8)}",
                username = trimmedManualName,
                phone = trimmedManualPhone,
                roleLabel = cleanRole,
                canPostAccounting = isGroupAdmin,
                canExportReports = isGroupAdmin,
                canEditRecords = isGroupAdmin,
                canMessageDirectly = isGroupAdmin || !workGroup.disableMemberDMs,
                isGroupAdmin = isGroupAdmin
            )
            toInsert.add(member)
            currentMembers.add(member)
        }

        if (toInsert.isEmpty()) return@withContext 0

        db.workGroupDao().insertGroupMembers(toInsert)

        val actor = actorUser ?: db.userDao().getCurrentUserSync()
        toInsert.forEach { added ->
            addedMentions.add("@${added.username.removePrefix("@")}")
            if (linkedProject != null) {
                val projAlreadyExists = existingProjMembers.any { pm ->
                    pm.userId == added.userId ||
                        (added.phone.isNotBlank() && pm.userPhone == added.phone) ||
                        pm.userName.trim().equals(added.username.trim(), ignoreCase = true)
                }
                if (!projAlreadyExists) {
                    val mappedRole = if (added.isGroupAdmin) {
                        ProjectMemberRole.EXECUTIVE_MANAGER
                    } else {
                        ProjectMemberRole.entries.firstOrNull { it.label == added.roleLabel }
                            ?: ProjectMemberRole.EXECUTIVE_MANAGER
                    }
                    val pm = ProjectMemberEntity(
                        id = UUID.randomUUID().toString(),
                        projectId = linkedProject.id,
                        userId = added.userId,
                        userName = added.username,
                        userPhone = added.phone,
                        role = mappedRole.name,
                        assignedTask = added.roleLabel
                    )
                    db.projectMemberDao().insertMember(pm)
                    existingProjMembers.add(pm)
                }
                db.projectAuditLogDao().insertAuditLog(
                    ProjectAuditLogEntity(
                        id = UUID.randomUUID().toString(),
                        projectId = linkedProject.id,
                        action = "إضافة عضو للمجموعة والمشروع",
                        actorName = actor?.name ?: "المدير",
                        actorPhone = actor?.phone ?: "",
                        details = "تمت إضافة ${added.username} (${added.roleLabel}) إلى ${workGroup.name}"
                    )
                )
            }
        }

        val targetChatId = chat?.id ?: ensureWorkGroupChat(workGroup, displayTitle = workGroup.name).id
        sendMessage(
            chatId = targetChatId,
            senderId = "system",
            senderName = "النظام",
            content = "تمت إضافة ${addedMentions.joinToString("، ")} إلى المجموعة",
            type = MessageType.SYSTEM.name
        )

        toInsert.size
    }

    suspend fun postSystemMessageToWorkGroupChat(workGroupId: String, content: String) = withContext(Dispatchers.IO) {
        if (workGroupId.isBlank() || content.isBlank()) return@withContext
        val chat = db.chatDao().getChatByGroupId(workGroupId)
            ?: db.chatDao().getChatById("wg_chat_$workGroupId")
            ?: db.chatDao().getChatById("chat_wg_$workGroupId")
            ?: return@withContext
        sendMessage(
            chatId = chat.id,
            senderId = "system",
            senderName = "النظام",
            content = content,
            type = MessageType.SYSTEM.name
        )
    }

    private fun ProjectMemberEntity.matchesGroupMember(gm: GroupMemberEntity): Boolean {
        return userId == gm.userId ||
            (userPhone.isNotBlank() && gm.phone.isNotBlank() && userPhone.trim() == gm.phone.trim()) ||
            userName.trim().equals(gm.username.trim(), ignoreCase = true)
    }

    suspend fun updateGroupMemberAdminStatus(member: GroupMemberEntity, isAdmin: Boolean) = withContext(Dispatchers.IO) {
        db.workGroupDao().insertGroupMember(
            member.copy(
                isGroupAdmin = isAdmin,
                canPostAccounting = if (isAdmin) true else member.canPostAccounting,
                canExportReports = if (isAdmin) true else member.canExportReports,
                canEditRecords = if (isAdmin) true else member.canEditRecords,
                canMessageDirectly = if (isAdmin) true else member.canMessageDirectly
            )
        )
        val linkedProject = db.projectDao().getProjectByWorkGroupIdSync(member.groupId)
        if (linkedProject != null) {
            val projMembers = db.projectMemberDao().getMembersForProjectSync(linkedProject.id)
            val matched = projMembers.firstOrNull { it.matchesGroupMember(member) }
            val nextRole = if (isAdmin) ProjectMemberRole.EXECUTIVE_MANAGER else ProjectMemberRole.PROCUREMENT_OFFICER
            if (matched != null && matched.role != ProjectMemberRole.OWNER.name) {
                db.projectMemberDao().updateMemberRole(matched.id, nextRole.name)
            }
            db.projectAuditLogDao().insertAuditLog(
                ProjectAuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = linkedProject.id,
                    action = if (isAdmin) "ترقية صلاحية عضو" else "تعديل صلاحية عضو",
                    actorName = db.userDao().getCurrentUserSync()?.name ?: "المدير",
                    actorPhone = db.userDao().getCurrentUserSync()?.phone ?: "",
                    details = "تم تحديث صلاحية ${member.username} في المجموعة والمشروع (${nextRole.label})"
                )
            )
        }
    }

    suspend fun updateGroupMemberRoleAndPermissions(
        member: GroupMemberEntity,
        roleLabel: String,
        projectRole: ProjectMemberRole? = null,
        isAdmin: Boolean,
        canPostAccounting: Boolean,
        canEditRecords: Boolean,
        canExportReports: Boolean,
        canMessageDirectly: Boolean
    ) = withContext(Dispatchers.IO) {
        val cleanRoleLabel = roleLabel.trim().ifBlank { member.roleLabel }
        val updatedMember = member.copy(
            roleLabel = cleanRoleLabel,
            isGroupAdmin = isAdmin,
            canPostAccounting = canPostAccounting || isAdmin,
            canEditRecords = canEditRecords || isAdmin,
            canExportReports = canExportReports || isAdmin,
            canMessageDirectly = canMessageDirectly || isAdmin
        )
        db.workGroupDao().insertGroupMember(updatedMember)

        val linkedProject = db.projectDao().getProjectByWorkGroupIdSync(member.groupId)
        if (linkedProject != null) {
            val resolvedRole = projectRole ?: when {
                isAdmin -> ProjectMemberRole.EXECUTIVE_MANAGER
                canExportReports && !canEditRecords && !canPostAccounting -> ProjectMemberRole.AUDITOR
                canExportReports && canPostAccounting -> ProjectMemberRole.FINANCIAL_PARTNER
                else -> ProjectMemberRole.PROCUREMENT_OFFICER
            }
            val projMembers = db.projectMemberDao().getMembersForProjectSync(linkedProject.id)
            val matched = projMembers.firstOrNull { it.matchesGroupMember(member) }
            if (matched != null) {
                db.projectMemberDao().insertMember(
                    matched.copy(
                        role = if (matched.role == ProjectMemberRole.OWNER.name) ProjectMemberRole.OWNER.name else resolvedRole.name,
                        assignedTask = cleanRoleLabel
                    )
                )
            } else {
                db.projectMemberDao().insertMember(
                    ProjectMemberEntity(
                        id = UUID.randomUUID().toString(),
                        projectId = linkedProject.id,
                        userId = member.userId,
                        userName = member.username,
                        userPhone = member.phone,
                        role = resolvedRole.name,
                        assignedTask = cleanRoleLabel
                    )
                )
            }
            val actor = db.userDao().getCurrentUserSync()
            db.projectAuditLogDao().insertAuditLog(
                ProjectAuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = linkedProject.id,
                    action = "تحديث وظيفة وصلاحيات عضو",
                    actorName = actor?.name ?: "المدير",
                    actorPhone = actor?.phone ?: "",
                    details = "تم تحديث ${member.username}: $cleanRoleLabel (${resolvedRole.label})"
                )
            )
        }
    }

    suspend fun syncProjectMemberToWorkGroup(
        projectId: String,
        member: ProjectMemberEntity,
        isGroupAdmin: Boolean,
        canPostAccounting: Boolean,
        canEditRecords: Boolean,
        canExportReports: Boolean,
        canMessageDirectly: Boolean
    ) = withContext(Dispatchers.IO) {
        val project = db.projectDao().getProjectById(projectId) ?: return@withContext
        val wg = db.workGroupDao().getWorkGroupById(project.workGroupId) ?: return@withContext
        val currentGroupMembers = db.workGroupDao().getGroupMembersSync(wg.id)
        val existingGm = currentGroupMembers.firstOrNull { member.matchesGroupMember(it) }
        val roleEnum = ProjectMemberRole.entries.firstOrNull { it.name == member.role }
            ?: ProjectMemberRole.EXECUTIVE_MANAGER
        val resolvedLabel = member.assignedTask?.takeIf { it.isNotBlank() } ?: roleEnum.label

        val gmToSave = (existingGm ?: GroupMemberEntity(
            id = UUID.randomUUID().toString(),
            groupId = wg.id,
            userId = member.userId,
            username = member.userName,
            phone = member.userPhone,
            roleLabel = resolvedLabel
        )).copy(
            roleLabel = resolvedLabel,
            isGroupAdmin = isGroupAdmin,
            canPostAccounting = canPostAccounting || isGroupAdmin,
            canEditRecords = canEditRecords || isGroupAdmin,
            canExportReports = canExportReports || isGroupAdmin,
            canMessageDirectly = canMessageDirectly || isGroupAdmin
        )
        db.workGroupDao().insertGroupMember(gmToSave)

        val chat = ensureWorkGroupChat(wg, displayTitle = wg.name)
        val mention = "@${member.userName.removePrefix("@")}"
        val msg = if (existingGm == null) {
            "تمت إضافة $mention بوظيفة ($resolvedLabel • ${roleEnum.label}) إلى المشروع والمجموعة"
        } else {
            "تم تحديث دور وصلاحيات $mention إلى ($resolvedLabel • ${roleEnum.label})"
        }
        sendMessage(
            chatId = chat.id,
            senderId = "system",
            senderName = "النظام",
            content = msg,
            type = MessageType.SYSTEM.name
        )
    }

    suspend fun removeProjectMemberWithGroupSync(projectId: String, member: ProjectMemberEntity) = withContext(Dispatchers.IO) {
        db.projectMemberDao().deleteMember(member.id)
        val project = db.projectDao().getProjectById(projectId) ?: return@withContext
        val gmList = db.workGroupDao().getGroupMembersSync(project.workGroupId)
        gmList.filter { member.matchesGroupMember(it) }.forEach { gm ->
            db.workGroupDao().deleteGroupMember(gm.id)
        }
        val actor = db.userDao().getCurrentUserSync()
        db.projectAuditLogDao().insertAuditLog(
            ProjectAuditLogEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                action = "إزالة عضو من المشروع والمجموعة",
                actorName = actor?.name ?: "المدير",
                actorPhone = actor?.phone ?: "",
                details = "تمت إزالة ${member.userName}"
            )
        )
        postSystemMessageToWorkGroupChat(
            workGroupId = project.workGroupId,
            content = "تمت إزالة @${member.userName.removePrefix("@")} من المشروع والمجموعة"
        )
    }

    suspend fun removeGroupMemberFromChat(chatId: String, member: GroupMemberEntity) = withContext(Dispatchers.IO) {
        db.workGroupDao().deleteGroupMember(member.id)
        val linkedProject = db.projectDao().getProjectByWorkGroupIdSync(member.groupId)
        if (linkedProject != null) {
            val projMembers = db.projectMemberDao().getMembersForProjectSync(linkedProject.id)
            projMembers.filter { it.matchesGroupMember(member) }.forEach { pm ->
                db.projectMemberDao().deleteMember(pm.id)
            }
            val actor = db.userDao().getCurrentUserSync()
            db.projectAuditLogDao().insertAuditLog(
                ProjectAuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = linkedProject.id,
                    action = "إزالة عضو من المجموعة والمشروع",
                    actorName = actor?.name ?: "المدير",
                    actorPhone = actor?.phone ?: "",
                    details = "تمت إزالة ${member.username}"
                )
            )
        }
        val chat = db.chatDao().getChatById(chatId)
        if (chat != null) {
            sendMessage(
                chatId = chat.id,
                senderId = "system",
                senderName = "النظام",
                content = "تمت إزالة ${member.username} من المجموعة والمشروع",
                type = MessageType.SYSTEM.name
            )
        }
    }

    suspend fun getWorkGroupByIdSync(groupId: String): WorkGroupEntity? = withContext(Dispatchers.IO) {
        db.workGroupDao().getWorkGroupById(groupId)
    }

    suspend fun getSubGroupsSync(parentGroupId: String): List<WorkGroupEntity> = withContext(Dispatchers.IO) {
        db.workGroupDao().getSubGroupsSync(parentGroupId)
    }

    suspend fun getGroupMembersSync(groupId: String): List<GroupMemberEntity> = withContext(Dispatchers.IO) {
        db.workGroupDao().getGroupMembersSync(groupId)
    }

    suspend fun ensureWorkGroupChat(
        workGroup: WorkGroupEntity,
        displayTitle: String = workGroup.name,
        initialSystemMessage: String? = null
    ): ChatEntity = withContext(Dispatchers.IO) {
        val chatId = "wg_chat_${workGroup.id}"
        val existing = db.chatDao().getChatByGroupId(workGroup.id) ?: db.chatDao().getChatById(chatId)
        if (existing != null) {
            return@withContext existing
        }

        val welcomeText = initialSystemMessage
            ?: "تم إنشاء محادثة مجموعة العمل (${displayTitle}) بنجاح"
        val now = System.currentTimeMillis()
        val newChat = ChatEntity(
            id = chatId,
            contactId = workGroup.id,
            contactName = displayTitle,
            contactType = UserType.BUSINESSMAN.name,
            contactVerified = true,
            isGroup = true,
            groupId = workGroup.id,
            groupName = workGroup.companyName ?: workGroup.name,
            lastMessage = welcomeText,
            lastMessageSenderId = "system",
            lastMessageTime = now,
            createdAt = workGroup.createdAt
        )
        db.chatDao().insertChat(newChat)

        val sysMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = newChat.id,
            senderId = "system",
            senderName = "النظام",
            type = MessageType.SYSTEM.name,
            content = welcomeText,
            timestamp = now,
            status = MessageStatus.READ.name
        )
        val encrypted = RoomLocalAES256Engine.encryptMessage(sysMsg, null, appContext)
        db.messageDao().insertMessage(encrypted)
        newChat
    }

    suspend fun deleteWorkGroup(groupId: String) = withContext(Dispatchers.IO) {
        db.workGroupDao().deleteWorkGroup(groupId)
    }

    suspend fun assignChatContactToProjectWorkGroup(
        chat: ChatEntity,
        workGroup: WorkGroupEntity,
        project: ProjectEntity?,
        roleLabel: String,
        projectRole: ProjectMemberRole,
        canPostAccounting: Boolean,
        canEditRecords: Boolean,
        canExportReports: Boolean,
        canMessageDirectly: Boolean,
        isGroupAdmin: Boolean,
        actorName: String,
        actorPhone: String
    ) = withContext(Dispatchers.IO) {
        val linkedContact = db.contactDao().getContactById(chat.contactId)
        val contactUserId = chat.contactId.takeIf { it.isNotBlank() } ?: chat.id
        val contactPhone = linkedContact?.phone?.takeIf { it.isNotBlank() } ?: ""
        val contactName = chat.contactName.ifBlank { "عضو" }

        val existingGroupMembers = db.workGroupDao().getGroupMembers(workGroup.id).first()
        val existingMember = existingGroupMembers.firstOrNull {
            it.userId == contactUserId || (contactPhone.isNotBlank() && it.phone == contactPhone)
        }

        val memberEntity = GroupMemberEntity(
            id = existingMember?.id ?: UUID.randomUUID().toString(),
            groupId = workGroup.id,
            userId = contactUserId,
            username = contactName,
            phone = contactPhone,
            roleLabel = roleLabel.ifBlank { projectRole.label },
            status = "accepted",
            canPostAccounting = canPostAccounting,
            canExportReports = canExportReports,
            canEditRecords = canEditRecords,
            canMessageDirectly = canMessageDirectly,
            isGroupAdmin = isGroupAdmin,
            addedAt = existingMember?.addedAt ?: System.currentTimeMillis()
        )
        db.workGroupDao().insertGroupMember(memberEntity)

        if (project != null) {
            val existingProjMembers = db.projectMemberDao().getMembersForProject(project.id).first()
            val existingProjMember = existingProjMembers.firstOrNull {
                it.userId == contactUserId || (contactPhone.isNotBlank() && it.userPhone == contactPhone)
            }
            val projMember = ProjectMemberEntity(
                id = existingProjMember?.id ?: UUID.randomUUID().toString(),
                projectId = project.id,
                userId = contactUserId,
                userName = contactName,
                userPhone = contactPhone,
                role = projectRole.name,
                assignedTask = roleLabel.ifBlank { workGroup.name },
                addedAt = existingProjMember?.addedAt ?: System.currentTimeMillis()
            )
            db.projectMemberDao().insertMember(projMember)

            val permTags = buildList {
                if (isGroupAdmin) add("مدير مجموعة")
                if (canPostAccounting) add("ترحيل قيود")
                if (canEditRecords) add("تعديل سجلات")
                if (canExportReports) add("تصدير تقارير")
                if (canMessageDirectly) add("تراسل مباشر")
            }.joinToString(" • ").ifBlank { "عضو" }

            db.projectAuditLogDao().insertAuditLog(
                ProjectAuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = project.id,
                    action = if (existingProjMember == null) "إضافة عضو لمجموعة عمل المشروع" else "تحديث صلاحيات عضو بالمجموعة",
                    actorName = actorName,
                    actorPhone = actorPhone,
                    details = "العضو: $contactName (${roleLabel.ifBlank { projectRole.label }}) • المجموعة: ${workGroup.name} • الصلاحيات: $permTags"
                )
            )
        }

        val groupChatTitle = project?.name?.let { "${workGroup.name} • $it" } ?: workGroup.name
        val groupChat = ensureWorkGroupChat(workGroup, displayTitle = groupChatTitle)
        sendMessage(
            chatId = groupChat.id,
            senderId = "system",
            senderName = "النظام",
            content = "تمت إضافة @$contactName إلى المجموعة بصفة (${roleLabel.ifBlank { projectRole.label }})",
            type = MessageType.SYSTEM.name
        )
    }

    // ---------------- Attendance Logging Methods ----------------
    fun getAllAttendance(): Flow<List<EmployeeAttendanceEntity>> = db.employeeAttendanceDao().getAllAttendance()

    fun getAttendanceForGroup(groupId: String): Flow<List<EmployeeAttendanceEntity>> = db.employeeAttendanceDao().getAttendanceForGroup(groupId)

    fun getAttendanceForMember(memberId: String): Flow<List<EmployeeAttendanceEntity>> = db.employeeAttendanceDao().getAttendanceForMember(memberId)

    suspend fun saveAttendance(attendance: EmployeeAttendanceEntity) = withContext(Dispatchers.IO) {
        db.employeeAttendanceDao().insertAttendance(attendance)
    }

    suspend fun deleteAttendance(id: String) = withContext(Dispatchers.IO) {
        db.employeeAttendanceDao().deleteAttendance(id)
    }

    suspend fun clockInMember(
        member: GroupMemberEntity,
        note: String? = null,
        currentLat: Double? = null,
        currentLng: Double? = null
    ): EmployeeAttendanceEntity = withContext(Dispatchers.IO) {
        val curDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val timeFmt = java.text.SimpleDateFormat("hh:mm a", java.util.Locale("ar")).format(java.util.Date())
        val now = System.currentTimeMillis()

        // Check if existing record for today
        val existing = db.employeeAttendanceDao().getMemberAttendanceForDate(member.id, curDate)
        if (existing != null && existing.clockInTimestamp != null && existing.clockOutTimestamp == null) {
            return@withContext existing
        }

        var isVerified = false
        var verificationMsg = "التتبع الجغرافي غير مفعل"

        if (member.geofencingEnabled) {
            val targetLat = member.workplaceLatitude
            val targetLng = member.workplaceLongitude
            val radius = member.geofencingRadiusMeters.toDouble()

            if (currentLat != null && currentLng != null && targetLat != null && targetLng != null) {
                val results = FloatArray(1)
                android.location.Location.distanceBetween(currentLat, currentLng, targetLat, targetLng, results)
                val distance = results[0]
                if (distance <= radius) {
                    isVerified = true
                    verificationMsg = "تم التحقق: ضمن النطاق (${distance.toInt()}م)"
                } else {
                    isVerified = false
                    verificationMsg = "تنبيه: خارج النطاق (${distance.toInt()}م)"
                }
            } else {
                isVerified = false
                verificationMsg = "خطأ: تعذر الحصول على الإحداثيات المطلوبة"
            }
        }

        val newAttendance = EmployeeAttendanceEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            memberId = member.id,
            groupId = member.groupId,
            employeeName = member.username,
            employeeRole = member.roleLabel,
            date = curDate,
            clockInTime = timeFmt,
            clockOutTime = null,
            clockInTimestamp = now,
            clockOutTimestamp = null,
            scheduledShiftStart = member.workingHoursStart ?: "09:00 ص",
            scheduledShiftEnd = member.workingHoursEnd ?: "05:00 م",
            scheduledShiftHours = member.shiftHours,
            actualWorkedHours = 0.0,
            status = "CLOCKED_IN",
            overtimeMinutes = 0,
            lateMinutes = 0,
            earlyDepartureMinutes = 0,
            locationTag = member.workplaceLocation ?: "الفرع الرئيسي",
            notes = note,
            isLocationVerified = isVerified,
            verificationDetails = verificationMsg,
            timestamp = now
        )
        db.employeeAttendanceDao().insertAttendance(newAttendance)
        newAttendance
    }

    suspend fun clockOutMember(attendanceId: String, note: String? = null) = withContext(Dispatchers.IO) {
        val all = db.employeeAttendanceDao().getAllAttendance().first()
        val target = all.find { it.id == attendanceId } ?: return@withContext
        val timeFmt = java.text.SimpleDateFormat("hh:mm a", java.util.Locale("ar")).format(java.util.Date())
        val now = System.currentTimeMillis()
        val inTime = target.clockInTimestamp ?: (now - (target.scheduledShiftHours * 3600 * 1000).toLong())
        val workedHours = ((now - inTime).toDouble() / (1000 * 3600)).coerceAtLeast(0.1)
        val roundedWorked = kotlin.math.round(workedHours * 100) / 100.0
        val diffMinutes = ((workedHours - target.scheduledShiftHours) * 60).toInt()
        val status = when {
            diffMinutes > 10 -> "OVERTIME"
            diffMinutes < -15 -> "EARLY_DEPARTURE"
            else -> "ON_TIME"
        }
        val overtimeMin = if (diffMinutes > 0) diffMinutes else 0
        val earlyMin = if (diffMinutes < 0) -diffMinutes else 0

        val updated = target.copy(
            clockOutTime = timeFmt,
            clockOutTimestamp = now,
            actualWorkedHours = roundedWorked,
            status = status,
            overtimeMinutes = overtimeMin,
            earlyDepartureMinutes = earlyMin,
            notes = if (!note.isNullOrBlank()) "${target.notes ?: ""} | $note".trim() else target.notes
        )
        db.employeeAttendanceDao().insertAttendance(updated)
    }

    suspend fun updateAppLock(isLockEnabled: Boolean, pin: String, biometricEnabled: Boolean = true): Unit = withContext(Dispatchers.IO) {
        val currentCfg = db.adminConfigDao().getConfig().first() ?: AdminConfigEntity()
        val updated = currentCfg.copy(
            isAppLockEnabled = isLockEnabled,
            appLockPin = pin,
            isBiometricEnabled = biometricEnabled
        )
        db.adminConfigDao().saveConfig(updated)
        val me = db.userDao().getCurrentUserSync()
        if (me != null) {
            db.userDao().updateUserAppLock(me.id, isLockEnabled, pin, biometricEnabled)
        }
    }

    suspend fun sendBroadcastMessage(
        targetCategory: BroadcastTargetCategory,
        title: String,
        content: String,
        isUrgent: Boolean,
        postAsSystemAnnouncement: Boolean
    ): Int = withContext(Dispatchers.IO) {
        val currentAdmin = db.userDao().getCurrentUserSync()?.takeIf { it.type == UserType.ADMIN.name }
            ?: db.userDao().getAllUsers().first().firstOrNull { it.type == UserType.ADMIN.name }
        val senderId = currentAdmin?.id ?: "system_admin"
        val senderName = currentAdmin?.name?.ifBlank { "الإدارة العامة" } ?: "الإدارة العامة"

        val allUsersList = db.userDao().getAllUsers().first()
        val allContactsList = db.contactDao().getAllContacts().first()
        val allSubProfiles = db.hiddenProfileDao().getAllHiddenProfiles().first()
        val allWorkGroupsList = db.workGroupDao().getAllWorkGroups().first()

        val recipientTargets = mutableListOf<Pair<String, String>>()

        when (targetCategory) {
            BroadcastTargetCategory.ALL_USERS -> {
                allUsersList.filter { it.id != senderId && it.type != UserType.ADMIN.name }.forEach {
                    recipientTargets.add(it.id to it.name)
                }
                allContactsList.filter { it.id != senderId && recipientTargets.none { r -> r.first == it.id } }.forEach {
                    recipientTargets.add(it.id to it.name)
                }
            }
            BroadcastTargetCategory.BUSINESSMEN -> {
                allUsersList.filter { it.type == UserType.BUSINESSMAN.name && it.id != senderId }.forEach {
                    recipientTargets.add(it.id to it.name)
                }
                allContactsList.filter { it.type == UserType.BUSINESSMAN.name && recipientTargets.none { r -> r.first == it.id } }.forEach {
                    recipientTargets.add(it.id to it.name)
                }
            }
            BroadcastTargetCategory.GOLD_TRADERS -> {
                allUsersList.filter { it.type == UserType.GOLD_TRADER.name || it.traderStatus == TraderStatus.APPROVED.name }.forEach {
                    recipientTargets.add(it.id to it.name)
                }
                allContactsList.filter { it.type == UserType.GOLD_TRADER.name && recipientTargets.none { r -> r.first == it.id } }.forEach {
                    recipientTargets.add(it.id to it.name)
                }
            }
            BroadcastTargetCategory.SUB_ACCOUNTS -> {
                allSubProfiles.forEach {
                    recipientTargets.add(it.id to it.name)
                }
            }
            BroadcastTargetCategory.WORKGROUPS -> {
                allWorkGroupsList.forEach {
                    recipientTargets.add(it.id to it.name)
                }
            }
            BroadcastTargetCategory.ACTIVE_RECENTLY -> {
                allUsersList.filter { it.id != senderId }.forEach {
                    recipientTargets.add(it.id to it.name)
                }
                allContactsList.filter { it.id != senderId && recipientTargets.none { r -> r.first == it.id } }.forEach {
                    recipientTargets.add(it.id to it.name)
                }
            }
        }

        if (recipientTargets.isEmpty()) {
            allContactsList.filter { it.id != senderId }.forEach {
                recipientTargets.add(it.id to it.name)
            }
        }

        val broadcastPrefix = if (isUrgent) "[بيان عاجل من الإدارة]\n" else "[رسالة إدارية رسمية]\n"
        val fullMessageContent = "$broadcastPrefix**$title**\n\n$content\n\n مرسل رسمي من: $senderName"

        var sentCount = 0
        val now = System.currentTimeMillis()

        // 2. Deliver into each target chat
        recipientTargets.distinctBy { it.first }.forEach { (targetId, targetName) ->
            val existingChat = db.chatDao().getAllChats().first().find { it.contactId == targetId || it.id == targetId }
            val chatId = existingChat?.id ?: UUID.randomUUID().toString()
            if (existingChat == null) {
                val newChat = ChatEntity(
                    id = chatId,
                    contactId = targetId,
                    contactName = targetName,
                    contactType = UserType.BUSINESSMAN.name,
                    contactVerified = true,
                    lastMessage = "$title",
                    lastMessageTime = now,
                    lastMessageSenderId = senderId,
                    unreadCount = 1
                )
                db.chatDao().insertChat(newChat)
            } else {
                db.chatDao().updateLastMessage(chatId, "$title: ${content.take(35)}...", now, senderId, MessageStatus.SENT.name, existingChat.unreadCount + 1)
            }

            val msg = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                type = MessageType.TEXT.name,
                content = fullMessageContent,
                timestamp = now,
                status = MessageStatus.SENT.name
            )
            db.messageDao().insertMessage(RoomLocalAES256Engine.encryptMessage(msg, null, appContext))
            sentCount++
        }

        // 3. Post system announcement if requested
        if (postAsSystemAnnouncement) {
            val announcement = PromoAnnouncementEntity(
                id = UUID.randomUUID().toString(),
                title = "$title",
                content = content,
                promoCode = if (isUrgent) "عاجل" else "رسمي",
                discountPercent = 0,
                targetPlanId = null,
                isActive = true,
                createdAt = now
            )
            db.promoAnnouncementDao().insertAnnouncement(announcement)
        }

        sentCount
    }

    // --- Offline Data Persistence & Sync: Invoices & Tasks Operations ---

    suspend fun addInvoice(invoice: InvoiceEntity) = withContext(Dispatchers.IO) {
        val safeAmount = if (invoice.totalAmount.isFinite() && invoice.totalAmount >= 0.0) invoice.totalAmount else 0.0
        val normalizedPaid = if (invoice.status == InvoiceStatus.PAID.name && invoice.paidAmount <= 0.0) {
            safeAmount
        } else {
            invoice.paidAmount.coerceIn(0.0, safeAmount)
        }
        val existingNumbers = db.invoiceDao().getAllInvoicesSync().map { it.invoiceNumber.uppercase(Locale.US) }.toSet()
        val safeInvNumber = if (invoice.invoiceNumber.isBlank() || invoice.invoiceNumber.contains("-")) {
            com.example.util.ShortCodeGenerator.generateInvoiceCode(existingNumbers)
        } else {
            invoice.invoiceNumber.uppercase(Locale.US)
        }
        val normalizedInvoice = invoice.copy(
            invoiceNumber = safeInvNumber,
            totalAmount = safeAmount,
            paidAmount = normalizedPaid
        )
        val now = System.currentTimeMillis()
        val isOnline = OfflineSyncManager.isNetworkAvailable(appContext)

        db.withTransaction {
            db.invoiceDao().insertInvoice(normalizedInvoice)

            // Sync with linked project accounting if projectId is present
            if (!normalizedInvoice.projectId.isNullOrBlank() && safeAmount > 0.0) {
                val isProjectIncome = normalizedInvoice.sourceType == "POS_BUSINESS" ||
                    normalizedInvoice.status == InvoiceStatus.PAID.name &&
                    normalizedInvoice.category != InvoiceCategory.PROJECT_SUPPLY.name
                if (isProjectIncome) {
                    db.projectDao().addIncomeDelta(normalizedInvoice.projectId, safeAmount)
                } else {
                    db.projectDao().addExpenseDelta(normalizedInvoice.projectId, safeAmount)
                }
            }

            // Sync into encrypted Daily Accounting Records so Excel/Hub stays consistent
            if (safeAmount > 0.0) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
                val isPaidIncome = normalizedInvoice.status == InvoiceStatus.PAID.name || normalizedPaid > 0.0
                val dailyRec = DailyAccountingRecordEntity(
                    id = UUID.randomUUID().toString(),
                    groupId = normalizedInvoice.projectId ?: "invoices_hub",
                    subGroupId = normalizedInvoice.hashtagTag?.let { "#$it" } ?: normalizedInvoice.invoiceNumber,
                    profileId = null,
                    entryDate = todayStr,
                    staffId = normalizedInvoice.senderId,
                    staffName = normalizedInvoice.cashierName ?: normalizedInvoice.senderName,
                    staffRole = if (normalizedInvoice.isExternalBusiness) "أعمال خارجية" else normalizedInvoice.sourceType,
                    dailyRevenue = if (isPaidIncome) normalizedPaid.coerceAtLeast(safeAmount) else 0.0,
                    incomingAmount = 0.0,
                    outgoingAmount = if (!isPaidIncome && normalizedInvoice.category == InvoiceCategory.PROJECT_SUPPLY.name) safeAmount else 0.0,
                    deficitAmount = 0.0,
                    complaintsCount = 0,
                    notes = "فاتورة ${normalizedInvoice.invoiceNumber}: ${normalizedInvoice.title} (${normalizedInvoice.receiverName})",
                    timestamp = now
                )
                db.dailyAccountingRecordDao().insertRecord(
                    RoomLocalAES256Engine.encryptDailyRecord(dailyRec, null, appContext)
                )
            }

            // Safe JSONObject SyncQueue item
            val payload = JSONObject().apply {
                put("invoiceId", normalizedInvoice.id)
                put("invoiceNumber", normalizedInvoice.invoiceNumber)
                put("title", normalizedInvoice.title)
                put("totalAmount", safeAmount)
                put("status", normalizedInvoice.status)
                put("sourceType", normalizedInvoice.sourceType)
            }.toString()
            val syncItem = SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                actionType = "CREATE_INVOICE",
                payloadJson = payload,
                status = if (isOnline) "SYNCED" else "PENDING",
                timestamp = now
            )
            db.syncQueueDao().insertSyncItem(syncItem)
        }
    }

    suspend fun updateInvoiceStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        if (status == InvoiceStatus.PAID.name) {
            val existing = db.invoiceDao().getInvoiceByIdSync(id)
            if (existing != null) {
                val remaining = (existing.totalAmount - existing.paidAmount).coerceAtLeast(0.0)
                if (remaining > 0.01) {
                    val user = db.userDao().getCurrentUserSync()
                    payInvoiceWithSync(
                        invoiceId = id,
                        paymentAmount = remaining,
                        paymentMethod = existing.paymentMethod,
                        actorId = user?.id ?: existing.senderId,
                        actorName = user?.name ?: existing.senderName,
                        notes = "اعتماد سداد الفاتورة بالكامل",
                        postChatNotice = true
                    )
                    return@withContext
                }
            }
        }
        db.invoiceDao().updateInvoiceStatus(id, status)
        val payload = JSONObject().apply {
            put("id", id)
            put("status", status)
        }.toString()
        val syncItem = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            actionType = "UPDATE_INVOICE_STATUS",
            payloadJson = payload,
            status = "PENDING"
        )
        db.syncQueueDao().insertSyncItem(syncItem)
    }

    suspend fun deleteInvoice(id: String) = withContext(Dispatchers.IO) {
        db.invoiceDao().deleteInvoice(id)
    }

    suspend fun addProjectTask(task: ProjectTaskEntity) = withContext(Dispatchers.IO) {
        db.projectTaskDao().insertTask(task)
        val payload = JSONObject().apply {
            put("projectId", task.projectId)
            put("taskId", task.id)
            put("title", task.title)
            put("priority", task.priority)
            put("status", task.status)
        }.toString()
        val syncItem = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            actionType = "CREATE_PROJECT_TASK",
            payloadJson = payload,
            status = "PENDING"
        )
        db.syncQueueDao().insertSyncItem(syncItem)
    }

    suspend fun updateProjectTaskStatus(taskId: String, status: String) = withContext(Dispatchers.IO) {
        db.projectTaskDao().updateTaskStatus(taskId, status)
        val payload = JSONObject().apply {
            put("taskId", taskId)
            put("status", status)
        }.toString()
        val syncItem = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            actionType = "UPDATE_TASK_STATUS",
            payloadJson = payload,
            status = "PENDING"
        )
        db.syncQueueDao().insertSyncItem(syncItem)
    }

    suspend fun assignProjectTask(taskId: String, name: String, phone: String?, userId: String?) = withContext(Dispatchers.IO) {
        db.projectTaskDao().assignTask(taskId, name, phone, userId)
    }

    suspend fun deleteProjectTask(taskId: String) = withContext(Dispatchers.IO) {
        db.projectTaskDao().deleteTask(taskId)
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.projectDao().deleteProject(projectId)
            db.projectTaskDao().deleteTasksForProject(projectId)
        }
    }

    suspend fun saveSmartExcelSheet(
        projectId: String,
        template: SmartSpreadsheetTemplate,
        rows: List<SmartSpreadsheetRow>
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val templateId = "sheet_$projectId"
        val colsArr = JSONArray().apply {
            template.columns.forEach { col ->
                put(JSONObject().apply {
                    put("id", col.id)
                    put("nameAr", col.nameAr)
                    put("type", col.type.name)
                    put("formulaDescription", col.formulaDescription ?: "")
                    put("isHiddenCalculation", col.isHiddenCalculation)
                    put("isUserCustomAdded", col.isUserCustomAdded)
                })
            }
        }
        val kpisArr = JSONArray().apply { template.summaryKpis.forEach { put(it) } }
        val templateEntity = DynamicExcelTemplateEntity(
            id = templateId,
            projectId = projectId,
            activityType = template.activityType.name,
            templateName = template.templateName,
            description = template.description,
            columnsJson = colsArr.toString(),
            defaultRowsJson = "[]",
            summaryKpisJson = kpisArr.toString(),
            updatedAt = now
        )
        val rowEntities = rows.mapIndexed { idx, row ->
            val valuesObj = JSONObject().apply {
                row.values.forEach { (k, v) -> put(k, v) }
            }
            val calcObj = JSONObject().apply {
                row.calculatedResults.forEach { (k, v) ->
                    if (v.isFinite()) put(k, v)
                }
            }
            DynamicExcelRowRecordEntity(
                id = row.id.ifBlank { "${templateId}_r_${idx + 1}" },
                templateId = templateId,
                projectId = projectId,
                rowNumber = idx + 1,
                rowTitle = row.title,
                rowCategory = row.category.ifBlank { "عام" },
                valuesJson = valuesObj.toString(),
                calculatedResultsJson = calcObj.toString(),
                photoUri = row.photoUri,
                videoUri = row.videoUri,
                notes = row.notes,
                createdAt = row.timestamp,
                updatedAt = now
            )
        }
        db.withTransaction {
            db.dynamicExcelDao().insertTemplate(templateEntity)
            db.dynamicExcelDao().clearTemplateRows(templateId)
            if (rowEntities.isNotEmpty()) {
                db.dynamicExcelDao().insertRows(rowEntities)
            }
        }
    }

    suspend fun loadSmartExcelSheet(
        projectId: String,
        projectType: String?
    ): Pair<SmartSpreadsheetTemplate, MutableList<SmartSpreadsheetRow>> = withContext(Dispatchers.IO) {
        val savedTemplate = db.dynamicExcelDao().getTemplateForProjectSync(projectId)
        if (savedTemplate == null) {
            return@withContext SmartExcelTemplateEngine.getOrCreateProjectSheet(projectId, projectType)
        }
        val actType = runCatching {
            com.example.util.ProjectActivityType.valueOf(savedTemplate.activityType)
        }.getOrElse { SmartExcelTemplateEngine.detectActivityType(projectType) }
        val baseTemplate = SmartExcelTemplateEngine.getTemplateForActivity(actType)
        val parsedCols = runCatching {
            val arr = JSONArray(savedTemplate.columnsJson)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val colType = runCatching {
                    com.example.util.SpreadsheetColumnType.valueOf(o.optString("type", "TEXT"))
                }.getOrDefault(com.example.util.SpreadsheetColumnType.TEXT)
                com.example.util.SmartColumnDefinition(
                    id = o.optString("id"),
                    nameAr = o.optString("nameAr"),
                    type = colType,
                    formulaDescription = o.optString("formulaDescription").ifBlank { null },
                    isHiddenCalculation = o.optBoolean("isHiddenCalculation", false),
                    isUserCustomAdded = o.optBoolean("isUserCustomAdded", false)
                )
            }.takeIf { it.isNotEmpty() }
        }.getOrNull()

        val resolvedTemplate = if (parsedCols != null) {
            baseTemplate.copy(
                templateName = savedTemplate.templateName.ifBlank { baseTemplate.templateName },
                description = savedTemplate.description ?: baseTemplate.description,
                columns = parsedCols
            )
        } else baseTemplate

        val rowEntities = db.dynamicExcelDao().getRowsForTemplateSync(savedTemplate.id)
        val loadedRows = if (rowEntities.isNotEmpty()) {
            rowEntities.map { ent ->
                val valMap = mutableMapOf<String, Any>()
                runCatching {
                    val obj = JSONObject(ent.valuesJson)
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val rawVal = obj.opt(k)
                        if (rawVal is Number) {
                            valMap[k] = rawVal.toDouble()
                        } else if (rawVal != null && rawVal != JSONObject.NULL) {
                            valMap[k] = rawVal.toString()
                        }
                    }
                }
                val calcMap = mutableMapOf<String, Double>()
                runCatching {
                    val obj = JSONObject(ent.calculatedResultsJson)
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val d = obj.optDouble(k, 0.0)
                        if (d.isFinite()) calcMap[k] = d
                    }
                }
                SmartSpreadsheetRow(
                    id = ent.id,
                    rowNumber = ent.rowNumber,
                    title = ent.rowTitle,
                    category = ent.rowCategory,
                    values = valMap,
                    calculatedResults = calcMap,
                    photoUri = ent.photoUri,
                    videoUri = ent.videoUri,
                    notes = ent.notes,
                    timestamp = ent.createdAt
                )
            }.toMutableList()
        } else {
            SmartExcelTemplateEngine.getOrCreateProjectSheet(projectId, projectType).second
        }

        SmartExcelTemplateEngine.updateProjectSheet(projectId, resolvedTemplate, loadedRows)
        Pair(resolvedTemplate, loadedRows)
    }

    suspend fun deleteTransaction(transactionId: String) = withContext(Dispatchers.IO) {
        db.transactionDao().deleteTransaction(transactionId)
    }

    suspend fun syncPendingOfflineData(): Int = withContext(Dispatchers.IO) {
        val isOnline = OfflineSyncManager.isNetworkAvailable(appContext)
        val hasSession = com.example.util.RetrofitClient.hasActiveSession()
        if (!isOnline || !hasSession) {
            return@withContext 0
        }

        val serverReachable = try {
            com.example.util.RetrofitClient.apiService().getHealth().isSuccessful
        } catch (_: Exception) {
            false
        }
        if (!serverReachable) {
            return@withContext 0
        }

        val pendingMsgs = db.messageDao().getPendingMessagesSync()
        for (msg in pendingMsgs) {
            db.messageDao().updateMessageStatus(msg.id, MessageStatus.DELIVERED.name)
            db.chatDao().updateLastMessageStatus(msg.chatId, MessageStatus.DELIVERED.name)
        }

        val unsyncedTasks = db.projectTaskDao().getUnsyncedTasksSync()
        for (task in unsyncedTasks) {
            db.projectTaskDao().updateSyncStatus(task.id, true)
        }

        val pendingQueue = db.syncQueueDao().getPendingItemsSync()
        for (item in pendingQueue) {
            db.syncQueueDao().updateSyncStatus(item.id, "SYNCED", null)
        }
        db.syncQueueDao().clearSynced()

        val totalSynced = pendingMsgs.size + unsyncedTasks.size + pendingQueue.size

        val chatsTotal = db.chatDao().getAllChats().first().size
        val projectsTotal = db.projectDao().getProjectsCountSync()
        val messagesTotal = db.messageDao().getMessagesCountSync()
        val tasksTotal = db.projectTaskDao().getTasksCountSync()

        OfflineSyncManager.setLastSyncTimestamp(appContext, System.currentTimeMillis())
        OfflineSyncManager.setCachedDataSummary(
            chatsCount = chatsTotal,
            projectsCount = projectsTotal,
            messagesCount = messagesTotal,
            tasksCount = tasksTotal,
            pendingCount = 0
        )

        totalSynced
    }

    suspend fun insertShiftHandover(handover: ShiftHandoverEntity) = withContext(Dispatchers.IO) {
        db.shiftHandoverDao().insertShiftHandover(handover)
    }

    suspend fun receiveShiftHandover(id: String, receiverId: String, receiverName: String, notes: String?) = withContext(Dispatchers.IO) {
        db.shiftHandoverDao().updateShiftReceiving(id, receiverId, receiverName, notes)
    }

    suspend fun receiveShiftHandoverFull(
        id: String,
        receiverId: String,
        receiverName: String,
        notes: String?,
        merchandiseStatus: String,
        physicalStockChecked: Boolean,
        cashDrawerConfirmed: Boolean,
        invoicesAuditedCount: Int,
        receiverSignature: String?,
        acceptanceType: String,
        goodsDiscrepancyNotes: String?
    ) = withContext(Dispatchers.IO) {
        db.shiftHandoverDao().updateShiftReceivingFull(
            id = id,
            receiverId = receiverId,
            receiverName = receiverName,
            notes = notes,
            merchandiseStatus = merchandiseStatus,
            physicalStockChecked = physicalStockChecked,
            cashDrawerConfirmed = cashDrawerConfirmed,
            invoicesAuditedCount = invoicesAuditedCount,
            receiverSignature = receiverSignature,
            acceptanceType = acceptanceType,
            goodsDiscrepancyNotes = goodsDiscrepancyNotes
        )
    }

    suspend fun applyJointComplaintDeduction(
        complaintId: String,
        primaryAmount: Double,
        receiverAmount: Double,
        receiverId: String?,
        resolutionNotes: String?
    ) = withContext(Dispatchers.IO) {
        db.financialComplaintDao().updateManagerDecision(
            id = complaintId,
            decision = "CONFIRMED_DEDUCTION",
            isDeducted = true,
            notes = resolutionNotes
        )
    }

    suspend fun updateComplaintEmployeeResponseWithMedia(
        id: String,
        status: String,
        reason: String?,
        proofUri: String?,
        proofMediaType: String?,
        isDeducted: Boolean
    ) = withContext(Dispatchers.IO) {
        db.financialComplaintDao().updateEmployeeResponseWithMedia(
            id, status, reason, proofUri, proofMediaType, isDeducted
        )
    }

    suspend fun updateComplaintEmployeeResponse(
        id: String,
        status: String,
        reason: String?,
        proofUri: String?,
        isDeducted: Boolean
    ) = withContext(Dispatchers.IO) {
        db.financialComplaintDao().updateEmployeeResponse(id, status, reason, proofUri, isDeducted)
    }

    suspend fun updateComplaintManagerDecision(
        id: String,
        decision: String,
        notes: String?
    ) = withContext(Dispatchers.IO) {
        db.financialComplaintDao().updateManagerDecision(id, decision, notes)
    }

    suspend fun submitFieldTaskWorkerResponse(
        id: String,
        uri: String?,
        type: String,
        notes: String?,
        userId: String?,
        userName: String?
    ) = withContext(Dispatchers.IO) {
        db.projectTaskDao().submitWorkerResponse(id, uri, type, notes, userId, userName)
    }

    suspend fun confirmAndArchiveFieldTask(
        id: String,
        financialAmount: Double,
        financialNotes: String?
    ) = withContext(Dispatchers.IO) {
        db.projectTaskDao().confirmAndArchiveTask(id, financialAmount, financialNotes)
    }

    // ---------------- Enterprise Business Profile, Company Group, Payroll & Leaves ----------------

    suspend fun getBusinessProfileSync(): BusinessProfileEntity? = withContext(Dispatchers.IO) {
        db.businessHrDao().getBusinessProfileSync()
    }

    suspend fun saveBusinessProfileAndSyncGroup(
        profile: BusinessProfileEntity,
        ownerId: String
    ): WorkGroupEntity = withContext(Dispatchers.IO) {
        val groupId = profile.companyGroupId.ifBlank { "wg_company_main" }
        val groupName = profile.companyGroupName.ifBlank {
            if (profile.companyName.isNotBlank()) "مجموعة موظفي ${profile.companyName}"
            else "مجموعة كل الموظفين"
        }
        val cleanProfile = profile.copy(
            id = "company_profile_main",
            ownerId = ownerId.ifBlank { profile.ownerId.ifBlank { "me" } },
            companyGroupId = groupId,
            companyGroupName = groupName,
            isSetupCompleted = profile.companyName.isNotBlank(),
            updatedAt = System.currentTimeMillis()
        )
        db.businessHrDao().saveBusinessProfile(cleanProfile)

        val existingGroup = db.workGroupDao().getWorkGroupById(groupId)
        val group = WorkGroupEntity(
            id = groupId,
            name = groupName,
            description = cleanProfile.customActivityLabel.ifBlank { cleanProfile.activityType },
            ownerId = cleanProfile.ownerId,
            isAccountingGroup = true,
            isPrivate = true,
            disableMemberDMs = false,
            companyName = cleanProfile.companyName.ifBlank { groupName },
            workType = "PHYSICAL_OPERATIONS",
            createdAt = existingGroup?.createdAt ?: System.currentTimeMillis()
        )
        db.workGroupDao().insertWorkGroup(group)
        ensureWorkGroupChat(
            workGroup = group,
            displayTitle = groupName,
            initialSystemMessage = "تم إنشاء/تحديث مجموعة كل الموظفين (${groupName}) لإدارة المهام والتنبيهات"
        )

        val existingEmployees = db.hiddenSubAccountDao().getAllSubAccounts().first()
        existingEmployees.filter { it.isActiveEmployee }.forEach { emp ->
            syncEmployeeToCompanyGroup(emp, cleanProfile)
        }

        val currentMembers = db.workGroupDao().getGroupMembersSync(groupId)
        currentMembers.forEach { member ->
            db.workGroupDao().insertGroupMember(
                member.copy(
                    workingHoursStart = cleanProfile.workStartTime,
                    workingHoursEnd = cleanProfile.workEndTime,
                    workplaceLocation = cleanProfile.address.ifBlank { cleanProfile.companyName },
                    workplaceLatitude = cleanProfile.hqLatitude,
                    workplaceLongitude = cleanProfile.hqLongitude,
                    geofencingRadiusMeters = cleanProfile.geofenceRadiusMeters.coerceIn(50, 5000)
                )
            )
        }
        group
    }

    suspend fun syncEmployeeToCompanyGroup(
        employee: HiddenSubAccountEntity,
        profile: BusinessProfileEntity? = null
    ) = withContext(Dispatchers.IO) {
        val activeProfile = profile ?: db.businessHrDao().getBusinessProfileSync()
        val groupId = activeProfile?.companyGroupId?.ifBlank { "wg_company_main" } ?: "wg_company_main"
        val existingGroup = db.workGroupDao().getWorkGroupById(groupId)
        val groupName = activeProfile?.companyGroupName?.ifBlank { "مجموعة كل الموظفين" } ?: "مجموعة كل الموظفين"
        if (existingGroup == null) {
            val wg = WorkGroupEntity(
                id = groupId,
                name = groupName,
                ownerId = activeProfile?.ownerId?.ifBlank { "me" } ?: "me",
                isAccountingGroup = true,
                isPrivate = true,
                disableMemberDMs = false,
                companyName = activeProfile?.companyName ?: groupName,
                workType = "PHYSICAL_OPERATIONS"
            )
            db.workGroupDao().insertWorkGroup(wg)
            ensureWorkGroupChat(wg, displayTitle = groupName)
        }

        val memberId = "gm_${employee.id}"
        val existingMember = db.workGroupDao().getGroupMembersSync(groupId).firstOrNull { it.id == memberId }
        val member = GroupMemberEntity(
            id = memberId,
            groupId = groupId,
            userId = employee.id,
            username = employee.displayName,
            phone = employee.phone.orEmpty(),
            roleLabel = employee.roleTitle.ifBlank { "موظف" },
            status = if (employee.isActiveEmployee) "accepted" else "inactive",
            workingHoursStart = activeProfile?.workStartTime ?: "09:00",
            workingHoursEnd = activeProfile?.workEndTime ?: "17:00",
            canPostAccounting = employee.permManageAccounting || employee.canCreateInvoices,
            canExportReports = employee.canViewReports,
            canEditRecords = employee.permManageProjects || employee.permAssignTasks,
            canMessageDirectly = employee.permDirectChat,
            isGroupAdmin = employee.permManageBusiness || employee.permManageStaff,
            notes = "${employee.department} • @${employee.username}",
            addedAt = existingMember?.addedAt ?: System.currentTimeMillis(),
            workplaceLocation = activeProfile?.address?.ifBlank { activeProfile.companyName } ?: "مقر العمل",
            workplaceLatitude = activeProfile?.hqLatitude,
            workplaceLongitude = activeProfile?.hqLongitude,
            geofencingEnabled = employee.geofenceEnforced && activeProfile?.hqLatitude != null && activeProfile.hqLongitude != null,
            geofencingRadiusMeters = (activeProfile?.geofenceRadiusMeters ?: 250).coerceIn(50, 5000)
        )
        db.workGroupDao().insertGroupMember(member)
    }

    suspend fun insertPayrollAdjustment(adjustment: PayrollAdjustmentEntity) = withContext(Dispatchers.IO) {
        db.businessHrDao().insertPayrollAdjustment(adjustment)
    }

    suspend fun deletePayrollAdjustment(id: String) = withContext(Dispatchers.IO) {
        db.businessHrDao().deletePayrollAdjustment(id)
    }

    suspend fun insertEmployeeLeave(leave: EmployeeLeaveEntity) = withContext(Dispatchers.IO) {
        db.businessHrDao().insertEmployeeLeave(leave)
    }

    suspend fun deleteEmployeeLeave(id: String) = withContext(Dispatchers.IO) {
        db.businessHrDao().deleteEmployeeLeave(id)
    }
}



