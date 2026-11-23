package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.R
import com.example.data.model.ProjectEntity
import com.example.data.model.WorkGroupEntity
import com.example.ui.components.DeleteProjectConfirmationDialog
import com.example.ui.components.ExecutiveProjectsEmptyState
import com.example.ui.components.OozoxCircleIconButton
import com.example.ui.components.ProjectAnalyticsDashboard
import com.example.ui.components.ProjectEmptyStateType
import com.example.ui.components.ProjectHeaderKpiSkeleton
import com.example.ui.components.ProjectListSkeleton
import com.example.ui.components.ProjectsSideDrawerContent
import com.example.ui.components.shimmerEffect
import com.example.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.GoldUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    workGroups: List<WorkGroupEntity>,
    projects: List<ProjectEntity>,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onProjectClick: (String) -> Unit,
    onOpenProjectChat: ((ProjectEntity) -> Unit)? = null,
    onCreateProjectClick: () -> Unit,
    onDeleteProject: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var projectPendingDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    val haptic = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) { com.example.util.BiometricHelper.findFragmentActivity(context) }

    var isBiometricLocked by remember { mutableStateOf(false) }
    var biometricFeedback by remember { mutableStateOf<String?>(null) }

    val lastSyncTime by com.example.util.OfflineSyncManager.lastSyncTime.collectAsStateWithLifecycle()
    val isNetworkOnline = remember(context) { com.example.util.OfflineSyncManager.isNetworkAvailable(context) }
    val show24hBackupPrompt by com.example.util.OfflineSyncManager.show24hBackupPrompt.collectAsStateWithLifecycle()
    val isBackingUp by com.example.util.OfflineSyncManager.isBackingUp.collectAsStateWithLifecycle()

    val triggerBiometricUnlock: () -> Unit = {
        if (activity != null) {
            val status = com.example.util.BiometricHelper.checkBiometricStatus(activity)
            if (status == com.example.util.BiometricStatus.AVAILABLE) {
                com.example.util.BiometricHelper.authenticate(
                    activity = activity,
                    title = "المصادقة البيومترية لتأمين بيانات المشاريع",
                    subtitle = "بصمة الإصبع أو التعرف على الوجه",
                    description = "يرجى تأكيد الهوية للوصول للميزانيات ورؤوس الأموال الحساسة",
                    negativeButtonText = "إلغاء",
                    onSuccess = {
                        isBiometricLocked = false
                        biometricFeedback = "تم تأكيد الهوية بنجاح"
                    },
                    onError = { _, err ->
                        biometricFeedback = err.toString()
                    },
                    onFailed = {
                        biometricFeedback = "فشلت البصمة، يرجى إعادة المحاولة"
                    }
                )
            } else {
                isBiometricLocked = false
            }
        } else {
            isBiometricLocked = false
        }
    }

    val totalCapital = remember(projects) { projects.sumOf { it.estimatedCapital } }
    val totalExpenses = remember(projects) { projects.sumOf { it.totalExpenses } }
    val overallBurnRate = remember(totalCapital, totalExpenses) {
        if (totalCapital > 0) ((totalExpenses / totalCapital) * 100).toInt().coerceIn(0, 100) else 0
    }

    val availableTypes = remember(projects) {
        projects.map { it.type }.filter { it.isNotBlank() }.distinct()
    }

    val availableStatuses = listOf("الكل", "نشط", "تحت الميزانية", "تجاوز الميزانية", "قيد التأسيس")

    val filteredProjects = remember(projects, searchQuery, selectedTypeFilter, selectedStatusFilter) {
        projects.filter { proj ->
            val progress = if (proj.estimatedCapital > 0) (proj.totalExpenses / proj.estimatedCapital) * 100 else 0.0
            val statusLabel = when {
                progress > 100 -> "تجاوز الميزانية"
                progress in 80.0..100.0 -> "استهلاك مرتفع"
                progress in 1.0..79.9 -> "تحت الميزانية"
                proj.totalExpenses == 0.0 && proj.estimatedCapital > 0 -> "قيد التأسيس"
                else -> "نشط"
            }

            val query = searchQuery.trim()
            val matchesSearch = query.isBlank() ||
                    proj.name.contains(query, ignoreCase = true) ||
                    statusLabel.contains(query, ignoreCase = true) ||
                    proj.location.contains(query, ignoreCase = true) ||
                    proj.type.contains(query, ignoreCase = true) ||
                    (query == "نشط" && (statusLabel == "نشط" || statusLabel == "تحت الميزانية")) ||
                    (query == "عجز" && statusLabel.contains("تجاوز")) ||
                    (query.contains("تجاوز", ignoreCase = true) && statusLabel.contains("تجاوز")) ||
                    (query.contains("ميزانية", ignoreCase = true) && statusLabel.contains("الميزانية")) ||
                    (proj.coManagerName?.contains(query, ignoreCase = true) == true)

            val matchesType = selectedTypeFilter == null || proj.type == selectedTypeFilter
            val matchesStatus = selectedStatusFilter == null || selectedStatusFilter == "الكل" || when (selectedStatusFilter) {
                "نشط" -> statusLabel == "نشط" || statusLabel == "تحت الميزانية"
                "تحت الميزانية" -> statusLabel == "تحت الميزانية"
                "تجاوز الميزانية" -> statusLabel.contains("تجاوز")
                "قيد التأسيس" -> statusLabel.contains("التأسيس")
                else -> statusLabel.contains(selectedStatusFilter ?: "")
            }

            matchesSearch && matchesType && matchesStatus
        }
    }

    var showAnalytics by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ProjectsSideDrawerContent(
                projects = projects,
                selectedStatusFilter = selectedStatusFilter,
                selectedTypeFilter = selectedTypeFilter,
                availableTypes = availableTypes,
                availableStatuses = availableStatuses,
                onSelectStatusFilter = { selectedStatusFilter = it },
                onSelectTypeFilter = { selectedTypeFilter = it },
                showAnalytics = showAnalytics,
                onToggleAnalytics = { showAnalytics = !showAnalytics },
                isBiometricLocked = isBiometricLocked,
                onToggleBiometricLock = {
                    if (isBiometricLocked) {
                        triggerBiometricUnlock()
                    } else {
                        isBiometricLocked = true
                    }
                },
                isNetworkOnline = isNetworkOnline,
                onTriggerSync = {
                    com.example.util.OfflineSyncManager.triggerImmediateSync(context)
                },
                show24hBackupPrompt = show24hBackupPrompt,
                isBackingUp = isBackingUp,
                onPerformBackup = {
                    com.example.util.OfflineSyncManager.performServerBackup(context) { success, msg ->
                        if (success) {
                            com.example.ui.components.ToastNotificationBus.showToast(
                                message = msg,
                                type = com.example.ui.components.AppToastType.SUCCESS
                            )
                        } else {
                            com.example.ui.components.ToastNotificationBus.showToast(
                                message = msg,
                                type = com.example.ui.components.AppToastType.ERROR
                            )
                        }
                    }
                },
                onCreateProjectClick = onCreateProjectClick,
                onCloseDrawer = {
                    coroutineScope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Sleek, Spacious Top Header with Three-Dash Menu Button
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Leading: Three-dash Menu Button & Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                onClick = {
                                    coroutineScope.launch {
                                        drawerState.open()
                                    }
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("projects_open_drawer_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "قائمة إعدادات المشاريع",
                                        tint = adaptiveGoldPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "المشاريع",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (selectedStatusFilter != null || selectedTypeFilter != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = selectedStatusFilter ?: selectedTypeFilter ?: "",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${filteredProjects.size} مشروع متاح",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Trailing: Compact Action Icons (Sync, New Project)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (isBiometricLocked) {
                                OozoxCircleIconButton(
                                    onClick = { triggerBiometricUnlock() },
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "فك القفل",
                                    tint = Color(0xFFEF4444),
                                    containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)
                                )
                            }

                            OozoxCircleIconButton(
                                onClick = onRefresh,
                                imageVector = Icons.Default.Sync,
                                contentDescription = "تحديث",
                                tint = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )

                            OozoxCircleIconButton(
                                onClick = onCreateProjectClick,
                                imageVector = Icons.Default.Add,
                                contentDescription = "مشروع جديد",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Clean Full-Width Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("بحث في المشاريع", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 42.dp)
                            .testTag("projects_search_bar")
                    )
                }
            }

        if (isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "projects_skeleton_list") {
                    ProjectListSkeleton(count = 4)
                }
            }
        } else if (filteredProjects.isEmpty()) {
            val emptyType = when {
                searchQuery.isNotBlank() -> ProjectEmptyStateType.SEARCH_NO_RESULTS
                selectedTypeFilter != null -> ProjectEmptyStateType.CATEGORY_FILTER_EMPTY
                else -> ProjectEmptyStateType.PROJECTS_EMPTY
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                ExecutiveProjectsEmptyState(
                    emptyType = emptyType,
                    searchQuery = searchQuery,
                    categoryTitle = selectedTypeFilter ?: "",
                    onCreateProjectClick = onCreateProjectClick,
                    onRefresh = onRefresh,
                    onResetFilters = {
                        searchQuery = ""
                        selectedTypeFilter = null
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isRefreshing) {
                    item(key = "projects_refresh_skeleton") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "جارٍ مزامنة المشاريع والحسابات التشاركية...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Interactive Budget & Status Analytics Visualizer (toggled from side drawer)
                if (showAnalytics) {
                    item(key = "projects_analytics_charts") {
                        ProjectAnalyticsDashboard(
                            projects = projects,
                            onProjectClick = onProjectClick
                        )
                    }
                }

                items(filteredProjects, key = { it.id }) { proj ->
                    val linkedGroup = workGroups.find { it.id == proj.workGroupId }
                    ProjectCard(
                        project = proj,
                        workGroup = linkedGroup,
                        onClick = { onProjectClick(proj.id) },
                        onOpenProjectChat = onOpenProjectChat?.let { { it(proj) } },
                        onDeleteClick = { projectPendingDelete = proj }
                    )
                }
            }
        }

        // Delete Project Confirmation Dialog Guard
        if (projectPendingDelete != null) {
            val targetProj = projectPendingDelete!!
            DeleteProjectConfirmationDialog(
                project = targetProj,
                onDismiss = { projectPendingDelete = null },
                onConfirm = {
                    onDeleteProject(targetProj.id)
                    projectPendingDelete = null
                }
            )
        }
    }
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectCard(
    project: ProjectEntity,
    workGroup: WorkGroupEntity?,
    onClick: () -> Unit,
    onOpenProjectChat: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    val isBudgetExceeded = project.estimatedCapital > 0 && project.totalExpenses > project.estimatedCapital
    val actualBurnPercent = if (project.estimatedCapital > 0) {
        ((project.totalExpenses / project.estimatedCapital) * 100).toInt()
    } else 0
    val progress = if (project.estimatedCapital > 0) {
        (project.totalExpenses / project.estimatedCapital).coerceIn(0.0, 1.0).toFloat()
    } else 0f
    val remainingBalance = (project.estimatedCapital - project.totalExpenses).coerceAtLeast(0.0)
    val excessAmount = if (isBudgetExceeded) project.totalExpenses - project.estimatedCapital else 0.0

    val burnRate = if (project.estimatedCapital > 0) (project.totalExpenses / project.estimatedCapital) * 100 else 0.0
    val (statusLabel, statusColor) = when {
        burnRate > 100 -> Pair("تجاوز الميزانية", Color(0xFFEF4444))
        burnRate in 80.0..100.0 -> Pair("استهلاك مرتفع", Color(0xFFF59E0B))
        burnRate in 1.0..79.9 -> Pair("تحت الميزانية", Color(0xFF10B981))
        project.totalExpenses == 0.0 && project.estimatedCapital > 0 -> Pair("قيد التأسيس", Color(0xFF3B82F6))
        else -> Pair("نشط", Color(0xFF10B981))
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "project_card_scale"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(
            if (isBudgetExceeded) 1.5.dp else 1.dp,
            if (isBudgetExceeded) Color(0xFFEF4444).copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
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
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Icon + Project Name (Horizontal) + Status Badge + Actions
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
                            .clip(RoundedCornerShape(9.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = project.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = statusColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, statusColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    val cardContext = androidx.compose.ui.platform.LocalContext.current
                    if (onOpenProjectChat != null) {
                        OozoxCircleIconButton(
                            onClick = onOpenProjectChat,
                            imageVector = Icons.Default.Forum,
                            contentDescription = "مجموعة دردشة المشروع",
                            tint = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.testTag("project_card_chat_btn_${project.id}")
                        )
                    }

                    OozoxCircleIconButton(
                        onClick = {
                            com.example.util.ReportExporter.exportProjectSummaryToPdf(
                                context = cardContext,
                                project = project
                            )
                        },
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )

                    if (onDeleteClick != null) {
                        OozoxCircleIconButton(
                            onClick = onDeleteClick,
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error,
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            // Row 2: Type & Group (Horizontal)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = project.type,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (workGroup != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("مجموعة: ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = workGroup.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Row 3: Location & Co-Manager (Horizontal)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("الموقع: ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = project.location.ifBlank { "موقع العمل" },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!project.coManagerName.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("شريك: ", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = project.coManagerName ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Row 4: Metrics Summary Surface
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("ميزانية: ", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = GoldUtils.formatCurrency(project.estimatedCapital),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
 
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("منصرف: ", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = GoldUtils.formatCurrency(project.totalExpenses),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBudgetExceeded) Color(0xFFDC2626) else StatusOrange,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
 
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                        Text(if (isBudgetExceeded) "عجز: " else "باقي: ", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (isBudgetExceeded) "- ${GoldUtils.formatCurrency(excessAmount)}" else GoldUtils.formatCurrency(remainingBalance),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBudgetExceeded) Color(0xFFDC2626) else StatusGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Row 5: Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isBudgetExceeded) "تجاوز الميزانية المرصودة" else "نسبة استهلاك الميزانية",
                        fontSize = 10.5.sp,
                        fontWeight = if (isBudgetExceeded) FontWeight.Bold else FontWeight.Normal,
                        color = if (isBudgetExceeded) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$actualBurnPercent%",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBudgetExceeded) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                    )
                }

                LinearProgressIndicator(
                    progress = { if (isBudgetExceeded) 1f else progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when {
                        isBudgetExceeded -> Color(0xFFDC2626)
                        actualBurnPercent > 90 -> StatusRed
                        actualBurnPercent > 70 -> StatusOrange
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = if (isBudgetExceeded) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

