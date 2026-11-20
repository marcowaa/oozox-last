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

enum class AppNavDestination(val titleAr: String, val titleEn: String) {
    CHATS("المحادثات", "Chats"),
    PROJECTS("المشاريع", "Projects"),
    BUSINESS("إدارة الأعمال", "Business"),
    INVOICES("الفواتير والقيود", "Invoices"),
    GOLD("الذهب والمعادن", "Gold Market"),
    STORE("المتجر", "Store"),
    ADMIN("لوحة الإدارة", "Admin"),
    PROFILE("حسابي", "Profile"),
    ACCOUNTING_HUB("المركز المحاسبي", "Accounting Hub");

    fun label(lang: AppLanguage): String =
        if (lang == AppLanguage.ARABIC || lang == AppLanguage.URDU || lang == AppLanguage.PERSIAN) titleAr else titleEn
}

enum class HubModalType(val titleAr: String, val titleEn: String) {
    INVOICE_COMPLAINTS("شكاوى الفواتير", "Invoice Disputes"),
    RETROACTIVE_COMPLAINT("تسوية رجعية", "Retroactive Claim"),
    DEFICIT_INSPECTOR("فحص العجز", "Deficit Audit"),
    ADD_RECORD("قيد يومي جديد", "New Entry"),
    SECRET_VAULT_AUTH("فتح الخزينة", "Unlock Vault");

    fun label(lang: AppLanguage): String =
        if (lang == AppLanguage.ARABIC || lang == AppLanguage.URDU || lang == AppLanguage.PERSIAN) titleAr else titleEn
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
        val format: String, // "EXCEL", "EXCEL_UNIFIED", or "PDF"
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

        private fun isRtlLang(lang: AppLanguage): Boolean =
            lang == AppLanguage.ARABIC || lang == AppLanguage.URDU || lang == AppLanguage.PERSIAN

        private fun normalizeArabic(input: String): String =
            input.trim()
                .lowercase(Locale.getDefault())
                .replace("أ", "ا")
                .replace("إ", "ا")
                .replace("آ", "ا")
                .replace("ة", "ه")
                .replace("ى", "ي")

        /**
         * Pure, testable command & explanation interpreter with live financial awareness.
         */
        fun interpretCommand(
            rawText: String,
            activeProfile: HiddenProfileEntity? = null,
            records: List<DailyAccountingRecordEntity>,
            groups: List<WorkGroupEntity>,
            unifiedSummary: UnifiedBusinessAccountingSummary? = null,
            pendingComplaintsCount: Int = 0,
            language: AppLanguage = AppLanguage.ARABIC
        ): AccountingVoiceCommand {
            val trimmed = rawText.trim()
            val norm = normalizeArabic(trimmed)
            val isAr = isRtlLang(language)

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
                    trimmed = trimmed,
                    norm = norm,
                    isAr = isAr,
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
                    spokenReply = if (isAr) "تم فتح نافذة شكاوى الفواتير والمراجعة." else "Opening invoice disputes."
                )
            }
            if (norm.contains("باثر رجعي") || norm.contains("اثر رجعي") || norm.contains("تسويه رجعيه") || norm.contains("شكوي رجعيه") || norm.contains("retro")) {
                return AccountingVoiceCommand.OpenHubModal(
                    modalType = HubModalType.RETROACTIVE_COMPLAINT,
                    title = HubModalType.RETROACTIVE_COMPLAINT.label(language),
                    spokenReply = if (isAr) "تم فتح نافذة التسوية والشكوى بأثر رجعي." else "Opening retroactive settlement."
                )
            }
            if (norm.contains("فحص العجز") || norm.contains("تدقيق العجز") || norm.contains("inspect deficit") || norm.contains("audit deficit")) {
                return AccountingVoiceCommand.OpenHubModal(
                    modalType = HubModalType.DEFICIT_INSPECTOR,
                    title = HubModalType.DEFICIT_INSPECTOR.label(language),
                    spokenReply = if (isAr) "تم فتح مدقق قيود العجز." else "Opening deficit inspector."
                )
            }

            // 3. Cross-App Section Open Intent
            if (wantsOpen || norm.startsWith("قسم ")) {
                detectAppDestination(norm)?.let { dest ->
                    if (dest != AppNavDestination.ACCOUNTING_HUB) {
                        return AccountingVoiceCommand.OpenAppScreen(
                            destination = dest,
                            title = dest.label(language),
                            spokenReply = if (isAr) "تم فتح قسم ${dest.titleAr}." else "Opening ${dest.titleEn}."
                        )
                    }
                }
            }
            if (norm == "الذهب" || norm == "قسم الذهب" || norm == "gold" || norm == "open gold") {
                return AccountingVoiceCommand.OpenAppScreen(
                    destination = AppNavDestination.GOLD,
                    title = AppNavDestination.GOLD.label(language),
                    spokenReply = if (isAr) "تم فتح قسم الذهب." else "Opening Gold Market."
                )
            }

            // 4. Hub Tab Navigation Intent (-1..4)
            detectHubSection(norm, language)?.let { nav ->
                return nav
            }

            // 5. Export Commands
            if (norm.contains("اكسل موحد") || norm.contains("اكسل شامل") || norm.contains("شيت موحد") || norm.contains("unified excel")) {
                return AccountingVoiceCommand.ExportReport(
                    format = "EXCEL_UNIFIED",
                    message = if (isAr) "جاري تجهيز الشيت المحاسبي الموحد..." else "Exporting unified workbook...",
                    spokenReply = if (isAr) "جاري تصدير الشيت المحاسبي الموحد." else "Exporting unified Excel workbook."
                )
            }
            if (norm.contains("اكسل") || norm.contains("excel") || norm.contains("شيت")) {
                return AccountingVoiceCommand.ExportReport(
                    format = "EXCEL",
                    message = if (isAr) "جاري تصدير كشف اليومية..." else "Exporting journal sheet...",
                    spokenReply = if (isAr) "جاري تصدير ملف الإكسل." else "Exporting Excel sheet."
                )
            }
            if (norm.contains("pdf") || norm.contains("بي دي اف") || norm.contains("طباعه")) {
                return AccountingVoiceCommand.ExportReport(
                    format = "PDF",
                    message = if (isAr) "جاري تجهيز التقرير الرسمي PDF..." else "Generating PDF report...",
                    spokenReply = if (isAr) "جاري تجهيز تقرير PDF." else "Generating PDF report."
                )
            }

            // 6. Live Balance & Cashflow Query
            if (norm.contains("كم الرصيد") || norm.contains("رصيد") || norm.contains("السيوله") ||
                norm.contains("صافي") || norm.contains("معانا كام") || norm.contains("في الخزنه") ||
                norm.contains("balance") || norm.contains("cashflow") || norm.contains("treasury")
            ) {
                val formattedBal = GoldUtils.formatCurrency(netBalance)
                val detailsStr = if (isAr) {
                    "وارد: ${GoldUtils.formatCurrency(summary.totalRevenue + summary.totalIncoming)} • صادر: ${GoldUtils.formatCurrency(summary.totalOutgoing)} • عجز: ${GoldUtils.formatCurrency(totalDeficit)}"
                } else {
                    "Inflow: ${GoldUtils.formatCurrency(summary.totalRevenue + summary.totalIncoming)} • Outflow: ${GoldUtils.formatCurrency(summary.totalOutgoing)} • Deficit: ${GoldUtils.formatCurrency(totalDeficit)}"
                }
                return AccountingVoiceCommand.BalanceQuery(
                    title = if (isAr) "رصيد الخزينة الصافي" else "Net Treasury Balance",
                    answer = formattedBal,
                    details = detailsStr,
                    spokenReply = if (isAr) "صافي الخزينة الحالي $formattedBal" else "Current net treasury is $formattedBal"
                )
            }

            // 7. Live Deficit & Disputes Query
            if (norm.contains("عجز") || norm.contains("شكاو") || norm.contains("شكوي") ||
                norm.contains("فروقات") || norm.contains("فروق") || norm.contains("deficit") || norm.contains("dispute")
            ) {
                val answerStr = if (totalDeficit > 0.0 || pendingComplaintsCount > 0) {
                    if (isAr) {
                        "إجمالي العجز: ${GoldUtils.formatCurrency(totalDeficit)} ($openDeficitCount قيد) • شكاوى معلقة: $pendingComplaintsCount"
                    } else {
                        "Total Deficit: ${GoldUtils.formatCurrency(totalDeficit)} ($openDeficitCount rows) • Open Disputes: $pendingComplaintsCount"
                    }
                } else {
                    if (isAr) "لا يوجد عجز أو شكاوى معلقة حالياً" else "Zero deficits or open disputes"
                }
                return AccountingVoiceCommand.DeficitQuery(
                    title = if (isAr) "فحص العجز والشكاوى" else "Deficits & Disputes",
                    answer = answerStr,
                    totalDeficit = totalDeficit,
                    complaintsCount = summary.totalComplaints + pendingComplaintsCount,
                    spokenReply = answerStr,
                    suggestedAction = SuggestedAssistantAction.OpenHubModal(
                        modalType = if (pendingComplaintsCount > 0) HubModalType.INVOICE_COMPLAINTS else HubModalType.DEFICIT_INSPECTOR,
                        label = if (isAr) "مراجعة التفاصيل" else "Inspect Now"
                    )
                )
            }

            // 8. Accounting Entry Recording (amount / weight / verbs)
            val parsed = SmartVoiceAccountingParser.parseVoiceAccountingText(trimmed, activeProfile?.name ?: "الخزينة")
            if (parsed.amount > 0.0 || parsed.weightGrams > 0.0 ||
                norm.contains("سجل") || norm.contains("مصروف") || norm.contains("قيد") ||
                norm.contains("قبض") || norm.contains("صرف") || norm.contains("ادفع") || norm.contains("دفعه")
            ) {
                if (parsed.amount == 0.0 && (norm == "قيد جديد" || norm == "اضافه قيد" || norm == "افتح قيد")) {
                    return AccountingVoiceCommand.OpenHubModal(
                        modalType = HubModalType.ADD_RECORD,
                        title = HubModalType.ADD_RECORD.label(language),
                        spokenReply = if (isAr) "تم فتح نموذج قيد جديد." else "Opening new journal entry."
                    )
                }

                val targetGroup = groups.firstOrNull()
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val isCredit = parsed.direction == "CREDIT"

                val candidateEntity = DailyAccountingRecordEntity(
                    id = UUID.randomUUID().toString(),
                    groupId = targetGroup?.id ?: "general_hub",
                    subGroupId = targetGroup?.companyName ?: "إدارة الحسابات",
                    profileId = activeProfile?.id,
                    entryDate = todayStr,
                    staffId = "ai_voice_stt",
                    staffName = if (parsed.counterPartyName.isNotBlank()) parsed.counterPartyName else (activeProfile?.name ?: "المساعد الذكي"),
                    staffRole = "محاسب ذكي",
                    dailyRevenue = if (isCredit && parsed.operationCategory != "مصروفات") parsed.amount else 0.0,
                    incomingAmount = if (isCredit) parsed.amount else 0.0,
                    outgoingAmount = if (!isCredit) parsed.amount else 0.0,
                    deficitAmount = 0.0,
                    complaintsCount = 0,
                    notes = "[أمر صوتي] ${parsed.rawSpokenText} (${parsed.suggestedTag})",
                    isEncrypted = true,
                    timestamp = System.currentTimeMillis()
                )

                val opLabel = if (isCredit) "إيراد وقبض" else "مصروف ومدفوعات"
                val reviewVoiceReadback = if (isAr) {
                    "قيد $opLabel بقيمة ${parsed.amount.toInt()} جنيه بتصنيف ${parsed.suggestedTag}"
                } else {
                    "Draft ${if (isCredit) "inflow" else "outflow"} entry for ${parsed.amount.toInt()} (${parsed.suggestedTag})"
                }

                return AccountingVoiceCommand.RecordTransaction(
                    parsedVoiceRecord = parsed,
                    candidateEntity = candidateEntity,
                    spokenConfirmation = reviewVoiceReadback
                )
            }

            // 9. Executive Financial Summary & Live Smart Assistant Advice
            val liveSummary = buildLiveExecutiveBriefing(
                isAr = isAr,
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
                    label = if (isAr) "فتح اليومية" else "Open Journal"
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
            val isAr = isRtlLang(lang)
            return when {
                norm.contains("اليوميه") || norm.contains("الدفتر") || norm.contains("القيود") || norm.contains("journal") || norm.contains("ledger") ->
                    AccountingVoiceCommand.SectionNavigation(1, AccountingI18n.get("sec_ledger", lang), if (isAr) "تم فتح دفتر اليومية." else "Opened Journal.")
                norm.contains("الشركات") || norm.contains("المجموعات") || norm.contains("الاقسام") || norm.contains("الفروع") || norm.contains("companies") || norm.contains("entities") ->
                    AccountingVoiceCommand.SectionNavigation(2, AccountingI18n.get("sec_groups", lang), if (isAr) "تم فتح الشركات والأقسام." else "Opened Entities.")
                norm.contains("الموظفين") || norm.contains("الورديات") || norm.contains("الحضور") || norm.contains("staff") || norm.contains("shifts") ->
                    AccountingVoiceCommand.SectionNavigation(3, AccountingI18n.get("sec_staff", lang), if (isAr) "تم فتح الموظفين والورديات." else "Opened Staff & Shifts.")
                norm.contains("الخزائن") || norm.contains("الخزينه السريه") || norm.contains("المخفيه") || norm.contains("vault") ->
                    AccountingVoiceCommand.SectionNavigation(0, AccountingI18n.get("sec_vaults", lang), if (isAr) "تم فتح الخزائن السرية." else "Opened Secret Vaults.")
                norm.contains("نسخ") || norm.contains("سحاب") || norm.contains("مزامن") || norm.contains("cloud") || norm.contains("backup") ->
                    AccountingVoiceCommand.SectionNavigation(4, AccountingI18n.get("sec_backup", lang), if (isAr) "تم فتح النسخ السحابي." else "Opened Cloud Backup.")
                norm.contains("الرئيسي") || norm.contains("المؤشرات") || norm.contains("داشبورد") || norm.contains("dashboard") || norm.contains("overview") ->
                    AccountingVoiceCommand.SectionNavigation(-1, AccountingI18n.get("sec_dashboard", lang), if (isAr) "تم فتح المؤشرات الرئيسية." else "Opened Overview.")
                else -> null
            }
        }

        private fun buildExplanationResponse(
            trimmed: String,
            norm: String,
            isAr: Boolean,
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
                    val ans = if (isAr) {
                        "الخزائن السرية هي مساحات مالية مستقلة بكلمة مرور، تفصل قيود كل نشاط أو شريك عن الخزينة العامة مع نفس أدوات اليومية والتصدير."
                    } else {
                        "Secret Vaults isolate ledgers, entities, and Excel/PDF reports per sub-entity behind an encrypted PIN."
                    }
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = if (isAr) "شرح الخزائن السرية" else "About Secret Vaults",
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenHubSection(0, if (isAr) "فتح الخزائن" else "Open Vaults")
                    )
                }
                norm.contains("رجعي") || norm.contains("تسويه") || norm.contains("عهد") || norm.contains("retro") -> {
                    val ans = if (isAr) {
                        "التسوية بأثر رجعي تتيح قيد فرق عهدة أو خطأ فاتورة قديم على عهدة الموظف مع حفظ الإثبات وتحديث صافي الرصيد تلقائياً."
                    } else {
                        "Retroactive Claims reconcile past shift or cash discrepancies directly against staff custody ledgers."
                    }
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = if (isAr) "التسوية بأثر رجعي" else "Retroactive Claims",
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenHubModal(HubModalType.RETROACTIVE_COMPLAINT, if (isAr) "فتح التسوية" else "Open Claim")
                    )
                }
                norm.contains("عجز") || norm.contains("شكوي") || norm.contains("شكاو") || norm.contains("نزاع") || norm.contains("deficit") || norm.contains("dispute") -> {
                    val ans = if (isAr) {
                        "العجز المسجل حالياً: ${GoldUtils.formatCurrency(summary.totalDeficit)} في $openDeficitCount قيد، مع $pendingComplaintsCount شكاوى معلقة. يمكنك فحص القيود أو التحقيق الجنائي في تسليم الشفت مباشرة."
                    } else {
                        "Current recorded deficit is ${GoldUtils.formatCurrency(summary.totalDeficit)} across $openDeficitCount entries, with $pendingComplaintsCount open disputes."
                    }
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = if (isAr) "شرح العجز والشكاوى" else "Deficits & Disputes",
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenHubModal(HubModalType.INVOICE_COMPLAINTS, if (isAr) "مراجعة الشكاوى" else "Open Disputes")
                    )
                }
                norm.contains("يوميه") || norm.contains("قيد") || norm.contains("دفتر") || norm.contains("journal") || norm.contains("ledger") -> {
                    val ans = if (isAr) {
                        "دفتر اليومية يجمع الوارد (${GoldUtils.formatCurrency(summary.totalRevenue + summary.totalIncoming)}) والصادر (${GoldUtils.formatCurrency(summary.totalOutgoing)}) عبر ${summary.recordsCount} قيد، مع تصدير واستيراد إكسل وPDF."
                    } else {
                        "Daily Journal tracks inflow (${GoldUtils.formatCurrency(summary.totalRevenue + summary.totalIncoming)}) and outflow (${GoldUtils.formatCurrency(summary.totalOutgoing)}) across ${summary.recordsCount} entries."
                    }
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = if (isAr) "شرح دفتر اليومية" else "Daily Journal",
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenHubSection(1, if (isAr) "فتح اليومية" else "Open Journal")
                    )
                }
                norm.contains("موظف") || norm.contains("وردي") || norm.contains("حضور") || norm.contains("شفت") || norm.contains("staff") || norm.contains("shift") -> {
                    val ans = if (isAr) {
                        "قسم الموظفين يضبط ساعات الورديات، صلاحيات ترحيل القيود، التتبع الجغرافي، وسجل الحضور والانصراف الفوري."
                    } else {
                        "Staff & Shifts manages work hours, geofenced clock-in/out logs, and accounting posting permissions."
                    }
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = if (isAr) "الموظفون والورديات" else "Staff & Shifts",
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenHubSection(3, if (isAr) "فتح الموظفين" else "Open Staff")
                    )
                }
                norm.contains("شرك") || norm.contains("مجموع") || norm.contains("فرع") || norm.contains("اقسام") || norm.contains("entities") || norm.contains("compan") -> {
                    val ans = if (isAr) {
                        "لديك حالياً $groupsCount شركة/قسم. كل كيان له قناة رسمية، تحكم في منع الخاص، وأرصدة وارد وصادر منفصلة."
                    } else {
                        "You have $groupsCount entities/branches with dedicated ledgers, privacy locks, and per-entity Excel export."
                    }
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = if (isAr) "الشركات والأقسام" else "Entities & Branches",
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenHubSection(2, if (isAr) "فتح الشركات" else "Open Entities")
                    )
                }
                norm.contains("اكسل") || norm.contains("excel") || norm.contains("شيت") || norm.contains("pdf") -> {
                    val ans = if (isAr) {
                        "الشيت الموحد يدمج قيود اليومية والمشاريع والفواتير وهاشتاجات الشات في ملف CSV جاهز لإكسل مع الحسابات المجمعة."
                    } else {
                        "Unified Excel combines daily entries, projects, invoices, and chat ledgers into a single executive workbook."
                    }
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = if (isAr) "التقارير الموحدة" else "Unified Reports",
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.ExportReport("EXCEL_UNIFIED", if (isAr) "تصدير إكسل" else "Export Excel")
                    )
                }
                norm.contains("ذهب") || norm.contains("gold") -> {
                    val ans = if (isAr) {
                        "قسم الذهب يعرض الأسعار الحية للعيارات 24/21/18، حاسبة المصنعية، فحص QR، الزكاة، وسجل الفواتير."
                    } else {
                        "Gold Market tracks live 24K/21K/18K rates, craftsmanship calculator, QR authenticity, and portfolio."
                    }
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = if (isAr) "قسم الذهب" else "Gold Market",
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenAppScreen(AppNavDestination.GOLD, if (isAr) "فتح الذهب" else "Open Gold")
                    )
                }
                else -> {
                    val combinedStr = unifiedSummary?.let { " | الصافي الموحد: ${GoldUtils.formatCurrency(it.combinedNetPosition)}" } ?: ""
                    val ans = if (isAr) {
                        val scope = activeProfile?.name ?: "الخزينة العامة"
                        "ملخص ($scope):\n• صافي الرصيد: ${GoldUtils.formatCurrency(netBalance)}\n• الوارد: ${GoldUtils.formatCurrency(summary.totalRevenue + summary.totalIncoming)} | الصادر: ${GoldUtils.formatCurrency(summary.totalOutgoing)}\n• العجز: ${GoldUtils.formatCurrency(summary.totalDeficit)} | الشكاوى: $pendingComplaintsCount$combinedStr"
                    } else {
                        val scope = activeProfile?.name ?: "Main Treasury"
                        "Summary ($scope):\n• Net Balance: ${GoldUtils.formatCurrency(netBalance)}\n• Inflow: ${GoldUtils.formatCurrency(summary.totalRevenue + summary.totalIncoming)} | Outflow: ${GoldUtils.formatCurrency(summary.totalOutgoing)}\n• Deficit: ${GoldUtils.formatCurrency(summary.totalDeficit)} | Disputes: $pendingComplaintsCount"
                    }
                    AccountingVoiceCommand.ExplainOrAnswer(
                        title = if (isAr) "التحليل المالي التنفيذي" else "Executive Summary",
                        answer = ans,
                        spokenReply = ans,
                        suggestedAction = SuggestedAssistantAction.OpenHubSection(-1, if (isAr) "عرض المؤشرات" else "Overview")
                    )
                }
            }
        }

        private fun buildLiveExecutiveBriefing(
            isAr: Boolean,
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
            return if (isAr) {
                buildString {
                    appendLine("صافي الخزينة: ${GoldUtils.formatCurrency(netBalance)}")
                    appendLine("• الإيراد والوارد: ${GoldUtils.formatCurrency(summary.totalRevenue + summary.totalIncoming)}")
                    appendLine("• الصادر والمصروف: ${GoldUtils.formatCurrency(summary.totalOutgoing)}")
                    if (summary.totalDeficit > 0 || pendingComplaintsCount > 0) {
                        appendLine("• تنبيه: عجز ${GoldUtils.formatCurrency(summary.totalDeficit)} و $pendingComplaintsCount شكاوى")
                    }
                    if (!topEntity.isNullOrBlank()) {
                        appendLine("• الأعلى نشاطاً: $topEntity (${groups.size} كيانات)")
                    }
                    unifiedSummary?.let {
                        append("• الموقف الموحد: ${GoldUtils.formatCurrency(it.combinedNetPosition)}")
                    }
                }.trim()
            } else {
                buildString {
                    appendLine("Net Treasury: ${GoldUtils.formatCurrency(netBalance)}")
                    appendLine("• Inflow: ${GoldUtils.formatCurrency(summary.totalRevenue + summary.totalIncoming)}")
                    appendLine("• Outflow: ${GoldUtils.formatCurrency(summary.totalOutgoing)}")
                    if (summary.totalDeficit > 0 || pendingComplaintsCount > 0) {
                        appendLine("• Alert: Deficit ${GoldUtils.formatCurrency(summary.totalDeficit)}, $pendingComplaintsCount open disputes")
                    }
                    unifiedSummary?.let {
                        append("• Enterprise Net: ${GoldUtils.formatCurrency(it.combinedNetPosition)}")
                    }
                }.trim()
            }
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
                    SpeechRecognizer.ERROR_AUDIO -> "خطأ في الميكروفون"
                    SpeechRecognizer.ERROR_CLIENT -> "تعذر الاتصال بالمساعد"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "فعل صلاحية الميكروفون"
                    SpeechRecognizer.ERROR_NETWORK -> "تحقق من اتصال الإنترنت"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "انتهت مهلة الشبكة"
                    SpeechRecognizer.ERROR_NO_MATCH -> "لم يلتقط أمراً واضحاً"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "المتعرف مشغول"
                    SpeechRecognizer.ERROR_SERVER -> "تعذر المعالجة"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يُسمع صوت"
                    else -> "تعذر التقاط الصوت ($error)"
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
        languageCode: String = "ar-EG",
        onPartial: ((String) -> Unit)? = null,
        onFinal: ((String) -> Unit)? = null
    ) {
        onPartialCallback = onPartial
        onFinalCallback = onFinal
        _spokenText.value = ""
        _partialText.value = ""
        _errorMessage.value = null

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن...")
                }

                _isListening.value = true
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening", e)
                _errorMessage.value = "تعذر بدء التسجيل الصوتي"
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
        language: AppLanguage = AppLanguage.ARABIC
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
