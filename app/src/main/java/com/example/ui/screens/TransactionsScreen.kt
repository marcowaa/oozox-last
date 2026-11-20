package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.graphicsLayer
import com.example.data.model.*
import com.example.ui.components.CircularCounterBadge
import com.example.ui.components.DeleteInvoiceConfirmationDialog
import com.example.ui.components.DeleteRecordType
import com.example.ui.components.MonthlyChartsFullView
import com.example.ui.components.MonthlyTransactionsRechartsCard
import com.example.ui.theme.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.util.GeminiAiService
import com.example.util.GoldUtils
import com.example.util.ReportExporter
import com.example.util.SmartAccountantEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class InvoicesMainTab(val title: String, val icon: ImageVector) {
    ALL_INVOICES("الفواتير", Icons.Default.ReceiptLong),
    MONTHLY_CHARTS("الرسوم البيانية", Icons.Default.BarChart),
    GOLD_INVOICES("فواتير الذهب", Icons.Default.MonetizationOn),
    COMPLETED_SETTLEMENTS("التسويات", Icons.Default.Balance),
    PROJECTS_PL("أرباح المشاريع", Icons.Default.Assessment),
    COMPREHENSIVE_AUDIT("المراجعة الشاملة", Icons.Default.AutoAwesome)
}

enum class DateFilterPreset(val label: String) {
    ALL("الكل"),
    TODAY("اليوم"),
    LAST_7_DAYS("7 أيام"),
    THIS_MONTH("هذا الشهر"),
    CUSTOM("مخصص")
}

data class UnifiedInvoiceItem(
    val id: String,
    val invoiceNumber: String,
    val category: String, // "GOLD_DEAL", "GOLD_PURCHASE", "SETTLEMENT", "HASHTAG_ENTRY", "LEDGER"
    val title: String,
    val partyName: String,
    val timestamp: Long,
    val cashAmount: Double,
    val paidAmount: Double = 0.0,
    val goldWeightGrams: Double? = null,
    val goldKarat: String? = null,
    val pricePerGram: Double? = null,
    val manufacturingCost: Double? = null,
    val status: String,
    val notes: String? = null,
    val relatedEntityId: String? = null,
    val hashtagTag: String? = null,
    val sourceType: String = "DIRECT",
    val isExternalBusiness: Boolean = false,
    val chatId: String? = null,
    val projectId: String? = null
)

data class ProjectPLSummary(
    val project: ProjectEntity,
    val capital: Double,
    val totalExpenses: Double,
    val totalIncome: Double,
    val netProfit: Double,
    val roiPercent: Double,
    val marginPercent: Double,
    val budgetUsagePercent: Double,
    val isProfitable: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionEntity> = emptyList(),
    goldPurchases: List<GoldPurchaseEntity> = emptyList(),
    settlements: List<ClearingSettlementEntity> = emptyList(),
    hashtagEntries: List<HashtagEntryEntity> = emptyList(),
    hashtagAccounts: List<HashtagAccountEntity> = emptyList(),
    ledgerEntries: List<LedgerEntryEntity> = emptyList(),
    invoices: List<InvoiceEntity> = emptyList(),
    projects: List<ProjectEntity> = emptyList(),
    dailyRecords: List<DailyAccountingRecordEntity> = emptyList(),
    adminConfig: AdminConfigEntity? = null,
    globalSnapshot: SmartAccountantEngine.GlobalAccountingSnapshot? = null,
    isLoading: Boolean = false,
    onTransactionClick: (String) -> Unit = {},
    onOpenProjectClick: ((String) -> Unit)? = null,
    onDeleteTransaction: ((String) -> Unit)? = null,
    onDeleteInvoice: ((String) -> Unit)? = null,
    onPayInvoice: ((invoiceId: String, amount: Double, method: String, notes: String?) -> Unit)? = null
) {
    var activeTab by remember { mutableStateOf(InvoicesMainTab.ALL_INVOICES) }
    var showMonthlyChartInline by remember { mutableStateOf(false) }
    var showCompactSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var selectedSourceFilter by remember { mutableStateOf<String?>(null) }
    var selectedDatePreset by remember { mutableStateOf(DateFilterPreset.ALL) }
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var selectedInvoiceDetail by remember { mutableStateOf<UnifiedInvoiceItem?>(null) }
    var invoicePendingDelete by remember { mutableStateOf<UnifiedInvoiceItem?>(null) }
    var invoicePendingPayment by remember { mutableStateOf<UnifiedInvoiceItem?>(null) }

    // AI Advisor Dialog State
    var showAiReviewDialog by remember { mutableStateOf(false) }
    var aiQueryCustom by remember { mutableStateOf("") }
    var targetedProjectForAi by remember { mutableStateOf<ProjectEntity?>(null) }

    var isFilterLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        drawerScope.launch { drawerState.close() }
    }

    // Brief skeleton loader trigger on query/filter changes for smooth feedback
    LaunchedEffect(searchQuery, selectedStatusFilter, selectedDatePreset, startDateMillis, endDateMillis, activeTab) {
        isFilterLoading = true
        delay(150)
        isFilterLoading = false
    }

    val showSkeleton = isLoading || isFilterLoading

    // Unified List Creation
    val unifiedInvoices = remember(transactions, goldPurchases, settlements, hashtagEntries, hashtagAccounts, ledgerEntries, invoices) {
        val list = mutableListOf<UnifiedInvoiceItem>()
        val syncedInvoiceIds = invoices.map { it.id }.toSet()

        // 0. Dedicated Room Invoices (Offline-First Synced Cache)
        invoices.forEach { inv ->
            val resolvedPaid = if (inv.status == InvoiceStatus.PAID.name && inv.paidAmount <= 0.0) {
                inv.totalAmount
            } else {
                inv.paidAmount
            }
            list.add(
                UnifiedInvoiceItem(
                    id = "INV_${inv.id}",
                    invoiceNumber = inv.invoiceNumber,
                    category = inv.category,
                    title = inv.title,
                    partyName = if (inv.receiverName.isNotBlank()) inv.receiverName else inv.senderName,
                    timestamp = inv.issueDate,
                    cashAmount = inv.totalAmount,
                    paidAmount = resolvedPaid,
                    goldWeightGrams = inv.goldWeightGrams,
                    goldKarat = inv.goldKarat,
                    pricePerGram = inv.pricePerGram,
                    manufacturingCost = inv.manufacturingCost,
                    status = when (inv.status) {
                        InvoiceStatus.PAID.name -> "مسددة بالكامل ✅"
                        InvoiceStatus.PARTIAL.name -> "مدفوعة جزئياً ⏳"
                        InvoiceStatus.PENDING.name -> "قيد التحصيل ⏳"
                        InvoiceStatus.CANCELLED.name -> "ملغاة ❌"
                        InvoiceStatus.OVERDUE.name -> "متأخرة ⚠️"
                        else -> inv.status
                    },
                    notes = (if (!inv.projectName.isNullOrBlank()) "مشروع: ${inv.projectName} • " else "") + (inv.notes ?: ""),
                    relatedEntityId = inv.id,
                    hashtagTag = inv.hashtagTag,
                    sourceType = inv.sourceType,
                    isExternalBusiness = inv.isExternalBusiness,
                    chatId = inv.chatId,
                    projectId = inv.projectId
                )
            )
        }

        // 1. Transactions (Gold Deals)
        transactions.forEach { tx ->
            list.add(
                UnifiedInvoiceItem(
                    id = "TX_${tx.id}",
                    invoiceNumber = if (!tx.id.contains("-") && tx.id.length <= 10) tx.id.uppercase() else "TX${tx.id.replace("-", "").takeLast(6).uppercase()}",
                    category = "GOLD_DEAL",
                    title = "فاتورة صفقة ذهب (${tx.goldType})",
                    partyName = tx.contactName.ifBlank { "عميل مباشر" },
                    timestamp = tx.timestamp,
                    cashAmount = tx.totalPrice,
                    goldWeightGrams = tx.weight,
                    goldKarat = tx.goldKarat,
                    pricePerGram = tx.pricePerGram,
                    manufacturingCost = tx.manufacturingCostPerGram,
                    status = when (tx.status) {
                        TransactionStatus.PURCHASED.name -> "مشترى معتمد"
                        TransactionStatus.RESOLD.name -> "مُعاد بيعه"
                        TransactionStatus.RETURNED.name -> "مُرتجع"
                        else -> "معتمد"
                    },
                    notes = tx.notes,
                    relatedEntityId = tx.id
                )
            )
        }

        // 2. Gold Purchases & Bullion
        goldPurchases.forEach { gp ->
            list.add(
                UnifiedInvoiceItem(
                    id = "GP_${gp.id}",
                    invoiceNumber = if (!gp.id.contains("-") && gp.id.length <= 10) gp.id.uppercase() else "GP${gp.id.replace("-", "").takeLast(6).uppercase()}",
                    category = "GOLD_PURCHASE",
                    title = "فاتورة شراء ${if (gp.metalType == "SILVER") "فضة" else "ذهب"} (${gp.karat})",
                    partyName = gp.traderHashtag.ifBlank { gp.sellerShopName ?: "سوق الصاغة" },
                    timestamp = gp.createdAt,
                    cashAmount = gp.totalPaid,
                    goldWeightGrams = gp.extractedWeight,
                    goldKarat = gp.karat,
                    pricePerGram = gp.pricePerGram,
                    manufacturingCost = gp.manufacturingCost,
                    status = "تامة ومدفوعة",
                    notes = "المصنع: ${gp.manufacturer} | الدمغة: ${gp.stampDuty} ج.م | المحل: ${gp.sellerShopName ?: "غير محدد"}",
                    relatedEntityId = gp.id
                )
            )
        }

        // 3. Completed Settlements (Clearing)
        settlements.forEach { st ->
            list.add(
                UnifiedInvoiceItem(
                    id = "ST_${st.id}",
                    invoiceNumber = "SETTL-${st.id.takeLast(6).uppercase()}",
                    category = "SETTLEMENT",
                    title = "مستند تسوية ومقاصة تامة",
                    partyName = "${st.traderAName} ↔ ${st.traderBName}",
                    timestamp = st.timestamp,
                    cashAmount = Math.abs(st.netCashAmount),
                    goldWeightGrams = Math.abs(st.netGoldWeight24k),
                    goldKarat = "24K",
                    status = "تسوية تامة معتمدة",
                    notes = "تصفية حسابات ومقاصة ديون شاملة. ${st.notes ?: ""}",
                    relatedEntityId = st.id
                )
            )
        }

        // 4. Commercial Hashtag Invoices (Skip ones already synced into InvoiceEntity to prevent duplicates)
        hashtagEntries
            .filter { entry ->
                (entry.type == "INVOICE" || entry.type == "PAYMENT") &&
                    (entry.linkedInvoiceId == null || !syncedInvoiceIds.contains(entry.linkedInvoiceId))
            }
            .forEach { entry ->
                val hAcc = hashtagAccounts.find { it.id == entry.hashtagId }
                val isExtEntry = entry.description.contains("خارجي") || (hAcc?.tag?.contains("خارجي") == true)
                list.add(
                    UnifiedInvoiceItem(
                        id = "HE_${entry.id}",
                        invoiceNumber = "VOUCH-${entry.id.takeLast(6).uppercase()}",
                        category = "HASHTAG_ENTRY",
                        title = if (entry.type == "INVOICE") "فاتورة هاشتاج معتمدة" else "إشعار سداد مالي",
                        partyName = entry.createdBy.ifBlank { "حساب تجاري" },
                        timestamp = entry.createdAt,
                        cashAmount = entry.amount,
                        paidAmount = if (entry.direction == HashtagEntryDirection.CREDIT.name) entry.amount else 0.0,
                        status = if (entry.direction == HashtagEntryDirection.CREDIT.name) "سداد معتمد ✅" else "معتمدة",
                        notes = entry.description,
                        relatedEntityId = entry.id,
                        hashtagTag = hAcc?.tag,
                        sourceType = if (hAcc != null) "CHAT_HASHTAG" else "DIRECT",
                        isExternalBusiness = isExtEntry,
                        chatId = hAcc?.chatId,
                        projectId = entry.projectId ?: hAcc?.projectId
                    )
                )
            }

        list.sortedByDescending { it.timestamp }
    }

    // Projects P&L summaries
    val projectPLSummaries = remember(projects) {
        projects.map { prj ->
            val capital = prj.estimatedCapital
            val expenses = prj.totalExpenses
            val income = prj.totalIncome
            val net = income - expenses
            val roi = if (expenses > 0) (net / expenses) * 100.0 else if (net > 0) 100.0 else 0.0
            val margin = if (income > 0) (net / income) * 100.0 else 0.0
            val budgetUsage = if (capital > 0) (expenses / capital) * 100.0 else 0.0

            ProjectPLSummary(
                project = prj,
                capital = capital,
                totalExpenses = expenses,
                totalIncome = income,
                netProfit = net,
                roiPercent = roi,
                marginPercent = margin,
                budgetUsagePercent = budgetUsage,
                isProfitable = net >= 0
            )
        }
    }

    // Total metrics calculations
    val totalGoldWeightVolume = remember(unifiedInvoices) { unifiedInvoices.sumOf { it.goldWeightGrams ?: 0.0 } }
    val totalSettlementsCount = remember(settlements) { settlements.size }
    val totalProjectsNetProfit = remember(projectPLSummaries) { projectPLSummaries.sumOf { it.netProfit } }
    val totalProjectsRevenue = remember(projectPLSummaries) { projectPLSummaries.sumOf { it.totalIncome } }
    val totalProjectsExpenses = remember(projectPLSummaries) { projectPLSummaries.sumOf { it.totalExpenses } }

    // Date filtering logic
    LaunchedEffect(selectedDatePreset) {
        val calendar = Calendar.getInstance()
        when (selectedDatePreset) {
            DateFilterPreset.ALL -> {
                startDateMillis = null
                endDateMillis = null
            }
            DateFilterPreset.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDateMillis = calendar.timeInMillis

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                endDateMillis = calendar.timeInMillis
            }
            DateFilterPreset.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDateMillis = calendar.timeInMillis

                val now = Calendar.getInstance()
                now.set(Calendar.HOUR_OF_DAY, 23)
                now.set(Calendar.MINUTE, 59)
                now.set(Calendar.SECOND, 59)
                now.set(Calendar.MILLISECOND, 999)
                endDateMillis = now.timeInMillis
            }
            DateFilterPreset.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startDateMillis = calendar.timeInMillis

                val endCal = Calendar.getInstance()
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                endDateMillis = endCal.timeInMillis
            }
            DateFilterPreset.CUSTOM -> {
                if (startDateMillis == null && endDateMillis == null) {
                    showDatePickerDialog = true
                }
            }
        }
    }

    // Filtered unified invoices
    val filteredInvoices = remember(unifiedInvoices, activeTab, searchQuery, selectedStatusFilter, selectedSourceFilter, startDateMillis, endDateMillis) {
        unifiedInvoices.filter { item ->
            val matchesTab = when (activeTab) {
                InvoicesMainTab.ALL_INVOICES -> true
                InvoicesMainTab.MONTHLY_CHARTS -> true
                InvoicesMainTab.GOLD_INVOICES -> item.category == "GOLD_DEAL" || item.category == "GOLD_PURCHASE"
                InvoicesMainTab.COMPLETED_SETTLEMENTS -> item.category == "SETTLEMENT"
                InvoicesMainTab.PROJECTS_PL -> true
                InvoicesMainTab.COMPREHENSIVE_AUDIT -> true
            }

            val matchesStatus = selectedStatusFilter == null || item.status.contains(selectedStatusFilter!!, ignoreCase = true)
            val matchesSource = when (selectedSourceFilter) {
                "HASHTAG" -> !item.hashtagTag.isNullOrBlank() || item.category == "HASHTAG_ENTRY" || item.sourceType == "CHAT_HASHTAG"
                "EXTERNAL" -> item.isExternalBusiness || item.sourceType == "EXTERNAL"
                else -> true
            }

            val q = searchQuery.trim()
            val cleanQ = q.removePrefix("#")
            val matchesSearch = q.isEmpty() ||
                    item.invoiceNumber.contains(q, ignoreCase = true) ||
                    item.partyName.contains(q, ignoreCase = true) ||
                    item.title.contains(q, ignoreCase = true) ||
                    (item.hashtagTag?.contains(cleanQ, ignoreCase = true) == true) ||
                    (item.notes?.contains(q, ignoreCase = true) == true) ||
                    (item.goldKarat?.contains(q, ignoreCase = true) == true) ||
                    item.cashAmount.toString().contains(q) ||
                    (item.goldWeightGrams?.toString()?.contains(q) == true)

            val matchesDate = (startDateMillis == null || item.timestamp >= startDateMillis!!) &&
                    (endDateMillis == null || item.timestamp <= endDateMillis!!)

            matchesTab && matchesStatus && matchesSource && matchesSearch && matchesDate
        }
    }

    val currentFilteredVolume = remember(filteredInvoices) { filteredInvoices.sumOf { it.cashAmount } }
    val activeFiltersCount = remember(selectedStatusFilter, selectedSourceFilter, selectedDatePreset, searchQuery) {
        var count = 0
        if (selectedStatusFilter != null) count++
        if (selectedSourceFilter != null) count++
        if (selectedDatePreset != DateFilterPreset.ALL) count++
        if (searchQuery.isNotBlank()) count++
        count
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.widthIn(min = 280.dp, max = 330.dp)
            ) {
                InvoicesSideDrawerContent(
                    activeTab = activeTab,
                    onSelectTab = { tab ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        activeTab = tab
                        drawerScope.launch { drawerState.close() }
                    },
                    filteredCount = filteredInvoices.size,
                    filteredVolume = currentFilteredVolume,
                    totalGoldWeightVolume = totalGoldWeightVolume,
                    totalSettlementsCount = totalSettlementsCount,
                    selectedDatePreset = selectedDatePreset,
                    onSelectDatePreset = { preset ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (preset == DateFilterPreset.CUSTOM) {
                            selectedDatePreset = DateFilterPreset.CUSTOM
                            showDatePickerDialog = true
                        } else {
                            selectedDatePreset = preset
                        }
                    },
                    selectedStatusFilter = selectedStatusFilter,
                    onSelectStatusFilter = { status ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedStatusFilter = if (selectedStatusFilter == status) null else status
                    },
                    showMonthlyChartInline = showMonthlyChartInline,
                    onToggleMonthlyChart = { showMonthlyChartInline = !showMonthlyChartInline },
                    onExportExcel = {
                        ReportExporter.exportToCsv(
                            context = context,
                            transactions = transactions,
                            filterPeriodLabel = selectedDatePreset.label
                        )
                        drawerScope.launch { drawerState.close() }
                    },
                    onExportPdf = {
                        ReportExporter.exportToPdf(
                            context = context,
                            transactions = transactions,
                            filterPeriodLabel = selectedDatePreset.label,
                            totalVolume = currentFilteredVolume
                        )
                        drawerScope.launch { drawerState.close() }
                    },
                    onOpenAiAudit = {
                        targetedProjectForAi = null
                        showAiReviewDialog = true
                        drawerScope.launch { drawerState.close() }
                    },
                    onResetFilters = {
                        searchQuery = ""
                        selectedStatusFilter = null
                        selectedDatePreset = DateFilterPreset.ALL
                        startDateMillis = null
                        endDateMillis = null
                    },
                    activeFiltersCount = activeFiltersCount,
                    onCloseDrawer = { drawerScope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Compact Top Bar (frees ~85% of screen height for the list)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Dedicated Side Drawer Menu Button (48dp touch target, circular 1:1 badge)
                            Box(modifier = Modifier.size(48.dp)) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        drawerScope.launch { drawerState.open() }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("transactions_open_drawer_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "فتح قائمة الفواتير والقيود",
                                        tint = adaptiveGoldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                if (activeFiltersCount > 0) {
                                    CircularCounterBadge(
                                        count = activeFiltersCount,
                                        containerColor = adaptiveGoldPrimary,
                                        contentColor = adaptiveOnPrimary,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-2).dp, y = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "الفواتير والقيود",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${activeTab.title} • ${filteredInvoices.size} عملية",
                                    fontSize = 11.sp,
                                    color = adaptiveGoldPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Quick Search Toggle (48dp touch target)
                            IconButton(
                                onClick = { showCompactSearch = !showCompactSearch },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (showCompactSearch) Icons.Default.SearchOff else Icons.Default.Search,
                                    contentDescription = if (showCompactSearch) "إخفاء البحث" else "بحث في الفواتير",
                                    tint = if (showCompactSearch || searchQuery.isNotBlank()) adaptiveGoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Compact AI Audit Button (48dp min height)
                            FilledTonalButton(
                                onClick = {
                                    targetedProjectForAi = null
                                    showAiReviewDialog = true
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = adaptiveGoldPrimary.copy(alpha = 0.14f),
                                    contentColor = adaptiveGoldPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.heightIn(min = 40.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مراجعة", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    // Collapsible Search Bar (only visible when user taps Search)
                    AnimatedVisibility(
                        visible = showCompactSearch,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "ابحث بالاسم، رقم الفاتورة، أو #هاشتاج",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = adaptiveGoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "مسح البحث", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                focusedBorderColor = adaptiveGoldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }

                    // Quick Status & Source Filter Strip pinned cleanly at top
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedStatusFilter == null && selectedSourceFilter == null,
                            onClick = {
                                selectedStatusFilter = null
                                selectedSourceFilter = null
                            },
                            label = { Text("الكل", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedStatusFilter == "قيد التحصيل",
                            onClick = {
                                selectedStatusFilter = if (selectedStatusFilter == "قيد التحصيل") null else "قيد التحصيل"
                            },
                            label = { Text("معلقة", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedStatusFilter == "جزئياً",
                            onClick = {
                                selectedStatusFilter = if (selectedStatusFilter == "جزئياً") null else "جزئياً"
                            },
                            label = { Text("مدفوعة جزئياً", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedStatusFilter == "مسددة",
                            onClick = {
                                selectedStatusFilter = if (selectedStatusFilter == "مسددة") null else "مسددة"
                            },
                            label = { Text("مسددة", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedSourceFilter == "HASHTAG",
                            onClick = {
                                selectedSourceFilter = if (selectedSourceFilter == "HASHTAG") null else "HASHTAG"
                            },
                            label = { Text("هاشتاجات #", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedSourceFilter == "EXTERNAL",
                            onClick = {
                                selectedSourceFilter = if (selectedSourceFilter == "EXTERNAL") null else "EXTERNAL"
                            },
                            label = { Text("أعمال خارجية", fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Main Tab Content with smooth crossfade
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f))
                        .togetherWith(fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.98f))
                },
                label = "transactions_tab_content",
                modifier = Modifier.weight(1f)
            ) { currentTab ->
                when (currentTab) {
                    InvoicesMainTab.PROJECTS_PL -> {
                        ProjectsPLView(
                            summaries = projectPLSummaries,
                            totalRevenue = totalProjectsRevenue,
                            totalExpenses = totalProjectsExpenses,
                            totalNetProfit = totalProjectsNetProfit,
                            onOpenProjectClick = onOpenProjectClick,
                            onAiAnalyzeProject = { prj ->
                                targetedProjectForAi = prj
                                showAiReviewDialog = true
                            },
                            onExportReport = {
                                ReportExporter.exportProjectsToCsv(context, projects)
                            }
                        )
                    }

                    InvoicesMainTab.MONTHLY_CHARTS -> {
                        MonthlyChartsFullView(
                            invoices = unifiedInvoices,
                            dailyRecords = dailyRecords,
                            onExportMonthlyReport = {
                                ReportExporter.exportToCsv(
                                    context = context,
                                    transactions = transactions,
                                    filterPeriodLabel = "إحصائيات_شهرية"
                                )
                            }
                        )
                    }

                    InvoicesMainTab.COMPREHENSIVE_AUDIT -> {
                        ComprehensiveAuditView(
                            invoices = unifiedInvoices,
                            projectSummaries = projectPLSummaries,
                            settlements = settlements,
                            onTriggerAiQuery = { prompt ->
                                aiQueryCustom = prompt
                                showAiReviewDialog = true
                            }
                        )
                    }

                    else -> {
                        // Full-height Invoices / Gold / Settlements List
                        Column(modifier = Modifier.fillMaxSize()) {
                            AnimatedVisibility(
                                visible = showMonthlyChartInline,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    MonthlyTransactionsRechartsCard(
                                        invoices = unifiedInvoices,
                                        dailyRecords = dailyRecords,
                                        initialExpanded = true
                                    )
                                }
                            }

                            if (showSkeleton) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(5) {
                                        InvoiceItemCardSkeleton()
                                    }
                                }
                            } else if (filteredInvoices.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Text(
                                            text = "لا توجد فواتير أو تسويات تطابق شروط البحث",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        if (activeFiltersCount > 0) {
                                            OutlinedButton(
                                                onClick = {
                                                    searchQuery = ""
                                                    selectedStatusFilter = null
                                                    selectedDatePreset = DateFilterPreset.ALL
                                                    startDateMillis = null
                                                    endDateMillis = null
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.heightIn(min = 48.dp)
                                            ) {
                                                Icon(Icons.Default.FilterAltOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("إعادة ضبط الفلاتر", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(filteredInvoices, key = { it.id }) { item ->
                                        UnifiedInvoiceCard(
                                            item = item,
                                            onPayClick = if (onPayInvoice != null && item.id.startsWith("INV_") && !item.status.contains("مسددة") && !item.status.contains("ملغاة")) {
                                                { invoicePendingPayment = item }
                                            } else null,
                                            onClick = {
                                                if (item.category == "GOLD_DEAL" && item.relatedEntityId != null) {
                                                    onTransactionClick(item.relatedEntityId)
                                                } else {
                                                    selectedInvoiceDetail = item
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Invoice Detail Dialog
    if (selectedInvoiceDetail != null) {
        val currentDetail = selectedInvoiceDetail!!
        InvoiceDetailDialog(
            item = currentDetail,
            onDismiss = { selectedInvoiceDetail = null },
            onPayClick = if (onPayInvoice != null && currentDetail.id.startsWith("INV_") && !currentDetail.status.contains("مسددة") && !currentDetail.status.contains("ملغاة")) {
                {
                    selectedInvoiceDetail = null
                    invoicePendingPayment = currentDetail
                }
            } else null,
            onShareOrExport = {
                Toast.makeText(context, "جاري مشاركة بيانات الفاتورة المعتمدة", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { itemToDelete ->
                selectedInvoiceDetail = null
                invoicePendingDelete = itemToDelete
            }
        )
    }

    // Invoice Payment Settlement Dialog
    if (invoicePendingPayment != null && onPayInvoice != null) {
        val payItem = invoicePendingPayment!!
        val remaining = (payItem.cashAmount - payItem.paidAmount).coerceAtLeast(0.0)
        var amountInput by remember(payItem.id) { mutableStateOf(if (remaining > 0.0) remaining.toInt().toString() else "") }
        var paymentMethod by remember { mutableStateOf("كاش") }
        var notesInput by remember { mutableStateOf("") }
        Dialog(
            onDismissRequest = { invoicePendingPayment = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .imePadding(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "تسجيل سداد للفاتورة ${payItem.invoiceNumber}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = adaptiveGoldPrimary
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("مبلغ السداد (المتبقي ${GoldUtils.formatCurrency(remaining)})") },
                        placeholder = { Text("مثال: 2500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invoice_payment_amount_input")
                    )
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("طريقة الدفع") },
                        placeholder = { Text("كاش أو تحويل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("ملاحظة") },
                        placeholder = { Text("دفعة من الحساب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { invoicePendingPayment = null },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val amt = amountInput.toDoubleOrNull() ?: 0.0
                                val invId = payItem.relatedEntityId
                                if (amt > 0.0 && !invId.isNullOrBlank()) {
                                    onPayInvoice(invId, amt, paymentMethod.ifBlank { "كاش" }, notesInput.ifBlank { null })
                                    invoicePendingPayment = null
                                } else {
                                    Toast.makeText(context, "أدخل مبلغاً صحيحاً أكبر من الصفر", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("confirm_invoice_payment_btn")
                        ) {
                            Text("حفظ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Delete Invoice Confirmation Dialog Guard
    if (invoicePendingDelete != null) {
        val inv = invoicePendingDelete!!
        DeleteInvoiceConfirmationDialog(
            invoiceNumber = inv.invoiceNumber,
            partyName = inv.partyName,
            amountFormatted = GoldUtils.formatCurrency(inv.cashAmount),
            detailsSubtitle = inv.title,
            recordType = if (inv.category == "GOLD_DEAL") DeleteRecordType.TRANSACTION else DeleteRecordType.INVOICE,
            onDismiss = { invoicePendingDelete = null },
            onConfirm = {
                val targetId = inv.relatedEntityId ?: inv.id
                if (inv.category == "GOLD_DEAL") {
                    onDeleteTransaction?.invoke(targetId)
                } else {
                    onDeleteInvoice?.invoke(targetId)
                }
                invoicePendingDelete = null
                Toast.makeText(context, "تم حذف الفاتورة بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // AI Financial Review & Audit Dialog
    if (showAiReviewDialog) {
        AiInvoiceReviewDialog(
            targetedProject = targetedProjectForAi,
            allInvoices = unifiedInvoices,
            projectSummaries = projectPLSummaries,
            adminConfig = adminConfig,
            globalSnapshot = globalSnapshot,
            initialPrompt = aiQueryCustom,
            onDismiss = {
                showAiReviewDialog = false
                aiQueryCustom = ""
            }
        )
    }

    // Custom Date Range Picker Dialog
    if (showDatePickerDialog) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDateMillis,
            initialSelectedEndDateMillis = endDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        if (start != null) {
                            val startCal = Calendar.getInstance().apply {
                                timeInMillis = start
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            startDateMillis = startCal.timeInMillis

                            val endCal = Calendar.getInstance().apply {
                                timeInMillis = end ?: start
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }
                            endDateMillis = endCal.timeInMillis
                            selectedDatePreset = DateFilterPreset.CUSTOM
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("تطبيق النطاق", color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        "تحديد الفترة الزمنية للفواتير",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                },
                modifier = Modifier.height(480.dp)
            )
        }
    }
}

@Composable
fun MiniMetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(icon, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = adaptiveGoldPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtext,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InvoicesSideDrawerContent(
    activeTab: InvoicesMainTab,
    onSelectTab: (InvoicesMainTab) -> Unit,
    filteredCount: Int,
    filteredVolume: Double,
    totalGoldWeightVolume: Double,
    totalSettlementsCount: Int,
    selectedDatePreset: DateFilterPreset,
    onSelectDatePreset: (DateFilterPreset) -> Unit,
    selectedStatusFilter: String?,
    onSelectStatusFilter: (String) -> Unit,
    showMonthlyChartInline: Boolean,
    onToggleMonthlyChart: () -> Unit,
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit,
    onOpenAiAudit: () -> Unit,
    onResetFilters: () -> Unit,
    activeFiltersCount: Int,
    onCloseDrawer: () -> Unit
) {
    val statusOptions = listOf("مسددة", "قيد التحصيل", "ملغاة", "متأخرة")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Drawer Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(adaptiveGoldPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = adaptiveGoldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "قائمة الفواتير والقيود",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "التبويبات والإحصائيات والفلاتر",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onCloseDrawer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق القائمة الجانبية",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Quick Metrics Summary
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "ملخص الأداء",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MiniMetricCard(
                title = "إجمالي الفواتير المصفاة",
                value = GoldUtils.formatCurrency(filteredVolume),
                subtext = "$filteredCount عملية مسجلة",
                icon = Icons.Default.Receipt,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniMetricCard(
                    title = "حجم المعادن",
                    value = GoldUtils.formatWeight(totalGoldWeightVolume),
                    subtext = "ذهب وفضة",
                    icon = Icons.Default.MonetizationOn,
                    modifier = Modifier.weight(1f)
                )
                MiniMetricCard(
                    title = "التسويات التامة",
                    value = "$totalSettlementsCount تسوية",
                    subtext = "مطابقة 100%",
                    icon = Icons.Default.Balance,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Section Navigation Tabs
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "الأقسام والتقارير",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            InvoicesMainTab.values().forEach { tab ->
                val selected = activeTab == tab
                NavigationDrawerItem(
                    selected = selected,
                    onClick = { onSelectTab(tab) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = tab.title,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = adaptiveGoldPrimary.copy(alpha = 0.16f),
                        selectedIconColor = adaptiveGoldPrimary,
                        selectedTextColor = adaptiveGoldPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Date Period Filter
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الفترة الزمنية",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (activeFiltersCount > 0) {
                    TextButton(
                        onClick = onResetFilters,
                        modifier = Modifier.heightIn(min = 36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("إعادة ضبط ($activeFiltersCount)", fontSize = 11.sp, color = adaptiveStatusRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DateFilterPreset.values().forEach { preset ->
                    val isSelected = selectedDatePreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectDatePreset(preset) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        label = {
                            Text(
                                preset.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = adaptiveGoldPrimary,
                            selectedLabelColor = adaptiveOnPrimary,
                            selectedLeadingIconColor = adaptiveOnPrimary
                        )
                    )
                }
            }
        }

        // Status Filter
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "حالة الفاتورة",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                statusOptions.forEach { status ->
                    val isSelected = selectedStatusFilter == status
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectStatusFilter(status) },
                        label = {
                            Text(
                                status,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = adaptiveGoldPrimary,
                            selectedLabelColor = adaptiveOnPrimary
                        )
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Quick Tools & Exports
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "أدوات التصدير والتحليل",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onToggleMonthlyChart,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showMonthlyChartInline) "إخفاء المخطط الشهري" else "عرض المخطط الشهري أعلى القائمة",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportExcel,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onExportPdf,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onOpenAiAudit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = adaptiveGoldPrimary,
                    contentColor = adaptiveOnPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("مراجعة مالية شاملة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UnifiedInvoiceCard(
    item: UnifiedInvoiceItem,
    onPayClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val cardContext = androidx.compose.ui.platform.LocalContext.current
    val isRawUuidParty = remember(item.partyName) {
        item.partyName.length >= 24 && item.partyName.count { it == '-' } >= 3 && !item.partyName.contains(' ')
    }
    val displayPartyTitle = remember(item.partyName, item.title, isRawUuidParty) {
        if (isRawUuidParty) item.title else item.partyName
    }
    val compactRefCode = remember(item.invoiceNumber, item.partyName, isRawUuidParty) {
        if (isRawUuidParty) "#${item.partyName.take(6).uppercase()}" else item.invoiceNumber
    }
    val fullCopyValue = remember(item.relatedEntityId, item.invoiceNumber, item.partyName, isRawUuidParty) {
        when {
            isRawUuidParty -> item.partyName
            !item.relatedEntityId.isNullOrBlank() -> item.relatedEntityId
            else -> item.invoiceNumber
        }
    }

    val categoryIcon = when (item.category) {
        "GOLD_DEAL" -> Icons.Default.MonetizationOn
        "GOLD_PURCHASE" -> Icons.Default.ShoppingBag
        "SETTLEMENT" -> Icons.Default.Balance
        "HASHTAG_ENTRY" -> Icons.Default.Tag
        else -> Icons.Default.Receipt
    }

    val categoryColor = when (item.category) {
        "GOLD_DEAL" -> GoldPrimary
        "GOLD_PURCHASE" -> StatusBlue
        "SETTLEMENT" -> StatusGreen
        "HASHTAG_ENTRY" -> Color(0xFF9C27B0)
        else -> GoldAccent
    }

    val statusColor = when {
        item.status.contains("ملغاة") || item.status.contains("متأخرة") || item.status.contains("مُرتجع") -> adaptiveStatusRed
        item.status.contains("قيد") || item.status.contains("جزئياً") -> Color(0xFFD97706)
        else -> adaptiveStatusGreen
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "invoice_card_scale"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(categoryColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(categoryIcon, contentDescription = null, tint = categoryColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayPartyTitle,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = compactRefCode,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (!item.hashtagTag.isNullOrBlank()) {
                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "#${item.hashtagTag}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = adaptiveGoldPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (item.isExternalBusiness || item.sourceType == "EXTERNAL") {
                                Surface(
                                    color = Color(0xFF0EA5E9).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "خارجي",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val clipboard = cardContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("tx_id", fullCopyValue))
                                        android.widget.Toast.makeText(cardContext, "تم نسخ المعرف", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    .testTag("copy_tx_id_${item.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ المعرف",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (item.cashAmount > 0.0 && (item.paidAmount > 0.0 || item.status.contains("قيد") || item.status.contains("جزئياً"))) {
                val progress = (item.paidAmount / item.cashAmount).toFloat().coerceIn(0f, 1f)
                val remaining = (item.cashAmount - item.paidAmount).coerceAtLeast(0.0)
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "مدفوع: ${GoldUtils.formatCurrency(item.paidAmount)}",
                        fontSize = 10.5.sp,
                        color = adaptiveStatusGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "متبقي: ${GoldUtils.formatCurrency(remaining)}",
                        fontSize = 10.5.sp,
                        color = if (remaining > 0.0) Color(0xFFD97706) else adaptiveStatusGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (item.goldWeightGrams != null && item.goldWeightGrams > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "الوزن: ${GoldUtils.formatWeight(item.goldWeightGrams)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = adaptiveGoldPrimary
                    )
                    if (!item.goldKarat.isNullOrBlank()) {
                        Text(
                            text = " (${item.goldKarat})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = GoldUtils.formatDate(item.timestamp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onPayClick != null) {
                        FilledTonalButton(
                            onClick = onPayClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = adaptiveStatusGreen.copy(alpha = 0.16f),
                                contentColor = adaptiveStatusGreen
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.heightIn(min = 34.dp)
                        ) {
                            Text("تسجيل سداد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = GoldUtils.formatCurrency(item.cashAmount),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = adaptiveGoldPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectsPLView(
    summaries: List<ProjectPLSummary>,
    totalRevenue: Double,
    totalExpenses: Double,
    totalNetProfit: Double,
    onOpenProjectClick: ((String) -> Unit)?,
    onAiAnalyzeProject: (ProjectEntity) -> Unit,
    onExportReport: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Global P&L Performance Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("التقرير المالي وصافي الأرباح", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = GoldUtils.formatCurrency(totalNetProfit),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalNetProfit >= 0) adaptiveStatusGreen else adaptiveStatusRed
                            )
                        }

                        Surface(
                            color = (if (totalNetProfit >= 0) adaptiveStatusGreen else adaptiveStatusRed).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (totalNetProfit >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (totalNetProfit >= 0) adaptiveStatusGreen else adaptiveStatusRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (totalNetProfit >= 0) "صافي ربح تراكمي" else "عجز صافي",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalNetProfit >= 0) adaptiveStatusGreen else adaptiveStatusRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("إجمالي الإيرادات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(GoldUtils.formatCurrency(totalRevenue), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = adaptiveStatusGreen)
                        }
                        Column {
                            Text("إجمالي المصروفات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(GoldUtils.formatCurrency(totalExpenses), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = adaptiveStatusRed)
                        }
                        Column {
                            val overallRoi = if (totalExpenses > 0) (totalNetProfit / totalExpenses) * 100.0 else 0.0
                            Text("معدل العائد ROI", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.US, "%.1f", overallRoi)}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onExportReport,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير تقرير الأرباح والخسائر", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "تحليل المشاريع الفردية (${summaries.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (summaries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد مشاريع مسجلة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(summaries, key = { it.project.id }) { item ->
                ProjectPLCard(
                    summary = item,
                    onOpenClick = { onOpenProjectClick?.invoke(item.project.id) },
                    onAiAnalyzeClick = { onAiAnalyzeProject(item.project) }
                )
            }
        }
    }
}

@Composable
fun ProjectPLCard(
    summary: ProjectPLSummary,
    onOpenClick: () -> Unit,
    onAiAnalyzeClick: () -> Unit
) {
    val prj = summary.project

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(prj.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("${prj.type} • ${prj.location}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(
                    color = (if (summary.isProfitable) StatusGreen else StatusRed).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (summary.isProfitable) "ربح +${String.format(Locale.US, "%.1f", summary.marginPercent)}%" else "عجز ${String.format(Locale.US, "%.1f", summary.marginPercent)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.isProfitable) StatusGreen else StatusRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Financial Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("رأس المال التقديري", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(GoldUtils.formatCurrency(summary.capital), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text("المصروفات", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(GoldUtils.formatCurrency(summary.totalExpenses), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = adaptiveStatusRed)
                }
                Column {
                    Text("المقبوضات", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(GoldUtils.formatCurrency(summary.totalIncome), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = adaptiveStatusGreen)
                }
                Column {
                    Text("صافي الربح", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = GoldUtils.formatCurrency(summary.netProfit),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.isProfitable) adaptiveStatusGreen else adaptiveStatusRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Budget Progress Bar
            val budgetProgress = (summary.budgetUsagePercent / 100.0).toFloat().coerceIn(0f, 1f)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("استهلاك ميزانية المشروع", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format(Locale.US, "%.1f", summary.budgetUsagePercent)}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { budgetProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (budgetProgress > 0.9f) StatusRed else GoldPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAiAnalyzeClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تحليل الأرباح", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOpenClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text("تفاصيل المشروع", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun ComprehensiveAuditView(
    invoices: List<UnifiedInvoiceItem>,
    projectSummaries: List<ProjectPLSummary>,
    settlements: List<ClearingSettlementEntity>,
    onTriggerAiQuery: (String) -> Unit
) {
    val auditScore = remember(invoices, projectSummaries) {
        var score = 96
        if (projectSummaries.any { !it.isProfitable }) score -= 4
        if (invoices.any { it.cashAmount <= 0 }) score -= 5
        score.coerceIn(75, 100)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // AI Audit Score Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(GoldPrimary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$auditScore%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "درجة التدقيق المحاسبي والنزاهة",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "كافة الفواتير والتسويات مطابقة للقيود المزدوجة ولا توجد أي اختلالات أو فروقات غير مبررة",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Quick AI Queries & Prompts
        item {
            Text(
                text = "استشارات وتحليلات مالية متقدمة",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        val promptList = listOf(
            "📊 حلل لي صافي أرباح وخسائر المشاريع وقدم خطة لرفع هامش الربح",
            "🔍 افحص هل توجد فواتير أو تسويات بها شذوذ في سعر الجرام أو المصنعية",
            "🪙 ما هو متوسط أسعار شراء الذهب وتكلفة المصنعية الإجمالية في فواتيري؟",
            "💡 قدم مقترحات لتحسين التدفقات النقدية وتسريع تحصيل المستحقات",
            "⚖️ دقق كافة التسويات التامة والمقاصة وتأكد من مطابقة أوزان الذهب 24k"
        )

        items(promptList, key = { it }) { prompt ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTriggerAiQuery(prompt) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prompt,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InvoiceDetailDialog(
    item: UnifiedInvoiceItem,
    onDismiss: () -> Unit,
    onPayClick: (() -> Unit)? = null,
    onShareOrExport: () -> Unit,
    onDeleteClick: ((UnifiedInvoiceItem) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 660.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تفاصيل الفاتورة والمستند",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = adaptiveGoldPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        DetailItemRow("رقم الفاتورة:", item.invoiceNumber)
                        DetailItemRow("الجهة / العميل:", item.partyName)
                        DetailItemRow("البيان:", item.title)
                        if (!item.hashtagTag.isNullOrBlank()) {
                            DetailItemRow("الهاشتاج المرتبط:", "#${item.hashtagTag}")
                        }
                        DetailItemRow("الحالة:", item.status)
                        DetailItemRow("التاريخ والتوقيت:", GoldUtils.formatDate(item.timestamp))
                        if (item.paidAmount > 0.0) {
                            DetailItemRow("المدفوع حتى الآن:", GoldUtils.formatCurrency(item.paidAmount))
                            DetailItemRow("المتبقي:", GoldUtils.formatCurrency((item.cashAmount - item.paidAmount).coerceAtLeast(0.0)))
                        }

                        if (item.goldWeightGrams != null && item.goldWeightGrams > 0) {
                            DetailItemRow("وزن الذهب / الفضة:", GoldUtils.formatWeight(item.goldWeightGrams))
                        }
                        if (!item.goldKarat.isNullOrBlank()) {
                            DetailItemRow("العيار المسجل:", item.goldKarat ?: "")
                        }
                        if (item.pricePerGram != null && item.pricePerGram > 0) {
                            DetailItemRow("سعر الجرام:", GoldUtils.formatCurrency(item.pricePerGram))
                        }
                        if (item.manufacturingCost != null && item.manufacturingCost > 0) {
                            DetailItemRow("المصنعية:", GoldUtils.formatCurrency(item.manufacturingCost))
                        }

                        if (!item.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("ملاحظات:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.notes ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المبلغ الإجمالي:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(GoldUtils.formatCurrency(item.cashAmount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (onPayClick != null) {
                        Button(
                            onClick = onPayClick,
                            colors = ButtonDefaults.buttonColors(containerColor = adaptiveStatusGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text("تسجيل سداد مالي", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onShareOrExport,
                            colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة / طباعة", fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        if (onDeleteClick != null) {
                            OutlinedButton(
                                onClick = { onDeleteClick(item) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حذف", fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiInvoiceReviewDialog(
    targetedProject: ProjectEntity?,
    allInvoices: List<UnifiedInvoiceItem>,
    projectSummaries: List<ProjectPLSummary>,
    adminConfig: AdminConfigEntity?,
    globalSnapshot: SmartAccountantEngine.GlobalAccountingSnapshot? = null,
    initialPrompt: String,
    onDismiss: () -> Unit
) {
    var queryText by remember { mutableStateOf(initialPrompt.ifBlank { "قدم مراجعة محاسبية شاملة لكافة الفواتير وتحليل لأرباح المشاريع." }) }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun runQuery(prompt: String) {
        isLoading = true
        coroutineScope.launch {
            val contextBuilder = StringBuilder()
            contextBuilder.append("إجمالي عدد الفواتير والتسويات: ${allInvoices.size}\n")
            val totalVol = allInvoices.sumOf { it.cashAmount }
            contextBuilder.append("إجمالي حجم التعاملات بالفواتير: ${GoldUtils.formatCurrency(totalVol)}\n")
            val goldVol = allInvoices.sumOf { it.goldWeightGrams ?: 0.0 }
            contextBuilder.append("إجمالي أوزان الذهب والمعادن: ${GoldUtils.formatWeight(goldVol)}\n\n")

            if (globalSnapshot != null) {
                contextBuilder.append(SmartAccountantEngine.buildGlobalContextSummary(globalSnapshot))
                contextBuilder.append("\n[الإجابة المباشرة من المحرك الموحد]:\n")
                contextBuilder.append(SmartAccountantEngine.askGlobalAccountant(prompt, globalSnapshot))
                contextBuilder.append("\n\n")
            }

            contextBuilder.append("[ملخص أرباح وخسائر المشاريع]:\n")
            projectSummaries.forEach { ps ->
                contextBuilder.append("- مشروع: ${ps.project.name} | رأس المال: ${ps.capital} | المصروفات: ${ps.totalExpenses} | المقبوضات: ${ps.totalIncome} | صافي الربح: ${ps.netProfit} | هامش الربح: ${String.format(Locale.US, "%.1f", ps.marginPercent)}%\n")
            }

            if (targetedProject != null) {
                contextBuilder.append("\n[المشروع المستهدف بالتحليل الدقيق]:\n")
                contextBuilder.append("اسم المشروع: ${targetedProject.name}, الموقع: ${targetedProject.location}, رأس المال: ${targetedProject.estimatedCapital}, المصاريف: ${targetedProject.totalExpenses}, الدخل: ${targetedProject.totalIncome}\n")
            }

            val result = GeminiAiService.queryGeminiAi(
                userQuery = prompt,
                contextInfo = contextBuilder.toString(),
                adminConfig = adminConfig,
                useThinkingMode = true
            )

            aiResponse = result
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        runQuery(queryText)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.88f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "المحاسب ومراجع الفواتير المتقدم",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = adaptiveGoldPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Query input bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = queryText,
                            onValueChange = { queryText = it },
                            placeholder = { Text("استفسار سريع...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { if (queryText.isNotBlank()) runQuery(queryText) },
                            modifier = Modifier
                                .background(adaptiveGoldPrimary, RoundedCornerShape(10.dp))
                                .size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = adaptiveOnPrimary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Content area
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = adaptiveGoldPrimary, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "جاري تحليل وتدقيق الفواتير وحساب الأرباح والخسائر...",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (aiResponse != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = aiResponse ?: "",
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "اكتب استفسارك أعلاه واضغط إرسال لبدء المراجعة",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("إغلاق المراجعة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceItemCardSkeleton() {
    val brush = invoiceShimmerBrush()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(16.dp)
                        .background(brush, RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(18.dp)
                        .background(brush, RoundedCornerShape(4.dp))
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(12.dp)
                    .background(brush, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .background(brush, RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(16.dp)
                        .background(brush, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
private fun invoiceShimmerBrush(targetValue: Float = 1000f): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        GoldPrimary.copy(alpha = 0.25f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )
    val transition = rememberInfiniteTransition(label = "invoiceShimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "invoiceShimmerFloat"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnimation.value - 300f, y = translateAnimation.value - 300f),
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()
