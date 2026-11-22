package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// User types
enum class UserType {
    BUSINESSMAN, GOLD_TRADER, ADMIN
}

enum class BroadcastTargetCategory(val label: String, val icon: String, val description: String) {
    ALL_USERS("الجميع (كافة مستخدمي المنصة)", "📢", "إرسال لكافة المستخدمين والدردشات في المنصة"),
    BUSINESSMEN("رجال الأعمال والمستثمرون", "💼", "إرسال لجميع حسابات المستثمرين والشركات"),
    GOLD_TRADERS("الشركات والتجار المعتمدون", "🏢", "إرسال لجميع التجار والشركات والمؤسسات المعتمدة"),
    SUB_ACCOUNTS("أصحاب الحسابات الفرعية والخزائن", "", "إرسال للموظفين والمحاسبين والخزائن الفرعية"),
    WORKGROUPS("مجموعات وفرق العمل والمشاريع", "️", "إرسال لأعضاء مجموعات العمل الميداني وإدارة المشاريع"),
    ACTIVE_RECENTLY("المستخدمين النشطين مؤخراً", "", "إرسال للمستخدمين المتفاعلين بالعمليات والصفقات");

    fun localizedLabel(language: com.example.util.AppLanguage): String = when (this) {
        ALL_USERS -> com.example.util.Translations.get("broadcast.cat_all", language)
        BUSINESSMEN -> com.example.util.Translations.get("broadcast.cat_business", language)
        GOLD_TRADERS -> com.example.util.Translations.get("broadcast.cat_traders", language)
        SUB_ACCOUNTS -> com.example.util.Translations.get("broadcast.cat_subaccounts", language)
        WORKGROUPS -> com.example.util.Translations.get("broadcast.cat_workgroups", language)
        ACTIVE_RECENTLY -> com.example.util.Translations.get("broadcast.cat_active", language)
    }

    fun localizedDescription(language: com.example.util.AppLanguage): String = when (this) {
        ALL_USERS -> com.example.util.Translations.get("broadcast.desc_all", language)
        BUSINESSMEN -> com.example.util.Translations.get("broadcast.desc_business", language)
        GOLD_TRADERS -> com.example.util.Translations.get("broadcast.desc_traders", language)
        SUB_ACCOUNTS -> com.example.util.Translations.get("broadcast.desc_subaccounts", language)
        WORKGROUPS -> com.example.util.Translations.get("broadcast.desc_workgroups", language)
        ACTIVE_RECENTLY -> com.example.util.Translations.get("broadcast.desc_active", language)
    }
}

enum class MetalType(val label: String) {
    GOLD("ذهب"),
    SILVER("فضة");

    fun localizedLabel(language: com.example.util.AppLanguage): String = when (this) {
        GOLD -> com.example.util.Translations.get("metal.gold", language)
        SILVER -> com.example.util.Translations.get("metal.silver", language)
    }
}

enum class SilverKarat(val label: String) {
    KARAT_999("999 - فضة نقية"),
    KARAT_925("925 - فضة استرلينية"),
    KARAT_800("800 - فضة شعبية");

    fun displayLabel(): String = when (this) {
        KARAT_999 -> "عيار 999"
        KARAT_925 -> "عيار 925"
        KARAT_800 -> "عيار 800"
    }

    fun displayLabel(language: com.example.util.AppLanguage): String = localizedLabel(language)

    fun localizedLabel(language: com.example.util.AppLanguage): String = when (this) {
        KARAT_999 -> com.example.util.Translations.get("silver.karat_999", language)
        KARAT_925 -> com.example.util.Translations.get("silver.karat_925", language)
        KARAT_800 -> com.example.util.Translations.get("silver.karat_800", language)
    }

    fun localizedFullLabel(language: com.example.util.AppLanguage): String = when (this) {
        KARAT_999 -> com.example.util.Translations.get("silver.karat_999_full", language)
        KARAT_925 -> com.example.util.Translations.get("silver.karat_925_full", language)
        KARAT_800 -> com.example.util.Translations.get("silver.karat_800_full", language)
    }
}

enum class GoldKarat(val label: String) {
    KARAT_14("14k"),
    KARAT_18("18k"),
    KARAT_21("21k"),
    KARAT_24("24k");

    fun displayLabel(): String = when (this) {
        KARAT_14 -> "عيار 14"
        KARAT_18 -> "عيار 18"
        KARAT_21 -> "عيار 21"
        KARAT_24 -> "عيار 24"
    }

    fun displayLabel(language: com.example.util.AppLanguage): String = localizedLabel(language)

    fun localizedLabel(language: com.example.util.AppLanguage): String = when (this) {
        KARAT_14 -> com.example.util.Translations.get("gold.karat_14", language)
        KARAT_18 -> com.example.util.Translations.get("gold.karat_18", language)
        KARAT_21 -> com.example.util.Translations.get("gold.karat_21", language)
        KARAT_24 -> com.example.util.Translations.get("gold.karat_24", language)
    }

    companion object {
        fun fromCode(code: String): GoldKarat = when (code.lowercase()) {
            "14k", "14" -> KARAT_14
            "18k", "18" -> KARAT_18
            "21k", "21" -> KARAT_21
            "24k", "24" -> KARAT_24
            else -> KARAT_21
        }
    }
}

enum class TransactionStatus {
    PURCHASED, RETURNED, RESOLD
}

enum class MessageType {
    TEXT, HASHTAG, INVOICE, PAYMENT, SYSTEM, GOLD_PURCHASE, TRANSACTION, DAY_SEPARATOR, VOICE, IMAGE, DOCUMENT, MISSED_CALL, CALL
}

enum class MessageStatus {
    PENDING, SENT, DELIVERED, READ
}

enum class HashtagStatus {
    ACTIVE, SETTLED
}

enum class HashtagEntryType {
    INVOICE, PAYMENT
}

enum class HashtagEntryDirection {
    DEBIT, CREDIT
}

enum class ProjectType {
    CONSTRUCTION, FINISHING, CONTRACTING, OTHER;

    fun displayLabel(): String = when (this) {
        CONSTRUCTION -> "إنشاءات ومباني"
        FINISHING -> "تشطيبات وتجهيز"
        CONTRACTING -> "مقاولات وتوريد"
        OTHER -> "مشروع آخر"
    }

    fun displayLabel(language: com.example.util.AppLanguage): String = localizedLabel(language)

    fun localizedLabel(language: com.example.util.AppLanguage): String = when (this) {
        CONSTRUCTION -> com.example.util.Translations.get("project.type_construction", language)
        FINISHING -> com.example.util.Translations.get("project.type_finishing", language)
        CONTRACTING -> com.example.util.Translations.get("project.type_contracting", language)
        OTHER -> com.example.util.Translations.get("project.type_other", language)
    }
}

enum class TraderStatus {
    NONE, PENDING, APPROVED, REJECTED
}

enum class SaleStatus {
    PENDING, CONFIRMED_BY_BUYER, CONFIRMED_BY_TRADER, COMPLETED, CANCELLED
}

// Room Entities

enum class SubscriptionTier(
    val label: String,
    val description: String,
    val badgeText: String,
    val defaultMonthlyPrice: Double
) {
    FREE_TRIAL(
        label = "الأساسية (تجربة مجانية)",
        description = "تمنح المستخدم الجديد استخدام كافة مزايا وخصائص المنصة بالكامل بلا حدود لمدة 60 يوماً متواصلة! بعد انتهاء المدة يتم تقييد المزايا المتقدمة.",
        badgeText = "شهران مجاناً",
        defaultMonthlyPrice = 0.0
    ),
    SILVER_TRADER(
        label = "الباقة الفضية (المحاسب وإدارة الأعمال)",
        description = "قيود محاسبية يومية غير محدودة، كشوف حسابات الهاشتاج، تصدير تقارير Excel و PDF، إدارة المشاريع والفواتير وحساب فرعي مشفر",
        badgeText = "الفضية",
        defaultMonthlyPrice = 499.0
    ),
    GOLD_ENTERPRISE(
        label = "الباقة الذهبية (الشركات والمؤسسات)",
        description = "حسابات فرعية مشفرة سرية بلا حدود، نظام التدقيق المالي ومكافحة العجز، إدارة المشاريع والصلاحيات، ودفتر الأستاذ المزدوج والذكاء الاصطناعي",
        badgeText = "الأكثر طلباً",
        defaultMonthlyPrice = 999.0
    ),
    DIAMOND_VIP(
        label = "الباقة الماسية (المؤسسات وكبرى الشركات VIP)",
        description = "كل الميزات بلا حدود + المحاسب الذكي والوكيل الصوتي AI + النسخ الاحتياطي التلقائي السحابي + دعم فني VIP مباشر",
        badgeText = "الماسية VIP",
        defaultMonthlyPrice = 1999.0
    );

    fun localizedLabel(language: com.example.util.AppLanguage): String = when (this) {
        FREE_TRIAL -> com.example.util.Translations.get("sub.tier_free_label", language)
        SILVER_TRADER -> com.example.util.Translations.get("sub.tier_silver_label", language)
        GOLD_ENTERPRISE -> com.example.util.Translations.get("sub.tier_gold_label", language)
        DIAMOND_VIP -> com.example.util.Translations.get("sub.tier_diamond_label", language)
    }

    fun localizedDescription(language: com.example.util.AppLanguage): String = when (this) {
        FREE_TRIAL -> com.example.util.Translations.get("sub.tier_free_desc", language)
        SILVER_TRADER -> com.example.util.Translations.get("sub.tier_silver_desc", language)
        GOLD_ENTERPRISE -> com.example.util.Translations.get("sub.tier_gold_desc", language)
        DIAMOND_VIP -> com.example.util.Translations.get("sub.tier_diamond_desc", language)
    }

    fun localizedBadgeText(language: com.example.util.AppLanguage): String = when (this) {
        FREE_TRIAL -> com.example.util.Translations.get("sub.tier_free_badge", language)
        SILVER_TRADER -> com.example.util.Translations.get("sub.tier_silver_badge", language)
        GOLD_ENTERPRISE -> com.example.util.Translations.get("sub.tier_gold_badge", language)
        DIAMOND_VIP -> com.example.util.Translations.get("sub.tier_diamond_badge", language)
    }
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val username: String,
    val type: String = UserType.BUSINESSMAN.name,
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val traderStatus: String = TraderStatus.NONE.name,
    val traderLicense: String? = null,
    val traderShopName: String? = null,
    val traderShopAddress: String? = null,
    val traderTotalGoldWeight: Double = 0.0,
    val traderApprovedAt: Long? = null,
    val avatarUri: String? = null,
    val coverUri: String? = null,
    val verificationNationalId: String? = null,
    val verificationCommercialReg: String? = null,
    val verificationStatus: String = "NONE",
    val subscriptionTier: String = SubscriptionTier.FREE_TRIAL.name,
    val subscriptionStartTimestamp: Long = System.currentTimeMillis(),
    val subscriptionEndTimestamp: Long = System.currentTimeMillis() + (60L * 24 * 3600 * 1000), // 60 days free trial by default
    val isTrialActive: Boolean = true,
    val subscriptionStatus: String = "ACTIVE",
    val appLockEnabled: Boolean = false,
    val appLockPin: String? = null,
    val biometricAuthEnabled: Boolean = true,
    val jobTitle: String? = null,
    val skills: String? = null, // comma or newline separated skills
    val servicesOffered: String? = null, // comma or newline separated services
    val showSkillsPublicly: Boolean = true,
    val experienceYears: Int = 8,
    val ratingScore: Float = 4.9f,
    val totalReviewsCount: Int = 24,
    val merchantBio: String? = null,
    val education: String? = null,
    val cvSectionOrder: String? = "BIO,RELIABILITY,SKILLS,EDUCATION", // Comma separated list of sections
    val city: String? = "القاهرة"
)

data class RepeatUserSummary(
    val name: String,
    val phone: String,
    val username: String,
    val type: String = UserType.BUSINESSMAN.name,
    val jobTitle: String? = null,
    val pin: String? = null,
    val biometricEnabled: Boolean = true,
    val lastLoginTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val username: String? = null,
    val type: String = UserType.BUSINESSMAN.name,
    val isVerified: Boolean = false,
    val fromPhoneBook: Boolean = false,
    val subAccountId: String? = null
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val contactName: String,
    val contactType: String = UserType.BUSINESSMAN.name,
    val contactVerified: Boolean = false,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null,
    val lastMessageStatus: String? = null,
    val lastMessageSenderId: String? = null,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isGroup: Boolean = false,
    val groupId: String? = null,
    val groupName: String? = null,
    val isArchived: Boolean = false,
    val subAccountId: String? = null
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val type: String = MessageType.TEXT.name,
    val content: String,
    val transactionId: String? = null,
    val hashtagId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = MessageStatus.SENT.name,
    val reaction: String? = null,
    val subAccountId: String? = null
)

@Entity(tableName = "chat_accounts")
data class ChatAccountEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val name: String,
    val creatorId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = HashtagStatus.ACTIVE.name
)

@Entity(tableName = "hashtag_accounts")
data class HashtagAccountEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val accountId: String,
    val tag: String,
    val creatorId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = HashtagStatus.ACTIVE.name,
    val projectId: String? = null,
    val projectName: String? = null
)

@Entity(tableName = "hashtag_entries")
data class HashtagEntryEntity(
    @PrimaryKey val id: String,
    val hashtagId: String,
    val accountId: String,
    val type: String = HashtagEntryType.INVOICE.name,
    val description: String,
    val amount: Double,
    val direction: String = HashtagEntryDirection.DEBIT.name,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val photoUri: String? = null,
    val linkedInvoiceId: String? = null,
    val projectId: String? = null,
    val projectName: String? = null
)

@Entity(tableName = "entry_comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val targetId: String, // hashtagId or entryId or accountId
    val text: String,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "hidden_profiles")
data class HiddenProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val secretPinOrPassword: String,
    val companyName: String,
    val roleTitle: String = "المدير المالي والمسؤول",
    val avatarUri: String? = null,
    val initialCapital: Double = 0.0,
    val isEncrypted: Boolean = true,
    val silentNotifications: Boolean = true,
    val colorHex: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "hidden_sub_accounts")
data class HiddenSubAccountEntity(
    @PrimaryKey val id: String,
    val username: String, // اسم المستخدم الفرعي (مثل: oozox_secret)
    val displayName: String, // الاسم المعروض للحساب
    val passwordHash: String, // تجزئة كلمة المرور المشفرة
    val passwordSalt: String, // ملح التشفير الخاص بكلمة المرور
    val rawSecretTrigger: String, // كلمة المرور / الرمز السري للدخول المباشر عبر شريط البحث
    val avatarUri: String? = null,
    val bio: String? = "حساب oozox فرعي مشفر ومحمي",
    val phone: String? = null,
    val muteNotifications: Boolean = true, // كتم كافة إشعارات الحساب المخفي
    val discreetNotifications: Boolean = false, // تمويه الإشعارات بإشعار نظام صامت
    val discreetNotificationTitle: String = "تحديث النظام",
    val autoLockOnExit: Boolean = true, // القفل والرجوع للحساب الأساسي فور الخروج
    val isEncrypted: Boolean = true, // تشفير محلي للبيانات
    val roleTitle: String = "كاشير ومبيعات", // مبيعات / كاشير / مسؤول أمين مخزن
    val canSetProductPrices: Boolean = true, // إمكانية تحديد وتعديل أسعار المنتجات مباشرة
    val canCreateInvoices: Boolean = true, // إمكانية إنشاء وفورورد الفواتير
    val canViewReports: Boolean = false, // إمكانية عرض تقارير الأرباح والمبيعات
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "work_groups")
data class WorkGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val ownerId: String,
    val parentGroupId: String? = null,
    val isAccountingGroup: Boolean = false,
    val isPrivate: Boolean = true,
    val disableMemberDMs: Boolean = true,
    val companyName: String? = null,
    val companyCapital: Double = 0.0,
    val profileId: String? = null,
    val workType: String = "FINANCIAL_ONLY", // "FINANCIAL_ONLY" (تعاملات مالية فقط) or "PHYSICAL_OPERATIONS" (عمليات فيزيائية / بناء عقار ومقاولات)
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_members")
data class GroupMemberEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val userId: String,
    val username: String,
    val phone: String,
    val roleLabel: String,
    val status: String = "accepted",
    val workingHoursStart: String? = "09:00 ص",
    val workingHoursEnd: String? = "05:00 م",
    val shiftHours: Double = 8.0,
    val workingDays: String = "السبت - الخميس",
    val shiftType: String = "نهارية (صباحية)",
    val canPostAccounting: Boolean = true,
    val canExportReports: Boolean = false,
    val canEditRecords: Boolean = false,
    val canMessageDirectly: Boolean = false,
    val isGroupAdmin: Boolean = false,
    val notes: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val workplaceLocation: String? = null,
    val workplaceLatitude: Double? = null,
    val workplaceLongitude: Double? = null,
    val geofencingEnabled: Boolean = false,
    val geofencingRadiusMeters: Int = 200
)

@Entity(tableName = "employee_attendance")
data class EmployeeAttendanceEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val groupId: String,
    val employeeName: String,
    val employeeRole: String,
    val date: String, // "YYYY-MM-DD"
    val clockInTime: String? = null, // e.g. "09:02 ص"
    val clockOutTime: String? = null, // e.g. "05:15 م"
    val clockInTimestamp: Long? = null,
    val clockOutTimestamp: Long? = null,
    val scheduledShiftStart: String? = "09:00 ص",
    val scheduledShiftEnd: String? = "05:00 م",
    val scheduledShiftHours: Double = 8.0,
    val actualWorkedHours: Double = 0.0,
    val status: String = "CLOCKED_IN", // "CLOCKED_IN", "CLOCKED_OUT", "ON_TIME", "LATE", "EARLY_DEPARTURE", "OVERTIME", "ABSENT"
    val overtimeMinutes: Int = 0,
    val lateMinutes: Int = 0,
    val earlyDepartureMinutes: Int = 0,
    val locationTag: String? = "المقر الرئيسي - الخزينة المركزية",
    val notes: String? = null,
    val isLocationVerified: Boolean = false,
    val verificationDetails: String? = null,
    val outOfBoundsSince: Long? = null,
    val gpsStatus: String = "ACTIVE", // "ACTIVE", "DISABLED", "SPOOFED_MOCK", "OUT_OF_BOUNDS"
    val locationPolicyViolated: Boolean = false,
    val lastLocationCheckTime: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_accounting_records")
data class DailyAccountingRecordEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val subGroupId: String? = null, // Department name / Sub-group
    val profileId: String? = null,
    val entryDate: String, // e.g. "2026-08-15"
    val staffId: String,
    val staffName: String,
    val staffRole: String,
    val dailyRevenue: Double = 0.0, // إيرادات اليوم
    val incomingAmount: Double = 0.0, // وارد ومقبوضات
    val outgoingAmount: Double = 0.0, // صادر ومصروفات
    val deficitAmount: Double = 0.0, // عجز الصندوق
    val complaintsCount: Int = 0, // عدد الشكاوى
    val complaintsNotes: String? = null, // تفاصيل الشكاوى
    val dailyCapital: Double = 0.0, // رأس المال اليومي
    val weeklyCapital: Double = 0.0, // رأس المال الأسبوعي
    val monthlyCapital: Double = 0.0, // رأس المال الشهري
    val screenshotUri: String? = null, // لقطة شاشة بالحسابات
    val notes: String? = null,
    val isEncrypted: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "financial_complaints")
data class FinancialComplaintEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val subGroupId: String? = null,
    val profileId: String? = null,
    val submittedByStaffId: String,
    val submittedByStaffName: String,
    val submittedByStaffRole: String,
    val title: String, // عنوان الشكوى / سبب الخلاف
    val details: String, // تفاصيل الشكوى
    val targetDate: String, // تاريخ اليوم المستهدف بأثر رجعي "YYYY-MM-DD"
    val impactType: String = "DEDUCTION", // "DEDUCTION" (خصم) or "ADDITION" (إضافة)
    val amount: Double = 0.0, // قيمة الخصم أو الإضافة
    val category: String = "عجز صندوق", // "عجز صندوق", "فرق حساب كاشير", "مرتجع عميل", "تسوية بضاعة", "أخرى"
    val status: String = "APPLIED", // "APPLIED", "PENDING_REVIEW", "RESOLVED"
    val resultingDayDeficit: Double = 0.0, // عجز ذلك اليوم بعد تطبيق الشكوى
    val adminAlertSent: Boolean = true,
    val resolutionNotes: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val accusedEmployeeId: String? = null, // الموظف المشكو في حقه بالمنشن
    val accusedEmployeeName: String? = null,
    val accusedEmployeePhone: String? = null,
    val invoiceId: String? = null, // معرف الفاتورة المرتبطة بالشكوى
    val invoiceNumber: String? = null, // رقم الفاتورة المرتبطة بالشكوى
    val mentionTag: String? = null, // تاق المنشن التلقائي للموظف المعني مثلا @أحمد_الكاشير
    val claimAmount: Double = amount, // المبلغ المشكو في حقه
    val employeeResponseStatus: String = "PENDING", // PENDING, ACCEPTED (خصم من الرصيد), REJECTED (رفض مع إرفاق سبب/صورة/فيديو)
    val employeeRejectionReason: String? = null, // نص سبب الرفض
    val employeeRejectionProofUri: String? = null, // رابط/ملف إثبات الرفض
    val employeeProofMediaType: String? = null, // IMAGE (صورة) or VIDEO (فيديو)
    val employeeResponseTime: Long? = null,
    val isDeductedFromBalance: Boolean = false, // هل تم خصم المبلغ من رصيده
    val managerAlertSent: Boolean = true, // إشعار مدير العمل بحالة الشكوى
    val managerDecision: String? = null, // PENDING, CONFIRMED_DEDUCTION, DISMISSED
    val forensicAnalysisCompleted: Boolean = false, // هل تم إجراء التحقيق الجنائي الذكي في استلام الشفت
    val forensicLiabilityType: String? = null, // GROSS_NEGLIGENCE_RECEIPT, SUSPECTED_COLLUSION, INADVERTENT_MISCOUNT, OUTGOING_STAFF_SOLE_LIABILITY
    val forensicReceiverStaffId: String? = null, // معرف الموظف المستلم الذي وافق على الشفت
    val forensicReceiverStaffName: String? = null, // اسم الموظف المستلم
    val forensicLiabilityDistribution: String? = null, // نسبة المسؤولية مثلا: 50% على المحرر و 50% على المستلم
    val forensicAuditDetails: String? = null, // التفاصيل الجنائية للتحليل
    val forensicRecommendedAction: String? = null, // الإجراء الموصى به للمدير
    val forensicJointDeductionApplied: Boolean = false // هل تم تطبيق الخصم المشترك
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val workGroupId: String,
    val type: String = ProjectType.CONSTRUCTION.name,
    val estimatedCapital: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val location: String = "القاهرة، مصر",
    val coManagerPhone: String? = null,
    val coManagerName: String? = null,
    val membersInfo: String = "", // Formatted text e.g. "@أحمد (0100000000) - مدير الموقع | @علي (0111111111) - مسؤول المشتريات"
    val createdAt: Long = System.currentTimeMillis()
)

data class ProjectMemberInput(
    val name: String,
    val phone: String,
    val roleTask: String,
    val isCoManager: Boolean = false
)

@Entity(tableName = "gold_transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val currentUserId: String,
    val currentUserName: String,
    val currentUserType: String = UserType.BUSINESSMAN.name,
    val contactId: String,
    val contactName: String,
    val goldKarat: String = GoldKarat.KARAT_21.name,
    val goldType: String = "مصاغ",
    val weight: Double,
    val pricePerGram: Double,
    val manufacturingCostPerGram: Double,
    val totalGoldPrice: Double,
    val totalManufacturing: Double,
    val totalPrice: Double,
    val traderCostPerGram: Double? = null,
    val profit: Double? = null,
    val photoUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = TransactionStatus.PURCHASED.name,
    val originalTransactionId: String? = null,
    val notes: String? = null,
    val metalLossRatePercent: Double = 0.5,
    val metalLossGrams: Double = 0.0
)

@Entity(tableName = "gold_purchases")
data class GoldPurchaseEntity(
    @PrimaryKey val id: String,
    val buyerId: String,
    val traderHashtag: String = "",
    val traderConfirmed: Boolean = false,
    val photoUri: String? = null,
    val extractedWeight: Double,
    val manuallyEdited: Boolean = false,
    val karat: String = GoldKarat.KARAT_21.name,
    val pricePerGram: Double,
    val currentPricePerGram: Double,
    val manufacturer: String = "عام",
    val manufacturingCost: Double = 0.0,
    val stampDuty: Double = 0.0,
    val totalPaid: Double,
    val currentValue: Double,
    val profitLoss: Double = 0.0,
    val profitLossPercent: Double = 0.0,
    val status: String = TransactionStatus.PURCHASED.name,
    val createdAt: Long = System.currentTimeMillis(),
    val metalType: String = MetalType.GOLD.name,
    val mfgType: String = "PER_GRAM",
    val cashbackRefund: Double = 0.0,
    val sellerShopName: String? = null,
    val sellerLocation: String? = null,
    val scalePhotoUri: String? = null,
    val metalLossRatePercent: Double = 0.5,
    val metalLossGrams: Double = 0.0
)

@Entity(tableName = "trader_requests")
data class TraderRequestEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val username: String,
    val shopName: String,
    val shopAddress: String,
    val license: String,
    val status: String = TraderStatus.PENDING.name,
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val adminNotes: String? = null
)

@Entity(tableName = "admin_config")
data class AdminConfigEntity(
    @PrimaryKey val id: Int = 1,
    val appName: String = "الصاغة - تداول الذهب والفضة",
    val appLogoUri: String? = null,
    val adminPin: String = "",
    val geminiApiKey: String? = null,
    val geminiModelName: String = "gemini-3.5-flash",
    val aiSystemPrompt: String = "أنت المحاسب الذكي وخبير تداول الذهب والفضة ومحاسبة المقاولات وتصفية التسويات المالية لمنصة الصاغة.",
    val voiceSelection: String = "ar-EG-Standard-A",
    val personaPrompt: String = "وكيل صوتي ذكي لإدارة الأعمال وتنسيق المحادثات والمجموعات والعمليات المحاسبية الفورية",
    val biometricAuthEnabled: Boolean = true,
    val notificationApiUrl: String? = null,
    val notificationApiKey: String? = null,
    val newsApiUrl: String? = null,
    val goldPriceApiUrl: String? = null,
    val goldPriceApiKey: String? = null,
    val goldPriceAutoRefreshIntervalSec: Int = 15,
    val encryptionEnabled: Boolean = true,
    val lastBackupAt: Long? = null,
    val isAppLockEnabled: Boolean = false,
    val appLockPin: String = "",
    val isBiometricEnabled: Boolean = true
)

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey val id: String,
    val metalType: String = MetalType.GOLD.name,
    val karat: String = GoldKarat.KARAT_21.name,
    val targetPrice: Double,
    val direction: String = "ABOVE", // ABOVE ("أعلى من") or BELOW ("أقل من")
    val isActive: Boolean = true,
    val isTriggered: Boolean = false,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// Non-entity UI models / Helper domain models

data class GoldPricesModel(
    val karat14: Double = 2500.0,
    val karat18: Double = 3200.0,
    val karat21: Double = 3750.0,
    val karat24: Double = 4300.0,
    val goldPound: Double = 30000.0,
    val ounceUSD: Double = 2650.0,
    val silver999: Double = 52.0,
    val silver925: Double = 44.0,
    val silver800: Double = 36.0,
    val updatedAt: Long = System.currentTimeMillis()
)

data class NearbyUserModel(
    val id: String,
    val name: String,
    val username: String,
    val phone: String,
    val userType: String,
    val isVerified: Boolean,
    val distanceMeters: Int,
    val shopName: String? = null,
    val avatarUri: String? = null,
    val bio: String? = null,
    val jobTitle: String? = null,
    val skills: String? = null,
    val ratingScore: Float = 4.9f,
    val totalReviewsCount: Int = 24,
    val lat: Double,
    val lng: Double
)

data class SmartAlertModel(
    val id: String,
    val type: String, // 'warning' | 'error' | 'info' | 'success' | 'critical'
    val title: String,
    val message: String,
    val accountName: String? = null,
    val chatName: String? = null,
    val amount: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AIAnalysisResultModel(
    val insights: List<String>,
    val alerts: List<SmartAlertModel>,
    val recommendations: List<String>,
    val healthScore: Int
)

data class AccountSummaryModel(
    val accountName: String,
    val accountId: String,
    val chatId: String,
    val hashtags: List<HashtagSummaryModel>,
    val totalDebit: Double,
    val totalCredit: Double,
    val totalRemaining: Double,
    val isSettled: Boolean
)

data class HashtagSummaryModel(
    val tag: String,
    val hashtagId: String,
    val totalDebit: Double,
    val totalCredit: Double,
    val remaining: Double,
    val entryCount: Int,
    val status: String
)

data class ChatFinancialReportModel(
    val chatId: String,
    val chatName: String,
    val accounts: List<AccountSummaryModel>,
    val grandTotalDebit: Double,
    val grandTotalCredit: Double,
    val grandTotalRemaining: Double,
    val totalTransactions: Int,
    val settledAccounts: Int,
    val activeAccounts: Int
)

// --- Architecture Feature 1: Project Role-Based Access Control (RBAC) & Audit Log ---
enum class ProjectMemberRole(val label: String, val description: String) {
    OWNER("مالك المشروع", "صلاحية كاملة للإدارة والتنفيذ والماليات"),
    FINANCIAL_PARTNER("شريك مالي ومساهم", "الاطلاع على رأس المال والميزانية والتقارير المالية"),
    EXECUTIVE_MANAGER("مدير تنفيذي", "إدارة العمليات والموقع الميداني وإرسال طلبات التوريد"),
    PROCUREMENT_OFFICER("مسؤول مشتريات وتوريد", "إدخال فواتير المواد الخام والمصروفات الميدانية"),
    AUDITOR("مدقق ومراجع حسابات", "عرض الحسابات والسجلات والمطابقة دون صلاحية التعديل");

    fun localizedLabel(language: com.example.util.AppLanguage): String = when (this) {
        OWNER -> com.example.util.Translations.get("proj.role_owner", language)
        FINANCIAL_PARTNER -> com.example.util.Translations.get("proj.role_partner", language)
        EXECUTIVE_MANAGER -> com.example.util.Translations.get("proj.role_exec", language)
        PROCUREMENT_OFFICER -> com.example.util.Translations.get("proj.role_procurement", language)
        AUDITOR -> com.example.util.Translations.get("proj.role_auditor", language)
    }

    fun localizedDescription(language: com.example.util.AppLanguage): String = when (this) {
        OWNER -> com.example.util.Translations.get("proj.role_desc_owner", language)
        FINANCIAL_PARTNER -> com.example.util.Translations.get("proj.role_desc_partner", language)
        EXECUTIVE_MANAGER -> com.example.util.Translations.get("proj.role_desc_exec", language)
        PROCUREMENT_OFFICER -> com.example.util.Translations.get("proj.role_desc_procurement", language)
        AUDITOR -> com.example.util.Translations.get("proj.role_desc_auditor", language)
    }
}

@Entity(tableName = "project_members")
data class ProjectMemberEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val userId: String,
    val userName: String,
    val userPhone: String,
    val role: String = ProjectMemberRole.EXECUTIVE_MANAGER.name,
    val assignedTask: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_audit_logs")
data class ProjectAuditLogEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val action: String,
    val actorName: String,
    val actorPhone: String,
    val details: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// --- Architecture Feature 2: Double-Entry Immutable Ledger ---
enum class LedgerAssetType {
    GOLD_24K, SILVER_999, CASH_EGP
}

enum class LedgerEntryType {
    DEBIT, CREDIT, REVERSAL
}

@Entity(tableName = "immutable_ledger")
data class LedgerEntryEntity(
    @PrimaryKey val id: String,
    val txId: String? = null,
    val purchaseId: String? = null,
    val accountOwnerId: String,
    val counterPartyId: String,
    val counterPartyName: String,
    val assetType: String = LedgerAssetType.GOLD_24K.name,
    val entryType: String = LedgerEntryType.DEBIT.name,
    val weightGrams: Double = 0.0,
    val cashAmount: Double = 0.0,
    val metalLossRate: Double = 0.0,
    val checksumHash: String,
    val isReversal: Boolean = false,
    val originalLedgerId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)

// --- Architecture Feature 3: Automated Clearing House (التقاص الآلي للصنعيات والذهب) ---
@Entity(tableName = "clearing_settlements")
data class ClearingSettlementEntity(
    @PrimaryKey val id: String,
    val traderAId: String,
    val traderAName: String,
    val traderBId: String,
    val traderBName: String,
    val netGoldWeight24k: Double, // + or -
    val netSilverWeight999: Double, // + or -
    val netCashAmount: Double, // + or -
    val metalLossDeduction: Double,
    val status: String = "SETTLED",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)

// --- Architecture Feature 4: Offline-First Write-Ahead Sync Queue ---
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val actionType: String, // ADD_TRANSACTION, ADD_PROJECT, ADD_PURCHASE, CLEARING_SETTLEMENT
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, SYNCED, FAILED
    val retryCount: Int = 0,
    val errorMessage: String? = null
)

// --- Dynamic Subscriptions & Admin Offer Management ---
@Entity(tableName = "subscription_plans")
data class SubscriptionPlanEntity(
    @PrimaryKey val id: String, // FREE_TRIAL, SILVER_TRADER, GOLD_ENTERPRISE, DIAMOND_VIP
    val title: String,
    val description: String,
    val monthlyPrice: Double,
    val annualPrice: Double,
    val discountPercent: Int = 0,
    val offerTitle: String? = null,
    val isOfferActive: Boolean = false,
    val featuresListText: String, // Newline separated features
    val badgeText: String? = null,
    val isPopular: Boolean = false,
    val isEnabled: Boolean = true
)

@Entity(tableName = "subscription_requests")
data class SubscriptionRequestEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userPhone: String,
    val requestedPlanId: String,
    val requestedPlanTitle: String,
    val pricePaid: Double,
    val paymentMethod: String = "InstaPay / فودافون كاش",
    val receiptPhotoUri: String? = null,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val requestedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val adminNotes: String? = null
)

@Entity(tableName = "promo_announcements")
data class PromoAnnouncementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val promoCode: String? = null,
    val discountPercent: Int = 0,
    val targetPlanId: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// --- Merchant Inventory & Storefront Models ---
enum class MerchantProductCategory(val label: String, val icon: String) {
    BULLION("سبائك ذهب", ""),
    JEWELRY("مشغولات ومصاغ", ""),
    GOLD_POUND("جنيهات ذهب", ""),
    SILVER("فضيات 925", ""),
    SERVICES("خدمات وصياغة", ""),
    CUSTOM_ORDERS("تفصيل وتصميم", "");

    fun localizedLabel(language: com.example.util.AppLanguage): String = when (this) {
        BULLION -> com.example.util.Translations.get("merchant.cat_bullion", language)
        JEWELRY -> com.example.util.Translations.get("merchant.cat_jewelry", language)
        GOLD_POUND -> com.example.util.Translations.get("merchant.cat_gold_pound", language)
        SILVER -> com.example.util.Translations.get("merchant.cat_silver", language)
        SERVICES -> com.example.util.Translations.get("merchant.cat_services", language)
        CUSTOM_ORDERS -> com.example.util.Translations.get("merchant.cat_custom", language)
    }
}

@Entity(tableName = "merchant_reviews")
data class MerchantReviewEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val reviewerId: String,
    val reviewerName: String,
    val reviewerPhone: String? = null,
    val reviewerAvatarUri: String? = null,
    val rating: Float = 5.0f, // 1.0 to 5.0
    val ratingAspectAccuracy: Float = 5.0f, // دقة الوزن والعيار
    val ratingAspectSpeed: Float = 5.0f, // سرعة المعاملة والتسليم
    val ratingAspectHonesty: Float = 5.0f, // الأمانة والنزاهة
    val comment: String,
    val isVerifiedBuyer: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "merchant_products")
data class MerchantProductEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val merchantShopName: String,
    val merchantPhone: String? = null,
    val title: String,
    val category: String = MerchantProductCategory.BULLION.name,
    val karat: String = GoldKarat.KARAT_24.name,
    val weightGrams: Double,
    val manufacturingCostPerGram: Double = 0.0,
    val consumerPriceEGP: Double = 0.0, // سعر المستهلك النهائي
    val wholesalePriceEGP: Double = 0.0, // سعر الجملة / التكلفة
    val discountRatePercent: Double = 0.0, // نسبة الخصم
    val quantityInStock: Double = 1.0, // العدد أو الكمية المتاحة في المخزن
    val priceApprovalStatus: String = "APPROVED", // APPROVED or PENDING_OWNER_REVIEW (إذا أضاف الموظف السعر بانتظار اعتماد المالك)
    val addedByStaffName: String? = null,
    val photoUri: String? = null,
    val description: String? = null,
    val extractedOcrText: String? = null, // النصوص المستخرجة بدقة AI من العلبة
    val extractedBarcode: String? = null, // الباركود أو الكود المقروء
    val extractedKeywords: String? = null, // الكلمات المفتاحية للمطابقة السريعة
    val isPanoramicScanned: Boolean = false, // تم تصويره بزاوية 360 درجة مفروضة
    val panoramicScanUri: String? = null, // مسار صورة المسح الأسطواني 360
    val isAvailable: Boolean = true,
    val viewsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "merchant_daily_sales")
data class MerchantDailySaleEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val productId: String? = null,
    val productTitle: String,
    val weightGrams: Double,
    val karat: String = GoldKarat.KARAT_21.name,
    val pricePerGram: Double,
    val mfgCostPerGram: Double = 0.0,
    val totalEGP: Double,
    val profitEGP: Double,
    val customerName: String,
    val customerPhone: String? = null,
    val saleTimestamp: Long = System.currentTimeMillis(),
    val isClosedInDailyReport: Boolean = false
)

@Entity(tableName = "merchant_daily_closing_reports")
data class MerchantDailyClosingReportEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val reportDateText: String, // e.g. "2026-08-13"
    val totalSalesCount: Int,
    val totalWeightSoldGrams: Double,
    val totalRevenueEGP: Double,
    val totalProfitEGP: Double,
    val remainingInventoryWeightGrams: Double,
    val reportTimestamp: Long = System.currentTimeMillis()
)

// --- Offline Data Persistence: Invoices & Project Tasks Entities ---

enum class InvoiceStatus {
    PAID, PARTIAL, PENDING, OVERDUE, CANCELLED
}

enum class InvoiceCategory {
    GOLD_DEAL, GOLD_PURCHASE, PROJECT_SUPPLY, SERVICE, GENERAL
}

data class PaymentMethodOption(
    val id: String,
    val name: String,
    val iconName: String = "CreditCard",
    val paymentDetails: String = "", // e.g. حساب إنستاباي، رقم محفظة فودافون كاش، رقم IBAN
    val paymentInstructions: String = "", // e.g. برجاء كتابة رقم الفاتورة في وصف التحويل وإرسال الإشعار
    val isCustom: Boolean = false
)

data class EndOfDayReportModel(
    val reportDateText: String,
    val totalInvoicesCount: Int,
    val totalRevenueEGP: Double,
    val totalProfitEGP: Double,
    val paymentMethodsBreakdown: Map<String, Double>,
    val totalInventoryCount: Int,
    val totalInventoryWeightGrams: Double,
    val totalInventoryValueEGP: Double,
    val deficitAmountEGP: Double,
    val pendingPriceReviewsCount: Int,
    val invoicesListToday: List<InvoiceEntity>
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val invoiceNumber: String,
    val title: String,
    val category: String = InvoiceCategory.GENERAL.name,
    val senderId: String,
    val senderName: String,
    val receiverId: String,
    val receiverName: String,
    val totalAmount: Double,
    val goldWeightGrams: Double? = null,
    val goldKarat: String? = null,
    val pricePerGram: Double? = null,
    val manufacturingCost: Double? = null,
    val paymentMethod: String = "كاش", // كاش / فودافون كاش / إنستا باي / فيزا / تحويل بنكي / شيك / آجل
    val cashierName: String? = null, // الموظف أو الكاشير المسؤول عن إصدار الفاتورة
    val discountAmountEGP: Double = 0.0, // الخصم المميز المطبق على الفاتورة
    val status: String = InvoiceStatus.PENDING.name,
    val issueDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val notes: String? = null,
    val projectId: String? = null,
    val projectName: String? = null,
    val isDeliveredToOozoxUser: Boolean = false, // هل العميل مستخدم في زوكس وتم التسليم داخل التطبيق
    val dispatchedViaSocialChannel: String? = null, // وسيلة الإرسال إذا لم يكن في زوكس (WhatsApp, Telegram, SMS)
    val supplierPayoutAmount: Double = 0.0, // أي مبالغ مدفوعة لمورد من هذه العملية
    val supplierPayoutMethod: String? = null, // وسيلة الدفع للمورد
    val paymentMethodDetails: String? = null, // بيانات وسيلة الدفع (حساب إنستاباي، محفظة كاش، IBAN)
    val paymentDemandNotice: String? = null, // نص المطالبة بالدفع الموجهة للعميل مع تفاصيل السداد
    val isSynced: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val hashtagId: String? = null,
    val hashtagTag: String? = null,
    val chatId: String? = null,
    val paidAmount: Double = 0.0,
    val isExternalBusiness: Boolean = false,
    val sourceType: String = "DIRECT" // DIRECT, CHAT_HASHTAG, PROJECT, POS_BUSINESS, EXTERNAL
)

enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED, CANCELLED
}

@Entity(tableName = "project_tasks")
data class ProjectTaskEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val projectName: String,
    val title: String,
    val description: String? = null,
    val assignedToUserId: String? = null,
    val assignedToName: String? = null,
    val assignedToPhone: String? = null,
    val priority: String = TaskPriority.MEDIUM.name,
    val status: String = TaskStatus.PENDING.name,
    val dueDate: String? = null,
    val costEstimate: Double = 0.0,
    val isSynced: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Physical Field Operations Support (بناء عقار / مقاولات / عمليات فيزيائية وميدانية)
    val workGroupId: String? = null,
    val mediaAttachmentUri: String? = null, // صورة أو فيديو العمل المطلوب من الموقع
    val mediaAttachmentType: String = "PHOTO", // PHOTO or VIDEO
    val assignedMembersList: String = "", // المنشن للمشار إليهم: حتى 3 أشخاص
    val assignedMemberIds: String = "",
    val deadlineDurationText: String = "بعد 3 ساعات", // مدة التسليم المطلوبة: بعد ساعة، ساعتين، 3 ساعات، يوم، يومين، 3 أيام...
    val deadlineTimestamp: Long = 0L,
    val urgencyAlertLevel: String = "NORMAL", // NORMAL, WARNING, URGENT_ESCALATING, OVERDUE
    val workerResponseUri: String? = null, // رد الموظف: فيديو أو صورة مماثلة توثق الإنجاز
    val workerResponseType: String? = "PHOTO", // PHOTO or VIDEO
    val workerResponseText: String? = null, // نص توضيحي يمثل المهمة المنجزة
    val workerResponseSubmittedAt: Long? = null,
    val workerResponseByUserId: String? = null,
    val workerResponseByUserName: String? = null,
    val isSubmittedForReview: Boolean = false, // تم إشعار الأدمن بأن المهمة تمت وبانتظار الفحص
    val isArchived: Boolean = false, // تم تأكيدها وأرشفتها بالسجلات
    val adminConfirmedAt: Long? = null,
    val financialAmount: Double = 0.0, // المعاملات المالية المربوطة بالمهمة
    val financialNotes: String? = null
)

object QuickBusinessReplies {
    val defaultReplies: List<String> = listOf(
        "تم التأكيد والموافقة",
        "تم تحويل المبلغ لحسابكم",
        "يرجى إرسال الفاتورة الرسمية",
        "تم استلام الشحنة والمطابقة",
        "ننتظر اعتماد سند الصرف"
    )

    fun localizedReplies(language: com.example.util.AppLanguage): List<String> = listOf(
        com.example.util.Translations.get("quick_reply.confirmed", language),
        com.example.util.Translations.get("quick_reply.transferred", language),
        com.example.util.Translations.get("quick_reply.send_invoice", language),
        com.example.util.Translations.get("quick_reply.shipment_matched", language),
        com.example.util.Translations.get("quick_reply.awaiting_voucher", language)
    )
}

@Entity(tableName = "shift_handovers")
data class ShiftHandoverEntity(
    @PrimaryKey val id: String,
    val cashierId: String,
    val cashierName: String,
    val loginTime: Long,
    val logoutTime: Long = System.currentTimeMillis(),
    val totalInvoicesCount: Int,
    val operationsCount: Int = totalInvoicesCount, // إجمالي عدد العمليات
    val totalRevenueEGP: Double,
    val totalPaidEGP: Double,
    val instaPayCollected: Double = 0.0, // المحصل عبر إنستاباي
    val vodafoneCashCollected: Double = 0.0, // المحصل عبر فودافون كاش والمحافظ
    val cashCollected: Double = 0.0, // المحصل نقداً في الخزينة
    val bankCollected: Double = 0.0, // المحصل بالتحويل البنكي أو الفيزا
    val supplierPayoutsDuringShift: Double = 0.0, // مدفوعات الموردين من الخزينة
    val netTreasuryCash: Double = 0.0, // صافي النقدية بالخزينة
    val totalIncomingTransactions: Double,
    val totalOutgoingTransactions: Double,
    val productsAddedCount: Int,
    val productsSoldCount: Int,
    val notes: String? = null,
    val expectedCash: Double,
    val actualCash: Double,
    val deficitAmount: Double = 0.0,
    val surplusAmount: Double = 0.0,
    val hasDeficit: Boolean = deficitAmount > 0.0,
    val isFlaggedToOwner: Boolean = false,
    val managerAlertSent: Boolean = false,
    val cashierPhone: String? = null,
    val shiftDateText: String = "", // e.g. "2026-08-15"
    val receivedByCashierId: String? = null, // الطرف الآخر الذي استلم الشفت
    val receivedByCashierName: String? = null,
    val isReceived: Boolean = false, // هل تم استلام الشفت وتأكيده
    val receivingNotes: String? = null, // ملاحظات مستلم الشفت
    val receivedAt: Long? = null,
    val retroactiveBalance: Double = 0.0, // رصيد الشفت المالي بأثر رجعي
    val merchandiseStatus: String = "مطابقة البضاعة والمخزون بالكامل", // حالة البضاعة والمخزون
    val physicalStockChecked: Boolean = true, // هل تم تأكيد جرد البضاعة فعلياً
    val cashDrawerConfirmed: Boolean = true, // هل تم تأكيد استلام الخزينة ومطابقتها
    val invoicesAuditedCount: Int = totalInvoicesCount, // عدد الفواتير التي تمت مراجعتها عند الاستلام
    val receiverSignature: String? = null, // التوقيع الإلكتروني لمستلم العهدة
    val acceptanceType: String = "PENDING", // PENDING, ACCEPTED_FULL_MATCH, ACCEPTED_WITH_DISCREPANCY, REJECTED
    val goodsDiscrepancyNotes: String? = null // تفاصيل أي فروقات في البضاعة أو النقدية عند الاستلام
)

data class RetroactiveEmployeeLedger(
    val employeeId: String,
    val employeeName: String,
    val employeeRole: String,
    val dateText: String,
    val shiftHandoverDate: String,
    val shiftHandoverId: String?,
    val isShiftReceived: Boolean,
    val openingBalance: Double,
    val totalRevenue: Double,
    val totalCashPaid: Double,
    val totalIncoming: Double,
    val totalOutgoing: Double,
    val complaintsDeductions: Double,
    val deficitAmount: Double,
    val netBalance: Double
)

// --- Dynamic Excel Spreadsheet Data Schema for Backend Formulas & Extensibility ---

@Entity(tableName = "dynamic_excel_templates")
data class DynamicExcelTemplateEntity(
    @PrimaryKey val id: String,
    val projectId: String? = null,
    val activityType: String, // CONSTRUCTION, GOLD_JEWELRY, RETAIL_COMMERCE, MANUFACTURING, SERVICES_AGENCY, CUSTOM
    val templateName: String,
    val description: String? = null,
    val columnsJson: String = "[]", // Serialized list of columns & formulas
    val defaultRowsJson: String = "[]", // Initial rows with values and formulas
    val summaryKpisJson: String = "[]", // KPIs to evaluate
    val isUserCustomCreated: Boolean = false,
    val createdByUserId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "dynamic_excel_custom_fields")
data class DynamicExcelCustomFieldEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val fieldName: String,
    val fieldKey: String,
    val fieldType: String = "FORMULA", // NUMBER, CURRENCY, PERCENTAGE, FORMULA, TEXT, MEDIA
    val naturalLanguagePrompt: String? = null, // e.g. "أريد حساب كذا..."
    val generatedFormulaExpression: String? = null, // backend mathematical formula
    val isHiddenBackendCalculation: Boolean = false,
    val evaluationOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "dynamic_excel_row_records")
data class DynamicExcelRowRecordEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val projectId: String? = null,
    val rowNumber: Int,
    val rowTitle: String,
    val rowCategory: String = "عام",
    val valuesJson: String = "{}", // Key-value map of user inputs
    val calculatedResultsJson: String = "{}", // Results evaluated by backend engine
    val photoUri: String? = null,
    val videoUri: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)



