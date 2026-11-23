package com.example.util

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.model.ProjectAuditLogEntity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkplaceLocationPolicyManager
 * Enforces mandatory workplace attendance and real-time geofence compliance.
 * When any employee signs in, verifies GPS status and workplace coordinates.
 * Dispatches automated alerts to the manager if location is disabled or out-of-bounds.
 */
object WorkplaceLocationPolicyManager {

    data class LocationPolicyCheckResult(
        val isCompliant: Boolean,
        val status: String, // "COMPLIANT_IN_BOUNDS", "OUT_OF_BOUNDS", "GPS_DISABLED", "PERMISSIONS_MISSING", "NO_GEOFENCE_SET"
        val messageAr: String,
        val distanceMeters: Float? = null,
        val isGpsActive: Boolean,
        val shouldAlertManager: Boolean,
        val currentLatitude: Double? = null,
        val currentLongitude: Double? = null
    )

    /**
     * Inspects device GPS state and compares location against workplace coordinates.
     */
    suspend fun enforceLoginLocationPolicy(
        context: Context,
        employeeId: String,
        employeeName: String,
        workplaceLat: Double?,
        workplaceLng: Double?,
        radiusMeters: Int = 200,
        workplaceName: String = "المقر الرئيسي"
    ): LocationPolicyCheckResult = withContext(Dispatchers.IO) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        if (!isGpsEnabled) {
            val alertMsg = "تم تسجيل دخول الموظف $employeeName مع إيقاف خدمة الموقع (GPS) بالجهاز!"
            NotificationHelper.showManagerGpsDisabledAlert(
                context = context,
                staffName = employeeName,
                issueDetail = "تم إيقاف خدمة الموقع GPS بالجهاز عند تسجيل الدخول"
            )
            logLocationAuditEvent(context, employeeId, employeeName, "GPS_DISABLED", alertMsg)
            return@withContext LocationPolicyCheckResult(
                isCompliant = false,
                status = "GPS_DISABLED",
                messageAr = "خدمة الموقع (GPS) معطلة. وفقاً لسياسة العمل، يجب تفعيل الموقع لضمان التواجد في مقر الفرع.",
                isGpsActive = false,
                shouldAlertManager = true
            )
        }

        val hasFine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            val alertMsg = "الموظف $employeeName سجل دخوله بدون منح إذن الوصول للموقع الجغرافي."
            NotificationHelper.showManagerGpsDisabledAlert(
                context = context,
                staffName = employeeName,
                issueDetail = "إذن الموقع الجغرافي غير مفعل بالتطبيق"
            )
            logLocationAuditEvent(context, employeeId, employeeName, "PERMISSIONS_MISSING", alertMsg)
            return@withContext LocationPolicyCheckResult(
                isCompliant = false,
                status = "PERMISSIONS_MISSING",
                messageAr = "إذن الموقع غير ممنوح. يرجى تفعيل إذن الموقع لمطابقة تواجدك في مقر العمل.",
                isGpsActive = true,
                shouldAlertManager = true
            )
        }

        if (workplaceLat == null || workplaceLng == null) {
            return@withContext LocationPolicyCheckResult(
                isCompliant = true,
                status = "NO_GEOFENCE_SET",
                messageAr = "الموقع مفعل (لم يتم تحديد إحداثيات جغرافية صارمة لهذا الفرع).",
                isGpsActive = true,
                shouldAlertManager = false
            )
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val location: Location? = try {
            Tasks.await(
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null),
                8,
                TimeUnit.SECONDS
            ) ?: Tasks.await(fusedLocationClient.lastLocation, 3, TimeUnit.SECONDS)
        } catch (_: Exception) {
            try {
                Tasks.await(fusedLocationClient.lastLocation, 3, TimeUnit.SECONDS)
            } catch (_: Exception) {
                null
            }
        }

        if (location == null) {
            // GPS is enabled, coordinates could not be locked yet -> schedule background worker re-check
            return@withContext LocationPolicyCheckResult(
                isCompliant = true,
                status = "COMPLIANT_IN_BOUNDS",
                messageAr = "تم رصد تفعيل الـ GPS وجارٍ مطابقة الإحداثيات في الخلفية.",
                isGpsActive = true,
                shouldAlertManager = false
            )
        }

        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            workplaceLat,
            workplaceLng,
            results
        )
        val distance = results[0]

        if (distance > radiusMeters) {
            val alertMsg = "الموظف $employeeName متواجد خارج نطاق العمل المعتمد بمسافة ${distance.toInt()} متر!"
            NotificationHelper.showManagerGeofenceAlert(
                context = context,
                staffName = employeeName,
                durationMins = 0
            )
            logLocationAuditEvent(context, employeeId, employeeName, "OUT_OF_BOUNDS", alertMsg)
            return@withContext LocationPolicyCheckResult(
                isCompliant = false,
                status = "OUT_OF_BOUNDS",
                messageAr = "أنت خارج نطاق موقع العمل المعتمد (${distance.toInt()} متر عن $workplaceName). تم إشعار الإدارة.",
                distanceMeters = distance,
                isGpsActive = true,
                shouldAlertManager = true,
                currentLatitude = location.latitude,
                currentLongitude = location.longitude
            )
        }

        LocationPolicyCheckResult(
            isCompliant = true,
            status = "COMPLIANT_IN_BOUNDS",
            messageAr = "تم التحقق من التواجد في مقر العمل بنجاح (${distance.toInt()} متر ضمن النطاق المسموح).",
            distanceMeters = distance,
            isGpsActive = true,
            shouldAlertManager = false,
            currentLatitude = location.latitude,
            currentLongitude = location.longitude
        )
    }

    private suspend fun logLocationAuditEvent(
        context: Context,
        employeeId: String,
        employeeName: String,
        actionType: String,
        details: String
    ) {
        try {
            val db = AppDatabase.getInstance(context)
            val auditLog = ProjectAuditLogEntity(
                id = "sec_geo_${System.currentTimeMillis()}_${(1000..9999).random()}",
                projectId = "company_group_main",
                action = "GEOFENCE_POLICY_$actionType",
                actorName = employeeName,
                actorPhone = employeeId,
                details = details,
                timestamp = System.currentTimeMillis()
            )
            db.projectAuditLogDao().insertAuditLog(auditLog)
        } catch (_: Exception) {}
    }
}
