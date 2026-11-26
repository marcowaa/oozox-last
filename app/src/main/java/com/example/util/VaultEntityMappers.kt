package com.example.util

import com.example.data.model.*
import org.json.JSONObject

/**
 * Lossless JSON serializers/deserializers for all 42 Room entities in AppDatabase.
 * Uses explicit field mapping (no reflection) for R8/ProGuard safety and deterministic Vault V3 & ECDH transfers.
 */
internal object VaultEntityMappers {

    private fun JSONObject.strOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it != "null" }

    private fun JSONObject.longOrNull(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)

    private fun JSONObject.doubleOrNull(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key)

    private fun JSONObject.putOptValue(key: String, value: Any?) {
        if (value != null) put(key, value)
    }

    private fun rewrite(uri: String?, map: Map<String, String>): String? {
        if (uri.isNullOrBlank()) return uri
        return map[uri] ?: map[uri.removePrefix("file://")] ?: uri
    }

    fun userToJson(u: UserEntity) = JSONObject().apply {
        put("id", u.id)
        put("name", u.name)
        put("phone", u.phone)
        put("username", u.username)
        put("type", u.type)
        put("isVerified", u.isVerified)
        put("createdAt", u.createdAt)
        put("traderStatus", u.traderStatus)
        putOptValue("traderLicense", u.traderLicense)
        putOptValue("traderShopName", u.traderShopName)
        putOptValue("traderShopAddress", u.traderShopAddress)
        put("traderTotalGoldWeight", u.traderTotalGoldWeight)
        putOptValue("traderApprovedAt", u.traderApprovedAt)
        putOptValue("avatarUri", u.avatarUri)
        putOptValue("coverUri", u.coverUri)
        putOptValue("verificationNationalId", u.verificationNationalId)
        putOptValue("verificationCommercialReg", u.verificationCommercialReg)
        put("verificationStatus", u.verificationStatus)
        put("subscriptionTier", u.subscriptionTier)
        put("subscriptionStartTimestamp", u.subscriptionStartTimestamp)
        put("subscriptionEndTimestamp", u.subscriptionEndTimestamp)
        put("isTrialActive", u.isTrialActive)
        put("subscriptionStatus", u.subscriptionStatus)
        put("appLockEnabled", u.appLockEnabled)
        putOptValue("appLockPin", u.appLockPin)
        put("biometricAuthEnabled", u.biometricAuthEnabled)
        putOptValue("jobTitle", u.jobTitle)
        putOptValue("skills", u.skills)
        putOptValue("servicesOffered", u.servicesOffered)
        put("showSkillsPublicly", u.showSkillsPublicly)
        put("experienceYears", u.experienceYears)
        put("ratingScore", u.ratingScore.toDouble())
        put("totalReviewsCount", u.totalReviewsCount)
        putOptValue("merchantBio", u.merchantBio)
        putOptValue("education", u.education)
        putOptValue("cvSectionOrder", u.cvSectionOrder)
        putOptValue("city", u.city)
    }

    fun jsonToUser(j: JSONObject, map: Map<String, String> = emptyMap()) = UserEntity(
        id = j.getString("id"),
        name = j.optString("name", "مستخدم أوزوكس"),
        phone = j.optString("phone", ""),
        username = j.optString("username", ""),
        type = j.optString("type", UserType.BUSINESSMAN.name),
        isVerified = j.optBoolean("isVerified", false),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        traderStatus = j.optString("traderStatus", TraderStatus.NONE.name),
        traderLicense = j.strOrNull("traderLicense"),
        traderShopName = j.strOrNull("traderShopName"),
        traderShopAddress = j.strOrNull("traderShopAddress"),
        traderTotalGoldWeight = j.optDouble("traderTotalGoldWeight", 0.0),
        traderApprovedAt = j.longOrNull("traderApprovedAt"),
        avatarUri = rewrite(j.strOrNull("avatarUri"), map),
        coverUri = rewrite(j.strOrNull("coverUri"), map),
        verificationNationalId = j.strOrNull("verificationNationalId"),
        verificationCommercialReg = j.strOrNull("verificationCommercialReg"),
        verificationStatus = j.optString("verificationStatus", "NONE"),
        subscriptionTier = j.optString("subscriptionTier", SubscriptionTier.FREE_TRIAL.name),
        subscriptionStartTimestamp = j.optLong("subscriptionStartTimestamp", System.currentTimeMillis()),
        subscriptionEndTimestamp = j.optLong("subscriptionEndTimestamp", System.currentTimeMillis() + 60L * 24 * 3600 * 1000),
        isTrialActive = j.optBoolean("isTrialActive", true),
        subscriptionStatus = j.optString("subscriptionStatus", "ACTIVE"),
        appLockEnabled = j.optBoolean("appLockEnabled", false),
        appLockPin = j.strOrNull("appLockPin"),
        biometricAuthEnabled = j.optBoolean("biometricAuthEnabled", true),
        jobTitle = j.strOrNull("jobTitle"),
        skills = j.strOrNull("skills"),
        servicesOffered = j.strOrNull("servicesOffered"),
        showSkillsPublicly = j.optBoolean("showSkillsPublicly", true),
        experienceYears = j.optInt("experienceYears", 8),
        ratingScore = j.optDouble("ratingScore", 4.9).toFloat(),
        totalReviewsCount = j.optInt("totalReviewsCount", 24),
        merchantBio = j.strOrNull("merchantBio"),
        education = j.strOrNull("education"),
        cvSectionOrder = j.strOrNull("cvSectionOrder"),
        city = j.strOrNull("city")
    )

    fun contactToJson(c: ContactEntity) = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("phone", c.phone)
        putOptValue("username", c.username)
        put("type", c.type)
        put("isVerified", c.isVerified)
        put("fromPhoneBook", c.fromPhoneBook)
        putOptValue("subAccountId", c.subAccountId)
    }

    fun jsonToContact(j: JSONObject) = ContactEntity(
        id = j.getString("id"),
        name = j.optString("name", ""),
        phone = j.optString("phone", ""),
        username = j.strOrNull("username"),
        type = j.optString("type", UserType.BUSINESSMAN.name),
        isVerified = j.optBoolean("isVerified", false),
        fromPhoneBook = j.optBoolean("fromPhoneBook", false),
        subAccountId = j.strOrNull("subAccountId")
    )

    fun chatToJson(c: ChatEntity) = JSONObject().apply {
        put("id", c.id)
        put("contactId", c.contactId)
        put("contactName", c.contactName)
        put("contactType", c.contactType)
        put("contactVerified", c.contactVerified)
        putOptValue("lastMessage", c.lastMessage)
        putOptValue("lastMessageTime", c.lastMessageTime)
        putOptValue("lastMessageStatus", c.lastMessageStatus)
        putOptValue("lastMessageSenderId", c.lastMessageSenderId)
        put("unreadCount", c.unreadCount)
        put("isPinned", c.isPinned)
        put("createdAt", c.createdAt)
        put("isGroup", c.isGroup)
        putOptValue("groupId", c.groupId)
        putOptValue("groupName", c.groupName)
        put("isArchived", c.isArchived)
        putOptValue("subAccountId", c.subAccountId)
    }

    fun jsonToChat(j: JSONObject) = ChatEntity(
        id = j.getString("id"),
        contactId = j.optString("contactId", ""),
        contactName = j.optString("contactName", ""),
        contactType = j.optString("contactType", UserType.BUSINESSMAN.name),
        contactVerified = j.optBoolean("contactVerified", false),
        lastMessage = j.strOrNull("lastMessage"),
        lastMessageTime = j.longOrNull("lastMessageTime")?.takeIf { it > 0L },
        lastMessageStatus = j.strOrNull("lastMessageStatus"),
        lastMessageSenderId = j.strOrNull("lastMessageSenderId"),
        unreadCount = j.optInt("unreadCount", 0),
        isPinned = j.optBoolean("isPinned", false),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        isGroup = j.optBoolean("isGroup", false),
        groupId = j.strOrNull("groupId"),
        groupName = j.strOrNull("groupName"),
        isArchived = j.optBoolean("isArchived", false),
        subAccountId = j.strOrNull("subAccountId")
    )

    fun messageToJson(m: MessageEntity) = JSONObject().apply {
        put("id", m.id)
        put("chatId", m.chatId)
        put("senderId", m.senderId)
        put("senderName", m.senderName)
        put("type", m.type)
        put("content", m.content)
        putOptValue("transactionId", m.transactionId)
        putOptValue("hashtagId", m.hashtagId)
        put("timestamp", m.timestamp)
        put("status", m.status)
        putOptValue("reaction", m.reaction)
        putOptValue("subAccountId", m.subAccountId)
    }

    fun jsonToMessage(j: JSONObject, map: Map<String, String> = emptyMap()): MessageEntity {
        val rawContent = j.optString("content", "")
        var updatedContent = rawContent
        if (map.isNotEmpty()) {
            for ((oldPath, newPath) in map) {
                if (updatedContent.contains(oldPath)) {
                    updatedContent = updatedContent.replace(oldPath, newPath)
                }
            }
        }
        return MessageEntity(
            id = j.getString("id"),
            chatId = j.getString("chatId"),
            senderId = j.optString("senderId", ""),
            senderName = j.optString("senderName", ""),
            type = j.optString("type", MessageType.TEXT.name),
            content = updatedContent,
            transactionId = j.strOrNull("transactionId"),
            hashtagId = j.strOrNull("hashtagId"),
            timestamp = j.optLong("timestamp", System.currentTimeMillis()),
            status = j.optString("status", MessageStatus.SENT.name),
            reaction = j.strOrNull("reaction"),
            subAccountId = j.strOrNull("subAccountId")
        )
    }

    fun chatAccountToJson(a: ChatAccountEntity) = JSONObject().apply {
        put("id", a.id)
        put("chatId", a.chatId)
        put("name", a.name)
        put("creatorId", a.creatorId)
        put("createdAt", a.createdAt)
        put("status", a.status)
    }

    fun jsonToChatAccount(j: JSONObject) = ChatAccountEntity(
        id = j.getString("id"),
        chatId = j.getString("chatId"),
        name = j.optString("name", ""),
        creatorId = j.optString("creatorId", ""),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        status = j.optString("status", HashtagStatus.ACTIVE.name)
    )

    fun hashtagAccountToJson(h: HashtagAccountEntity) = JSONObject().apply {
        put("id", h.id)
        put("chatId", h.chatId)
        put("accountId", h.accountId)
        put("tag", h.tag)
        put("creatorId", h.creatorId)
        put("createdAt", h.createdAt)
        put("status", h.status)
        putOptValue("projectId", h.projectId)
        putOptValue("projectName", h.projectName)
    }

    fun jsonToHashtagAccount(j: JSONObject) = HashtagAccountEntity(
        id = j.getString("id"),
        chatId = j.getString("chatId"),
        accountId = j.optString("accountId", ""),
        tag = j.optString("tag", ""),
        creatorId = j.optString("creatorId", ""),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        status = j.optString("status", HashtagStatus.ACTIVE.name),
        projectId = j.strOrNull("projectId"),
        projectName = j.strOrNull("projectName")
    )

    fun hashtagEntryToJson(e: HashtagEntryEntity) = JSONObject().apply {
        put("id", e.id)
        put("hashtagId", e.hashtagId)
        put("accountId", e.accountId)
        put("type", e.type)
        put("description", e.description)
        put("amount", e.amount)
        put("direction", e.direction)
        put("createdBy", e.createdBy)
        put("createdAt", e.createdAt)
        putOptValue("photoUri", e.photoUri)
        putOptValue("linkedInvoiceId", e.linkedInvoiceId)
        putOptValue("projectId", e.projectId)
        putOptValue("projectName", e.projectName)
    }

    fun jsonToHashtagEntry(j: JSONObject, map: Map<String, String> = emptyMap()) = HashtagEntryEntity(
        id = j.getString("id"),
        hashtagId = j.getString("hashtagId"),
        accountId = j.optString("accountId", ""),
        type = j.optString("type", HashtagEntryType.INVOICE.name),
        description = j.optString("description", ""),
        amount = j.optDouble("amount", 0.0),
        direction = j.optString("direction", HashtagEntryDirection.DEBIT.name),
        createdBy = j.optString("createdBy", ""),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        photoUri = rewrite(j.strOrNull("photoUri"), map),
        linkedInvoiceId = j.strOrNull("linkedInvoiceId"),
        projectId = j.strOrNull("projectId"),
        projectName = j.strOrNull("projectName")
    )

    fun commentToJson(c: CommentEntity) = JSONObject().apply {
        put("id", c.id)
        put("targetId", c.targetId)
        put("text", c.text)
        put("createdBy", c.createdBy)
        put("createdAt", c.createdAt)
    }

    fun jsonToComment(j: JSONObject) = CommentEntity(
        id = j.getString("id"),
        targetId = j.optString("targetId", ""),
        text = j.optString("text", ""),
        createdBy = j.optString("createdBy", ""),
        createdAt = j.optLong("createdAt", System.currentTimeMillis())
    )

    fun hiddenProfileToJson(p: HiddenProfileEntity) = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("secretPinOrPassword", p.secretPinOrPassword)
        put("companyName", p.companyName)
        put("roleTitle", p.roleTitle)
        putOptValue("avatarUri", p.avatarUri)
        put("initialCapital", p.initialCapital)
        put("isEncrypted", p.isEncrypted)
        put("silentNotifications", p.silentNotifications)
        putOptValue("colorHex", p.colorHex)
        putOptValue("notes", p.notes)
        put("createdAt", p.createdAt)
    }

    fun jsonToHiddenProfile(j: JSONObject, map: Map<String, String> = emptyMap()) = HiddenProfileEntity(
        id = j.getString("id"),
        name = j.optString("name", ""),
        secretPinOrPassword = j.optString("secretPinOrPassword", ""),
        companyName = j.optString("companyName", ""),
        roleTitle = j.optString("roleTitle", "المدير المالي والمسؤول"),
        avatarUri = rewrite(j.strOrNull("avatarUri"), map),
        initialCapital = j.optDouble("initialCapital", 0.0),
        isEncrypted = j.optBoolean("isEncrypted", true),
        silentNotifications = j.optBoolean("silentNotifications", true),
        colorHex = j.strOrNull("colorHex"),
        notes = j.strOrNull("notes"),
        createdAt = j.optLong("createdAt", System.currentTimeMillis())
    )

    fun hiddenSubAccountToJson(a: HiddenSubAccountEntity) = JSONObject().apply {
        put("id", a.id)
        put("username", a.username)
        put("displayName", a.displayName)
        put("passwordHash", a.passwordHash)
        put("passwordSalt", a.passwordSalt)
        put("rawSecretTrigger", a.rawSecretTrigger)
        putOptValue("avatarUri", a.avatarUri)
        putOptValue("bio", a.bio)
        putOptValue("phone", a.phone)
        put("muteNotifications", a.muteNotifications)
        put("discreetNotifications", a.discreetNotifications)
        put("discreetNotificationTitle", a.discreetNotificationTitle)
        put("autoLockOnExit", a.autoLockOnExit)
        put("isEncrypted", a.isEncrypted)
        put("roleTitle", a.roleTitle)
        put("canSetProductPrices", a.canSetProductPrices)
        put("canCreateInvoices", a.canCreateInvoices)
        put("canViewReports", a.canViewReports)
        put("createdAt", a.createdAt)
        put("lastActiveAt", a.lastActiveAt)
        put("department", a.department)
        put("baseSalary", a.baseSalary)
        put("salaryCurrency", a.salaryCurrency)
        put("geofenceEnforced", a.geofenceEnforced)
        put("permManageBusiness", a.permManageBusiness)
        put("permManageStaff", a.permManageStaff)
        put("permManagePayroll", a.permManagePayroll)
        put("permManageProjects", a.permManageProjects)
        put("permAssignTasks", a.permAssignTasks)
        put("permManageAccounting", a.permManageAccounting)
        put("permApproveDiscounts", a.permApproveDiscounts)
        put("permDirectChat", a.permDirectChat)
        put("isActiveEmployee", a.isActiveEmployee)
    }

    fun jsonToHiddenSubAccount(j: JSONObject, map: Map<String, String> = emptyMap()) = HiddenSubAccountEntity(
        id = j.getString("id"),
        username = j.optString("username", ""),
        displayName = j.optString("displayName", ""),
        passwordHash = j.optString("passwordHash", ""),
        passwordSalt = j.optString("passwordSalt", ""),
        rawSecretTrigger = j.optString("rawSecretTrigger", ""),
        avatarUri = rewrite(j.strOrNull("avatarUri"), map),
        bio = j.strOrNull("bio"),
        phone = j.strOrNull("phone"),
        muteNotifications = j.optBoolean("muteNotifications", true),
        discreetNotifications = j.optBoolean("discreetNotifications", false),
        discreetNotificationTitle = j.optString("discreetNotificationTitle", "تحديث النظام"),
        autoLockOnExit = j.optBoolean("autoLockOnExit", true),
        isEncrypted = j.optBoolean("isEncrypted", true),
        roleTitle = j.optString("roleTitle", "كاشير ومبيعات"),
        canSetProductPrices = j.optBoolean("canSetProductPrices", true),
        canCreateInvoices = j.optBoolean("canCreateInvoices", true),
        canViewReports = j.optBoolean("canViewReports", false),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        lastActiveAt = j.optLong("lastActiveAt", System.currentTimeMillis()),
        department = j.optString("department", "العمليات"),
        baseSalary = j.optDouble("baseSalary", 0.0),
        salaryCurrency = j.optString("salaryCurrency", "EGP"),
        geofenceEnforced = j.optBoolean("geofenceEnforced", true),
        permManageBusiness = j.optBoolean("permManageBusiness", false),
        permManageStaff = j.optBoolean("permManageStaff", false),
        permManagePayroll = j.optBoolean("permManagePayroll", false),
        permManageProjects = j.optBoolean("permManageProjects", false),
        permAssignTasks = j.optBoolean("permAssignTasks", false),
        permManageAccounting = j.optBoolean("permManageAccounting", true),
        permApproveDiscounts = j.optBoolean("permApproveDiscounts", false),
        permDirectChat = j.optBoolean("permDirectChat", true),
        isActiveEmployee = j.optBoolean("isActiveEmployee", true)
    )

    fun workGroupToJson(g: WorkGroupEntity) = JSONObject().apply {
        put("id", g.id)
        put("name", g.name)
        putOptValue("description", g.description)
        put("ownerId", g.ownerId)
        putOptValue("parentGroupId", g.parentGroupId)
        put("isAccountingGroup", g.isAccountingGroup)
        put("isPrivate", g.isPrivate)
        put("disableMemberDMs", g.disableMemberDMs)
        putOptValue("companyName", g.companyName)
        put("companyCapital", g.companyCapital)
        putOptValue("profileId", g.profileId)
        put("workType", g.workType)
        put("createdAt", g.createdAt)
    }

    fun jsonToWorkGroup(j: JSONObject) = WorkGroupEntity(
        id = j.getString("id"),
        name = j.optString("name", ""),
        description = j.strOrNull("description"),
        ownerId = j.optString("ownerId", ""),
        parentGroupId = j.strOrNull("parentGroupId"),
        isAccountingGroup = j.optBoolean("isAccountingGroup", false),
        isPrivate = j.optBoolean("isPrivate", true),
        disableMemberDMs = j.optBoolean("disableMemberDMs", true),
        companyName = j.strOrNull("companyName"),
        companyCapital = j.optDouble("companyCapital", 0.0),
        profileId = j.strOrNull("profileId"),
        workType = j.optString("workType", "FINANCIAL_ONLY"),
        createdAt = j.optLong("createdAt", System.currentTimeMillis())
    )

    fun groupMemberToJson(m: GroupMemberEntity) = JSONObject().apply {
        put("id", m.id)
        put("groupId", m.groupId)
        put("userId", m.userId)
        put("username", m.username)
        put("phone", m.phone)
        put("roleLabel", m.roleLabel)
        put("status", m.status)
        putOptValue("workingHoursStart", m.workingHoursStart)
        putOptValue("workingHoursEnd", m.workingHoursEnd)
        put("shiftHours", m.shiftHours)
        put("workingDays", m.workingDays)
        put("shiftType", m.shiftType)
        put("canPostAccounting", m.canPostAccounting)
        put("canExportReports", m.canExportReports)
        put("canEditRecords", m.canEditRecords)
        put("canMessageDirectly", m.canMessageDirectly)
        put("isGroupAdmin", m.isGroupAdmin)
        putOptValue("notes", m.notes)
        put("addedAt", m.addedAt)
        putOptValue("workplaceLocation", m.workplaceLocation)
        putOptValue("workplaceLatitude", m.workplaceLatitude)
        putOptValue("workplaceLongitude", m.workplaceLongitude)
        put("geofencingEnabled", m.geofencingEnabled)
        put("geofencingRadiusMeters", m.geofencingRadiusMeters)
    }

    fun jsonToGroupMember(j: JSONObject) = GroupMemberEntity(
        id = j.getString("id"),
        groupId = j.getString("groupId"),
        userId = j.optString("userId", ""),
        username = j.optString("username", ""),
        phone = j.optString("phone", ""),
        roleLabel = j.optString("roleLabel", ""),
        status = j.optString("status", "accepted"),
        workingHoursStart = j.strOrNull("workingHoursStart"),
        workingHoursEnd = j.strOrNull("workingHoursEnd"),
        shiftHours = j.optDouble("shiftHours", 8.0),
        workingDays = j.optString("workingDays", "السبت - الخميس"),
        shiftType = j.optString("shiftType", "نهارية (صباحية)"),
        canPostAccounting = j.optBoolean("canPostAccounting", true),
        canExportReports = j.optBoolean("canExportReports", false),
        canEditRecords = j.optBoolean("canEditRecords", false),
        canMessageDirectly = j.optBoolean("canMessageDirectly", false),
        isGroupAdmin = j.optBoolean("isGroupAdmin", false),
        notes = j.strOrNull("notes"),
        addedAt = j.optLong("addedAt", System.currentTimeMillis()),
        workplaceLocation = j.strOrNull("workplaceLocation"),
        workplaceLatitude = j.doubleOrNull("workplaceLatitude"),
        workplaceLongitude = j.doubleOrNull("workplaceLongitude"),
        geofencingEnabled = j.optBoolean("geofencingEnabled", false),
        geofencingRadiusMeters = j.optInt("geofencingRadiusMeters", 200)
    )

    fun attendanceToJson(a: EmployeeAttendanceEntity) = JSONObject().apply {
        put("id", a.id)
        put("memberId", a.memberId)
        put("groupId", a.groupId)
        put("employeeName", a.employeeName)
        put("employeeRole", a.employeeRole)
        put("date", a.date)
        putOptValue("clockInTime", a.clockInTime)
        putOptValue("clockOutTime", a.clockOutTime)
        putOptValue("clockInTimestamp", a.clockInTimestamp)
        putOptValue("clockOutTimestamp", a.clockOutTimestamp)
        putOptValue("scheduledShiftStart", a.scheduledShiftStart)
        putOptValue("scheduledShiftEnd", a.scheduledShiftEnd)
        put("scheduledShiftHours", a.scheduledShiftHours)
        put("actualWorkedHours", a.actualWorkedHours)
        put("status", a.status)
        put("overtimeMinutes", a.overtimeMinutes)
        put("lateMinutes", a.lateMinutes)
        put("earlyDepartureMinutes", a.earlyDepartureMinutes)
        putOptValue("locationTag", a.locationTag)
        putOptValue("notes", a.notes)
        put("isLocationVerified", a.isLocationVerified)
        putOptValue("verificationDetails", a.verificationDetails)
        putOptValue("outOfBoundsSince", a.outOfBoundsSince)
        put("gpsStatus", a.gpsStatus)
        put("locationPolicyViolated", a.locationPolicyViolated)
        put("lastLocationCheckTime", a.lastLocationCheckTime)
        put("timestamp", a.timestamp)
    }

    fun jsonToAttendance(j: JSONObject) = EmployeeAttendanceEntity(
        id = j.getString("id"),
        memberId = j.optString("memberId", ""),
        groupId = j.optString("groupId", ""),
        employeeName = j.optString("employeeName", ""),
        employeeRole = j.optString("employeeRole", ""),
        date = j.optString("date", ""),
        clockInTime = j.strOrNull("clockInTime"),
        clockOutTime = j.strOrNull("clockOutTime"),
        clockInTimestamp = j.longOrNull("clockInTimestamp")?.takeIf { it > 0L },
        clockOutTimestamp = j.longOrNull("clockOutTimestamp")?.takeIf { it > 0L },
        scheduledShiftStart = j.strOrNull("scheduledShiftStart"),
        scheduledShiftEnd = j.strOrNull("scheduledShiftEnd"),
        scheduledShiftHours = j.optDouble("scheduledShiftHours", 8.0),
        actualWorkedHours = j.optDouble("actualWorkedHours", 0.0),
        status = j.optString("status", "CLOCKED_IN"),
        overtimeMinutes = j.optInt("overtimeMinutes", 0),
        lateMinutes = j.optInt("lateMinutes", 0),
        earlyDepartureMinutes = j.optInt("earlyDepartureMinutes", 0),
        locationTag = j.strOrNull("locationTag"),
        notes = j.strOrNull("notes"),
        isLocationVerified = j.optBoolean("isLocationVerified", false),
        verificationDetails = j.strOrNull("verificationDetails"),
        outOfBoundsSince = j.longOrNull("outOfBoundsSince"),
        gpsStatus = j.optString("gpsStatus", "ACTIVE"),
        locationPolicyViolated = j.optBoolean("locationPolicyViolated", false),
        lastLocationCheckTime = j.optLong("lastLocationCheckTime", System.currentTimeMillis()),
        timestamp = j.optLong("timestamp", System.currentTimeMillis())
    )

    fun dailyRecordToJson(r: DailyAccountingRecordEntity) = JSONObject().apply {
        put("id", r.id)
        put("groupId", r.groupId)
        putOptValue("subGroupId", r.subGroupId)
        putOptValue("profileId", r.profileId)
        put("entryDate", r.entryDate)
        put("staffId", r.staffId)
        put("staffName", r.staffName)
        put("staffRole", r.staffRole)
        put("dailyRevenue", r.dailyRevenue)
        put("incomingAmount", r.incomingAmount)
        put("outgoingAmount", r.outgoingAmount)
        put("deficitAmount", r.deficitAmount)
        put("complaintsCount", r.complaintsCount)
        putOptValue("complaintsNotes", r.complaintsNotes)
        put("dailyCapital", r.dailyCapital)
        put("weeklyCapital", r.weeklyCapital)
        put("monthlyCapital", r.monthlyCapital)
        putOptValue("screenshotUri", r.screenshotUri)
        putOptValue("notes", r.notes)
        put("isEncrypted", r.isEncrypted)
        put("timestamp", r.timestamp)
    }

    fun jsonToDailyRecord(j: JSONObject, map: Map<String, String> = emptyMap()) = DailyAccountingRecordEntity(
        id = j.getString("id"),
        groupId = j.optString("groupId", ""),
        subGroupId = j.strOrNull("subGroupId"),
        profileId = j.strOrNull("profileId"),
        entryDate = j.optString("entryDate", ""),
        staffId = j.optString("staffId", ""),
        staffName = j.optString("staffName", ""),
        staffRole = j.optString("staffRole", ""),
        dailyRevenue = j.optDouble("dailyRevenue", 0.0),
        incomingAmount = j.optDouble("incomingAmount", 0.0),
        outgoingAmount = j.optDouble("outgoingAmount", 0.0),
        deficitAmount = j.optDouble("deficitAmount", 0.0),
        complaintsCount = j.optInt("complaintsCount", 0),
        complaintsNotes = j.strOrNull("complaintsNotes"),
        dailyCapital = j.optDouble("dailyCapital", 0.0),
        weeklyCapital = j.optDouble("weeklyCapital", 0.0),
        monthlyCapital = j.optDouble("monthlyCapital", 0.0),
        screenshotUri = rewrite(j.strOrNull("screenshotUri"), map),
        notes = j.strOrNull("notes"),
        isEncrypted = j.optBoolean("isEncrypted", true),
        timestamp = j.optLong("timestamp", System.currentTimeMillis())
    )

    fun complaintToJson(c: FinancialComplaintEntity) = JSONObject().apply {
        put("id", c.id)
        put("groupId", c.groupId)
        putOptValue("subGroupId", c.subGroupId)
        putOptValue("profileId", c.profileId)
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
        putOptValue("resolutionNotes", c.resolutionNotes)
        put("timestamp", c.timestamp)
        putOptValue("accusedEmployeeId", c.accusedEmployeeId)
        putOptValue("accusedEmployeeName", c.accusedEmployeeName)
        putOptValue("accusedEmployeePhone", c.accusedEmployeePhone)
        putOptValue("invoiceId", c.invoiceId)
        putOptValue("invoiceNumber", c.invoiceNumber)
        putOptValue("mentionTag", c.mentionTag)
        put("claimAmount", c.claimAmount)
        put("employeeResponseStatus", c.employeeResponseStatus)
        putOptValue("employeeRejectionReason", c.employeeRejectionReason)
        putOptValue("employeeRejectionProofUri", c.employeeRejectionProofUri)
        putOptValue("employeeProofMediaType", c.employeeProofMediaType)
        putOptValue("employeeResponseTime", c.employeeResponseTime)
        put("isDeductedFromBalance", c.isDeductedFromBalance)
        put("managerAlertSent", c.managerAlertSent)
        putOptValue("managerDecision", c.managerDecision)
        put("forensicAnalysisCompleted", c.forensicAnalysisCompleted)
        putOptValue("forensicLiabilityType", c.forensicLiabilityType)
        putOptValue("forensicReceiverStaffId", c.forensicReceiverStaffId)
        putOptValue("forensicReceiverStaffName", c.forensicReceiverStaffName)
        putOptValue("forensicLiabilityDistribution", c.forensicLiabilityDistribution)
        putOptValue("forensicAuditDetails", c.forensicAuditDetails)
        putOptValue("forensicRecommendedAction", c.forensicRecommendedAction)
        put("forensicJointDeductionApplied", c.forensicJointDeductionApplied)
    }

    fun jsonToComplaint(j: JSONObject, map: Map<String, String> = emptyMap()) = FinancialComplaintEntity(
        id = j.getString("id"),
        groupId = j.optString("groupId", ""),
        subGroupId = j.strOrNull("subGroupId"),
        profileId = j.strOrNull("profileId"),
        submittedByStaffId = j.optString("submittedByStaffId", ""),
        submittedByStaffName = j.optString("submittedByStaffName", ""),
        submittedByStaffRole = j.optString("submittedByStaffRole", ""),
        title = j.optString("title", ""),
        details = j.optString("details", ""),
        targetDate = j.optString("targetDate", ""),
        impactType = j.optString("impactType", "DEDUCTION"),
        amount = j.optDouble("amount", 0.0),
        category = j.optString("category", "عجز صندوق"),
        status = j.optString("status", "APPLIED"),
        resultingDayDeficit = j.optDouble("resultingDayDeficit", 0.0),
        adminAlertSent = j.optBoolean("adminAlertSent", true),
        resolutionNotes = j.strOrNull("resolutionNotes"),
        timestamp = j.optLong("timestamp", System.currentTimeMillis()),
        accusedEmployeeId = j.strOrNull("accusedEmployeeId"),
        accusedEmployeeName = j.strOrNull("accusedEmployeeName"),
        accusedEmployeePhone = j.strOrNull("accusedEmployeePhone"),
        invoiceId = j.strOrNull("invoiceId"),
        invoiceNumber = j.strOrNull("invoiceNumber"),
        mentionTag = j.strOrNull("mentionTag"),
        claimAmount = j.optDouble("claimAmount", j.optDouble("amount", 0.0)),
        employeeResponseStatus = j.optString("employeeResponseStatus", "PENDING"),
        employeeRejectionReason = j.strOrNull("employeeRejectionReason"),
        employeeRejectionProofUri = rewrite(j.strOrNull("employeeRejectionProofUri"), map),
        employeeProofMediaType = j.strOrNull("employeeProofMediaType"),
        employeeResponseTime = j.longOrNull("employeeResponseTime"),
        isDeductedFromBalance = j.optBoolean("isDeductedFromBalance", false),
        managerAlertSent = j.optBoolean("managerAlertSent", true),
        managerDecision = j.strOrNull("managerDecision"),
        forensicAnalysisCompleted = j.optBoolean("forensicAnalysisCompleted", false),
        forensicLiabilityType = j.strOrNull("forensicLiabilityType"),
        forensicReceiverStaffId = j.strOrNull("forensicReceiverStaffId"),
        forensicReceiverStaffName = j.strOrNull("forensicReceiverStaffName"),
        forensicLiabilityDistribution = j.strOrNull("forensicLiabilityDistribution"),
        forensicAuditDetails = j.strOrNull("forensicAuditDetails"),
        forensicRecommendedAction = j.strOrNull("forensicRecommendedAction"),
        forensicJointDeductionApplied = j.optBoolean("forensicJointDeductionApplied", false)
    )

    fun projectToJson(p: ProjectEntity) = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("workGroupId", p.workGroupId)
        put("type", p.type)
        put("estimatedCapital", p.estimatedCapital)
        put("totalExpenses", p.totalExpenses)
        put("totalIncome", p.totalIncome)
        put("location", p.location)
        putOptValue("coManagerPhone", p.coManagerPhone)
        putOptValue("coManagerName", p.coManagerName)
        put("membersInfo", p.membersInfo)
        put("createdAt", p.createdAt)
    }

    fun jsonToProject(j: JSONObject) = ProjectEntity(
        id = j.getString("id"),
        name = j.optString("name", ""),
        workGroupId = j.optString("workGroupId", ""),
        type = j.optString("type", ProjectType.CONSTRUCTION.name),
        estimatedCapital = j.optDouble("estimatedCapital", 0.0),
        totalExpenses = j.optDouble("totalExpenses", 0.0),
        totalIncome = j.optDouble("totalIncome", 0.0),
        location = j.optString("location", "القاهرة، مصر"),
        coManagerPhone = j.strOrNull("coManagerPhone"),
        coManagerName = j.strOrNull("coManagerName"),
        membersInfo = j.optString("membersInfo", ""),
        createdAt = j.optLong("createdAt", System.currentTimeMillis())
    )

    fun transactionToJson(t: TransactionEntity) = JSONObject().apply {
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
        putOptValue("traderCostPerGram", t.traderCostPerGram)
        putOptValue("profit", t.profit)
        putOptValue("photoUri", t.photoUri)
        put("timestamp", t.timestamp)
        put("status", t.status)
        putOptValue("originalTransactionId", t.originalTransactionId)
        putOptValue("notes", t.notes)
        put("metalLossRatePercent", t.metalLossRatePercent)
        put("metalLossGrams", t.metalLossGrams)
    }

    fun jsonToTransaction(j: JSONObject, map: Map<String, String> = emptyMap()) = TransactionEntity(
        id = j.getString("id"),
        chatId = j.optString("chatId", ""),
        currentUserId = j.optString("currentUserId", ""),
        currentUserName = j.optString("currentUserName", ""),
        currentUserType = j.optString("currentUserType", UserType.BUSINESSMAN.name),
        contactId = j.optString("contactId", ""),
        contactName = j.optString("contactName", ""),
        goldKarat = j.optString("goldKarat", GoldKarat.KARAT_21.name),
        goldType = j.optString("goldType", "مصاغ"),
        weight = j.optDouble("weight", 0.0),
        pricePerGram = j.optDouble("pricePerGram", 0.0),
        manufacturingCostPerGram = j.optDouble("manufacturingCostPerGram", 0.0),
        totalGoldPrice = j.optDouble("totalGoldPrice", 0.0),
        totalManufacturing = j.optDouble("totalManufacturing", 0.0),
        totalPrice = j.optDouble("totalPrice", 0.0),
        traderCostPerGram = j.doubleOrNull("traderCostPerGram"),
        profit = j.doubleOrNull("profit"),
        photoUri = rewrite(j.strOrNull("photoUri"), map),
        timestamp = j.optLong("timestamp", System.currentTimeMillis()),
        status = j.optString("status", TransactionStatus.PURCHASED.name),
        originalTransactionId = j.strOrNull("originalTransactionId"),
        notes = j.strOrNull("notes"),
        metalLossRatePercent = j.optDouble("metalLossRatePercent", 0.5),
        metalLossGrams = j.optDouble("metalLossGrams", 0.0)
    )

    fun purchaseToJson(p: GoldPurchaseEntity) = JSONObject().apply {
        put("id", p.id)
        put("buyerId", p.buyerId)
        put("traderHashtag", p.traderHashtag)
        put("traderConfirmed", p.traderConfirmed)
        putOptValue("photoUri", p.photoUri)
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
        putOptValue("sellerShopName", p.sellerShopName)
        putOptValue("sellerLocation", p.sellerLocation)
        putOptValue("scalePhotoUri", p.scalePhotoUri)
        put("metalLossRatePercent", p.metalLossRatePercent)
        put("metalLossGrams", p.metalLossGrams)
    }

    fun jsonToPurchase(j: JSONObject, map: Map<String, String> = emptyMap()) = GoldPurchaseEntity(
        id = j.getString("id"),
        buyerId = j.optString("buyerId", ""),
        traderHashtag = j.optString("traderHashtag", ""),
        traderConfirmed = j.optBoolean("traderConfirmed", false),
        photoUri = rewrite(j.strOrNull("photoUri"), map),
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
        sellerShopName = j.strOrNull("sellerShopName"),
        sellerLocation = j.strOrNull("sellerLocation"),
        scalePhotoUri = rewrite(j.strOrNull("scalePhotoUri"), map),
        metalLossRatePercent = j.optDouble("metalLossRatePercent", 0.5),
        metalLossGrams = j.optDouble("metalLossGrams", 0.0)
    )

    fun traderRequestToJson(r: TraderRequestEntity) = JSONObject().apply {
        put("id", r.id)
        put("userId", r.userId)
        put("name", r.name)
        put("phone", r.phone)
        put("username", r.username)
        put("shopName", r.shopName)
        put("shopAddress", r.shopAddress)
        put("license", r.license)
        put("status", r.status)
        put("submittedAt", r.submittedAt)
        putOptValue("reviewedAt", r.reviewedAt)
        putOptValue("adminNotes", r.adminNotes)
    }

    fun jsonToTraderRequest(j: JSONObject) = TraderRequestEntity(
        id = j.getString("id"),
        userId = j.optString("userId", ""),
        name = j.optString("name", ""),
        phone = j.optString("phone", ""),
        username = j.optString("username", ""),
        shopName = j.optString("shopName", ""),
        shopAddress = j.optString("shopAddress", ""),
        license = j.optString("license", ""),
        status = j.optString("status", TraderStatus.PENDING.name),
        submittedAt = j.optLong("submittedAt", System.currentTimeMillis()),
        reviewedAt = j.longOrNull("reviewedAt"),
        adminNotes = j.strOrNull("adminNotes")
    )

    fun adminConfigToJson(a: AdminConfigEntity) = JSONObject().apply {
        put("id", a.id)
        put("appName", a.appName)
        putOptValue("appLogoUri", a.appLogoUri)
        put("adminPin", a.adminPin)
        putOptValue("geminiApiKey", a.geminiApiKey)
        put("geminiModelName", a.geminiModelName)
        put("aiSystemPrompt", a.aiSystemPrompt)
        put("voiceSelection", a.voiceSelection)
        put("personaPrompt", a.personaPrompt)
        put("biometricAuthEnabled", a.biometricAuthEnabled)
        putOptValue("notificationApiUrl", a.notificationApiUrl)
        putOptValue("notificationApiKey", a.notificationApiKey)
        putOptValue("newsApiUrl", a.newsApiUrl)
        putOptValue("goldPriceApiUrl", a.goldPriceApiUrl)
        putOptValue("goldPriceApiKey", a.goldPriceApiKey)
        put("goldPriceAutoRefreshIntervalSec", a.goldPriceAutoRefreshIntervalSec)
        put("encryptionEnabled", a.encryptionEnabled)
        putOptValue("lastBackupAt", a.lastBackupAt)
        put("isAppLockEnabled", a.isAppLockEnabled)
        put("appLockPin", a.appLockPin)
        put("isBiometricEnabled", a.isBiometricEnabled)
    }

    fun jsonToAdminConfig(j: JSONObject, map: Map<String, String> = emptyMap()) = AdminConfigEntity(
        id = j.optInt("id", 1),
        appName = j.optString("appName", "الصاغة - تداول الذهب والفضة"),
        appLogoUri = rewrite(j.strOrNull("appLogoUri"), map),
        adminPin = j.optString("adminPin", ""),
        geminiApiKey = j.strOrNull("geminiApiKey"),
        geminiModelName = j.optString("geminiModelName", "gemini-3.5-flash"),
        aiSystemPrompt = j.optString("aiSystemPrompt", "أنت المحاسب الذكي لمنصة أوزوكس."),
        voiceSelection = j.optString("voiceSelection", "ar-EG-Standard-A"),
        personaPrompt = j.optString("personaPrompt", "وكيل صوتي ذكي لإدارة الأعمال"),
        biometricAuthEnabled = j.optBoolean("biometricAuthEnabled", true),
        notificationApiUrl = j.strOrNull("notificationApiUrl"),
        notificationApiKey = j.strOrNull("notificationApiKey"),
        newsApiUrl = j.strOrNull("newsApiUrl"),
        goldPriceApiUrl = j.strOrNull("goldPriceApiUrl"),
        goldPriceApiKey = j.strOrNull("goldPriceApiKey"),
        goldPriceAutoRefreshIntervalSec = j.optInt("goldPriceAutoRefreshIntervalSec", 15),
        encryptionEnabled = j.optBoolean("encryptionEnabled", true),
        lastBackupAt = j.longOrNull("lastBackupAt"),
        isAppLockEnabled = j.optBoolean("isAppLockEnabled", false),
        appLockPin = j.optString("appLockPin", ""),
        isBiometricEnabled = j.optBoolean("isBiometricEnabled", true)
    )

    fun priceAlertToJson(p: PriceAlertEntity) = JSONObject().apply {
        put("id", p.id)
        put("metalType", p.metalType)
        put("karat", p.karat)
        put("targetPrice", p.targetPrice)
        put("direction", p.direction)
        put("isActive", p.isActive)
        put("isTriggered", p.isTriggered)
        putOptValue("note", p.note)
        put("createdAt", p.createdAt)
    }

    fun jsonToPriceAlert(j: JSONObject) = PriceAlertEntity(
        id = j.getString("id"),
        metalType = j.optString("metalType", MetalType.GOLD.name),
        karat = j.optString("karat", GoldKarat.KARAT_21.name),
        targetPrice = j.optDouble("targetPrice", 0.0),
        direction = j.optString("direction", "ABOVE"),
        isActive = j.optBoolean("isActive", true),
        isTriggered = j.optBoolean("isTriggered", false),
        note = j.strOrNull("note"),
        createdAt = j.optLong("createdAt", System.currentTimeMillis())
    )

    fun projectMemberToJson(m: ProjectMemberEntity) = JSONObject().apply {
        put("id", m.id)
        put("projectId", m.projectId)
        put("userId", m.userId)
        put("userName", m.userName)
        put("userPhone", m.userPhone)
        put("role", m.role)
        putOptValue("assignedTask", m.assignedTask)
        put("addedAt", m.addedAt)
    }

    fun jsonToProjectMember(j: JSONObject) = ProjectMemberEntity(
        id = j.getString("id"),
        projectId = j.getString("projectId"),
        userId = j.optString("userId", ""),
        userName = j.optString("userName", ""),
        userPhone = j.optString("userPhone", ""),
        role = j.optString("role", ProjectMemberRole.EXECUTIVE_MANAGER.name),
        assignedTask = j.strOrNull("assignedTask"),
        addedAt = j.optLong("addedAt", System.currentTimeMillis())
    )

    fun projectAuditLogToJson(l: ProjectAuditLogEntity) = JSONObject().apply {
        put("id", l.id)
        put("projectId", l.projectId)
        put("action", l.action)
        put("actorName", l.actorName)
        put("actorPhone", l.actorPhone)
        putOptValue("details", l.details)
        put("timestamp", l.timestamp)
    }

    fun jsonToProjectAuditLog(j: JSONObject) = ProjectAuditLogEntity(
        id = j.getString("id"),
        projectId = j.getString("projectId"),
        action = j.optString("action", ""),
        actorName = j.optString("actorName", ""),
        actorPhone = j.optString("actorPhone", ""),
        details = j.strOrNull("details"),
        timestamp = j.optLong("timestamp", System.currentTimeMillis())
    )

    fun ledgerEntryToJson(l: LedgerEntryEntity) = JSONObject().apply {
        put("id", l.id)
        putOptValue("txId", l.txId)
        putOptValue("purchaseId", l.purchaseId)
        put("accountOwnerId", l.accountOwnerId)
        put("counterPartyId", l.counterPartyId)
        put("counterPartyName", l.counterPartyName)
        put("assetType", l.assetType)
        put("entryType", l.entryType)
        put("weightGrams", l.weightGrams)
        put("cashAmount", l.cashAmount)
        put("metalLossRate", l.metalLossRate)
        put("checksumHash", l.checksumHash)
        put("isReversal", l.isReversal)
        putOptValue("originalLedgerId", l.originalLedgerId)
        put("timestamp", l.timestamp)
        putOptValue("notes", l.notes)
    }

    fun jsonToLedgerEntry(j: JSONObject) = LedgerEntryEntity(
        id = j.getString("id"),
        txId = j.strOrNull("txId"),
        purchaseId = j.strOrNull("purchaseId"),
        accountOwnerId = j.optString("accountOwnerId", ""),
        counterPartyId = j.optString("counterPartyId", ""),
        counterPartyName = j.optString("counterPartyName", ""),
        assetType = j.optString("assetType", LedgerAssetType.GOLD_24K.name),
        entryType = j.optString("entryType", LedgerEntryType.DEBIT.name),
        weightGrams = j.optDouble("weightGrams", 0.0),
        cashAmount = j.optDouble("cashAmount", 0.0),
        metalLossRate = j.optDouble("metalLossRate", 0.0),
        checksumHash = j.optString("checksumHash", ""),
        isReversal = j.optBoolean("isReversal", false),
        originalLedgerId = j.strOrNull("originalLedgerId"),
        timestamp = j.optLong("timestamp", System.currentTimeMillis()),
        notes = j.strOrNull("notes")
    )

    fun settlementToJson(s: ClearingSettlementEntity) = JSONObject().apply {
        put("id", s.id)
        put("traderAId", s.traderAId)
        put("traderAName", s.traderAName)
        put("traderBId", s.traderBId)
        put("traderBName", s.traderBName)
        put("netGoldWeight24k", s.netGoldWeight24k)
        put("netSilverWeight999", s.netSilverWeight999)
        put("netCashAmount", s.netCashAmount)
        put("metalLossDeduction", s.metalLossDeduction)
        put("status", s.status)
        put("timestamp", s.timestamp)
        putOptValue("notes", s.notes)
    }

    fun jsonToSettlement(j: JSONObject) = ClearingSettlementEntity(
        id = j.getString("id"),
        traderAId = j.optString("traderAId", ""),
        traderAName = j.optString("traderAName", ""),
        traderBId = j.optString("traderBId", ""),
        traderBName = j.optString("traderBName", ""),
        netGoldWeight24k = j.optDouble("netGoldWeight24k", 0.0),
        netSilverWeight999 = j.optDouble("netSilverWeight999", 0.0),
        netCashAmount = j.optDouble("netCashAmount", 0.0),
        metalLossDeduction = j.optDouble("metalLossDeduction", 0.0),
        status = j.optString("status", "SETTLED"),
        timestamp = j.optLong("timestamp", System.currentTimeMillis()),
        notes = j.strOrNull("notes")
    )

    fun syncQueueToJson(s: SyncQueueEntity) = JSONObject().apply {
        put("id", s.id)
        put("actionType", s.actionType)
        put("payloadJson", s.payloadJson)
        put("timestamp", s.timestamp)
        put("status", s.status)
        put("retryCount", s.retryCount)
        putOptValue("errorMessage", s.errorMessage)
    }

    fun jsonToSyncQueue(j: JSONObject) = SyncQueueEntity(
        id = j.getString("id"),
        actionType = j.optString("actionType", ""),
        payloadJson = j.optString("payloadJson", "{}"),
        timestamp = j.optLong("timestamp", System.currentTimeMillis()),
        status = j.optString("status", "PENDING"),
        retryCount = j.optInt("retryCount", 0),
        errorMessage = j.strOrNull("errorMessage")
    )

    fun subscriptionPlanToJson(p: SubscriptionPlanEntity) = JSONObject().apply {
        put("id", p.id)
        put("title", p.title)
        put("description", p.description)
        put("monthlyPrice", p.monthlyPrice)
        put("annualPrice", p.annualPrice)
        put("discountPercent", p.discountPercent)
        putOptValue("offerTitle", p.offerTitle)
        put("isOfferActive", p.isOfferActive)
        put("featuresListText", p.featuresListText)
        putOptValue("badgeText", p.badgeText)
        put("isPopular", p.isPopular)
        put("isEnabled", p.isEnabled)
    }

    fun jsonToSubscriptionPlan(j: JSONObject) = SubscriptionPlanEntity(
        id = j.getString("id"),
        title = j.optString("title", ""),
        description = j.optString("description", ""),
        monthlyPrice = j.optDouble("monthlyPrice", 0.0),
        annualPrice = j.optDouble("annualPrice", 0.0),
        discountPercent = j.optInt("discountPercent", 0),
        offerTitle = j.strOrNull("offerTitle"),
        isOfferActive = j.optBoolean("isOfferActive", false),
        featuresListText = j.optString("featuresListText", ""),
        badgeText = j.strOrNull("badgeText"),
        isPopular = j.optBoolean("isPopular", false),
        isEnabled = j.optBoolean("isEnabled", true)
    )

    fun subscriptionRequestToJson(r: SubscriptionRequestEntity) = JSONObject().apply {
        put("id", r.id)
        put("userId", r.userId)
        put("userName", r.userName)
        put("userPhone", r.userPhone)
        put("requestedPlanId", r.requestedPlanId)
        put("requestedPlanTitle", r.requestedPlanTitle)
        put("pricePaid", r.pricePaid)
        put("paymentMethod", r.paymentMethod)
        putOptValue("receiptPhotoUri", r.receiptPhotoUri)
        put("status", r.status)
        put("requestedAt", r.requestedAt)
        putOptValue("reviewedAt", r.reviewedAt)
        putOptValue("adminNotes", r.adminNotes)
    }

    fun jsonToSubscriptionRequest(j: JSONObject, map: Map<String, String> = emptyMap()) = SubscriptionRequestEntity(
        id = j.getString("id"),
        userId = j.optString("userId", ""),
        userName = j.optString("userName", ""),
        userPhone = j.optString("userPhone", ""),
        requestedPlanId = j.optString("requestedPlanId", ""),
        requestedPlanTitle = j.optString("requestedPlanTitle", ""),
        pricePaid = j.optDouble("pricePaid", 0.0),
        paymentMethod = j.optString("paymentMethod", "InstaPay"),
        receiptPhotoUri = rewrite(j.strOrNull("receiptPhotoUri"), map),
        status = j.optString("status", "PENDING"),
        requestedAt = j.optLong("requestedAt", System.currentTimeMillis()),
        reviewedAt = j.longOrNull("reviewedAt"),
        adminNotes = j.strOrNull("adminNotes")
    )

    fun promoAnnouncementToJson(a: PromoAnnouncementEntity) = JSONObject().apply {
        put("id", a.id)
        put("title", a.title)
        put("content", a.content)
        putOptValue("promoCode", a.promoCode)
        put("discountPercent", a.discountPercent)
        putOptValue("targetPlanId", a.targetPlanId)
        put("isActive", a.isActive)
        put("createdAt", a.createdAt)
    }

    fun jsonToPromoAnnouncement(j: JSONObject) = PromoAnnouncementEntity(
        id = j.getString("id"),
        title = j.optString("title", ""),
        content = j.optString("content", ""),
        promoCode = j.strOrNull("promoCode"),
        discountPercent = j.optInt("discountPercent", 0),
        targetPlanId = j.strOrNull("targetPlanId"),
        isActive = j.optBoolean("isActive", true),
        createdAt = j.optLong("createdAt", System.currentTimeMillis())
    )

    fun merchantReviewToJson(r: MerchantReviewEntity) = JSONObject().apply {
        put("id", r.id)
        put("merchantId", r.merchantId)
        put("reviewerId", r.reviewerId)
        put("reviewerName", r.reviewerName)
        putOptValue("reviewerPhone", r.reviewerPhone)
        putOptValue("reviewerAvatarUri", r.reviewerAvatarUri)
        put("rating", r.rating.toDouble())
        put("ratingAspectAccuracy", r.ratingAspectAccuracy.toDouble())
        put("ratingAspectSpeed", r.ratingAspectSpeed.toDouble())
        put("ratingAspectHonesty", r.ratingAspectHonesty.toDouble())
        put("comment", r.comment)
        put("isVerifiedBuyer", r.isVerifiedBuyer)
        put("timestamp", r.timestamp)
    }

    fun jsonToMerchantReview(j: JSONObject, map: Map<String, String> = emptyMap()) = MerchantReviewEntity(
        id = j.getString("id"),
        merchantId = j.optString("merchantId", ""),
        reviewerId = j.optString("reviewerId", ""),
        reviewerName = j.optString("reviewerName", ""),
        reviewerPhone = j.strOrNull("reviewerPhone"),
        reviewerAvatarUri = rewrite(j.strOrNull("reviewerAvatarUri"), map),
        rating = j.optDouble("rating", 5.0).toFloat(),
        ratingAspectAccuracy = j.optDouble("ratingAspectAccuracy", 5.0).toFloat(),
        ratingAspectSpeed = j.optDouble("ratingAspectSpeed", 5.0).toFloat(),
        ratingAspectHonesty = j.optDouble("ratingAspectHonesty", 5.0).toFloat(),
        comment = j.optString("comment", ""),
        isVerifiedBuyer = j.optBoolean("isVerifiedBuyer", true),
        timestamp = j.optLong("timestamp", System.currentTimeMillis())
    )

    fun productToJson(p: MerchantProductEntity) = JSONObject().apply {
        put("id", p.id)
        put("merchantId", p.merchantId)
        put("merchantShopName", p.merchantShopName)
        putOptValue("merchantPhone", p.merchantPhone)
        put("title", p.title)
        put("category", p.category)
        put("karat", p.karat)
        put("weightGrams", p.weightGrams)
        put("manufacturingCostPerGram", p.manufacturingCostPerGram)
        put("consumerPriceEGP", p.consumerPriceEGP)
        put("wholesalePriceEGP", p.wholesalePriceEGP)
        put("discountRatePercent", p.discountRatePercent)
        put("quantityInStock", p.quantityInStock)
        put("priceApprovalStatus", p.priceApprovalStatus)
        putOptValue("addedByStaffName", p.addedByStaffName)
        putOptValue("photoUri", p.photoUri)
        putOptValue("description", p.description)
        putOptValue("extractedOcrText", p.extractedOcrText)
        putOptValue("extractedBarcode", p.extractedBarcode)
        putOptValue("extractedKeywords", p.extractedKeywords)
        put("isPanoramicScanned", p.isPanoramicScanned)
        putOptValue("panoramicScanUri", p.panoramicScanUri)
        put("isAvailable", p.isAvailable)
        put("viewsCount", p.viewsCount)
        put("createdAt", p.createdAt)
    }

    fun jsonToProduct(j: JSONObject, map: Map<String, String> = emptyMap()) = MerchantProductEntity(
        id = j.getString("id"),
        merchantId = j.optString("merchantId", ""),
        merchantShopName = j.optString("merchantShopName", ""),
        merchantPhone = j.strOrNull("merchantPhone"),
        title = j.optString("title", ""),
        category = j.optString("category", MerchantProductCategory.BULLION.name),
        karat = j.optString("karat", GoldKarat.KARAT_24.name),
        weightGrams = j.optDouble("weightGrams", 0.0),
        manufacturingCostPerGram = j.optDouble("manufacturingCostPerGram", 0.0),
        consumerPriceEGP = j.optDouble("consumerPriceEGP", 0.0),
        wholesalePriceEGP = j.optDouble("wholesalePriceEGP", 0.0),
        discountRatePercent = j.optDouble("discountRatePercent", 0.0),
        quantityInStock = j.optDouble("quantityInStock", 1.0),
        priceApprovalStatus = j.optString("priceApprovalStatus", "APPROVED"),
        addedByStaffName = j.strOrNull("addedByStaffName"),
        photoUri = rewrite(j.strOrNull("photoUri"), map),
        description = j.strOrNull("description"),
        extractedOcrText = j.strOrNull("extractedOcrText"),
        extractedBarcode = j.strOrNull("extractedBarcode"),
        extractedKeywords = j.strOrNull("extractedKeywords"),
        isPanoramicScanned = j.optBoolean("isPanoramicScanned", false),
        panoramicScanUri = rewrite(j.strOrNull("panoramicScanUri"), map),
        isAvailable = j.optBoolean("isAvailable", true),
        viewsCount = j.optInt("viewsCount", 0),
        createdAt = j.optLong("createdAt", System.currentTimeMillis())
    )

    fun merchantSaleToJson(s: MerchantDailySaleEntity) = JSONObject().apply {
        put("id", s.id)
        put("merchantId", s.merchantId)
        putOptValue("productId", s.productId)
        put("productTitle", s.productTitle)
        put("weightGrams", s.weightGrams)
        put("karat", s.karat)
        put("pricePerGram", s.pricePerGram)
        put("mfgCostPerGram", s.mfgCostPerGram)
        put("totalEGP", s.totalEGP)
        put("profitEGP", s.profitEGP)
        put("customerName", s.customerName)
        putOptValue("customerPhone", s.customerPhone)
        put("saleTimestamp", s.saleTimestamp)
        put("isClosedInDailyReport", s.isClosedInDailyReport)
    }

    fun jsonToMerchantSale(j: JSONObject) = MerchantDailySaleEntity(
        id = j.getString("id"),
        merchantId = j.optString("merchantId", ""),
        productId = j.strOrNull("productId"),
        productTitle = j.optString("productTitle", ""),
        weightGrams = j.optDouble("weightGrams", 0.0),
        karat = j.optString("karat", GoldKarat.KARAT_21.name),
        pricePerGram = j.optDouble("pricePerGram", 0.0),
        mfgCostPerGram = j.optDouble("mfgCostPerGram", 0.0),
        totalEGP = j.optDouble("totalEGP", 0.0),
        profitEGP = j.optDouble("profitEGP", 0.0),
        customerName = j.optString("customerName", ""),
        customerPhone = j.strOrNull("customerPhone"),
        saleTimestamp = j.optLong("saleTimestamp", System.currentTimeMillis()),
        isClosedInDailyReport = j.optBoolean("isClosedInDailyReport", false)
    )

    fun closingReportToJson(r: MerchantDailyClosingReportEntity) = JSONObject().apply {
        put("id", r.id)
        put("merchantId", r.merchantId)
        put("reportDateText", r.reportDateText)
        put("totalSalesCount", r.totalSalesCount)
        put("totalWeightSoldGrams", r.totalWeightSoldGrams)
        put("totalRevenueEGP", r.totalRevenueEGP)
        put("totalProfitEGP", r.totalProfitEGP)
        put("remainingInventoryWeightGrams", r.remainingInventoryWeightGrams)
        put("reportTimestamp", r.reportTimestamp)
    }

    fun jsonToClosingReport(j: JSONObject) = MerchantDailyClosingReportEntity(
        id = j.getString("id"),
        merchantId = j.optString("merchantId", ""),
        reportDateText = j.optString("reportDateText", ""),
        totalSalesCount = j.optInt("totalSalesCount", 0),
        totalWeightSoldGrams = j.optDouble("totalWeightSoldGrams", 0.0),
        totalRevenueEGP = j.optDouble("totalRevenueEGP", 0.0),
        totalProfitEGP = j.optDouble("totalProfitEGP", 0.0),
        remainingInventoryWeightGrams = j.optDouble("remainingInventoryWeightGrams", 0.0),
        reportTimestamp = j.optLong("reportTimestamp", System.currentTimeMillis())
    )

    fun invoiceToJson(i: InvoiceEntity) = JSONObject().apply {
        put("id", i.id)
        put("invoiceNumber", i.invoiceNumber)
        put("title", i.title)
        put("category", i.category)
        put("senderId", i.senderId)
        put("senderName", i.senderName)
        put("receiverId", i.receiverId)
        put("receiverName", i.receiverName)
        put("totalAmount", i.totalAmount)
        putOptValue("goldWeightGrams", i.goldWeightGrams)
        putOptValue("goldKarat", i.goldKarat)
        putOptValue("pricePerGram", i.pricePerGram)
        putOptValue("manufacturingCost", i.manufacturingCost)
        put("paymentMethod", i.paymentMethod)
        putOptValue("cashierName", i.cashierName)
        put("discountAmountEGP", i.discountAmountEGP)
        put("status", i.status)
        put("issueDate", i.issueDate)
        putOptValue("dueDate", i.dueDate)
        putOptValue("notes", i.notes)
        putOptValue("projectId", i.projectId)
        putOptValue("projectName", i.projectName)
        put("isDeliveredToOozoxUser", i.isDeliveredToOozoxUser)
        putOptValue("dispatchedViaSocialChannel", i.dispatchedViaSocialChannel)
        put("supplierPayoutAmount", i.supplierPayoutAmount)
        putOptValue("supplierPayoutMethod", i.supplierPayoutMethod)
        putOptValue("paymentMethodDetails", i.paymentMethodDetails)
        putOptValue("paymentDemandNotice", i.paymentDemandNotice)
        put("isSynced", i.isSynced)
        put("createdAt", i.createdAt)
        put("updatedAt", i.updatedAt)
        putOptValue("hashtagId", i.hashtagId)
        putOptValue("hashtagTag", i.hashtagTag)
        putOptValue("chatId", i.chatId)
        put("paidAmount", i.paidAmount)
        put("isExternalBusiness", i.isExternalBusiness)
        put("sourceType", i.sourceType)
    }

    fun jsonToInvoice(j: JSONObject) = InvoiceEntity(
        id = j.getString("id"),
        invoiceNumber = j.optString("invoiceNumber", ""),
        title = j.optString("title", ""),
        category = j.optString("category", InvoiceCategory.GENERAL.name),
        senderId = j.optString("senderId", ""),
        senderName = j.optString("senderName", ""),
        receiverId = j.optString("receiverId", ""),
        receiverName = j.optString("receiverName", ""),
        totalAmount = j.optDouble("totalAmount", 0.0),
        goldWeightGrams = j.doubleOrNull("goldWeightGrams"),
        goldKarat = j.strOrNull("goldKarat"),
        pricePerGram = j.doubleOrNull("pricePerGram"),
        manufacturingCost = j.doubleOrNull("manufacturingCost"),
        paymentMethod = j.optString("paymentMethod", "كاش"),
        cashierName = j.strOrNull("cashierName"),
        discountAmountEGP = j.optDouble("discountAmountEGP", 0.0),
        status = j.optString("status", InvoiceStatus.PENDING.name),
        issueDate = j.optLong("issueDate", System.currentTimeMillis()),
        dueDate = j.longOrNull("dueDate"),
        notes = j.strOrNull("notes"),
        projectId = j.strOrNull("projectId"),
        projectName = j.strOrNull("projectName"),
        isDeliveredToOozoxUser = j.optBoolean("isDeliveredToOozoxUser", false),
        dispatchedViaSocialChannel = j.strOrNull("dispatchedViaSocialChannel"),
        supplierPayoutAmount = j.optDouble("supplierPayoutAmount", 0.0),
        supplierPayoutMethod = j.strOrNull("supplierPayoutMethod"),
        paymentMethodDetails = j.strOrNull("paymentMethodDetails"),
        paymentDemandNotice = j.strOrNull("paymentDemandNotice"),
        isSynced = j.optBoolean("isSynced", true),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = j.optLong("updatedAt", System.currentTimeMillis()),
        hashtagId = j.strOrNull("hashtagId"),
        hashtagTag = j.strOrNull("hashtagTag"),
        chatId = j.strOrNull("chatId"),
        paidAmount = j.optDouble("paidAmount", 0.0),
        isExternalBusiness = j.optBoolean("isExternalBusiness", false),
        sourceType = j.optString("sourceType", "DIRECT")
    )

    fun projectTaskToJson(t: ProjectTaskEntity) = JSONObject().apply {
        put("id", t.id)
        put("projectId", t.projectId)
        put("projectName", t.projectName)
        put("title", t.title)
        putOptValue("description", t.description)
        putOptValue("assignedToUserId", t.assignedToUserId)
        putOptValue("assignedToName", t.assignedToName)
        putOptValue("assignedToPhone", t.assignedToPhone)
        put("priority", t.priority)
        put("status", t.status)
        putOptValue("dueDate", t.dueDate)
        put("costEstimate", t.costEstimate)
        put("isSynced", t.isSynced)
        put("createdAt", t.createdAt)
        put("updatedAt", t.updatedAt)
        putOptValue("workGroupId", t.workGroupId)
        putOptValue("mediaAttachmentUri", t.mediaAttachmentUri)
        put("mediaAttachmentType", t.mediaAttachmentType)
        put("assignedMembersList", t.assignedMembersList)
        put("assignedMemberIds", t.assignedMemberIds)
        put("deadlineDurationText", t.deadlineDurationText)
        put("deadlineTimestamp", t.deadlineTimestamp)
        put("urgencyAlertLevel", t.urgencyAlertLevel)
        putOptValue("workerResponseUri", t.workerResponseUri)
        putOptValue("workerResponseType", t.workerResponseType)
        putOptValue("workerResponseText", t.workerResponseText)
        putOptValue("workerResponseSubmittedAt", t.workerResponseSubmittedAt)
        putOptValue("workerResponseByUserId", t.workerResponseByUserId)
        putOptValue("workerResponseByUserName", t.workerResponseByUserName)
        put("isSubmittedForReview", t.isSubmittedForReview)
        put("isArchived", t.isArchived)
        putOptValue("adminConfirmedAt", t.adminConfirmedAt)
        put("financialAmount", t.financialAmount)
        putOptValue("financialNotes", t.financialNotes)
        put("timingMode", t.timingMode)
        put("durationValue", t.durationValue)
    }

    fun jsonToProjectTask(j: JSONObject, map: Map<String, String> = emptyMap()) = ProjectTaskEntity(
        id = j.getString("id"),
        projectId = j.optString("projectId", ""),
        projectName = j.optString("projectName", ""),
        title = j.optString("title", ""),
        description = j.strOrNull("description"),
        assignedToUserId = j.strOrNull("assignedToUserId"),
        assignedToName = j.strOrNull("assignedToName"),
        assignedToPhone = j.strOrNull("assignedToPhone"),
        priority = j.optString("priority", TaskPriority.MEDIUM.name),
        status = j.optString("status", TaskStatus.PENDING.name),
        dueDate = j.strOrNull("dueDate"),
        costEstimate = j.optDouble("costEstimate", 0.0),
        isSynced = j.optBoolean("isSynced", true),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = j.optLong("updatedAt", System.currentTimeMillis()),
        workGroupId = j.strOrNull("workGroupId"),
        mediaAttachmentUri = rewrite(j.strOrNull("mediaAttachmentUri"), map),
        mediaAttachmentType = j.optString("mediaAttachmentType", "PHOTO"),
        assignedMembersList = j.optString("assignedMembersList", ""),
        assignedMemberIds = j.optString("assignedMemberIds", ""),
        deadlineDurationText = j.optString("deadlineDurationText", "بعد 3 ساعات"),
        deadlineTimestamp = j.optLong("deadlineTimestamp", 0L),
        urgencyAlertLevel = j.optString("urgencyAlertLevel", "NORMAL"),
        workerResponseUri = rewrite(j.strOrNull("workerResponseUri"), map),
        workerResponseType = j.strOrNull("workerResponseType"),
        workerResponseText = j.strOrNull("workerResponseText"),
        workerResponseSubmittedAt = j.longOrNull("workerResponseSubmittedAt"),
        workerResponseByUserId = j.strOrNull("workerResponseByUserId"),
        workerResponseByUserName = j.strOrNull("workerResponseByUserName"),
        isSubmittedForReview = j.optBoolean("isSubmittedForReview", false),
        isArchived = j.optBoolean("isArchived", false),
        adminConfirmedAt = j.longOrNull("adminConfirmedAt"),
        financialAmount = j.optDouble("financialAmount", 0.0),
        financialNotes = j.strOrNull("financialNotes"),
        timingMode = j.optString("timingMode", "HOURS"),
        durationValue = j.optInt("durationValue", 3)
    )

    fun shiftHandoverToJson(s: ShiftHandoverEntity) = JSONObject().apply {
        put("id", s.id)
        put("cashierId", s.cashierId)
        put("cashierName", s.cashierName)
        put("loginTime", s.loginTime)
        put("logoutTime", s.logoutTime)
        put("totalInvoicesCount", s.totalInvoicesCount)
        put("operationsCount", s.operationsCount)
        put("totalRevenueEGP", s.totalRevenueEGP)
        put("totalPaidEGP", s.totalPaidEGP)
        put("instaPayCollected", s.instaPayCollected)
        put("vodafoneCashCollected", s.vodafoneCashCollected)
        put("cashCollected", s.cashCollected)
        put("bankCollected", s.bankCollected)
        put("supplierPayoutsDuringShift", s.supplierPayoutsDuringShift)
        put("netTreasuryCash", s.netTreasuryCash)
        put("totalIncomingTransactions", s.totalIncomingTransactions)
        put("totalOutgoingTransactions", s.totalOutgoingTransactions)
        put("productsAddedCount", s.productsAddedCount)
        put("productsSoldCount", s.productsSoldCount)
        putOptValue("notes", s.notes)
        put("expectedCash", s.expectedCash)
        put("actualCash", s.actualCash)
        put("deficitAmount", s.deficitAmount)
        put("surplusAmount", s.surplusAmount)
        put("hasDeficit", s.hasDeficit)
        put("isFlaggedToOwner", s.isFlaggedToOwner)
        put("managerAlertSent", s.managerAlertSent)
        putOptValue("cashierPhone", s.cashierPhone)
        put("shiftDateText", s.shiftDateText)
        putOptValue("receivedByCashierId", s.receivedByCashierId)
        putOptValue("receivedByCashierName", s.receivedByCashierName)
        put("isReceived", s.isReceived)
        putOptValue("receivingNotes", s.receivingNotes)
        putOptValue("receivedAt", s.receivedAt)
        put("retroactiveBalance", s.retroactiveBalance)
        put("merchandiseStatus", s.merchandiseStatus)
        put("physicalStockChecked", s.physicalStockChecked)
        put("cashDrawerConfirmed", s.cashDrawerConfirmed)
        put("invoicesAuditedCount", s.invoicesAuditedCount)
        putOptValue("receiverSignature", s.receiverSignature)
        put("acceptanceType", s.acceptanceType)
        putOptValue("goodsDiscrepancyNotes", s.goodsDiscrepancyNotes)
    }

    fun jsonToShiftHandover(j: JSONObject) = ShiftHandoverEntity(
        id = j.getString("id"),
        cashierId = j.optString("cashierId", ""),
        cashierName = j.optString("cashierName", ""),
        loginTime = j.optLong("loginTime", System.currentTimeMillis()),
        logoutTime = j.optLong("logoutTime", System.currentTimeMillis()),
        totalInvoicesCount = j.optInt("totalInvoicesCount", 0),
        operationsCount = j.optInt("operationsCount", 0),
        totalRevenueEGP = j.optDouble("totalRevenueEGP", 0.0),
        totalPaidEGP = j.optDouble("totalPaidEGP", 0.0),
        instaPayCollected = j.optDouble("instaPayCollected", 0.0),
        vodafoneCashCollected = j.optDouble("vodafoneCashCollected", 0.0),
        cashCollected = j.optDouble("cashCollected", 0.0),
        bankCollected = j.optDouble("bankCollected", 0.0),
        supplierPayoutsDuringShift = j.optDouble("supplierPayoutsDuringShift", 0.0),
        netTreasuryCash = j.optDouble("netTreasuryCash", 0.0),
        totalIncomingTransactions = j.optDouble("totalIncomingTransactions", 0.0),
        totalOutgoingTransactions = j.optDouble("totalOutgoingTransactions", 0.0),
        productsAddedCount = j.optInt("productsAddedCount", 0),
        productsSoldCount = j.optInt("productsSoldCount", 0),
        notes = j.strOrNull("notes"),
        expectedCash = j.optDouble("expectedCash", 0.0),
        actualCash = j.optDouble("actualCash", 0.0),
        deficitAmount = j.optDouble("deficitAmount", 0.0),
        surplusAmount = j.optDouble("surplusAmount", 0.0),
        hasDeficit = j.optBoolean("hasDeficit", false),
        isFlaggedToOwner = j.optBoolean("isFlaggedToOwner", false),
        managerAlertSent = j.optBoolean("managerAlertSent", false),
        cashierPhone = j.strOrNull("cashierPhone"),
        shiftDateText = j.optString("shiftDateText", ""),
        receivedByCashierId = j.strOrNull("receivedByCashierId"),
        receivedByCashierName = j.strOrNull("receivedByCashierName"),
        isReceived = j.optBoolean("isReceived", false),
        receivingNotes = j.strOrNull("receivingNotes"),
        receivedAt = j.longOrNull("receivedAt"),
        retroactiveBalance = j.optDouble("retroactiveBalance", 0.0),
        merchandiseStatus = j.optString("merchandiseStatus", "مطابقة البضاعة والمخزون بالكامل"),
        physicalStockChecked = j.optBoolean("physicalStockChecked", true),
        cashDrawerConfirmed = j.optBoolean("cashDrawerConfirmed", true),
        invoicesAuditedCount = j.optInt("invoicesAuditedCount", 0),
        receiverSignature = j.strOrNull("receiverSignature"),
        acceptanceType = j.optString("acceptanceType", "PENDING"),
        goodsDiscrepancyNotes = j.strOrNull("goodsDiscrepancyNotes")
    )

    fun excelTemplateToJson(t: DynamicExcelTemplateEntity) = JSONObject().apply {
        put("id", t.id)
        putOptValue("projectId", t.projectId)
        put("activityType", t.activityType)
        put("templateName", t.templateName)
        putOptValue("description", t.description)
        put("columnsJson", t.columnsJson)
        put("defaultRowsJson", t.defaultRowsJson)
        put("summaryKpisJson", t.summaryKpisJson)
        put("isUserCustomCreated", t.isUserCustomCreated)
        putOptValue("createdByUserId", t.createdByUserId)
        put("createdAt", t.createdAt)
        put("updatedAt", t.updatedAt)
    }

    fun jsonToExcelTemplate(j: JSONObject) = DynamicExcelTemplateEntity(
        id = j.getString("id"),
        projectId = j.strOrNull("projectId"),
        activityType = j.optString("activityType", "CUSTOM"),
        templateName = j.optString("templateName", ""),
        description = j.strOrNull("description"),
        columnsJson = j.optString("columnsJson", "[]"),
        defaultRowsJson = j.optString("defaultRowsJson", "[]"),
        summaryKpisJson = j.optString("summaryKpisJson", "[]"),
        isUserCustomCreated = j.optBoolean("isUserCustomCreated", false),
        createdByUserId = j.strOrNull("createdByUserId"),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = j.optLong("updatedAt", System.currentTimeMillis())
    )

    fun excelCustomFieldToJson(f: DynamicExcelCustomFieldEntity) = JSONObject().apply {
        put("id", f.id)
        put("templateId", f.templateId)
        put("fieldName", f.fieldName)
        put("fieldKey", f.fieldKey)
        put("fieldType", f.fieldType)
        putOptValue("naturalLanguagePrompt", f.naturalLanguagePrompt)
        putOptValue("generatedFormulaExpression", f.generatedFormulaExpression)
        put("isHiddenBackendCalculation", f.isHiddenBackendCalculation)
        put("evaluationOrder", f.evaluationOrder)
        put("createdAt", f.createdAt)
    }

    fun jsonToExcelCustomField(j: JSONObject) = DynamicExcelCustomFieldEntity(
        id = j.getString("id"),
        templateId = j.getString("templateId"),
        fieldName = j.optString("fieldName", ""),
        fieldKey = j.optString("fieldKey", ""),
        fieldType = j.optString("fieldType", "FORMULA"),
        naturalLanguagePrompt = j.strOrNull("naturalLanguagePrompt"),
        generatedFormulaExpression = j.strOrNull("generatedFormulaExpression"),
        isHiddenBackendCalculation = j.optBoolean("isHiddenBackendCalculation", false),
        evaluationOrder = j.optInt("evaluationOrder", 0),
        createdAt = j.optLong("createdAt", System.currentTimeMillis())
    )

    fun excelRowToJson(r: DynamicExcelRowRecordEntity) = JSONObject().apply {
        put("id", r.id)
        put("templateId", r.templateId)
        putOptValue("projectId", r.projectId)
        put("rowNumber", r.rowNumber)
        put("rowTitle", r.rowTitle)
        put("rowCategory", r.rowCategory)
        put("valuesJson", r.valuesJson)
        put("calculatedResultsJson", r.calculatedResultsJson)
        putOptValue("photoUri", r.photoUri)
        putOptValue("videoUri", r.videoUri)
        putOptValue("notes", r.notes)
        put("createdAt", r.createdAt)
        put("updatedAt", r.updatedAt)
    }

    fun jsonToExcelRow(j: JSONObject, map: Map<String, String> = emptyMap()) = DynamicExcelRowRecordEntity(
        id = j.getString("id"),
        templateId = j.getString("templateId"),
        projectId = j.strOrNull("projectId"),
        rowNumber = j.optInt("rowNumber", 0),
        rowTitle = j.optString("rowTitle", ""),
        rowCategory = j.optString("rowCategory", "عام"),
        valuesJson = j.optString("valuesJson", "{}"),
        calculatedResultsJson = j.optString("calculatedResultsJson", "{}"),
        photoUri = rewrite(j.strOrNull("photoUri"), map),
        videoUri = rewrite(j.strOrNull("videoUri"), map),
        notes = j.strOrNull("notes"),
        createdAt = j.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = j.optLong("updatedAt", System.currentTimeMillis())
    )

    fun businessProfileToJson(b: BusinessProfileEntity) = JSONObject().apply {
        put("id", b.id)
        put("ownerId", b.ownerId)
        put("companyName", b.companyName)
        put("activityType", b.activityType)
        put("customActivityLabel", b.customActivityLabel)
        put("commercialRegistration", b.commercialRegistration)
        put("taxNumber", b.taxNumber)
        put("phone", b.phone)
        put("address", b.address)
        put("currency", b.currency)
        put("workStartTime", b.workStartTime)
        put("workEndTime", b.workEndTime)
        putOptValue("hqLatitude", b.hqLatitude)
        putOptValue("hqLongitude", b.hqLongitude)
        put("geofenceRadiusMeters", b.geofenceRadiusMeters)
        put("companyGroupId", b.companyGroupId)
        put("companyGroupName", b.companyGroupName)
        put("isSetupCompleted", b.isSetupCompleted)
        put("updatedAt", b.updatedAt)
    }

    fun jsonToBusinessProfile(j: JSONObject) = BusinessProfileEntity(
        id = j.optString("id", "company_profile_main"),
        ownerId = j.optString("ownerId", ""),
        companyName = j.optString("companyName", ""),
        activityType = j.optString("activityType", BusinessActivitySector.GENERAL_TRADING.name),
        customActivityLabel = j.optString("customActivityLabel", ""),
        commercialRegistration = j.optString("commercialRegistration", ""),
        taxNumber = j.optString("taxNumber", ""),
        phone = j.optString("phone", ""),
        address = j.optString("address", ""),
        currency = j.optString("currency", "EGP"),
        workStartTime = j.optString("workStartTime", "09:00"),
        workEndTime = j.optString("workEndTime", "17:00"),
        hqLatitude = j.doubleOrNull("hqLatitude"),
        hqLongitude = j.doubleOrNull("hqLongitude"),
        geofenceRadiusMeters = j.optInt("geofenceRadiusMeters", 250),
        companyGroupId = j.optString("companyGroupId", "wg_company_main"),
        companyGroupName = j.optString("companyGroupName", "مجموعة كل الموظفين"),
        isSetupCompleted = j.optBoolean("isSetupCompleted", false),
        updatedAt = j.optLong("updatedAt", System.currentTimeMillis())
    )

    fun payrollAdjustmentToJson(p: PayrollAdjustmentEntity) = JSONObject().apply {
        put("id", p.id)
        put("employeeId", p.employeeId)
        put("employeeName", p.employeeName)
        put("type", p.type)
        put("amount", p.amount)
        put("reason", p.reason)
        put("effectiveDate", p.effectiveDate)
        put("monthKey", p.monthKey)
        put("createdByName", p.createdByName)
        put("timestamp", p.timestamp)
    }

    fun jsonToPayrollAdjustment(j: JSONObject) = PayrollAdjustmentEntity(
        id = j.getString("id"),
        employeeId = j.optString("employeeId", ""),
        employeeName = j.optString("employeeName", ""),
        type = j.optString("type", "BONUS"),
        amount = j.optDouble("amount", 0.0),
        reason = j.optString("reason", ""),
        effectiveDate = j.optString("effectiveDate", ""),
        monthKey = j.optString("monthKey", ""),
        createdByName = j.optString("createdByName", "المدير"),
        timestamp = j.optLong("timestamp", System.currentTimeMillis())
    )

    fun employeeLeaveToJson(l: EmployeeLeaveEntity) = JSONObject().apply {
        put("id", l.id)
        put("employeeId", l.employeeId)
        put("employeeName", l.employeeName)
        put("leaveType", l.leaveType)
        put("startDate", l.startDate)
        put("endDate", l.endDate)
        put("daysCount", l.daysCount)
        put("status", l.status)
        put("notes", l.notes)
        put("approvedByName", l.approvedByName)
        put("timestamp", l.timestamp)
    }

    fun jsonToEmployeeLeave(j: JSONObject) = EmployeeLeaveEntity(
        id = j.getString("id"),
        employeeId = j.optString("employeeId", ""),
        employeeName = j.optString("employeeName", ""),
        leaveType = j.optString("leaveType", "ANNUAL"),
        startDate = j.optString("startDate", ""),
        endDate = j.optString("endDate", ""),
        daysCount = j.optInt("daysCount", 1),
        status = j.optString("status", "APPROVED"),
        notes = j.optString("notes", ""),
        approvedByName = j.optString("approvedByName", "المدير"),
        timestamp = j.optLong("timestamp", System.currentTimeMillis())
    )
}
