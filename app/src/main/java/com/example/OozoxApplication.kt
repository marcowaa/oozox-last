package com.example

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy

/**
 * OOZOX Application instance configuring global Coil ImageLoader with
 * high-performance memory cache (25% RAM limit), disk cache (64MB),
 * hardware bitmap acceleration, and modern trimMemory handling for Android Q+.
 */
class OozoxApplication : Application(), ImageLoaderFactory {

    private var coilImageLoader: ImageLoader? = null

    override fun onCreate() {
        super.onCreate()
        // Initialize Notification Channels & FCM Token Manager
        com.example.util.FcmNotificationManager.initialize(this)
        try {
            scheduleGeofenceMonitoring()
            com.example.util.OfflineSyncManager.schedulePeriodicSync(this)
        } catch (e: Throwable) {
            android.util.Log.w("OozoxApp", "WorkManager init skipped or failed: ${e.message}")
        }
    }

    private fun scheduleGeofenceMonitoring() {
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.service.GeofenceMonitoringWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        ).setConstraints(
            androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
                .build()
        ).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "GeofenceMonitoringWork",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun newImageLoader(): ImageLoader {
        val loader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Allocate at most 25% of available heap memory to the cache
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("oozox_coil_cache"))
                    .maxSizeBytes(64L * 1024 * 1024) // 64 MB
                    .build()
            }
            // Memory & Disk cache policies
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // Hardware bitmaps for GPU-backed zero-copy composition on Android O+
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            .allowRgb565(false) // Disable legacy RGB565 to prevent deprecated ashmem pinning on Android Q+
            .crossfade(true)
            .crossfade(200)
            .respectCacheHeaders(false) // Optimize cache hits for static assets
            .build()

        coilImageLoader = loader
        return loader
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            coilImageLoader?.memoryCache?.clear()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        coilImageLoader?.memoryCache?.clear()
    }
}

