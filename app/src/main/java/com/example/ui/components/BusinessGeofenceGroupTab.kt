package com.example.ui.components

import android.Manifest
import android.content.Context
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.BusinessProfileEntity
import com.example.data.model.EmployeeAttendanceEntity
import com.example.data.model.HiddenSubAccountEntity
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OozoxTeal
import com.example.util.WorkplaceLocationPolicyManager
import com.example.util.tr
import kotlinx.coroutines.launch

/**
 * Business Geofence & Company Group Tab:
 * Displays the Unified Company Staff Group (with direct chat and task assignment),
 * Mandatory Employee GPS Clock-In Portal, and Real-Time Geofence Violation Alerts.
 */
@Composable
fun BusinessGeofenceGroupTab(
    businessProfile: BusinessProfileEntity?,
    allEmployees: List<HiddenSubAccountEntity>,
    allAttendanceRecords: List<EmployeeAttendanceEntity>,
    activeSubAccount: HiddenSubAccountEntity?,
    onClockInMandatoryGps: suspend (HiddenSubAccountEntity) -> WorkplaceLocationPolicyManager.LocationPolicyCheckResult,
    onOpenCompanyGroupChat: () -> Unit,
    onAssignTaskToGroup: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isCheckingGps by remember { mutableStateOf(false) }
    var lastCheckResult by remember { mutableStateOf<WorkplaceLocationPolicyManager.LocationPolicyCheckResult?>(null) }
    var selectedEmployeeForClockIn by remember(allEmployees, activeSubAccount) {
        mutableStateOf(activeSubAccount ?: allEmployees.firstOrNull { it.isActiveEmployee })
    }

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
                    val res = onClockInMandatoryGps(emp)
                    lastCheckResult = res
                    isCheckingGps = false
                }
            }
        } else {
            Toast.makeText(context, "إذن الموقع إلزامي لاستلام العمل", Toast.LENGTH_LONG).show()
        }
    }

    fun startClockInProcess() {
        val emp = selectedEmployeeForClockIn
        if (emp == null) {
            Toast.makeText(context, "يرجى اختيار الموظف أولاً", Toast.LENGTH_SHORT).show()
            return
        }

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
            val res = onClockInMandatoryGps(emp)
            lastCheckResult = res
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    text = businessProfile?.companyGroupName?.ifBlank { "مجموعة كل الموظفين" } ?: "مجموعة كل الموظفين",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تضم كافة موظفي الشركة لإسناد المهام والتنبيهات المباشرة",
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
                                text = "${allEmployees.count { it.isActiveEmployee }} موظف",
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
                            modifier = Modifier.weight(1.2f).heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("hr.open_staff_group"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onAssignTaskToGroup,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            modifier = Modifier.weight(1.4f).heightIn(min = 44.dp)
                        ) {
                            Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("hr.assign_task_group"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Mandatory GPS Shift Check-in Portal
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
                        Text("بوابة استلام العمل (إلزام بالـ GPS)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "يفرض النظام على الموظف تفعيل الموقع والبقاء داخل نطاق المقر الرئيسي (${businessProfile?.geofenceRadiusMeters ?: 250}م). في حال الخروج أو إيقاف الـ GPS، يتم إشعار المدير تلقائياً.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Employee Selector Chips
                    if (allEmployees.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allEmployees.filter { it.isActiveEmployee }.take(4).forEach { emp ->
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
                        modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp).testTag("mandatory_gps_clockin_btn")
                    ) {
                        if (isCheckingGps) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري فحص الموقع والـ GPS...", fontSize = 12.sp)
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
                                        text = if (isOk) "✓ تم التحقق واستلام العمل بنجاح" else "🛑 تنبيه إداري ومخالفة سياسة الموقع",
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
                                        Text("⚙️ فتح إعدادات الموقع بالجهاز لتفعيل الـ GPS", fontSize = 11.5.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Live Attendance & Geofence Logs Feed
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                    Text("سجل الحضور والالتزام الجغرافي اليوم", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }

                Text("${allAttendanceRecords.size} سجلات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("لا توجد حركات حضور مسجلة اليوم", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(allAttendanceRecords) { rec ->
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    "CLOCKED_IN" -> "حاضر بالمقر ✓"
                                    "OUT_OF_BOUNDS" -> "خارج النطاق 🚨"
                                    "GPS_DISABLED" -> "GPS معطل ⚠"
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
