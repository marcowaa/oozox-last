package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.CompanyAccountingSummary
import com.example.util.GoldUtils
import com.example.util.UnifiedBusinessAccountingSummary
import com.example.util.acctStr

@Composable
fun DashboardContent(
    summary: CompanyAccountingSummary,
    balance: Double,
    initialCapital: Double = 1500000.0,
    todayRevenueAndIn: Double = 0.0,
    todayExpenseAndOut: Double = 0.0,
    pendingComplaintsCount: Int = 0,
    latestDeficitDate: String? = null,
    groupsCount: Int = 0,
    staffCount: Int = 0,
    hiddenProfilesCount: Int = 0,
    unifiedSummary: UnifiedBusinessAccountingSummary? = null,
    onExportUnifiedExcel: (() -> Unit)? = null,
    onQuickAddRecord: () -> Unit = {},
    onQuickRetroComplaint: () -> Unit = {},
    onOpenInvoiceComplaints: () -> Unit = {},
    onInspectLatestDeficit: (String) -> Unit = {},
    onOpenVoiceAssistant: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onNavigate: (Int) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val totalInflow = remember(summary) { summary.totalRevenue + summary.totalIncoming }
    val totalOutflow = remember(summary) { summary.totalOutgoing + summary.totalDeficit }
    val formattedBalance = remember(balance) { GoldUtils.formatCurrency(balance) }
    val formattedIncoming = remember(totalInflow) { GoldUtils.formatCurrency(totalInflow) }
    val formattedOutgoing = remember(totalOutflow) { GoldUtils.formatCurrency(totalOutflow) }

    val growthRatioPercent = remember(summary.netCashflow, initialCapital) {
        if (initialCapital > 0.0) (summary.netCashflow / initialCapital) * 100.0 else 0.0
    }

    val onNavigateVaults = remember(onNavigate) { { onNavigate(0) } }
    val onNavigateDaily = remember(onNavigate) { { onNavigate(1) } }
    val onNavigateEntities = remember(onNavigate) { { onNavigate(2) } }
    val onNavigateStaff = remember(onNavigate) { { onNavigate(3) } }
    val onNavigateCloud = remember(onNavigate) { { onNavigate(4) } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("accounting_dashboard_lazy_column"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Live Audit / Deficit / Pending Complaints Alert Banner (ultra compact)
        val hasUrgentAlerts = summary.totalDeficit > 0 || pendingComplaintsCount > 0 || (unifiedSummary?.unpaidInvoicesCount ?: 0) > 0
        if (hasUrgentAlerts) {
            item {
                AuditAlertBannerCard(
                    totalDeficit = summary.totalDeficit,
                    pendingComplaintsCount = pendingComplaintsCount,
                    unpaidInvoicesCount = unifiedSummary?.unpaidInvoicesCount ?: 0,
                    onInspectDeficit = {
                        if (latestDeficitDate != null) {
                            onInspectLatestDeficit(latestDeficitDate)
                        } else {
                            onNavigateDaily()
                        }
                    },
                    onOpenComplaints = onOpenInvoiceComplaints
                )
            }
        }

        // 2. Hero Executive Treasury Card (unified 16.dp craft + 3-KPI strip + Canvas bar)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .testTag("accounting_hero_treasury_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            adaptiveGoldPrimary.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 3.dp else 1.5.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = acctStr("net_treasury"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formattedBalance,
                                color = adaptiveGoldPrimary,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val isPositiveGrowth = summary.netCashflow >= 0
                        val badgeTint = if (isPositiveGrowth) adaptiveStatusGreen else adaptiveStatusRed
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeTint.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, badgeTint.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPositiveGrowth) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = if (isPositiveGrowth) acctStr("surplus") else acctStr("deficit"),
                                    tint = badgeTint,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${if (isPositiveGrowth) "+" else ""}${String.format(java.util.Locale.US, "%.1f", growthRatioPercent)}%",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTint
                                )
                            }
                        }
                    }

                    CashflowComparisonCanvasBar(
                        inflow = totalInflow,
                        outgoing = summary.totalOutgoing,
                        deficit = summary.totalDeficit
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(adaptiveStatusGreen))
                                Text(acctStr("inflow"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            Text(formattedIncoming, color = adaptiveStatusGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(acctStr("outflow"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(adaptiveStatusRed))
                            }
                            Text(formattedOutgoing, color = adaptiveStatusRed, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroMiniMetricPill(
                            label = acctStr("today_in"),
                            value = GoldUtils.formatCurrency(todayRevenueAndIn),
                            tint = adaptiveStatusGreen,
                            modifier = Modifier.weight(1f)
                        )
                        HeroMiniMetricPill(
                            label = acctStr("today_out"),
                            value = GoldUtils.formatCurrency(todayExpenseAndOut),
                            tint = adaptiveStatusRed,
                            modifier = Modifier.weight(1f)
                        )
                        HeroMiniMetricPill(
                            label = acctStr("deficit"),
                            value = GoldUtils.formatCurrency(summary.totalDeficit),
                            tint = if (summary.totalDeficit > 0) adaptiveStatusOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Executive Command Deck (crisp 1-2 word buttons, 48.dp touch target)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = acctStr("quick_actions"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionPillButton(
                        label = acctStr("cmd_new_record"),
                        icon = Icons.Default.PostAdd,
                        containerColor = adaptiveGoldPrimary,
                        contentColor = adaptiveOnPrimary,
                        testTag = "quick_action_add_record",
                        onClick = onQuickAddRecord
                    )
                    QuickActionPillButton(
                        label = acctStr("cmd_voice"),
                        icon = Icons.Default.Mic,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = adaptiveGoldPrimary,
                        testTag = "quick_action_voice",
                        onClick = onOpenVoiceAssistant
                    )
                    if (onExportUnifiedExcel != null) {
                        QuickActionPillButton(
                            label = acctStr("cmd_excel"),
                            icon = Icons.Default.TableChart,
                            containerColor = adaptiveStatusGreen.copy(alpha = 0.14f),
                            contentColor = adaptiveStatusGreen,
                            testTag = "quick_action_unified_excel",
                            onClick = onExportUnifiedExcel
                        )
                    }
                    QuickActionPillButton(
                        label = acctStr("cmd_complaints"),
                        icon = Icons.Default.ReportProblem,
                        containerColor = adaptiveStatusRed.copy(alpha = 0.14f),
                        contentColor = adaptiveStatusRed,
                        testTag = "quick_action_invoice_complaints",
                        onClick = onOpenInvoiceComplaints
                    )
                    QuickActionPillButton(
                        label = acctStr("cmd_retro"),
                        icon = Icons.Default.History,
                        containerColor = adaptiveStatusOrange.copy(alpha = 0.14f),
                        contentColor = adaptiveStatusOrange,
                        testTag = "quick_action_retro_complaint",
                        onClick = onQuickRetroComplaint
                    )
                    QuickActionPillButton(
                        label = acctStr("cmd_pdf"),
                        icon = Icons.Default.PictureAsPdf,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        testTag = "quick_action_export_pdf",
                        onClick = onExportPdf
                    )
                }
            }
        }

        // 4. Unified Cross-Module Enterprise Position Card
        if (unifiedSummary != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, adaptiveStatusGreen.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unified_excel_summary_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(adaptiveStatusGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = acctStr("enterprise_position"),
                                        tint = adaptiveStatusGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = acctStr("enterprise_position"),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = adaptiveStatusGreen
                                    )
                                    Text(
                                        text = "${unifiedSummary.projectsCount} ${acctStr("projects")} • ${unifiedSummary.totalInvoicesCount} ${acctStr("invoices")}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (onExportUnifiedExcel != null) {
                                FilledTonalButton(
                                    onClick = onExportUnifiedExcel,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = adaptiveStatusGreen.copy(alpha = 0.16f),
                                        contentColor = adaptiveStatusGreen
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .heightIn(min = 42.dp)
                                        .testTag("export_unified_excel_btn")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(acctStr("excel"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UnifiedMetricTile(
                                title = acctStr("invoices"),
                                value = GoldUtils.formatCurrency(unifiedSummary.invoicesOutstanding),
                                color = if (unifiedSummary.invoicesOutstanding > 0) adaptiveStatusOrange else adaptiveStatusGreen,
                                modifier = Modifier.weight(1f)
                            )
                            UnifiedMetricTile(
                                title = acctStr("hashtags"),
                                value = GoldUtils.formatCurrency(unifiedSummary.hashtagsNetReceivable),
                                color = adaptiveStatusBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UnifiedMetricTile(
                                title = acctStr("deficits_label"),
                                value = GoldUtils.formatCurrency(unifiedSummary.shiftDeficitsTotal),
                                color = if (unifiedSummary.shiftDeficitsTotal > 0) adaptiveStatusRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            UnifiedMetricTile(
                                title = acctStr("combined_net"),
                                value = GoldUtils.formatCurrency(unifiedSummary.combinedNetPosition),
                                color = adaptiveGoldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 5. All 5 Accounting Modules Grid
        item {
            Text(
                text = acctStr("departments_nav"),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardCard(
                        title = acctStr("sec_ledger"),
                        subtitle = acctStr("sub_ledger"),
                        badgeText = "${summary.recordsCount}",
                        icon = Icons.Default.ReceiptLong,
                        color = adaptiveGoldPrimary,
                        modifier = Modifier.weight(1f),
                        testTag = "dashboard_card_ledger",
                        onClick = onNavigateDaily
                    )
                    DashboardCard(
                        title = acctStr("sec_groups"),
                        subtitle = acctStr("sub_groups"),
                        badgeText = "$groupsCount",
                        icon = Icons.Default.Business,
                        color = adaptiveStatusGreen,
                        modifier = Modifier.weight(1f),
                        testTag = "dashboard_card_entities",
                        onClick = onNavigateEntities
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardCard(
                        title = acctStr("sec_staff"),
                        subtitle = acctStr("sub_staff"),
                        badgeText = "$staffCount",
                        icon = Icons.Default.People,
                        color = adaptiveStatusBlue,
                        modifier = Modifier.weight(1f),
                        testTag = "dashboard_card_staff",
                        onClick = onNavigateStaff
                    )
                    DashboardCard(
                        title = acctStr("sec_vaults"),
                        subtitle = acctStr("sub_vaults"),
                        badgeText = "$hiddenProfilesCount",
                        icon = Icons.Default.Security,
                        color = adaptiveRealGold,
                        modifier = Modifier.weight(1f),
                        testTag = "dashboard_card_vaults",
                        onClick = onNavigateVaults
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateCloud)
                        .testTag("dashboard_card_cloud"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = if (isDark) 0.dp else 1.dp
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
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = acctStr("sec_backup"),
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = acctStr("sec_backup"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = acctStr("sub_backup"),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditAlertBannerCard(
    totalDeficit: Double,
    pendingComplaintsCount: Int,
    unpaidInvoicesCount: Int,
    onInspectDeficit: () -> Unit,
    onOpenComplaints: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = adaptiveStatusRed.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, adaptiveStatusRed.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("accounting_audit_alert_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = acctStr("pending_complaints"),
                    tint = adaptiveStatusRed,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    if (totalDeficit > 0) {
                        Text(
                            text = "${acctStr("latest_deficit")}: ${GoldUtils.formatCurrency(totalDeficit)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = adaptiveStatusRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (pendingComplaintsCount > 0 || unpaidInvoicesCount > 0) {
                        Text(
                            text = "$pendingComplaintsCount ${acctStr("pending_complaints")} • $unpaidInvoicesCount ${acctStr("invoices")}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (totalDeficit > 0) {
                    FilledTonalButton(
                        onClick = onInspectDeficit,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = adaptiveStatusRed.copy(alpha = 0.18f),
                            contentColor = adaptiveStatusRed
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 38.dp)
                    ) {
                        Text(acctStr("inspect"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (pendingComplaintsCount > 0 || unpaidInvoicesCount > 0) {
                    FilledTonalButton(
                        onClick = onOpenComplaints,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = adaptiveStatusOrange.copy(alpha = 0.18f),
                            contentColor = adaptiveStatusOrange
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 38.dp)
                    ) {
                        Text(acctStr("review"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CashflowComparisonCanvasBar(
    inflow: Double,
    outgoing: Double,
    deficit: Double
) {
    val totalVolume = (inflow + outgoing + deficit).coerceAtLeast(1.0)
    val inflowFraction = (inflow / totalVolume).toFloat().coerceIn(0f, 1f)
    val outgoingFraction = (outgoing / totalVolume).toFloat().coerceIn(0f, 1f)
    val deficitFraction = (deficit / totalVolume).toFloat().coerceIn(0f, 1f)

    val greenColor = adaptiveStatusGreen
    val redColor = adaptiveStatusRed
    val amberColor = adaptiveStatusOrange
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .semantics {
                contentDescription = "شريط مقارنة الوارد والمنصرف والعجز"
            }
    ) {
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        if (inflow + outgoing + deficit <= 0.0) return@Canvas

        var currentX = 0f
        if (inflowFraction > 0f) {
            val w = size.width * inflowFraction
            drawRect(color = greenColor, topLeft = Offset(currentX, 0f), size = Size(w, size.height))
            currentX += w
        }
        if (outgoingFraction > 0f) {
            val w = size.width * outgoingFraction
            drawRect(color = redColor, topLeft = Offset(currentX, 0f), size = Size(w, size.height))
            currentX += w
        }
        if (deficitFraction > 0f) {
            val w = (size.width - currentX).coerceAtLeast(0f)
            drawRect(color = amberColor, topLeft = Offset(currentX, 0f), size = Size(w, size.height))
        }
    }
}

@Composable
private fun HeroMiniMetricPill(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UnifiedMetricTile(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionPillButton(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(17.dp))
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = contentColor, maxLines = 1)
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    badgeText: String? = null,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isDark) color.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        shadowElevation = if (isDark) 0.dp else 1.5.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
                }

                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = color.copy(alpha = 0.13f)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
