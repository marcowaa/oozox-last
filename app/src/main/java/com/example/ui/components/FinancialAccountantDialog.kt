package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.GoldUtils
import com.example.util.SmartAccountantEngine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enterprise Luxury AI Accountant & Financial Auditor
 * Completely redesigned with segmented tabs, high-contrast typography,
 * instant audit metrics, and voice-assisted query chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAccountantDialog(
    chatName: String = "جميع الحسابات والقيود",
    accounts: List<ChatAccountEntity> = emptyList(),
    hashtags: List<HashtagAccountEntity> = emptyList(),
    entries: List<HashtagEntryEntity> = emptyList(),
    transactions: List<TransactionEntity> = emptyList(),
    ledgerEntries: List<LedgerEntryEntity> = emptyList(),
    dailyRecords: List<DailyAccountingRecordEntity> = emptyList(),
    globalSnapshot: SmartAccountantEngine.GlobalAccountingSnapshot? = null,
    onOpenVoiceBusinessAgent: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: المحادثة الذكية, 1: لوحة التدقيق المالي, 2: الاستعلامات السريعة
    var query by remember { mutableStateOf("") }
    var chatHistory by remember {
        mutableStateOf<List<Pair<String, Boolean>>>(
            listOf(
                "أهلاً بك! أنا المحاسب المالي وخبير تسويات الذهب المعتمد .\nقمت بفحص كافة قيودك، دفاتر اليومية، وحركات الذهب، ومستعد للإجابة عن أدق تفاصيل الأرصدة والديون والسيولة." to false
            )
        )
    }

    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Calculated Aggregate Metrics
    val totalTransactionsAmount = remember(transactions) {
        transactions.sumOf { it.totalPrice }
    }
    val totalTransactionsWeight = remember(transactions) {
        transactions.sumOf { it.weight }
    }
    val totalGoldLedgerGrams = remember(ledgerEntries) {
        ledgerEntries.sumOf { it.weightGrams }
    }
    val totalDailyRevenue = remember(dailyRecords) {
        dailyRecords.sumOf { it.dailyRevenue }
    }
    val totalDailyDeficit = remember(dailyRecords) {
        dailyRecords.sumOf { it.deficitAmount }
    }

    val analysis = remember(accounts, hashtags, entries, transactions, ledgerEntries, dailyRecords) {
        val baseAnalysis = SmartAccountantEngine.calculateHealthScore(accounts, hashtags, entries, chatName)
        val enrichedInsights = baseAnalysis.insights.toMutableList()

        if (transactions.isNotEmpty()) {
            enrichedInsights.add("إجمالي حركة الصفقات: ${GoldUtils.formatCurrency(totalTransactionsAmount)} (${transactions.size} صفقة بوزن ${String.format(Locale.US,"%.2f", totalTransactionsWeight)} جرام)")
        }
        if (totalGoldLedgerGrams > 0.0) {
            enrichedInsights.add("رصيد الذهب المتداول بالدفتر العام: ${String.format(Locale.US,"%.2f", totalGoldLedgerGrams)} جرام")
        }
        if (dailyRecords.isNotEmpty()) {
            enrichedInsights.add("سجلات اليومية والورديات: ${dailyRecords.size} سجل (إجمالي الإيرادات: ${GoldUtils.formatCurrency(totalDailyRevenue)} - عجز مسجل: ${GoldUtils.formatCurrency(totalDailyDeficit)})")
        }

        baseAnalysis.copy(insights = enrichedInsights)
    }

    // Scroll chat to bottom on new interaction
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = GoldPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        },
        modifier = Modifier
            .fillMaxHeight(0.94f)
            .testTag("financial_accountant_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // 1. Luxury Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF7C3AED), Color(0xFFA78BFA), GoldPrimary)
                                )
                            )
                            .shadow(6.dp, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "المحاسب المالي",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF7C3AED).copy(alpha = 0.2f),
                                border = BorderStroke(0.8.dp, Color(0xFFA78BFA).copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "V 2.0",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA78BFA),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "نطاق التدقيق: $chatName",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (onOpenVoiceBusinessAgent != null) {
                        OozoxCircleIconButton(
                            onClick = onOpenVoiceBusinessAgent,
                            imageVector = Icons.Default.Mic,
                            contentDescription = "الوكيل الصوتي",
                            tint = OozoxTeal,
                            containerColor = OozoxTeal.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("open_voice_business_agent_from_accountant")
                        )
                    }

                    OozoxCircleIconButton(
                        onClick = onDismiss,
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.testTag("close_accountant_sheet_button")
                    )
                }
            }

            // 2. High-End Tab Switcher
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        "المحادثة والاستشارة" to 0,
                        "لوحة التدقيق المالي" to 1,
                        "الاستعلامات السريعة" to 2
                    )

                    tabs.forEach { (title, index) ->
                        val isSelected = (selectedTab == index)
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTab = index
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) GoldPrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) adaptiveOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 3. Tab Content Switcher
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "accountant_tab_transition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> ChatAndAdvisoryTab(
                        chatHistory = chatHistory,
                        analysis = analysis,
                        listState = listState,
                        clipboard = clipboard,
                        haptic = haptic
                    )

                    1 -> FinancialAuditMetricsTab(
                        analysis = analysis,
                        totalTransactionsAmount = totalTransactionsAmount,
                        totalGoldLedgerGrams = totalGoldLedgerGrams,
                        totalDailyRevenue = totalDailyRevenue,
                        totalDailyDeficit = totalDailyDeficit,
                        transactionsCount = transactions.size
                    )

                    2 -> QuickInquiriesTab(
                        onSelectPrompt = { promptText, replyText ->
                            chatHistory = chatHistory + (promptText to true) + (replyText to false)
                            selectedTab = 0
                        },
                        accounts = accounts,
                        hashtags = hashtags,
                        entries = entries,
                        transactions = transactions,
                        ledgerEntries = ledgerEntries,
                        dailyRecords = dailyRecords,
                        globalSnapshot = globalSnapshot,
                        chatName = chatName,
                        haptic = haptic
                    )
                }
            }

            // 4. Bottom Interactive Input Bar (When in Chat Tab)
            if (selectedTab == 0) {
                Spacer(modifier = Modifier.height(6.dp))

                // Quick horizontal helper chips above input
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickPrompts = listOf(
                        "فواتير غير مدفوعة",
                        "ملخص المشاريع",
                        "الأعمال الخارجية",
                        "ملخص الحسابات",
                        "كم المتبقي؟",
                        "ابحث عن ذهب",
                        "تدقيق صفقات الذهب",
                        "هل يوجد عجز مسجل؟"
                    )
                    items(quickPrompts) { prompt ->
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val reply = when {
                                    globalSnapshot != null -> SmartAccountantEngine.askGlobalAccountant(prompt, globalSnapshot)
                                    prompt.contains("ذهب") -> "تدقيق صفقات وأرصدة الذهب:\n\n• إجمالي رصيد الذهب المتداول: ${String.format(Locale.US,"%.2f", totalGoldLedgerGrams)} جرام\n• عدد صفقات الذهب: ${transactions.size} صفقة\n• الحالة: الحسابات متطابقة مع الدفتر العام."
                                    prompt.contains("عجز") -> if (totalDailyDeficit > 0) "تنبيه: يوجد عجز مسجل بقيمة ${GoldUtils.formatCurrency(totalDailyDeficit)}، يُرجى تدقيق اليومية." else "ممتاز: لا يوجد أي عجز مسجل بالخزينة أو الورديات."
                                    else -> SmartAccountantEngine.askAccountant(prompt, accounts, hashtags, entries, chatName)
                                }
                                chatHistory = chatHistory + (prompt to true) + (reply to false)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = prompt,
                                fontSize = 11.sp,
                                color = GoldLight,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input Bar with Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text("اسأل أو ابحث عن فاتورة أو هاشتاج", fontSize = 12.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("accountant_input_field"),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            val text = query.trim()
                            if (text.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val reply = if (globalSnapshot != null) {
                                    SmartAccountantEngine.askGlobalAccountant(text, globalSnapshot)
                                } else {
                                    SmartAccountantEngine.askAccountant(text, accounts, hashtags, entries, chatName)
                                }
                                chatHistory = chatHistory + (text to true) + (reply to false)
                                query = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(GoldPrimary, GoldLight, GoldDark))
                            )
                            .testTag("accountant_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال",
                            tint = adaptiveOnPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: Interactive AI Advisory & Chat
 */
@Composable
private fun ChatAndAdvisoryTab(
    chatHistory: List<Pair<String, Boolean>>,
    analysis: AIAnalysisResultModel,
    listState: androidx.compose.foundation.lazy.LazyListState,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Quick Health Pill Banner
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = when {
                                analysis.healthScore >= 80 -> StatusGreen
                                analysis.healthScore >= 50 -> StatusOrange
                                else -> StatusRed
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "مؤشر صحة الحسابات: ${analysis.healthScore}/100",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = when {
                            analysis.healthScore >= 80 -> "سليم ومستقر"
                            analysis.healthScore >= 50 -> "يحتاج تدقيق"
                            else -> "تنبيه مخاطر"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            analysis.healthScore >= 80 -> StatusGreen
                            analysis.healthScore >= 50 -> StatusOrange
                            else -> StatusRed
                        }
                    )
                }
            }
        }

        // Chat Message Bubbles
        items(chatHistory) { (msg, isUser) ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    ),
                    color = if (isUser) GoldDark else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isUser) GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.widthIn(max = 320.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isUser) "أنت" else "المحاسب المالي",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) GoldLight else Color(0xFFA78BFA)
                            )

                            if (!isUser) {
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(msg))
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "نسخ الإجابة",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Full Financial Health & Balances Audit Dashboard
 */
@Composable
private fun FinancialAuditMetricsTab(
    analysis: AIAnalysisResultModel,
    totalTransactionsAmount: Double,
    totalGoldLedgerGrams: Double,
    totalDailyRevenue: Double,
    totalDailyDeficit: Double,
    transactionsCount: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Health Score Visual Dial Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "درجة السلامة المالية الشاملة",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${analysis.healthScore}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = when {
                                        analysis.healthScore >= 80 -> StatusGreen
                                        analysis.healthScore >= 50 -> StatusOrange
                                        else -> StatusRed
                                    }
                                )
                                Text(
                                    text = " / 100",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                analysis.healthScore >= 80 -> StatusGreen.copy(alpha = 0.15f)
                                analysis.healthScore >= 50 -> StatusOrange.copy(alpha = 0.15f)
                                else -> StatusRed.copy(alpha = 0.15f)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    analysis.healthScore >= 80 -> StatusGreen.copy(alpha = 0.5f)
                                    analysis.healthScore >= 50 -> StatusOrange.copy(alpha = 0.5f)
                                    else -> StatusRed.copy(alpha = 0.5f)
                                }
                            )
                        ) {
                            Text(
                                text = when {
                                    analysis.healthScore >= 80 -> "تدقيق ممتاز ومستقر"
                                    analysis.healthScore >= 50 -> "يحتاج تسوية فورية"
                                    else -> "تنبيه مخاطر وديون"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    analysis.healthScore >= 80 -> StatusGreen
                                    analysis.healthScore >= 50 -> StatusOrange
                                    else -> StatusRed
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { analysis.healthScore / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when {
                            analysis.healthScore >= 80 -> StatusGreen
                            analysis.healthScore >= 50 -> StatusOrange
                            else -> StatusRed
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // 2. Financial Metrics Summary 2x2 Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Gold Reserves Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("رصيد الذهب المتداول", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.2f", totalGoldLedgerGrams)} جرام",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                        }
                    }

                    // Transactions Total Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إجمالي الصفقات ($transactionsCount)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = GoldUtils.formatCurrency(totalTransactionsAmount),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Daily Revenue Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إيرادات اليومية والورديات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = GoldUtils.formatCurrency(totalDailyRevenue),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Deficit Alert Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (totalDailyDeficit > 0) StatusRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, if (totalDailyDeficit > 0) StatusRed else StatusGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (totalDailyDeficit > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (totalDailyDeficit > 0) StatusRed else StatusGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("العجز المسجل بالخزائن", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (totalDailyDeficit > 0) GoldUtils.formatCurrency(totalDailyDeficit) else "0.00 ج.م (مطابق)",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalDailyDeficit > 0) StatusRed else StatusGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 3. Detailed Insights List
        if (analysis.insights.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("رؤى وتوصيات المساعد المالي:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        for (insight in analysis.insights) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", fontSize = 14.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = insight,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 3: Quick One-Tap Accounting Reports & Inquiries
 */
@Composable
private fun QuickInquiriesTab(
    onSelectPrompt: (prompt: String, reply: String) -> Unit,
    accounts: List<ChatAccountEntity>,
    hashtags: List<HashtagAccountEntity>,
    entries: List<HashtagEntryEntity>,
    transactions: List<TransactionEntity>,
    ledgerEntries: List<LedgerEntryEntity>,
    dailyRecords: List<DailyAccountingRecordEntity>,
    globalSnapshot: SmartAccountantEngine.GlobalAccountingSnapshot? = null,
    chatName: String,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val inquiries = listOf(
        Triple(
            "تدقيق صفقات وتداولات الذهب عيار 21",
            "فحص أوزان الصفقات والذهب بالدفتر ومطابقتها مع سعر الشاشة",
            {
                val goldGrams = ledgerEntries.sumOf { it.weightGrams }
                "تدقيق صفقات وأرصدة الذهب:\n\n• إجمالي رصيد الذهب المتداول بالدفتر: ${String.format(Locale.US,"%.2f", goldGrams)} جرام\n• عدد صفقات الذهب المسجلة: ${transactions.size} صفقة\n• حالة التسوية: قيود الذهب مطابقة للدفتر العام.\n• نصيحة: تابع فروق أسعار عيار 21 وسعر الشاشة عند إغلاق كل صفقة."
            }
        ),
        Triple(
            "تقرير ورديات الشركات ومبيعات اليومية",
            "ملخص الإيرادات والمصروفات والعجز المحتمل بالورديات",
            {
                val count = dailyRecords.size
                val totalRevenue = dailyRecords.sumOf { it.dailyRevenue }
                val totalDeficit = dailyRecords.sumOf { it.deficitAmount }
                "تقرير ورديات ومبيعات الشركات:\n\n• إجمالي السجلات المحفوظة: $count سجل\n• إجمالي الإيرادات المسجلة: ${GoldUtils.formatCurrency(totalRevenue)}\n• العجز المسجل: ${GoldUtils.formatCurrency(totalDeficit)}\n• حالة الخزينة: 100% جاهزة للتصدير وإغلاق الميزانية."
            }
        ),
        Triple(
            "فحص الفواتير والهاشتاجات والمشاريع",
            "تدقيق موحد للفواتير غير المدفوعة والأعمال الخارجية والهاشتاجات",
            {
                if (globalSnapshot != null) {
                    SmartAccountantEngine.askGlobalAccountant("فواتير غير مدفوعة", globalSnapshot)
                } else {
                    SmartAccountantEngine.askAccountant("كم المتبقي لي وعلي؟", accounts, hashtags, entries, chatName)
                }
            }
        ),
        Triple(
            "فحص تنبيهات المخاطر والعجز المالي",
            "التحقق من أي قيود متأخرة أو أرصدة سالبة تحتاج تدخلاً",
            {
                if (globalSnapshot != null) {
                    SmartAccountantEngine.askGlobalAccountant("ملخص المشاريع", globalSnapshot)
                } else {
                    SmartAccountantEngine.askAccountant("فحص تنبيهات الأرصدة", accounts, hashtags, entries, chatName)
                }
            }
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(inquiries) { (title, subtitle, answerProvider) ->
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val reply = answerProvider()
                    onSelectPrompt(title, reply)
                },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                        }

                        Column {
                            Text(
                                text = title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
