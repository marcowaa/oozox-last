package com.example.service

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.model.ProjectAuditLogEntity
import com.example.data.repository.GoldChatRepository
import com.example.util.ApiAttendanceGeoRequest
import com.example.util.NotificationHelper
import com.example.util.RetrofitClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeofenceMonitoringWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = GoldChatRepository(applicationContext)
        val curDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val allAttendance = repository.db.employeeAttendanceDao().getAllAttendance().first()
        val clockedInMembers = allAttendance.filter {
            (it.status == "CLOCKED_IN" || it.status == "OUT_OF_BOUNDS" || it.status == "GPS_DISABLED") && it.date == curDate
        }

        if (clockedInMembers.isEmpty()) return Result.success()

        val businessProfile = repository.db.businessHrDao().getBusinessProfileSync()
        val lm = applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        val hasFineLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            applicationContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            applicationContext,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!isGpsEnabled || (!hasFineLocation && !hasCoarseLocation)) {
            for (attendance in clockedInMembers) {
                val updatedAttendance = attendance.copy(
                    status = "GPS_DISABLED",
                    gpsStatus = "DISABLED",
                    isLocationVerified = false,
                    locationPolicyViolated = true,
                    lastLocationCheckTime = System.currentTimeMillis(),
                    verificationDetails = "تم إيقاف خدمة الموقع أثناء العمل - تم إشعار المدير"
                )
                repository.db.employeeAttendanceDao().insertAttendance(updatedAttendance)
                NotificationHelper.showManagerGpsDisabledAlert(
                    applicationContext,
                    attendance.employeeName,
                    "قام الموظف بإيقاف GPS أثناء الدوام"
                )
                recordViolationAudit(
                    repository = repository,
                    employeeId = attendance.memberId,
                    employeeName = attendance.employeeName,
                    action = "GEOFENCE_GPS_DISABLED",
                    details = "تنبيه: أوقف الموظف ${attendance.employeeName} خدمة الموقع أثناء الشفت"
                )
                syncHeartbeatSilently(gpsEnabled = false, lat = null, lng = null)
            }
            return Result.success()
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        val location = try {
            Tasks.await(
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null),
                10,
                TimeUnit.SECONDS
            )
        } catch (_: Exception) {
            null
        } ?: return Result.retry()

        for (attendance in clockedInMembers) {
            val member = repository.db.workGroupDao().getMemberById(attendance.memberId).first()
            val targetLat = member?.workplaceLatitude ?: businessProfile?.hqLatitude
            val targetLng = member?.workplaceLongitude ?: businessProfile?.hqLongitude
            val radius = (member?.geofencingRadiusMeters ?: businessProfile?.geofenceRadiusMeters ?: 250).toDouble()
            val enforced = member?.geofencingEnabled ?: (businessProfile?.hqLatitude != null)

            if (enforced && targetLat != null && targetLng != null) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    location.latitude,
                    location.longitude,
                    targetLat,
                    targetLng,
                    results
                )
                val distance = results[0]

                if (distance > radius) {
                    val now = System.currentTimeMillis()
                    val outOfBoundsSince = attendance.outOfBoundsSince ?: now
                    val diffMins = ((now - outOfBoundsSince) / (1000 * 60)).toInt()

                    val updatedAttendance = attendance.copy(
                        outOfBoundsSince = outOfBoundsSince,
                        isLocationVerified = false,
                        gpsStatus = "OUT_OF_BOUNDS",
                        status = "OUT_OF_BOUNDS",
                        locationPolicyViolated = true,
                        lastLocationCheckTime = now,
                        verificationDetails = "خارج نطاق العمل (${distance.toInt()}م / الحد ${radius.toInt()}م) - تم إشعار المدير"
                    )
                    repository.db.employeeAttendanceDao().insertAttendance(updatedAttendance)

                    // Alert both employee & manager immediately on geofence breach
                    NotificationHelper.showStaffGeofenceWarning(applicationContext, attendance.employeeName)
                    NotificationHelper.showManagerGeofenceAlert(applicationContext, attendance.employeeName, diffMins)
                    recordViolationAudit(
                        repository = repository,
                        employeeId = attendance.memberId,
                        employeeName = attendance.employeeName,
                        action = "GEOFENCE_OUT_OF_BOUNDS",
                        details = "خرج الموظف ${attendance.employeeName} عن نطاق مقر العمل بمسافة ${distance.toInt()} متر"
                    )
                    syncHeartbeatSilently(gpsEnabled = true, lat = location.latitude, lng = location.longitude)
                } else {
                    val updatedAttendance = attendance.copy(
                        outOfBoundsSince = null,
                        isLocationVerified = true,
                        gpsStatus = "ACTIVE",
                        status = "CLOCKED_IN",
                        locationPolicyViolated = false,
                        lastLocationCheckTime = System.currentTimeMillis(),
                        verificationDetails = "داخل نطاق العمل (${distance.toInt()}م)"
                    )
                    repository.db.employeeAttendanceDao().insertAttendance(updatedAttendance)
                    syncHeartbeatSilently(gpsEnabled = true, lat = location.latitude, lng = location.longitude)
                }
            }
        }

        return Result.success()
    }

    private suspend fun recordViolationAudit(
        repository: GoldChatRepository,
        employeeId: String,
        employeeName: String,
        action: String,
        details: String
    ) {
        try {
            repository.db.projectAuditLogDao().insertAuditLog(
                ProjectAuditLogEntity(
                    id = "geo_alert_${System.currentTimeMillis()}_${(100..999).random()}",
                    projectId = "wg_company_main",
                    action = action,
                    actorName = employeeName,
                    actorPhone = employeeId,
                    details = details,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {}
    }

    private suspend fun syncHeartbeatSilently(gpsEnabled: Boolean, lat: Double?, lng: Double?) {
        if (!RetrofitClient.hasActiveSession()) return
        try {
            RetrofitClient.apiService().sendAttendanceHeartbeat(
                ApiAttendanceGeoRequest(
                    gpsEnabled = gpsEnabled,
                    latitude = lat,
                    longitude = lng
                )
            )
        } catch (_: Exception) {}
    }
}
