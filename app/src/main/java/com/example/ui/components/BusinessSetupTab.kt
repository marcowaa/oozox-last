package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import com.example.util.trFmt
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
    canEditBusiness: Boolean = true,
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
    val defaultGroupTitle = tr("hr.staff_group_title")
    val defaultCompanyNameTitle = tr("biz.default_company_name")
    var companyGroupName by remember(businessProfile, defaultGroupTitle) {
        mutableStateOf(
            businessProfile?.companyGroupName?.ifBlank { defaultGroupTitle }
                ?: defaultGroupTitle
        )
    }
    var isLocatingGps by remember { mutableStateOf(false) }

    val msgPermNeeded = tr("biz.gps_perm_needed")
    val msgCapturedOk = tr("biz.gps_captured_ok")
    val msgGpsUnavailable = tr("biz.gps_unavailable")

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            Toast.makeText(context, msgPermNeeded, Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, msgCapturedOk, Toast.LENGTH_SHORT).show()
                } else {
                    client.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            hqLatitude = lastLoc.latitude
                            hqLongitude = lastLoc.longitude
                            Toast.makeText(context, msgCapturedOk, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, msgGpsUnavailable, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                isLocatingGps = false
                Toast.makeText(context, msgGpsUnavailable, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, msgPermNeeded, Toast.LENGTH_SHORT).show()
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
                text = tr("biz.setup_desc"),
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
                        text = tr("biz.step_sector"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )

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
                                        onClick = { if (canEditBusiness) selectedSector = sector },
                                        enabled = canEditBusiness,
                                        label = {
                                            Text(
                                                text = sector.localizedLabel(),
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
                            enabled = canEditBusiness,
                            label = { Text(tr("biz.custom_activity"), fontSize = 11.5.sp) },
                            placeholder = { Text(tr("biz.custom_activity_placeholder")) },
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
                        text = tr("biz.step_identity"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )

                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        enabled = canEditBusiness,
                        label = { Text(tr("biz.company_name")) },
                        placeholder = { Text(tr("biz.company_name_placeholder")) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("biz_company_name_input")
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = commercialReg,
                            onValueChange = { commercialReg = it },
                            enabled = canEditBusiness,
                            label = { Text(tr("biz.commercial_reg"), fontSize = 11.5.sp) },
                            placeholder = { Text("123456") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = taxNumber,
                            onValueChange = { taxNumber = it },
                            enabled = canEditBusiness,
                            label = { Text(tr("biz.tax_number"), fontSize = 11.5.sp) },
                            placeholder = { Text("987-654-321") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            enabled = canEditBusiness,
                            label = { Text(tr("biz.company_phone"), fontSize = 11.5.sp) },
                            placeholder = { Text("010XXXXXXXX") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it.uppercase() },
                            enabled = canEditBusiness,
                            label = { Text(tr("biz.currency"), fontSize = 11.5.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("EGP", "SAR", "AED", "KWD", "USD", "EUR").forEach { code ->
                            FilterChip(
                                selected = currency == code,
                                onClick = { if (canEditBusiness) currency = code },
                                enabled = canEditBusiness,
                                label = { Text(code, fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OozoxTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        enabled = canEditBusiness,
                        label = { Text(tr("biz.hq_address"), fontSize = 11.5.sp) },
                        placeholder = { Text(tr("biz.address_placeholder")) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = workStartTime,
                            onValueChange = { workStartTime = it },
                            enabled = canEditBusiness,
                            label = { Text(tr("biz.work_start"), fontSize = 11.sp) },
                            placeholder = { Text("09:00") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = workEndTime,
                            onValueChange = { workEndTime = it },
                            enabled = canEditBusiness,
                            label = { Text(tr("biz.work_end"), fontSize = 11.sp) },
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
                        text = tr("biz.step_geofence"),
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
                        enabled = canEditBusiness && !isLocatingGps,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    ) {
                        if (isLocatingGps) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tr("biz.gps_capturing"), fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tr("biz.use_current_gps"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            } catch (_: Exception) {}
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(tr("biz.open_gps_settings"), fontSize = 11.sp, color = OozoxTeal)
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
                                    text = trFmt(
                                        "biz.gps_coords_fmt",
                                        "lat" to String.format(Locale.US, "%.5f", hqLatitude),
                                        "lng" to String.format(Locale.US, "%.5f", hqLongitude)
                                    ),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Text(tr("biz.gps_set_ok"), fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = trFmt("biz.geofence_radius_slider_fmt", "meters" to geofenceRadius.toInt()),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = geofenceRadius,
                        onValueChange = { if (canEditBusiness) geofenceRadius = it },
                        enabled = canEditBusiness,
                        valueRange = 50f..1500f,
                        steps = 28,
                        colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary)
                    )

                    OutlinedTextField(
                        value = companyGroupName,
                        onValueChange = { companyGroupName = it },
                        enabled = canEditBusiness,
                        label = { Text(tr("biz.company_group_name"), fontSize = 11.5.sp) },
                        placeholder = { Text(tr("hr.staff_group_title")) },
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
                        customSectorText.trim().ifBlank { selectedSector.localizedLabel() }
                    } else {
                        selectedSector.localizedLabel()
                    }
                    val profile = (businessProfile ?: BusinessProfileEntity()).copy(
                        companyName = companyName.trim().ifBlank { defaultCompanyNameTitle },
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
                        companyGroupName = companyGroupName.trim().ifBlank { defaultGroupTitle },
                        isSetupCompleted = true,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSaveProfile(profile)
                },
                enabled = canEditBusiness && companyName.isNotBlank(),
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
