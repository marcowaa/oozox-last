package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.EmployeeAttendanceEntity
import com.example.data.model.GroupMemberEntity
import com.example.data.model.WorkGroupEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.SimpleExcelEngine
import com.example.util.acctStr
import java.text.SimpleDateFormat
import java.util.*

/**
 * Visual Attendance Logger & Shift Adherence Section
 * Displays clocked-in/out times against defined shift hours for each employee with visual gauges,
 * KPI statistics, quick clock-in/out triggers, manual logging, and Excel export.
 */
@Composable
fun VisualAttendanceLoggerSection(
    currentGroup: WorkGroupEntity?,
    allGroups: List<WorkGroupEntity>,
    members: List<GroupMemberEntity>,
    viewModel: MainViewModel,
    onEditShift: (GroupMemberEntity) -> Unit,
    onAddStaffClick: () -> Unit,
    onDmAttempt: (GroupMemberEntity, WorkGroupEntity?) -> Unit,
    onDeleteStaff: (GroupMemberEntity) -> Unit,
    onToggleGroupPrivacy: (WorkGroupEntity, Boolean) -> Unit
) {
    val context = LocalContext.current
    val allAttendance by viewModel.allAttendanceRecords.collectAsStateWithLifecycle()

    // Filter attendance for current group
    val groupAttendance = remember(allAttendance, currentGroup) {
        if (currentGroup != null) {
            allAttendance.filter { it.groupId == currentGroup.id }
        } else {
            allAttendance
        }
    }

    var activeSubTab by remember { mutableIntStateOf(0) } // 0: مسجل الحضور المرئي, 1: كادر العمل والورديات, 2: تحليلات الالتزام
    var selectedDateFilter by remember { mutableStateOf("اليوم") } // "اليوم", "الكل"
    var selectedMemberFilter by remember { mutableStateOf<String?>(null) } // null for all
    var staffSearchQuery by remember { mutableStateOf("") }
    var staffRoleFilter by remember { mutableStateOf("الكل") } // "الكل", "مسؤولون", "ترحيل قيود"
    var showManualAttendanceDialog by remember { mutableStateOf(false) }
    var editingAttendanceRecord by remember { mutableStateOf<EmployeeAttendanceEntity?>(null) }
    var quickClockInMember by remember { mutableStateOf<GroupMemberEntity?>(null) }
    var quickClockOutRecord by remember { mutableStateOf<EmployeeAttendanceEntity?>(null) }

    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val filteredRecords = remember(groupAttendance, selectedDateFilter, selectedMemberFilter) {
        groupAttendance.filter { record ->
            val matchesDate = when (selectedDateFilter) {
                "اليوم" -> record.date == todayDateStr
                else -> true
            }
            val matchesMember = selectedMemberFilter == null || record.memberId == selectedMemberFilter
            matchesDate && matchesMember
        }
    }

    val filteredMembers = remember(members, staffSearchQuery, staffRoleFilter) {
        val q = staffSearchQuery.trim()
        members.filter { m ->
            val matchesSearch = q.isEmpty() ||
                m.username.contains(q, ignoreCase = true) ||
                m.roleLabel.contains(q, ignoreCase = true) ||
                m.phone.contains(q, ignoreCase = true)
            val matchesRole = when (staffRoleFilter) {
                "مسؤولون" -> m.isGroupAdmin
                "ترحيل قيود" -> m.canPostAccounting
                else -> true
            }
            matchesSearch && matchesRole
        }
    }

    // Metrics for the current view
    val totalActiveNow = remember(filteredRecords) { filteredRecords.count { it.clockOutTime == null } }
    val onTimeCount = remember(filteredRecords) { filteredRecords.count { it.status == "ON_TIME" || it.status == "OVERTIME" } }
    val lateCount = remember(filteredRecords) { filteredRecords.count { it.status == "LATE" || it.lateMinutes > 0 } }
    val totalOvertimeMinutes = remember(filteredRecords) { filteredRecords.sumOf { it.overtimeMinutes } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sub-Tab Switcher
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple(0, acctStr("staff_tab_attendance"), Icons.Default.AccessTimeFilled),
                    Triple(1, acctStr("staff_tab_team"), Icons.Default.Groups),
                    Triple(2, acctStr("staff_tab_analytics"), Icons.Default.Analytics)
                ).forEach { (idx, title, icon) ->
                    val isSelected = activeSubTab == idx
                    Surface(
                        color = if (isSelected) adaptiveGoldPrimary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable { activeSubTab = idx }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) adaptiveOnPrimary else adaptiveGoldPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) adaptiveOnPrimary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Sub-Tab Contents
        when (activeSubTab) {
            0 -> {
                // Visual Attendance Logger Sub-Tab
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 112.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Live Shift & Current Time Header Card
                    item {
                        val addStaffLabel = acctStr("add_staff")
                        val groupsFallback = acctStr("sec_groups")
                        val attendanceLabel = acctStr("staff_tab_attendance")
                        val exportErrorLabel = acctStr("export_error")
                        LiveShiftClockHeader(
                            onQuickClockInClick = {
                                if (members.isNotEmpty()) {
                                    quickClockInMember = members.first()
                                } else {
                                    Toast.makeText(context, addStaffLabel, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onManualAddClick = { showManualAttendanceDialog = true },
                            onExportExcelClick = {
                                val file = SimpleExcelEngine.exportAttendanceToExcelCsv(
                                    context = context,
                                    groupName = currentGroup?.name ?: groupsFallback,
                                    companyName = currentGroup?.companyName,
                                    attendanceList = groupAttendance
                                )
                                if (file != null) {
                                    SimpleExcelEngine.shareExcelFile(context, file, attendanceLabel)
                                } else {
                                    Toast.makeText(context, exportErrorLabel, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    // Shift & Attendance KPI Quick Metrics
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AttendanceKpiBox(
                                title = acctStr("present_now"),
                                value = "$totalActiveNow",
                                subtitle = "/ ${members.size}",
                                icon = Icons.Default.CheckCircle,
                                color = adaptiveStatusGreen,
                                modifier = Modifier.weight(1f)
                            )
                            AttendanceKpiBox(
                                title = acctStr("on_time"),
                                value = "$onTimeCount",
                                subtitle = acctStr("staff_tab_attendance"),
                                icon = Icons.Default.Verified,
                                color = adaptiveGoldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            AttendanceKpiBox(
                                title = acctStr("late_arrivals"),
                                value = "$lateCount",
                                subtitle = acctStr("late_minutes_unit"),
                                icon = Icons.Default.WarningAmber,
                                color = if (lateCount > 0) adaptiveStatusRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            AttendanceKpiBox(
                                title = acctStr("overtime"),
                                value = if (totalOvertimeMinutes > 0) "+${totalOvertimeMinutes / 60}${acctStr("work_hours_unit")}" else "0",
                                subtitle = acctStr("overtime_minutes_unit"),
                                icon = Icons.Default.Bolt,
                                color = adaptiveStatusBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Filters: Date & Staff Filter Row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedDateFilter == "اليوم",
                                onClick = { selectedDateFilter = "اليوم" },
                                label = { Text("${acctStr("filter_today")} ($todayDateStr)", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = adaptiveGoldPrimary,
                                    selectedLabelColor = adaptiveOnPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            FilterChip(
                                selected = selectedDateFilter == "الكل",
                                onClick = { selectedDateFilter = "الكل" },
                                label = { Text("${acctStr("all")} (${groupAttendance.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = adaptiveGoldPrimary,
                                    selectedLabelColor = adaptiveOnPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            VerticalDivider(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant)

                            FilterChip(
                                selected = selectedMemberFilter == null,
                                onClick = { selectedMemberFilter = null },
                                label = { Text(acctStr("all"), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = adaptiveGoldPrimary,
                                    selectedLabelColor = adaptiveOnPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            members.forEach { m ->
                                FilterChip(
                                    selected = selectedMemberFilter == m.id,
                                    onClick = { selectedMemberFilter = if (selectedMemberFilter == m.id) null else m.id },
                                    label = { Text(m.username, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = adaptiveGoldPrimary,
                                        selectedLabelColor = adaptiveOnPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // Attendance Records List Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${acctStr("staff_tab_attendance")} (${filteredRecords.size})",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = adaptiveGoldPrimary
                            )
                        }
                    }

                    if (filteredRecords.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        acctStr("no_attendance"),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Visual Attendance Item Cards
                    items(filteredRecords, key = { it.id }) { record ->
                        VisualEmployeeAttendanceCard(
                            record = record,
                            onClockOut = {
                                quickClockOutRecord = record
                            },
                            onEdit = {
                                editingAttendanceRecord = record
                            },
                            onDelete = {
                                viewModel.deleteAttendanceRecord(record.id)
                                Toast.makeText(context, "تم حذف قيد الحضور", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            1 -> {
                // Staff & Shifts Matrix Sub-Tab
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 112.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Group Privacy Card
                    if (currentGroup != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (currentGroup.disableMemberDMs) adaptiveGoldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (currentGroup.disableMemberDMs) adaptiveGoldPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Surface(
                                                color = if (currentGroup.disableMemberDMs) adaptiveStatusRed.copy(alpha = 0.16f) else adaptiveStatusGreen.copy(alpha = 0.16f),
                                                shape = CircleShape,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (currentGroup.disableMemberDMs) Icons.Default.SpeakerNotesOff else Icons.Default.Forum,
                                                        contentDescription = null,
                                                        tint = if (currentGroup.disableMemberDMs) adaptiveStatusRed else adaptiveStatusGreen,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    acctStr("lock_dms_label"),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (currentGroup.disableMemberDMs) adaptiveGoldPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    if (currentGroup.disableMemberDMs) acctStr("dm_blocked") else acctStr("dm_allowed"),
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = currentGroup.disableMemberDMs,
                                            onCheckedChange = { isChecked ->
                                                onToggleGroupPrivacy(currentGroup, isChecked)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = adaptiveOnPrimary,
                                                checkedTrackColor = adaptiveGoldPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add Staff Button & Search Bar
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onAddStaffClick,
                                colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(acctStr("add_staff"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedTextField(
                                value = staffSearchQuery,
                                onValueChange = { staffSearchQuery = it },
                                placeholder = { Text(acctStr("search_staff"), fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (staffSearchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { staffSearchQuery = "" },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = acctStr("close"))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "الكل" to acctStr("all"),
                                    "مسؤولون" to acctStr("default_badge"),
                                    "ترحيل قيود" to acctStr("record_entry")
                                ).forEach { (key, label) ->
                                    FilterChip(
                                        selected = staffRoleFilter == key,
                                        onClick = { staffRoleFilter = key },
                                        shape = RoundedCornerShape(12.dp),
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = adaptiveGoldPrimary,
                                            selectedLabelColor = adaptiveOnPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Staff List
                    item {
                        Text(
                            "${acctStr("sec_staff")} (${filteredMembers.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = adaptiveGoldPrimary
                        )
                    }

                    items(filteredMembers, key = { it.id }) { member ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (member.isGroupAdmin) adaptiveGoldPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            color = if (member.isGroupAdmin) adaptiveGoldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = CircleShape,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (member.isGroupAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = if (member.isGroupAdmin) adaptiveGoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(member.username, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                if (member.isGroupAdmin) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(color = adaptiveGoldPrimary.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                                                        Text(acctStr("default_badge"), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                                    }
                                                }
                                            }
                                            Text(member.roleLabel, fontSize = 11.sp, color = adaptiveGoldPrimary)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { quickClockInMember = member },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.Timer, contentDescription = acctStr("quick_checkin"), tint = adaptiveStatusGreen, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(
                                            onClick = { onDmAttempt(member, currentGroup) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = if (currentGroup?.disableMemberDMs == true && !member.canMessageDirectly) Icons.Default.CommentsDisabled else Icons.Default.Chat,
                                                contentDescription = acctStr("open_channel"),
                                                tint = if (currentGroup?.disableMemberDMs == true && !member.canMessageDirectly) adaptiveStatusRed else adaptiveStatusGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onEditShift(member) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = acctStr("edit"), tint = adaptiveGoldPrimary, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(
                                            onClick = { onDeleteStaff(member) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = acctStr("delete"), tint = adaptiveStatusRed, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "${member.workingHoursStart ?: "09:00"} - ${member.workingHoursEnd ?: "17:00"}",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Surface(
                                        color = adaptiveGoldPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "${member.shiftHours} ${acctStr("work_hours_unit")}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = adaptiveGoldPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(member.workingDays, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(member.shiftType, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                //  Shift Adherence Analytics Sub-Tab
                ShiftAdherenceAnalyticsTab(
                    attendanceList = groupAttendance,
                    members = members,
                    groupName = currentGroup?.name ?: "الفرع الرئيسي"
                )
            }
        }
    }

    // Quick Clock-In Dialog
    if (quickClockInMember != null) {
        QuickClockInDialog(
            member = quickClockInMember!!,
            membersList = members,
            onDismiss = { quickClockInMember = null },
            onConfirm = { selectedMember, note, lat, lng ->
                viewModel.clockInEmployee(selectedMember, note, lat, lng) { created ->
                    val verification = if (created.isLocationVerified) "تم التحقق من الموقع" else "بدون تحقق جغرافي"
                    Toast.makeText(context, "تم تسجيل حضور ${selectedMember.username} بنجاح ($verification)", Toast.LENGTH_LONG).show()
                    quickClockInMember = null
                }
            }
        )
    }

    // Quick Clock-Out Dialog
    if (quickClockOutRecord != null) {
        QuickClockOutDialog(
            record = quickClockOutRecord!!,
            onDismiss = { quickClockOutRecord = null },
            onConfirm = { note ->
                viewModel.clockOutEmployee(quickClockOutRecord!!.id, note) {
                    Toast.makeText(context, "تم تسجيل انصراف ${quickClockOutRecord!!.employeeName} وحساب الساعات الفعلية", Toast.LENGTH_SHORT).show()
                    quickClockOutRecord = null
                }
            }
        )
    }

    // Manual Attendance Dialog (New or Edit)
    if (showManualAttendanceDialog || editingAttendanceRecord != null) {
        ManualAttendanceEntryDialog(
            existingRecord = editingAttendanceRecord,
            members = members,
            groupId = currentGroup?.id ?: "",
            onDismiss = {
                showManualAttendanceDialog = false
                editingAttendanceRecord = null
            },
            onSave = { record ->
                viewModel.saveAttendanceRecord(record) {
                    Toast.makeText(context, "تم حفظ قيد الحضور بنجاح", Toast.LENGTH_SHORT).show()
                    showManualAttendanceDialog = false
                    editingAttendanceRecord = null
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Live Shift Clock & Action Header
// -------------------------------------------------------------
@Composable
private fun LiveShiftClockHeader(
    onQuickClockInClick: () -> Unit,
    onManualAddClick: () -> Unit,
    onExportExcelClick: () -> Unit
) {
    val currentTimeStr = remember {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Calendar.getInstance().time)
    }
    val currentDateStr = remember {
        SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date())
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = adaptiveStatusGreen.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(10.dp)
                        ) {
                            Box(modifier = Modifier.background(adaptiveStatusGreen))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = acctStr("staff_tab_attendance"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = adaptiveGoldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentDateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = adaptiveGoldPrimary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, adaptiveGoldPrimary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(currentTimeStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onQuickClockInClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1.3f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(acctStr("quick_checkin"), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onManualAddClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = adaptiveGoldPrimary),
                    border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(acctStr("manual_entry"), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = onExportExcelClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = adaptiveStatusGreen),
                    border = BorderStroke(1.dp, adaptiveStatusGreen.copy(alpha = 0.65f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(0.9f).heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = adaptiveStatusGreen, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(acctStr("excel"), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun AttendanceKpiBox(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(3.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun VisualEmployeeAttendanceCard(
    record: EmployeeAttendanceEntity,
    onClockOut: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isClockedInOnly = record.clockOutTime == null
    val targetHours = record.scheduledShiftHours.coerceAtLeast(1.0)
    val workedHours = record.actualWorkedHours
    val progressRatio = (workedHours / targetHours).toFloat().coerceIn(0f, 1.3f)

    val primaryGold = adaptiveGoldPrimary
    val onGold = adaptiveOnPrimary
    val greenColor = adaptiveStatusGreen
    val redColor = adaptiveStatusRed
    val blueColor = adaptiveStatusBlue
    val orangeColor = adaptiveStatusOrange

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            when {
                record.status == "OVERTIME" -> blueColor.copy(alpha = 0.5f)
                record.status == "LATE" -> redColor.copy(alpha = 0.45f)
                isClockedInOnly -> greenColor.copy(alpha = 0.45f)
                else -> primaryGold.copy(alpha = 0.3f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = if (isClockedInOnly) greenColor.copy(alpha = 0.16f) else primaryGold.copy(alpha = 0.14f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, if (isClockedInOnly) greenColor else primaryGold.copy(alpha = 0.45f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = record.employeeName.take(1),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isClockedInOnly) greenColor else primaryGold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = record.employeeName,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(record.employeeRole, fontSize = 10.5.sp, color = primaryGold)
                            if (!record.locationTag.isNullOrBlank()) {
                                Text(" • ${record.locationTag}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                val badgeColor = when (record.status) {
                    "CLOCKED_IN" -> greenColor
                    "OVERTIME" -> blueColor
                    "LATE" -> redColor
                    "EARLY_DEPARTURE" -> orangeColor
                    else -> primaryGold
                }
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = when (record.status) {
                            "CLOCKED_IN" -> acctStr("present_now")
                            "OVERTIME" -> acctStr("overtime")
                            "LATE" -> acctStr("late_arrivals")
                            "ON_TIME" -> acctStr("on_time")
                            else -> record.status
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, primaryGold.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(acctStr("shift_hours"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${record.scheduledShiftStart ?: "09:00"} - ${record.scheduledShiftEnd ?: "17:00"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryGold
                            )
                            Text("${record.scheduledShiftHours} ${acctStr("work_hours_unit")}", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(acctStr("staff_tab_attendance"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${record.clockInTime ?: "-"} - ${record.clockOutTime ?: "..."}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isClockedInOnly) greenColor else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${record.actualWorkedHours} ${acctStr("work_hours_unit")}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (record.overtimeMinutes > 0) blueColor else primaryGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(acctStr("adherence_rate"), fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = when {
                                    record.overtimeMinutes > 0 -> "+${record.overtimeMinutes} ${acctStr("overtime_minutes_unit")}"
                                    record.lateMinutes > 0 -> "${record.lateMinutes} ${acctStr("late_minutes_unit")}"
                                    else -> acctStr("on_time")
                                },
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (record.overtimeMinutes > 0) blueColor else if (record.lateMinutes > 0) redColor else greenColor
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = progressRatio.coerceAtMost(1f))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                primaryGold,
                                                if (record.overtimeMinutes > 0) blueColor else greenColor
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            if (!record.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notes, contentDescription = null, tint = primaryGold.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(record.notes, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(record.date, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isClockedInOnly) {
                        Button(
                            onClick = onClockOut,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = orangeColor, contentColor = onGold),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(acctStr("clock_out"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Edit, contentDescription = acctStr("edit"), tint = primaryGold, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = acctStr("delete"), tint = redColor, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftAdherenceAnalyticsTab(
    attendanceList: List<EmployeeAttendanceEntity>,
    members: List<GroupMemberEntity>,
    groupName: String
) {
    val totalHoursWorked = attendanceList.sumOf { it.actualWorkedHours }
    val totalOvertimeMinutes = attendanceList.sumOf { it.overtimeMinutes }
    val totalRecords = attendanceList.size.coerceAtLeast(1)
    val onTimeRate = (attendanceList.count { it.status == "ON_TIME" || it.status == "OVERTIME" } * 100) / totalRecords

    val primaryGold = adaptiveGoldPrimary
    val greenColor = adaptiveStatusGreen
    val blueColor = adaptiveStatusBlue
    val orangeColor = adaptiveStatusOrange
    val redColor = adaptiveStatusRed

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 112.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, primaryGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(acctStr("staff_tab_analytics"), fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                    Text(groupName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(acctStr("adherence_rate"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Text("$onTimeRate%", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (onTimeRate >= 80) greenColor else orangeColor)
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(acctStr("total_hours"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Text("${kotlin.math.round(totalHoursWorked * 10) / 10} ${acctStr("work_hours_unit")}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = primaryGold)
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(acctStr("overtime"), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Text("${totalOvertimeMinutes / 60}${acctStr("work_hours_unit")}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = blueColor)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(acctStr("sec_staff"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryGold)
        }

        items(members, key = { it.id }) { member ->
            val memberRecords = attendanceList.filter { it.memberId == member.id }
            val memberWorked = memberRecords.sumOf { it.actualWorkedHours }
            val memberOvertime = memberRecords.sumOf { it.overtimeMinutes }
            val memberLate = memberRecords.sumOf { it.lateMinutes }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(member.username, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(member.roleLabel, fontSize = 10.5.sp, color = primaryGold)
                        Text("${member.shiftHours} ${acctStr("work_hours_unit")} (${member.workingHoursStart ?: "-"} - ${member.workingHoursEnd ?: "-"})", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                        Text("${kotlin.math.round(memberWorked * 10) / 10} ${acctStr("work_hours_unit")}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                        if (memberOvertime > 0) {
                            Text("+${memberOvertime} ${acctStr("overtime_minutes_unit")}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = blueColor)
                        }
                        if (memberLate > 0) {
                            Text("${memberLate} ${acctStr("late_minutes_unit")}", fontSize = 10.sp, color = redColor)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Quick Clock-In Dialog
// -------------------------------------------------------------
@Composable
private fun QuickClockInDialog(
    member: GroupMemberEntity,
    membersList: List<GroupMemberEntity>,
    onDismiss: () -> Unit,
    onConfirm: (GroupMemberEntity, String?, Double?, Double?) -> Unit
) {
    var selectedMember by remember { mutableStateOf(member) }
    var noteText by remember { mutableStateOf("") }
    val timeNowLocalized = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) }

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentLat by remember { mutableStateOf<Double?>(null) }
    var currentLng by remember { mutableStateOf<Double?>(null) }
    var isLocating by remember { mutableStateOf(false) }

    val primaryGold = adaptiveGoldPrimary
    val onGold = adaptiveOnPrimary
    val greenColor = adaptiveStatusGreen

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    fun obtainLocation() {
        isLocating = true
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
                if (loc != null) {
                    currentLat = loc.latitude
                    currentLng = loc.longitude
                    isLocating = false
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            currentLat = lastLoc.latitude
                            currentLng = lastLoc.longitude
                        }
                        isLocating = false
                    }.addOnFailureListener {
                        isLocating = false
                    }
                }
            }.addOnFailureListener {
                isLocating = false
            }
        } catch (_: SecurityException) {
            isLocating = false
        }
    }

    val manualNoGpsMsg = acctStr("manual_no_gps")
    val requestLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            obtainLocation()
        } else {
            isLocating = false
            Toast.makeText(context, manualNoGpsMsg, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(selectedMember.id) {
        if (selectedMember.geofencingEnabled) {
            val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                           ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            hasLocationPermission = isGranted
            if (isGranted) {
                obtainLocation()
            } else {
                currentLat = null
                currentLng = null
                isLocating = false
            }
        } else {
            currentLat = null
            currentLng = null
            isLocating = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
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
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, primaryGold),
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 620.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = primaryGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(acctStr("quick_checkin"), fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(acctStr("sec_staff"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        membersList.forEach { m ->
                            FilterChip(
                                selected = selectedMember.id == m.id,
                                onClick = { selectedMember = m },
                                label = { Text(m.username, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryGold,
                                    selectedLabelColor = onGold
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(acctStr("job_title"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(selectedMember.roleLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(acctStr("shift_start"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(selectedMember.workingHoursStart ?: "09:00", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(acctStr("quick_checkin"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(timeNowLocalized, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = greenColor)
                            }
                        }
                    }

                    if (selectedMember.geofencingEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = if (currentLat != null) greenColor.copy(alpha = 0.12f) else if (!hasLocationPermission) primaryGold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (currentLat != null) greenColor.copy(alpha = 0.5f) else if (!hasLocationPermission) primaryGold.copy(alpha = 0.5f) else Color.Transparent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!hasLocationPermission) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = primaryGold, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(acctStr("geofencing"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                                    }
                                    Button(
                                        onClick = {
                                            requestLocationLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryGold, contentColor = onGold),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(acctStr("allow_location"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (isLocating) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = primaryGold, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(acctStr("geofencing"), fontSize = 11.sp, color = primaryGold)
                                    }
                                } else if (currentLat != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = greenColor, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(acctStr("location_verified"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = greenColor)
                                    }
                                } else {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(acctStr("manual_no_gps"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        TextButton(onClick = { obtainLocation() }, modifier = Modifier.heightIn(min = 48.dp)) {
                                            Text(acctStr("refresh"), fontSize = 11.sp, color = primaryGold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text(acctStr("notes_optional")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text(acctStr("cancel"))
                        }

                        Button(
                            onClick = {
                                if (selectedMember.geofencingEnabled && !hasLocationPermission) {
                                    requestLocationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else {
                                    onConfirm(selectedMember, noteText.ifBlank { null }, currentLat, currentLng)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGold, contentColor = onGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f).heightIn(min = 48.dp),
                            enabled = !isLocating
                        ) {
                            if (isLocating) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = onGold, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = acctStr("confirm_checkin"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickClockOutDialog(
    record: EmployeeAttendanceEntity,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var noteText by remember { mutableStateOf("") }
    val timeNowLocalized = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) }
    val primaryGold = adaptiveGoldPrimary
    val onGold = adaptiveOnPrimary
    val greenColor = adaptiveStatusGreen
    val orangeColor = adaptiveStatusOrange

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
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
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, orangeColor),
                modifier = Modifier.fillMaxWidth(0.94f)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = orangeColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(acctStr("clock_out"), fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(acctStr("sec_staff"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(record.employeeName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(acctStr("quick_checkin"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(record.clockInTime ?: "-", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = greenColor)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(acctStr("clock_out"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(timeNowLocalized, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = orangeColor)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text(acctStr("notes_optional")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text(acctStr("cancel"))
                        }

                        Button(
                            onClick = { onConfirm(noteText.ifBlank { null }) },
                            colors = ButtonDefaults.buttonColors(containerColor = orangeColor, contentColor = onGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f).heightIn(min = 48.dp)
                        ) {
                            Text(acctStr("confirm_checkout"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualAttendanceEntryDialog(
    existingRecord: EmployeeAttendanceEntity?,
    members: List<GroupMemberEntity>,
    groupId: String,
    onDismiss: () -> Unit,
    onSave: (EmployeeAttendanceEntity) -> Unit
) {
    var selectedMember by remember {
        mutableStateOf(members.find { it.id == existingRecord?.memberId } ?: members.firstOrNull())
    }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var dateText by remember { mutableStateOf(existingRecord?.date ?: todayStr) }
    var clockInText by remember { mutableStateOf(existingRecord?.clockInTime ?: (selectedMember?.workingHoursStart ?: "09:00")) }
    var clockOutText by remember { mutableStateOf(existingRecord?.clockOutTime ?: (selectedMember?.workingHoursEnd ?: "17:00")) }
    var shiftHoursText by remember { mutableStateOf(existingRecord?.scheduledShiftHours?.toString() ?: (selectedMember?.shiftHours?.toString() ?: "8.0")) }
    var actualHoursText by remember { mutableStateOf(existingRecord?.actualWorkedHours?.toString() ?: "8.0") }
    val defaultTreasuryLabel = acctStr("main_treasury")
    var locationText by remember { mutableStateOf(existingRecord?.locationTag ?: defaultTreasuryLabel) }
    var notesText by remember { mutableStateOf(existingRecord?.notes ?: "") }
    var statusSelection by remember { mutableStateOf(existingRecord?.status ?: "ON_TIME") }

    val primaryGold = adaptiveGoldPrimary
    val onGold = adaptiveOnPrimary

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
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
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, primaryGold),
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 640.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PostAdd, contentDescription = null, tint = primaryGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (existingRecord != null) acctStr("edit") else acctStr("manual_entry"),
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryGold
                        )
                    }

                    Text(acctStr("sec_staff"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = primaryGold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        members.forEach { m ->
                            FilterChip(
                                selected = selectedMember?.id == m.id,
                                onClick = {
                                    selectedMember = m
                                    shiftHoursText = m.shiftHours.toString()
                                    if (clockInText.isBlank()) clockInText = m.workingHoursStart ?: "09:00"
                                    if (clockOutText.isBlank()) clockOutText = m.workingHoursEnd ?: "17:00"
                                },
                                label = { Text(m.username, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryGold,
                                    selectedLabelColor = onGold
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text(acctStr("target_date")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = clockInText,
                            onValueChange = { clockInText = it },
                            label = { Text(acctStr("shift_start")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = clockOutText,
                            onValueChange = { clockOutText = it },
                            label = { Text(acctStr("shift_end")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = shiftHoursText,
                            onValueChange = { shiftHoursText = it },
                            label = { Text(acctStr("shift_hours")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = actualHoursText,
                            onValueChange = { actualHoursText = it },
                            label = { Text(acctStr("total_hours")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ON_TIME" to acctStr("on_time"),
                            "OVERTIME" to acctStr("overtime"),
                            "LATE" to acctStr("late_arrivals"),
                            "CLOCKED_IN" to acctStr("present_now")
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = statusSelection == code,
                                onClick = { statusSelection = code },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryGold,
                                    selectedLabelColor = onGold
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = locationText,
                        onValueChange = { locationText = it },
                        label = { Text(acctStr("workplace_name")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text(acctStr("notes_optional")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text(acctStr("cancel"))
                        }

                        Button(
                            onClick = {
                                val memberToUse = selectedMember ?: return@Button
                                val schedHours = shiftHoursText.toDoubleOrNull() ?: 8.0
                                val actualHours = actualHoursText.toDoubleOrNull() ?: schedHours
                                val diffMinutes = ((actualHours - schedHours) * 60).toInt()
                                val record = EmployeeAttendanceEntity(
                                    id = existingRecord?.id ?: UUID.randomUUID().toString(),
                                    memberId = memberToUse.id,
                                    groupId = memberToUse.groupId.ifBlank { groupId },
                                    employeeName = memberToUse.username,
                                    employeeRole = memberToUse.roleLabel,
                                    date = dateText.ifBlank { todayStr },
                                    clockInTime = clockInText.ifBlank { null },
                                    clockOutTime = clockOutText.ifBlank { null },
                                    scheduledShiftStart = memberToUse.workingHoursStart ?: "09:00",
                                    scheduledShiftEnd = memberToUse.workingHoursEnd ?: "17:00",
                                    scheduledShiftHours = schedHours,
                                    actualWorkedHours = actualHours,
                                    status = statusSelection,
                                    overtimeMinutes = if (diffMinutes > 0) diffMinutes else 0,
                                    lateMinutes = if (statusSelection == "LATE") 15 else 0,
                                    locationTag = locationText.ifBlank { defaultTreasuryLabel },
                                    notes = notesText.ifBlank { null },
                                    timestamp = existingRecord?.timestamp ?: System.currentTimeMillis()
                                )
                                onSave(record)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGold, contentColor = onGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f).heightIn(min = 48.dp)
                        ) {
                            Text(acctStr("save"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
