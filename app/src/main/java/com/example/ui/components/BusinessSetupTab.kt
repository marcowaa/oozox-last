package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.BusinessActivitySector
import com.example.data.model.BusinessProfileEntity
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OozoxTeal
import com.example.util.tr
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

/**
 * Business Setup Tab: Configure Company Sector, Commercial Reg,
 * Tax Number, Workplace GPS Coordinates, Geofence Radius, and Company-Wide Staff Group.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupTab(
    businessProfile: BusinessProfileEntity?,
    onSaveProfile: (BusinessProfileEntity) -> Unit
) {
    val context = LocalContext.current

    var selectedSector by remember(businessProfile) {
        mutableStateOf(
            BusinessActivitySector.entries.firstOrNull { it.name == businessProfile?.activityType }
                ?: BusinessActivitySector.GENERAL_TRADING
        )
    }
    var customSectorText by remember(businessProfile) {
        mutableStateOf(businessProfile?.customActivityLabel.orEmpty())
    }
    var companyName by remember(businessProfile) {
        mutableStateOf(businessProfile?.companyName.orEmpty())
    }
    var commercialReg by remember(businessProfile) {
        mutableStateOf(businessProfile?.commercialRegistration.orEmpty())
    }
    var taxNumber by remember(businessProfile) {
        mutableStateOf(businessProfile?.taxNumber.orEmpty())
    }
    var phone by remember(businessProfile) {
        mutableStateOf(businessProfile?.phone.orEmpty())
    }
    var address by remember(businessProfile) {
        mutableStateOf(businessProfile?.address.orEmpty())
    }
    var currency by remember(businessProfile) {
        mutableStateOf(businessProfile?.currency?.ifBlank { "EGP" } ?: "EGP")
    }
    var workStartTime by remember(businessProfile) {
        mutableStateOf(businessProfile?.workStartTime?.ifBlank { "09:00" } ?: "09:00")
    }
    var workEndTime by remember(businessProfile) {
        mutableStateOf(businessProfile?.workEndTime?.ifBlank { "17:00" } ?: "17:00")
    }
    var hqLatitude by remember(businessProfile) {
        mutableStateOf(businessProfile?.hqLatitude)
    }
    var hqLongitude by remember(businessProfile) {
        mutableStateOf(businessProfile?.hqLongitude)
    }
    var geofenceRadius by remember(businessProfile) {
        mutableFloatStateOf((businessProfile?.geofenceRadiusMeters ?: 250).toFloat())
    }
    var companyGroupName by remember(businessProfile) {
        mutableStateOf(businessProfile?.companyGroupName?.ifBlank { "مجموعة كل الموظفين" } ?: "مجموعة كل الموظفين")
    }
    var isLocatingGps by remember { mutableStateOf(false) }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            Toast.makeText(context, "يرجى منح إذن الموقع لتحديد المقر بدقة", Toast.LENGTH_SHORT).show()
            return
        }
        isLocatingGps = true
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                isLocatingGps = false
                if (loc != null) {
                    hqLatitude = loc.latitude
                    hqLongitude = loc.longitude
                    Toast.makeText(context, "تم التقاط إحداثيات موقعك الحالي كمقر رئيسي ✓", Toast.LENGTH_SHORT).show()
                } else {
                    client.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            hqLatitude = lastLoc.latitude
                            hqLongitude = lastLoc.longitude
                            Toast.makeText(context, "تم التقاط موقع المقر بنجاح ✓", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "تعذر تحديد الموقع بدقة، تأكد من تشغيل GPS", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                isLocatingGps = false
                Toast.makeText(context, "خطأ في قراءة الـ GPS: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchCurrentLocation()
        } else {
            Toast.makeText(context, "إذن الموقع مرفوض، لا يمكن قراءة GPS آلياً", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("business_setup_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = tr("biz.setup_prompt_title"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "حدد نشاطك التجاري، بيانات السجل والضريبة، ونطاق الـ GPS لإلزام الموظفين بالحضور داخل المقر.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Sector Selection
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "١. ${tr("biz.activity_type")}:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )

                    // Sector Grid
                    val sectors = BusinessActivitySector.entries
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sectors.chunked(2).forEach { rowSectors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowSectors.forEach { sector ->
                                    val isSelected = selectedSector == sector
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedSector = sector },
                                        label = {
                                            Text(
                                                text = sector.labelAr,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoldPrimary,
                                            selectedLabelColor = Color.Black
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowSectors.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (selectedSector == BusinessActivitySector.CUSTOM) {
                        OutlinedTextField(
                            value = customSectorText,
                            onValueChange = { customSectorText = it },
                            label = { Text(tr("biz.custom_activity"), fontSize = 11.5.sp) },
                            placeholder = { Text("اكتب مسمى النشاط الخاص بك...") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 2. Company Identity & Registration
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "٢. البيانات الرسمية للشركة:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text(tr("biz.company_name")) },
                        placeholder = { Text("مثال: شركة النور للمقاولات والتوريدات") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("biz_company_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commercialReg,
                            onValueChange = { commercialReg = it },
                            label = { Text(tr("biz.commercial_reg"), fontSize = 11.5.sp) },
                            placeholder = { Text("123456") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = taxNumber,
                            onValueChange = { taxNumber = it },
                            label = { Text(tr("biz.tax_number"), fontSize = 11.5.sp) },
                            placeholder = { Text("987-654-321") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم هاتف المنشأة", fontSize = 11.5.sp) },
                            placeholder = { Text("010XXXXXXXX") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it.uppercase() },
                            label = { Text("عملة التعامل", fontSize = 11.5.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(tr("biz.hq_address"), fontSize = 11.5.sp) },
                        placeholder = { Text("العنوان بالتفصيل: المدينة، الشارع، المبنى...") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = workStartTime,
                            onValueChange = { workStartTime = it },
                            label = { Text("بداية الدوام", fontSize = 11.sp) },
                            placeholder = { Text("09:00") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = workEndTime,
                            onValueChange = { workEndTime = it },
                            label = { Text("نهاية الدوام", fontSize = 11.sp) },
                            placeholder = { Text("17:00") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Workplace GPS Geofence & Staff Group
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "٣. ${tr("biz.hq_location_title")}:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )

                    Button(
                        onClick = {
                            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (hasFine) {
                                fetchCurrentLocation()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    ) {
                        if (isLocatingGps) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري التقاط إحداثيات GPS...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tr("biz.use_current_gps"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (hqLatitude != null && hqLongitude != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "إحداثيات المقر: ${String.format(Locale.US, "%.5f", hqLatitude)}, ${String.format(Locale.US, "%.5f", hqLongitude)}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Text("✓ محدد", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Geofence Radius Slider
                    Text(
                        text = "نطاق السماح الجغرافي للموظف: ${geofenceRadius.toInt()} متر",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = geofenceRadius,
                        onValueChange = { geofenceRadius = it },
                        valueRange = 50f..1500f,
                        steps = 28,
                        colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary)
                    )

                    OutlinedTextField(
                        value = companyGroupName,
                        onValueChange = { companyGroupName = it },
                        label = { Text("اسم مجموعة كل الموظفين", fontSize = 11.5.sp) },
                        placeholder = { Text("مجموعة موظفي الشركة") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    val resolvedLabel = if (selectedSector == BusinessActivitySector.CUSTOM) {
                        customSectorText.trim().ifBlank { "نشاط مخصص" }
                    } else {
                        selectedSector.labelAr
                    }
                    val profile = (businessProfile ?: BusinessProfileEntity()).copy(
                        companyName = companyName.trim().ifBlank { "شركتي" },
                        activityType = selectedSector.name,
                        customActivityLabel = resolvedLabel,
                        commercialRegistration = commercialReg.trim(),
                        taxNumber = taxNumber.trim(),
                        phone = phone.trim(),
                        address = address.trim(),
                        currency = currency.trim().ifBlank { "EGP" },
                        workStartTime = workStartTime.trim().ifBlank { "09:00" },
                        workEndTime = workEndTime.trim().ifBlank { "17:00" },
                        hqLatitude = hqLatitude,
                        hqLongitude = hqLongitude,
                        geofenceRadiusMeters = geofenceRadius.toInt().coerceIn(50, 5000),
                        companyGroupName = companyGroupName.trim().ifBlank { "مجموعة كل الموظفين" },
                        isSetupCompleted = true,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSaveProfile(profile)
                },
                enabled = companyName.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("save_business_profile_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("biz.save_company_btn"), fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
