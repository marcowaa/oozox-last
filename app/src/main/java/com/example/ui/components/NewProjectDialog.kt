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
import com.example.data.model.ContactEntity
import com.example.data.model.ProjectMemberInput
import com.example.data.model.ProjectType
import com.example.ui.theme.*
import com.example.util.ProjectIndustryCatalog
import com.example.util.tr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectDialog(
    contacts: List<ContactEntity> = emptyList(),
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
        customProjectTypeName: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
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
    var selectedWorkMode by remember { mutableStateOf("PHYSICAL_OPERATIONS") } // "FINANCIAL_ONLY" or "PHYSICAL_OPERATIONS"
    var selectedLocation by remember { mutableStateOf("القاهرة - التجمع الخامس (الموقع الميداني)") }
    var customLocationText by remember { mutableStateOf("") }

    // Co-Manager state
    var coManagerName by remember { mutableStateOf("") }
    var coManagerPhone by remember { mutableStateOf("") }
    var showCoManagerPicker by remember { mutableStateOf(false) }

    // Participant Mentions & Roles list
    var memberNameInput by remember { mutableStateOf("") }
    var memberPhoneInput by remember { mutableStateOf("") }
    var memberRoleTaskInput by remember { mutableStateOf("") }
    val membersList = remember { mutableStateListOf<ProjectMemberInput>() }
    var showMemberContactPicker by remember { mutableStateOf(false) }

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

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
                                    value = groupName,
                                    onValueChange = { groupName = it },
                                    label = { Text("اسم مجموعة العمل (مثال: تحالف المقاولات)", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = projectName,
                                    onValueChange = { projectName = it },
                                    label = { Text("اسم المشروع الاستثماري", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

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

                    // 4. Participants Mentions & Role Tasks
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
                                    Text("٤. أعضاء الفريق والمهام", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GoldPrimary)
                                    TextButton(
                                        onClick = { showMemberContactPicker = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("اختيار جهة اتصال", fontSize = 11.sp, color = GoldPrimary)
                                    }
                                }

                                OutlinedTextField(
                                    value = memberNameInput,
                                    onValueChange = { memberNameInput = it },
                                    label = { Text("اسم العضو المشارك", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = memberPhoneInput,
                                    onValueChange = { memberPhoneInput = it },
                                    label = { Text("رقم الهاتف للدعوة", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = memberRoleTaskInput,
                                    onValueChange = { memberRoleTaskInput = it },
                                    label = { Text("الدور / الوظيفة المسندة", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedButton(
                                    onClick = {
                                        if (memberNameInput.isNotBlank() && memberPhoneInput.isNotBlank()) {
                                            membersList.add(
                                                ProjectMemberInput(
                                                    name = memberNameInput.trim(),
                                                    phone = memberPhoneInput.trim(),
                                                    roleTask = if (memberRoleTaskInput.isBlank()) "عضو مشارك" else memberRoleTaskInput.trim()
                                                )
                                            )
                                            memberNameInput = ""
                                            memberPhoneInput = ""
                                            memberRoleTaskInput = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إضافة العضو إلى القائمة", fontSize = 12.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                                }

                                if (membersList.isNotEmpty()) {
                                    Text("المشاركون المضافون (${membersList.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    membersList.forEachIndexed { index, mem ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(mem.name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    Text("${mem.phone} • ${mem.roleTask}", fontSize = 11.sp, color = GoldPrimary)
                                                }
                                                IconButton(
                                                    onClick = { membersList.removeAt(index) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val cap = capitalStr.toDoubleOrNull() ?: 0.0
                            val finalLoc = if (selectedLocation == "موقع مخصص آخر...") customLocationText.ifBlank { "القاهرة، مصر" } else selectedLocation

                            val membersInfoText = membersList.joinToString(" | ") { mem ->
                                "@${mem.name} (${mem.phone}) - ${mem.roleTask}"
                            }

                            if (groupName.isNotBlank() && projectName.isNotBlank()) {
                                onCreateProject(
                                    groupName.trim(),
                                    null,
                                    projectName.trim(),
                                    cap,
                                    selectedType,
                                    finalLoc,
                                    coManagerPhone.ifBlank { null },
                                    coManagerName.ifBlank { null },
                                    membersInfoText,
                                    selectedTemplateName
                                )
                                onDismiss()
                            }
                        },
                        enabled = groupName.isNotBlank() && projectName.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.weight(1.8f)
                    ) {
                        Text("إنشاء المشروع", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                        IconButton(onClick = { showCoManagerPicker = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(16.dp))
                        }
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
                        IconButton(onClick = { showMemberContactPicker = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(16.dp))
                        }
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
