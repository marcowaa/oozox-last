package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ProjectEntity
import com.example.ui.theme.*
import com.example.util.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelStudioDialog(
    project: ProjectEntity,
    onLoadSheet: (suspend (String, String?) -> Pair<SmartSpreadsheetTemplate, MutableList<SmartSpreadsheetRow>>)? = null,
    onSaveSheet: ((String, SmartSpreadsheetTemplate, List<SmartSpreadsheetRow>) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Load or initialize spreadsheet state for this project
    val initialData = remember(project.id) {
        SmartExcelTemplateEngine.getOrCreateProjectSheet(project.id, project.type)
    }

    var currentTemplate by remember { mutableStateOf(initialData.first) }
    var sheetRows by remember { mutableStateOf<List<SmartSpreadsheetRow>>(initialData.second) }

    LaunchedEffect(project.id) {
        if (onLoadSheet != null) {
            runCatching { onLoadSheet(project.id, project.type) }.getOrNull()?.let { loaded ->
                currentTemplate = loaded.first
                sheetRows = loaded.second.toList()
                SmartExcelTemplateEngine.updateProjectSheet(project.id, loaded.first, loaded.second)
            }
        }
    }

    // Active tab: 0 = Live Spreadsheet, 1 = Smart NLP Hidden Formulas, 2 = Proactive Virtual Accountant
    var selectedTab by remember { mutableIntStateOf(0) }

    // NLP Formula state
    var naturalLanguageFormulaPrompt by remember {
        mutableStateOf("أريد حساب صافي الربح بعد خصم 14% ضريبة و 5% هالك وإضافة 500 حافز")
    }
    var lastFormulaResultExplanation by remember { mutableStateOf<String?>(null) }
    var isProcessingFormula by remember { mutableStateOf(false) }

    // Add new row dialog
    var showAddRowDialog by remember { mutableStateOf(false) }
    var newRowTitle by remember { mutableStateOf("") }
    var newRowCategory by remember { mutableStateOf("عام") }
    var newRowValue1 by remember { mutableStateOf("") }
    var newRowValue2 by remember { mutableStateOf("") }
    var newRowValue3 by remember { mutableStateOf("") }
    var newRowNotes by remember { mutableStateOf("") }
    var newRowPhotoUri by remember { mutableStateOf<String?>(null) }
    var newRowVideoUri by remember { mutableStateOf<String?>(null) }

    // Media Viewer preview
    var previewMediaUri by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // Pair(Uri, isVideo)

    // Virtual Accountant Chat
    var accountantQuery by remember { mutableStateOf("") }
    var accountantChatMessages by remember {
        mutableStateOf(
            listOf(
                Pair(
                    "المساعد المالي",
                    "أهلاً بك! أنا محاسبك المرافق لمشروع (${project.name}). أتابع معك الشيت الإكسل في الخلفية لحظة بلحظة، وأراجع هوامش الأرباح والتكاليف والضرائب. اسألني أي سؤال أو اطلب أي عملية حسابية تريدها!"
                )
            )
        )
    }

    // Proactive Audit calculation
    val accountantAudit = remember(currentTemplate, sheetRows) {
        SmartExcelTemplateEngine.performVirtualAccountantAudit(
            template = currentTemplate,
            rows = sheetRows,
            estimatedCapital = project.estimatedCapital
        )
    }

    // Helper to persist changes
    val saveSheetChanges = {
        SmartExcelTemplateEngine.updateProjectSheet(project.id, currentTemplate, sheetRows)
        onSaveSheet?.invoke(project.id, currentTemplate, sheetRows)
        Unit
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp
                ) {
                    Column {
                        TopAppBar(
                            title = {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.TableChart,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "منظومة الإكسل والمساعد المالي",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${project.name} • ${currentTemplate.templateName}",
                                        fontSize = 11.sp,
                                        color = GoldPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            },
                            actions = {
                                // Export to Excel Button
                                IconButton(
                                    onClick = {
                                        val file = SmartExcelTemplateEngine.exportSmartSpreadsheetToExcel(
                                            context = context,
                                            template = currentTemplate,
                                            rows = sheetRows,
                                            projectName = project.name
                                        )
                                        if (file != null) {
                                            SmartExcelTemplateEngine.shareExcelFile(
                                                context = context,
                                                file = file,
                                                title = "تصدير شيت إكسل المشروع - ${project.name}"
                                            )
                                        } else {
                                            Toast.makeText(context, "فشل تصدير ملف الإكسل", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.testTag("export_smart_excel_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "تصدير ومشاركة Excel",
                                        tint = Color(0xFF10B981)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                        )

                        // 3 Primary Tabs
                        PrimaryTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = GoldPrimary
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("شيت الإكسل التفاعلي", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("الخانات باللغة الطبيعية", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("المحاسب المرافق", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                        if (accountantAudit.alerts.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                color = Color(0xFFEF4444),
                                                shape = CircleShape,
                                                modifier = Modifier.size(8.dp)
                                            ) {}
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> LiveSpreadsheetTab(
                        template = currentTemplate,
                        rows = sheetRows,
                        onSelectActivity = { newType ->
                            currentTemplate = SmartExcelTemplateEngine.getTemplateForActivity(newType)
                            sheetRows = currentTemplate.defaultRows.map { it.copy(values = HashMap(it.values), calculatedResults = HashMap(it.calculatedResults)) }
                            saveSheetChanges()
                        },
                        onAddRowClick = { showAddRowDialog = true },
                        onDeleteRow = { rowId ->
                            sheetRows = sheetRows.filter { it.id != rowId }
                            saveSheetChanges()
                        },
                        onPreviewMedia = { uri, isVid -> previewMediaUri = Pair(uri, isVid) }
                    )

                    1 -> NaturalLanguageFormulasTab(
                        template = currentTemplate,
                        rows = sheetRows,
                        prompt = naturalLanguageFormulaPrompt,
                        onPromptChange = { naturalLanguageFormulaPrompt = it },
                        isProcessing = isProcessingFormula,
                        explanation = lastFormulaResultExplanation,
                        onExecuteFormula = {
                            isProcessingFormula = true
                            var computedExplanation = ""
                            val updatedRows = sheetRows.map { row ->
                                val res = SmartExcelTemplateEngine.evaluateNaturalLanguageFormula(
                                    userInputPrompt = naturalLanguageFormulaPrompt,
                                    currentRow = row,
                                    template = currentTemplate
                                )
                                computedExplanation = res.first
                                val newCalculated = HashMap(row.calculatedResults)
                                newCalculated["nlp_custom_calc"] = res.second
                                row.copy(calculatedResults = newCalculated)
                            }
                            sheetRows = updatedRows
                            lastFormulaResultExplanation = computedExplanation
                            isProcessingFormula = false
                            saveSheetChanges()
                            Toast.makeText(context, "تمت المعالجة في الخلفية وتحديث كافة خلايا الشيت بدقة فائقة!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    2 -> VirtualAccountantAdvisorTab(
                        report = accountantAudit,
                        template = currentTemplate,
                        project = project,
                        chatMessages = accountantChatMessages,
                        query = accountantQuery,
                        onQueryChange = { accountantQuery = it },
                        onSendMessage = {
                            if (accountantQuery.isNotBlank()) {
                                val userQ = accountantQuery.trim()
                                val response = SmartExcelTemplateEngine.askProjectAccountant(
                                    question = userQ,
                                    template = currentTemplate,
                                    rows = sheetRows,
                                    report = accountantAudit
                                )
                                accountantChatMessages = accountantChatMessages + Pair("أنت", userQ) + Pair("المساعد المالي", response)
                                accountantQuery = ""
                            }
                        }
                    )
                }

                // Add Row Modal Dialog
                if (showAddRowDialog) {
                    AlertDialog(
                        onDismissRequest = { showAddRowDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, tint = GoldPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إضافة بند جديد للشيت المحاسبي", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .imePadding()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = newRowTitle,
                                    onValueChange = { newRowTitle = it },
                                    label = { Text("بيان البند / العمل / الصنف") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = newRowCategory,
                                    onValueChange = { newRowCategory = it },
                                    label = { Text("التصنيف أو المرحلة") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = newRowValue1,
                                        onValueChange = { newRowValue1 = it },
                                        label = { Text("الكمية / الوزن") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = newRowValue2,
                                        onValueChange = { newRowValue2 = it },
                                        label = { Text("السعر / الفئة") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                OutlinedTextField(
                                    value = newRowValue3,
                                    onValueChange = { newRowValue3 = it },
                                    label = { Text("المصروفات أو التكلفة المباشرة") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = newRowNotes,
                                    onValueChange = { newRowNotes = it },
                                    label = { Text("ملاحظات وتوثيق البند") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )

                                // Media Attachment Simulator buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            newRowPhotoUri = "content://media/oozox/site_photo_${System.currentTimeMillis()}.jpg"
                                            Toast.makeText(context, "تم إرفاق صورة موقع الإنجاز", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (newRowPhotoUri != null) Color(0xFF10B981) else GoldPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (newRowPhotoUri != null) "تم إرفاق صورة ✓" else "إرفاق صورة 📷", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            newRowVideoUri = "content://media/oozox/site_video_${System.currentTimeMillis()}.mp4"
                                            Toast.makeText(context, "تم إرفاق فيديو الإنجاز الميداني", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Videocam,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (newRowVideoUri != null) Color(0xFF10B981) else GoldPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (newRowVideoUri != null) "تم إرفاق فيديو ✓" else "إرفاق فيديو 🎥", fontSize = 11.sp)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newRowTitle.isNotBlank()) {
                                        val q = newRowValue1.toDoubleOrNull() ?: 1.0
                                        val p = newRowValue2.toDoubleOrNull() ?: 0.0
                                        val c = newRowValue3.toDoubleOrNull() ?: 0.0

                                        val newRow = SmartSpreadsheetRow(
                                            id = "row_${System.currentTimeMillis()}",
                                            rowNumber = sheetRows.size + 1,
                                            title = newRowTitle.trim(),
                                            category = newRowCategory.trim(),
                                            values = mutableMapOf(
                                                "quantity" to q,
                                                "unit_rate" to p,
                                                "materials_cost" to c,
                                                "labor_cost" to 0.0,
                                                "total_sales" to (q * p),
                                                "total_cost" to c,
                                                "weight_grams" to q,
                                                "gold_price_per_gram" to p,
                                                "contract_value" to (q * p),
                                                "direct_expenses" to c
                                            ),
                                            notes = newRowNotes.ifBlank { null },
                                            photoUri = newRowPhotoUri,
                                            videoUri = newRowVideoUri
                                        )
                                        sheetRows = sheetRows + newRow
                                        saveSheetChanges()
                                        showAddRowDialog = false
                                        // Reset fields
                                        newRowTitle = ""
                                        newRowValue1 = ""
                                        newRowValue2 = ""
                                        newRowValue3 = ""
                                        newRowNotes = ""
                                        newRowPhotoUri = null
                                        newRowVideoUri = null
                                    } else {
                                        Toast.makeText(context, "يرجى كتابة بيان البند", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                            ) {
                                Text("إدراج في الشيت", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddRowDialog = false }) {
                                Text("إلغاء")
                            }
                        }
                    )
                }

                // Media Preview Dialog
                if (previewMediaUri != null) {
                    val isVideo = previewMediaUri!!.second
                    AlertDialog(
                        onDismissRequest = { previewMediaUri = null },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isVideo) Icons.Default.Videocam else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = GoldPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isVideo) "معاينة فيديو الموقع المرفق" else "معاينة صورة الموقع المرفقة", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        if (isVideo) Icons.Default.PlayCircleFilled else Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isVideo) "ملف فيديو عالي الدقة موثق للأعمال" else "صورة موقعية معتمدة للبند والمطابقة الميدانية",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = previewMediaUri!!.first,
                                        fontSize = 10.sp,
                                        color = GoldAccent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = { previewMediaUri = null }) {
                                Text("تم")
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Tab 1: Live Interactive Spreadsheet View with Activity Template Switcher
 */
@Composable
private fun LiveSpreadsheetTab(
    template: SmartSpreadsheetTemplate,
    rows: List<SmartSpreadsheetRow>,
    onSelectActivity: (ProjectActivityType) -> Unit,
    onAddRowClick: () -> Unit,
    onDeleteRow: (String) -> Unit,
    onPreviewMedia: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Activity Switcher Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "القوالب المحاسبية المعتمدة بحسب نوع النشاط:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ProjectActivityType.values()) { actType ->
                        val isSelected = actType == template.activityType
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectActivity(actType) },
                            label = {
                                Text(
                                    actType.arLabel,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    when (actType) {
                                        ProjectActivityType.CONSTRUCTION -> Icons.Default.Engineering
                                        ProjectActivityType.GOLD_JEWELRY -> Icons.Default.Diamond
                                        ProjectActivityType.RETAIL_COMMERCE -> Icons.Default.Storefront
                                        ProjectActivityType.MANUFACTURING -> Icons.Default.PrecisionManufacturing
                                        ProjectActivityType.SERVICES_AGENCY -> Icons.Default.BusinessCenter
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color.Black,
                                selectedLeadingIconColor = Color.Black
                            )
                        )
                    }
                }
            }
        }

        // Summary KPIs Deck
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مؤشرات الأداء المحاسبي للشيت", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                        }
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "معالجة فورية دقيقة ⚡",
                                fontSize = 10.5.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(template.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        template.summaryKpis.take(3).forEach { kpi ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(kpi, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("✓ معتمد", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Spreadsheet Table Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("جدول البيانات والمعادلات المحسوبة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("مرر أفقياً للاطلاع على كافة الأعمدة والصيغ", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = onAddRowClick,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة بند", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val horizontalScrollState = rememberScrollState()

                    // Table View
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        Column {
                            // Header Row
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                                Text("البيان / البند", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(160.dp))
                                val visibleCols = template.columns.filter { !it.isHiddenCalculation }
                                visibleCols.forEach { col ->
                                    Text(
                                        col.nameAr,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(110.dp),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text("الوسائط والملاحظات", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(130.dp), textAlign = TextAlign.Center)
                                Text("إجراء", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(45.dp), textAlign = TextAlign.Center)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Data Rows
                            rows.forEachIndexed { idx, row ->
                                Row(
                                    modifier = Modifier
                                        .padding(vertical = 6.dp, horizontal = 6.dp)
                                        .background(
                                            if (idx % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            RoundedCornerShape(6.dp)
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${idx + 1}", fontSize = 11.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                                    Column(modifier = Modifier.width(160.dp)) {
                                        Text(row.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(row.category, fontSize = 10.sp, color = GoldPrimary)
                                    }

                                    val visibleCols = template.columns.filter { !it.isHiddenCalculation }
                                    visibleCols.forEach { col ->
                                        val rawVal = when (col.id) {
                                            "item_name" -> row.title
                                            "total_cost" -> {
                                                val mat = (row.values["materials_cost"] as? Double) ?: 0.0
                                                val lab = (row.values["labor_cost"] as? Double) ?: 0.0
                                                mat + lab
                                            }
                                            "contract_value" -> {
                                                val qty = (row.values["quantity"] as? Double) ?: 0.0
                                                val rate = (row.values["unit_rate"] as? Double) ?: 0.0
                                                qty * rate
                                            }
                                            "net_profit" -> {
                                                val rev = (row.values["contract_value"] as? Double) ?: ((row.values["quantity"] as? Double ?: 0.0) * (row.values["unit_rate"] as? Double ?: 0.0))
                                                val exp = ((row.values["materials_cost"] as? Double) ?: 0.0) + ((row.values["labor_cost"] as? Double) ?: 0.0)
                                                rev - exp
                                            }
                                            else -> row.values[col.id] ?: row.calculatedResults[col.id] ?: ""
                                        }

                                        val displayText = when (rawVal) {
                                            is Double -> String.format(Locale.US, "%.1f", rawVal)
                                            else -> rawVal.toString()
                                        }

                                        Text(
                                            displayText,
                                            fontSize = 11.sp,
                                            modifier = Modifier.width(110.dp),
                                            textAlign = TextAlign.Center,
                                            color = if (col.type == SpreadsheetColumnType.FORMULA) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Media attachments column
                                    Row(
                                        modifier = Modifier.width(130.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (row.photoUri != null) {
                                            IconButton(
                                                onClick = { onPreviewMedia(row.photoUri, false) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Image, contentDescription = "صورة", tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        if (row.videoUri != null) {
                                            IconButton(
                                                onClick = { onPreviewMedia(row.videoUri, true) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Videocam, contentDescription = "فيديو", tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        if (row.notes != null) {
                                            Text(row.notes, fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    // Delete Action
                                    IconButton(
                                        onClick = { onDeleteRow(row.id) },
                                        modifier = Modifier.width(45.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف البند", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Smart NLP Natural Language Formulas & Background Calculation Engine
 */
@Composable
private fun NaturalLanguageFormulasTab(
    template: SmartSpreadsheetTemplate,
    rows: List<SmartSpreadsheetRow>,
    prompt: String,
    onPromptChange: (String) -> Unit,
    isProcessing: Boolean,
    explanation: String?,
    onExecuteFormula: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Concept Hero Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = GoldPrimary.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("الحسابات والمعادلات المخصصة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("صياغة المعادلات المالية والعمليات الحسابية", fontSize = 11.sp, color = GoldPrimary)
                        }
                    }

                    Text(
                        "اكتب العملية الحسابية المطلوبة (مثلاً: حساب الضرائب، الحوافز، أو مخصصات المخاطر) وسيتم معالجة البيانات وتحديث الجدول مباشرة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Input & Preset Prompts
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("صياغة العملية الحسابية:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = onPromptChange,
                        label = { Text("مثال: أريد حساب كذا ليحدث كذا...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )

                    // Preset quick prompts
                    Text("أمثلة عمليات حسابية جاهزة:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val presets = listOf(
                        "أريد حساب صافي الربح بعد خصم 14% ضريبة و 5% هالك وإضافة 500 حافز",
                        "احسب التكلفة الإجمالية بعد خصم 10% مخصص مخاطر وهبوط أسعار",
                        "أريد حساب صافي المستحق بعد استقطاع 15% تأمين وضمان أعمال",
                        "احسب إجمالي سعر البيع مع إضافة 14% ضريبة قيمة مضافة وهامش 20%"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.forEach { preset ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPromptChange(preset) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(preset, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onExecuteFormula,
                        enabled = prompt.isNotBlank() && !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("apply_nlp_formula_btn")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ المعالجة في الخلفية...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Functions, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تطبيق الخانة الحسابية على الشيت ⚡", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Backend Processing Explanation Result Card
        if (explanation != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تمت المعالجة في الخلفية بنجاح (Backend Engine)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }

                        Text("المعادلة الرياضية التي تم توليدها وتطبيقها تلقائياً:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = explanation,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "تم تحديث كافة صفوف الشيت (${rows.size} بند) بحسابات فورية دون أي خطأ في التقريب.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 3: Proactive Virtual Accountant Advisor & Real-time Audit Deck
 */
@Composable
private fun VirtualAccountantAdvisorTab(
    report: VirtualAccountantAdviceReport,
    template: SmartSpreadsheetTemplate,
    project: ProjectEntity,
    chatMessages: List<Pair<String, String>>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Health Score & Status Hero
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("مؤشر السلامة والأمان المالي", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("فحص استباقي لحظي لحسابات وتدفقات المشروع", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Surface(
                            color = if (report.healthScore >= 75) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                report.healthStatusLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (report.healthScore >= 75) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Score Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { report.healthScore / 100f },
                            modifier = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = if (report.healthScore >= 75) Color(0xFF10B981) else Color(0xFFF59E0B),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            "${report.healthScore}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }

                    // Key Financial Metrics Grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricSmallBox("إجمالي الإيراد", GoldUtils.formatCurrency(report.totalRevenue), Color(0xFF10B981), Modifier.weight(1f))
                        MetricSmallBox("المصروفات", GoldUtils.formatCurrency(report.totalExpenses), Color(0xFFEF4444), Modifier.weight(1f))
                        MetricSmallBox("صافي التدفق", GoldUtils.formatCurrency(report.netCashflow), if (report.netCashflow >= 0) Color(0xFF10B981) else Color(0xFFEF4444), Modifier.weight(1f))
                        MetricSmallBox("هامش الربح", "${report.profitMarginPercent.toInt()}%", GoldPrimary, Modifier.weight(1f))
                    }
                }
            }
        }

        // Active Alerts & Warnings
        if (report.alerts.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تنبيهات المحاسب العاجلة ومؤشرات العجز:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }

                        report.alerts.forEach { alert ->
                            Text("• $alert", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 17.sp)
                        }
                    }
                }
            }
        }

        // Proactive Tips & Recommendations
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نصائح المحاسب وتوجيهات النشاط:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    (report.proactiveTips + report.accountingRecommendations).forEach { tip ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tip, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(10.dp), lineHeight = 17.sp)
                        }
                    }
                }
            }
        }

        // Interactive Chat with Virtual Accountant
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استشر المحاسب المالي المرافق:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                    }

                    // Chat messages list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chatMessages.takeLast(4).forEach { (sender, text) ->
                            val isMe = sender == "أنت"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (isMe) GoldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(sender, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isMe) Color.Black else GoldPrimary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text, fontSize = 11.5.sp, color = if (isMe) Color.Black else MaterialTheme.colorScheme.onSurface, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Input Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            placeholder = { Text("اسأل المحاسب: كم صافي الربح؟ هل هناك عجز؟", fontSize = 11.5.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        IconButton(
                            onClick = onSendMessage,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "إرسال")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricSmallBox(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor, maxLines = 1)
        }
    }
}
