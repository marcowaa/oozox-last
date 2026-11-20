package com.example.util

import com.example.data.model.*

object SmartAccountantEngine {

    fun generateSmartAlerts(
        accounts: List<ChatAccountEntity>,
        hashtags: List<HashtagAccountEntity>,
        entries: List<HashtagEntryEntity>,
        chatName: String
    ): List<SmartAlertModel> {
        val alerts = mutableListOf<SmartAlertModel>()
        val now = System.currentTimeMillis()

        for (account in accounts) {
            val accountHashtags = hashtags.filter { it.accountId == account.id }
            val accountEntries = entries.filter { entry -> accountHashtags.any { it.id == entry.hashtagId } }
            val totals = HashtagUtils.calculateHashtagTotals(accountEntries)

            // 1. Large debt open > 30 days
            if (totals.remaining > 50000 && account.status != HashtagStatus.SETTLED.name) {
                val oldestEntry = accountEntries.minByOrNull { it.createdAt }
                if (oldestEntry != null) {
                    val daysOld = ((now - oldestEntry.createdAt) / 86400000).toInt()
                    if (daysOld > 30) {
                        alerts.add(
                            SmartAlertModel(
                                id = "debt_old_${account.id}",
                                type = "warning",
                                title = "⚠️ دين مستمر لفترة طويلة",
                                message = "حساب \"${account.name}\" لديه دين ${GoldUtils.formatCurrency(totals.remaining)} مستمر منذ $daysOld يوم. ننصح بالمتابعة.",
                                accountName = account.name,
                                chatName = chatName,
                                amount = totals.remaining
                            )
                        )
                    }
                }
            }

            // 2. Overpayment (negative remaining)
            if (totals.remaining < 0) {
                alerts.add(
                    SmartAlertModel(
                        id = "overpay_${account.id}",
                        type = "error",
                        title = "🚨 دفعة زائدة عن الدين",
                        message = "حساب \"${account.name}\" تم دفع ${GoldUtils.formatCurrency(Math.abs(totals.remaining))} أكثر من المطلوب.",
                        accountName = account.name,
                        chatName = chatName,
                        amount = Math.abs(totals.remaining)
                    )
                )
            }

            // 3. Huge entry
            for (entry in accountEntries) {
                if (entry.amount > 1000000) {
                    alerts.add(
                        SmartAlertModel(
                            id = "huge_${entry.id}",
                            type = "critical",
                            title = "🚨 مبلغ ضخم غير معتاد",
                            message = "معاملة \"${entry.description}\" بمبلغ ${GoldUtils.formatCurrency(entry.amount)} في حساب \"${account.name}\". يرجى المراجعة.",
                            accountName = account.name,
                            chatName = chatName,
                            amount = entry.amount
                        )
                    )
                }
            }
        }

        return alerts
    }

    fun calculateHealthScore(
        accounts: List<ChatAccountEntity>,
        hashtags: List<HashtagAccountEntity>,
        entries: List<HashtagEntryEntity>,
        chatName: String
    ): AIAnalysisResultModel {
        val alerts = generateSmartAlerts(accounts, hashtags, entries, chatName)
        val insights = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        var totalDebit = 0.0
        var totalCredit = 0.0
        for (acc in accounts) {
            val accHashtags = hashtags.filter { it.accountId == acc.id }
            val accEntries = entries.filter { entry -> accHashtags.any { it.id == entry.hashtagId } }
            val totals = HashtagUtils.calculateHashtagTotals(accEntries)
            totalDebit += totals.totalDebit
            totalCredit += totals.totalCredit
        }
        val grandRemaining = totalDebit - totalCredit

        if (grandRemaining > 0) {
            insights.add("💰 إجمالي الدين المستحق: ${GoldUtils.formatCurrency(grandRemaining)}")
        } else if (grandRemaining < 0) {
            insights.add("💚 لديك رصيد دائن: ${GoldUtils.formatCurrency(Math.abs(grandRemaining))}")
        }

        val settledCount = accounts.count { it.status == HashtagStatus.SETTLED.name }
        if (settledCount == accounts.size && accounts.isNotEmpty()) {
            insights.add("✅ جميع الحسابات مسدّدة بالكامل")
        }

        val criticalCount = alerts.count { it.type == "critical" }
        val errorCount = alerts.count { it.type == "error" }
        val warningCount = alerts.count { it.type == "warning" }

        if (criticalCount > 0) {
            recommendations.add("🚨 لديك $criticalCount مشكلة حرجة تتطلب مراجعة فورية")
        }
        if (errorCount > 0) {
            recommendations.add("❌ $errorCount خطأ في الحسابات — راجع الدفعات الزائدة")
        }
        if (warningCount > 0) {
            recommendations.add("⚠️ $warningCount تنبيه يحتاج متابعة مع العملات")
        }

        var score = 100
        score -= criticalCount * 25
        score -= errorCount * 15
        score -= warningCount * 8
        score = score.coerceIn(0, 100)

        return AIAnalysisResultModel(
            insights = insights,
            alerts = alerts,
            recommendations = recommendations,
            healthScore = score
        )
    }

    fun askAccountant(
        question: String,
        accounts: List<ChatAccountEntity>,
        hashtags: List<HashtagAccountEntity>,
        entries: List<HashtagEntryEntity>,
        chatName: String
    ): String {
        val q = question.trim().lowercase()
        val alerts = generateSmartAlerts(accounts, hashtags, entries, chatName)
        val analysis = calculateHealthScore(accounts, hashtags, entries, chatName)

        var grandDebit = 0.0
        var grandCredit = 0.0
        for (acc in accounts) {
            val accHashtags = hashtags.filter { it.accountId == acc.id }
            val accEntries = entries.filter { entry -> accHashtags.any { it.id == entry.hashtagId } }
            val totals = HashtagUtils.calculateHashtagTotals(accEntries)
            grandDebit += totals.totalDebit
            grandCredit += totals.totalCredit
        }
        val grandRemaining = grandDebit - grandCredit

        // 1. البحث في القيود والحسابات
        if (q.startsWith("بحث") || q.startsWith("ابحث") || q.contains("دور على")) {
            val keyword = q.removePrefix("ابحث عن").removePrefix("ابحث").removePrefix("بحث عن").removePrefix("بحث").removePrefix("دور على").trim()
            if (keyword.isBlank()) {
                return "اكتب كلمة البحث المطلوبة، مثال: \"ابحث عن ذهب\" أو \"ابحث عن 5000\"."
            }
            val matchedEntries = entries.filter {
                it.description.lowercase().contains(keyword) ||
                    it.amount.toString().contains(keyword)
            }
            val matchedAccounts = accounts.filter { it.name.lowercase().contains(keyword) }
            if (matchedEntries.isEmpty() && matchedAccounts.isEmpty()) {
                return "لم يتم العثور على نتائج مطابقة لـ \"$keyword\" في \"$chatName\"."
            }
            val sb = StringBuilder("نتائج البحث عن \"$keyword\":\n")
            if (matchedAccounts.isNotEmpty()) {
                sb.append("\n• الحسابات (${matchedAccounts.size}): ${matchedAccounts.joinToString("، ") { it.name }}")
            }
            if (matchedEntries.isNotEmpty()) {
                sb.append("\n• القيود (${matchedEntries.size}):\n")
                matchedEntries.take(5).forEach { e ->
                    sb.append("  - ${e.description}: ${GoldUtils.formatCurrency(e.amount)}\n")
                }
            }
            return sb.toString().trim()
        }

        // 2. إسناد المهام
        if (q.contains("مهمة") || q.contains("اسند") || q.contains("إسناد") || q.contains("تكليف")) {
            val taskDetail = question.trim()
                .removePrefix("اسند مهمة")
                .removePrefix("إسناد مهمة")
                .removePrefix("مهمة جديدة")
                .removePrefix("مهمة:")
                .trim()
            return if (taskDetail.isBlank()) {
                "لإسناد مهمة، اكتب تفاصيلها أو استخدم تبويب المشاريع لتعيين المسؤول والموعد، مثال: \"اسند مهمة مراجعة جرد اليوم لأحمد\"."
            } else {
                "تم تجهيز المهمة: \"$taskDetail\".\nيمكنك تأكيد إرسالها للمسؤول مباشرة من شاشة المشاريع والمهام أو داخل المحادثة."
            }
        }

        if (q.contains("تنبيه") || q.contains("مشكلة") || q.contains("خطأ")) {
            if (alerts.isEmpty()) {
                return "لا توجد مشاكل أو تنبيهات في \"$chatName\". صحة حساباتك ممتازة."
            }
            return "لديك ${alerts.size} تنبيه في \"$chatName\":\n\n" + alerts.joinToString("\n\n") { "${it.title}\n${it.message}" }
        }

        if (q.contains("صحة") || q.contains("تقييم") || q.contains("حالة")) {
            val emoji = if (analysis.healthScore >= 80) "🟢" else if (analysis.healthScore >= 50) "🟡" else "🔴"
            var res = "$emoji صحة الحسابات: ${analysis.healthScore}/100\n\n"
            if (analysis.insights.isNotEmpty()) {
                res += "تحليلات:\n" + analysis.insights.joinToString("\n") { "• $it" } + "\n\n"
            }
            if (analysis.recommendations.isNotEmpty()) {
                res += "توصيات:\n" + analysis.recommendations.joinToString("\n") { "• $it" }
            }
            return res
        }

        if (q.contains("متبقي") || q.contains("دين") || q.contains("مستحق") || q.contains("له") || q.contains("عليه")) {
            return if (grandRemaining > 0) {
                "إجمالي المبلغ المستحق عليك في \"$chatName\": ${GoldUtils.formatCurrency(grandRemaining)}"
            } else if (grandRemaining < 0) {
                "ليس عليك أي ديون في \"$chatName\" — لديك رصيد دائن ${GoldUtils.formatCurrency(Math.abs(grandRemaining))}"
            } else {
                "جميع الحسابات مسدّدة بالكامل في \"$chatName\""
            }
        }

        if (q.contains("ملخص") || q.contains("تقرير") || q.contains("حسابات") || q.contains("summary")) {
            return "ملخص الحسابات في \"$chatName\":\n\n• إجمالي المدين: ${GoldUtils.formatCurrency(grandDebit)}\n• إجمالي الدائن: ${GoldUtils.formatCurrency(grandCredit)}\n• المتبقي: ${GoldUtils.formatCurrency(grandRemaining)}\n• عدد الحسابات: ${accounts.size}\n• التنبيهات: ${alerts.size}"
        }

        return "أهلاً بك في المساعد الذكي. أقدر أساعدك في:\n• البحث في القيود والحسابات (مثال: \"ابحث عن ذهب\")\n• إدارة الحسابات والأرصدة (\"كم المتبقي؟\" / \"ملخص الحسابات\")\n• إسناد المهام (مثال: \"اسند مهمة مراجعة الفواتير\")"
    }

    data class GlobalAccountingSnapshot(
        val invoices: List<InvoiceEntity> = emptyList(),
        val projects: List<ProjectEntity> = emptyList(),
        val hashtagAccounts: List<HashtagAccountEntity> = emptyList(),
        val hashtagEntries: List<HashtagEntryEntity> = emptyList(),
        val chatAccounts: List<ChatAccountEntity> = emptyList(),
        val dailyRecords: List<DailyAccountingRecordEntity> = emptyList(),
        val products: List<MerchantProductEntity> = emptyList(),
        val shiftHandovers: List<ShiftHandoverEntity> = emptyList(),
        val goldPurchases: List<GoldPurchaseEntity> = emptyList()
    )

    data class GlobalSearchResult(
        val matchedInvoices: List<InvoiceEntity>,
        val matchedProjects: List<ProjectEntity>,
        val matchedHashtags: List<HashtagAccountEntity>,
        val matchedProducts: List<MerchantProductEntity>,
        val matchedRecords: List<DailyAccountingRecordEntity>
    )

    fun searchGlobalRecords(query: String, snapshot: GlobalAccountingSnapshot): GlobalSearchResult {
        val q = query.trim().removePrefix("ابحث عن").removePrefix("ابحث").removePrefix("بحث عن").removePrefix("بحث").trim().lowercase()
        if (q.isBlank()) {
            return GlobalSearchResult(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        }
        val cleanTagQ = q.removePrefix("#")
        return GlobalSearchResult(
            matchedInvoices = snapshot.invoices.filter {
                it.invoiceNumber.lowercase().contains(q) ||
                    it.title.lowercase().contains(q) ||
                    it.receiverName.lowercase().contains(q) ||
                    it.senderName.lowercase().contains(q) ||
                    (it.hashtagTag?.lowercase()?.contains(cleanTagQ) == true) ||
                    (it.projectName?.lowercase()?.contains(q) == true) ||
                    it.totalAmount.toString().contains(q)
            },
            matchedProjects = snapshot.projects.filter {
                it.name.lowercase().contains(q) ||
                    it.type.lowercase().contains(q) ||
                    it.location.lowercase().contains(q)
            },
            matchedHashtags = snapshot.hashtagAccounts.filter {
                it.tag.lowercase().contains(cleanTagQ) ||
                    (it.projectName?.lowercase()?.contains(q) == true)
            },
            matchedProducts = snapshot.products.filter {
                it.title.lowercase().contains(q) ||
                    (it.extractedBarcode?.lowercase()?.contains(q) == true)
            },
            matchedRecords = snapshot.dailyRecords.filter {
                it.staffName.lowercase().contains(q) ||
                    (it.subGroupId?.lowercase()?.contains(cleanTagQ) == true) ||
                    (it.notes?.lowercase()?.contains(q) == true)
            }
        )
    }

    fun buildGlobalContextSummary(snapshot: GlobalAccountingSnapshot): String {
        val unified = SimpleExcelEngine.calculateUnifiedBusinessAccounting(
            projects = snapshot.projects,
            invoices = snapshot.invoices,
            hashtagAccounts = snapshot.hashtagAccounts,
            hashtagEntries = snapshot.hashtagEntries,
            dailyRecords = snapshot.dailyRecords,
            shiftHandovers = snapshot.shiftHandovers
        )
        return buildString {
            appendLine("الملخص الشامل لبيانات الأعمال الحالية في OOZOX:")
            appendLine("- المشاريع (${unified.projectsCount}): دخل ${GoldUtils.formatCurrency(unified.projectsIncome)} | مصروف ${GoldUtils.formatCurrency(unified.projectsExpenses)}")
            appendLine("- الفواتير (${unified.totalInvoicesCount}): إجمالي ${GoldUtils.formatCurrency(unified.invoicesTotalBilled)} | مدفوع ${GoldUtils.formatCurrency(unified.invoicesPaidTotal)} | متبقي ${GoldUtils.formatCurrency(unified.invoicesOutstanding)} (غير مكتملة: ${unified.unpaidInvoicesCount})")
            appendLine("- الأعمال الخارجية: إجمالي ${GoldUtils.formatCurrency(unified.externalBusinessTotal)} | محصل ${GoldUtils.formatCurrency(unified.externalBusinessPaid)}")
            appendLine("- هاشتاجات الشات النشطة (${unified.activeHashtagsCount}): صافي الرصيد ${GoldUtils.formatCurrency(unified.hashtagsNetReceivable)}")
            appendLine("- الخزينة واليوميات: إيراد ${GoldUtils.formatCurrency(unified.dailyJournalRevenue)} | عجز الورديات ${GoldUtils.formatCurrency(unified.shiftDeficitsTotal)}")
            appendLine("- صافي الموقف المالي الموحد: ${GoldUtils.formatCurrency(unified.combinedNetPosition)}")
        }
    }

    fun detectGlobalDestination(query: String): AppNavDestination? {
        val norm = query.trim().lowercase()
            .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ة", "ه").replace("ى", "ي")
        val wantsOpen = listOf("افتح", "اعرض", "وريني", "ادخل", "انتقل", "اذهب", "روح", "open", "go to", "show")
            .any { norm.contains(it) }
        if (!wantsOpen && !norm.startsWith("قسم ")) return null
        return when {
            norm.contains("مركز محاسبي") || norm.contains("حسابات الشركات") || norm.contains("اليوميه") || norm.contains("خزائن") || norm.contains("accounting hub") -> AppNavDestination.ACCOUNTING_HUB
            norm.contains("ذهب") || norm.contains("فضه") || norm.contains("عيار") || norm.contains("gold") -> AppNavDestination.GOLD
            norm.contains("مشروع") || norm.contains("مشاريع") || norm.contains("project") -> AppNavDestination.PROJECTS
            norm.contains("اداره الاعمال") || norm.contains("كاشير") || norm.contains("business") -> AppNavDestination.BUSINESS
            norm.contains("فواتير") || norm.contains("فاتوره") || norm.contains("invoice") -> AppNavDestination.INVOICES
            norm.contains("متجر") || norm.contains("معرض") || norm.contains("store") -> AppNavDestination.STORE
            norm.contains("اداره النظام") || norm.contains("ادمن") || norm.contains("admin") -> AppNavDestination.ADMIN
            norm.contains("محادثات") || norm.contains("شات") || norm.contains("chat") -> AppNavDestination.CHATS
            norm.contains("حسابي") || norm.contains("ملفي") || norm.contains("profile") -> AppNavDestination.PROFILE
            else -> null
        }
    }

    fun askGlobalAccountant(question: String, snapshot: GlobalAccountingSnapshot): String {
        val q = question.trim().lowercase()
            .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ة", "ه").replace("ى", "ي")
        val unified = SimpleExcelEngine.calculateUnifiedBusinessAccounting(
            projects = snapshot.projects,
            invoices = snapshot.invoices,
            hashtagAccounts = snapshot.hashtagAccounts,
            hashtagEntries = snapshot.hashtagEntries,
            dailyRecords = snapshot.dailyRecords,
            shiftHandovers = snapshot.shiftHandovers
        )

        // 0. Feature / concept explanations grounded in live data
        val asksLiveMetrics = listOf(
            "غير مدفوع", "متبقي", "مديوني", "فواتير", "مشاريع", "ديون", "معلق", "شيكات", "ارصده"
        ).any { q.contains(it) }
        val explicitExplain = listOf("اشرح", "فهمني", "يعني ايه", "كيف ", "ازاي", "explain", "how to")
            .any { q.contains(it) }
        val genericWhatIs = (q.contains("ما هو") || q.contains("ما هي") || q.contains("what is")) && !asksLiveMetrics
        val wantsExplain = explicitExplain || genericWhatIs
        if (wantsExplain) {
            return when {
                q.contains("مركز محاسبي") || q.contains("خزينه") || q.contains("يوميه") || q.contains("رجعي") ->
                    "المركز المحاسبي يجمع دفتر اليومية (${snapshot.dailyRecords.size} قيد)، الخزائن السرية، الشركات، الحضور، وتصدير إكسل الموحد بصافي موقف حالي ${GoldUtils.formatCurrency(unified.combinedNetPosition)}."
                q.contains("ذهب") ->
                    "قسم الذهب يتابع الأسعار الحية، حاسبة المصنعية والزكاة، فحص QR، وسجل حيازة الذهب (${snapshot.goldPurchases.size} فاتورة)."
                q.contains("عجز") || q.contains("ورديه") || q.contains("شفت") ->
                    "إجمالي عجز الورديات المسجل حالياً هو ${GoldUtils.formatCurrency(unified.shiftDeficitsTotal)} عبر ${snapshot.shiftHandovers.size} شفت. يمكنك فتح شكاوى الفواتير أو فحص العجز للمراجعة."
                else -> buildGlobalContextSummary(snapshot)
            }
        }

        // 1. Direct unpaid / overdue / external / specific search
        if (q.contains("غير مدفوع") || q.contains("معلق") || q.contains("متبقي") || q.contains("مديوني") || q.contains("فواتير")) {
            val openInvoices = snapshot.invoices.filter {
                it.status != InvoiceStatus.PAID.name && it.status != InvoiceStatus.CANCELLED.name
            }
            if (q.contains("ابحث") || q.contains("بحث") || q.contains("#") || q.contains("عن")) {
                val res = searchGlobalRecords(question, snapshot)
                if (res.matchedInvoices.isNotEmpty() || res.matchedHashtags.isNotEmpty() || res.matchedProjects.isNotEmpty()) {
                    return formatSearchAnswer(question, res, snapshot)
                }
            }
            return buildString {
                appendLine("تقرير الفواتير والمستحقات (${openInvoices.size} مفتوحة):")
                appendLine("• الإجمالي: ${GoldUtils.formatCurrency(unified.invoicesTotalBilled)}")
                appendLine("• المحصل: ${GoldUtils.formatCurrency(unified.invoicesPaidTotal)}")
                appendLine("• المتبقي: ${GoldUtils.formatCurrency(unified.invoicesOutstanding)}")
                if (openInvoices.isNotEmpty()) {
                    openInvoices.take(4).forEach { inv ->
                        val rem = (inv.totalAmount - inv.paidAmount).coerceAtLeast(0.0)
                        val tag = inv.hashtagTag?.let { " #$it" } ?: ""
                        appendLine("• ${inv.invoiceNumber}$tag (${inv.receiverName}): ${GoldUtils.formatCurrency(rem)}")
                    }
                }
            }.trim()
        }

        // 2. Search across everything (Hashtags, Invoices, Projects, Products, Daily Records)
        if (q.startsWith("ابحث") || q.startsWith("بحث") || q.contains("دور") || q.contains("اين") || q.contains("فين") || q.contains("#")) {
            val res = searchGlobalRecords(question, snapshot)
            return formatSearchAnswer(question, res, snapshot)
        }

        // 3. Projects overview
        if (q.contains("مشروع") || q.contains("مشاريع") || q.contains("مقاول")) {
            return buildString {
                appendLine("المشاريع (${snapshot.projects.size}):")
                appendLine("• الدخل: ${GoldUtils.formatCurrency(unified.projectsIncome)} | المصروف: ${GoldUtils.formatCurrency(unified.projectsExpenses)}")
                appendLine("• الصافي: ${GoldUtils.formatCurrency(unified.projectsIncome - unified.projectsExpenses)}")
                snapshot.projects.take(4).forEach { p ->
                    appendLine("• ${p.name}: صافي ${GoldUtils.formatCurrency(p.totalIncome - p.totalExpenses)}")
                }
            }.trim()
        }

        // 4. External work
        if (q.contains("خارج") || q.contains("مستقل")) {
            val ext = snapshot.invoices.filter { it.isExternalBusiness || it.sourceType == "EXTERNAL" }
            return "الأعمال الخارجية (${ext.size}):\n" +
                "• الإجمالي: ${GoldUtils.formatCurrency(unified.externalBusinessTotal)}\n" +
                "• المحصل: ${GoldUtils.formatCurrency(unified.externalBusinessPaid)}\n" +
                "• المتبقي: ${GoldUtils.formatCurrency((unified.externalBusinessTotal - unified.externalBusinessPaid).coerceAtLeast(0.0))}"
        }

        // 5. Fallback: try searching entities directly by keyword before returning unified dashboard
        val quickSearch = searchGlobalRecords(question, snapshot)
        if (quickSearch.matchedInvoices.isNotEmpty() || quickSearch.matchedProjects.isNotEmpty() ||
            quickSearch.matchedHashtags.isNotEmpty() || quickSearch.matchedProducts.isNotEmpty()
        ) {
            return formatSearchAnswer(question, quickSearch, snapshot)
        }

        return buildGlobalContextSummary(snapshot)
    }

    private fun formatSearchAnswer(
        rawQuery: String,
        res: GlobalSearchResult,
        snapshot: GlobalAccountingSnapshot
    ): String {
        if (res.matchedInvoices.isEmpty() && res.matchedProjects.isEmpty() &&
            res.matchedHashtags.isEmpty() && res.matchedProducts.isEmpty() && res.matchedRecords.isEmpty()
        ) {
            return "لم يتم العثور على نتائج مطابقة لـ \"$rawQuery\" في الفواتير أو المشاريع أو الهاشتاجات أو المخزون."
        }
        return buildString {
            appendLine("🔍 نتائج البحث الشاملة:")
            if (res.matchedInvoices.isNotEmpty()) {
                appendLine("• الفواتير (${res.matchedInvoices.size}):")
                res.matchedInvoices.take(5).forEach { inv ->
                    val tag = inv.hashtagTag?.let { " #$it" } ?: ""
                    appendLine("  - ${inv.invoiceNumber}$tag | ${inv.receiverName}: ${GoldUtils.formatCurrency(inv.totalAmount)} (${inv.status})")
                }
            }
            if (res.matchedHashtags.isNotEmpty()) {
                appendLine("• الهاشتاجات (${res.matchedHashtags.size}):")
                res.matchedHashtags.take(5).forEach { h ->
                    val totals = HashtagUtils.calculateHashtagTotals(snapshot.hashtagEntries.filter { it.hashtagId == h.id })
                    appendLine("  - #${h.tag}: إجمالي ${GoldUtils.formatCurrency(totals.totalDebit)} | متبقي ${GoldUtils.formatCurrency(totals.remaining)} (${h.status})")
                }
            }
            if (res.matchedProjects.isNotEmpty()) {
                appendLine("• المشاريع (${res.matchedProjects.size}):")
                res.matchedProjects.take(5).forEach { p ->
                    appendLine("  - ${p.name}: دخل ${GoldUtils.formatCurrency(p.totalIncome)} | مصروف ${GoldUtils.formatCurrency(p.totalExpenses)}")
                }
            }
            if (res.matchedProducts.isNotEmpty()) {
                appendLine("• الأصناف والمنتجات (${res.matchedProducts.size}):")
                res.matchedProducts.take(4).forEach { prod ->
                    appendLine("  - ${prod.title}: ${GoldUtils.formatCurrency(prod.consumerPriceEGP)} | وزن ${prod.weightGrams}جم")
                }
            }
            if (res.matchedRecords.isNotEmpty()) {
                appendLine("• قيود اليومية (${res.matchedRecords.size}) تم رصدها ومتطابقة.")
            }
        }.trim()
    }
}
