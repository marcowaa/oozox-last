package com.example.util

import com.example.data.model.SubscriptionTier
import com.example.data.model.UserEntity

enum class SubscriptionPhase(
    val titleKey: String,
    val descriptionKey: String,
    val alertLevel: String // "SUCCESS", "INFO", "WARNING", "DANGER"
) {
    TRIAL_ACTIVE(
        titleKey = "sub.phase_trial_title",
        descriptionKey = "sub.phase_trial_desc",
        alertLevel = "SUCCESS"
    ),
    GRACE_WARNING_STAGE_1(
        titleKey = "sub.phase_stage1_title",
        descriptionKey = "sub.phase_stage1_desc",
        alertLevel = "WARNING"
    ),
    GRACE_WARNING_STAGE_2(
        titleKey = "sub.phase_stage2_title",
        descriptionKey = "sub.phase_stage2_desc",
        alertLevel = "DANGER"
    ),
    BASE_RESTRICTED(
        titleKey = "sub.phase_restricted_title",
        descriptionKey = "sub.phase_restricted_desc",
        alertLevel = "DANGER"
    ),
    PAID_ACTIVE(
        titleKey = "sub.phase_paid_active_title",
        descriptionKey = "sub.phase_paid_active_desc",
        alertLevel = "SUCCESS"
    ),
    PAID_EXPIRED(
        titleKey = "sub.phase_paid_expired_title",
        descriptionKey = "sub.phase_paid_expired_desc",
        alertLevel = "WARNING"
    );

    fun localizedTitle(language: AppLanguage = Translations.activeLanguage(null)): String =
        Translations.get(titleKey, language)

    fun localizedDescription(language: AppLanguage = Translations.activeLanguage(null)): String =
        Translations.get(descriptionKey, language)
}

enum class AppFeature(
    val titleKey: String,
    val descriptionKey: String,
    val minimumTier: SubscriptionTier,
    val iconName: String
) {
    GEMINI_AI_AND_OCR(
        titleKey = "sub.feat_ai_title",
        descriptionKey = "sub.feat_ai_desc",
        minimumTier = SubscriptionTier.DIAMOND_VIP,
        iconName = "Psychology"
    ),
    CONTEXTUAL_AI_CHAT_TRANSLATION(
        titleKey = "sub.feat_translate_title",
        descriptionKey = "sub.feat_translate_desc",
        minimumTier = SubscriptionTier.SILVER_TRADER,
        iconName = "Translate"
    ),
    GOOGLE_DRIVE_CLOUD_BACKUP(
        titleKey = "sub.feat_backup_title",
        descriptionKey = "sub.feat_backup_desc",
        minimumTier = SubscriptionTier.DIAMOND_VIP,
        iconName = "CloudSync"
    ),
    ADVANCED_EXCEL_PDF_EXPORT(
        titleKey = "sub.feat_export_title",
        descriptionKey = "sub.feat_export_desc",
        minimumTier = SubscriptionTier.SILVER_TRADER,
        iconName = "FileDownload"
    ),
    SECRET_SUB_ACCOUNTS_AND_VAULTS(
        titleKey = "sub.feat_vaults_title",
        descriptionKey = "sub.feat_vaults_desc",
        minimumTier = SubscriptionTier.SILVER_TRADER,
        iconName = "Security"
    ),
    FINANCIAL_COMPLAINTS_AND_AUDIT(
        titleKey = "sub.feat_complaints_title",
        descriptionKey = "sub.feat_complaints_desc",
        minimumTier = SubscriptionTier.GOLD_ENTERPRISE,
        iconName = "FactCheck"
    ),
    WORKGROUPS_AND_RBAC(
        titleKey = "sub.feat_workgroups_title",
        descriptionKey = "sub.feat_workgroups_desc",
        minimumTier = SubscriptionTier.GOLD_ENTERPRISE,
        iconName = "Groups"
    ),
    BASIC_ACCOUNTING_AND_CHATS(
        titleKey = "sub.feat_basic_title",
        descriptionKey = "sub.feat_basic_desc",
        minimumTier = SubscriptionTier.FREE_TRIAL,
        iconName = "Forum"
    );

    fun localizedTitle(language: AppLanguage = Translations.activeLanguage(null)): String =
        Translations.get(titleKey, language)

    fun localizedDescription(language: AppLanguage = Translations.activeLanguage(null)): String =
        Translations.get(descriptionKey, language)
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

    fun evaluateSubscription(
        user: UserEntity?,
        language: AppLanguage = Translations.activeLanguage(null)
    ): SubscriptionDetails {
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
                alertTitle = SubscriptionPhase.TRIAL_ACTIVE.localizedTitle(language),
                alertMessage = Translations.get("sub.trial_default_msg", language),
                daysUntilNextPhase = 60L
            )
        }

        val now = System.currentTimeMillis()
        val currentTier = SubscriptionTier.entries.find { it.name == user.subscriptionTier } ?: SubscriptionTier.FREE_TRIAL
        val isPaidTier = currentTier != SubscriptionTier.FREE_TRIAL
        val tierLabel = currentTier.localizedLabel(language)
        val tierBadge = currentTier.localizedBadgeText(language)

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
                    alertTitle = Translations.format("sub.paid_active_full_fmt", language, "tier" to tierLabel, "badge" to tierBadge),
                    alertMessage = Translations.format("sub.paid_active_until_fmt", language, "date" to formatDate(user.subscriptionEndTimestamp, language), "days" to remainingPaidDays),
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
                    alertTitle = Translations.format("sub.paid_expired_fmt", language, "tier" to tierLabel, "days" to daysExpired),
                    alertMessage = Translations.get("sub.paid_expired_msg", language),
                    daysUntilNextPhase = 0L
                )
            }
        }

        // --- FREE TRIAL CALCULATION (60 Days + Gradual Restriction) ---
        val trialEnd = user.subscriptionEndTimestamp
        val trialTotalMs = (TRIAL_DURATION_DAYS * 24L * 3600L * 1000L).coerceAtLeast(1L)
        val diffToTrialEnd = trialEnd - now

        if (diffToTrialEnd > 0) {
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
                alertTitle = Translations.format("sub.trial_active_hero", language, "days" to daysRemaining),
                alertMessage = Translations.get("sub.trial_default_msg", language),
                daysUntilNextPhase = daysRemaining
            )
        }

        // Trial has expired, calculate days elapsed since expiration
        val elapsedSinceTrialExpiredMs = now - trialEnd
        val daysElapsedSinceExpired = (elapsedSinceTrialExpiredMs / (1000L * 3600 * 24)).coerceAtLeast(0L)

        return when {
            daysElapsedSinceExpired < GRACE_STAGE_1_DAYS -> {
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
                    alertTitle = Translations.format("sub.stage1_alert_fmt", language, "days" to daysRemainingInStage),
                    alertMessage = Translations.format("sub.stage1_full_msg_fmt", language, "days" to daysRemainingInStage),
                    daysUntilNextPhase = daysRemainingInStage
                )
            }
            daysElapsedSinceExpired < (GRACE_STAGE_1_DAYS + GRACE_STAGE_2_DAYS) -> {
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
                    alertTitle = Translations.format("sub.stage2_alert_fmt", language, "days" to daysRemainingInStage),
                    alertMessage = Translations.format("sub.stage2_full_msg_fmt", language, "days" to daysRemainingInStage),
                    daysUntilNextPhase = daysRemainingInStage
                )
            }
            else -> {
                SubscriptionDetails(
                    tier = SubscriptionTier.FREE_TRIAL,
                    phase = SubscriptionPhase.BASE_RESTRICTED,
                    isTrial = true,
                    isPaid = false,
                    trialDaysRemaining = 0L,
                    daysPastTrial = daysElapsedSinceExpired,
                    daysPastPaidExpiry = 0L,
                    trialProgressRatio = 0.0f,
                    alertTitle = SubscriptionPhase.BASE_RESTRICTED.localizedTitle(language),
                    alertMessage = Translations.get("sub.base_restricted_full_msg", language),
                    daysUntilNextPhase = 0L
                )
            }
        }
    }

    fun checkFeatureAccess(
        user: UserEntity?,
        feature: AppFeature,
        language: AppLanguage = Translations.activeLanguage(null)
    ): FeatureAccessDecision {
        val sub = evaluateSubscription(user, language)
        val featureName = feature.localizedTitle(language)
        val minTierName = feature.minimumTier.localizedLabel(language)

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
                    restrictionMessage = Translations.format("sub.tier_required_msg_fmt", language, "tier" to minTierName),
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
                    restrictionMessage = Translations.get("sub.gate_ai_msg", language),
                    suggestedTier = SubscriptionTier.DIAMOND_VIP
                )
                AppFeature.GOOGLE_DRIVE_CLOUD_BACKUP -> FeatureAccessDecision(
                    isAllowed = false,
                    feature = feature,
                    currentPhase = sub.phase,
                    restrictionMessage = Translations.get("sub.gate_backup_msg", language),
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
                    restrictionMessage = Translations.format("sub.gate_stage2_fmt", language, "feature" to featureName),
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
                restrictionMessage = Translations.format("sub.gate_restricted_fmt", language, "feature" to featureName, "tier" to minTierName),
                suggestedTier = feature.minimumTier
            )
        }
    }

    private fun formatDate(timestamp: Long, language: AppLanguage): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale(language.code))
            sdf.format(java.util.Date(timestamp))
        } catch (_: Exception) {
            Translations.get("sub.soon", language)
        }
    }
}
