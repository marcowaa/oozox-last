package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.data.model.DailyAccountingRecordEntity
import com.example.data.model.HiddenProfileEntity
import com.example.data.model.WorkGroupEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AppNavDestination(val key: String) {
    CHATS("nav.chats"),
    PROJECTS("nav.projects"),
    BUSINESS("nav.business"),
    INVOICES("nav.invoices"),
    GOLD("nav.gold"),
    STORE("nav.store"),
    ADMIN("nav.admin"),
    PROFILE("nav.profile"),
    ACCOUNTING_HUB("nav.accounting_hub");

    fun label(lang: AppLanguage = Translations.activeLanguage(null)): String =
        Translations.get(key, lang)
}

enum class HubModalType(val key: String) {
    INVOICE_COMPLAINTS("nav.modal_disputes"),
    RETROACTIVE_COMPLAINT("nav.modal_retro"),
    DEFICIT_INSPECTOR("nav.modal_deficit"),
    ADD_RECORD("nav.modal_add_record"),
    SECRET_VAULT_AUTH("nav.modal_unlock_vault");

    fun label(lang: AppLanguage = Translations.activeLanguage(null)): String =
        Translations.get(key, lang)
}

sealed class SuggestedAssistantAction {
    abstract val label: String

    data class OpenHubSection(val sectionIndex: Int, override val label: String) : SuggestedAssistantAction()
    data class OpenHubModal(val modalType: HubModalType, override val label: String) : SuggestedAssistantAction()
    data class OpenAppScreen(val destination: AppNavDestination, override val label: String) : SuggestedAssistantAction()
    data class ExportReport(val format: String, override val label: String) : SuggestedAssistantAction()
}

/**
 * High-level Accounting Voice Command representation.
 */
sealed class AccountingVoiceCommand {
    data class RecordTransaction(
        val parsedVoiceRecord: ParsedVoiceAccountingRecord,
        val candidateEntity: DailyAccountingRecordEntity,
        val spokenConfirmation: String
    ) : AccountingVoiceCommand()

    data class BalanceQuery(
        val title: String,
        val answer: String,
        val details: String,
        val spokenReply: String
    ) : AccountingVoiceCommand()

    data class DeficitQuery(
        val title: String,
        val answer: String,
        val totalDeficit: Double,
        val complaintsCount: Int,
        val spokenReply: String,
        val suggestedAction: SuggestedAssistantAction? = null
    ) : AccountingVoiceCommand()

    data class SectionNavigation(
        val sectionIndex: Int,
        val sectionTitle: String,
        val spokenReply: String
    ) : AccountingVoiceCommand()

    data class OpenHubModal(
        val modalType: HubModalType,
        val title: String,
        val spokenReply: String
    ) : AccountingVoiceCommand()

    data class OpenAppScreen(
        val destination: AppNavDestination,
        val title: String,
        val spokenReply: String
    ) : AccountingVoiceCommand()

    data class ExportReport(
        val format: String,
        val message: String,
        val spokenReply: String
    ) : AccountingVoiceCommand()

    data class ExplainOrAnswer(
        val title: String,
        val answer: String,
        val spokenReply: String,
        val suggestedAction: SuggestedAssistantAction? = null
    ) : AccountingVoiceCommand()

    data class SmartAssistantAdvice(
        val question: String,
        val answer: String,
        val spokenReply: String,
        val suggestedAction: SuggestedAssistantAction? = null
    ) : AccountingVoiceCommand()
}

/**
 * Dedicated Speech-to-Text & executive command/explanation engine for Accounting & App Navigation.
 */
class AccountingSpeechToTextEngine(private val context: Context) {

    companion object {
        private const val TAG = "AccountingSpeechEngine"

        fun speechLocaleFor(language: AppLanguage): String = when (language) {
            AppLanguage.ARABIC -> "ar-EG"
            AppLanguage.ENGLISH -> "en-US"
            AppLanguage.FRENCH -> "fr-FR"
            AppLanguage.SPANISH -> "es-ES"
            AppLanguage.TURKISH -> "tr-TR"
            AppLanguage.GERMAN -> "de-DE"
            AppLanguage.CHINESE -> "zh-CN"
            AppLanguage.HINDI -> "hi-IN"
            AppLanguage.RUSSIAN -> "ru-RU"
            AppLanguage.JAPANESE -> "ja-JP"
            AppLanguage.PORTUGUESE -> "pt-BR"
            AppLanguage.ITALIAN -> "it-IT"
            AppLanguage.URDU -> "ur-PK"
            AppLanguage.PERSIAN -> "fa-IR"
        }

        private fun normalizeArabic(input: String): String =
            input.trim()
                .lowercase(Locale.getDefault())
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي")

        /**
         * Pure, testable command & explanation interpreter with live financial awareness across all 14 languages.
         */
        fun interpretCommand(
            rawText: String,
            activeProfile: HiddenProfileEntity? = null,
            records: List<DailyAccountingRecordEntity>,
            groups: List<WorkGroupEntity>,
            unifiedSummary: UnifiedBusinessAccountingSummary? = null,
            pendingComplaintsCount: Int = 0,
            language: AppLanguage = Translations.activeLanguage(null)
        ): AccountingVoiceCommand {
            val trimmed = rawText.trim()
            val norm = normalizeArabic(trimmed)

            val summary = SimpleExcelEngine.computeSummary(
                records = records,
                initialCapital = activeProfile?.initialCapital ?: 1500000.0
            )
            val netBalance = (activeProfile?.initialCapital ?: 1500000.0) + summary.netCashflow
            val totalDeficit = summary.totalDeficit
            val openDeficitCount = records.count { it.deficitAmount > 0.0 }

            val hasPureQuestionWord = norm.contains("ما هو") || norm.contains("ما هي") ||
                norm.contains("ماهو") || norm.contains("ماهي") || norm.contains("what is")
            val mentionsLiveBalance = norm.contains("رصيد") || norm.contains("السيوله") ||
                norm.contains("معانا كام") || norm.contains("في الخزنه") ||
                norm.contains("balance") || norm.contains("cashflow") || norm.contains("treasury")

            val wantsExplanation = listOf(
                "اشرح", "فهمني", "شرح", "يعني ايه", "يعنى ايه", "ما هو", "ما هي", "ماهو", "ماهي",
                "ايه هو", "ايه هي", "كيف ", "ازاي", "إزاي", "علمني", "وظيفه", "فايده", "فائده",
                "explain", "what is", "how to", "how does", "guide", "help me understand"
            ).any { norm.contains(it) } && !(hasPureQuestionWord && mentionsLiveBalance)

            val wantsOpen = listOf(
                "افتح", "اعرض", "وريني", "ادخل", "انتقل", "اذهب", "روح", "شغل", "هات", "وديني",
                "open", "go to", "show", "navigate", "launch", "switch to"
            ).any { norm.contains(it) }

            // 1. Explanations (with real live figures & 1-tap module shortcut)
            if (wantsExplanation) {
                return buildExplanationResponse(
                    norm = norm,
                    language = language,
                    activeProfile = activeProfile,
                    summary = summary,
                    netBalance = netBalance,
                    unifiedSummary = unifiedSummary,
                    groupsCount = groups.size,
                    pendingComplaintsCount = pendingComplaintsCount,
                    openDeficitCount = openDeficitCount
                )
            }

            // 2. Modal / Sub-tool Open Intent inside Accounting Hub
            if (norm.contains("شكاوي الفواتير") || norm.contains("نزاعات") || norm.contains("شكوي فاتوره") || norm.contains("invoice dispute")) {
                return AccountingVoiceCommand.OpenHubModal(
                    modalType = HubModalType.INVOICE_COMPLAINTS,
                    title = HubModalType.INVOICE_COMPLAINTS.label(language),
                    spokenReply = Translations.get("speech.open_disputes_reply", language)
                )
            }
            if (norm.contains("باثر رجعي") || norm.contains("اثر رجعي") || norm.contains("تسويه رجعيه") || norm.contains("شكوي رجعيه") || norm.contains("retro")) {
                return AccountingVoiceCommand.OpenHubModal(
                    modalType = HubModalType.RETROACTIVE_COMPLAINT,
                    title = HubModalType.RETROACTIVE_COMPLAINT.label(language),
                    spokenReply = Translations.get("speech.open_retro_reply", language)
                )
            }
            if (norm.contains("فحص العجز") || norm.contains("تدقيق العجز") || norm.contains("inspect deficit") || norm.contains("audit deficit")) {
                return AccountingVoiceCommand.OpenHubModal(
                    modalType = HubModalType.DEFICIT_INSPECTOR,
                    title = HubModalType.DEFICIT_INSPECTOR.label(language),
                    spokenReply = Translations.get("speech.open_deficit_reply", language)
                )
            }

            // 3. Cross-App Section Open Intent
            if (wantsOpen || norm.startsWith("قسم ")) {
                detectAppDestination(norm)?.let { dest ->
                    if (dest != AppNavDestination.ACCOUNTING_HUB) {
                        val destLabel = dest.label(language)
                        return AccountingVoiceCommand.OpenAppScreen(
                            destination = dest,
                            title = destLabel,
                            spokenReply = Translations.format("speech.open_dest_reply_fmt", language, "dest" to destLabel)
                        )
                    }
                }
            }
            if (norm == "الذهب" || norm == "قسم الذهب" || norm == "gold" || norm == "open gold") {
                val goldLabel = AppNavDestination.GOLD.label(language)
                return AccountingVoiceCommand.OpenAppScreen(
                    destination = AppNavDestination.GOLD,
                    title = goldLabel,
                    spokenReply = Translations.format("speech.open_dest_reply_fmt", language, "dest" to goldLabel)
                )
            }

            // 4. Hub Tab Navigation Intent (-1..4)
            detectHubSection(norm, language)?.let { nav ->
                return nav
            }

            // 5. Export Commands
            if (norm.contains("اكسل موحد") || norm.contains("اكسل شامل") || norm.contains("شيت موحد") || norm.contains("unified excel")) {
                val msg = Translations.get("speech.export_unified_msg", language)
                return AccountingVoiceCommand.ExportReport(
                    format = "EXCEL_UNIFIED",
                    message = msg,
                    spokenReply = msg
                )
            }
            if (norm.contains("اكسل") || norm.contains("excel") || norm.contains("شيت")) {
                val msg = Translations.get("speech.export_excel_msg", language)
                return AccountingVoiceCommand.ExportReport(
                    format = "EXCEL",
                    message = msg,
                    spokenReply = msg
                )
            }
            if (norm.contains("pdf") || norm.contains("بي دي اف") || norm.contains("طباعه")) {
                val msg = Translations.get("speech.export_pdf_msg", language)
                return AccountingVoiceCommand.ExportReport(
                    format = "PDF",
                    message = msg,
                    spokenReply = msg
                )
            }

            // 6. Live Balance & Cashflow Query
            if (norm.contains("كم الرصيد") || norm.contains("رصيد") || norm.contains("السيوله") ||
                norm.contains("صافي") || norm.contains("معانا كام") || norm.contains("في الخزنه") ||
                norm.contains("balance") || norm.contains("cashflow") || norm.contains("treasury")
            ) {
                val formattedBal = GoldUtils.formatCurrency(netBalance, language)
                val detailsStr = Translations.format(
                    "speech.balance_details_fmt",
                    language,
                    "inflow" to GoldUtils.formatCurrency(summary.totalRevenue + summary.totalIncoming, language),
                    "outflow" to GoldUtils.formatCurrency(summary.totalOutgoing, language),
                    "deficit" to GoldUtils.formatCurrency(totalDeficit, language)
                )
                return AccountingVoiceCommand.BalanceQuery(
                    title = Translations.get("speech.balance_title", language),
                    answer = formattedBal,
                    details = detailsStr,
                    spokenReply = Translations.format("speech.balance_spoken_fmt", language, "bal" to formattedBal)
                )
            }

            // 7. Live Deficit & Disputes Query
            if (norm.contains("عجز") || norm.contains("شكاو") || norm.contains("شكوي") ||
                norm.contains("فروقات") || norm.contains("فروق") || norm.contains("deficit") || norm.contains("dispute")
            ) {
                val answerStr = if (totalDeficit > 0.0 || pendingComplaintsCount > 0) {
                    Translations.format(
                        "speech.deficit_ans_fmt",
                        language,
                        "total" to GoldUtils.formatCurrency(totalDeficit, language),
                        "count" to openDeficitCount,
                        "pending" to pendingComplaintsCount
                    )
                } else {
                    Translations.get("speech.deficit_zero", language)
                }
                return AccountingVoiceCommand.DeficitQuery(
                    title = Translations.get("speech.deficit_title", language),
                    answer = answerStr,
                    totalDeficit = totalDeficit,
                    complaintsCount = summary.totalComplaints + pendingComplaintsCount,
                    spokenReply = answerStr,
                    suggestedAction = SuggestedAssistantAction.OpenHubModal(
                        modalType = if (pendingComplaintsCount > 0) HubModalType.INVOICE_COMPLAINTS else HubModalType.DEFICIT_INSPECTOR,
                        label = Translations.get("speech.inspect_now", language)
                    )
                )
            }

            // 8. Accounting Entry Recording (amount / weight / verbs)
            val treasuryScope = activeProfile?.name ?: Translations.get("acct.main_treasury", language)
            val parsed = SmartVoiceAccountingParser.parseVoiceAccountingText(trimmed, treasuryScope)
            if (parsed.amount > 0.0 || parsed.weightGrams > 0.0 ||
                norm.contains("سجل") || norm.contains("مصروف") || norm.contains("قيد") ||
                norm.contains("قبض") || norm.contains("صرف") || norm.contains("ادفع") || norm.contains("دفعه")
            ) {
                if (parsed.amount == 0.0 && (norm == "قيد جديد" || norm == "اضافه قيد" || norm == "افتح قيد")) {
                    return AccountingVoiceCommand.OpenHubModal(
                        modalType = HubModalType.ADD_RECORD,
                        title = HubModalType.ADD_RECORD.label(language),
                        spokenReply = Translations.get("speech.open_new_entry_reply", language)
                    )
                }

                val targetGroup = groups.firstOrNull()
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val isCredit = parsed.direction == "CREDIT"

                val candidateEntity = DailyAccountingRecordEntity(
                    id = UUID.randomUUID().toString(),
                    groupId = targetGroup?.id ?: "general_hub",
                    subGroupId = targetGroup?.companyName ?: Translations.get("acct.hub_title", language),
                    profileId = activeProfile?.id,
                    entryDate = todayStr,
                    staffId = "ai_voice_stt",
                    staffName = if (parsed.counterPartyName.isNotBlank()) {
                        parsed.counterPartyName
                    } else {
                        activeProfile?.name ?: Translations.get("acct.assistant_title", language)
                    },
                    staffRole = Translations.get("speech.smart_accountant_role", language),
                    dailyRevenue = if (isCredit && parsed.operationCategory != "مصروفات") parsed.amount else 0.0,
                    incomingAmount = if (isCredit) parsed.amount else 0.0,
                    outgoingAmount = if (!isCredit) parsed.amount else 0.0,
                    deficitAmount = 0.0,
                    complaintsCount = 0,
                    notes = Translations.format(
                        "speech.voice_tag_fmt",
                        language,
                        "raw" to parsed.rawSpokenText,
                        "tag" to parsed.suggestedTag
                    ),
                    isEncrypted = true,
                    timestamp = System.currentTimeMillis()
                )

                val reviewVoiceReadback = Translations.format(
                    if (isCredit) "speech.draft_inflow_fmt" else "speech.draft_outflow_fmt",
                    language,
                    "amount" to GoldUtils.formatCurrency(parsed.amount, language),
                    "tag" to parsed.suggestedTag
                )

                return AccountingVoiceCommand.RecordTransaction(
                    parsedVoiceRecord = parsed,
                    candidateEntity = candidateEntity,
                    spokenConfirmation = reviewVoiceReadback
                )
            }

            // 9. Executive Financial Summary & Live Smart Assistant Advice
            val liveSummary = buildLiveExecutiveBriefing(
                language = language,
                summary = summary,
                netBalance = netBalance,
                unifiedSummary = unifiedSummary,
                groups = groups,
                records = records,
                pendingComplaintsCount = pendingComplaintsCount
            )
            return AccountingVoiceCommand.SmartAssistantAdvice(
                question = trimmed,
                answer = liveSummary,
                spokenReply = liveSummary.lineSequence().firstOrNull().orEmpty(),
                suggestedAction = SuggestedAssistantAction.OpenHubSection(
                    sectionIndex = 1,
                    label = Translations.get("speech.open_journal", language)
                )
            )
        }

        private fun detectAppDestination(norm: String): AppNavDestination? = when {
            norm.contains("ذهب") || norm.contains("فضه") || norm.contains("مصوغ") || norm.contains("عيار") || norm.contains("gold") -> AppNavDestination.GOLD
            norm.contains("مشروع") || norm.contains("مشاريع") || norm.contains("استثمار") || norm.contains("project") -> AppNavDestination.PROJECTS
            norm.contains("اداره الاعمال") || norm.contains("كاشير") || norm.contains("نقطه بيع") || norm.contains("business") || norm.contains("pos") -> AppNavDestination.BUSINESS
            norm.contains("فواتير") || norm.contains("فاتوره") || norm.contains("invoice") || norm.contains("transaction") -> AppNavDestination.INVOICES
            norm.contains("متجر") || norm.contains("معرض") || norm.contains("منتجات") || norm.contains("store") || norm.contains("shop") -> AppNavDestination.STORE
            norm.contains("محادثات") || norm.contains("دردش") || norm.contains("شات") || norm.contains("chat") -> AppNavDestination.CHATS
            norm.contains("اداره النظام") || norm.contains("لوحه الاداره") || norm.contains("الادمن") || norm.contains("admin") -> AppNavDestination.ADMIN
            norm.contains("حسابي") || norm.contains("ملفي") || norm.contains("البروفايل") || norm.contains("profile") -> AppNavDestination.PROFILE
            else -> null
        }

        private fun detectHubSection(norm: String, lang: AppLanguage): AccountingVoiceCommand.SectionNavigation? {
            val secKey = when {
                norm.contains("اليوميه") || norm.contains("الدفتر") || norm.contains("القيود") || norm.contains("journal") || norm.contains("ledger") ->
                    1 to "sec_ledger"
                norm.contains("الشركات") || norm.contains("المجموعات") || norm.contains("الاقسام") || norm.contains("الفروع") || norm.contains("companies") || norm.contains("entities") ->
                    2 to "sec_groups"
                norm.contains("الموظفين") || norm.contains("الورديات") || norm.contains("الحضور") || norm.contains("staff") || norm.contains("shifts") ->
                    3 to "sec_staff"
                norm.contains("الخزائن") || norm.contains("الخزينه السريه") || norm.contains("المخفيه") || norm.contains("vault") ->
                    0 to "sec_vaults"
                norm.contains("نسخ") || norm.contains("سحاب") || norm.contains("مزامن") || norm.contains("cloud") || norm.contains("backup") ->
                    4 to "sec_backup"
                norm.contains("الرئيسي") || norm.contains("المؤشرات") || norm.contains("داشبورد") || norm.contains("dashboard") || norm.contains("overview") ->
                    -1 to "sec_dashboard"
                else -> null
            } ?: return null

            val (index, key) = secKey
            val sectionTitle = AccountingI18n.get(key, lang)
            return AccountingVoiceCommand.SectionNavigation(
                sectionIndex = index,
                sectionTitle = sectionTitle,
                spokenReply = Translations.format("speech.sec_opened_fmt", lang, "section" to sectionTitle)
            )
        }

        private fun buildExplanationResponse(
            norm: String,
            language: AppLanguage,
            activeProfile: HiddenProfileEntity?,
            summary: CompanyAccountingSummary,
            netBalance: Double,
            unifiedSummary: UnifiedBusinessAccountingSummary?,
            groupsCount: Int,
            pendingComplaintsCount: Int,
            openDeficitCount: Int
        ): AccountingVoiceCommand.ExplainOrAnswer {
            return when {
                norm.contains("خزينه") || norm.contains("خزائن") || norm.contains("مخفي") || norm.contains("سري") || norm.contains("vault") -> {
                    val ans = Translations.get("speech.exp_vaults_ans", language)
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = Translations.get("speech.exp_vaults_title", language),
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenHubSection(0, Translations.get("speech.exp_vaults_btn", language))
                    )
                }
                norm.contains("رجعي") || norm.contains("تسويه") || norm.contains("عهد") || norm.contains("retro") -> {
                    val ans = Translations.get("speech.exp_retro_ans", language)
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = Translations.get("speech.exp_retro_title", language),
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenHubModal(
                            HubModalType.RETROACTIVE_COMPLAINT,
                            Translations.get("speech.exp_retro_btn", language)
                        )
                    )
                }
                norm.contains("عجز") || norm.contains("شكوي") || norm.contains("شكاو") || norm.contains("نزاع") || norm.contains("deficit") || norm.contains("dispute") -> {
                    val ans = Translations.format(
                        "speech.exp_deficit_ans_fmt",
                        language,
                        "total" to GoldUtils.formatCurrency(summary.totalDeficit, language),
                        "count" to openDeficitCount,
                        "pending" to pendingComplaintsCount
                    )
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = Translations.get("speech.deficit_title", language),
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenHubModal(
                            HubModalType.INVOICE_COMPLAINTS,
                            Translations.get("speech.exp_deficit_btn", language)
                        )
                    )
                }
                norm.contains("يوميه") || norm.contains("قيد") || norm.contains("دفتر") || norm.contains("journal") || norm.contains("ledger") -> {
                    val ans = Translations.format(
                        "speech.exp_journal_ans_fmt",
                        language,
                        "inflow" to GoldUtils.formatCurrency(summary.totalRevenue + summary.totalIncoming, language),
                        "outflow" to GoldUtils.formatCurrency(summary.totalOutgoing, language),
                        "count" to summary.recordsCount
                    )
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = Translations.get("speech.exp_journal_title", language),
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenHubSection(1, Translations.get("speech.open_journal", language))
                    )
                }
                norm.contains("موظف") || norm.contains("وردي") || norm.contains("حضور") || norm.contains("شفت") || norm.contains("staff") || norm.contains("shift") -> {
                    val ans = Translations.get("speech.exp_staff_ans", language)
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = Translations.get("speech.exp_staff_title", language),
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenHubSection(3, Translations.get("speech.exp_staff_btn", language))
                    )
                }
                norm.contains("شرك") || norm.contains("مجموع") || norm.contains("فرع") || norm.contains("اقسام") || norm.contains("entities") || norm.contains("compan") -> {
                    val ans = Translations.format("speech.exp_entities_ans_fmt", language, "count" to groupsCount)
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = Translations.get("speech.exp_entities_title", language),
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenHubSection(2, Translations.get("speech.exp_entities_btn", language))
                    )
                }
                norm.contains("اكسل") || norm.contains("excel") || norm.contains("شيت") || norm.contains("pdf") -> {
                    val ans = Translations.get("speech.exp_reports_ans", language)
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = Translations.get("speech.exp_reports_title", language),
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.ExportReport(
                            "EXCEL_UNIFIED",
                            Translations.get("speech.export_excel_btn", language)
                        )
                    )
                }
                norm.contains("ذهب") || norm.contains("gold") -> {
                    val ans = Translations.get("speech.exp_gold_ans", language)
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = AppNavDestination.GOLD.label(language),
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenAppScreen(
                            AppNavDestination.GOLD,
                            Translations.get("speech.open_gold_btn", language)
                        )
                    )
                }
                else -> {
                    val combinedStr = unifiedSummary?.let {
                        " " + Translations.format(
                            "speech.exec_combined_suffix_fmt",
                            language,
                            "net" to GoldUtils.formatCurrency(it.combinedNetPosition, language)
                        )
                    } ?: ""
                    val scope = activeProfile?.name ?: Translations.get("acct.main_treasury", language)
                    val ans = Translations.format(
                        "speech.exec_summary_body_fmt",
                        language,
                        "scope" to scope,
                        "net" to GoldUtils.formatCurrency(netBalance, language),
                        "inflow" to GoldUtils.formatCurrency(summary.totalRevenue + summary.totalIncoming, language),
                        "outflow" to GoldUtils.formatCurrency(summary.totalOutgoing, language),
                        "deficit" to GoldUtils.formatCurrency(summary.totalDeficit, language),
                        "pending" to pendingComplaintsCount,
                        "combined" to combinedStr
                    )
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = Translations.get("speech.exec_summary_title", language),
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenHubSection(-1, Translations.get("speech.overview_btn", language))
                    )
                }
            }
        }

        private fun buildLiveExecutiveBriefing(
            language: AppLanguage,
            summary: CompanyAccountingSummary,
            netBalance: Double,
            unifiedSummary: UnifiedBusinessAccountingSummary?,
            groups: List<WorkGroupEntity>,
            records: List<DailyAccountingRecordEntity>,
            pendingComplaintsCount: Int
        ): String {
            val topEntity = records.groupBy { it.subGroupId ?: it.staffName }
                .maxByOrNull { (_, list) -> list.sumOf { it.dailyRevenue + it.incomingAmount } }
                ?.key
            return buildString {
                appendLine(
                    Translations.format(
                        "speech.briefing_net_fmt",
                        language,
                        "net" to GoldUtils.formatCurrency(netBalance, language)
                    )
                )
                appendLine(
                    Translations.format(
                        "speech.briefing_inflow_fmt",
                        language,
                        "amt" to GoldUtils.formatCurrency(summary.totalRevenue + summary.totalIncoming, language)
                    )
                )
                appendLine(
                    Translations.format(
                        "speech.briefing_outflow_fmt",
                        language,
                        "amt" to GoldUtils.formatCurrency(summary.totalOutgoing, language)
                    )
                )
                if (summary.totalDeficit > 0 || pendingComplaintsCount > 0) {
                    appendLine(
                        Translations.format(
                            "speech.briefing_alert_fmt",
                            language,
                            "deficit" to GoldUtils.formatCurrency(summary.totalDeficit, language),
                            "pending" to pendingComplaintsCount
                        )
                    )
                }
                if (!topEntity.isNullOrBlank()) {
                    appendLine(
                        Translations.format(
                            "speech.briefing_top_fmt",
                            language,
                            "entity" to topEntity,
                            "count" to groups.size
                        )
                    )
                }
                unifiedSummary?.let {
                    append(
                        Translations.format(
                            "speech.briefing_unified_fmt",
                            language,
                            "net" to GoldUtils.formatCurrency(it.combinedNetPosition, language)
                        )
                    )
                }
            }.trim()
        }
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0.1f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var onPartialCallback: ((String) -> Unit)? = null
    private var onFinalCallback: ((String) -> Unit)? = null

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createListener())
                    }
                } else {
                    Log.w(TAG, "SpeechRecognizer is not natively available on this Android device.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize SpeechRecognizer", e)
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _errorMessage.value = null
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.08f, 1.0f)
                _rmsLevel.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> Translations.getFromContext(context, "speech.err_audio")
                    SpeechRecognizer.ERROR_CLIENT -> Translations.getFromContext(context, "speech.err_client")
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Translations.getFromContext(context, "speech.err_perm")
                    SpeechRecognizer.ERROR_NETWORK -> Translations.getFromContext(context, "speech.err_network")
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Translations.getFromContext(context, "speech.err_timeout")
                    SpeechRecognizer.ERROR_NO_MATCH -> Translations.getFromContext(context, "speech.err_nomatch")
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Translations.getFromContext(context, "speech.err_busy")
                    SpeechRecognizer.ERROR_SERVER -> Translations.getFromContext(context, "speech.err_server")
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Translations.getFromContext(context, "speech.err_speech_timeout")
                    else -> Translations.formatFromContext(context, "speech.err_generic_fmt", "code" to error)
                }
                Log.w(TAG, "Speech error code $error: $msg")
                _errorMessage.value = msg
                _isListening.value = false
                _rmsLevel.value = 0.1f
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    _spokenText.value = text
                    _partialText.value = text
                    onFinalCallback?.invoke(text)
                }
                _isListening.value = false
                _rmsLevel.value = 0.1f
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    _partialText.value = text
                    onPartialCallback?.invoke(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun isAvailable(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Exception) {
            false
        }
    }

    fun startListening(
        languageCode: String? = null,
        onPartial: ((String) -> Unit)? = null,
        onFinal: ((String) -> Unit)? = null
    ) {
        onPartialCallback = onPartial
        onFinalCallback = onFinal
        _spokenText.value = ""
        _partialText.value = ""
        _errorMessage.value = null

        val activeLang = Translations.activeLanguage(context)
        val effectiveLocale = languageCode?.takeIf { it.isNotBlank() } ?: speechLocaleFor(activeLang)
        val speakPrompt = Translations.get("speech.prompt_speak_now", activeLang)

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, effectiveLocale)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, effectiveLocale)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, effectiveLocale)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, speakPrompt)
                }

                _isListening.value = true
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening", e)
                _errorMessage.value = Translations.get("speech.err_start_failed", activeLang)
                _isListening.value = false
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech", e)
            }
            _isListening.value = false
            _rmsLevel.value = 0.1f
        }
    }

    fun cancelListening() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error canceling speech", e)
            }
            _isListening.value = false
            _spokenText.value = ""
            _partialText.value = ""
            _rmsLevel.value = 0.1f
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying speech recognizer", e)
            }
            _isListening.value = false
        }
    }

    fun interpretAccountingVoiceCommand(
        rawText: String,
        activeProfile: HiddenProfileEntity?,
        records: List<DailyAccountingRecordEntity>,
        groups: List<WorkGroupEntity>,
        unifiedSummary: UnifiedBusinessAccountingSummary? = null,
        pendingComplaintsCount: Int = 0,
        language: AppLanguage = Translations.activeLanguage(context)
    ): AccountingVoiceCommand = interpretCommand(
        rawText = rawText,
        activeProfile = activeProfile,
        records = records,
        groups = groups,
        unifiedSummary = unifiedSummary,
        pendingComplaintsCount = pendingComplaintsCount,
        language = language
    )
}
