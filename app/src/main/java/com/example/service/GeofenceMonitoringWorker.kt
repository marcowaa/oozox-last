package com.example.service

import android.content.Context
import android.location.Location
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.repository.GoldChatRepository
import com.example.util.NotificationHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class GeofenceMonitoringWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = GoldChatRepository(applicationContext)
        val curDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val allAttendance = repository.db.employeeAttendanceDao().getAllAttendance().first()
        val clockedInMembers = allAttendance.filter { it.status == "CLOCKED_IN" && it.date == curDate }

        if (clockedInMembers.isEmpty()) return Result.success()

        // Check for location permissions before accessing fusedLocationClient
        val hasFineLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            applicationContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(
            applicationContext,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            return Result.success()
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        val location = try {
            val priority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
            Tasks.await(fusedLocationClient.getCurrentLocation(priority, null), 10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            null
        } ?: return Result.retry()

        for (attendance in clockedInMembers) {
            val member = repository.db.workGroupDao().getMemberById(attendance.memberId).first() ?: continue
            
            if (member.geofencingEnabled && member.workplaceLatitude != null && member.workplaceLongitude != null) {
                val results = FloatArray(1)
                Location.distanceBetween(
                    location.latitude,
                    location.longitude,
                    member.workplaceLatitude,
                    member.workplaceLongitude,
                    results
                )
                val distance = results[0]
                val radius = member.geofencingRadiusMeters.toDouble()

                if (distance > radius) {
                    // Out of bounds
                    val now = System.currentTimeMillis()
                    val outOfBoundsSince = attendance.outOfBoundsSince ?: now
                    
                    // Update attendance record
                    val updatedAttendance = attendance.copy(
                        outOfBoundsSince = outOfBoundsSince,
                        isLocationVerified = false,
                        verificationDetails = "خارج النطاق (${distance.toInt()}م)"
                    )
                    repository.db.employeeAttendanceDao().insertAttendance(updatedAttendance)

                    // Send notification to employee
                    NotificationHelper.showStaffGeofenceWarning(applicationContext, attendance.employeeName)

                    // Check if > 15 mins
                    val diffMins = (now - outOfBoundsSince) / (1000 * 60)
                    if (diffMins >= 15) {
                        NotificationHelper.showManagerGeofenceAlert(applicationContext, attendance.employeeName, diffMins.toInt())
                    }
                } else {
                    // Back in bounds
                    if (attendance.outOfBoundsSince != null) {
                        val updatedAttendance = attendance.copy(
                            outOfBoundsSince = null,
                            isLocationVerified = true,
                            verificationDetails = "ضمن النطاق (${distance.toInt()}م)"
                        )
                        repository.db.employeeAttendanceDao().insertAttendance(updatedAttendance)
                    }
                }
            }
        }

        return Result.success()
    }
}
