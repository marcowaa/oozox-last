package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt ASC LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY createdAt ASC LIMIT 1")
    suspend fun getCurrentUserSync(): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id IN (:userIds)")
    suspend fun deleteUsers(userIds: List<String>)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    @Query("UPDATE users SET subscriptionTier = :tier, subscriptionStartTimestamp = :start, subscriptionEndTimestamp = :end, isTrialActive = :isTrial, subscriptionStatus = :status WHERE id = :userId")
    suspend fun updateUserSubscription(userId: String, tier: String, start: Long, end: Long, isTrial: Boolean, status: String)

    @Query("UPDATE users SET appLockEnabled = :enabled, appLockPin = :pin, biometricAuthEnabled = :biometricEnabled WHERE id = :userId")
    suspend fun updateUserAppLock(userId: String, enabled: Boolean, pin: String?, biometricEnabled: Boolean = true)

    @Query("UPDATE users SET jobTitle = :jobTitle, skills = :skills, servicesOffered = :services, showSkillsPublicly = :showSkills, experienceYears = :exp, merchantBio = :bio, education = :education, cvSectionOrder = :cvSectionOrder, traderShopName = :shopName, traderShopAddress = :address, ratingScore = :ratingScore, totalReviewsCount = :reviewsCount WHERE id = :userId")
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
    )

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE phone = :phone LIMIT 1")
    suspend fun getContactByPhone(phone: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTime DESC, createdAt DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE subAccountId IS NULL ORDER BY isPinned DESC, lastMessageTime DESC, createdAt DESC")
    fun getMainChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE subAccountId = :subAccountId ORDER BY isPinned DESC, lastMessageTime DESC, createdAt DESC")
    fun getSubAccountChats(subAccountId: String): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getChatById(id: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE groupId = :groupId LIMIT 1")
    suspend fun getChatByGroupId(groupId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("UPDATE chats SET lastMessage = :lastMsg, lastMessageTime = :time, lastMessageSenderId = :senderId, lastMessageStatus = :lastStatus, unreadCount = :unread WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMsg: String, time: Long, senderId: String, lastStatus: String?, unread: Int)

    @Query("UPDATE chats SET lastMessageStatus = :status WHERE id = :chatId")
    suspend fun updateLastMessageStatus(chatId: String, status: String)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun clearUnread(chatId: String)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :chatId")
    suspend fun setChatPinned(chatId: String, isPinned: Boolean)

    @Query("UPDATE chats SET isArchived = :isArchived WHERE id = :chatId")
    suspend fun setChatArchived(chatId: String, isArchived: Boolean)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC, id DESC LIMIT :limit")
    fun getRecentMessagesForChat(chatId: String, limit: Int): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND (timestamp < :beforeTimestamp OR (timestamp = :beforeTimestamp AND id < :beforeId)) ORDER BY timestamp DESC, id DESC LIMIT :limit")
    suspend fun getOlderMessagesKeyset(chatId: String, beforeTimestamp: Long, beforeId: String, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC, id ASC")
    suspend fun getAllMessagesForChatSync(chatId: String): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId")
    fun getMessageCountForChat(chatId: String): Flow<Int>

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("UPDATE messages SET status = :readStatus WHERE chatId = :chatId AND senderId != :currentUserId AND status != :readStatus")
    suspend fun markIncomingMessagesAsRead(chatId: String, currentUserId: String, readStatus: String = "READ")

    @Query("UPDATE messages SET status = :readStatus WHERE chatId = :chatId AND senderId = :senderId")
    suspend fun markOutgoingMessagesAsRead(chatId: String, senderId: String, readStatus: String = "READ")

    @Query("UPDATE messages SET reaction = :reaction WHERE id = :messageId")
    suspend fun updateMessageReaction(messageId: String, reaction: String?)

    @Query("SELECT * FROM messages WHERE status = 'PENDING' ORDER BY timestamp ASC")
    fun getPendingMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingMessagesSync(): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages")
    fun getMessagesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessagesCountSync(): Int

    @Query("SELECT COUNT(*) FROM messages WHERE status = 'PENDING'")
    fun getPendingMessagesCount(): Flow<Int>
}

@Dao
interface ChatAccountDao {
    @Query("SELECT * FROM chat_accounts WHERE chatId = :chatId ORDER BY createdAt ASC")
    fun getAccountsForChat(chatId: String): Flow<List<ChatAccountEntity>>

    @Query("SELECT * FROM chat_accounts ORDER BY createdAt ASC")
    fun getAllChatAccounts(): Flow<List<ChatAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: ChatAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<ChatAccountEntity>)

    @Query("SELECT * FROM hashtag_accounts WHERE accountId = :accountId")
    fun getHashtagAccountsForAccount(accountId: String): Flow<List<HashtagAccountEntity>>

    @Query("SELECT * FROM hashtag_accounts WHERE chatId = :chatId")
    fun getHashtagsForChat(chatId: String): Flow<List<HashtagAccountEntity>>

    @Query("SELECT * FROM hashtag_accounts")
    fun getAllHashtagAccounts(): Flow<List<HashtagAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHashtagAccount(hashtagAccount: HashtagAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHashtagAccounts(hashtagAccounts: List<HashtagAccountEntity>)

    @Query("SELECT * FROM hashtag_entries WHERE hashtagId = :hashtagId ORDER BY createdAt ASC")
    fun getEntriesForHashtag(hashtagId: String): Flow<List<HashtagEntryEntity>>

    @Query("SELECT * FROM hashtag_entries")
    fun getAllHashtagEntries(): Flow<List<HashtagEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHashtagEntry(entry: HashtagEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHashtagEntries(entries: List<HashtagEntryEntity>)

    @Query("SELECT * FROM chat_accounts WHERE chatId = :chatId ORDER BY createdAt ASC")
    suspend fun getAccountsForChatSync(chatId: String): List<ChatAccountEntity>

    @Query("SELECT * FROM hashtag_accounts WHERE chatId = :chatId")
    suspend fun getHashtagsForChatSync(chatId: String): List<HashtagAccountEntity>

    @Query("SELECT * FROM hashtag_accounts WHERE id = :hashtagId LIMIT 1")
    suspend fun getHashtagByIdSync(hashtagId: String): HashtagAccountEntity?

    @Query("UPDATE hashtag_accounts SET status = :status WHERE id = :hashtagId")
    suspend fun updateHashtagStatus(hashtagId: String, status: String)

    @Query("UPDATE chat_accounts SET status = :status WHERE id = :accountId")
    suspend fun updateAccountStatus(accountId: String, status: String)

    @Query("SELECT * FROM hashtag_entries WHERE hashtagId = :hashtagId ORDER BY createdAt ASC")
    suspend fun getEntriesForHashtagSync(hashtagId: String): List<HashtagEntryEntity>

    @Query("SELECT * FROM hashtag_entries")
    suspend fun getAllHashtagEntriesSync(): List<HashtagEntryEntity>

    @Query("SELECT * FROM entry_comments WHERE targetId = :targetId ORDER BY createdAt ASC")
    fun getCommentsForTarget(targetId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)
}

@Dao
interface WorkGroupDao {
    @Query("SELECT * FROM work_groups ORDER BY createdAt DESC")
    fun getAllWorkGroups(): Flow<List<WorkGroupEntity>>

    @Query("SELECT * FROM work_groups WHERE parentGroupId = :parentGroupId ORDER BY createdAt DESC")
    fun getSubGroups(parentGroupId: String): Flow<List<WorkGroupEntity>>

    @Query("SELECT * FROM work_groups WHERE parentGroupId = :parentGroupId ORDER BY createdAt DESC")
    suspend fun getSubGroupsSync(parentGroupId: String): List<WorkGroupEntity>

    @Query("SELECT * FROM work_groups WHERE profileId = :profileId OR profileId IS NULL ORDER BY createdAt DESC")
    fun getWorkGroupsForProfile(profileId: String): Flow<List<WorkGroupEntity>>

    @Query("SELECT * FROM work_groups WHERE id = :id LIMIT 1")
    suspend fun getWorkGroupById(id: String): WorkGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkGroup(workGroup: WorkGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkGroups(workGroups: List<WorkGroupEntity>)

    @Query("SELECT * FROM group_members")
    fun getAllGroupMembers(): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun getGroupMembers(groupId: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    suspend fun getGroupMembersSync(groupId: String): List<GroupMemberEntity>

    @Query("SELECT * FROM group_members WHERE id = :memberId LIMIT 1")
    fun getMemberById(memberId: String): Flow<GroupMemberEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMember(member: GroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMembers(members: List<GroupMemberEntity>)

    @Query("UPDATE group_members SET roleLabel = :role, workingHoursStart = :start, workingHoursEnd = :end, shiftHours = :shift, workingDays = :workingDays, shiftType = :shiftType, canPostAccounting = :canPost, canExportReports = :canExport, canEditRecords = :canEdit, canMessageDirectly = :canMessage, isGroupAdmin = :isAdmin, notes = :notes, workplaceLocation = :location, workplaceLatitude = :lat, workplaceLongitude = :lng, geofencingEnabled = :geoEnabled, geofencingRadiusMeters = :radius WHERE id = :memberId")
    suspend fun updateMemberPermissionsAndShift(
        memberId: String,
        role: String,
        start: String?,
        end: String?,
        shift: Double,
        workingDays: String,
        shiftType: String,
        canPost: Boolean,
        canExport: Boolean,
        canEdit: Boolean,
        canMessage: Boolean,
        isAdmin: Boolean,
        notes: String? = null,
        location: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        geoEnabled: Boolean = false,
        radius: Int = 200
    )

    @Query("UPDATE work_groups SET disableMemberDMs = :disableDMs, isPrivate = :isPrivate WHERE id = :groupId")
    suspend fun updateGroupPrivacySettings(groupId: String, disableDMs: Boolean, isPrivate: Boolean)

    @Query("DELETE FROM group_members WHERE id = :memberId")
    suspend fun deleteGroupMember(memberId: String)

    @Query("DELETE FROM work_groups WHERE id = :groupId OR parentGroupId = :groupId")
    suspend fun deleteWorkGroup(groupId: String)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE workGroupId = :workGroupId LIMIT 1")
    suspend fun getProjectByWorkGroupIdSync(workGroupId: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    @Query("UPDATE projects SET estimatedCapital = :capital WHERE id = :id")
    suspend fun updateCapital(id: String, capital: Double)

    @Query("UPDATE projects SET totalIncome = totalIncome + :delta WHERE id = :id")
    suspend fun addIncomeDelta(id: String, delta: Double)

    @Query("UPDATE projects SET totalExpenses = totalExpenses + :delta WHERE id = :id")
    suspend fun addExpenseDelta(id: String, delta: Double)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    @Query("SELECT COUNT(*) FROM projects")
    fun getProjectsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectsCountSync(): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM gold_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM gold_transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("UPDATE gold_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM gold_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)
}

@Dao
interface GoldPurchaseDao {
    @Query("SELECT * FROM gold_purchases ORDER BY createdAt DESC")
    fun getAllPurchases(): Flow<List<GoldPurchaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: GoldPurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchases(purchases: List<GoldPurchaseEntity>)
}

@Dao
interface TraderRequestDao {
    @Query("SELECT * FROM trader_requests ORDER BY submittedAt DESC")
    fun getAllTraderRequests(): Flow<List<TraderRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTraderRequest(request: TraderRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTraderRequests(requests: List<TraderRequestEntity>)

    @Query("UPDATE trader_requests SET status = :status, reviewedAt = :reviewedAt, adminNotes = :adminNotes WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, reviewedAt: Long, adminNotes: String?)
}

@Dao
interface AdminConfigDao {
    @Query("SELECT * FROM admin_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<AdminConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: AdminConfigEntity)
}

@Dao
interface ProjectMemberDao {
    @Query("SELECT * FROM project_members WHERE projectId = :projectId")
    fun getMembersForProject(projectId: String): Flow<List<ProjectMemberEntity>>

    @Query("SELECT * FROM project_members WHERE projectId = :projectId")
    suspend fun getMembersForProjectSync(projectId: String): List<ProjectMemberEntity>

    @Query("SELECT * FROM project_members ORDER BY addedAt DESC")
    fun getAllProjectMembers(): Flow<List<ProjectMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: ProjectMemberEntity)

    @Query("UPDATE project_members SET role = :newRole WHERE id = :memberId")
    suspend fun updateMemberRole(memberId: String, newRole: String)

    @Query("DELETE FROM project_members WHERE id = :memberId")
    suspend fun deleteMember(memberId: String)
}

@Dao
interface ProjectAuditLogDao {
    @Query("SELECT * FROM project_audit_logs WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getAuditLogsForProject(projectId: String): Flow<List<ProjectAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: ProjectAuditLogEntity)
}

@Dao
interface ImmutableLedgerDao {
    @Query("SELECT * FROM immutable_ledger ORDER BY timestamp DESC")
    fun getAllLedgerEntries(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM immutable_ledger WHERE accountOwnerId = :ownerId OR counterPartyId = :ownerId ORDER BY timestamp DESC")
    fun getLedgerEntriesForUser(ownerId: String): Flow<List<LedgerEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: LedgerEntryEntity)
}

@Dao
interface ClearingSettlementDao {
    @Query("SELECT * FROM clearing_settlements ORDER BY timestamp DESC")
    fun getAllSettlements(): Flow<List<ClearingSettlementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: ClearingSettlementEntity)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY timestamp ASC")
    fun getPendingItems(): Flow<List<SyncQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET status = :status, retryCount = retryCount + 1, errorMessage = :error WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String, error: String?)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun clearSynced()

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingItemsSync(): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    suspend fun getPendingCountSync(): Int
}

@Dao
interface SubscriptionPlanDao {
    @Query("SELECT * FROM subscription_plans ORDER BY monthlyPrice ASC")
    fun getAllPlans(): Flow<List<SubscriptionPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SubscriptionPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<SubscriptionPlanEntity>)

    @Query("UPDATE subscription_plans SET monthlyPrice = :monthly, discountPercent = :discount, offerTitle = :offerTitle, isOfferActive = :isOfferActive WHERE id = :planId")
    suspend fun updatePlanPriceAndOffer(planId: String, monthly: Double, discount: Int, offerTitle: String?, isOfferActive: Boolean)
}

@Dao
interface SubscriptionRequestDao {
    @Query("SELECT * FROM subscription_requests ORDER BY requestedAt DESC")
    fun getAllRequests(): Flow<List<SubscriptionRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: SubscriptionRequestEntity)

    @Query("UPDATE subscription_requests SET status = :status, reviewedAt = :reviewedAt, adminNotes = :notes WHERE id = :id")
    suspend fun updateRequestStatus(id: String, status: String, reviewedAt: Long, notes: String?)
}

@Dao
interface PromoAnnouncementDao {
    @Query("SELECT * FROM promo_announcements WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveAnnouncements(): Flow<List<PromoAnnouncementEntity>>

    @Query("SELECT * FROM promo_announcements ORDER BY createdAt DESC")
    fun getAllAnnouncements(): Flow<List<PromoAnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: PromoAnnouncementEntity)

    @Query("UPDATE promo_announcements SET isActive = :isActive WHERE id = :id")
    suspend fun setAnnouncementActive(id: String, isActive: Boolean)

    @Query("UPDATE promo_announcements SET isActive = :isActive WHERE id IN (:ids)")
    suspend fun setAnnouncementsActive(ids: List<String>, isActive: Boolean)

    @Query("DELETE FROM promo_announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: String)
}

@Dao
interface MerchantStoreDao {
    @Query("SELECT * FROM merchant_products WHERE isAvailable = 1 ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<MerchantProductEntity>>

    @Query("SELECT * FROM merchant_products WHERE merchantId = :merchantId ORDER BY createdAt DESC")
    fun getProductsByMerchant(merchantId: String): Flow<List<MerchantProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: MerchantProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<MerchantProductEntity>)

    @Query("DELETE FROM merchant_products WHERE id = :productId")
    suspend fun deleteProduct(productId: String)

    @Query("UPDATE merchant_products SET viewsCount = viewsCount + 1 WHERE id = :productId")
    suspend fun incrementProductViews(productId: String)

    @Query("SELECT * FROM merchant_daily_sales WHERE merchantId = :merchantId AND isClosedInDailyReport = 0 ORDER BY saleTimestamp DESC")
    fun getUnclosedSalesForMerchant(merchantId: String): Flow<List<MerchantDailySaleEntity>>

    @Query("SELECT * FROM merchant_daily_sales WHERE merchantId = :merchantId ORDER BY saleTimestamp DESC")
    fun getAllSalesForMerchant(merchantId: String): Flow<List<MerchantDailySaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: MerchantDailySaleEntity)

    @Query("UPDATE merchant_daily_sales SET isClosedInDailyReport = 1 WHERE merchantId = :merchantId AND isClosedInDailyReport = 0")
    suspend fun markSalesAsClosed(merchantId: String)

    @Query("SELECT * FROM merchant_daily_closing_reports WHERE merchantId = :merchantId ORDER BY reportTimestamp DESC")
    fun getClosingReportsForMerchant(merchantId: String): Flow<List<MerchantDailyClosingReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClosingReport(report: MerchantDailyClosingReportEntity)
}

@Dao
interface MerchantReviewDao {
    @Query("SELECT * FROM merchant_reviews WHERE merchantId = :merchantId ORDER BY timestamp DESC")
    fun getReviewsForMerchant(merchantId: String): Flow<List<MerchantReviewEntity>>

    @Query("SELECT * FROM merchant_reviews ORDER BY timestamp DESC")
    fun getAllReviews(): Flow<List<MerchantReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: MerchantReviewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<MerchantReviewEntity>)

    @Query("DELETE FROM merchant_reviews WHERE id = :reviewId")
    suspend fun deleteReview(reviewId: String)
}

@Dao
interface PriceAlertDao {
    @Query("SELECT * FROM price_alerts ORDER BY createdAt DESC")
    fun getAllAlerts(): Flow<List<PriceAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PriceAlertEntity)

    @Query("DELETE FROM price_alerts WHERE id = :alertId")
    suspend fun deleteAlert(alertId: String)

    @Query("UPDATE price_alerts SET isActive = :isActive WHERE id = :alertId")
    suspend fun setAlertActive(alertId: String, isActive: Boolean)

    @Query("UPDATE price_alerts SET isTriggered = :isTriggered WHERE id = :alertId")
    suspend fun setAlertTriggered(alertId: String, isTriggered: Boolean)
}

@Dao
interface HiddenProfileDao {
    @Query("SELECT * FROM hidden_profiles ORDER BY createdAt DESC")
    fun getAllHiddenProfiles(): Flow<List<HiddenProfileEntity>>

    @Query("SELECT * FROM hidden_profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: String): HiddenProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenProfile(profile: HiddenProfileEntity)

    @Query("UPDATE hidden_profiles SET silentNotifications = :silent WHERE id = :profileId")
    suspend fun updateProfileNotificationSetting(profileId: String, silent: Boolean)

    @Query("UPDATE hidden_profiles SET secretPinOrPassword = :newPassword WHERE id = :profileId")
    suspend fun updateProfilePassword(profileId: String, newPassword: String)

    @Query("DELETE FROM hidden_profiles WHERE id = :profileId")
    suspend fun deleteHiddenProfile(profileId: String)
}

@Dao
interface HiddenSubAccountDao {
    @Query("SELECT * FROM hidden_sub_accounts ORDER BY createdAt DESC")
    fun getAllSubAccounts(): Flow<List<HiddenSubAccountEntity>>

    @Query("SELECT * FROM hidden_sub_accounts WHERE id = :id LIMIT 1")
    suspend fun getSubAccountById(id: String): HiddenSubAccountEntity?

    @Query("SELECT * FROM hidden_sub_accounts WHERE username = :username LIMIT 1")
    suspend fun getSubAccountByUsername(username: String): HiddenSubAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubAccount(account: HiddenSubAccountEntity)

    @Update
    suspend fun updateSubAccount(account: HiddenSubAccountEntity)

    @Query("UPDATE hidden_sub_accounts SET muteNotifications = :mute, discreetNotifications = :discreet WHERE id = :id")
    suspend fun updateNotificationSettings(id: String, mute: Boolean, discreet: Boolean)

    @Query("UPDATE hidden_sub_accounts SET passwordHash = :hash, passwordSalt = :salt, rawSecretTrigger = :rawSecret WHERE id = :id")
    suspend fun updatePassword(id: String, hash: String, salt: String, rawSecret: String)

    @Query("UPDATE hidden_sub_accounts SET lastActiveAt = :time WHERE id = :id")
    suspend fun updateLastActive(id: String, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM hidden_sub_accounts WHERE id = :id")
    suspend fun deleteSubAccount(id: String)
}

@Dao
interface DailyAccountingRecordDao {
    @Query("SELECT * FROM daily_accounting_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<DailyAccountingRecordEntity>>

    @Query("SELECT * FROM daily_accounting_records WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getRecordsForGroup(groupId: String): Flow<List<DailyAccountingRecordEntity>>

    @Query("SELECT * FROM daily_accounting_records WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getRecordsForProfile(profileId: String): Flow<List<DailyAccountingRecordEntity>>

    @Query("SELECT * FROM daily_accounting_records WHERE groupId = :groupId AND entryDate = :date ORDER BY timestamp DESC")
    fun getRecordsForGroupAndDate(groupId: String, date: String): Flow<List<DailyAccountingRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DailyAccountingRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<DailyAccountingRecordEntity>)

    @Query("DELETE FROM daily_accounting_records WHERE id = :recordId")
    suspend fun deleteRecord(recordId: String)
}

@Dao
interface EmployeeAttendanceDao {
    @Query("SELECT * FROM employee_attendance ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<EmployeeAttendanceEntity>>

    @Query("SELECT * FROM employee_attendance WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getAttendanceForGroup(groupId: String): Flow<List<EmployeeAttendanceEntity>>

    @Query("SELECT * FROM employee_attendance WHERE memberId = :memberId ORDER BY timestamp DESC")
    fun getAttendanceForMember(memberId: String): Flow<List<EmployeeAttendanceEntity>>

    @Query("SELECT * FROM employee_attendance WHERE groupId = :groupId AND date = :date ORDER BY timestamp DESC")
    fun getAttendanceForGroupAndDate(groupId: String, date: String): Flow<List<EmployeeAttendanceEntity>>

    @Query("SELECT * FROM employee_attendance WHERE memberId = :memberId AND date = :date ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMemberAttendanceForDate(memberId: String, date: String): EmployeeAttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: EmployeeAttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<EmployeeAttendanceEntity>)

    @Query("DELETE FROM employee_attendance WHERE id = :id")
    suspend fun deleteAttendance(id: String)
}

@Dao
interface FinancialComplaintDao {
    @Query("SELECT * FROM financial_complaints ORDER BY timestamp DESC")
    fun getAllComplaints(): Flow<List<FinancialComplaintEntity>>

    @Query("SELECT * FROM financial_complaints WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getComplaintsForGroup(groupId: String): Flow<List<FinancialComplaintEntity>>

    @Query("SELECT * FROM financial_complaints WHERE targetDate = :date ORDER BY timestamp DESC")
    fun getComplaintsForTargetDate(date: String): Flow<List<FinancialComplaintEntity>>

    @Query("SELECT * FROM financial_complaints WHERE groupId = :groupId AND targetDate = :date ORDER BY timestamp DESC")
    fun getComplaintsForGroupAndDate(groupId: String, date: String): Flow<List<FinancialComplaintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: FinancialComplaintEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaints(complaints: List<FinancialComplaintEntity>)

    @Query("DELETE FROM financial_complaints WHERE id = :id")
    suspend fun deleteComplaint(id: String)

    @Query("UPDATE financial_complaints SET status = :status, resolutionNotes = :notes WHERE id = :id")
    suspend fun updateComplaintStatus(id: String, status: String, notes: String?)

    @Query("SELECT * FROM financial_complaints WHERE accusedEmployeeId = :employeeId ORDER BY timestamp DESC")
    fun getComplaintsForAccusedEmployee(employeeId: String): Flow<List<FinancialComplaintEntity>>

    @Query("SELECT * FROM financial_complaints WHERE invoiceId = :invoiceId ORDER BY timestamp DESC")
    fun getComplaintsForInvoice(invoiceId: String): Flow<List<FinancialComplaintEntity>>

    @Query("UPDATE financial_complaints SET employeeResponseStatus = :status, employeeRejectionReason = :reason, employeeRejectionProofUri = :proofUri, employeeProofMediaType = :proofMediaType, isDeductedFromBalance = :isDeducted, employeeResponseTime = :responseTime WHERE id = :id")
    suspend fun updateEmployeeResponseWithMedia(
        id: String,
        status: String,
        reason: String?,
        proofUri: String?,
        proofMediaType: String?,
        isDeducted: Boolean,
        responseTime: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM financial_complaints WHERE id = :id LIMIT 1")
    suspend fun getComplaintById(id: String): FinancialComplaintEntity?

    @Query("UPDATE financial_complaints SET forensicAnalysisCompleted = :analysisCompleted, forensicLiabilityType = :liabilityType, forensicReceiverStaffId = :receiverId, forensicReceiverStaffName = :receiverName, forensicLiabilityDistribution = :liabilityDist, forensicAuditDetails = :auditDetails, forensicRecommendedAction = :recommendedAction, forensicJointDeductionApplied = :jointDeduction WHERE id = :id")
    suspend fun updateForensicAnalysis(
        id: String,
        analysisCompleted: Boolean,
        liabilityType: String?,
        receiverId: String?,
        receiverName: String?,
        liabilityDist: String?,
        auditDetails: String?,
        recommendedAction: String?,
        jointDeduction: Boolean
    )

    @Query("UPDATE financial_complaints SET managerDecision = :decision, isDeductedFromBalance = :isDeducted, resolutionNotes = :notes WHERE id = :id")
    suspend fun updateManagerDecision(id: String, decision: String, isDeducted: Boolean, notes: String?)

    @Query("UPDATE financial_complaints SET employeeResponseStatus = :status, employeeRejectionReason = :reason, employeeRejectionProofUri = :proofUri, isDeductedFromBalance = :isDeducted, employeeResponseTime = :responseTime WHERE id = :id")
    suspend fun updateEmployeeResponse(id: String, status: String, reason: String?, proofUri: String?, isDeducted: Boolean, responseTime: Long = System.currentTimeMillis())

    @Query("UPDATE financial_complaints SET managerDecision = :decision, resolutionNotes = :notes WHERE id = :id")
    suspend fun updateManagerDecision(id: String, decision: String, notes: String?)
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY issueDate DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    fun getInvoiceById(id: String): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceByIdSync(id: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE UPPER(invoiceNumber) = UPPER(:invoiceNumber) OR UPPER(id) = UPPER(:invoiceNumber) LIMIT 1")
    suspend fun getInvoiceByNumberSync(invoiceNumber: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE paymentMethod = :method ORDER BY issueDate DESC")
    fun getInvoicesByPaymentMethod(method: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE cashierName = :cashierName ORDER BY issueDate DESC")
    fun getInvoicesByCashier(cashierName: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE issueDate BETWEEN :startMs AND :endMs ORDER BY issueDate DESC")
    fun getInvoicesBetween(startMs: Long, endMs: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE senderId = :userId OR receiverId = :userId ORDER BY issueDate DESC")
    fun getInvoicesForUser(userId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE projectId = :projectId ORDER BY issueDate DESC")
    fun getInvoicesForProject(projectId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY issueDate DESC")
    fun getInvoicesByStatus(status: String): Flow<List<InvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoices(invoices: List<InvoiceEntity>)

    @Query("UPDATE invoices SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateInvoiceStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE invoices SET paidAmount = :paidAmount, status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateInvoicePayment(id: String, paidAmount: Double, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM invoices WHERE hashtagId = :hashtagId ORDER BY issueDate ASC")
    suspend fun getInvoicesByHashtagIdSync(hashtagId: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE chatId = :chatId ORDER BY issueDate ASC")
    suspend fun getInvoicesByChatIdSync(chatId: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE (receiverId = :clientId OR receiverName = :clientName) AND status != 'PAID' AND status != 'CANCELLED' ORDER BY issueDate ASC")
    suspend fun getUnpaidInvoicesByClientSync(clientId: String, clientName: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoices ORDER BY issueDate DESC")
    suspend fun getAllInvoicesSync(): List<InvoiceEntity>

    @Query("UPDATE invoices SET isSynced = :synced, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: String, synced: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoice(id: String)

    @Query("DELETE FROM invoices")
    suspend fun clearInvoices()
}

@Dao
interface ProjectTaskDao {
    @Query("SELECT * FROM project_tasks ORDER BY createdAt DESC")
    fun getAllProjectTasks(): Flow<List<ProjectTaskEntity>>

    @Query("SELECT * FROM project_tasks WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getTasksForProject(projectId: String): Flow<List<ProjectTaskEntity>>

    @Query("SELECT * FROM project_tasks WHERE id = :id LIMIT 1")
    fun getTaskById(id: String): Flow<ProjectTaskEntity?>

    @Query("SELECT * FROM project_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdSync(id: String): ProjectTaskEntity?

    @Query("SELECT * FROM project_tasks WHERE status = :status ORDER BY createdAt DESC")
    fun getTasksByStatus(status: String): Flow<List<ProjectTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ProjectTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<ProjectTaskEntity>)

    @Query("UPDATE project_tasks SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE project_tasks SET assignedToName = :name, assignedToPhone = :phone, assignedToUserId = :userId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun assignTask(id: String, name: String, phone: String?, userId: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE project_tasks SET isSynced = :synced, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: String, synced: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM project_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)

    @Query("DELETE FROM project_tasks WHERE projectId = :projectId")
    suspend fun deleteTasksForProject(projectId: String)

    @Query("SELECT * FROM project_tasks WHERE isSynced = 0 ORDER BY createdAt DESC")
    fun getUnsyncedTasks(): Flow<List<ProjectTaskEntity>>

    @Query("SELECT * FROM project_tasks WHERE isSynced = 0 ORDER BY createdAt DESC")
    suspend fun getUnsyncedTasksSync(): List<ProjectTaskEntity>

    @Query("SELECT COUNT(*) FROM project_tasks")
    fun getTasksCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM project_tasks")
    suspend fun getTasksCountSync(): Int

    @Query("UPDATE project_tasks SET workerResponseUri = :uri, workerResponseType = :type, workerResponseText = :notes, workerResponseByUserId = :userId, workerResponseByUserName = :userName, workerResponseSubmittedAt = :submittedAt, isSubmittedForReview = 1, status = 'IN_PROGRESS', updatedAt = :submittedAt WHERE id = :id")
    suspend fun submitWorkerResponse(id: String, uri: String?, type: String, notes: String?, userId: String?, userName: String?, submittedAt: Long = System.currentTimeMillis())

    @Query("UPDATE project_tasks SET isArchived = 1, status = 'COMPLETED', financialAmount = :financialAmount, financialNotes = :financialNotes, adminConfirmedAt = :confirmedAt, updatedAt = :confirmedAt WHERE id = :id")
    suspend fun confirmAndArchiveTask(id: String, financialAmount: Double, financialNotes: String?, confirmedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM project_tasks WHERE isArchived = 1 ORDER BY adminConfirmedAt DESC")
    fun getArchivedTasks(): Flow<List<ProjectTaskEntity>>
}

@Dao
interface ShiftHandoverDao {
    @Query("SELECT * FROM shift_handovers ORDER BY logoutTime DESC")
    fun getAllShiftHandovers(): Flow<List<ShiftHandoverEntity>>

    @Query("SELECT * FROM shift_handovers WHERE cashierId = :cashierId ORDER BY logoutTime DESC")
    fun getHandoversForCashier(cashierId: String): Flow<List<ShiftHandoverEntity>>

    @Query("SELECT * FROM shift_handovers WHERE shiftDateText = :date ORDER BY logoutTime DESC")
    fun getHandoversForDate(date: String): Flow<List<ShiftHandoverEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftHandover(handover: ShiftHandoverEntity)

    @Query("SELECT * FROM shift_handovers WHERE id = :id LIMIT 1")
    suspend fun getHandoverById(id: String): ShiftHandoverEntity?

    @Query("UPDATE shift_handovers SET receivedByCashierId = :receiverId, receivedByCashierName = :receiverName, isReceived = 1, receivingNotes = :notes, merchandiseStatus = :merchandiseStatus, physicalStockChecked = :physicalStockChecked, cashDrawerConfirmed = :cashDrawerConfirmed, invoicesAuditedCount = :invoicesAuditedCount, receiverSignature = :receiverSignature, acceptanceType = :acceptanceType, goodsDiscrepancyNotes = :goodsDiscrepancyNotes, receivedAt = :receivedAt WHERE id = :id")
    suspend fun updateShiftReceivingFull(
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
        goodsDiscrepancyNotes: String?,
        receivedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE shift_handovers SET receivedByCashierId = :receiverId, receivedByCashierName = :receiverName, isReceived = 1, receivingNotes = :notes, receivedAt = :receivedAt WHERE id = :id")
    suspend fun updateShiftReceiving(id: String, receiverId: String, receiverName: String, notes: String?, receivedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM shift_handovers WHERE id = :id")
    suspend fun deleteShiftHandover(id: String)
}

@Dao
interface DynamicExcelDao {
    @Query("SELECT * FROM dynamic_excel_templates WHERE projectId = :projectId OR projectId IS NULL ORDER BY updatedAt DESC")
    fun getTemplatesForProject(projectId: String?): Flow<List<DynamicExcelTemplateEntity>>

    @Query("SELECT * FROM dynamic_excel_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: String): DynamicExcelTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: DynamicExcelTemplateEntity)

    @Query("SELECT * FROM dynamic_excel_custom_fields WHERE templateId = :templateId ORDER BY evaluationOrder ASC")
    fun getCustomFields(templateId: String): Flow<List<DynamicExcelCustomFieldEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomField(field: DynamicExcelCustomFieldEntity)

    @Query("SELECT * FROM dynamic_excel_row_records WHERE templateId = :templateId ORDER BY rowNumber ASC")
    fun getRowsForTemplate(templateId: String): Flow<List<DynamicExcelRowRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRow(row: DynamicExcelRowRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRows(rows: List<DynamicExcelRowRecordEntity>)

    @Query("SELECT * FROM dynamic_excel_templates WHERE projectId = :projectId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getTemplateForProjectSync(projectId: String): DynamicExcelTemplateEntity?

    @Query("SELECT * FROM dynamic_excel_row_records WHERE templateId = :templateId ORDER BY rowNumber ASC")
    suspend fun getRowsForTemplateSync(templateId: String): List<DynamicExcelRowRecordEntity>

    @Query("DELETE FROM dynamic_excel_row_records WHERE id = :rowId")
    suspend fun deleteRow(rowId: String)

    @Query("DELETE FROM dynamic_excel_row_records WHERE templateId = :templateId")
    suspend fun clearTemplateRows(templateId: String)
}

@Dao
interface BusinessHrDao {
    @Query("SELECT * FROM business_profiles LIMIT 1")
    fun observeBusinessProfile(): Flow<BusinessProfileEntity?>

    @Query("SELECT * FROM business_profiles LIMIT 1")
    suspend fun getBusinessProfileSync(): BusinessProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBusinessProfile(profile: BusinessProfileEntity)

    @Query("SELECT * FROM payroll_adjustments ORDER BY timestamp DESC")
    fun getAllPayrollAdjustments(): Flow<List<PayrollAdjustmentEntity>>

    @Query("SELECT * FROM payroll_adjustments WHERE employeeId = :employeeId ORDER BY timestamp DESC")
    fun getPayrollForEmployee(employeeId: String): Flow<List<PayrollAdjustmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayrollAdjustment(adjustment: PayrollAdjustmentEntity)

    @Query("DELETE FROM payroll_adjustments WHERE id = :id")
    suspend fun deletePayrollAdjustment(id: String)

    @Query("SELECT * FROM employee_leaves ORDER BY startDate DESC")
    fun getAllEmployeeLeaves(): Flow<List<EmployeeLeaveEntity>>

    @Query("SELECT * FROM employee_leaves WHERE employeeId = :employeeId ORDER BY startDate DESC")
    fun getLeavesForEmployee(employeeId: String): Flow<List<EmployeeLeaveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployeeLeave(leave: EmployeeLeaveEntity)

    @Query("DELETE FROM employee_leaves WHERE id = :id")
    suspend fun deleteEmployeeLeave(id: String)
}
