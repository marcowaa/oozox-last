package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.EmployeeAttendanceEntity
import com.example.data.model.HiddenSubAccountEntity
import com.example.data.model.ProjectTaskEntity
import com.example.data.model.TaskStatus
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OozoxTeal
import com.example.util.WorkplaceLocationPolicyManager
import com.example.util.tr
import com.example.util.trFmt
import kotlinx.coroutines.launch

/**
 * Business Geofence & Company Group Tab:
 * Displays the Unified Company Staff Group (with direct chat and task assignment),
 * Company Group Task List with flexible Days/Hours countdown,
 * Mandatory Employee Login + GPS Clock-In Portal, and Real-Time Geofence Violation Logs.
 */
@Composable
fun BusinessGeofenceGroupTab(
    businessProfile: BusinessProfileEntity?,
    allEmployees: List<HiddenSubAccountEntity>,
    allAttendanceRecords: List<EmployeeAttendanceEntity>,
    companyTasks: List<ProjectTaskEntity> = emptyList(),
    activeSubAccount: HiddenSubAccountEntity?,
    canAssignTasks: Boolean = true,
    onClockInMandatoryGps: suspend (HiddenSubAccountEntity) -> WorkplaceLocationPolicyManager.LocationPolicyCheckResult,
    onOpenEmployeeLoginClick: () -> Unit = {},
    onOpenCompanyGroupChat: () -> Unit,
    onAssignTaskToGroup: () -> Unit,
    onUpdateTaskStatus: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val activeEmployees = remember(allEmployees) { allEmployees.filter { it.isActiveEmployee } }
    var isCheckingGps by remember { mutableStateOf(false) }
    var lastCheckResult by remember { mutableStateOf<WorkplaceLocationPolicyManager.LocationPolicyCheckResult?>(null) }
    var selectedEmployeeForClockIn by remember(activeEmployees, activeSubAccount) {
        mutableStateOf(activeSubAccount ?: activeEmployees.firstOrNull())
    }

    val gpsPermNeededMsg = tr("biz.gps_perm_needed")
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            val emp = selectedEmployeeForClockIn
            if (emp != null) {
                coroutineScope.launch {
                    isCheckingGps = true
                    lastCheckResult = onClockInMandatoryGps(emp)
                    isCheckingGps = false
                }
            }
        } else {
            Toast.makeText(context, gpsPermNeededMsg, Toast.LENGTH_LONG).show()
        }
    }

    fun startClockInProcess() {
        val emp = selectedEmployeeForClockIn ?: return
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            return
        }
        coroutineScope.launch {
            isCheckingGps = true
            lastCheckResult = onClockInMandatoryGps(emp)
            isCheckingGps = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("business_geofence_group_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Company Staff Group Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(OozoxTeal.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(24.dp))
                            }
                            Column {
                                Text(
                                    text = businessProfile?.companyGroupName?.ifBlank { tr("hr.staff_group_title") } ?: tr("hr.staff_group_title"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tr("hr.group_desc"),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OozoxTeal.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = trFmt("biz.kpi_staff_count_fmt", "count" to activeEmployees.size.toString()),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OozoxTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenCompanyGroupChat,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("open_staff_group_chat_btn")
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("hr.open_staff_group"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        if (canAssignTasks) {
                            Button(
                                onClick = onAssignTaskToGroup,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("assign_group_task_btn")
                            ) {
                                Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tr("hr.assign_task_group"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // 2. Unified Company Group Tasks Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(18.dp))
                    Text(tr("hr.company_tasks_title"), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = trFmt("hr.records_count_fmt", "count" to companyTasks.size.toString()),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (companyTasks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr("hr.no_company_tasks"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(companyTasks, key = { it.id }) { task ->
                val isDone = task.status == TaskStatus.COMPLETED.name
                val now = System.currentTimeMillis()
                val diffMs = (task.deadlineTimestamp - now).coerceAtLeast(0L)
                val totalHoursRem = diffMs / 3600000L
                val daysRem = totalHoursRem / 24L
                val remHoursPart = totalHoursRem % 24L
                val remMinsPart = (diffMs % 3600000L) / 60000L
                val countdownText = when {
                    isDone -> "✓"
                    task.deadlineTimestamp <= 0L -> task.deadlineDurationText
                    daysRem >= 1L -> trFmt("task.remaining_days_hours_fmt", "days" to daysRem.toString(), "hours" to remHoursPart.toString())
                    else -> trFmt("task.remaining_hours_mins_fmt", "hours" to totalHoursRem.toString(), "mins" to remMinsPart.toString())
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(0.8.dp, if (isDone) Color(0xFF10B981).copy(alpha = 0.35f) else GoldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = task.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = task.assignedMembersList.ifBlank { task.assignedToName ?: "-" },
                                fontSize = 11.sp,
                                color = OozoxTeal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${task.deadlineDurationText} • $countdownText",
                                fontSize = 10.5.sp,
                                color = if (isDone) Color(0xFF10B981) else GoldPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (!isDone) {
                            IconButton(
                                onClick = { onUpdateTaskStatus(task.id, TaskStatus.COMPLETED.name) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.CheckCircleOutline, contentDescription = tr("common.confirm"), tint = Color(0xFF10B981))
                            }
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }

        // 3. Mandatory Employee Login + GPS Shift Check-in Portal
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocationSearching, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        Text(tr("hr.gps_portal_title"), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = trFmt("hr.gps_portal_desc_fmt", "radius" to (businessProfile?.geofenceRadiusMeters ?: 250).toString()),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onOpenEmployeeLoginClick,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, OozoxTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("open_employee_login_portal_btn")
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = OozoxTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr("hr.login_portal_btn"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OozoxTeal)
                    }

                    // All Active Employees Selector Chips (LazyRow — no 4-item limit)
                    if (activeEmployees.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(activeEmployees, key = { it.id }) { emp ->
                                FilterChip(
                                    selected = selectedEmployeeForClockIn?.id == emp.id,
                                    onClick = { selectedEmployeeForClockIn = emp },
                                    label = { Text(emp.displayName, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldPrimary,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    // Clock In Button
                    Button(
                        onClick = { startClockInProcess() },
                        enabled = !isCheckingGps && selectedEmployeeForClockIn != null,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("mandatory_gps_clockin_btn")
                    ) {
                        if (isCheckingGps) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tr("hr.gps_checking"), fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.TouchApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tr("hr.clock_in_required_gps"), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Result Banner
                    val res = lastCheckResult
                    if (res != null) {
                        val isOk = res.isCompliant
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isOk) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, if (isOk) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isOk) Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isOk) tr("hr.gps_ok_title") else tr("hr.gps_violation_title"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isOk) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }

                                Text(text = res.messageAr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)

                                if (!res.isGpsActive) {
                                    TextButton(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                            } catch (_: Exception) {}
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(tr("biz.open_gps_settings"), fontSize = 11.5.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Live Attendance & Geofence Logs Feed
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                    Text(tr("hr.attendance_log_title"), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = trFmt("hr.records_count_fmt", "count" to allAttendanceRecords.size.toString()),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (allAttendanceRecords.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                        Text(tr("hr.no_attendance_today"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(allAttendanceRecords, key = { it.id }) { rec ->
                val isCompliant = rec.status == "CLOCKED_IN" && rec.isLocationVerified
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(0.8.dp, if (isCompliant) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFFEF4444).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isCompliant) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isCompliant) Icons.Default.Check else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isCompliant) Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(rec.employeeName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${rec.clockInTime} • ${rec.notes.orEmpty()}",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCompliant) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = when (rec.status) {
                                    "CLOCKED_IN" -> tr("hr.status_clocked_in")
                                    "OUT_OF_BOUNDS" -> tr("hr.status_out_of_bounds")
                                    "GPS_DISABLED" -> tr("hr.status_gps_disabled")
                                    else -> rec.status
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompliant) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
