package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DailyAccountingRecordEntity
import com.example.data.model.FinancialComplaintEntity
import com.example.data.model.GroupMemberEntity
import com.example.data.model.WorkGroupEntity
import com.example.ui.theme.*
import com.example.util.GoldUtils
import com.example.util.ParsedAccountingImportRow
import com.example.util.ReportExporter
import com.example.util.SimpleExcelEngine
import com.example.util.acctStr
import java.text.SimpleDateFormat
import java.util.*

enum class DateFilterType(val labelKey: String, val icon: ImageVector) {
    ALL("filter_all", Icons.Default.DateRange),
    TODAY("filter_today", Icons.Default.Today),
    YESTERDAY("filter_yesterday", Icons.Default.History),
    LAST_7_DAYS("filter_7days", Icons.Default.CalendarViewWeek),
    THIS_MONTH("filter_month", Icons.Default.CalendarMonth)
}

enum class MovementTypeFilter(val labelKey: String) {
    ALL("filter_all"),
    INCOMING_ONLY("type_in"),
    OUTGOING_ONLY("type_out"),
    DEFICIT_ONLY("type_deficit")
}

@Composable
private fun MovementTypeFilter.resolvedColor(): Color = when (this) {
    MovementTypeFilter.ALL -> adaptiveGoldPrimary
    MovementTypeFilter.INCOMING_ONLY -> adaptiveStatusGreen
    MovementTypeFilter.OUTGOING_ONLY -> adaptiveStatusRed
    MovementTypeFilter.DEFICIT_ONLY -> adaptiveStatusOrange
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedTransactionsLedgerSection(
    records: List<DailyAccountingRecordEntity>,
    groups: List<WorkGroupEntity> = emptyList(),
    members: List<GroupMemberEntity> = emptyList(),
    complaints: List<FinancialComplaintEntity> = emptyList(),
    companyName: String?,
    groupName: String,
    onAddRecordClick: () -> Unit,
    onDeleteRecord: (String) -> Unit,
    onOpenComplaintDialog: () -> Unit = {},
    onInspectDeficit: (String) -> Unit = {},
    onImportRecords: (List<ParsedAccountingImportRow>) -> Unit = {}
) {
    val context = LocalContext.current
    val exportErrorMsg = acctStr("export_error")

    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf(DateFilterType.ALL) }
    var selectedMovementType by remember { mutableStateOf(MovementTypeFilter.ALL) }
    var selectedGroupFilterId by remember { mutableStateOf<String?>(null) }
    var previewMediaUri by remember { mutableStateOf<String?>(null) }

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val parsed = SimpleExcelEngine.parseAccountingCsv(stream)
                    if (parsed.isNotEmpty()) {
                        onImportRecords(parsed)
                    } else {
                        Toast.makeText(context, exportErrorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(context, exportErrorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val yesterdayStr = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
    val sevenDaysAgoMillis = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        cal.timeInMillis
    }
    val currentMonthPrefix = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }

    val filteredRecords = remember(
        records, searchQuery, selectedDateFilter, selectedMovementType, selectedGroupFilterId
    ) {
        val q = searchQuery.trim().lowercase(Locale.getDefault())
        records.filter { record ->
            val compactCode = record.id.replace("-", "").take(8).lowercase(Locale.getDefault())
            val queryMatches = if (q.isBlank()) true else {
                record.staffName.lowercase(Locale.getDefault()).contains(q) ||
                    (record.subGroupId?.lowercase(Locale.getDefault())?.contains(q) == true) ||
                    (record.notes?.lowercase(Locale.getDefault())?.contains(q) == true) ||
                    record.entryDate.contains(q) ||
                    compactCode.contains(q)
            }

            val dateMatches = when (selectedDateFilter) {
                DateFilterType.ALL -> true
                DateFilterType.TODAY -> record.entryDate == todayStr
                DateFilterType.YESTERDAY -> record.entryDate == yesterdayStr
                DateFilterType.LAST_7_DAYS -> record.timestamp >= sevenDaysAgoMillis
                DateFilterType.THIS_MONTH -> record.entryDate.startsWith(currentMonthPrefix)
            }

            val typeMatches = when (selectedMovementType) {
                MovementTypeFilter.ALL -> true
                MovementTypeFilter.INCOMING_ONLY -> (record.dailyRevenue > 0 || record.incomingAmount > 0)
                MovementTypeFilter.OUTGOING_ONLY -> record.outgoingAmount > 0
                MovementTypeFilter.DEFICIT_ONLY -> record.deficitAmount > 0
            }

            val groupMatches = if (selectedGroupFilterId == null) {
                true
            } else {
                val targetName = groups.find { it.id == selectedGroupFilterId }?.name
                record.groupId == selectedGroupFilterId || (targetName != null && record.subGroupId == targetName)
            }

            queryMatches && dateMatches && typeMatches && groupMatches
        }.sortedByDescending { it.timestamp }
    }

    val dynamicSummary = remember(filteredRecords) {
        SimpleExcelEngine.computeSummary(filteredRecords)
    }

    val groupedRecords = remember(filteredRecords) {
        filteredRecords.groupBy { it.entryDate }
    }

    val dateLabel = acctStr(selectedDateFilter.labelKey)
    val typeLabel = acctStr(selectedMovementType.labelKey)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("ledger_section_root")
    ) {
        // Compact Sticky Header + Search + Filters
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = acctStr("sec_ledger"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${filteredRecords.size} / ${records.size} ${acctStr("records_unit")}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { csvImportLauncher.launch(arrayOf("text/*", "application/*")) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("ledger_import_csv_btn")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = acctStr("import_file"), tint = adaptiveStatusBlue)
                        }
                        val ledgerTitle = acctStr("sec_ledger")
                        IconButton(
                            onClick = {
                                val file = SimpleExcelEngine.exportToExcelCsv(
                                    context = context,
                                    groupName = groupName,
                                    companyName = companyName,
                                    records = filteredRecords,
                                    members = members
                                )
                                if (file != null) {
                                    SimpleExcelEngine.shareExcelFile(context, file, ledgerTitle)
                                } else {
                                    Toast.makeText(context, exportErrorMsg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("ledger_export_excel_btn")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = acctStr("export_excel"), tint = adaptiveStatusGreen)
                        }
                        IconButton(
                            onClick = onOpenComplaintDialog,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("ledger_retro_complaint_btn")
                        ) {
                            Icon(Icons.Default.ReportProblem, contentDescription = acctStr("cmd_retro"), tint = adaptiveStatusRed)
                        }
                        IconButton(
                            onClick = onAddRecordClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("ledger_quick_add_btn")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = acctStr("cmd_new_record"), tint = adaptiveGoldPrimary, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(acctStr("ledger_search_hint"), fontSize = 12.5.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.Close, contentDescription = acctStr("close"))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = adaptiveGoldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )

                // Date + Type + Group Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(DateFilterType.entries, key = { it.name }) { filter ->
                        FilterChip(
                            selected = selectedDateFilter == filter,
                            onClick = { selectedDateFilter = filter },
                            label = { Text(acctStr(filter.labelKey), fontSize = 11.5.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = adaptiveGoldPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = adaptiveGoldPrimary
                            )
                        )
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .height(22.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                    items(MovementTypeFilter.entries, key = { it.name }) { filter ->
                        val chipColor = filter.resolvedColor()
                        FilterChip(
                            selected = selectedMovementType == filter,
                            onClick = { selectedMovementType = filter },
                            label = { Text(acctStr(filter.labelKey), fontSize = 11.5.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor.copy(alpha = 0.2f),
                                selectedLabelColor = chipColor
                            )
                        )
                    }
                    if (groups.size > 1) {
                        item {
                            Box(
                                modifier = Modifier
                                    .height(22.dp)
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                        items(groups, key = { it.id }) { grp ->
                            FilterChip(
                                selected = selectedGroupFilterId == grp.id,
                                onClick = {
                                    selectedGroupFilterId = if (selectedGroupFilterId == grp.id) null else grp.id
                                },
                                label = { Text(grp.name, fontSize = 11.5.sp) },
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                }
            }
        }

        // Scrollable Summary Hero Card + Grouped Entries Feed
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val totalIn = dynamicSummary.totalRevenue + dynamicSummary.totalIncoming
                val totalOut = dynamicSummary.totalOutgoing + dynamicSummary.totalDeficit

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 4.dp else 2.dp)
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
                                    text = acctStr("net_filtered"),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = GoldUtils.formatCurrency(dynamicSummary.netCashflow),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (dynamicSummary.netCashflow >= 0) adaptiveStatusGreen else adaptiveStatusRed
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    ReportExporter.exportAccountingRecordsToPdf(
                                        context = context,
                                        groupName = groupName,
                                        companyName = companyName ?: groupName,
                                        records = filteredRecords,
                                        filterCriteriaLabel = "$dateLabel • $typeLabel"
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = adaptiveGoldPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                            }
                        }

                        // Visual Split Progress Bar
                        val volume = (totalIn + totalOut).coerceAtLeast(1.0)
                        val inFrac = (totalIn / volume).toFloat().coerceIn(0f, 1f)
                        val green = adaptiveStatusGreen
                        val red = adaptiveStatusRed
                        val track = MaterialTheme.colorScheme.surfaceVariant
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            drawRoundRect(color = track, size = size, cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()))
                            if (totalIn + totalOut > 0) {
                                val inW = size.width * inFrac
                                drawRect(color = green, topLeft = Offset.Zero, size = Size(inW, size.height))
                                drawRect(color = red, topLeft = Offset(inW, 0f), size = Size((size.width - inW).coerceAtLeast(0f), size.height))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = adaptiveStatusGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(acctStr("inflow"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(GoldUtils.formatCurrency(totalIn), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = adaptiveStatusGreen)
                            }

                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = adaptiveStatusRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(acctStr("outflow"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(GoldUtils.formatCurrency(dynamicSummary.totalOutgoing), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = adaptiveStatusRed)
                            }

                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(acctStr("deficit"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = GoldUtils.formatCurrency(dynamicSummary.totalDeficit),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dynamicSummary.totalDeficit > 0) adaptiveStatusOrange else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (dynamicSummary.totalDeficit > 0) {
                            val firstDeficitDate = filteredRecords.firstOrNull { it.deficitAmount > 0 }?.entryDate ?: todayStr
                            FilledTonalButton(
                                onClick = { onInspectDeficit(firstDeficitDate) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = adaptiveStatusRed.copy(alpha = 0.14f),
                                    contentColor = adaptiveStatusRed
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${acctStr("inspect")} (${GoldUtils.formatCurrency(dynamicSummary.totalDeficit)})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (filteredRecords.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(52.dp))
                            Text(acctStr("no_records"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onAddRecordClick,
                                colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(acctStr("cmd_new_record"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                groupedRecords.forEach { (date, dailyRecords) ->
                    item(key = "header_$date") {
                        val dayComplaintsCount = complaints.count { it.targetDate == date }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (date) {
                                    todayStr -> "${acctStr("filter_today")} • $date"
                                    yesterdayStr -> "${acctStr("filter_yesterday")} • $date"
                                    else -> date
                                },
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = adaptiveGoldPrimary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (dayComplaintsCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = adaptiveStatusRed.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "$dayComplaintsCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = adaptiveStatusRed,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${dailyRecords.size} ${acctStr("records_unit")}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(dailyRecords, key = { it.id }) { record ->
                        ModernAccountingRecordRow(
                            record = record,
                            onDelete = { onDeleteRecord(record.id) },
                            onInspectDeficit = onInspectDeficit,
                            onPreviewAttachment = { uri -> previewMediaUri = uri }
                        )
                    }
                }
            }
        }
    }

    if (previewMediaUri != null) {
        MediaPreviewModal(
            item = MediaPreviewItem(
                uri = previewMediaUri!!,
                caption = acctStr("view_attachment"),
                senderName = groupName,
                timestamp = System.currentTimeMillis(),
                isVideo = com.example.util.FileUtils.isVideoUriString(previewMediaUri!!)
            ),
            onDismiss = { previewMediaUri = null }
        )
    }
}

@Composable
fun ModernAccountingRecordRow(
    record: DailyAccountingRecordEntity,
    onDelete: () -> Unit,
    onInspectDeficit: (String) -> Unit,
    onPreviewAttachment: (String) -> Unit = {}
) {
    val totalIn = record.dailyRevenue + record.incomingAmount
    val totalOut = record.outgoingAmount + record.deficitAmount
    val net = totalIn - totalOut
    val isPositive = net >= 0
    val refCode = remember(record.id) { record.id.replace("-", "").take(8).uppercase(Locale.US) }
    val formattedTime = remember(record.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(record.timestamp))
    }

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (record.deficitAmount > 0) adaptiveStatusOrange.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                record.deficitAmount > 0 -> adaptiveStatusOrange.copy(alpha = 0.16f)
                                isPositive -> adaptiveStatusGreen.copy(alpha = 0.14f)
                                else -> adaptiveStatusRed.copy(alpha = 0.14f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            record.deficitAmount > 0 -> Icons.Default.Warning
                            isPositive -> Icons.Default.ArrowDownward
                            else -> Icons.Default.ArrowUpward
                        },
                        contentDescription = null,
                        tint = when {
                            record.deficitAmount > 0 -> adaptiveStatusOrange
                            isPositive -> adaptiveStatusGreen
                            else -> adaptiveStatusRed
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = record.staffName,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = refCode,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = adaptiveGoldPrimary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "${record.subGroupId ?: acctStr("main_treasury")} • $formattedTime",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isPositive) "+" else ""}${GoldUtils.formatCurrency(net)}",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPositive) adaptiveStatusGreen else adaptiveStatusRed
                    )
                    if (record.deficitAmount > 0) {
                        Text(
                            text = "${acctStr("deficit")}: ${GoldUtils.formatCurrency(record.deficitAmount)}",
                            fontSize = 10.5.sp,
                            color = adaptiveStatusOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    if (!record.notes.isNullOrBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(record.notes, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    if (!record.screenshotUri.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable { onPreviewAttachment(record.screenshotUri) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = record.screenshotUri,
                                contentDescription = acctStr("view_attachment"),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Text(
                                text = acctStr("view_attachment"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = adaptiveGoldPrimary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${record.entryDate} • ${record.staffRole}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (record.deficitAmount > 0) {
                                OutlinedButton(
                                    onClick = { onInspectDeficit(record.entryDate) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text(acctStr("inspect"), fontSize = 11.5.sp, color = adaptiveStatusOrange, fontWeight = FontWeight.Bold)
                                }
                            }
                            OozoxCircleIconButton(
                                onClick = onDelete,
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = acctStr("delete"),
                                tint = adaptiveStatusRed,
                                containerColor = adaptiveStatusRed.copy(alpha = 0.12f)
                            )
                        }
                    }
                }
            }
        }
    }
}
