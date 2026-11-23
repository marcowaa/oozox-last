package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.ContactEntity
import com.example.data.model.HiddenSubAccountEntity
import com.example.data.model.ProjectMemberInput
import com.example.data.model.ProjectMemberRole
import com.example.data.model.ProjectType
import com.example.ui.theme.*
import com.example.util.LocalAppLanguage
import com.example.util.ProjectIndustryCatalog
import com.example.util.tr

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewProjectDialog(
    contacts: List<ContactEntity> = emptyList(),
    employees: List<HiddenSubAccountEntity> = emptyList(),
    onCreateProject: (
        groupName: String,
        groupDesc: String?,
        projectName: String,
        capital: Double,
        projectType: ProjectType,
        location: String,
        coManagerPhone: String?,
        coManagerName: String?,
        membersInfo: String,
        customProjectTypeName: String,
        memberInputs: List<ProjectMemberInput>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val currentLanguage = LocalAppLanguage.current
    var groupName by remember { mutableStateOf("") }
    var hasCustomGroupName by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("") }
    var capitalStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ProjectType.CONSTRUCTION) }
    var catalogSearchQuery by remember { mutableStateOf("") }
    var selectedSectorId by remember { mutableStateOf<String?>(ProjectIndustryCatalog.sectors.firstOrNull()?.id) }
    var selectedTemplateName by remember {
        mutableStateOf(
            ProjectIndustryCatalog.sectors.firstOrNull()?.projectTemplates?.firstOrNull()
                ?: ProjectType.CONSTRUCTION.displayLabel()
        )
    }
    var selectedWorkMode by remember { mutableStateOf("PHYSICAL_OPERATIONS") }
    var selectedLocation by remember { mutableStateOf("القاهرة - التجمع الخامس (الموقع الميداني)") }
    var customLocationText by remember { mutableStateOf("") }

    // Co-Manager state
    var coManagerName by remember { mutableStateOf("") }
    var coManagerPhone by remember { mutableStateOf("") }
    var showCoManagerPicker by remember { mutableStateOf(false) }

    // Employee / Member addition state with job role & granular permissions
    var memberUserIdInput by remember { mutableStateOf<String?>(null) }
    var memberNameInput by remember { mutableStateOf("") }
    var memberPhoneInput by remember { mutableStateOf("") }
    var memberRoleTaskInput by remember { mutableStateOf("") }
    var selectedMemberRole by remember { mutableStateOf(ProjectMemberRole.EXECUTIVE_MANAGER) }
    var permGroupAdmin by remember { mutableStateOf(ProjectMemberRole.EXECUTIVE_MANAGER.defaultIsAdmin()) }
    var permPostAccounting by remember { mutableStateOf(ProjectMemberRole.EXECUTIVE_MANAGER.defaultCanPostAccounting()) }
    var permEditRecords by remember { mutableStateOf(ProjectMemberRole.EXECUTIVE_MANAGER.defaultCanEditRecords()) }
    var permExportReports by remember { mutableStateOf(ProjectMemberRole.EXECUTIVE_MANAGER.defaultCanExportReports()) }
    var permDirectChat by remember { mutableStateOf(true) }

    val membersList = remember { mutableStateListOf<ProjectMemberInput>() }
    var showMemberContactPicker by remember { mutableStateOf(false) }
    var showEmployeePicker by remember { mutableStateOf(false) }

    val presetLocations = listOf(
        "القاهرة - التجمع الخامس (الموقع الميداني)",
        "الجيزة - الشيخ زايد (الفرع الرئيسي)",
        "الإسكندرية - سموحة (مركز التوزيع)",
        "دبي - سوق الذهب (المركز اللوجستي)",
        "الرياض - حي العليا (المكتب الإقليمي)",
        "موقع مخصص آخر..."
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BusinessCenter,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "مشروع جديد",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "البيانات الأساسية وفريق العمل",
                                fontSize = 11.sp,
                                color = GoldPrimary
                            )
                        }
                    }

                    OozoxCircleIconButton(
                        onClick = onDismiss,
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 0.8.dp
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Group & Project Name
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "١. البيانات الأساسية للمشروع",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GoldPrimary
                                )

                                OutlinedTextField(
                                    value = projectName,
                                    onValueChange = {
                                        projectName = it
                                        if (!hasCustomGroupName) {
                                            groupName = it
                                        }
                                    },
                                    label = { Text("اسم المشروع الاستثماري", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("new_project_name_input")
                                )

                                OutlinedTextField(
                                    value = groupName,
                                    onValueChange = {
                                        groupName = it
                                        hasCustomGroupName = it.isNotBlank() && it != projectName
                                    },
                                    label = { Text(tr("proj_group.group_name_auto_label", "اسم مجموعة الدردشة (يتطابق تلقائياً مع اسم المشروع)"), fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("new_project_group_name_input")
                                )

                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.7.dp, GoldPrimary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Forum, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
                                        Text(
                                            text = tr("proj_group.auto_chat_badge", "سيتم تلقائياً إنشاء مجموعة دردشة باسم المشروع وإضافة كافة الموظفين بصلاحياتهم"),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = GoldPrimary
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = capitalStr,
                                    onValueChange = { capitalStr = it },
                                    label = { Text("رأس المال التقديري", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = "نمط وطبيعة إدارة العمل:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = { selectedWorkMode = "FINANCIAL_ONLY" },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selectedWorkMode == "FINANCIAL_ONLY") GoldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (selectedWorkMode == "FINANCIAL_ONLY") GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                                Text("تعاملات مالية فقط", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text("إدارة حسابات، فواتير، شفتات، كاشير، ومطابقات نقدية.", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Surface(
                                        onClick = { selectedWorkMode = "PHYSICAL_OPERATIONS" },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selectedWorkMode == "PHYSICAL_OPERATIONS") GoldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (selectedWorkMode == "PHYSICAL_OPERATIONS") GoldPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.Engineering, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                                Text("عمليات فيزيائية / بناء", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text("مهام ميدانية بالصور والفيديو، تكليف بمدد وتنبيهات متصاعدة.", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                val filteredCatalogTemplates = remember(catalogSearchQuery, selectedSectorId) {
                                    val query = catalogSearchQuery.trim()
                                    if (query.isNotBlank()) {
                                        ProjectIndustryCatalog.allTemplates.filter { (template, sector) ->
                                            template.contains(query, ignoreCase = true) ||
                                                sector.nameAr.contains(query, ignoreCase = true)
                                        }.take(24)
                                    } else {
                                        val activeSector = ProjectIndustryCatalog.sectors.find { it.id == selectedSectorId }
                                            ?: ProjectIndustryCatalog.sectors.first()
                                        activeSector.projectTemplates.map { it to activeSector }
                                    }
                                }
                                val activeProfile = remember(selectedTemplateName) {
                                    ProjectIndustryCatalog.resolveAccountingProfile(selectedTemplateName)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "نوع المشروع (${ProjectIndustryCatalog.totalTemplatesCount}+ نشاط):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                    Surface(
                                        color = GoldPrimary.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = selectedTemplateName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = catalogSearchQuery,
                                    onValueChange = { catalogSearchQuery = it },
                                    label = { Text("ابحث في أكثر من 320 مشروع (مطعم، صيدلية، مصنع، شحن...)", fontSize = 11.5.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                    },
                                    trailingIcon = {
                                        if (catalogSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { catalogSearchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "مسح البحث", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("project_catalog_search")
                                )

                                // Sector filter chips
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ProjectIndustryCatalog.sectors.forEach { sector ->
                                        val isSecSelected = selectedSectorId == sector.id && catalogSearchQuery.isBlank()
                                        FilterChip(
                                            selected = isSecSelected,
                                            onClick = {
                                                selectedSectorId = sector.id
                                                catalogSearchQuery = ""
                                            },
                                            label = { Text(sector.nameAr, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GoldPrimary,
                                                selectedLabelColor = MaterialTheme.colorScheme.background
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                // Matching templates horizontal picker
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    filteredCatalogTemplates.forEach { (templateName, sector) ->
                                        val isTmplSelected = selectedTemplateName == templateName
                                        FilterChip(
                                            selected = isTmplSelected,
                                            onClick = {
                                                selectedTemplateName = templateName
                                                selectedSectorId = sector.id
                                                selectedType = sector.accountingProfile.baseProjectType
                                                if (projectName.isBlank()) {
                                                    projectName = templateName
                                                    if (!hasCustomGroupName) {
                                                        groupName = templateName
                                                    }
                                                }
                                            },
                                            label = { Text(templateName, fontSize = 11.sp) },
                                            leadingIcon = if (isTmplSelected) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                            } else null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF10B981),
                                                selectedLabelColor = Color.White,
                                                selectedLeadingIconColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                // Tailored Accounting Profile Preview
                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
                                            Text(
                                                text = "الحسابات المالية: ${activeProfile.sectorNameAr}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldPrimary
                                            )
                                        }
                                        Text(
                                            text = "الإيرادات: ${activeProfile.revenueAccounts.take(3).joinToString(" • ")}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "المصروفات: ${activeProfile.expenseAccounts.take(3).joinToString(" • ")}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (activeProfile.tracksPhysicalGoods) "📦 يشمل جرد البضائع والعهدة في تسليم الشفتات" else "💼 محاسبة خدمات وعقود بدون مخزون سلعي",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (activeProfile.tracksPhysicalGoods) Color(0xFF10B981) else GoldAccent
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Map & Location Selection
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("٢. الموقع الجغرافي للمشروع", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                }

                                presetLocations.forEach { loc ->
                                    val isLocSelected = selectedLocation == loc
                                    Surface(
                                        onClick = { selectedLocation = loc },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isLocSelected) GoldPrimary.copy(alpha = 0.1f) else Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            RadioButton(
                                                selected = isLocSelected,
                                                onClick = { selectedLocation = loc },
                                                colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(loc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }

                                if (selectedLocation == "موقع مخصص آخر...") {
                                    OutlinedTextField(
                                        value = customLocationText,
                                        onValueChange = { customLocationText = it },
                                        label = { Text("أدخل العنوان التفصيلي", fontSize = 12.sp) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // 3. Co-Manager / Partner Assignment
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("٣. تعيين الشريك الإداري", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GoldPrimary)
                                    TextButton(
                                        onClick = { showCoManagerPicker = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("من جهات الاتصال", fontSize = 11.sp, color = GoldPrimary)
                                    }
                                }

                                OutlinedTextField(
                                    value = coManagerName,
                                    onValueChange = { coManagerName = it },
                                    label = { Text("اسم الشريك المسؤول", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = coManagerPhone,
                                    onValueChange = { coManagerPhone = it },
                                    label = { Text("رقم هاتف الشريك", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // 4. Employees, Job Titles & Granular Permissions
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = tr("proj_group.team_roles_section", "٤. الموظفون والوظائف والصلاحيات"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = GoldPrimary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showEmployeePicker = true },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.55f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("pick_company_employee_btn")
                                    ) {
                                        Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(15.dp), tint = GoldPrimary)
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "${tr("proj_group.pick_from_employees", "من موظفي الشركة")} (${employees.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { showMemberContactPicker = true },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = tr("proj_group.pick_from_contacts", "من جهات الاتصال"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = memberNameInput,
                                    onValueChange = { memberNameInput = it },
                                    label = { Text("اسم الموظف / العضو", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = memberPhoneInput,
                                    onValueChange = { memberPhoneInput = it },
                                    label = { Text("رقم الهاتف", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = memberRoleTaskInput,
                                    onValueChange = { memberRoleTaskInput = it },
                                    label = { Text(tr("proj_group.job_title_label", "الوظيفة المسندة في المشروع"), fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Text(
                                    text = tr("proj_group.project_role_label", "الدور الإداري في المشروع:"),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ProjectMemberRole.entries.filter { it != ProjectMemberRole.OWNER }.forEach { roleOption ->
                                        val isSelected = selectedMemberRole == roleOption
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedMemberRole = roleOption
                                                permGroupAdmin = roleOption.defaultIsAdmin()
                                                permPostAccounting = roleOption.defaultCanPostAccounting()
                                                permEditRecords = roleOption.defaultCanEditRecords()
                                                permExportReports = roleOption.defaultCanExportReports()
                                                permDirectChat = true
                                                if (memberRoleTaskInput.isBlank()) {
                                                    memberRoleTaskInput = roleOption.localizedLabel(currentLanguage)
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = roleOption.localizedLabel(currentLanguage),
                                                    fontSize = 11.sp,
                                                    maxLines = 1
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GoldPrimary,
                                                selectedLabelColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = tr("proj_group.permissions_title", "صلاحيات الموظف داخل المشروع والمجموعة:"),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(
                                        Triple(tr("proj_group.perm_admin", "مدير مجموعة"), permGroupAdmin) { v: Boolean -> permGroupAdmin = v },
                                        Triple(tr("proj_group.perm_accounting", "قيود مالية"), permPostAccounting) { v: Boolean -> permPostAccounting = v },
                                        Triple(tr("proj_group.perm_edit", "تعديل السجلات"), permEditRecords) { v: Boolean -> permEditRecords = v },
                                        Triple(tr("proj_group.perm_export", "تصدير تقارير"), permExportReports) { v: Boolean -> permExportReports = v },
                                        Triple(tr("proj_group.perm_direct_chat", "تراسل مباشر"), permDirectChat) { v: Boolean -> permDirectChat = v }
                                    ).forEach { (permLabel, checked, onToggle) ->
                                        FilterChip(
                                            selected = checked,
                                            onClick = { onToggle(!checked) },
                                            leadingIcon = if (checked) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                            } else null,
                                            label = { Text(permLabel, fontSize = 10.5.sp, maxLines = 1) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GoldPrimary.copy(alpha = 0.16f),
                                                selectedLabelColor = GoldPrimary,
                                                selectedLeadingIconColor = GoldPrimary
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (memberNameInput.isNotBlank()) {
                                            membersList.add(
                                                ProjectMemberInput(
                                                    name = memberNameInput.trim(),
                                                    phone = memberPhoneInput.trim(),
                                                    roleTask = memberRoleTaskInput.trim().ifBlank { selectedMemberRole.localizedLabel(currentLanguage) },
                                                    isCoManager = permGroupAdmin || selectedMemberRole == ProjectMemberRole.EXECUTIVE_MANAGER,
                                                    userId = memberUserIdInput,
                                                    role = selectedMemberRole,
                                                    isGroupAdmin = permGroupAdmin,
                                                    canPostAccounting = permPostAccounting,
                                                    canEditRecords = permEditRecords,
                                                    canExportReports = permExportReports,
                                                    canMessageDirectly = permDirectChat
                                                )
                                            )
                                            memberUserIdInput = null
                                            memberNameInput = ""
                                            memberPhoneInput = ""
                                            memberRoleTaskInput = ""
                                        }
                                    },
                                    enabled = memberNameInput.isNotBlank(),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("add_project_member_to_draft_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إضافة الموظف مع الصلاحيات للقائمة", fontSize = 12.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                                }

                                if (membersList.isNotEmpty()) {
                                    Text("فريق العمل والموظفون (${membersList.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    membersList.forEachIndexed { index, mem ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.35f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(mem.name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                                        Surface(
                                                            color = GoldPrimary.copy(alpha = 0.14f),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = mem.role.localizedLabel(currentLanguage),
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = GoldPrimary,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = listOfNotNull(
                                                            mem.phone.takeIf { it.isNotBlank() },
                                                            mem.roleTask
                                                        ).joinToString(" • "),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    val permBadges = buildList {
                                                        if (mem.isGroupAdmin) add(tr("proj_group.perm_admin", "مدير مجموعة"))
                                                        if (mem.canPostAccounting) add(tr("proj_group.perm_accounting", "قيود مالية"))
                                                        if (mem.canEditRecords) add(tr("proj_group.perm_edit", "تعديل"))
                                                        if (mem.canExportReports) add(tr("proj_group.perm_export", "تقارير"))
                                                    }
                                                    if (permBadges.isNotEmpty()) {
                                                        Text(
                                                            text = permBadges.joinToString(" • "),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = GoldPrimary
                                                        )
                                                    }
                                                }
                                                OozoxCircleIconButton(
                                                    onClick = { membersList.removeAt(index) },
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "حذف",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 0.8.dp
                )

                // Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("إلغاء", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val cap = capitalStr.toDoubleOrNull() ?: 0.0
                            val finalLoc = if (selectedLocation == "موقع مخصص آخر...") customLocationText.ifBlank { "القاهرة، مصر" } else selectedLocation
                            val finalGroupName = groupName.trim().ifBlank { projectName.trim() }

                            val membersInfoText = membersList.joinToString(" | ") { mem ->
                                val phonePart = mem.phone.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                                "@${mem.name}$phonePart - ${mem.roleTask}"
                            }

                            if (projectName.isNotBlank()) {
                                onCreateProject(
                                    finalGroupName,
                                    null,
                                    projectName.trim(),
                                    cap,
                                    selectedType,
                                    finalLoc,
                                    coManagerPhone.ifBlank { null },
                                    coManagerName.ifBlank { null },
                                    membersInfoText,
                                    selectedTemplateName,
                                    membersList.toList()
                                )
                                onDismiss()
                            }
                        },
                        enabled = projectName.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.8f)
                            .heightIn(min = 48.dp)
                            .testTag("submit_new_project_btn")
                    ) {
                        Text("إنشاء المشروع والمجموعة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Company Employees Picker Modal
    if (showEmployeePicker) {
        Dialog(
            onDismissRequest = { showEmployeePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tr("proj_group.select_employee_title", "اختيار موظف من الشركة"), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        OozoxCircleIconButton(
                            onClick = { showEmployeePicker = false },
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق"
                        )
                    }

                    val activeEmployees = remember(employees) { employees.filter { it.isActiveEmployee } }
                    if (activeEmployees.isEmpty()) {
                        Text(tr("proj_group.no_employees_found", "لا يوجد موظفون مسجلون في إدارة الأعمال بعد"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(activeEmployees, key = { it.id }) { emp ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            memberUserIdInput = emp.id
                                            memberNameInput = emp.displayName
                                            memberPhoneInput = emp.phone.orEmpty()
                                            memberRoleTaskInput = emp.roleTitle.ifBlank { emp.department }
                                            val autoRole = when {
                                                emp.permManageProjects || emp.permManageBusiness || emp.permManageStaff -> ProjectMemberRole.EXECUTIVE_MANAGER
                                                emp.canViewReports -> ProjectMemberRole.FINANCIAL_PARTNER
                                                else -> ProjectMemberRole.PROCUREMENT_OFFICER
                                            }
                                            selectedMemberRole = autoRole
                                            permGroupAdmin = emp.permManageBusiness || emp.permManageStaff || emp.permManageProjects
                                            permPostAccounting = emp.permManageAccounting || emp.canCreateInvoices
                                            permEditRecords = emp.permManageProjects || emp.permAssignTasks
                                            permExportReports = emp.canViewReports
                                            permDirectChat = emp.permDirectChat
                                            showEmployeePicker = false
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Badge, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(emp.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text("${emp.roleTitle} • ${emp.department}", fontSize = 11.sp, color = GoldPrimary)
                                            if (!emp.phone.isNullOrBlank()) {
                                                Text(emp.phone, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    }

    // Co-Manager Contact Picker Modal
    if (showCoManagerPicker) {
        Dialog(
            onDismissRequest = { showCoManagerPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("اختيار الشريك الإداري", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        OozoxCircleIconButton(
                            onClick = { showCoManagerPicker = false },
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق"
                        )
                    }

                    if (contacts.isEmpty()) {
                        Text("لا توجد جهات اتصال مسجلة، يمكنك إدخال الرقم يدوياً.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(contacts) { contact ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coManagerName = contact.name
                                            coManagerPhone = contact.phone
                                            showCoManagerPicker = false
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(contact.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    // Member Contact Picker Modal
    if (showMemberContactPicker) {
        Dialog(
            onDismissRequest = { showMemberContactPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("اختيار عضو مشارك", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        OozoxCircleIconButton(
                            onClick = { showMemberContactPicker = false },
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق"
                        )
                    }

                    if (contacts.isEmpty()) {
                        Text("لا توجد جهات اتصال مسجلة، يمكنك إدخال الرقم يدوياً.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(contacts) { contact ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            memberUserIdInput = contact.id
                                            memberNameInput = contact.name
                                            memberPhoneInput = contact.phone
                                            showMemberContactPicker = false
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(contact.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}
