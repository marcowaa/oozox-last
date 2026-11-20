package com.example.util

import com.example.data.model.SubscriptionTier
import com.example.data.model.UserEntity

enum class SubscriptionPhase(
    val titleAr: String,
    val descriptionAr: String,
    val alertLevel: String // "SUCCESS", "INFO", "WARNING", "DANGER"
) {
    TRIAL_ACTIVE(
        titleAr = "الفترة التجريبية المجانية الشاملة (60 يوماً)",
        descriptionAr = "متاح لك استخدام كافة ميزات المنصة والذكاء الاصطناعي والحسابات المشفرة بدون أي قيود.",
        alertLevel = "SUCCESS"
    ),
    GRACE_WARNING_STAGE_1(
        titleAr = "المرحلة الأولى: انتهاء التجربة وحجب الذكاء الاصطناعي",
        descriptionAr = "انتهت فترة الـ 60 يوماً التجريبية. تم تقييد المحاسب الذكي وفحص الموازين والمزامنة السحابية. يرجى الترقية.",
        alertLevel = "WARNING"
    ),
    GRACE_WARNING_STAGE_2(
        titleAr = "المرحلة الثانية: حجب التصدير والخزائن المشفرة",
        descriptionAr = "تنبيه المرحلة الثانية: تم تقييد تصدير تقارير Excel و PDF وإنشاء الخزائن السرية الجديدة ونظام تدقيق العجز.",
        alertLevel = "DANGER"
    ),
    BASE_RESTRICTED(
        titleAr = "الحجب الكامل للمزايا المدفوعة",
        descriptionAr = "تم حجب جميع الميزات المتقدمة لانتهاء فترة التجربة والتدرج. يمكنك الترقية الآن للاستمرار بكامل الصلاحيات.",
        alertLevel = "DANGER"
    ),
    PAID_ACTIVE(
        titleAr = "اشتراك مدفوع نشط ⚡",
        descriptionAr = "حسابك مفعل بخطة مدفوعة وتتمتع بكافة المزايا المخصصة لباقاتك.",
        alertLevel = "SUCCESS"
    ),
    PAID_EXPIRED(
        titleAr = "انتهى الاشتراك المدفوع",
        descriptionAr = "انتهت مدة اشتراكك الحالي. يرجى التجديد لمواصلة الاستفادة من الميزات المتقدمة.",
        alertLevel = "WARNING"
    )
}

enum class AppFeature(
    val titleAr: String,
    val descriptionAr: String,
    val minimumTier: SubscriptionTier,
    val iconName: String
) {
    GEMINI_AI_AND_OCR(
        titleAr = "المحاسب الذكي وفحص موازين الذهب بالـ AI",
        descriptionAr = "تحليل الفواتير الذكي، فحص موازين الصاغة بالكاميرا، والدردشة مع المحاسب الآلي.",
        minimumTier = SubscriptionTier.DIAMOND_VIP,
        iconName = "Psychology"
    ),
    CONTEXTUAL_AI_CHAT_TRANSLATION(
        titleAr = "الترجمة السياقية الفورية والنطق الصوتي بالذكاء الاصطناعي",
        descriptionAr = "ترجمة المحادثات والرسائل وفق سياق المعنى وليس ترجمة حرفية مع النطق الصوتي التلقائي والتعرف اللغوي.",
        minimumTier = SubscriptionTier.SILVER_TRADER,
        iconName = "Translate"
    ),
    GOOGLE_DRIVE_CLOUD_BACKUP(
        titleAr = "النسخ الاحتياطي السحابي المشفر (Google Drive)",
        descriptionAr = "ترحيل وتأمين نسخ الخزائن السرية والحسابات الفرعية على السحابة واستعادتها برقم الهاتف.",
        minimumTier = SubscriptionTier.DIAMOND_VIP,
        iconName = "CloudSync"
    ),
    ADVANCED_EXCEL_PDF_EXPORT(
        titleAr = "تصدير تقارير المحاسبة والقيود (Excel & PDF)",
        descriptionAr = "استخراج اليومية الأمريكية، أستاذ الحسابات، وتقارير مبيعات وجرد الصاغة لملفات قابلة للطباعة.",
        minimumTier = SubscriptionTier.SILVER_TRADER,
        iconName = "FileDownload"
    ),
    SECRET_SUB_ACCOUNTS_AND_VAULTS(
        titleAr = "الخزائن السرية والحسابات الفرعية المشفرة",
        descriptionAr = "إنشاء وإدارة حسابات مخفية بكلمات مرور سرية تفتح من شريط البحث ولا ترسل إشعارات.",
        minimumTier = SubscriptionTier.SILVER_TRADER,
        iconName = "Security"
    ),
    FINANCIAL_COMPLAINTS_AND_AUDIT(
        titleAr = "نظام الشكاوى المالية وتدقيق العجز بأثر رجعي",
        descriptionAr = "تقديم شكاوى تدقيق الحسابات وإعادة احتساب الفروقات في القيود اليومية آلياً.",
        minimumTier = SubscriptionTier.GOLD_ENTERPRISE,
        iconName = "FactCheck"
    ),
    WORKGROUPS_AND_RBAC(
        titleAr = "إدارة مجموعات العمل وصلاحيات الموظفين",
        descriptionAr = "تأسيس أقسام متفرعة للمؤسسة، تتبع الحضور والانصراف وصلاحيات القيود المحاسبية للموظفين.",
        minimumTier = SubscriptionTier.GOLD_ENTERPRISE,
        iconName = "Groups"
    ),
    BASIC_ACCOUNTING_AND_CHATS(
        titleAr = "الدردشات الفردية وتسجيل العمليات الأساسية",
        descriptionAr = "محادثات فورية، تسجيل صفقات البيع والشراء والأسعار الحية للذهب والفضة.",
        minimumTier = SubscriptionTier.FREE_TRIAL,
        iconName = "Forum"
    )
}

data class FeatureAccessDecision(
    val isAllowed: Boolean,
    val feature: AppFeature,
    val currentPhase: SubscriptionPhase,
    val restrictionMessage: String? = null,
    val suggestedTier: SubscriptionTier = feature.minimumTier
)

data class SubscriptionDetails(
    val tier: SubscriptionTier,
    val phase: SubscriptionPhase,
    val isTrial: Boolean,
    val isPaid: Boolean,
    val totalTrialDays: Int = 60,
    val trialDaysRemaining: Long,
    val daysPastTrial: Long,
    val daysPastPaidExpiry: Long,
    val trialProgressRatio: Float, // 0.0f (expired) to 1.0f (full trial remaining)
    val alertTitle: String,
    val alertMessage: String,
    val daysUntilNextPhase: Long
)

object SubscriptionFeatureGate {

    const val TRIAL_DURATION_DAYS = 60L
    const val GRACE_STAGE_1_DAYS = 10L // Days 61 to 70
    const val GRACE_STAGE_2_DAYS = 10L // Days 71 to 80

    fun evaluateSubscription(user: UserEntity?): SubscriptionDetails {
        if (user == null) {
            return SubscriptionDetails(
                tier = SubscriptionTier.FREE_TRIAL,
                phase = SubscriptionPhase.TRIAL_ACTIVE,
                isTrial = true,
                isPaid = false,
                trialDaysRemaining = 60L,
                daysPastTrial = 0L,
                daysPastPaidExpiry = 0L,
                trialProgressRatio = 1.0f,
                alertTitle = SubscriptionPhase.TRIAL_ACTIVE.titleAr,
                alertMessage = "فترة تجريبية مجانية لمدة 60 يوماً بكامل المزايا.",
                daysUntilNextPhase = 60L
            )
        }

        val now = System.currentTimeMillis()
        val currentTier = SubscriptionTier.entries.find { it.name == user.subscriptionTier } ?: SubscriptionTier.FREE_TRIAL
        val isPaidTier = currentTier != SubscriptionTier.FREE_TRIAL

        if (isPaidTier) {
            val isPaidActive = user.subscriptionEndTimestamp > now
            if (isPaidActive) {
                val remainingPaidDays = ((user.subscriptionEndTimestamp - now) / (1000L * 3600 * 24)).coerceAtLeast(0L)
                return SubscriptionDetails(
                    tier = currentTier,
                    phase = SubscriptionPhase.PAID_ACTIVE,
                    isTrial = false,
                    isPaid = true,
                    trialDaysRemaining = 0L,
                    daysPastTrial = 0L,
                    daysPastPaidExpiry = 0L,
                    trialProgressRatio = 1.0f,
                    alertTitle = "باقتك: ${currentTier.label} (${currentTier.badgeText}) ⚡",
                    alertMessage = "حسابك نشط ومفعل حتى ${formatDate(user.subscriptionEndTimestamp)} (متبقي $remainingPaidDays يوماً).",
                    daysUntilNextPhase = remainingPaidDays
                )
            } else {
                val daysExpired = ((now - user.subscriptionEndTimestamp) / (1000L * 3600 * 24)).coerceAtLeast(1L)
                return SubscriptionDetails(
                    tier = currentTier,
                    phase = SubscriptionPhase.PAID_EXPIRED,
                    isTrial = false,
                    isPaid = true,
                    trialDaysRemaining = 0L,
                    daysPastTrial = 0L,
                    daysPastPaidExpiry = daysExpired,
                    trialProgressRatio = 0.0f,
                    alertTitle = "انتهى اشتراك ${currentTier.label} منذ $daysExpired يوم",
                    alertMessage = "انتهت فترة الاشتراك المدفوع. يرجى تجديد باقتك لمواصلة استخدام المزايا الحصرية.",
                    daysUntilNextPhase = 0L
                )
            }
        }

        // --- FREE TRIAL CALCULATION (60 Days + Gradual Restriction) ---
        val trialStart = if (user.subscriptionStartTimestamp > 0) user.subscriptionStartTimestamp else user.createdAt
        val trialEnd = user.subscriptionEndTimestamp
        val trialTotalMs = (TRIAL_DURATION_DAYS * 24L * 3600L * 1000L).coerceAtLeast(1L)

        val diffToTrialEnd = trialEnd - now

        if (diffToTrialEnd > 0) {
            // Still in the 60-day full free trial!
            val daysRemaining = (diffToTrialEnd / (1000L * 3600 * 24)).coerceAtLeast(1L)
            val progress = (diffToTrialEnd.toFloat() / trialTotalMs.toFloat()).coerceIn(0.0f, 1.0f)
            return SubscriptionDetails(
                tier = SubscriptionTier.FREE_TRIAL,
                phase = SubscriptionPhase.TRIAL_ACTIVE,
                isTrial = true,
                isPaid = false,
                trialDaysRemaining = daysRemaining,
                daysPastTrial = 0L,
                daysPastPaidExpiry = 0L,
                trialProgressRatio = progress,
                alertTitle = "🎁 تجربة مجانية شاملة: متبقي $daysRemaining يوماً",
                alertMessage = "تتمتع بكامل ميزات المنصة المدفوعة والذكاء الاصطناعي مجاناً لمدة شهرين (60 يوماً).",
                daysUntilNextPhase = daysRemaining
            )
        }

        // Trial has expired, calculate days elapsed since expiration
        val elapsedSinceTrialExpiredMs = now - trialEnd
        val daysElapsedSinceExpired = (elapsedSinceTrialExpiredMs / (1000L * 3600 * 24)).coerceAtLeast(0L)

        return when {
            daysElapsedSinceExpired < GRACE_STAGE_1_DAYS -> {
                // Phase 1: Days 1 to 10 after trial end (Days 61..70 from start)
                val daysRemainingInStage = GRACE_STAGE_1_DAYS - daysElapsedSinceExpired
                SubscriptionDetails(
                    tier = SubscriptionTier.FREE_TRIAL,
                    phase = SubscriptionPhase.GRACE_WARNING_STAGE_1,
                    isTrial = true,
                    isPaid = false,
                    trialDaysRemaining = 0L,
                    daysPastTrial = daysElapsedSinceExpired,
                    daysPastPaidExpiry = 0L,
                    trialProgressRatio = 0.0f,
                    alertTitle = "⚠️ تنبيه التدرج (المرحلة 1): انتهت الـ 60 يوماً التجريبية",
                    alertMessage = "تم تقييد الذكاء الاصطناعي والمزامنة السحابية. متبقي $daysRemainingInStage يوماً قبل حجب التقارير والخزائن.",
                    daysUntilNextPhase = daysRemainingInStage
                )
            }
            daysElapsedSinceExpired < (GRACE_STAGE_1_DAYS + GRACE_STAGE_2_DAYS) -> {
                // Phase 2: Days 11 to 20 after trial end (Days 71..80 from start)
                val daysInStage2 = daysElapsedSinceExpired - GRACE_STAGE_1_DAYS
                val daysRemainingInStage = GRACE_STAGE_2_DAYS - daysInStage2
                SubscriptionDetails(
                    tier = SubscriptionTier.FREE_TRIAL,
                    phase = SubscriptionPhase.GRACE_WARNING_STAGE_2,
                    isTrial = true,
                    isPaid = false,
                    trialDaysRemaining = 0L,
                    daysPastTrial = daysElapsedSinceExpired,
                    daysPastPaidExpiry = 0L,
                    trialProgressRatio = 0.0f,
                    alertTitle = "🚨 تنبيه التدرج (المرحلة 2): تقييد التقارير والخزائن السرية",
                    alertMessage = "تم حجب تصدير Excel ونظام تدقيق العجز والخزائن الجديدة. متبقي $daysRemainingInStage يوماً قبل الحجب الشامل.",
                    daysUntilNextPhase = daysRemainingInStage
                )
            }
            else -> {
                // Base Restricted: After Day 80
                SubscriptionDetails(
                    tier = SubscriptionTier.FREE_TRIAL,
                    phase = SubscriptionPhase.BASE_RESTRICTED,
                    isTrial = true,
                    isPaid = false,
                    trialDaysRemaining = 0L,
                    daysPastTrial = daysElapsedSinceExpired,
                    daysPastPaidExpiry = 0L,
                    trialProgressRatio = 0.0f,
                    alertTitle = "🔒 تم حجب المزايا المتقدمة بالكامل",
                    alertMessage = "انتهت فترة التجربة والتدرج الزمني. اشترك في إحدى الباقات للاستمتاع بكافة مزايا إدارة الأعمال.",
                    daysUntilNextPhase = 0L
                )
            }
        }
    }

    fun checkFeatureAccess(user: UserEntity?, feature: AppFeature): FeatureAccessDecision {
        val sub = evaluateSubscription(user)

        // 1. Paid Active tier checks
        if (sub.phase == SubscriptionPhase.PAID_ACTIVE) {
            val isTierAllowed = when (feature.minimumTier) {
                SubscriptionTier.FREE_TRIAL -> true
                SubscriptionTier.SILVER_TRADER -> sub.tier == SubscriptionTier.SILVER_TRADER || sub.tier == SubscriptionTier.GOLD_ENTERPRISE || sub.tier == SubscriptionTier.DIAMOND_VIP
                SubscriptionTier.GOLD_ENTERPRISE -> sub.tier == SubscriptionTier.GOLD_ENTERPRISE || sub.tier == SubscriptionTier.DIAMOND_VIP
                SubscriptionTier.DIAMOND_VIP -> sub.tier == SubscriptionTier.DIAMOND_VIP
            }
            return if (isTierAllowed) {
                FeatureAccessDecision(isAllowed = true, feature = feature, currentPhase = sub.phase)
            } else {
                FeatureAccessDecision(
                    isAllowed = false,
                    feature = feature,
                    currentPhase = sub.phase,
                    restrictionMessage = "هذه الميزة تتطلب الترقية إلى باقة '${feature.minimumTier.label}' أو أعلى.",
                    suggestedTier = feature.minimumTier
                )
            }
        }

        // 2. Active 60-day Free Trial: 100% UNRESTRICTED ACCESS
        if (sub.phase == SubscriptionPhase.TRIAL_ACTIVE) {
            return FeatureAccessDecision(
                isAllowed = true,
                feature = feature,
                currentPhase = sub.phase
            )
        }

        // 3. Stage 1 Grace (Days 61..70): Locks AI + Google Drive Cloud Sync
        if (sub.phase == SubscriptionPhase.GRACE_WARNING_STAGE_1) {
            return when (feature) {
                AppFeature.GEMINI_AI_AND_OCR -> FeatureAccessDecision(
                    isAllowed = false,
                    feature = feature,
                    currentPhase = sub.phase,
                    restrictionMessage = "تم حجب ميزات المحاسب الذكي وفحص الموازين بالذكاء الاصطناعي لانتهاء فترة الـ 60 يوماً المجانية. اشترك في باقة VIP للمتابعة.",
                    suggestedTier = SubscriptionTier.DIAMOND_VIP
                )
                AppFeature.GOOGLE_DRIVE_CLOUD_BACKUP -> FeatureAccessDecision(
                    isAllowed = false,
                    feature = feature,
                    currentPhase = sub.phase,
                    restrictionMessage = "تم حجب المزامنة السحابية التلقائية في Google Drive. يرجى الترقية إلى باقة VIP.",
                    suggestedTier = SubscriptionTier.DIAMOND_VIP
                )
                else -> FeatureAccessDecision(isAllowed = true, feature = feature, currentPhase = sub.phase)
            }
        }

        // 4. Stage 2 Grace (Days 71..80): Locks AI + Google Drive + Excel/PDF Export + Secret Vault Creation + Complaints
        if (sub.phase == SubscriptionPhase.GRACE_WARNING_STAGE_2) {
            return when (feature) {
                AppFeature.GEMINI_AI_AND_OCR,
                AppFeature.GOOGLE_DRIVE_CLOUD_BACKUP,
                AppFeature.ADVANCED_EXCEL_PDF_EXPORT,
                AppFeature.SECRET_SUB_ACCOUNTS_AND_VAULTS,
                AppFeature.FINANCIAL_COMPLAINTS_AND_AUDIT -> FeatureAccessDecision(
                    isAllowed = false,
                    feature = feature,
                    currentPhase = sub.phase,
                    restrictionMessage = "تم حجب '${feature.titleAr}' ضمن المرحلة الثانية للتدرج بعد انتهاء التجربة المجانية.",
                    suggestedTier = feature.minimumTier
                )
                else -> FeatureAccessDecision(isAllowed = true, feature = feature, currentPhase = sub.phase)
            }
        }

        // 5. Base Restricted or Paid Expired: Locks all non-basic features
        return if (feature == AppFeature.BASIC_ACCOUNTING_AND_CHATS) {
            FeatureAccessDecision(isAllowed = true, feature = feature, currentPhase = sub.phase)
        } else {
            FeatureAccessDecision(
                isAllowed = false,
                feature = feature,
                currentPhase = sub.phase,
                restrictionMessage = "تم حجب '${feature.titleAr}' بالكامل. اشترك الآن في باقة '${feature.minimumTier.label}' لمواصلة الاستخدام.",
                suggestedTier = feature.minimumTier
            )
        }
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        } catch (e: Exception) {
            "قريباً"
        }
    }
}
