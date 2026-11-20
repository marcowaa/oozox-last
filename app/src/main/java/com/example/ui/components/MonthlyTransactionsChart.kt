package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyAccountingRecordEntity
import com.example.ui.screens.UnifiedInvoiceItem
import com.example.ui.theme.*
import com.example.util.GoldUtils
import java.util.Calendar

enum class RechartsChartType(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    BAR("أعمدة بيانية", Icons.Default.BarChart),
    AREA("منحنى التدفق", Icons.Default.ShowChart)
}

enum class RechartsPeriod(val label: String, val monthsCount: Int) {
    LAST_6_MONTHS("آخر 6 أشهر", 6),
    FULL_YEAR("12 شهرًا", 12)
}

data class MonthlyTransactionStatItem(
    val year: Int,
    val month: Int, // 1..12
    val monthLabel: String,
    val monthShortName: String,
    val totalVolume: Double,
    val txCount: Int,
    val inflowAmount: Double,
    val outflowAmount: Double,
    val goldWeightGrams: Double,
    val averageAmount: Double = if (txCount > 0) totalVolume / txCount else 0.0,
    val growthPercent: Double? = null // Compared to previous month
)

/**
 * Lightweight, high-performance Material 3 charting component inspired by the Recharts visual design system.
 * Delivers clean CartesianGrid lines, interactive Tooltip cards, responsive rounded bars,
 * and smooth area gradients for user monthly financial transaction statistics in Jetpack Compose.
 */
@Composable
fun MonthlyTransactionsRechartsCard(
    invoices: List<UnifiedInvoiceItem>,
    dailyRecords: List<DailyAccountingRecordEntity> = emptyList(),
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(initialExpanded) }
    var selectedChartType by remember { mutableStateOf(RechartsChartType.BAR) }
    var selectedPeriod by remember { mutableStateOf(RechartsPeriod.LAST_6_MONTHS) }
    var selectedMonthIndex by remember { mutableIntStateOf(-1) }

    // Month name lookup in Arabic
    val arabicMonthNames = remember {
        listOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        )
    }

    // Monthly data aggregation
    val monthlyStats = remember(invoices, dailyRecords, selectedPeriod) {
        val result = mutableListOf<MonthlyTransactionStatItem>()
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // 1..12

        val monthsToGenerate = selectedPeriod.monthsCount

        // Generate target months in chronological order
        val monthSlots = mutableListOf<Pair<Int, Int>>() // Pair(year, month)
        for (i in (monthsToGenerate - 1) downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            monthSlots.add(Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1))
        }

        // Aggregate actual data
        var previousMonthVolume: Double? = null

        val actualHasData = invoices.isNotEmpty() || dailyRecords.isNotEmpty()

        for (slot in monthSlots) {
            val y = slot.first
            val m = slot.second
            val monthName = arabicMonthNames.getOrElse(m - 1) { "شهر $m" }

            var monthTotal = 0.0
            var monthCount = 0
            var monthInflow = 0.0
            var monthOutflow = 0.0
            var monthGold = 0.0

            run {
                // Scan Unified Invoices
                invoices.forEach { item ->
                    val itemCal = Calendar.getInstance().apply { timeInMillis = item.timestamp }
                    if (itemCal.get(Calendar.YEAR) == y && itemCal.get(Calendar.MONTH) + 1 == m) {
                        monthTotal += item.cashAmount
                        monthCount++
                        if (item.category == "GOLD_PURCHASE") {
                            monthOutflow += item.cashAmount
                        } else {
                            monthInflow += item.cashAmount
                        }
                        monthGold += (item.goldWeightGrams ?: 0.0)
                    }
                }

                // Scan Daily Accounting Records
                dailyRecords.forEach { rec ->
                    var recYear = y
                    var recMonth = m
                    var matched = false
                    try {
                        val parts = rec.entryDate.split("-")
                        if (parts.size >= 2) {
                            recYear = parts[0].toIntOrNull() ?: y
                            recMonth = parts[1].toIntOrNull() ?: m
                            matched = (recYear == y && recMonth == m)
                        }
                    } catch (_: Exception) {
                        val recCal = Calendar.getInstance().apply { timeInMillis = rec.timestamp }
                        matched = (recCal.get(Calendar.YEAR) == y && recCal.get(Calendar.MONTH) + 1 == m)
                    }

                    if (matched) {
                        val recSum = rec.incomingAmount + rec.outgoingAmount + rec.dailyRevenue
                        monthTotal += recSum
                        monthInflow += (rec.incomingAmount + rec.dailyRevenue)
                        monthOutflow += rec.outgoingAmount
                        monthCount++
                    }
                }
            }

            val growth = if (previousMonthVolume != null && previousMonthVolume!! > 0) {
                ((monthTotal - previousMonthVolume!!) / previousMonthVolume!!) * 100.0
            } else null

            previousMonthVolume = monthTotal

            result.add(
                MonthlyTransactionStatItem(
                    year = y,
                    month = m,
                    monthLabel = "$monthName $y",
                    monthShortName = monthName,
                    totalVolume = monthTotal,
                    txCount = monthCount,
                    inflowAmount = monthInflow,
                    outflowAmount = monthOutflow,
                    goldWeightGrams = monthGold,
                    growthPercent = growth
                )
            )
        }

        result
    }

    // High level metrics
    val totalPeriodVolume = remember(monthlyStats) { monthlyStats.sumOf { it.totalVolume } }
    val totalPeriodTxCount = remember(monthlyStats) { monthlyStats.sumOf { it.txCount } }
    val peakMonth = remember(monthlyStats) { monthlyStats.maxByOrNull { it.totalVolume } }
    val averageMonthlyVolume = remember(monthlyStats, totalPeriodVolume) {
        if (monthlyStats.isNotEmpty()) totalPeriodVolume / monthlyStats.size else 0.0
    }
    val hasAnyData = remember(monthlyStats) { monthlyStats.any { it.totalVolume > 0 || it.txCount > 0 } }

    // Active tooltip selection (defaults to last month if not selected)
    val activeSelectedItem = remember(selectedMonthIndex, monthlyStats) {
        if (selectedMonthIndex in monthlyStats.indices) {
            monthlyStats[selectedMonthIndex]
        } else if (monthlyStats.isNotEmpty()) {
            monthlyStats.lastOrNull()
        } else null
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.2.dp,
            Brush.horizontalGradient(
                listOf(
                    GoldPrimary.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    GoldAccent.copy(alpha = 0.35f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with Recharts badge and toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GoldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertChartOutlined,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "إحصائيات المعاملات الشهرية",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GoldPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Recharts Style 📊",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "تحليل حجم المبيعات والصفقات والتدفق المالي الشهري",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Expand / Collapse Chevron
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isExpanded = !isExpanded
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "طي" else "توسيع",
                        tint = GoldPrimary
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Controls Row: Chart Type Selector + Period Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Chart Type Switcher (Bar vs Area)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                RechartsChartType.entries.forEach { type ->
                                    val isSelected = selectedChartType == type
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) GoldPrimary else Color.Transparent)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedChartType = type
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = type.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = if (isSelected) adaptiveOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = type.label,
                                                fontSize = 10.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) adaptiveOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Period Selector (6 vs 12 Months)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            RechartsPeriod.entries.forEach { period ->
                                val isSelected = selectedPeriod == period
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) GoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = BorderStroke(
                                        0.8.dp,
                                        if (isSelected) GoldPrimary else Color.Transparent
                                    ),
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedPeriod = period
                                        selectedMonthIndex = -1
                                    }
                                ) {
                                    Text(
                                        text = period.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Summary Stat Pills Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SummaryStatPill(
                            title = "حجم التداول",
                            value = GoldUtils.formatCurrency(totalPeriodVolume),
                            subtext = "$totalPeriodTxCount عملية",
                            icon = Icons.Default.AccountBalanceWallet,
                            color = GoldPrimary,
                            modifier = Modifier.weight(1.1f)
                        )
                        SummaryStatPill(
                            title = "أعلى شهر",
                            value = peakMonth?.monthShortName ?: "-",
                            subtext = GoldUtils.formatCurrency(peakMonth?.totalVolume ?: 0.0),
                            icon = Icons.Default.TrendingUp,
                            color = StatusGreen,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatPill(
                            title = "المعدل الشهري",
                            value = GoldUtils.formatCurrency(averageMonthlyVolume),
                            subtext = "متوسط النشاط",
                            icon = Icons.Default.Equalizer,
                            color = StatusBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Empty State prompt if no data exists
                    if (!hasAnyData) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "لا توجد معاملات مسجلة في هذه الفترة حتى الآن",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "ستظهر البيانات والإحصائيات هنا تلقائياً عند تسجيل أول معاملة أو فاتورة.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Tooltip Card (Recharts <Tooltip />)
                    if (activeSelectedItem != null) {
                        RechartsFloatingTooltip(
                            item = activeSelectedItem,
                            onDismiss = { selectedMonthIndex = -1 }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Main Visual Canvas Container (Recharts <ResponsiveContainer />)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        if (selectedChartType == RechartsChartType.BAR) {
                            RechartsMonthlyBarChart(
                                items = monthlyStats,
                                selectedIndex = selectedMonthIndex,
                                onSelectMonth = { idx ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedMonthIndex = if (selectedMonthIndex == idx) -1 else idx
                                }
                            )
                        } else {
                            RechartsMonthlyAreaChart(
                                items = monthlyStats,
                                selectedIndex = selectedMonthIndex,
                                onSelectMonth = { idx ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedMonthIndex = if (selectedMonthIndex == idx) -1 else idx
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Recharts Legend Row (<Legend />)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RechartsLegendItem(
                                color = GoldPrimary,
                                label = "المقبوضات والوارد"
                            )
                            RechartsLegendItem(
                                color = StatusOrange,
                                label = "المصروفات والمشتريات"
                            )
                            RechartsLegendItem(
                                color = StatusBlue,
                                label = "المتوسط الشهري"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts-style Bar Chart implementation:
 * Displays dual or stacked rounded bars per month with subtle grid guide lines (CartesianGrid)
 * and active selection highlight.
 */
@Composable
private fun RechartsMonthlyBarChart(
    items: List<MonthlyTransactionStatItem>,
    selectedIndex: Int,
    onSelectMonth: (Int) -> Unit
) {
    if (items.isEmpty()) return

    val maxVal = remember(items) {
        val peak = items.maxOfOrNull { maxOf(it.inflowAmount, it.outflowAmount, it.totalVolume) } ?: 1.0
        peak.coerceAtLeast(1.0)
    }

    // Smooth animation on data/period update
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(items) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Cartesian Grid background (Dashed horizontal lines)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height - 24.dp.toPx() // leave space for X-axis labels
            val w = size.width
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            // 4 Guide Lines (0%, 33%, 66%, 100%)
            for (i in 0..3) {
                val y = h * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }
        }

        // Columns layout for each month
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                val inflowFrac = ((item.inflowAmount / maxVal) * animProgress.value).toFloat().coerceIn(0f, 1f)
                val outflowFrac = ((item.outflowAmount / maxVal) * animProgress.value).toFloat().coerceIn(0f, 1f)
                val totalFrac = ((item.totalVolume / maxVal) * animProgress.value).toFloat().coerceIn(0f, 1f)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelectMonth(index) }
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Bar graphics container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 4.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Background guide track (like Recharts bar background)
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 22.dp else 18.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (isSelected) GoldPrimary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                        )

                        // Dual Bars: Inflow (Gold) + Outflow (Orange)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            // Inflow Bar
                            val inHeightFrac = if (item.totalVolume > 0 && inflowFrac <= 0.05f) 0.05f else inflowFrac
                            Box(
                                modifier = Modifier
                                    .width(if (items.size > 8) 7.dp else 9.dp)
                                    .fillMaxHeight(inHeightFrac.coerceIn(0.04f, 1f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                GoldPrimary,
                                                GoldPrimary.copy(alpha = 0.65f)
                                            )
                                        )
                                    )
                            )

                            // Outflow Bar
                            val outHeightFrac = if (item.totalVolume > 0 && outflowFrac <= 0.05f) 0.03f else outflowFrac
                            Box(
                                modifier = Modifier
                                    .width(if (items.size > 8) 7.dp else 9.dp)
                                    .fillMaxHeight(outHeightFrac.coerceIn(0.02f, 1f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                StatusOrange,
                                                StatusOrange.copy(alpha = 0.65f)
                                            )
                                        )
                                    )
                            )
                        }

                        // Selected indicator glow dot
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary)
                            )
                        }
                    }

                    // Month X-Axis Label
                    Text(
                        text = item.monthShortName,
                        fontSize = if (items.size > 8) 9.sp else 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Recharts-style Area & Trend Line Chart implementation:
 * Features a smooth Bézier cubic spline, lush gradient fill underneath (<Area />),
 * glowing line stroke, and interactive anchor dots (<Dot />).
 */
@Composable
private fun RechartsMonthlyAreaChart(
    items: List<MonthlyTransactionStatItem>,
    selectedIndex: Int,
    onSelectMonth: (Int) -> Unit
) {
    if (items.isEmpty()) return

    val maxVal = remember(items) {
        (items.maxOfOrNull { it.totalVolume } ?: 1.0).coerceAtLeast(1.0)
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(items) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(items) {
                    detectTapGestures { offset ->
                        val n = items.size
                        if (n > 0) {
                            val slotWidth = size.width / n
                            val tappedIdx = (offset.x / slotWidth).toInt().coerceIn(0, n - 1)
                            onSelectMonth(tappedIdx)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartWidth = size.width
                val chartHeight = size.height
                val n = items.size

                if (n < 2) return@Canvas

                val stepX = chartWidth / (n - 1)
                val points = items.mapIndexed { index, item ->
                    val x = index * stepX
                    val normalized = ((item.totalVolume / maxVal) * animProgress.value).toFloat().coerceIn(0f, 1f)
                    val y = chartHeight - (normalized * (chartHeight - 16.dp.toPx())) - 8.dp.toPx()
                    Offset(x, y)
                }

                // Cartesian Grid lines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                for (i in 0..2) {
                    val gy = chartHeight * (i / 2f)
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, gy),
                        end = Offset(chartWidth, gy),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                // Construct smooth cubic Bézier path
                val strokePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val controlX1 = p0.x + (p1.x - p0.x) / 2f
                        val controlY1 = p0.y
                        val controlX2 = p0.x + (p1.x - p0.x) / 2f
                        val controlY2 = p1.y
                        cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                    }
                }

                // Fill Path for Area Gradient
                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(points.last().x, chartHeight)
                    lineTo(points.first().x, chartHeight)
                    close()
                }

                // Draw Gradient Fill under the curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GoldPrimary.copy(alpha = 0.42f),
                            GoldPrimary.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = chartHeight
                    )
                )

                // Draw Smooth Line Stroke
                drawPath(
                    path = strokePath,
                    color = GoldPrimary,
                    style = Stroke(
                        width = 2.6.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Draw Data Dots (<Dot />)
                points.forEachIndexed { idx, pt ->
                    val isSelected = (idx == selectedIndex)
                    if (isSelected) {
                        // Outer Halo
                        drawCircle(
                            color = GoldPrimary.copy(alpha = 0.28f),
                            radius = 9.dp.toPx(),
                            center = pt
                        )
                        // Inner Dot
                        drawCircle(
                            color = GoldPrimary,
                            radius = 4.5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    } else {
                        drawCircle(
                            color = surfaceColor,
                            radius = 3.5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = GoldPrimary,
                            radius = 3.dp.toPx(),
                            center = pt,
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                    }
                }
            }
        }

        // X-Axis Month Labels Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                Text(
                    text = item.monthShortName,
                    fontSize = if (items.size > 8) 9.sp else 10.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onSelectMonth(index) }
                )
            }
        }
    }
}

/**
 * Recharts Floating Tooltip (<Tooltip />)
 * Appears dynamically when the user taps on any month bar or data node.
 */
@Composable
private fun RechartsFloatingTooltip(
    item: MonthlyTransactionStatItem,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row: Month Name & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "تفاصيل ${item.monthLabel}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }

                // Growth badge vs previous month
                if (item.growthPercent != null) {
                    val isPositive = item.growthPercent >= 0
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPositive) StatusGreen.copy(alpha = 0.15f) else StatusRed.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.1f", item.growthPercent)}% ${if (isPositive) "▲" else "▼"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) StatusGreen else StatusRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Stats grid in tooltip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("إجمالي المعاملات:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = GoldUtils.formatCurrency(item.totalVolume),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text("عدد العمليات:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${item.txCount} عملية",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text("المقبوضات / الوارد:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = GoldUtils.formatCurrency(item.inflowAmount),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusGreen
                    )
                }

                Column {
                    Text("المصروفات / الصادر:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = GoldUtils.formatCurrency(item.outflowAmount),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusOrange
                    )
                }
            }

            if (item.goldWeightGrams > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = RealGoldYellow,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "حجم الذهب المتداول: ${GoldUtils.formatWeight(item.goldWeightGrams)}",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Recharts Legend Item (<Legend />)
 */
@Composable
private fun RechartsLegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Metric summary pill card for top indicators.
 */
@Composable
private fun SummaryStatPill(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = title,
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtext,
                fontSize = 8.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * Full dedicated tab view for Monthly Transactions Analytics & Recharts Data Visualizer.
 */
@Composable
fun MonthlyChartsFullView(
    invoices: List<UnifiedInvoiceItem>,
    dailyRecords: List<DailyAccountingRecordEntity> = emptyList(),
    onExportMonthlyReport: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // Breakdown by financial categories
    val totalGoldVolume = remember(invoices) {
        invoices.filter { it.category == "GOLD_DEAL" || it.category == "GOLD_PURCHASE" }.sumOf { it.cashAmount }
    }
    val goldCount = remember(invoices) {
        invoices.count { it.category == "GOLD_DEAL" || it.category == "GOLD_PURCHASE" }
    }

    val totalCommercialInvoices = remember(invoices) {
        invoices.filter { it.category == "INVOICE" || it.category == "HASHTAG_ENTRY" }.sumOf { it.cashAmount }
    }
    val commercialCount = remember(invoices) {
        invoices.count { it.category == "INVOICE" || it.category == "HASHTAG_ENTRY" }
    }

    val totalSettlements = remember(invoices) {
        invoices.filter { it.category == "SETTLEMENT" }.sumOf { it.cashAmount }
    }
    val settlementsCount = remember(invoices) {
        invoices.count { it.category == "SETTLEMENT" }
    }

    val totalDailyJournal = remember(dailyRecords) {
        dailyRecords.sumOf { it.incomingAmount + it.outgoingAmount + it.dailyRevenue }
    }
    val dailyJournalCount = remember(dailyRecords) { dailyRecords.size }

    val grandTotal = (totalGoldVolume + totalCommercialInvoices + totalSettlements + totalDailyJournal).coerceAtLeast(1.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Main Recharts Interactive Card
        MonthlyTransactionsRechartsCard(
            invoices = invoices,
            dailyRecords = dailyRecords,
            initialExpanded = true
        )

        // Section: Distribution by Categories
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "توزيع العمليات حسب الفئة المالية",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${invoices.size + dailyRecords.size} قيد وسجل",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress ratio bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                ) {
                    if (totalGoldVolume > 0) {
                        Box(
                            modifier = Modifier
                                .weight((totalGoldVolume / grandTotal).toFloat().coerceIn(0.05f, 1f))
                                .fillMaxHeight()
                                .background(RealGoldAmber)
                        )
                    }
                    if (totalCommercialInvoices > 0) {
                        Box(
                            modifier = Modifier
                                .weight((totalCommercialInvoices / grandTotal).toFloat().coerceIn(0.05f, 1f))
                                .fillMaxHeight()
                                .background(GoldPrimary)
                        )
                    }
                    if (totalSettlements > 0) {
                        Box(
                            modifier = Modifier
                                .weight((totalSettlements / grandTotal).toFloat().coerceIn(0.05f, 1f))
                                .fillMaxHeight()
                                .background(StatusBlue)
                        )
                    }
                    if (totalDailyJournal > 0) {
                        Box(
                            modifier = Modifier
                                .weight((totalDailyJournal / grandTotal).toFloat().coerceIn(0.05f, 1f))
                                .fillMaxHeight()
                                .background(StatusGreen)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detailed distribution rows
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryDistributionRow(
                        color = RealGoldAmber,
                        title = "صفقات وتجارة الذهب",
                        count = goldCount,
                        amount = totalGoldVolume,
                        percent = (totalGoldVolume / grandTotal * 100).toInt()
                    )
                    CategoryDistributionRow(
                        color = GoldPrimary,
                        title = "فواتير تجارية واعتمادات",
                        count = commercialCount,
                        amount = totalCommercialInvoices,
                        percent = (totalCommercialInvoices / grandTotal * 100).toInt()
                    )
                    CategoryDistributionRow(
                        color = StatusBlue,
                        title = "تسويات ومقاصات تامة",
                        count = settlementsCount,
                        amount = totalSettlements,
                        percent = (totalSettlements / grandTotal * 100).toInt()
                    )
                    CategoryDistributionRow(
                        color = StatusGreen,
                        title = "سجلات محاسبة ودفتر يومية",
                        count = dailyJournalCount,
                        amount = totalDailyJournal,
                        percent = (totalDailyJournal / grandTotal * 100).toInt()
                    )
                }
            }
        }

        // Export Action Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = GoldPrimary.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "تصدير التقرير الشهري الشامل",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Text(
                            text = "حفظ ومشاركة إحصائيات المعاملات الشهرية بصيغة Excel / CSV",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onExportMonthlyReport,
                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("تصدير 📊", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CategoryDistributionRow(
    color: Color,
    title: String,
    count: Int,
    amount: Double,
    percent: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "($count عملية)",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = GoldUtils.formatCurrency(amount),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "$percent%",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

