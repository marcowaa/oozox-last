package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.GoldChatRepository
import com.example.util.GoldUtils
import com.example.util.NotificationHelper
import com.example.util.SmartAccountantEngine
import com.example.util.GeminiAiService
import com.example.util.SecureBackupVaultEngine
import com.example.util.BackupResult
import com.example.util.MessengerBubbleManager
import com.example.util.RoomLocalAES256Engine
import com.example.util.SmartSpreadsheetRow
import com.example.util.SmartSpreadsheetTemplate
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = GoldChatRepository(application)

    private val safeExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        com.example.ui.components.ToastNotificationBus.showToast(
            message = throwable.message?.takeIf { it.isNotBlank() } ?: "حدث خطأ غير متوقع أثناء معالجة العملية",
            type = com.example.ui.components.AppToastType.ERROR
        )
    }

    private val olderMessagesCache = MutableStateFlow<Map<String, List<MessageEntity>>>(emptyMap())
    private val loadingOlderChats = MutableStateFlow<Set<String>>(emptySet())
    private val exhaustedOlderChats = MutableStateFlow<Set<String>>(emptySet())
    private val latestLoadedCursorByChat = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, String>>()

    val isInitialized = MutableStateFlow(false)

    private val _signalingManager = MutableStateFlow<com.example.util.WebRtcSignalingManager?>(null)
    val signalingManager: StateFlow<com.example.util.WebRtcSignalingManager?> = _signalingManager.asStateFlow()

    private val _incomingCallPayload = MutableStateFlow<com.example.util.SignalingPayload?>(null)
    val incomingCallPayload: StateFlow<com.example.util.SignalingPayload?> = _incomingCallPayload.asStateFlow()

    private val _incomingTransferSignal = MutableStateFlow(0L)
    val incomingTransferSignal: StateFlow<Long> = _incomingTransferSignal.asStateFlow()

    private var signalingCollectorJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            launch {
                com.example.util.FcmNotificationManager.incomingTransferPushEvents.collect {
                    _incomingTransferSignal.value = System.currentTimeMillis()
                }
            }
            launch {
                com.example.util.FcmNotificationManager.sessionRevokedPushEvents.collect { reason ->
                    com.example.ui.components.ToastNotificationBus.showToast(
                        message = reason,
                        type = com.example.ui.components.AppToastType.ERROR
                    )
                    logout()
                }
            }
            launch {
                com.example.util.FcmNotificationManager.incomingCallPushEvents.collect { payload ->
                    _incomingCallPayload.value = payload
                }
            }
        }
    }

    val currentUser: StateFlow<UserEntity?> = repository.currentUser.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val allContacts: StateFlow<List<ContactEntity>> = repository.allContacts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allChats: StateFlow<List<ChatEntity>> = repository.allChats.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _activeSubAccount = MutableStateFlow<HiddenSubAccountEntity?>(null)
    val activeSubAccount: StateFlow<HiddenSubAccountEntity?> = _activeSubAccount.asStateFlow()

    val currentDisplayChats: StateFlow<List<ChatEntity>> = combine(allChats, _activeSubAccount) { chats, subAccount ->
        if (subAccount != null) {
            chats.filter { it.subAccountId == subAccount.id }
        } else {
            chats.filter { it.subAccountId == null }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allGoldPurchases: StateFlow<List<GoldPurchaseEntity>> = repository.allGoldPurchases.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allWorkGroups: StateFlow<List<WorkGroupEntity>> = repository.allWorkGroups.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allGroupMembers: StateFlow<List<GroupMemberEntity>> = repository.allGroupMembers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allProjectMembers: StateFlow<List<ProjectMemberEntity>> = repository.allProjectMembers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allTraderRequests: StateFlow<List<TraderRequestEntity>> = repository.allTraderRequests.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val adminConfig: StateFlow<AdminConfigEntity?> = repository.adminConfig.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val allLedgerEntries: StateFlow<List<LedgerEntryEntity>> = repository.allLedgerEntries.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allSettlements: StateFlow<List<ClearingSettlementEntity>> = repository.allSettlements.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val pendingSyncItems: StateFlow<List<SyncQueueEntity>> = repository.pendingSyncItems.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allSubscriptionPlans: StateFlow<List<SubscriptionPlanEntity>> = repository.allSubscriptionPlans.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allSubscriptionRequests: StateFlow<List<SubscriptionRequestEntity>> = repository.allSubscriptionRequests.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val activePromoAnnouncements: StateFlow<List<PromoAnnouncementEntity>> = repository.activePromoAnnouncements.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allPromoAnnouncements: StateFlow<List<PromoAnnouncementEntity>> = repository.allPromoAnnouncements.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allMerchantProducts: StateFlow<List<MerchantProductEntity>> = repository.allMerchantProducts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allChatAccounts: StateFlow<List<ChatAccountEntity>> = repository.allChatAccounts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allHashtagAccounts: StateFlow<List<HashtagAccountEntity>> = repository.allHashtagAccounts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allHashtagEntries: StateFlow<List<HashtagEntryEntity>> = repository.allHashtagEntries.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allPriceAlerts: StateFlow<List<PriceAlertEntity>> = repository.allPriceAlerts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allHiddenProfiles: StateFlow<List<HiddenProfileEntity>> = repository.allHiddenProfiles.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allSubAccounts: StateFlow<List<HiddenSubAccountEntity>> = repository.allSubAccounts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _activeHiddenProfile = MutableStateFlow<HiddenProfileEntity?>(null)
    val activeHiddenProfile: StateFlow<HiddenProfileEntity?> = _activeHiddenProfile.asStateFlow()

    val allDailyAccountingRecords: StateFlow<List<DailyAccountingRecordEntity>> = repository.allDailyAccountingRecords.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allFinancialComplaints: StateFlow<List<FinancialComplaintEntity>> = repository.allFinancialComplaints.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allAttendanceRecords: StateFlow<List<EmployeeAttendanceEntity>> = repository.getAllAttendance().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allInvoices: StateFlow<List<InvoiceEntity>> = repository.allInvoices.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allProjectTasks: StateFlow<List<ProjectTaskEntity>> = repository.allProjectTasks.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allShiftHandovers: StateFlow<List<ShiftHandoverEntity>> = repository.allShiftHandovers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val businessProfile: StateFlow<BusinessProfileEntity?> = repository.businessProfile.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val allPayrollAdjustments: StateFlow<List<PayrollAdjustmentEntity>> = repository.allPayrollAdjustments.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allEmployeeLeaves: StateFlow<List<EmployeeLeaveEntity>> = repository.allEmployeeLeaves.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun getTasksForProject(projectId: String): Flow<List<ProjectTaskEntity>> = repository.getTasksForProject(projectId)
    fun getInvoicesForProject(projectId: String): Flow<List<InvoiceEntity>> = repository.getInvoicesForProject(projectId)

    fun addProjectTask(
        projectId: String,
        projectName: String,
        title: String,
        description: String? = null,
        assignedToName: String? = null,
        assignedToPhone: String? = null,
        assignedToUserId: String? = null,
        priority: String = TaskPriority.MEDIUM.name,
        dueDate: String? = null,
        costEstimate: Double = 0.0,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val task = ProjectTaskEntity(
                id = java.util.UUID.randomUUID().toString(),
                projectId = projectId,
                projectName = projectName,
                title = title,
                description = description,
                assignedToName = assignedToName,
                assignedToPhone = assignedToPhone,
                assignedToUserId = assignedToUserId,
                priority = priority,
                status = TaskStatus.PENDING.name,
                dueDate = dueDate,
                costEstimate = costEstimate,
                isSynced = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.addProjectTask(task)
            com.example.ui.components.ToastNotificationBus.showProjectUpdate(projectName, "تمت إضافة مهمة جديدة: $title")
            onComplete()
        }
    }

    fun insertProjectTask(task: ProjectTaskEntity, targetChatId: String? = null) {
        viewModelScope.launch {
            repository.addProjectTask(task)
            val assignees = task.assignedMembersList.ifBlank { task.assignedToName ?: "فريق العمل" }
            repository.addProjectAuditLog(
                ProjectAuditLogEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    projectId = task.projectId,
                    action = "إسناد مهمة ميدانية",
                    actorName = currentUser.value?.name ?: "المدير",
                    actorPhone = currentUser.value?.phone ?: "",
                    details = "تم إسناد (${task.title}) إلى: $assignees"
                )
            )
            val resolvedChatId = targetChatId ?: task.workGroupId?.let { wgId ->
                repository.getWorkGroupByIdSync(wgId)?.let { wg ->
                    repository.ensureWorkGroupChat(wg, displayTitle = wg.name).id
                }
            }
            if (!resolvedChatId.isNullOrBlank()) {
                val me = currentUser.value ?: repository.currentUser.firstOrNull()
                repository.sendMessage(
                    chatId = resolvedChatId,
                    senderId = me?.id ?: "me",
                    senderName = me?.name ?: "المدير",
                    content = "[FIELD_TASK:${task.id}]",
                    type = "FIELD_TASK"
                )
            }
            NotificationHelper.showProjectUpdateNotification(
                context = getApplication(),
                projectId = task.projectId,
                projectName = task.projectName,
                title = "مهمة جديدة مسندة إليك 📋",
                updateMessage = "أسند المدير مهمة (${task.title}) إلى: $assignees • المدة: ${task.deadlineDurationText}"
            )
            com.example.ui.components.ToastNotificationBus.showProjectUpdate(
                task.projectName,
                "تم إسناد المهمة وإرسال إشعار للموظف: ${task.title}"
            )
            if (com.example.util.RetrofitClient.hasActiveSession()) {
                try {
                    val parsedAssignees = assignees.split(" | ", "، ", ",").map { it.trim() }.filter { it.isNotBlank() }
                    com.example.util.RetrofitClient.apiService().createWorkTask(
                        com.example.util.ApiCreateTaskRequest(
                            projectId = task.projectId,
                            projectName = task.projectName,
                            workGroupId = task.workGroupId ?: "wg_company_main",
                            title = task.title,
                            description = task.description.orEmpty(),
                            timingMode = task.timingMode,
                            durationValue = task.durationValue,
                            assignees = parsedAssignees.ifEmpty { listOf("فريق العمل") },
                            priority = task.priority,
                            financialAmount = task.financialAmount,
                            financialNotes = task.financialNotes.orEmpty()
                        )
                    )
                } catch (_: Exception) {}
            }
        }
    }

    fun updateProjectTaskStatus(taskId: String, status: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateProjectTaskStatus(taskId, status)
            val task = repository.getProjectTaskByIdSync(taskId)
            if (task != null) {
                val statusLabel = when (status) {
                    TaskStatus.COMPLETED.name -> "مكتملة ✅"
                    TaskStatus.IN_PROGRESS.name -> "قيد التنفيذ ⏳"
                    else -> "قيد الانتظار 📌"
                }
                repository.addProjectAuditLog(
                    ProjectAuditLogEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        projectId = task.projectId,
                        action = "تحديث حالة مهمة",
                        actorName = currentUser.value?.name ?: "المدير",
                        actorPhone = currentUser.value?.phone ?: "",
                        details = "تم تحديث (${task.title}) إلى: $statusLabel"
                    )
                )
                val targetWgId = task.workGroupId
                    ?: repository.getProjectByIdSync(task.projectId)?.workGroupId
                if (!targetWgId.isNullOrBlank()) {
                    repository.postSystemMessageToWorkGroupChat(
                        workGroupId = targetWgId,
                        content = "تم تحديث حالة مهمة (${task.title}) إلى $statusLabel"
                    )
                }
            }
            com.example.ui.components.ToastNotificationBus.showProjectUpdate("المشاريع", "تم تحديث حالة المهمة بنجاح")
            onComplete()
        }
    }

    fun deleteProjectTask(taskId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteProjectTask(taskId)
            com.example.ui.components.ToastNotificationBus.showProjectUpdate("المشاريع", "تم حذف المهمة")
            onComplete()
        }
    }

    fun deleteProject(projectId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            com.example.ui.components.ToastNotificationBus.showProjectUpdate("المشاريع", "تم حذف المشروع بنجاح")
            onComplete()
        }
    }

    fun syncOfflineData() {
        viewModelScope.launch {
            val count = repository.syncPendingOfflineData()
            if (count > 0) {
                com.example.ui.components.ToastNotificationBus.showToast(
                    message = "تمت مزامنة $count عنصر بنجاح وتحديث ذاكرة Room ☁️",
                    type = com.example.ui.components.AppToastType.SUCCESS
                )
            } else {
                com.example.ui.components.ToastNotificationBus.showToast(
                    message = "جميع بيانات Room محدثة بالكامل ومزامنة ☁️",
                    type = com.example.ui.components.AppToastType.INFO
                )
            }
        }
    }

    fun deleteTransaction(transactionId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
            onComplete()
        }
    }

    fun addInvoice(
        invoiceNumber: String,
        title: String,
        category: String = InvoiceCategory.GENERAL.name,
        receiverId: String,
        receiverName: String,
        totalAmount: Double,
        goldWeightGrams: Double? = null,
        goldKarat: String? = null,
        pricePerGram: Double? = null,
        manufacturingCost: Double? = null,
        status: String = InvoiceStatus.PENDING.name,
        notes: String? = null,
        projectId: String? = null,
        projectName: String? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val me = currentUser.value
            val senderId = me?.id ?: "me"
            val senderName = me?.name?.ifBlank { "المستخدم" } ?: "المستخدم"
            val invoice = InvoiceEntity(
                id = java.util.UUID.randomUUID().toString(),
                invoiceNumber = invoiceNumber,
                title = title,
                category = category,
                senderId = senderId,
                senderName = senderName,
                receiverId = receiverId,
                receiverName = receiverName,
                totalAmount = totalAmount,
                goldWeightGrams = goldWeightGrams,
                goldKarat = goldKarat,
                pricePerGram = pricePerGram,
                manufacturingCost = manufacturingCost,
                status = status,
                notes = notes,
                projectId = projectId,
                projectName = projectName,
                isSynced = false,
                issueDate = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.addInvoice(invoice)
            onComplete()
        }
    }

    fun updateInvoiceStatus(invoiceId: String, status: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateInvoiceStatus(invoiceId, status)
            onComplete()
        }
    }

    fun deleteInvoice(invoiceId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteInvoice(invoiceId)
            onComplete()
        }
    }

    fun checkAndUnlockHiddenProfile(searchCandidate: String): HiddenProfileEntity? {
        val trimmed = searchCandidate.trim()
        if (trimmed.isBlank() || trimmed.length < 2) return null

        val matched = allHiddenProfiles.value.find { profile ->
            profile.secretPinOrPassword.trim() == trimmed
        }
        if (matched != null) {
            _activeHiddenProfile.value = matched
            RoomLocalAES256Engine.registerUnlockedProfile(matched)
        }
        return matched
    }

    fun checkAndUnlockSubAccount(searchCandidate: String): HiddenSubAccountEntity? {
        val trimmed = searchCandidate.trim()
        if (trimmed.isBlank() || trimmed.length < 2) return null
        val matched = com.example.util.HiddenAccountSecurityEngine.evaluateSearchTrigger(trimmed, allSubAccounts.value)
        if (matched != null) {
            _activeSubAccount.value = matched
            com.example.util.HiddenAccountSecurityEngine.setActiveSubAccount(matched)
            viewModelScope.launch {
                repository.updateSubAccount(matched.copy(lastActiveAt = System.currentTimeMillis()))
            }
        }
        return matched
    }

    fun loginEmployee(phoneOrUsername: String, pin: String): Boolean {
        val cleanInput = phoneOrUsername.trim().removePrefix("@")
        val cleanSecret = pin.trim()
        val matched = allSubAccounts.value.find {
            val matchesUser = it.phone.equals(cleanInput, ignoreCase = true) ||
                it.username.equals(cleanInput, ignoreCase = true)
            val matchesPassword = com.example.util.HiddenAccountSecurityEngine.verifyPassword(cleanSecret, it)
            matchesUser && matchesPassword && it.isActiveEmployee
        }
        if (matched != null) {
            _activeSubAccount.value = matched
            com.example.util.HiddenAccountSecurityEngine.setActiveSubAccount(matched)
            viewModelScope.launch {
                repository.updateSubAccount(matched.copy(lastActiveAt = System.currentTimeMillis()))
            }
            return true
        }
        return false
    }

    fun switchActiveSubAccount(subAccount: HiddenSubAccountEntity?) {
        _activeSubAccount.value = subAccount
        com.example.util.HiddenAccountSecurityEngine.setActiveSubAccount(subAccount)
    }

    fun lockToMainAccount() {
        _activeSubAccount.value = null
        com.example.util.HiddenAccountSecurityEngine.setActiveSubAccount(null)
    }

    fun toggleSubAccountNotifications(accountId: String, mute: Boolean, discreet: Boolean = false) {
        viewModelScope.launch {
            repository.updateSubAccountNotifications(accountId, mute, discreet)
            if (_activeSubAccount.value?.id == accountId) {
                _activeSubAccount.value = _activeSubAccount.value?.copy(
                    muteNotifications = mute,
                    discreetNotifications = discreet
                )
            }
        }
    }

    fun updateSubAccountPassword(accountId: String, newPassword: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateSubAccountPassword(accountId, newPassword)
            val updated = allSubAccounts.value.find { it.id == accountId }
            if (_activeSubAccount.value?.id == accountId && updated != null) {
                _activeSubAccount.value = updated
            }
            onSuccess()
        }
    }

    fun createSubAccount(
        username: String,
        displayName: String,
        secretPassword: String,
        avatarUri: String? = null,
        bio: String? = "حساب oozox فرعي مشفر ومحمي",
        phone: String? = null,
        muteNotifications: Boolean = true,
        discreetNotifications: Boolean = false,
        onSuccess: (HiddenSubAccountEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val salt = com.example.util.HiddenAccountSecurityEngine.generateSalt()
            val hash = com.example.util.HiddenAccountSecurityEngine.hashPassword(secretPassword.trim(), salt)
            val newAccount = HiddenSubAccountEntity(
                id = java.util.UUID.randomUUID().toString(),
                username = username.trim().removePrefix("@"),
                displayName = displayName.trim(),
                passwordHash = hash,
                passwordSalt = salt,
                rawSecretTrigger = secretPassword.trim(),
                avatarUri = avatarUri,
                bio = bio,
                phone = phone,
                muteNotifications = muteNotifications,
                discreetNotifications = discreetNotifications,
                autoLockOnExit = true,
                isEncrypted = true,
                createdAt = System.currentTimeMillis(),
                lastActiveAt = System.currentTimeMillis()
            )
            repository.addHiddenSubAccount(newAccount)
            
            // Create a group chat automatically for the store & sub-account containing the staff ID
            val groupChatId = "group_chat_${newAccount.id}"
            val groupChat = ChatEntity(
                id = groupChatId,
                contactId = newAccount.id,
                contactName = "جروب محاسبي - ${newAccount.displayName}",
                contactType = UserType.BUSINESSMAN.name,
                isGroup = true,
                groupId = groupChatId,
                groupName = "جروب محاسبي - ${newAccount.displayName}",
                lastMessage = "تم إنشاء الجروب تلقائياً وتفعيل الحساب: @${newAccount.username}",
                lastMessageSenderId = "system",
                lastMessageTime = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            repository.db.chatDao().insertChat(groupChat)

            val systemMsg = MessageEntity(
                id = "msg_welcome_${newAccount.id}",
                chatId = groupChatId,
                senderId = "system",
                senderName = "النظام",
                content = "أهلاً بك! تم إنشاء هذا الجروب المحاسبي التلقائي لربط المالك والموظفين بالـ ID وحساباتهم.",
                timestamp = System.currentTimeMillis(),
                status = "READ"
            )
            repository.db.messageDao().insertMessage(systemMsg)

            onSuccess(newAccount)
        }
    }

    fun deleteSubAccount(accountId: String) {
        viewModelScope.launch {
            if (_activeSubAccount.value?.id == accountId) {
                _activeSubAccount.value = null
                com.example.util.HiddenAccountSecurityEngine.setActiveSubAccount(null)
            }
            repository.deleteSubAccount(accountId)
            if (com.example.util.RetrofitClient.hasActiveSession()) {
                try {
                    com.example.util.RetrofitClient.apiService().deleteEmployee(accountId)
                } catch (_: Exception) {}
            }
        }
    }

    fun switchActiveProfile(profile: HiddenProfileEntity?) {
        _activeHiddenProfile.value = profile
        if (profile != null) {
            RoomLocalAES256Engine.registerUnlockedProfile(profile)
        } else {
            RoomLocalAES256Engine.clearUnlockedProfiles()
        }
    }

    fun lockToMainProfile() {
        _activeHiddenProfile.value = null
        RoomLocalAES256Engine.clearUnlockedProfiles()
    }

    fun toggleProfileNotifications(profileId: String, silent: Boolean) {
        viewModelScope.launch {
            repository.updateProfileNotificationSetting(profileId, silent)
            if (_activeHiddenProfile.value?.id == profileId) {
                _activeHiddenProfile.value = _activeHiddenProfile.value?.copy(silentNotifications = silent)
            }
        }
    }

    fun updateProfilePassword(profileId: String, newPassword: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateProfilePassword(profileId, newPassword)
            if (_activeHiddenProfile.value?.id == profileId) {
                _activeHiddenProfile.value = _activeHiddenProfile.value?.copy(secretPinOrPassword = newPassword)
            }
            onSuccess()
        }
    }

    fun createHiddenProfile(
        name: String,
        secretPin: String,
        companyName: String,
        roleTitle: String = "المدير المالي والمسؤول",
        initialCapital: Double = 0.0,
        silentNotifications: Boolean = true,
        notes: String? = null,
        onSuccess: (HiddenProfileEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val newProfile = HiddenProfileEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                secretPinOrPassword = secretPin,
                companyName = companyName,
                roleTitle = roleTitle,
                initialCapital = initialCapital,
                isEncrypted = true,
                silentNotifications = silentNotifications,
                notes = notes,
                createdAt = System.currentTimeMillis()
            )
            repository.addHiddenProfile(newProfile)
            onSuccess(newProfile)
        }
    }

    fun deleteHiddenProfile(profileId: String) {
        viewModelScope.launch {
            if (_activeHiddenProfile.value?.id == profileId) {
                _activeHiddenProfile.value = null
            }
            repository.deleteHiddenProfile(profileId)
        }
    }

    fun createAccountingCompanyGroup(
        name: String,
        companyName: String,
        initialCapital: Double,
        disableMemberDMs: Boolean = true,
        parentGroupId: String? = null,
        profileId: String? = null,
        departments: List<String> = emptyList(),
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val me = currentUser.value ?: repository.currentUser.firstOrNull()
            val ownerId = me?.id ?: "me"
            val compGroupId = java.util.UUID.randomUUID().toString()
            val mainGroup = WorkGroupEntity(
                id = compGroupId,
                name = name,
                description = "مجموعة محاسبية ومالية لإدارة الشركة: $companyName",
                ownerId = ownerId,
                parentGroupId = parentGroupId,
                isAccountingGroup = true,
                isPrivate = true,
                disableMemberDMs = disableMemberDMs,
                companyName = companyName,
                companyCapital = initialCapital,
                profileId = profileId ?: _activeHiddenProfile.value?.id,
                createdAt = System.currentTimeMillis()
            )
            repository.saveWorkGroup(mainGroup)

            // Add creator as primary executive
            val ownerMember = GroupMemberEntity(
                id = java.util.UUID.randomUUID().toString(),
                groupId = compGroupId,
                userId = ownerId,
                username = me?.name ?: "المدير العام",
                phone = me?.phone ?: "",
                roleLabel = "المدير المالي والمسؤول",
                workingHoursStart = "09:00 ص",
                workingHoursEnd = "05:00 م",
                shiftHours = 8.0,
                workingDays = "السبت - الخميس",
                shiftType = "نهارية (صباحية)",
                canPostAccounting = true,
                canExportReports = true,
                canEditRecords = true,
                canMessageDirectly = true,
                isGroupAdmin = true
            )
            repository.saveGroupMember(ownerMember)

            val cleanDepts = departments.map { it.trim() }.filter { it.isNotBlank() }
            val mainChatWelcome = if (cleanDepts.isNotEmpty()) {
                "تم تأسيس مجموعة الشركة ($companyName) وإنشاء ${cleanDepts.size} مجموعات للأقسام بصلاحيات الإدارة الكاملة"
            } else {
                "تم تأسيس مجموعة الشركة ($companyName) بنجاح"
            }
            repository.ensureWorkGroupChat(
                workGroup = mainGroup,
                displayTitle = if (name == companyName) companyName else "$companyName • $name",
                initialSystemMessage = mainChatWelcome
            )

            // Create nested department groups + manager admin membership + department group chat
            cleanDepts.forEach { deptName ->
                val subGroupId = java.util.UUID.randomUUID().toString()
                val subGroup = WorkGroupEntity(
                    id = subGroupId,
                    name = deptName,
                    description = "قسم فرعي تابع لـ $name ($companyName)",
                    ownerId = ownerId,
                    parentGroupId = compGroupId,
                    isAccountingGroup = true,
                    isPrivate = true,
                    disableMemberDMs = disableMemberDMs,
                    companyName = companyName,
                    profileId = profileId ?: _activeHiddenProfile.value?.id,
                    createdAt = System.currentTimeMillis()
                )
                repository.saveWorkGroup(subGroup)
                repository.saveGroupMember(
                    ownerMember.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        groupId = subGroupId,
                        roleLabel = "مدير الشركة / مسؤول القسم",
                        isGroupAdmin = true
                    )
                )
                repository.ensureWorkGroupChat(
                    workGroup = subGroup,
                    displayTitle = "$companyName • $deptName",
                    initialSystemMessage = "تم إنشاء مجموعة قسم ($deptName) التابعة لـ $companyName بصلاحيات الإدارة الكاملة"
                )
            }

            onCreated(compGroupId)
        }
    }

    fun addDailyAccountingRecord(record: DailyAccountingRecordEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addDailyAccountingRecord(record)
            onComplete()
        }
    }

    fun importParsedAccountingRows(
        workGroupId: String,
        rows: List<com.example.util.ParsedAccountingImportRow>,
        onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            var count = 0
            val now = System.currentTimeMillis()
            rows.forEach { r ->
                val entity = DailyAccountingRecordEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    groupId = workGroupId,
                    subGroupId = null,
                    profileId = _activeHiddenProfile.value?.id,
                    entryDate = r.entryDate,
                    staffId = "imp_" + java.util.UUID.randomUUID().toString().take(8),
                    staffName = r.staffName,
                    staffRole = r.staffRole,
                    dailyRevenue = r.dailyRevenue,
                    incomingAmount = r.incomingAmount,
                    outgoingAmount = r.outgoingAmount,
                    deficitAmount = r.deficitAmount,
                    complaintsCount = 0,
                    complaintsNotes = null,
                    dailyCapital = 0.0,
                    weeklyCapital = 0.0,
                    monthlyCapital = 0.0,
                    screenshotUri = null,
                    notes = r.notes,
                    isEncrypted = true,
                    timestamp = now
                )
                repository.addDailyAccountingRecord(entity)
                count++
            }
            onComplete(count)
        }
    }

    fun deleteDailyAccountingRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteDailyAccountingRecord(recordId)
        }
    }

    fun submitRetroactiveComplaint(complaint: FinancialComplaintEntity, onResult: (Double) -> Unit = {}) {
        viewModelScope.launch {
            val deficit = repository.submitRetroactiveComplaintAndApply(complaint)
            onResult(deficit)
        }
    }

    fun deleteFinancialComplaint(complaintId: String) {
        viewModelScope.launch {
            repository.deleteFinancialComplaint(complaintId)
        }
    }

    fun updateComplaintResolution(complaintId: String, status: String, notes: String?) {
        viewModelScope.launch {
            repository.updateComplaintResolution(complaintId, status, notes)
        }
    }

    fun updateEmployeeComplaintResponseWithMedia(
        complaintId: String,
        status: String,
        reason: String?,
        proofUri: String?,
        proofMediaType: String?,
        isDeducted: Boolean
    ) {
        viewModelScope.launch {
            repository.updateComplaintEmployeeResponseWithMedia(
                complaintId,
                status,
                reason,
                proofUri,
                proofMediaType,
                isDeducted
            )
        }
    }

    fun updateStaffPermissionsAndShift(
        memberId: String,
        role: String,
        startHours: String?,
        endHours: String?,
        shiftHours: Double,
        workingDays: String = "السبت - الخميس",
        shiftType: String = "نهارية (صباحية)",
        canPostAccounting: Boolean = true,
        canExportReports: Boolean = false,
        canEditRecords: Boolean = false,
        canMessageDirectly: Boolean = false,
        isGroupAdmin: Boolean = false,
        notes: String? = null,
        location: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        geoEnabled: Boolean = false,
        radius: Int = 200
    ) {
        viewModelScope.launch {
            repository.updateMemberPermissionsAndShift(
                memberId = memberId,
                role = role,
                startHours = startHours,
                endHours = endHours,
                shiftHours = shiftHours,
                workingDays = workingDays,
                shiftType = shiftType,
                canPostAccounting = canPostAccounting,
                canExportReports = canExportReports,
                canEditRecords = canEditRecords,
                canMessageDirectly = canMessageDirectly,
                isGroupAdmin = isGroupAdmin,
                notes = notes,
                location = location,
                lat = lat,
                lng = lng,
                geoEnabled = geoEnabled,
                radius = radius
            )
        }
    }

    fun addStaffMember(
        groupId: String,
        username: String,
        phone: String,
        role: String,
        startHours: String = "09:00 ص",
        endHours: String = "05:00 م",
        shiftHours: Double = 8.0,
        workingDays: String = "السبت - الخميس",
        shiftType: String = "نهارية (صباحية)",
        canPostAccounting: Boolean = true,
        canExportReports: Boolean = false,
        canEditRecords: Boolean = false,
        canMessageDirectly: Boolean = false,
        isGroupAdmin: Boolean = false,
        notes: String? = null,
        location: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        geoEnabled: Boolean = false,
        radius: Int = 200
    ) {
        viewModelScope.launch {
            val newMember = GroupMemberEntity(
                id = java.util.UUID.randomUUID().toString(),
                groupId = groupId,
                userId = "staff_" + System.currentTimeMillis().toString().takeLast(6),
                username = username,
                phone = phone,
                roleLabel = role,
                workingHoursStart = startHours,
                workingHoursEnd = endHours,
                shiftHours = shiftHours,
                workingDays = workingDays,
                shiftType = shiftType,
                canPostAccounting = canPostAccounting,
                canExportReports = canExportReports,
                canEditRecords = canEditRecords,
                canMessageDirectly = canMessageDirectly,
                isGroupAdmin = isGroupAdmin,
                notes = notes,
                workplaceLocation = location,
                workplaceLatitude = lat,
                workplaceLongitude = lng,
                geofencingEnabled = geoEnabled,
                geofencingRadiusMeters = radius
            )
            repository.saveGroupMember(newMember)
            val wg = repository.getWorkGroupByIdSync(groupId)
            if (wg != null) {
                val title = wg.companyName?.takeIf { it.isNotBlank() && it != wg.name }?.let { "$it • ${wg.name}" } ?: wg.name
                val groupChat = repository.ensureWorkGroupChat(wg, displayTitle = title)
                repository.sendMessage(
                    chatId = groupChat.id,
                    senderId = "system",
                    senderName = "النظام",
                    content = "تمت إضافة @$username ($role) إلى المجموعة",
                    type = MessageType.SYSTEM.name
                )
            }
        }
    }

    fun deleteStaffMember(memberId: String) {
        viewModelScope.launch {
            repository.deleteGroupMember(memberId)
        }
    }

    fun clockInEmployee(
        member: GroupMemberEntity,
        note: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        onComplete: (EmployeeAttendanceEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val record = repository.clockInMember(member, note, lat, lng)
            onComplete(record)
        }
    }

    fun clockOutEmployee(attendanceId: String, note: String? = null, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clockOutMember(attendanceId, note)
            onComplete()
        }
    }

    fun saveAttendanceRecord(record: EmployeeAttendanceEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveAttendance(record)
            onComplete()
        }
    }

    fun deleteAttendanceRecord(attendanceId: String) {
        viewModelScope.launch {
            repository.deleteAttendance(attendanceId)
        }
    }

    fun getAttendanceForGroup(groupId: String): Flow<List<EmployeeAttendanceEntity>> = repository.getAttendanceForGroup(groupId)

    fun getAttendanceForMember(memberId: String): Flow<List<EmployeeAttendanceEntity>> = repository.getAttendanceForMember(memberId)

    fun deleteWorkGroup(groupId: String) {
        viewModelScope.launch {
            repository.deleteWorkGroup(groupId)
        }
    }

    fun deleteUsers(userIds: List<String>) {
        viewModelScope.launch {
            repository.deleteUsers(userIds)
        }
    }

    fun createNestedSubGroup(
        parentGroupId: String,
        name: String,
        description: String?,
        disableMemberDMs: Boolean = true,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val me = currentUser.value
            val ownerId = me?.id ?: "me"
            val parent = allWorkGroups.value.firstOrNull { it.id == parentGroupId }
            val subGroupId = java.util.UUID.randomUUID().toString()
            val subGroup = WorkGroupEntity(
                id = subGroupId,
                name = name,
                description = description ?: "مجموعة فرعية / قسم تابع لـ ${parent?.name ?: "الشركة"}",
                ownerId = ownerId,
                parentGroupId = parentGroupId,
                isAccountingGroup = parent?.isAccountingGroup ?: true,
                isPrivate = true,
                disableMemberDMs = disableMemberDMs,
                companyName = parent?.companyName,
                profileId = parent?.profileId ?: _activeHiddenProfile.value?.id,
                createdAt = System.currentTimeMillis()
            )
            repository.saveWorkGroup(subGroup)

            // Automatically add creator / primary manager
            val ownerMember = GroupMemberEntity(
                id = java.util.UUID.randomUUID().toString(),
                groupId = subGroupId,
                userId = ownerId,
                username = me?.name ?: "المدير العام",
                phone = me?.phone ?: "",
                roleLabel = "مسؤول القسم والمجموعة الفرعية",
                workingHoursStart = "09:00 ص",
                workingHoursEnd = "05:00 م",
                shiftHours = 8.0,
                workingDays = "السبت - الخميس",
                shiftType = "نهارية (صباحية)",
                canPostAccounting = true,
                canExportReports = true,
                canEditRecords = true,
                canMessageDirectly = true,
                isGroupAdmin = true
            )
            repository.saveGroupMember(ownerMember)

            // Ensure any parent company admins keep admin permissions in the nested department group
            val parentAdmins = repository.getGroupMembersSync(parentGroupId)
                .filter { it.isGroupAdmin && it.userId != ownerId }
            parentAdmins.forEach { admin ->
                repository.saveGroupMember(
                    admin.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        groupId = subGroupId,
                        roleLabel = "مدير أعلى • ${admin.roleLabel}",
                        isGroupAdmin = true,
                        canPostAccounting = true,
                        canExportReports = true,
                        canEditRecords = true,
                        canMessageDirectly = true
                    )
                )
            }

            val deptDisplay = parent?.companyName?.takeIf { it.isNotBlank() }?.let { "$it • $name" }
                ?: parent?.name?.let { "$it • $name" }
                ?: name
            repository.ensureWorkGroupChat(
                workGroup = subGroup,
                displayTitle = deptDisplay,
                initialSystemMessage = "تم إنشاء مجموعة قسم ($name) بصلاحيات المدير المتكاملة"
            )
            onCreated(subGroupId)
        }
    }

    fun createStandaloneWorkGroup(
        groupName: String,
        description: String? = null,
        selectedContacts: List<ContactEntity> = emptyList(),
        disableMemberDMs: Boolean = false,
        onCreated: (String) -> Unit = {}
    ) {
        val cleanName = groupName.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val me = currentUser.value
            val ownerId = me?.id ?: "me"
            val wg = repository.createWorkGroup(
                name = cleanName,
                description = description?.trim()?.takeIf { it.isNotBlank() } ?: "مجموعة عمل وتنفيذ مهام",
                ownerId = ownerId
            ).copy(disableMemberDMs = disableMemberDMs, isPrivate = false)
            repository.saveWorkGroup(wg)

            val creatorMember = GroupMemberEntity(
                id = java.util.UUID.randomUUID().toString(),
                groupId = wg.id,
                userId = ownerId,
                username = me?.name ?: "المدير",
                phone = me?.phone ?: "",
                roleLabel = "مدير المجموعة",
                canPostAccounting = true,
                canExportReports = true,
                canEditRecords = true,
                canMessageDirectly = true,
                isGroupAdmin = true
            )
            repository.saveGroupMember(creatorMember)

            val addedNames = mutableListOf<String>()
            selectedContacts.distinctBy { it.id }.forEach { contact ->
                val member = GroupMemberEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    groupId = wg.id,
                    userId = contact.id,
                    username = contact.name,
                    phone = contact.phone,
                    roleLabel = "عضو فريق العمل",
                    canPostAccounting = false,
                    canExportReports = false,
                    canEditRecords = false,
                    canMessageDirectly = !disableMemberDMs,
                    isGroupAdmin = false
                )
                repository.saveGroupMember(member)
                addedNames.add("@${contact.name}")
            }

            val welcome = if (addedNames.isNotEmpty()) {
                "تم إنشاء مجموعة العمل ($cleanName) وإضافة: ${addedNames.joinToString("، ")}"
            } else {
                "تم إنشاء مجموعة العمل ($cleanName) بنجاح — يمكنك إسناد المهام والمنشن من زر +"
            }
            val chat = repository.ensureWorkGroupChat(wg, displayTitle = cleanName, initialSystemMessage = welcome)
            com.example.ui.components.ToastNotificationBus.showToast(
                message = "تم إنشاء مجموعة العمل ($cleanName) بنجاح",
                type = com.example.ui.components.AppToastType.SUCCESS
            )
            onCreated(chat.id)
        }
    }

    fun ensureWorkGroupChatAndOpen(groupId: String, onChatReady: (String) -> Unit) {
        viewModelScope.launch {
            val wg = repository.getWorkGroupByIdSync(groupId) ?: return@launch
            val display = wg.companyName?.takeIf { it.isNotBlank() && it != wg.name }?.let { "$it • ${wg.name}" } ?: wg.name
            val chat = repository.ensureWorkGroupChat(wg, displayTitle = display)
            onChatReady(chat.id)
        }
    }

    fun addMembersToGroupChat(
        chatId: String,
        groupId: String?,
        contacts: List<ContactEntity>,
        manualName: String? = null,
        manualPhone: String? = null,
        roleLabel: String = "عضو فريق العمل",
        isGroupAdmin: Boolean = false,
        onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch(safeExceptionHandler) {
            val actor = currentUser.value ?: repository.currentUser.firstOrNull()
            val addedCount = repository.addMembersToWorkGroupChat(
                chatId = chatId,
                existingGroupId = groupId,
                contacts = contacts,
                manualName = manualName,
                manualPhone = manualPhone,
                roleLabel = roleLabel,
                isGroupAdmin = isGroupAdmin,
                actorUser = actor
            )
            if (addedCount > 0) {
                com.example.ui.components.ToastNotificationBus.showToast(
                    message = if (addedCount == 1) "تمت إضافة عضو جديد للمجموعة" else "تمت إضافة $addedCount أعضاء للمجموعة",
                    type = com.example.ui.components.AppToastType.SUCCESS
                )
            }
            onComplete(addedCount)
        }
    }

    fun toggleGroupMemberAdmin(member: GroupMemberEntity) {
        viewModelScope.launch(safeExceptionHandler) {
            val nextAdmin = !member.isGroupAdmin
            repository.updateGroupMemberAdminStatus(member, nextAdmin)
            com.example.ui.components.ToastNotificationBus.showToast(
                message = if (nextAdmin) "تمت ترقية ${member.username} لمدير مجموعة" else "تم تعديل صلاحية ${member.username}",
                type = com.example.ui.components.AppToastType.SUCCESS
            )
        }
    }

    fun removeGroupMemberFromChat(chatId: String, member: GroupMemberEntity) {
        viewModelScope.launch(safeExceptionHandler) {
            repository.removeGroupMemberFromChat(chatId, member)
            com.example.ui.components.ToastNotificationBus.showToast(
                message = "تمت إزالة ${member.username} من المجموعة",
                type = com.example.ui.components.AppToastType.INFO
            )
        }
    }

    fun updateGroupPrivacySettings(groupId: String, disableMemberDMs: Boolean, isPrivate: Boolean) {
        viewModelScope.launch {
            repository.updateGroupPrivacySettings(groupId, disableMemberDMs, isPrivate)
        }
    }

    fun getRecordsForGroup(groupId: String): Flow<List<DailyAccountingRecordEntity>> =
        repository.getAccountingRecordsForGroup(groupId)

    fun getSubGroups(parentGroupId: String): Flow<List<WorkGroupEntity>> =
        repository.getSubGroups(parentGroupId)

    fun getProductsForMerchant(merchantId: String): Flow<List<MerchantProductEntity>> = repository.getProductsByMerchant(merchantId)
    fun getUnclosedSalesForMerchant(merchantId: String): Flow<List<MerchantDailySaleEntity>> = repository.getUnclosedSalesForMerchant(merchantId)
    fun getAllSalesForMerchant(merchantId: String): Flow<List<MerchantDailySaleEntity>> = repository.getAllSalesForMerchant(merchantId)
    fun getClosingReportsForMerchant(merchantId: String): Flow<List<MerchantDailyClosingReportEntity>> = repository.getClosingReportsForMerchant(merchantId)


    private val prefs = application.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)

    private fun loadRepeatUserFromPrefs(): RepeatUserSummary? {
        val name = prefs.getString("repeat_user_name", null) ?: return null
        val phone = prefs.getString("repeat_user_phone", null) ?: return null
        val username = prefs.getString("repeat_user_username", "user") ?: "user"
        val type = prefs.getString("repeat_user_type", UserType.BUSINESSMAN.name) ?: UserType.BUSINESSMAN.name
        val jobTitle = prefs.getString("repeat_user_job_title", null)
        val pin = prefs.getString("repeat_user_pin", null)?.takeIf { it.isNotBlank() }
        val biometricEnabled = prefs.getBoolean("repeat_user_biometric_enabled", true)
        val lastLogin = prefs.getLong("repeat_user_last_login", System.currentTimeMillis())
        return RepeatUserSummary(
            name = name,
            phone = phone,
            username = username,
            type = type,
            jobTitle = jobTitle,
            pin = pin,
            biometricEnabled = biometricEnabled,
            lastLoginTimestamp = lastLogin
        )
    }

    private val _repeatUser = MutableStateFlow<RepeatUserSummary?>(loadRepeatUserFromPrefs())
    val repeatUser: StateFlow<RepeatUserSummary?> = _repeatUser.asStateFlow()

    private val _isOfflineCacheMode = MutableStateFlow(false)
    val isOfflineCacheMode: StateFlow<Boolean> = _isOfflineCacheMode.asStateFlow()

    private val _trustedDevices = MutableStateFlow<List<com.example.util.ApiTrustedDeviceDto>>(emptyList())
    val trustedDevices: StateFlow<List<com.example.util.ApiTrustedDeviceDto>> = _trustedDevices.asStateFlow()

    private val _isLoadingTrustedDevices = MutableStateFlow(false)
    val isLoadingTrustedDevices: StateFlow<Boolean> = _isLoadingTrustedDevices.asStateFlow()

    private fun saveRepeatUser(user: UserEntity) {
        val summary = RepeatUserSummary(
            name = user.name,
            phone = user.phone,
            username = user.username,
            type = user.type,
            jobTitle = user.jobTitle,
            pin = user.appLockPin?.takeIf { it.isNotBlank() },
            biometricEnabled = user.biometricAuthEnabled,
            lastLoginTimestamp = System.currentTimeMillis()
        )
        _repeatUser.value = summary
        prefs.edit()
            .putString("repeat_user_name", summary.name)
            .putString("repeat_user_phone", summary.phone)
            .putString("repeat_user_username", summary.username)
            .putString("repeat_user_type", summary.type)
            .putString("repeat_user_job_title", summary.jobTitle)
            .putString("repeat_user_pin", summary.pin)
            .putBoolean("repeat_user_biometric_enabled", summary.biometricEnabled)
            .putLong("repeat_user_last_login", summary.lastLoginTimestamp)
            .apply()
    }

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _selectedThemePalette = MutableStateFlow(
        com.example.ui.theme.AppThemePalette.fromId(prefs.getString("theme_palette", "OOZOX_INFINITY"))
    )
    val selectedThemePalette: StateFlow<com.example.ui.theme.AppThemePalette> = _selectedThemePalette.asStateFlow()

    fun setThemePalette(palette: com.example.ui.theme.AppThemePalette) {
        _selectedThemePalette.value = palette
        prefs.edit().putString("theme_palette", palette.id).apply()
    }

    fun toggleTheme(isDark: Boolean = !_isDarkTheme.value) {
        _isDarkTheme.value = isDark
        prefs.edit().putBoolean("is_dark_theme", isDark).apply()
    }

    private val _currentLanguage = MutableStateFlow(
        com.example.util.AppLanguage.entries.find { it.code == prefs.getString("app_language", "ar") }
            ?: com.example.util.AppLanguage.ARABIC
    )
    val currentLanguage: StateFlow<com.example.util.AppLanguage> = _currentLanguage.asStateFlow()

    private val _goldPrices = MutableStateFlow(GoldUtils.DEFAULT_GOLD_PRICES)
    val goldPrices: StateFlow<GoldPricesModel> = _goldPrices.asStateFlow()

    private val _isRefreshingChats = MutableStateFlow(false)
    val isRefreshingChats: StateFlow<Boolean> = _isRefreshingChats.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    fun forceSyncChats(onCompleted: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isRefreshingChats.value = true
            try {
                repository.syncPendingOfflineData()
                _lastSyncTimestamp.value = System.currentTimeMillis()
                onCompleted(true)
            } catch (e: Exception) {
                onCompleted(false)
            } finally {
                _isRefreshingChats.value = false
            }
        }
    }

    private val _allNearbyUsers = MutableStateFlow<List<NearbyUserModel>>(emptyList())
    val allNearbyUsers: StateFlow<List<NearbyUserModel>> = _allNearbyUsers.asStateFlow()

    fun setLanguage(language: com.example.util.AppLanguage) {
        _currentLanguage.value = language
        prefs.edit().putString("app_language", language.code).apply()
        val app = getApplication<android.app.Application>()
        com.example.util.Translations.applyLocale(app, language)
        com.example.util.NotificationHelper.createNotificationChannels(app)
        com.example.util.ServerLocalizationSyncManager.syncLanguageAsync(
            context = app,
            language = language,
            pushPreferenceToServer = true
        )
    }

    init {
        val appContext = getApplication<android.app.Application>()
        com.example.util.Translations.init(appContext)
        com.example.data.repository.AuthRepository.restoreSessionOnStartup(appContext)
        com.example.util.RetrofitClient.onUnauthorizedSession = {
            com.example.util.SecureCredentialVault.clearSessionOnly(appContext)
            com.example.util.RetrofitClient.setAuthToken(null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedInitialDataIfEmpty()
            repository.seedSubscriptionPlansIfEmpty()
            isInitialized.value = true

            // Automatically monitor gold and silver price changes against user alerts
            launch(Dispatchers.Default) {
                _goldPrices.collect { prices ->
                    checkAndTriggerPriceAlerts(prices)
                }
            }

            // Continuous background data sync queue worker (reconciles pending items when authenticated)
            launch(Dispatchers.IO) {
                while (true) {
                    kotlinx.coroutines.delay(15000)
                    try {
                        repository.syncPendingOfflineData()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = repository.getMessagesForChat(chatId)
    fun getRecentMessagesForChat(chatId: String, limit: Int = 30): Flow<List<MessageEntity>> =
        repository.getRecentMessagesForChat(chatId, limit)

    fun observePagedChatHistory(chatId: String): Flow<ChatHistoryUiState> {
        return combine(
            repository.getRecentMessagesForChatDesc(chatId, INITIAL_CHAT_PAGE_SIZE),
            repository.getMessageCountForChat(chatId),
            olderMessagesCache,
            loadingOlderChats,
            exhaustedOlderChats
        ) { recentDesc, totalCount, olderMap, loadingSet, exhaustedSet ->
            val olderDesc = olderMap[chatId].orEmpty()
            val mergedDesc = if (olderDesc.isEmpty()) {
                recentDesc
            } else {
                val recentIds = recentDesc.asSequence().map { it.id }.toHashSet()
                recentDesc + olderDesc.filter { it.id !in recentIds }
            }
            val oldestLoaded = mergedDesc.lastOrNull()
            if (oldestLoaded != null) {
                latestLoadedCursorByChat[chatId] = oldestLoaded.timestamp to oldestLoaded.id
            }
            val hasMore = (chatId !in exhaustedSet) && (mergedDesc.size < totalCount)
            ChatHistoryUiState(
                messages = mergedDesc.asReversed(),
                isInitialLoading = false,
                isLoadingOlder = chatId in loadingSet,
                hasMoreOlder = hasMore,
                totalCount = totalCount
            )
        }
    }

    fun loadOlderMessagesForChat(chatId: String) {
        if (chatId.isBlank()) return
        if (chatId in loadingOlderChats.value || chatId in exhaustedOlderChats.value) return
        viewModelScope.launch {
            loadingOlderChats.update { it + chatId }
            try {
                val cursor = latestLoadedCursorByChat[chatId]
                if (cursor != null) {
                    val nextPage = repository.getOlderMessagesPageKeyset(
                        chatId = chatId,
                        beforeTimestamp = cursor.first,
                        beforeId = cursor.second,
                        limit = OLDER_CHAT_PAGE_SIZE
                    )
                    if (nextPage.isEmpty()) {
                        exhaustedOlderChats.update { it + chatId }
                    } else {
                        olderMessagesCache.update { currentMap ->
                            val existing = currentMap[chatId].orEmpty()
                            val existingIds = existing.asSequence().map { it.id }.toHashSet()
                            val appended = existing + nextPage.filter { it.id !in existingIds }
                            currentMap + (chatId to appended)
                        }
                        if (nextPage.size < OLDER_CHAT_PAGE_SIZE) {
                            exhaustedOlderChats.update { it + chatId }
                        }
                    }
                }
            } finally {
                loadingOlderChats.update { it - chatId }
            }
        }
    }

    fun exportFullChatToPdf(context: Context, chat: ChatEntity) {
        viewModelScope.launch {
            val allMessages = repository.getAllMessagesForChatDecrypted(chat.id)
            com.example.util.ReportExporter.exportChatConversationToPdf(context, chat, allMessages)
        }
    }

    fun getAccountsForChat(chatId: String): Flow<List<ChatAccountEntity>> = repository.getAccountsForChat(chatId)
    fun getHashtagsForChat(chatId: String): Flow<List<HashtagAccountEntity>> = repository.getHashtagsForChat(chatId)
    fun getEntriesForHashtag(hashtagId: String): Flow<List<HashtagEntryEntity>> = repository.getEntriesForHashtag(hashtagId)
    fun getGroupMembers(groupId: String): Flow<List<GroupMemberEntity>> = repository.getGroupMembers(groupId)

    fun markChatAsRead(chatId: String) {
        NotificationHelper.clearChatNotification(getApplication(), chatId)
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.markChatAsRead(chatId, user.id)
        }
    }

    fun loginOrRegister(name: String, phone: String, username: String, type: UserType) {
        viewModelScope.launch {
            val existingUser = repository.getUserByPhone(phone)
            val user = if (existingUser != null) {
                existingUser.copy(
                    phone = phone,
                    name = if (name.isNotBlank() && name != "مستخدم جديد" && name != "مستخدم OOZOX") name else existingUser.name,
                    username = if (username.isNotBlank() && !username.startsWith("user_")) username else existingUser.username
                )
            } else {
                val displayName = if (name.isNotBlank() && name != "مستخدم جديد") name else "مستخدم OOZOX"
                val cleanDigits = phone.filter { it.isDigit() }.takeLast(4)
                val displayUsername = if (username.isNotBlank()) username else "user_${if (cleanDigits.isNotBlank()) cleanDigits else System.currentTimeMillis().toString().takeLast(4)}"
                UserEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    name = displayName,
                    phone = phone,
                    username = displayUsername,
                    type = type.name,
                    isVerified = false
                )
            }
            repository.saveUser(user)
            saveRepeatUser(user)
        }
    }

    fun updateUserProfileInfo(
        name: String,
        username: String,
        type: UserType,
        jobTitle: String? = null,
        bio: String? = null,
        education: String? = null,
        cvSectionOrder: String? = null,
        storeName: String? = null,
        skills: String? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val current = currentUser.value ?: return@launch
            val updated = current.copy(
                name = name.trim().ifBlank { current.name },
                username = username.trim().removePrefix("@").ifBlank { current.username },
                type = type.name,
                jobTitle = jobTitle ?: current.jobTitle,
                merchantBio = bio ?: current.merchantBio,
                education = education ?: current.education,
                cvSectionOrder = cvSectionOrder ?: current.cvSectionOrder,
                traderShopName = storeName ?: current.traderShopName,
                skills = skills ?: current.skills
            )
            repository.saveUser(updated)
            saveRepeatUser(updated)
            viewModelScope.launch(Dispatchers.IO) {
                com.example.data.repository.AuthRepository.updateMyProfile(
                    name = updated.name,
                    preferredLanguage = _currentLanguage.value.code
                )
            }
            onComplete()
        }
    }

    fun sendBroadcastMessage(
        targetCategory: BroadcastTargetCategory,
        title: String,
        content: String,
        isUrgent: Boolean = false,
        postAsSystemAnnouncement: Boolean = true,
        onResult: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val count = repository.sendBroadcastMessage(
                targetCategory = targetCategory,
                title = title,
                content = content,
                isUrgent = isUrgent,
                postAsSystemAnnouncement = postAsSystemAnnouncement
            )
            onResult(count)
        }
    }

    suspend fun completeServerAuthenticatedUser(
        apiUser: com.example.util.ApiUserDto,
        fallbackPhone: String = "",
        fallbackType: UserType? = null
    ) {
        val resolvedPhone = apiUser.phoneE164.ifBlank { apiUser.phone }.ifBlank { fallbackPhone }
        val existingUser = repository.getUserByPhone(resolvedPhone)
        val resolvedRole = when (apiUser.role.uppercase()) {
            "ADMIN" -> UserType.ADMIN
            "MERCHANT", "GOLD_TRADER" -> UserType.GOLD_TRADER
            else -> fallbackType ?: existingUser?.let {
                try { UserType.valueOf(it.type) } catch (_: Exception) { UserType.BUSINESSMAN }
            } ?: UserType.BUSINESSMAN
        }
        val serverId = apiUser.id.ifBlank { apiUser.mongoId }.ifBlank { existingUser?.id ?: java.util.UUID.randomUUID().toString() }
        val cleanDigits = resolvedPhone.filter { it.isDigit() }.takeLast(4)
        val updatedUser = (existingUser ?: UserEntity(
            id = serverId,
            name = apiUser.name.ifBlank { "مستخدم OOZOX" },
            phone = resolvedPhone,
            username = "user_${if (cleanDigits.isNotBlank()) cleanDigits else serverId.takeLast(4)}",
            type = resolvedRole.name,
            isVerified = apiUser.isVerified
        )).copy(
            id = serverId,
            name = apiUser.name.ifBlank { existingUser?.name ?: "مستخدم OOZOX" },
            phone = resolvedPhone,
            type = resolvedRole.name,
            isVerified = apiUser.isVerified
        )
        repository.saveUser(updatedUser)
        saveRepeatUser(updatedUser)
        _isOfflineCacheMode.value = false
    }

    suspend fun requestServerOtp(
        purpose: String,
        channel: String = "AUTO",
        phone: String? = null,
        newPhone: String? = null,
        preAuthToken: String? = null
    ): com.example.data.repository.OtpRequestOutcome {
        return com.example.data.repository.AuthRepository.requestOtp(
            context = getApplication(),
            purpose = purpose,
            channel = channel,
            phone = phone,
            newPhone = newPhone,
            preAuthToken = preAuthToken,
            allowFallback = true
        )
    }

    suspend fun verifyServerOtp(
        challengeId: String,
        code: String,
        phone: String? = null,
        preAuthToken: String? = null,
        trustDevice: Boolean = true
    ): com.example.data.repository.OtpVerifyOutcome {
        val outcome = com.example.data.repository.AuthRepository.verifyOtp(
            context = getApplication(),
            challengeId = challengeId,
            code = code,
            phone = phone,
            preAuthToken = preAuthToken,
            trustDevice = trustDevice
        )
        if (outcome is com.example.data.repository.OtpVerifyOutcome.Verified) {
            outcome.data.user?.let { userDto ->
                completeServerAuthenticatedUser(userDto, fallbackPhone = phone.orEmpty())
            }
        }
        return outcome
    }

    suspend fun trustedBiometricOnlineLogin(phone: String): com.example.data.repository.AuthOutcome {
        val outcome = com.example.data.repository.AuthRepository.trustedDeviceLogin(
            context = getApplication(),
            phoneOrIdentifier = phone
        )
        if (outcome is com.example.data.repository.AuthOutcome.Authenticated) {
            completeServerAuthenticatedUser(outcome.user, fallbackPhone = phone)
        }
        return outcome
    }

    suspend fun unlockOfflineCacheOnly(phone: String): Boolean {
        val app = getApplication<android.app.Application>()
        if (!com.example.util.DeviceTrustManager.isOfflineCacheAccessAllowed(app, phone)) {
            return false
        }
        val cachedUser = repository.getUserByPhone(phone)
        val summary = _repeatUser.value
        val restored = cachedUser ?: if (summary != null && summary.phone == phone) {
            UserEntity(
                id = com.example.util.SecureCredentialVault.getTrustedUserId(app) ?: "offline_${phone.takeLast(6)}",
                name = summary.name,
                phone = summary.phone,
                username = summary.username,
                type = summary.type,
                jobTitle = summary.jobTitle,
                appLockPin = summary.pin,
                biometricAuthEnabled = summary.biometricEnabled,
                isVerified = true
            )
        } else {
            null
        }
        if (restored != null) {
            repository.saveUser(restored)
            _isOfflineCacheMode.value = true
            return true
        }
        return false
    }

    fun loadTrustedDevices() {
        viewModelScope.launch {
            _isLoadingTrustedDevices.value = true
            val result = com.example.data.repository.AuthRepository.listTrustedDevices(getApplication())
            result.onSuccess { list ->
                _trustedDevices.value = list
            }.onFailure { err ->
                com.example.ui.components.ToastNotificationBus.showToast(
                    message = err.message ?: "تعذر تحميل قائمة الأجهزة الموثوقة",
                    type = com.example.ui.components.AppToastType.ERROR
                )
            }
            _isLoadingTrustedDevices.value = false
        }
    }

    fun revokeTrustedDevice(deviceId: String, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val app = getApplication<android.app.Application>()
            val currentDeviceId = com.example.util.SecureCredentialVault.getOrCreateDeviceId(app)
            val result = com.example.data.repository.AuthRepository.revokeTrustedDevice(app, deviceId)
            result.onSuccess { msg ->
                _trustedDevices.update { it.filterNot { dev -> dev.deviceId == deviceId } }
                if (deviceId == currentDeviceId) {
                    repository.clearUserSession()
                    _isOfflineCacheMode.value = false
                }
                onDone(true, msg)
            }.onFailure { err ->
                onDone(false, err.message ?: "تعذر إلغاء وثوق الجهاز")
            }
        }
    }

    fun syncRealtimeSignalingState() {
        val user = currentUser.value
        val hasSession = com.example.util.RetrofitClient.hasActiveSession()
        if (user == null || _isOfflineCacheMode.value || !hasSession) {
            signalingCollectorJob?.cancel()
            signalingCollectorJob = null
            _signalingManager.value?.disconnect()
            _signalingManager.value = null
            return
        }
        val existing = _signalingManager.value
        if (existing != null && existing.isConnected.value) return

        existing?.disconnect()
        val devId = com.example.util.SecureCredentialVault.getOrCreateDeviceId(getApplication())
        val mgr = com.example.util.WebRtcSignalingManager(
            currentUserId = user.id,
            deviceId = devId,
            coroutineScope = viewModelScope
        )
        _signalingManager.value = mgr
        signalingCollectorJob?.cancel()
        signalingCollectorJob = viewModelScope.launch {
            launch {
                mgr.incomingCalls.collect { payload ->
                    _incomingCallPayload.value = payload
                }
            }
            launch {
                mgr.transferEvents.collect {
                    _incomingTransferSignal.value = System.currentTimeMillis()
                }
            }
            launch {
                mgr.forceLogoutEvents.collect { reason ->
                    com.example.ui.components.ToastNotificationBus.showToast(
                        message = reason,
                        type = com.example.ui.components.AppToastType.ERROR
                    )
                    logout()
                }
            }
            launch {
                val fcmToken = com.example.util.FcmNotificationManager.fcmToken.value
                    ?: com.example.util.OozoxFirebaseMessagingService.getSavedToken(getApplication())
                if (!fcmToken.isNullOrBlank()) {
                    com.example.data.repository.AuthRepository.registerFcmToken(getApplication(), fcmToken)
                }
            }
        }
        mgr.connect()
    }

    fun dismissIncomingCall() {
        _incomingCallPayload.value = null
    }

    fun logout() {
        viewModelScope.launch {
            signalingCollectorJob?.cancel()
            signalingCollectorJob = null
            _signalingManager.value?.disconnect()
            _signalingManager.value = null
            _incomingCallPayload.value = null
            com.example.data.repository.AuthRepository.logout(getApplication(), clearDeviceTrust = false)
            _isOfflineCacheMode.value = false
            _trustedDevices.value = emptyList()
            repository.clearUserSession()
        }
    }

    fun sendMessage(chatId: String, content: String, type: String = MessageType.TEXT.name) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.sendMessage(chatId, user.id, user.name, content, type)

            // Real-time automatic hashtag, ledger, and accounting processing (only when explicit # exists)
            if (content.contains("#")) {
                repository.autoProcessHashtagFromMessage(chatId, content, user.id, user.name)
            }

            val isOnline = com.example.util.OfflineSyncManager.isNetworkOnline.value
            if (!isOnline) {
                com.example.ui.components.ToastNotificationBus.showToast(
                    message = "تم حفظ الرسالة محلياً في Room - ستُرسل فور عودة الاتصال ⏱️",
                    type = com.example.ui.components.AppToastType.INFO
                )
            }

            val currentSub = _activeHiddenProfile.value
            val isSubAcc = (currentSub != null)
            val isSubEnabled = (currentSub?.silentNotifications == false)

            // Gemini AI Assistant reply for AI chat / @ai mentions only
            if (chatId == "ai_accountant" || chatId.contains("ai") || chatId.contains("bot") || content.startsWith("@ai")) {
                val currentPrices = _goldPrices.value
                val contextSummary = "سعر جرام الذهب عيار 21 الآن: ${currentPrices.karat21} ج.م. عيار 24: ${currentPrices.karat24} ج.م. الجنيه الذهب: ${currentPrices.goldPound} ج.م."
                val aiReply = GeminiAiService.queryGeminiAi(
                    userQuery = content,
                    contextInfo = contextSummary,
                    adminConfig = adminConfig.value
                )
                repository.sendMessage(
                    chatId = chatId,
                    senderId = "gemini_ai_bot",
                    senderName = "المحاسب المالي",
                    content = aiReply,
                    type = MessageType.TEXT.name
                )
                NotificationHelper.showChatMessageNotification(
                    context = getApplication(),
                    chatId = chatId,
                    senderName = "المحاسب المالي",
                    messageText = aiReply,
                    isSubAccount = isSubAcc,
                    isSubAccountNotificationEnabled = isSubEnabled
                )
                if (!isSubAcc) {
                    MessengerBubbleManager.showBubble(
                        chatId = chatId,
                        contactName = "المحاسب المالي",
                        contactAvatarUri = null,
                        contactRole = "المساعد المالي والمحاسبي",
                        messageText = aiReply
                    )
                }
            }
        }
    }

    fun toggleMessageReaction(messageId: String, currentReaction: String?, emoji: String) {
        viewModelScope.launch {
            val newReaction = if (currentReaction == emoji) null else emoji
            repository.updateMessageReaction(messageId, newReaction)
            olderMessagesCache.update { currentMap ->
                var changed = false
                val updatedMap = currentMap.mapValues { (_, list) ->
                    if (list.any { it.id == messageId }) {
                        changed = true
                        list.map { if (it.id == messageId) it.copy(reaction = newReaction) else it }
                    } else list
                }
                if (changed) updatedMap else currentMap
            }
        }
    }

    fun startChatWithContact(contact: ContactEntity, onChatCreated: (String) -> Unit) {
        viewModelScope.launch {
            val chat = repository.createChat(contact)
            onChatCreated(chat.id)
        }
    }

    fun createGroupChatFolder(groupName: String, selectedChatIds: List<String>) {
        viewModelScope.launch {
            repository.createGroupChatFolder(groupName, selectedChatIds)
        }
    }

    fun archiveChat(chatId: String, isArchived: Boolean) {
        viewModelScope.launch {
            repository.setChatArchived(chatId, isArchived)
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            olderMessagesCache.update { it - chatId }
            exhaustedOlderChats.update { it - chatId }
            latestLoadedCursorByChat.remove(chatId)
        }
    }

    fun removeChatGroupFolder(groupName: String) {
        viewModelScope.launch {
            val chats = repository.allChats.first()
            chats.filter { it.groupName == groupName }.forEach { chat ->
                repository.updateChatGroup(chat.id, null)
            }
        }
    }

    fun renameChatGroupFolder(
        oldName: String,
        newName: String,
        selectedChatIds: List<String>? = null
    ) {
        val cleanNew = newName.trim()
        if (cleanNew.isEmpty()) return
        viewModelScope.launch {
            val chats = repository.allChats.first()
            if (selectedChatIds != null) {
                val targetIds = selectedChatIds.toSet()
                chats.forEach { chat ->
                    when {
                        chat.id in targetIds -> repository.updateChatGroup(chat.id, cleanNew)
                        chat.groupName == oldName -> repository.updateChatGroup(chat.id, null)
                    }
                }
            } else {
                chats.filter { it.groupName == oldName }.forEach { chat ->
                    repository.updateChatGroup(chat.id, cleanNew)
                }
            }
        }
    }

    fun createAccount(chatId: String, name: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.createChatAccount(chatId, name, user.id)
        }
    }

    fun createHashtagAccount(chatId: String, accountId: String, tag: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.createHashtagAccount(chatId, accountId, tag, user.id)
        }
    }

    fun addHashtagEntry(
        hashtagId: String,
        accountId: String,
        type: HashtagEntryType,
        description: String,
        amount: Double,
        direction: HashtagEntryDirection
    ) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.addHashtagEntry(
                hashtagId = hashtagId,
                accountId = accountId,
                type = type.name,
                description = description,
                amount = amount,
                direction = direction.name,
                createdBy = user.name
            )
        }
    }

    fun recordChatHashtagOperation(
        chatId: String,
        tag: String,
        entryType: HashtagEntryType,
        description: String,
        amount: Double,
        direction: HashtagEntryDirection,
        projectId: String? = null,
        projectName: String? = null,
        paymentMethod: String = "كاش",
        isExternalBusiness: Boolean = false,
        photoUri: String? = null,
        linkedInvoiceId: String? = null,
        postChatMessage: Boolean = true,
        onSuccess: (GoldChatRepository.ChatHashtagSyncResult) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch(safeExceptionHandler) {
            try {
                val user = currentUser.value
                val actorId = user?.id ?: "current_user"
                val actorName = user?.name ?: "أنت"
                val res = repository.recordChatHashtagOperation(
                    chatId = chatId,
                    tag = tag,
                    entryType = entryType,
                    description = description,
                    amount = amount,
                    direction = direction,
                    actorId = actorId,
                    actorName = actorName,
                    projectId = projectId,
                    projectName = projectName,
                    paymentMethod = paymentMethod,
                    isExternalBusiness = isExternalBusiness,
                    photoUri = photoUri,
                    linkedInvoiceId = linkedInvoiceId,
                    postChatMessage = postChatMessage
                )
                val statusMsg = if (res.isSettled) {
                    "تم تسجيل العملية ومطابقة #${res.hashtagAccount.tag} (مسدد بالكامل)"
                } else {
                    "تم تسجيل #${res.hashtagAccount.tag} ومزامنة الفاتورة والدفتر"
                }
                com.example.ui.components.ToastNotificationBus.showToast(
                    message = statusMsg,
                    type = com.example.ui.components.AppToastType.SUCCESS
                )
                onSuccess(res)
            } catch (e: Exception) {
                val err = e.message ?: "تعذر تسجيل العملية المالية للهاشتاج"
                com.example.ui.components.ToastNotificationBus.showToast(
                    message = err,
                    type = com.example.ui.components.AppToastType.ERROR
                )
                onError(err)
            }
        }
    }

    fun payInvoiceWithSync(
        invoiceId: String,
        paymentAmount: Double,
        paymentMethod: String = "كاش",
        notes: String? = null,
        postChatNotice: Boolean = true,
        onComplete: (InvoiceEntity?) -> Unit = {}
    ) {
        viewModelScope.launch(safeExceptionHandler) {
            try {
                val user = currentUser.value
                val updated = repository.payInvoiceWithSync(
                    invoiceId = invoiceId,
                    paymentAmount = paymentAmount,
                    paymentMethod = paymentMethod,
                    actorId = user?.id ?: "current_user",
                    actorName = user?.name ?: "أنت",
                    notes = notes,
                    postChatNotice = postChatNotice
                )
                val label = if (updated.status == InvoiceStatus.PAID.name) {
                    "تم سداد الفاتورة ${updated.invoiceNumber} بالكامل وتصفير الرصيد"
                } else {
                    "تم تسجيل دفعة من الفاتورة ${updated.invoiceNumber} وتحديث المتبقي"
                }
                com.example.ui.components.ToastNotificationBus.showToast(
                    message = label,
                    type = com.example.ui.components.AppToastType.SUCCESS
                )
                onComplete(updated)
            } catch (e: Exception) {
                com.example.ui.components.ToastNotificationBus.showToast(
                    message = e.message ?: "تعذر تسجيل سداد الفاتورة",
                    type = com.example.ui.components.AppToastType.ERROR
                )
                onComplete(null)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    val globalAccountingSnapshot: StateFlow<SmartAccountantEngine.GlobalAccountingSnapshot> = combine(
        combine(
            allInvoices,
            allProjects,
            allHashtagAccounts,
            allHashtagEntries,
            allChatAccounts
        ) { invoices, projects, hashtags, entries, chatAccounts ->
            arrayOf(invoices, projects, hashtags, entries, chatAccounts)
        },
        combine(
            allDailyAccountingRecords,
            allMerchantProducts,
            allShiftHandovers,
            allGoldPurchases
        ) { dailyRecords, products, shiftHandovers, goldPurchases ->
            arrayOf(dailyRecords, products, shiftHandovers, goldPurchases)
        }
    ) { firstGroup, secondGroup ->
        SmartAccountantEngine.GlobalAccountingSnapshot(
            invoices = firstGroup[0] as List<InvoiceEntity>,
            projects = firstGroup[1] as List<ProjectEntity>,
            hashtagAccounts = firstGroup[2] as List<HashtagAccountEntity>,
            hashtagEntries = firstGroup[3] as List<HashtagEntryEntity>,
            chatAccounts = firstGroup[4] as List<ChatAccountEntity>,
            dailyRecords = secondGroup[0] as List<DailyAccountingRecordEntity>,
            products = secondGroup[1] as List<MerchantProductEntity>,
            shiftHandovers = secondGroup[2] as List<ShiftHandoverEntity>,
            goldPurchases = secondGroup[3] as List<GoldPurchaseEntity>
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SmartAccountantEngine.GlobalAccountingSnapshot()
        )

    fun buildGlobalAccountingSnapshot(): SmartAccountantEngine.GlobalAccountingSnapshot {
        return SmartAccountantEngine.GlobalAccountingSnapshot(
            invoices = allInvoices.value,
            projects = allProjects.value,
            hashtagAccounts = allHashtagAccounts.value,
            hashtagEntries = allHashtagEntries.value,
            chatAccounts = allChatAccounts.value,
            dailyRecords = allDailyAccountingRecords.value,
            products = allMerchantProducts.value,
            shiftHandovers = allShiftHandovers.value,
            goldPurchases = allGoldPurchases.value
        )
    }

    suspend fun loadProjectExcelSheet(
        projectId: String,
        projectType: String?
    ): Pair<SmartSpreadsheetTemplate, MutableList<SmartSpreadsheetRow>> {
        return repository.loadSmartExcelSheet(projectId, projectType)
    }

    fun saveProjectExcelSheet(
        projectId: String,
        template: SmartSpreadsheetTemplate,
        rows: List<SmartSpreadsheetRow>
    ) {
        viewModelScope.launch(safeExceptionHandler) {
            repository.saveSmartExcelSheet(projectId, template, rows)
        }
    }

    fun createWorkGroupAndProject(
        groupName: String,
        groupDesc: String?,
        projectName: String,
        capital: Double,
        projectType: ProjectType,
        location: String = "القاهرة، مصر",
        coManagerPhone: String? = null,
        coManagerName: String? = null,
        membersInfo: String = "",
        customProjectTypeName: String? = null,
        memberInputs: List<ProjectMemberInput> = emptyList()
    ) {
        viewModelScope.launch {
            val user = currentUser.value
                ?: repository.currentUser.firstOrNull()
                ?: UserEntity(
                    id = "me",
                    name = "المدير العام",
                    phone = "",
                    username = "admin"
                )
            val cleanProjectName = projectName.trim()
            val resolvedGroupName = groupName.trim().ifBlank { cleanProjectName }
            val resolvedMembersInfo = membersInfo.ifBlank {
                memberInputs.joinToString(" | ") { mem ->
                    "@${mem.name.trim()} (${mem.phone.trim()}) - ${mem.roleTask.trim().ifBlank { mem.role.label }}"
                }
            }

            val wg = repository.createWorkGroup(resolvedGroupName, groupDesc, user.id)
            val resolvedType = customProjectTypeName?.takeIf { it.isNotBlank() } ?: projectType.name
            val proj = repository.createProject(
                name = cleanProjectName,
                workGroupId = wg.id,
                capital = capital,
                type = resolvedType,
                location = location,
                coManagerPhone = coManagerPhone,
                coManagerName = coManagerName,
                membersInfo = resolvedMembersInfo
            )

            // Creator / primary manager
            repository.saveGroupMember(
                GroupMemberEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    groupId = wg.id,
                    userId = user.id,
                    username = user.name,
                    phone = user.phone,
                    roleLabel = "مالك ومدير المشروع",
                    canPostAccounting = true,
                    canExportReports = true,
                    canEditRecords = true,
                    canMessageDirectly = true,
                    isGroupAdmin = true
                )
            )
            repository.addProjectMember(
                ProjectMemberEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    projectId = proj.id,
                    userId = user.id,
                    userName = user.name,
                    userPhone = user.phone,
                    role = ProjectMemberRole.OWNER.name,
                    assignedTask = "إدارة المشروع"
                )
            )

            val addedLabels = mutableListOf<String>()
            // Co-manager if assigned
            if (!coManagerName.isNullOrBlank()) {
                val coPhone = coManagerPhone?.trim().orEmpty()
                val matchedCo = allContacts.value.firstOrNull {
                    it.name == coManagerName.trim() || (coPhone.isNotBlank() && it.phone == coPhone)
                }
                val coUserId = matchedCo?.id ?: "comgr_${java.util.UUID.randomUUID().toString().take(8)}"
                repository.saveGroupMember(
                    GroupMemberEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        groupId = wg.id,
                        userId = coUserId,
                        username = coManagerName.trim(),
                        phone = coPhone,
                        roleLabel = "المدير التنفيذي المشارك",
                        canPostAccounting = true,
                        canExportReports = true,
                        canEditRecords = true,
                        canMessageDirectly = true,
                        isGroupAdmin = true
                    )
                )
                repository.addProjectMember(
                    ProjectMemberEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        projectId = proj.id,
                        userId = coUserId,
                        userName = coManagerName.trim(),
                        userPhone = coPhone,
                        role = ProjectMemberRole.EXECUTIVE_MANAGER.name,
                        assignedTask = "إدارة تنفيذية"
                    )
                )
                addedLabels.add("@${coManagerName.trim()} (مدير تنفيذي)")
            }

            if (memberInputs.isNotEmpty()) {
                memberInputs.forEach { input ->
                    val memberName = input.name.trim().removePrefix("@").trim()
                    if (memberName.isNotBlank()) {
                        val matchedContact = allContacts.value.firstOrNull {
                            it.name.equals(memberName, ignoreCase = true) ||
                                (input.phone.isNotBlank() && it.phone == input.phone.trim())
                        }
                        val matchedEmployee = allSubAccounts.value.firstOrNull {
                            it.id == input.userId ||
                                it.displayName.equals(memberName, ignoreCase = true) ||
                                (!it.phone.isNullOrBlank() && it.phone == input.phone.trim())
                        }
                        val memberUserId = input.userId?.takeIf { it.isNotBlank() }
                            ?: matchedEmployee?.id
                            ?: matchedContact?.id
                            ?: "pm_${java.util.UUID.randomUUID().toString().take(8)}"
                        val memberPhone = input.phone.trim()
                            .ifBlank { matchedEmployee?.phone.orEmpty() }
                            .ifBlank { matchedContact?.phone.orEmpty() }
                        val resolvedJobTitle = input.roleTask.trim().ifBlank { input.role.label }

                        repository.saveGroupMember(
                            GroupMemberEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                groupId = wg.id,
                                userId = memberUserId,
                                username = memberName,
                                phone = memberPhone,
                                roleLabel = resolvedJobTitle,
                                canPostAccounting = input.canPostAccounting || input.isGroupAdmin,
                                canExportReports = input.canExportReports || input.isGroupAdmin,
                                canEditRecords = input.canEditRecords || input.isGroupAdmin,
                                canMessageDirectly = input.canMessageDirectly || input.isGroupAdmin,
                                isGroupAdmin = input.isGroupAdmin
                            )
                        )
                        repository.addProjectMember(
                            ProjectMemberEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                projectId = proj.id,
                                userId = memberUserId,
                                userName = memberName,
                                userPhone = memberPhone,
                                role = input.role.name,
                                assignedTask = resolvedJobTitle
                            )
                        )
                        addedLabels.add("@$memberName ($resolvedJobTitle)")
                    }
                }
            } else if (membersInfo.isNotBlank()) {
                // Backward-compatible fallback for callers passing formatted membersInfo only
                membersInfo.split("|").map { it.trim() }.filter { it.isNotBlank() }.forEach { entry ->
                    val beforeDash = entry.substringBefore(" - ").trim()
                    val roleText = entry.substringAfter(" - ", "عضو مشروع").trim()
                    val phoneInParens = Regex("\\(([^)]*)\\)").find(beforeDash)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                    val memberName = beforeDash.replace(Regex("\\([^)]*\\)"), "").trim().removePrefix("@").trim()
                    if (memberName.isNotBlank()) {
                        val matchedContact = allContacts.value.firstOrNull {
                            it.name.equals(memberName, ignoreCase = true) ||
                                (phoneInParens.isNotBlank() && it.phone == phoneInParens)
                        }
                        val memberUserId = matchedContact?.id ?: "pm_${java.util.UUID.randomUUID().toString().take(8)}"
                        val memberPhone = phoneInParens.ifBlank { matchedContact?.phone ?: "" }
                        val mappedRole = ProjectMemberRole.entries.firstOrNull { it.label == roleText }
                            ?: ProjectMemberRole.EXECUTIVE_MANAGER
                        repository.saveGroupMember(
                            GroupMemberEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                groupId = wg.id,
                                userId = memberUserId,
                                username = memberName,
                                phone = memberPhone,
                                roleLabel = roleText.ifBlank { mappedRole.label },
                                canPostAccounting = mappedRole.defaultCanPostAccounting(),
                                canExportReports = mappedRole.defaultCanExportReports(),
                                canEditRecords = mappedRole.defaultCanEditRecords(),
                                canMessageDirectly = true,
                                isGroupAdmin = mappedRole.defaultIsAdmin()
                            )
                        )
                        repository.addProjectMember(
                            ProjectMemberEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                projectId = proj.id,
                                userId = memberUserId,
                                userName = memberName,
                                userPhone = memberPhone,
                                role = mappedRole.name,
                                assignedTask = roleText
                            )
                        )
                        addedLabels.add("@$memberName ($roleText)")
                    }
                }
            }

            repository.addProjectAuditLog(
                ProjectAuditLogEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    projectId = proj.id,
                    action = "تأسيس المشروع ومجموعة الدردشة",
                    actorName = user.name,
                    actorPhone = user.phone,
                    details = "تم إنشاء المشروع ($cleanProjectName) وربطه بمجموعة الدردشة ($resolvedGroupName) مع ${addedLabels.size + 1} عضو"
                )
            )

            val membersSummary = if (addedLabels.isNotEmpty()) {
                " • الموظفون المضافون: ${addedLabels.joinToString("، ")}"
            } else ""
            repository.ensureWorkGroupChat(
                workGroup = wg,
                displayTitle = resolvedGroupName,
                initialSystemMessage = "تم إنشاء مجموعة دردشة المشروع ($cleanProjectName)$membersSummary — البيانات والحسابات والمهام مشتركة ومزامنة مباشرة مع قسم المشاريع"
            )

            NotificationHelper.showProjectUpdateNotification(
                context = getApplication(),
                projectId = wg.id,
                projectName = cleanProjectName,
                title = "تم إنشاء مشروع ومجموعة عمل جديدة",
                updateMessage = "تم تأسيس '$cleanProjectName' وإنشاء مجموعة الدردشة '$resolvedGroupName' برأس مال ${GoldUtils.formatCurrency(capital)}"
            )
        }
    }

    fun addGoldTransaction(
        chatId: String,
        contactId: String,
        contactName: String,
        karat: GoldKarat,
        weight: Double,
        pricePerGram: Double,
        mfgCostPerGram: Double,
        notes: String?,
        metalLossRatePercent: Double = 0.5,
        metalLossGrams: Double = 0.0
    ) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val (totalGold, totalMfg, totalPrice) = GoldUtils.calculateGoldPrice(weight, pricePerGram, mfgCostPerGram)

            val tx = TransactionEntity(
                id = java.util.UUID.randomUUID().toString(),
                chatId = chatId,
                currentUserId = user.id,
                currentUserName = user.name,
                currentUserType = user.type,
                contactId = contactId,
                contactName = contactName,
                goldKarat = karat.name,
                weight = weight,
                pricePerGram = pricePerGram,
                manufacturingCostPerGram = mfgCostPerGram,
                totalGoldPrice = totalGold,
                totalManufacturing = totalMfg,
                totalPrice = totalPrice,
                notes = notes,
                status = TransactionStatus.PURCHASED.name,
                metalLossRatePercent = metalLossRatePercent,
                metalLossGrams = metalLossGrams
            )
            repository.addGoldTransaction(tx)

            // Also send a transaction message in chat
            repository.sendMessage(
                chatId = chatId,
                senderId = user.id,
                senderName = user.name,
                content = "صفقة ذهب جديدة: ${karat.displayLabel()} - ${GoldUtils.formatWeight(weight)} (تآكل: ${GoldUtils.formatDecimal(metalLossRatePercent)}%) بسعر ${GoldUtils.formatCurrency(totalPrice)}",
                type = MessageType.GOLD_PURCHASE.name
            )

            NotificationHelper.showTransactionNotification(
                context = getApplication(),
                title = "صفقة ذهب جديدة مع $contactName",
                body = "تم تسجيل صفقة ${karat.displayLabel()} بحجم ${GoldUtils.formatWeight(weight)} بقيمة ${GoldUtils.formatCurrency(totalPrice)}"
            )
        }
    }

    fun addGoldPurchaseToPortfolio(
        weight: Double,
        karat: GoldKarat,
        pricePerGram: Double,
        manufacturer: String,
        manufacturingCost: Double,
        stampDuty: Double,
        traderHashtag: String
    ) {
        addMetalPurchaseToPortfolio(
            metalType = MetalType.GOLD,
            weight = weight,
            karat = karat.name,
            pricePerGram = pricePerGram,
            manufacturer = manufacturer,
            manufacturingCost = manufacturingCost,
            mfgType = "PER_GRAM",
            stampDuty = stampDuty,
            cashbackRefund = 0.0,
            traderHashtag = traderHashtag,
            sellerShopName = traderHashtag,
            sellerLocation = "داخل التطبيق",
            scalePhotoUri = null,
            metalLossRatePercent = 0.5,
            metalLossGrams = weight * 0.005
        )
    }

    fun addMetalPurchaseToPortfolio(
        metalType: MetalType,
        weight: Double,
        karat: String,
        pricePerGram: Double,
        manufacturer: String,
        manufacturingCost: Double,
        mfgType: String,
        stampDuty: Double,
        cashbackRefund: Double,
        traderHashtag: String,
        sellerShopName: String,
        sellerLocation: String,
        scalePhotoUri: String?,
        metalLossRatePercent: Double = 0.5,
        metalLossGrams: Double = 0.0
    ) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val currentPrice = pricePerGram
            val totalMfg = if (mfgType == "PER_GRAM") weight * manufacturingCost else manufacturingCost
            val totalStamp = weight * stampDuty
            val totalCost = (weight * pricePerGram) + totalMfg + totalStamp

            val cleanTag = traderHashtag.removePrefix("@").removePrefix("#")
            val isVerifiedTrader = allContacts.value.any { contact ->
                (contact.name.equals(sellerShopName, ignoreCase = true) ||
                 contact.name.equals(cleanTag, ignoreCase = true) ||
                 contact.username.equals(cleanTag, ignoreCase = true)) &&
                 (contact.isVerified || contact.type == UserType.GOLD_TRADER.name)
            }

            val purchase = GoldPurchaseEntity(
                id = java.util.UUID.randomUUID().toString(),
                buyerId = user.id,
                traderHashtag = traderHashtag,
                traderConfirmed = true,
                extractedWeight = weight,
                karat = karat,
                pricePerGram = pricePerGram,
                currentPricePerGram = currentPrice,
                manufacturer = manufacturer,
                manufacturingCost = manufacturingCost,
                stampDuty = stampDuty,
                totalPaid = totalCost,
                currentValue = totalCost,
                profitLoss = 0.0,
                profitLossPercent = 0.0,
                status = if (isVerifiedTrader) "معتمدة وموثقة من التاجر" else TransactionStatus.PURCHASED.name,
                metalType = metalType.name,
                mfgType = mfgType,
                cashbackRefund = cashbackRefund,
                sellerShopName = sellerShopName,
                sellerLocation = sellerLocation,
                scalePhotoUri = scalePhotoUri,
                metalLossRatePercent = metalLossRatePercent,
                metalLossGrams = metalLossGrams
            )
            repository.addGoldPurchase(purchase)

            val notifBody = if (isVerifiedTrader) {
                "تم تسجيل وتوثيق فاتورة شراء ${GoldUtils.formatWeight(weight)} $karat من تاجر موثق (${sellerShopName}) بنجاح! الإجمالي: ${GoldUtils.formatCurrency(totalCost)}"
            } else {
                "تم تسجيل فاتورة شراء ${GoldUtils.formatWeight(weight)} $karat بتكلفة إجمالية ${GoldUtils.formatCurrency(totalCost)}"
            }

            NotificationHelper.showTransactionNotification(
                context = getApplication(),
                title = if (isVerifiedTrader) "فاتورة ذهب موثقة من التاجر" else "تحديث حيازة ${metalType.label}",
                body = notifBody
            )
        }
    }

    fun updateProfileImages(avatarUri: String?, coverUri: String?) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updated = user.copy(
                avatarUri = avatarUri ?: user.avatarUri,
                coverUri = coverUri ?: user.coverUri
            )
            repository.saveUser(updated)
        }
    }

    private val _callPrivacy = MutableStateFlow(com.example.util.CallPrivacyManager.getCallPrivacy(application))
    val callPrivacy: StateFlow<com.example.util.CallPrivacy> = _callPrivacy.asStateFlow()

    fun updateCallPrivacy(privacy: com.example.util.CallPrivacy) {
        com.example.util.CallPrivacyManager.setCallPrivacy(getApplication(), privacy)
        _callPrivacy.value = privacy
    }

    fun toggleBlockUser(contactOrUserId: String, block: Boolean) {
        if (block) {
            com.example.util.BlockManager.blockUser(getApplication(), contactOrUserId)
        } else {
            com.example.util.BlockManager.unblockUser(getApplication(), contactOrUserId)
        }
    }

    fun importDeviceContacts(context: Context, onComplete: (count: Int) -> Unit) {
        viewModelScope.launch {
            val imported = withContext(Dispatchers.IO) {
                com.example.util.ContactSyncEngine.importPhoneContacts(context)
            }
            if (imported.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    repository.insertContacts(imported)
                }
            }
            onComplete(imported.size)
        }
    }

    fun checkAndPerformDailyContactSync(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.READ_CONTACTS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    if (com.example.util.ContactSyncEngine.shouldSyncToday(context)) {
                        val imported = com.example.util.ContactSyncEngine.importPhoneContacts(context)
                        if (imported.isNotEmpty()) {
                            repository.insertContacts(imported)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Daily contact sync error", e)
            }
        }
    }

    fun submitVerificationForm(nationalId: String, commercialReg: String, shopAddress: String, notes: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updated = user.copy(
                verificationNationalId = nationalId,
                verificationCommercialReg = commercialReg,
                traderShopAddress = shopAddress,
                verificationStatus = "PENDING"
            )
            repository.saveUser(updated)

            NotificationHelper.showTransactionNotification(
                context = getApplication(),
                title = "تم تقديم طلب التوثيق",
                body = "تم استلام بيانات السجل التجاري والهوية. طلبك قيد المراجعة لدى الإدارة."
            )
        }
    }

    fun submitTraderRequest(shopName: String, shopAddress: String, license: String) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val request = TraderRequestEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = user.id,
                name = user.name,
                phone = user.phone,
                username = user.username,
                shopName = shopName,
                shopAddress = shopAddress,
                license = license,
                status = TraderStatus.PENDING.name
            )
            repository.submitTraderRequest(request)
        }
    }

    fun updateTraderRequestStatus(requestId: String, status: TraderStatus, adminNotes: String?) {
        viewModelScope.launch {
            repository.updateTraderRequestStatus(requestId, status.name, adminNotes)
            NotificationHelper.showTransactionNotification(
                context = getApplication(),
                title = "تحديث حالة التوثيق التجاري",
                body = "تم تغيير حالة طلب توثيق حسابك إلى: ${if (status == TraderStatus.APPROVED) "مقبول وموثق" else "مرفوض"}"
            )
        }
    }

    fun updateGoldPrices(karat21: Double) {
        val k21 = karat21
        val k24 = k21 * (24.0 / 21.0)
        val k18 = k21 * (18.0 / 21.0)
        val k14 = k21 * (14.0 / 21.0)
        val pound = k21 * 8.0

        _goldPrices.value = GoldPricesModel(
            karat14 = k14,
            karat18 = k18,
            karat21 = k21,
            karat24 = k24,
            goldPound = pound,
            ounceUSD = 2650.0,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun updateAdminConfig(
        appName: String? = null,
        appLogoUri: String? = null,
        adminPin: String? = null,
        geminiApiKey: String? = null,
        geminiModelName: String? = null,
        aiSystemPrompt: String? = null,
        notificationUrl: String? = null,
        notificationKey: String? = null,
        newsUrl: String? = null,
        goldUrl: String? = null,
        goldKey: String? = null,
        goldRefreshIntervalSec: Int? = null,
        encryption: Boolean = true,
        voiceSelection: String? = null,
        personaPrompt: String? = null,
        biometricAuthEnabled: Boolean? = null
    ) {
        viewModelScope.launch {
            val current = adminConfig.value ?: AdminConfigEntity()
            val config = current.copy(
                appName = if (!appName.isNullOrBlank()) appName else current.appName,
                appLogoUri = appLogoUri ?: current.appLogoUri,
                adminPin = if (!adminPin.isNullOrBlank()) adminPin else current.adminPin,
                geminiApiKey = geminiApiKey ?: current.geminiApiKey,
                geminiModelName = if (!geminiModelName.isNullOrBlank()) geminiModelName else current.geminiModelName,
                aiSystemPrompt = if (!aiSystemPrompt.isNullOrBlank()) aiSystemPrompt else current.aiSystemPrompt,
                voiceSelection = if (!voiceSelection.isNullOrBlank()) voiceSelection else current.voiceSelection,
                personaPrompt = if (!personaPrompt.isNullOrBlank()) personaPrompt else current.personaPrompt,
                biometricAuthEnabled = biometricAuthEnabled ?: current.biometricAuthEnabled,
                notificationApiUrl = notificationUrl,
                notificationApiKey = notificationKey,
                newsApiUrl = newsUrl,
                goldPriceApiUrl = goldUrl,
                goldPriceApiKey = goldKey,
                goldPriceAutoRefreshIntervalSec = goldRefreshIntervalSec ?: current.goldPriceAutoRefreshIntervalSec,
                encryptionEnabled = encryption
            )
            repository.updateAdminConfig(config)
        }
    }

    fun deleteCurrentUserAccount() {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.saveUser(user.copy(name = "حساب محذوف", phone = "00000000000", isVerified = false, subscriptionStatus = "EXPIRED"))
        }
    }

    // --- Feature 1 & 3: Clearing House & Double-Entry Ledger ---
    fun executeClearingSettlement(
        contactId: String,
        contactName: String,
        netGold24k: Double,
        netSilver999: Double,
        netCash: Double,
        metalLoss: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch

            val settlement = ClearingSettlementEntity(
                id = java.util.UUID.randomUUID().toString(),
                traderAId = user.id,
                traderAName = user.name,
                traderBId = contactId,
                traderBName = contactName,
                netGoldWeight24k = netGold24k,
                netSilverWeight999 = netSilver999,
                netCashAmount = netCash,
                metalLossDeduction = metalLoss,
                notes = notes
            )
            repository.addClearingSettlement(settlement)

            // Double Entry Ledger Record
            val ledgerEntry = LedgerEntryEntity(
                id = java.util.UUID.randomUUID().toString(),
                accountOwnerId = user.id,
                counterPartyId = contactId,
                counterPartyName = contactName,
                assetType = LedgerAssetType.GOLD_24K.name,
                entryType = LedgerEntryType.CREDIT.name,
                weightGrams = netGold24k,
                cashAmount = netCash,
                metalLossRate = metalLoss,
                checksumHash = "SHA256-${System.currentTimeMillis()}-${user.id.take(4)}",
                notes = "عقد تقاص وتسوية آلي: $notes"
            )
            repository.addLedgerEntry(ledgerEntry)

            NotificationHelper.showTransactionNotification(
                context = getApplication(),
                title = "تم إبرام عقد التقاص والتصفية ️",
                body = "تمت تصفية الذمة المالية مع $contactName بصافي ذهب ${GoldUtils.formatWeight(netGold24k)}"
            )
        }
    }

    // --- Feature 5: RBAC for Projects (Bidirectionally synced with WorkGroup Chat) ---
    fun addProjectMember(
        projectId: String,
        userName: String,
        userPhone: String,
        role: ProjectMemberRole,
        task: String?,
        userIdOverride: String? = null,
        isGroupAdmin: Boolean = role.defaultIsAdmin(),
        canPostAccounting: Boolean = role.defaultCanPostAccounting(),
        canEditRecords: Boolean = role.defaultCanEditRecords(),
        canExportReports: Boolean = role.defaultCanExportReports(),
        canMessageDirectly: Boolean = true
    ) {
        viewModelScope.launch {
            val resolvedUserId = userIdOverride?.takeIf { it.isNotBlank() }
                ?: allSubAccounts.value.firstOrNull {
                    it.displayName.equals(userName.trim(), ignoreCase = true) ||
                        (!it.phone.isNullOrBlank() && it.phone == userPhone.trim())
                }?.id
                ?: allContacts.value.firstOrNull {
                    it.name.equals(userName.trim(), ignoreCase = true) ||
                        (userPhone.isNotBlank() && it.phone == userPhone.trim())
                }?.id
                ?: java.util.UUID.randomUUID().toString()

            val member = ProjectMemberEntity(
                id = java.util.UUID.randomUUID().toString(),
                projectId = projectId,
                userId = resolvedUserId,
                userName = userName.trim(),
                userPhone = userPhone.trim(),
                role = role.name,
                assignedTask = task?.trim()?.takeIf { it.isNotBlank() }
            )
            repository.addProjectMember(member)
            repository.syncProjectMemberToWorkGroup(
                projectId = projectId,
                member = member,
                isGroupAdmin = isGroupAdmin,
                canPostAccounting = canPostAccounting,
                canEditRecords = canEditRecords,
                canExportReports = canExportReports,
                canMessageDirectly = canMessageDirectly
            )

            val auditLog = ProjectAuditLogEntity(
                id = java.util.UUID.randomUUID().toString(),
                projectId = projectId,
                action = "إضافة عضو وسند دور",
                actorName = currentUser.value?.name ?: "المالك",
                actorPhone = currentUser.value?.phone ?: "",
                details = "تم تعيين ${userName.trim()} بصفة ${role.label}"
            )
            repository.addProjectAuditLog(auditLog)

            NotificationHelper.showProjectUpdateNotification(
                context = getApplication(),
                projectId = projectId,
                projectName = "فريق المشروع",
                title = "تعيين عضو جديد",
                updateMessage = "تم إسناد مهمة (${userName.trim()}) بصفة ${role.label}"
            )
        }
    }

    fun promoteProjectMemberToManager(
        projectId: String,
        member: ProjectMemberEntity
    ) {
        viewModelScope.launch {
            repository.updateProjectMemberRole(member.id, ProjectMemberRole.EXECUTIVE_MANAGER.name)
            repository.syncProjectMemberToWorkGroup(
                projectId = projectId,
                member = member.copy(role = ProjectMemberRole.EXECUTIVE_MANAGER.name),
                isGroupAdmin = true,
                canPostAccounting = true,
                canEditRecords = true,
                canExportReports = true,
                canMessageDirectly = true
            )
            repository.addProjectAuditLog(
                ProjectAuditLogEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    projectId = projectId,
                    action = "ترقية موظف لصلاحيات مدير",
                    actorName = currentUser.value?.name ?: "المالك",
                    actorPhone = currentUser.value?.phone ?: "",
                    details = "تمت ترقية ${member.userName} إلى ${ProjectMemberRole.EXECUTIVE_MANAGER.label}"
                )
            )
            NotificationHelper.showProjectUpdateNotification(
                context = getApplication(),
                projectId = projectId,
                projectName = "إدارة الصلاحيات",
                title = "ترقية صلاحيات موظف ⭐",
                updateMessage = "تمت ترقية ${member.userName} إلى صلاحيات ${ProjectMemberRole.EXECUTIVE_MANAGER.label}"
            )
            com.example.ui.components.ToastNotificationBus.showToast(
                message = "تمت ترقية ${member.userName} بصلاحيات مدير تنفيذي بنجاح ⭐",
                type = com.example.ui.components.AppToastType.SUCCESS
            )
        }
    }

    fun removeProjectMember(projectId: String, member: ProjectMemberEntity) {
        viewModelScope.launch(safeExceptionHandler) {
            repository.removeProjectMemberWithGroupSync(projectId, member)
            com.example.ui.components.ToastNotificationBus.showToast(
                message = "تمت إزالة ${member.userName} من المشروع والمجموعة",
                type = com.example.ui.components.AppToastType.INFO
            )
        }
    }

    fun updateGroupMemberRoleAndPermissions(
        member: GroupMemberEntity,
        roleLabel: String,
        projectRole: ProjectMemberRole?,
        isAdmin: Boolean,
        canPostAccounting: Boolean,
        canEditRecords: Boolean,
        canExportReports: Boolean,
        canMessageDirectly: Boolean
    ) {
        viewModelScope.launch(safeExceptionHandler) {
            repository.updateGroupMemberRoleAndPermissions(
                member = member,
                roleLabel = roleLabel,
                projectRole = projectRole,
                isAdmin = isAdmin,
                canPostAccounting = canPostAccounting,
                canEditRecords = canEditRecords,
                canExportReports = canExportReports,
                canMessageDirectly = canMessageDirectly
            )
            repository.postSystemMessageToWorkGroupChat(
                workGroupId = member.groupId,
                content = "تم تحديث وظيفة وصلاحيات @${member.username.removePrefix("@")} (${roleLabel.ifBlank { member.roleLabel }})"
            )
            com.example.ui.components.ToastNotificationBus.showToast(
                message = "تم تحديث وظيفة وصلاحيات ${member.username} في المشروع والمجموعة ✅",
                type = com.example.ui.components.AppToastType.SUCCESS
            )
        }
    }

    fun addProjectAccountingEntry(
        projectId: String,
        projectName: String,
        title: String,
        amount: Double,
        isIncome: Boolean,
        accountCategory: String
    ) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            repository.recordProjectAccountingEntry(projectId, amount, isIncome)
            val prefix = if (isIncome) "إيراد" else "مصروف"
            repository.addProjectAuditLog(
                ProjectAuditLogEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    projectId = projectId,
                    action = "قيد محاسبي ($prefix - $accountCategory)",
                    actorName = currentUser.value?.name ?: "المحاسب",
                    actorPhone = currentUser.value?.phone ?: "",
                    details = "$title • المبلغ: ${GoldUtils.formatCurrency(amount)}"
                )
            )
            val linkedProj = repository.getProjectByIdSync(projectId)
            if (linkedProj != null) {
                repository.postSystemMessageToWorkGroupChat(
                    workGroupId = linkedProj.workGroupId,
                    content = "📊 قيد مالي ($prefix - $accountCategory): $title • ${GoldUtils.formatCurrency(amount)}"
                )
            }
            com.example.ui.components.ToastNotificationBus.showToast(
                message = "تم تسجيل $prefix ($title) في حسابات $projectName ✅",
                type = com.example.ui.components.AppToastType.SUCCESS
            )
        }
    }

    fun getProjectMembers(projectId: String): Flow<List<ProjectMemberEntity>> {
        return repository.getProjectMembers(projectId)
    }

    fun getProjectAuditLogs(projectId: String): Flow<List<ProjectAuditLogEntity>> {
        return repository.getProjectAuditLogs(projectId)
    }

    fun triggerProcessSyncQueue() {
        viewModelScope.launch {
            repository.clearSyncedItems()
            NotificationHelper.showTransactionNotification(
                context = getApplication(),
                title = "مزامنة البيانات الميدانية",
                body = "تمت مزامنة جميع المخرجات والعمليات المحفوظة أوفلاين بنجاح مع السيرفر."
            )
        }
    }

    // --- Subscription & Admin Offers Management ---
    fun updatePlanPriceAndOffer(
        planId: String,
        monthlyPrice: Double,
        discountPercent: Int,
        offerTitle: String?,
        isOfferActive: Boolean
    ) {
        viewModelScope.launch {
            repository.updatePlanPriceAndOffer(planId, monthlyPrice, discountPercent, offerTitle, isOfferActive)
        }
    }

    fun submitSubscriptionRequest(
        requestedPlanId: String,
        requestedPlanTitle: String,
        pricePaid: Double,
        paymentMethod: String = "InstaPay / فودافون كاش",
        receiptPhotoUri: String? = null
    ) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val request = SubscriptionRequestEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = user.id,
                userName = user.name,
                userPhone = user.phone,
                requestedPlanId = requestedPlanId,
                requestedPlanTitle = requestedPlanTitle,
                pricePaid = pricePaid,
                paymentMethod = paymentMethod,
                receiptPhotoUri = receiptPhotoUri,
                status = "PENDING"
            )
            repository.submitSubscriptionRequest(request)
        }
    }

    fun updateSubscriptionRequestStatus(requestId: String, status: String, notes: String?, userId: String? = null, planId: String? = null) {
        viewModelScope.launch {
            repository.updateSubscriptionRequestStatus(requestId, status, notes)
            if (status == "APPROVED" && userId != null && planId != null) {
                // Activate plan for 30 days
                val start = System.currentTimeMillis()
                val end = start + (30L * 24 * 3600 * 1000)
                repository.updateUserSubscription(
                    userId = userId,
                    tier = planId,
                    start = start,
                    end = end,
                    isTrial = (planId == SubscriptionTier.FREE_TRIAL.name),
                    status = "ACTIVE"
                )
            }
        }
    }

    fun extendUserTrial(userId: String, additionalDays: Long = 30) {
        viewModelScope.launch {
            val targetUser = allUsers.value.find { it.id == userId } ?: return@launch
            val currentEnd = if (targetUser.subscriptionEndTimestamp > System.currentTimeMillis()) targetUser.subscriptionEndTimestamp else System.currentTimeMillis()
            val newEnd = currentEnd + (additionalDays * 24 * 3600 * 1000)
            repository.updateUserSubscription(
                userId = userId,
                tier = SubscriptionTier.FREE_TRIAL.name,
                start = targetUser.subscriptionStartTimestamp,
                end = newEnd,
                isTrial = true,
                status = "ACTIVE"
            )
        }
    }

    fun updateUserTierDirectly(userId: String, tierName: String, durationDays: Long = 30) {
        viewModelScope.launch {
            val targetUser = allUsers.value.find { it.id == userId } ?: return@launch
            val start = System.currentTimeMillis()
            val end = start + (durationDays * 24 * 3600 * 1000)
            repository.updateUserSubscription(
                userId = userId,
                tier = tierName,
                start = start,
                end = end,
                isTrial = (tierName == SubscriptionTier.FREE_TRIAL.name),
                status = "ACTIVE"
            )
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun postPromoAnnouncement(
        title: String,
        content: String,
        promoCode: String? = null,
        discountPercent: Int = 0,
        targetPlanId: String? = null
    ) {
        viewModelScope.launch {
            val announcement = PromoAnnouncementEntity(
                id = java.util.UUID.randomUUID().toString(),
                title = title,
                content = content,
                promoCode = promoCode,
                discountPercent = discountPercent,
                targetPlanId = targetPlanId,
                isActive = true
            )
            repository.postPromoAnnouncement(announcement)
        }
    }

    fun deletePromoAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deletePromoAnnouncement(id)
        }
    }

    fun setPromoAnnouncementsActive(ids: List<String>, isActive: Boolean) {
        viewModelScope.launch {
            repository.setPromoAnnouncementsActive(ids, isActive)
        }
    }

    // --- Merchant Inventory & Storefront Management ---
    fun addMerchantProduct(
        title: String,
        category: MerchantProductCategory,
        karat: GoldKarat,
        weightGrams: Double,
        mfgCostPerGram: Double,
        photoUri: String?,
        description: String?
    ) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val product = MerchantProductEntity(
                id = java.util.UUID.randomUUID().toString(),
                merchantId = user.id,
                merchantShopName = user.traderShopName ?: user.name,
                merchantPhone = user.phone,
                title = title,
                category = category.name,
                karat = karat.name,
                weightGrams = weightGrams,
                manufacturingCostPerGram = mfgCostPerGram,
                photoUri = photoUri,
                description = description,
                isAvailable = true
            )
            repository.addMerchantProduct(product)

            NotificationHelper.showTransactionNotification(
                context = getApplication(),
                title = "تم إضافة بضاعة جديدة للمتجر ️",
                body = "تم إضافة $title وزن $weightGrams جرام بمعرض متجرك للعملاء."
            )
        }
    }

    fun deleteMerchantProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteMerchantProduct(productId)
        }
    }

    fun incrementMerchantProductViews(productId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementMerchantProductViews(productId)
        }
    }

    fun getReviewsForMerchant(merchantId: String): Flow<List<MerchantReviewEntity>> {
        return repository.getReviewsForMerchant(merchantId)
    }

    fun addMerchantReview(
        merchantId: String,
        rating: Float,
        ratingAccuracy: Float,
        ratingSpeed: Float,
        ratingHonesty: Float,
        comment: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = currentUser.value
            val reviewerName = user?.name ?: "عميل ومشتري معتمد"
            val reviewerPhone = user?.phone
            val review = MerchantReviewEntity(
                id = java.util.UUID.randomUUID().toString(),
                merchantId = merchantId,
                reviewerId = user?.id ?: java.util.UUID.randomUUID().toString(),
                reviewerName = reviewerName,
                reviewerPhone = reviewerPhone,
                reviewerAvatarUri = user?.avatarUri,
                rating = rating,
                ratingAspectAccuracy = ratingAccuracy,
                ratingAspectSpeed = ratingSpeed,
                ratingAspectHonesty = ratingHonesty,
                comment = comment,
                isVerifiedBuyer = true,
                timestamp = System.currentTimeMillis()
            )
            repository.addMerchantReview(review)

            NotificationHelper.showTransactionNotification(
                context = getApplication(),
                title = "تم إضافة تقييمك ومراجعتك بنجاح",
                body = "شكراً لمشاركتك التقييم، يساعد تقييمك في تحسين ترتيب التاجر ومساعدة العملاء."
            )
            onComplete()
        }
    }

    fun updateMerchantProfileInfo(
        jobTitle: String?,
        skills: String?,
        services: String?,
        showSkills: Boolean,
        experienceYears: Int,
        bio: String?,
        shopName: String?,
        address: String?
    ) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updatedUser = user.copy(
                jobTitle = jobTitle,
                skills = skills,
                servicesOffered = services,
                showSkillsPublicly = showSkills,
                experienceYears = experienceYears,
                merchantBio = bio,
                traderShopName = shopName ?: user.traderShopName,
                traderShopAddress = address ?: user.traderShopAddress
            )
            repository.saveUser(updatedUser)
            NotificationHelper.showTransactionNotification(
                context = getApplication(),
                title = "تم تحديث الملف المهني للمتجر",
                body = "تم حفظ وظائفك ومهاراتك وخدماتك وتحديثها لجميع العملاء بنجاح."
            )
        }
    }

    fun recordMerchantSale(
        productId: String?,
        productTitle: String,
        weightGrams: Double,
        karat: GoldKarat,
        pricePerGram: Double,
        mfgCostPerGram: Double,
        totalEGP: Double,
        profitEGP: Double,
        customerName: String,
        customerPhone: String?
    ) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val sale = MerchantDailySaleEntity(
                id = java.util.UUID.randomUUID().toString(),
                merchantId = user.id,
                productId = productId,
                productTitle = productTitle,
                weightGrams = weightGrams,
                karat = karat.name,
                pricePerGram = pricePerGram,
                mfgCostPerGram = mfgCostPerGram,
                totalEGP = totalEGP,
                profitEGP = profitEGP,
                customerName = customerName,
                customerPhone = customerPhone,
                isClosedInDailyReport = false
            )
            repository.recordMerchantSale(sale)

            NotificationHelper.showTransactionNotification(
                context = getApplication(),
                title = "تم تسجيل عملية بيع للمتجر",
                body = "تم بيع $productTitle بقيمة ${GoldUtils.formatCurrency(totalEGP)} وصافي ربح ${GoldUtils.formatCurrency(profitEGP)}"
            )
        }
    }

    fun updateTraderStockWeight(newTotalWeight: Double) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            val updated = user.copy(traderTotalGoldWeight = newTotalWeight)
            repository.saveUser(updated)
            NotificationHelper.showTransactionNotification(
                context = getApplication(),
                title = "تحديث إجمالي مخزون المتجر ️",
                body = "تم تحديث إجمالي جرد الذهب إلى ${GoldUtils.formatWeight(newTotalWeight)}"
            )
        }
    }

    fun executeDailyClosingReport(
        unclosedSales: List<MerchantDailySaleEntity>,
        currentStockWeight: Double
    ) {
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            if (unclosedSales.isEmpty()) return@launch

            val totalCount = unclosedSales.size
            val totalWeightSold = unclosedSales.sumOf { it.weightGrams }
            val totalRevenue = unclosedSales.sumOf { it.totalEGP }
            val totalProfit = unclosedSales.sumOf { it.profitEGP }

            val remainingStock = (currentStockWeight - totalWeightSold).coerceAtLeast(0.0)

            val reportDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(java.util.Date())

            val report = MerchantDailyClosingReportEntity(
                id = java.util.UUID.randomUUID().toString(),
                merchantId = user.id,
                reportDateText = reportDate,
                totalSalesCount = totalCount,
                totalWeightSoldGrams = totalWeightSold,
                totalRevenueEGP = totalRevenue,
                totalProfitEGP = totalProfit,
                remainingInventoryWeightGrams = remainingStock
            )

            // Save report & mark sales closed
            repository.insertMerchantClosingReport(report)
            repository.markMerchantSalesAsClosed(user.id)

            // Permanently deduct sold weight from user's inventory
            val updatedUser = user.copy(traderTotalGoldWeight = remainingStock)
            repository.saveUser(updatedUser)

            NotificationHelper.showTransactionNotification(
                context = getApplication(),
                title = "تم إغلاق تقرير نهاية اليوم بنجاح",
                body = "مبيعات اليوم: ${GoldUtils.formatWeight(totalWeightSold)} بقيمة ${GoldUtils.formatCurrency(totalRevenue)}، والأرباح الصافية ${GoldUtils.formatCurrency(totalProfit)}."
            )
        }
    }

    // --- Pinning & Chat Management ---
    fun togglePinChat(chatId: String, currentPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePinChat(chatId, currentPinned)
        }
    }

    // --- Custom Price Alerts with Push Notifications ---
    fun addPriceAlert(
        metalType: String,
        karat: String,
        targetPrice: Double,
        direction: String,
        note: String?
    ) {
        viewModelScope.launch {
            val alert = PriceAlertEntity(
                id = java.util.UUID.randomUUID().toString(),
                metalType = metalType,
                karat = karat,
                targetPrice = targetPrice,
                direction = direction,
                isActive = true,
                isTriggered = false,
                note = note
            )
            repository.addPriceAlert(alert)
            NotificationHelper.showTransactionNotification(
                context = getApplication(),
                title = "تم ضبط وتفعيل تنبيه السعر",
                body = "سيتم إرسال إشعار فوري لحظة وصول ${if (metalType == "GOLD") "الذهب" else "الفضة"} عيار $karat إلى ${GoldUtils.formatCurrency(targetPrice)}"
            )

            // Immediately check against current market prices
            checkAndTriggerPriceAlerts(_goldPrices.value)
        }
    }

    fun deletePriceAlert(alertId: String) {
        viewModelScope.launch {
            repository.deletePriceAlert(alertId)
        }
    }

    fun togglePriceAlert(alertId: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.togglePriceAlert(alertId, isActive)
            if (isActive) {
                checkAndTriggerPriceAlerts(_goldPrices.value)
            }
        }
    }

    fun resetPriceAlertTrigger(alertId: String) {
        viewModelScope.launch {
            repository.setPriceAlertTriggered(alertId, false)
            repository.togglePriceAlert(alertId, true)
            checkAndTriggerPriceAlerts(_goldPrices.value)
        }
    }

    fun checkAndTriggerPriceAlerts(prices: GoldPricesModel = _goldPrices.value) {
        viewModelScope.launch {
            val alerts = repository.allPriceAlerts.first()
            for (alert in alerts) {
                if (!alert.isActive) continue

                val currentPrice = getMetalPriceForAlert(alert, prices)

                val isThresholdReached = if (alert.direction == "ABOVE" || alert.direction == "أعلى") {
                    currentPrice >= alert.targetPrice
                } else {
                    currentPrice <= alert.targetPrice
                }

                if (isThresholdReached && !alert.isTriggered) {
                    // Send real-time high-priority push notification
                    NotificationHelper.showGoldPriceThresholdNotification(
                        context = getApplication(),
                        alertId = alert.id,
                        metalType = alert.metalType,
                        karat = alert.karat,
                        targetPrice = alert.targetPrice,
                        currentPrice = currentPrice,
                        direction = alert.direction,
                        note = alert.note
                    )
                    // Mark as triggered in database
                    repository.setPriceAlertTriggered(alert.id, true)
                } else if (!isThresholdReached && alert.isTriggered) {
                    // When market price returns to normal range, allow re-triggering when target is reached again
                    repository.setPriceAlertTriggered(alert.id, false)
                }
            }
        }
    }

    fun testTriggerPriceAlert(alert: PriceAlertEntity) {
        viewModelScope.launch {
            val currentPrice = getMetalPriceForAlert(alert, _goldPrices.value)
            NotificationHelper.showGoldPriceThresholdNotification(
                context = getApplication(),
                alertId = alert.id,
                metalType = alert.metalType,
                karat = alert.karat,
                targetPrice = alert.targetPrice,
                currentPrice = currentPrice,
                direction = alert.direction,
                note = alert.note
            )
            repository.setPriceAlertTriggered(alert.id, true)
        }
    }

    private fun getMetalPriceForAlert(alert: PriceAlertEntity, prices: GoldPricesModel): Double {
        return if (alert.metalType.equals("SILVER", ignoreCase = true) || alert.metalType == "فضة") {
            when (alert.karat) {
                "999", "SILVER_999" -> prices.silver999
                "925", "SILVER_925" -> prices.silver925
                "800", "SILVER_800" -> prices.silver800
                else -> prices.silver925
            }
        } else {
            when (alert.karat) {
                "24", "KARAT_24" -> prices.karat24
                "21", "KARAT_21" -> prices.karat21
                "18", "KARAT_18" -> prices.karat18
                "14", "KARAT_14" -> prices.karat14
                "POUND", "GOLD_POUND" -> prices.goldPound
                "OUNCE", "OUNCE_USD" -> prices.ounceUSD
                else -> prices.karat21
            }
        }
    }

    // --- App Lock & Security ---
    fun updateAppLock(isLockEnabled: Boolean, pin: String, biometricEnabled: Boolean = true) {
        viewModelScope.launch {
            repository.updateAppLock(isLockEnabled, pin, biometricEnabled)
            val current = currentUser.value
            if (current != null) {
                saveRepeatUser(current.copy(appLockEnabled = isLockEnabled, appLockPin = pin, biometricAuthEnabled = biometricEnabled))
            }
            NotificationHelper.showTransactionNotification(
                context = getApplication(),
                title = if (isLockEnabled) "تم تفعيل القفل الأمني والمصادقة البيومترية" else "تم إلغاء القفل الأمني",
                body = if (isLockEnabled) "تم تأمين بيانات الصفقات والذهب والمحفظة ببصمة الإصبع والوجه ورمز PIN." else "تم تعطيل القفل الأمني عند الدخول."
            )
        }
    }

    // --- AI Thinking Mode Query State ---
    private val _aiThinkingResponse = MutableStateFlow<String?>(null)
    val aiThinkingResponse: StateFlow<String?> = _aiThinkingResponse.asStateFlow()

    private val _isAiThinkingLoading = MutableStateFlow(false)
    val isAiThinkingLoading: StateFlow<Boolean> = _isAiThinkingLoading.asStateFlow()

    fun queryAiWithThinking(prompt: String, useThinking: Boolean = true) {
        viewModelScope.launch(safeExceptionHandler) {
            _isAiThinkingLoading.value = true
            try {
                val snapshot = buildGlobalAccountingSnapshot()
                val globalSummary = SmartAccountantEngine.buildGlobalContextSummary(snapshot)
                val searchRes = SmartAccountantEngine.searchGlobalRecords(prompt, snapshot)
                val hasDirectSearchHits = searchRes.matchedInvoices.isNotEmpty() ||
                    searchRes.matchedProjects.isNotEmpty() ||
                    searchRes.matchedHashtags.isNotEmpty() ||
                    searchRes.matchedProducts.isNotEmpty() ||
                    searchRes.matchedRecords.isNotEmpty()

                val contextData = buildString {
                    appendLine("أسعار الذهب اللحظية: عيار 24=${_goldPrices.value.karat24} ج.م، عيار 21=${_goldPrices.value.karat21} ج.م، عيار 18=${_goldPrices.value.karat18} ج.م، فضة 999=${_goldPrices.value.silver999} ج.م.")
                    appendLine("عدد المشتريات بالحقيبة: ${allGoldPurchases.value.size}، إجمالي الصفقات: ${allTransactions.value.sumOf { it.totalPrice }} ج.م.")
                    appendLine(globalSummary)
                    if (hasDirectSearchHits) {
                        appendLine("نتائج البحث المباشر عن \"$prompt\":")
                        searchRes.matchedInvoices.take(6).forEach { inv ->
                            appendLine("- فاتورة ${inv.invoiceNumber} (${inv.receiverName}) #${inv.hashtagTag ?: "-"}: الإجمالي ${inv.totalAmount} | المدفوع ${inv.paidAmount} | الحالة ${inv.status}")
                        }
                        searchRes.matchedHashtags.take(5).forEach { h ->
                            appendLine("- هاشتاج #${h.tag} (${h.projectName ?: "عام"}): ${h.status}")
                        }
                        searchRes.matchedProjects.take(5).forEach { p ->
                            appendLine("- مشروع ${p.name}: دخل ${p.totalIncome} | مصروف ${p.totalExpenses}")
                        }
                    }
                }
                val result = GeminiAiService.queryGeminiAi(
                    userQuery = prompt,
                    contextInfo = contextData,
                    adminConfig = adminConfig.value,
                    useThinkingMode = useThinking
                )
                _aiThinkingResponse.value = result
            } catch (e: Exception) {
                _aiThinkingResponse.value = "حدث خطأ أثناء معالجة الاستعلام: ${e.message ?: "غير معروف"}"
            } finally {
                _isAiThinkingLoading.value = false
            }
        }
    }

    fun clearAiThinkingResponse() {
        _aiThinkingResponse.value = null
    }

    // --- Encrypted Backup & Cross-Device Transfer Operations ---
    private val _backupStatus = MutableStateFlow<BackupResult?>(null)
    val backupStatus: StateFlow<BackupResult?> = _backupStatus.asStateFlow()

    private val _isBackupProcessing = MutableStateFlow(false)
    val isBackupProcessing: StateFlow<Boolean> = _isBackupProcessing.asStateFlow()

    private val _cloudBackups = MutableStateFlow<List<com.example.util.CloudBackupRecord>>(emptyList())
    val cloudBackups: StateFlow<List<com.example.util.CloudBackupRecord>> = _cloudBackups.asStateFlow()

    private val _isCloudBackupsLoading = MutableStateFlow(false)
    val isCloudBackupsLoading: StateFlow<Boolean> = _isCloudBackupsLoading.asStateFlow()

    private val _cloudBackupsError = MutableStateFlow<String?>(null)
    val cloudBackupsError: StateFlow<String?> = _cloudBackupsError.asStateFlow()

    fun refreshCloudBackups() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            _isCloudBackupsLoading.value = true
            _cloudBackupsError.value = null
            val res = SecureBackupVaultEngine.listServerCloudBackups(user.phone)
            res.onSuccess { list ->
                _cloudBackups.value = list
            }.onFailure { err ->
                _cloudBackupsError.value = err.message
            }
            _isCloudBackupsLoading.value = false
        }
    }

    fun deleteCloudBackup(backupId: String) {
        viewModelScope.launch {
            _isBackupProcessing.value = true
            val res = SecureBackupVaultEngine.deleteServerCloudBackup(backupId)
            res.onFailure { err ->
                _backupStatus.value = BackupResult.Error(err.message ?: "تعذر حذف النسخة السحابية من الخادم.")
            }
            _isBackupProcessing.value = false
            refreshCloudBackups()
        }
    }

    fun downloadCloudBackupContent(
        backupId: String,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            _isBackupProcessing.value = true
            val res = SecureBackupVaultEngine.downloadServerCloudBackup(backupId)
            _isBackupProcessing.value = false
            res.onSuccess { item ->
                val content = item.vaultContent
                if (content.isNullOrBlank()) {
                    onResult(Result.failure(IllegalStateException("محتوى النسخة السحابية فارغ.")))
                } else {
                    onResult(Result.success(content))
                }
            }.onFailure { err ->
                _backupStatus.value = BackupResult.Error(err.message ?: "فشل تحميل النسخة السحابية من الخادم.")
                onResult(Result.failure(err))
            }
        }
    }

    fun restoreCloudBackupById(
        backupId: String,
        passphrase: String,
        onComplete: (BackupResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user == null) {
                val err = BackupResult.Error("يجب تسجيل الدخول أولاً للتحقق من هوية رقم الهاتف.")
                _backupStatus.value = err
                onComplete(err)
                return@launch
            }
            _isBackupProcessing.value = true
            val dl = SecureBackupVaultEngine.downloadServerCloudBackup(backupId)
            if (dl.isFailure) {
                val err = BackupResult.Error(dl.exceptionOrNull()?.message ?: "تعذر تنزيل النسخة السحابية.")
                _backupStatus.value = err
                _isBackupProcessing.value = false
                onComplete(err)
                return@launch
            }
            val vaultContent = dl.getOrNull()?.vaultContent.orEmpty()
            val db = com.example.data.local.AppDatabase.getInstance(getApplication())
            val result = SecureBackupVaultEngine.restoreEncryptedBackup(
                context = getApplication(),
                db = db,
                backupFileContent = vaultContent,
                currentUserPhone = user.phone,
                enteredPassphrase = passphrase
            )
            _backupStatus.value = result
            _isBackupProcessing.value = false
            onComplete(result)
        }
    }

    fun refreshLocalStateAfterRestoreOrTransfer() {
        viewModelScope.launch {
            val restoredUser = repository.currentUser.firstOrNull()
            if (restoredUser != null) {
                saveRepeatUser(restoredUser)
            }
            refreshCloudBackups()
        }
    }

    /**
     * Targeted migration export for sub-accounts and secret vaults.
     */
    fun createSubAccountsMigrationBackup(
        passphrase: String,
        selectedProfileIds: List<String>? = null,
        cloudProvider: com.example.util.CloudStorageProvider = com.example.util.CloudStorageProvider.LOCAL_DEVICE,
        onComplete: (BackupResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user == null) {
                val err = BackupResult.Error("لم يتم العثور على المستخدم الحالي.")
                _backupStatus.value = err
                onComplete(err)
                return@launch
            }
            _isBackupProcessing.value = true
            val db = com.example.data.local.AppDatabase.getInstance(getApplication())
            val result = SecureBackupVaultEngine.createSubAccountsMigrationBackup(
                context = getApplication(),
                db = db,
                currentUserPhone = user.phone,
                encryptionPassphrase = passphrase,
                selectedProfileIds = selectedProfileIds,
                cloudProvider = cloudProvider
            )
            _backupStatus.value = result
            _isBackupProcessing.value = false
            refreshCloudBackups()
            onComplete(result)
        }
    }

    fun createEncryptedBackup(
        passphrase: String,
        isGoogleDriveCloudExport: Boolean = false,
        onComplete: (BackupResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user == null) {
                val err = BackupResult.Error("لم يتم العثور على المستخدم الحالي.")
                _backupStatus.value = err
                onComplete(err)
                return@launch
            }
            _isBackupProcessing.value = true
            val db = com.example.data.local.AppDatabase.getInstance(getApplication())
            val result = SecureBackupVaultEngine.createEncryptedBackup(
                context = getApplication(),
                db = db,
                currentUserPhone = user.phone,
                encryptionPassphrase = passphrase,
                isGoogleDriveCloudExport = isGoogleDriveCloudExport
            )
            _backupStatus.value = result
            _isBackupProcessing.value = false
            refreshCloudBackups()
            onComplete(result)
        }
    }

    fun restoreEncryptedBackup(
        fileContent: String,
        passphrase: String,
        onComplete: (BackupResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user == null) {
                val err = BackupResult.Error("يجب تسجيل الدخول أولاً للتحقق من هوية رقم الهاتف.")
                _backupStatus.value = err
                onComplete(err)
                return@launch
            }
            _isBackupProcessing.value = true
            val db = com.example.data.local.AppDatabase.getInstance(getApplication())
            val result = SecureBackupVaultEngine.restoreEncryptedBackup(
                context = getApplication(),
                db = db,
                backupFileContent = fileContent,
                currentUserPhone = user.phone,
                enteredPassphrase = passphrase
            )
            _backupStatus.value = result
            _isBackupProcessing.value = false
            onComplete(result)
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }

    private val _appFontScale = MutableStateFlow(com.example.ui.theme.AppFontScale.STANDARD)
    val appFontScale: StateFlow<com.example.ui.theme.AppFontScale> = _appFontScale.asStateFlow()

    fun setAppFontScale(scale: com.example.ui.theme.AppFontScale) {
        _appFontScale.value = scale
    }

    // ==========================================
    // Contextual AI Chat Translation Engine
    // ==========================================
    private val _translatedMessages = MutableStateFlow<Map<String, TranslatedMessageUiState>>(emptyMap())
    val translatedMessages: StateFlow<Map<String, TranslatedMessageUiState>> = _translatedMessages.asStateFlow()

    fun translateMessageContextually(
        message: MessageEntity,
        recentChatMessages: List<MessageEntity>,
        targetLanguage: String = "auto"
    ) {
        viewModelScope.launch {
            _translatedMessages.update { current ->
                current + (message.id to TranslatedMessageUiState(
                    messageId = message.id,
                    originalText = message.content,
                    translatedText = "",
                    sourceLang = "auto",
                    targetLang = targetLanguage,
                    isLoading = true
                ))
            }

            val currentAdminConfig = adminConfig.value
            val contextStrings = recentChatMessages
                .filter { it.type == MessageType.TEXT.name }
                .takeLast(6)
                .map { "${it.senderName}: ${it.content}" }

            val result = GeminiAiService.translateContextually(
                targetMessage = message.content,
                surroundingContext = contextStrings,
                targetLanguage = targetLanguage,
                adminConfig = currentAdminConfig
            )

            _translatedMessages.update { current ->
                current + (message.id to TranslatedMessageUiState(
                    messageId = message.id,
                    originalText = message.content,
                    translatedText = result.translatedText,
                    sourceLang = result.sourceLanguage,
                    targetLang = result.targetLanguage,
                    contextualNote = result.contextualNotes,
                    isLoading = false
                ))
            }
        }
    }

    fun dismissTranslation(messageId: String) {
        _translatedMessages.update { current ->
            current - messageId
        }
    }

    // ==========================================
    // Translation Typography & Appearance Customization
    // ==========================================
    private val _translationFontSize = MutableStateFlow(
        prefs.getString("translation_font_size", "medium") ?: "medium"
    )
    val translationFontSize: StateFlow<String> = _translationFontSize.asStateFlow()

    private val _translationColorStyle = MutableStateFlow(
        prefs.getString("translation_color_style", "gold") ?: "gold"
    )
    val translationColorStyle: StateFlow<String> = _translationColorStyle.asStateFlow()

    fun setTranslationFontSize(size: String) {
        _translationFontSize.value = size
        prefs.edit().putString("translation_font_size", size).apply()
    }

    fun setTranslationColorStyle(style: String) {
        _translationColorStyle.value = style
        prefs.edit().putString("translation_color_style", style).apply()
    }

    fun clearAllTranslations() {
        _translatedMessages.value = emptyMap()
    }

    // ==========================================
    // Custom Message Notification Tones & Sounds
    // ==========================================
    private val _messageNotificationTone = MutableStateFlow(
        com.example.util.MessageNotificationSoundHelper.getSelectedTone(getApplication())
    )
    val messageNotificationTone: StateFlow<String> = _messageNotificationTone.asStateFlow()

    private val _isMessageSoundEnabled = MutableStateFlow(
        com.example.util.MessageNotificationSoundHelper.isSoundEnabled(getApplication())
    )
    val isMessageSoundEnabled: StateFlow<Boolean> = _isMessageSoundEnabled.asStateFlow()

    private val _isMessageVibrationEnabled = MutableStateFlow(
        com.example.util.MessageNotificationSoundHelper.isVibrationEnabled(getApplication())
    )
    val isMessageVibrationEnabled: StateFlow<Boolean> = _isMessageVibrationEnabled.asStateFlow()

    fun setMessageNotificationTone(toneId: String) {
        _messageNotificationTone.value = toneId
        com.example.util.MessageNotificationSoundHelper.setSelectedTone(getApplication(), toneId)
    }

    fun setMessageSoundEnabled(enabled: Boolean) {
        _isMessageSoundEnabled.value = enabled
        com.example.util.MessageNotificationSoundHelper.setSoundEnabled(getApplication(), enabled)
    }

    fun setMessageVibrationEnabled(enabled: Boolean) {
        _isMessageVibrationEnabled.value = enabled
        com.example.util.MessageNotificationSoundHelper.setVibrationEnabled(getApplication(), enabled)
    }

    fun playTonePreview(toneId: String) {
        com.example.util.MessageNotificationSoundHelper.playTonePreview(getApplication(), toneId)
    }

    fun stopTonePlayback() {
        com.example.util.MessageNotificationSoundHelper.stopPlayback()
    }

    fun testMessageNotification() {
        com.example.util.MessageNotificationSoundHelper.playNewMessageSound(getApplication())
        com.example.util.NotificationHelper.showChatMessageNotification(
            context = getApplication(),
            chatId = "preview_test",
            senderName = "OOZOX",
            messageText = "مرحباً! هذا إشعار تجريبي لاختبار نغمة الرسائل.",
            senderAvatarUri = null
        )
    }

    fun updateUserProfession(profession: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value ?: return@launch
            val updatedUser = user.copy(jobTitle = profession)
            repository.insertUser(updatedUser)
        }
    }

    fun addMerchantProduct(product: MerchantProductEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addMerchantProduct(product)
        }
    }

    fun addInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addInvoice(invoice)
        }
    }

    fun addShiftHandover(handover: ShiftHandoverEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertShiftHandover(handover)
        }
    }

    fun receiveShiftHandover(handoverId: String, receiverId: String, receiverName: String, notes: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.receiveShiftHandover(handoverId, receiverId, receiverName, notes)
        }
    }

    fun receiveShiftHandoverFull(
        handoverId: String,
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
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.receiveShiftHandoverFull(
                id = handoverId,
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
    }

    fun applyJointComplaintDeduction(
        complaintId: String,
        primaryAmount: Double,
        receiverAmount: Double,
        receiverId: String?,
        notes: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.applyJointComplaintDeduction(
                complaintId = complaintId,
                primaryAmount = primaryAmount,
                receiverAmount = receiverAmount,
                receiverId = receiverId,
                resolutionNotes = notes
            )
        }
    }

    fun respondToComplaint(
        complaintId: String,
        accepted: Boolean,
        rejectionReason: String?,
        rejectionProofUri: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val status = if (accepted) "ACCEPTED" else "REJECTED"
            repository.updateComplaintEmployeeResponse(
                id = complaintId,
                status = status,
                reason = rejectionReason,
                proofUri = rejectionProofUri,
                isDeducted = accepted
            )
        }
    }

    fun decideOnComplaintByManager(complaintId: String, decision: String, notes: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateComplaintManagerDecision(complaintId, decision, notes)
        }
    }

    fun submitFieldTaskWorkerResponse(
        taskId: String,
        uri: String?,
        type: String,
        notes: String?,
        userId: String?,
        userName: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.submitFieldTaskWorkerResponse(taskId, uri, type, notes, userId, userName)
            val task = repository.getProjectTaskByIdSync(taskId)
            val employee = userName?.ifBlank { null } ?: currentUser.value?.name ?: "الموظف"
            val mediaLabel = if (type == "VIDEO") "فيديو قصير" else "صورة موثقة"
            if (task != null) {
                repository.addProjectAuditLog(
                    ProjectAuditLogEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        projectId = task.projectId,
                        action = "توثيق إتمام مهمة ($mediaLabel)",
                        actorName = employee,
                        actorPhone = currentUser.value?.phone ?: "",
                        details = "وثق الموظف إتمام (${task.title}): ${notes ?: "تم التنفيذ"}"
                    )
                )
                task.workGroupId?.let { wgId ->
                    repository.getWorkGroupByIdSync(wgId)?.let { wg ->
                        val gChat = repository.ensureWorkGroupChat(wg, displayTitle = wg.name)
                        repository.sendMessage(
                            chatId = gChat.id,
                            senderId = "system",
                            senderName = "النظام",
                            content = "وثق @$employee إتمام مهمة (${task.title}) وبانتظار الاعتماد ✅",
                            type = MessageType.SYSTEM.name
                        )
                    }
                }
                NotificationHelper.showProjectUpdateNotification(
                    context = getApplication(),
                    projectId = task.projectId,
                    projectName = task.projectName,
                    title = "إشعار للأدمن: تم توثيق مهمة ✅",
                    updateMessage = "وثّق الموظف ($employee) إتمام مهمة (${task.title}) عبر $mediaLabel وبانتظار اعتمادك"
                )
            }
            com.example.ui.components.ToastNotificationBus.showToast(
                message = "تم إرسال توثيق المهمة وإشعار الأدمن بنجاح ✅",
                type = com.example.ui.components.AppToastType.SUCCESS
            )
        }
    }

    fun confirmAndArchiveFieldTask(
        taskId: String,
        financialAmount: Double,
        financialNotes: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.confirmAndArchiveFieldTask(taskId, financialAmount, financialNotes)
            val task = repository.getProjectTaskByIdSync(taskId)
            if (task != null) {
                if (financialAmount > 0.0) {
                    repository.recordProjectAccountingEntry(task.projectId, financialAmount, isIncome = false)
                }
                repository.addProjectAuditLog(
                    ProjectAuditLogEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        projectId = task.projectId,
                        action = "اعتماد وأرشفة مهمة",
                        actorName = currentUser.value?.name ?: "الأدمن",
                        actorPhone = currentUser.value?.phone ?: "",
                        details = "تم اعتماد (${task.title}) • القيمة المالية: ${GoldUtils.formatCurrency(financialAmount)}"
                    )
                )
                task.workGroupId?.let { wgId ->
                    repository.getWorkGroupByIdSync(wgId)?.let { wg ->
                        val gChat = repository.ensureWorkGroupChat(wg, displayTitle = wg.name)
                        repository.sendMessage(
                            chatId = gChat.id,
                            senderId = "system",
                            senderName = "النظام",
                            content = "تم اعتماد وأرشفة مهمة (${task.title}) ✅${if (financialAmount > 0) " • ${GoldUtils.formatCurrency(financialAmount)}" else ""}",
                            type = MessageType.SYSTEM.name
                        )
                    }
                }
            }
            com.example.ui.components.ToastNotificationBus.showToast(
                message = "تم اعتماد المهمة وأرشفتها وتحديث محاسبة المشروع ✅",
                type = com.example.ui.components.AppToastType.SUCCESS
            )
        }
    }

    fun assignChatContactToProjectWorkGroup(
        chat: ChatEntity,
        workGroup: WorkGroupEntity,
        project: ProjectEntity?,
        roleLabel: String,
        projectRole: ProjectMemberRole,
        canPostAccounting: Boolean,
        canEditRecords: Boolean,
        canExportReports: Boolean,
        canMessageDirectly: Boolean,
        isGroupAdmin: Boolean
    ) {
        viewModelScope.launch(safeExceptionHandler) {
            val actor = currentUser.value
            repository.assignChatContactToProjectWorkGroup(
                chat = chat,
                workGroup = workGroup,
                project = project,
                roleLabel = roleLabel,
                projectRole = projectRole,
                canPostAccounting = canPostAccounting,
                canEditRecords = canEditRecords,
                canExportReports = canExportReports,
                canMessageDirectly = canMessageDirectly,
                isGroupAdmin = isGroupAdmin,
                actorName = actor?.name ?: "الإدارة",
                actorPhone = actor?.phone ?: ""
            )
            val targetName = project?.name?.let { "${workGroup.name} ($it)" } ?: workGroup.name
            com.example.ui.components.ToastNotificationBus.showToast(
                message = "تمت إضافة ${chat.contactName} إلى $targetName بنجاح",
                type = com.example.ui.components.AppToastType.SUCCESS
            )
        }
    }

    // ---------------- Enterprise Business Setup, HR, Payroll, Leaves & GPS Check-In ----------------

    fun saveBusinessProfile(
        profile: BusinessProfileEntity,
        onSaved: (WorkGroupEntity) -> Unit = {}
    ) {
        viewModelScope.launch(safeExceptionHandler) {
            val ownerId = currentUser.value?.id ?: "me"
            val wg = repository.saveBusinessProfileAndSyncGroup(profile, ownerId)
            val sectorLabel = BusinessActivitySector.entries
                .firstOrNull { it.name == profile.activityType }
                ?.labelAr
                ?: profile.customActivityLabel.ifBlank { profile.activityType }
            updateUserProfession(sectorLabel)

            if (com.example.util.RetrofitClient.hasActiveSession()) {
                try {
                    com.example.util.RetrofitClient.apiService().upsertBusinessProfile(
                        com.example.util.ApiBusinessProfileRequest(
                            companyName = profile.companyName,
                            activityType = profile.activityType,
                            customActivityLabel = profile.customActivityLabel,
                            commercialRegistration = profile.commercialRegistration,
                            taxNumber = profile.taxNumber,
                            phone = profile.phone,
                            address = profile.address,
                            currency = profile.currency,
                            hqLatitude = profile.hqLatitude,
                            hqLongitude = profile.hqLongitude,
                            geofenceRadiusMeters = profile.geofenceRadiusMeters,
                            companyGroupName = profile.companyGroupName
                        )
                    )
                } catch (_: Exception) {}
            }

            com.example.ui.components.ToastNotificationBus.showToast(
                message = "تم حفظ بيانات النشاط ومجموعة كل الموظفين بنجاح 🏢",
                type = com.example.ui.components.AppToastType.SUCCESS
            )
            onSaved(wg)
        }
    }

    fun createOrUpdateEnterpriseEmployee(
        existingId: String? = null,
        fullName: String,
        username: String,
        phone: String,
        password: String,
        roleTitle: String,
        department: String,
        baseSalary: Double,
        salaryCurrency: String,
        geofenceEnforced: Boolean,
        isActiveEmployee: Boolean = true,
        canCreateInvoices: Boolean,
        canViewReports: Boolean,
        canSetProductPrices: Boolean,
        permManageBusiness: Boolean,
        permManageStaff: Boolean,
        permManagePayroll: Boolean,
        permManageProjects: Boolean,
        permAssignTasks: Boolean,
        permManageAccounting: Boolean,
        permApproveDiscounts: Boolean,
        permDirectChat: Boolean,
        onComplete: (HiddenSubAccountEntity) -> Unit = {}
    ) {
        viewModelScope.launch(safeExceptionHandler) {
            val cleanUsername = username.trim().removePrefix("@").lowercase()
            val existing = existingId?.let { id -> allSubAccounts.value.find { it.id == id } }
            val salt = if (password.isNotBlank() || existing == null) {
                com.example.util.HiddenAccountSecurityEngine.generateSalt()
            } else {
                existing.passwordSalt
            }
            val hash = if (password.isNotBlank() || existing == null) {
                com.example.util.HiddenAccountSecurityEngine.hashPassword(password.trim(), salt)
            } else {
                existing.passwordHash
            }

            val employee = HiddenSubAccountEntity(
                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                username = cleanUsername,
                displayName = fullName.trim(),
                passwordHash = hash,
                passwordSalt = salt,
                rawSecretTrigger = if (password.isNotBlank()) password.trim() else existing?.rawSecretTrigger.orEmpty(),
                avatarUri = existing?.avatarUri,
                bio = "$roleTitle • $department",
                phone = phone.trim().ifBlank { null },
                muteNotifications = existing?.muteNotifications ?: false,
                discreetNotifications = existing?.discreetNotifications ?: false,
                autoLockOnExit = false,
                isEncrypted = true,
                roleTitle = roleTitle.trim().ifBlank { "موظف" },
                canSetProductPrices = canSetProductPrices,
                canCreateInvoices = canCreateInvoices,
                canViewReports = canViewReports,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                lastActiveAt = System.currentTimeMillis(),
                department = department.trim().ifBlank { "العمليات" },
                baseSalary = baseSalary.coerceAtLeast(0.0),
                salaryCurrency = salaryCurrency.ifBlank { "EGP" },
                geofenceEnforced = geofenceEnforced,
                permManageBusiness = permManageBusiness,
                permManageStaff = permManageStaff,
                permManagePayroll = permManagePayroll,
                permManageProjects = permManageProjects,
                permAssignTasks = permAssignTasks,
                permManageAccounting = permManageAccounting,
                permApproveDiscounts = permApproveDiscounts,
                permDirectChat = permDirectChat,
                isActiveEmployee = isActiveEmployee
            )

            repository.addHiddenSubAccount(employee)
            val profile = repository.getBusinessProfileSync()
            repository.syncEmployeeToCompanyGroup(employee, profile)

            val permsDto = com.example.util.ApiEmployeePermissionsDto(
                manageBusiness = permManageBusiness,
                manageStaff = permManageStaff,
                managePayroll = permManagePayroll,
                manageProjects = permManageProjects,
                assignTasks = permAssignTasks,
                manageAccounting = permManageAccounting,
                createInvoices = canCreateInvoices,
                approveDiscounts = permApproveDiscounts,
                viewReports = canViewReports,
                directChat = permDirectChat
            )

            if (com.example.util.RetrofitClient.hasActiveSession()) {
                try {
                    if (existing == null && password.isNotBlank()) {
                        com.example.util.RetrofitClient.apiService().createEmployee(
                            com.example.util.ApiCreateEmployeeRequest(
                                fullName = employee.displayName,
                                username = employee.username,
                                phone = employee.phone.orEmpty(),
                                password = password.trim(),
                                roleTitle = employee.roleTitle,
                                department = employee.department,
                                baseSalary = employee.baseSalary,
                                salaryCurrency = employee.salaryCurrency,
                                geofenceEnforced = employee.geofenceEnforced,
                                permissions = permsDto
                            )
                        )
                    } else if (existing != null) {
                        com.example.util.RetrofitClient.apiService().updateEmployee(
                            employeeId = employee.id,
                            request = com.example.util.ApiUpdateEmployeeRequest(
                                fullName = employee.displayName,
                                phone = employee.phone.orEmpty(),
                                password = password.trim().ifBlank { null },
                                roleTitle = employee.roleTitle,
                                department = employee.department,
                                baseSalary = employee.baseSalary,
                                salaryCurrency = employee.salaryCurrency,
                                geofenceEnforced = employee.geofenceEnforced,
                                isActive = employee.isActiveEmployee,
                                permissions = permsDto
                            )
                        )
                    }
                } catch (_: Exception) {}
            }

            com.example.ui.components.ToastNotificationBus.showToast(
                message = "تم حفظ الموظف ${employee.displayName} وإضافته لمجموعة الشركة ✅",
                type = com.example.ui.components.AppToastType.SUCCESS
            )
            onComplete(employee)
        }
    }

    fun addEmployeePayrollAdjustment(
        employee: HiddenSubAccountEntity,
        type: String,
        amount: Double,
        reason: String,
        effectiveDate: String
    ) {
        viewModelScope.launch(safeExceptionHandler) {
            if (amount <= 0.0) return@launch
            val monthKey = effectiveDate.take(7).ifBlank {
                java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date())
            }
            val adj = PayrollAdjustmentEntity(
                id = "pay_${System.currentTimeMillis()}_${(100..999).random()}",
                employeeId = employee.id,
                employeeName = employee.displayName,
                type = type,
                amount = amount,
                reason = reason.trim(),
                effectiveDate = effectiveDate,
                monthKey = monthKey,
                createdByName = currentUser.value?.name ?: "المدير"
            )
            repository.insertPayrollAdjustment(adj)
            if (com.example.util.RetrofitClient.hasActiveSession()) {
                try {
                    com.example.util.RetrofitClient.apiService().addPayrollAdjustment(
                        com.example.util.ApiPayrollAdjustmentRequest(
                            employeeId = employee.id,
                            type = type,
                            amount = amount,
                            reason = reason.trim(),
                            effectiveDate = effectiveDate
                        )
                    )
                } catch (_: Exception) {}
            }
            val actionLabel = if (type == "BONUS") "حافز مالي +" else "خصم مالي -"
            com.example.ui.components.ToastNotificationBus.showToast(
                message = "تم تسجيل $actionLabel ${GoldUtils.formatCurrency(amount)} للموظف ${employee.displayName}",
                type = com.example.ui.components.AppToastType.SUCCESS
            )
        }
    }

    fun deletePayrollAdjustment(id: String) {
        viewModelScope.launch(safeExceptionHandler) {
            repository.deletePayrollAdjustment(id)
            if (com.example.util.RetrofitClient.hasActiveSession()) {
                try {
                    com.example.util.RetrofitClient.apiService().deletePayrollAdjustment(id)
                } catch (_: Exception) {}
            }
        }
    }

    fun addEmployeeLeaveRecord(
        employee: HiddenSubAccountEntity,
        leaveType: String,
        startDate: String,
        endDate: String,
        notes: String
    ) {
        viewModelScope.launch(safeExceptionHandler) {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val days = try {
                val s = fmt.parse(startDate)?.time ?: System.currentTimeMillis()
                val e = fmt.parse(endDate)?.time ?: s
                (((e - s) / (1000L * 60 * 60 * 24)).toInt() + 1).coerceAtLeast(1)
            } catch (_: Exception) { 1 }

            val leave = EmployeeLeaveEntity(
                id = "lv_${System.currentTimeMillis()}_${(100..999).random()}",
                employeeId = employee.id,
                employeeName = employee.displayName,
                leaveType = leaveType,
                startDate = startDate,
                endDate = endDate,
                daysCount = days,
                status = "APPROVED",
                notes = notes.trim(),
                approvedByName = currentUser.value?.name ?: "المدير"
            )
            repository.insertEmployeeLeave(leave)
            if (com.example.util.RetrofitClient.hasActiveSession()) {
                try {
                    com.example.util.RetrofitClient.apiService().addEmployeeLeave(
                        com.example.util.ApiEmployeeLeaveRequest(
                            employeeId = employee.id,
                            leaveType = leaveType,
                            startDate = startDate,
                            endDate = endDate,
                            notes = notes.trim()
                        )
                    )
                } catch (_: Exception) {}
            }
            com.example.ui.components.ToastNotificationBus.showToast(
                message = "تم اعتماد إجازة (${days} يوم) للموظف ${employee.displayName}",
                type = com.example.ui.components.AppToastType.SUCCESS
            )
        }
    }

    fun deleteEmployeeLeave(id: String) {
        viewModelScope.launch(safeExceptionHandler) {
            repository.deleteEmployeeLeave(id)
            if (com.example.util.RetrofitClient.hasActiveSession()) {
                try {
                    com.example.util.RetrofitClient.apiService().deleteEmployeeLeave(id)
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun loginEmployeeWithMandatoryGps(
        usernameOrPhone: String,
        password: String
    ): Pair<Boolean, com.example.util.WorkplaceLocationPolicyManager.LocationPolicyCheckResult?> {
        val cleanUser = usernameOrPhone.trim().removePrefix("@")
        val cleanSecret = password.trim()
        val matched = allSubAccounts.value.find {
            val matchesId = it.phone.equals(cleanUser, ignoreCase = true) ||
                it.username.equals(cleanUser, ignoreCase = true)
            val matchesPass = com.example.util.HiddenAccountSecurityEngine.verifyPassword(cleanSecret, it)
            matchesId && matchesPass && it.isActiveEmployee
        } ?: return false to null

        if (com.example.util.RetrofitClient.hasActiveSession()) {
            try {
                com.example.util.RetrofitClient.apiService().employeeLogin(
                    com.example.util.ApiEmployeeLoginRequest(
                        username = matched.username,
                        password = cleanSecret
                    )
                )
            } catch (_: Exception) {}
        }

        val gpsResult = clockInEmployeeWithMandatoryGps(matched)
        if (!matched.geofenceEnforced || gpsResult.isCompliant) {
            _activeSubAccount.value = matched
            com.example.util.HiddenAccountSecurityEngine.setActiveSubAccount(matched)
            repository.updateSubAccount(matched.copy(lastActiveAt = System.currentTimeMillis()))
        }
        return true to gpsResult
    }

    suspend fun clockInEmployeeWithMandatoryGps(
        employee: HiddenSubAccountEntity
    ): com.example.util.WorkplaceLocationPolicyManager.LocationPolicyCheckResult {
        val appCtx = getApplication<Application>()
        val profile = repository.getBusinessProfileSync()
        val groupId = profile?.companyGroupId?.ifBlank { "wg_company_main" } ?: "wg_company_main"

        val result = if (employee.geofenceEnforced) {
            com.example.util.WorkplaceLocationPolicyManager.enforceLoginLocationPolicy(
                context = appCtx,
                employeeId = employee.id,
                employeeName = employee.displayName,
                workplaceLat = profile?.hqLatitude,
                workplaceLng = profile?.hqLongitude,
                radiusMeters = profile?.geofenceRadiusMeters ?: 250,
                workplaceName = profile?.companyName?.ifBlank { "مقر العمل الرئيسي" } ?: "مقر العمل الرئيسي"
            )
        } else {
            com.example.util.WorkplaceLocationPolicyManager.LocationPolicyCheckResult(
                isCompliant = true,
                status = "COMPLIANT_IN_BOUNDS",
                messageAr = "تم استلام العمل بنجاح (إلزام الموقع مستثنى لهذا الموظف)",
                isGpsActive = true,
                shouldAlertManager = false
            )
        }

        val curDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val curTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale("ar")).format(java.util.Date())
        val now = System.currentTimeMillis()

        val attendance = EmployeeAttendanceEntity(
            id = "att_${employee.id}_$curDate",
            memberId = "gm_${employee.id}",
            groupId = groupId,
            employeeName = employee.displayName,
            employeeRole = employee.roleTitle,
            date = curDate,
            clockInTime = curTime,
            clockInTimestamp = now,
            status = when {
                !result.isGpsActive || result.status == "PERMISSIONS_MISSING" -> "GPS_DISABLED"
                !result.isCompliant -> "OUT_OF_BOUNDS"
                else -> "CLOCKED_IN"
            },
            locationTag = profile?.companyName?.ifBlank { "المقر الرئيسي" } ?: "المقر الرئيسي",
            notes = result.messageAr,
            isLocationVerified = result.isCompliant,
            verificationDetails = result.messageAr,
            outOfBoundsSince = if (result.isCompliant) null else now,
            gpsStatus = result.status,
            locationPolicyViolated = !result.isCompliant,
            lastLocationCheckTime = now,
            timestamp = now
        )
        repository.saveAttendance(attendance)

        if (!result.isCompliant && result.shouldAlertManager) {
            val wg = repository.getWorkGroupByIdSync(groupId)
            if (wg != null) {
                val chat = repository.ensureWorkGroupChat(wg, displayTitle = wg.name)
                repository.sendMessage(
                    chatId = chat.id,
                    senderId = "system",
                    senderName = "نظام المراقبة الجغرافية",
                    content = "🚨 تنبيه إداري: ${result.messageAr} (الموظف: ${employee.displayName})",
                    type = MessageType.SYSTEM.name
                )
            }
        } else {
            try {
                val oneTimeReq = androidx.work.OneTimeWorkRequestBuilder<com.example.service.GeofenceMonitoringWorker>().build()
                androidx.work.WorkManager.getInstance(appCtx).enqueue(oneTimeReq)
            } catch (_: Exception) {}
        }

        if (com.example.util.RetrofitClient.hasActiveSession()) {
            try {
                com.example.util.RetrofitClient.apiService().checkInAttendance(
                    com.example.util.ApiAttendanceGeoRequest(
                        employeeId = employee.id,
                        gpsEnabled = result.isGpsActive,
                        latitude = result.currentLatitude,
                        longitude = result.currentLongitude
                    )
                )
            } catch (_: Exception) {}
        }

        return result
    }

    fun openCompanyStaffGroupChat(onChatReady: (String) -> Unit) {
        viewModelScope.launch(safeExceptionHandler) {
            val profile = repository.getBusinessProfileSync()
            val ownerId = currentUser.value?.id ?: "me"
            val wg = repository.saveBusinessProfileAndSyncGroup(
                profile = profile ?: BusinessProfileEntity(
                    ownerId = ownerId,
                    companyName = currentUser.value?.name ?: "الشركة الرئيسية",
                    companyGroupName = "مجموعة كل الموظفين"
                ),
                ownerId = ownerId
            )
            val chat = repository.ensureWorkGroupChat(wg, displayTitle = wg.name)
            onChatReady(chat.id)
        }
    }

    companion object {
        const val INITIAL_CHAT_PAGE_SIZE = 35
        const val OLDER_CHAT_PAGE_SIZE = 30
    }
}

data class ChatHistoryUiState(
    val messages: List<MessageEntity> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isLoadingOlder: Boolean = false,
    val hasMoreOlder: Boolean = false,
    val totalCount: Int = 0
)

data class TranslatedMessageUiState(
    val messageId: String,
    val originalText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val contextualNote: String? = null,
    val isLoading: Boolean = false,
    val isSpeaking: Boolean = false
)

