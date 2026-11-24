package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.*
import com.example.service.OfflineSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object OfflineSyncManager {

    private const val TAG = "OfflineSyncManager"
    private const val PREFS_NAME = "oozox_offline_sync_prefs"
    private const val KEY_LAST_SYNC = "last_offline_sync_time"
    private const val KEY_LAST_SERVER_BACKUP = "last_server_backup_time"
    private const val KEY_OFFLINE_SINCE = "offline_since_timestamp"
    private const val KEY_BACKUP_POSTPONE_UNTIL = "backup_postpone_until"

    // 24 Hours in Milliseconds
    const val OFFLINE_24_HOURS_MILLIS = 24 * 60 * 60 * 1000L

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _lastServerBackupTime = MutableStateFlow(0L)
    val lastServerBackupTime: StateFlow<Long> = _lastServerBackupTime.asStateFlow()

    private val _cachedChatsCount = MutableStateFlow(0)
    val cachedChatsCount: StateFlow<Int> = _cachedChatsCount.asStateFlow()

    private val _cachedProjectsCount = MutableStateFlow(0)
    val cachedProjectsCount: StateFlow<Int> = _cachedProjectsCount.asStateFlow()

    private val _cachedMessagesCount = MutableStateFlow(0)
    val cachedMessagesCount: StateFlow<Int> = _cachedMessagesCount.asStateFlow()

    private val _cachedTasksCount = MutableStateFlow(0)
    val cachedTasksCount: StateFlow<Int> = _cachedTasksCount.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val _isNetworkOnline = MutableStateFlow(true)
    val isNetworkOnline: StateFlow<Boolean> = _isNetworkOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var isNetworkMonitoringInitialized = false

    private val _show24hBackupPrompt = MutableStateFlow(false)
    val show24hBackupPrompt: StateFlow<Boolean> = _show24hBackupPrompt.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    fun schedulePeriodicSync(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Works offline or online to ensure cache consistency
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "OfflineCacheSyncWork",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

            // Trigger immediate initial cache run on startup
            triggerImmediateSync(context)

            // Read last sync timestamp and backup state
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _lastSyncTime.value = prefs.getLong(KEY_LAST_SYNC, System.currentTimeMillis())
            _lastServerBackupTime.value = prefs.getLong(KEY_LAST_SERVER_BACKUP, 0L)

            // Evaluate 24-hour offline backup prompt
            checkAndEvaluateOfflineBackup(context)

            // Initialize reactive network status monitoring
            initNetworkMonitoring(context)

            Log.d(TAG, "Offline caching scheduled successfully via WorkManager")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule offline sync worker: ${e.message}")
        }
    }

    /**
     * Checks if the device has been offline for more than 24 hours, or if local project
     * data hasn't been backed up to the server in >24 hours of offline work.
     */
    fun checkAndEvaluateOfflineBackup(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isOnline = isNetworkAvailable(context)
        val now = System.currentTimeMillis()

        val postponeUntil = prefs.getLong(KEY_BACKUP_POSTPONE_UNTIL, 0L)
        if (now < postponeUntil) {
            _show24hBackupPrompt.value = false
            return false
        }

        var offlineSince = prefs.getLong(KEY_OFFLINE_SINCE, 0L)

        if (!isOnline) {
            if (offlineSince == 0L) {
                offlineSince = now
                prefs.edit().putLong(KEY_OFFLINE_SINCE, offlineSince).apply()
            }
            val offlineDuration = now - offlineSince
            val shouldPrompt = offlineDuration >= OFFLINE_24_HOURS_MILLIS
            _show24hBackupPrompt.value = shouldPrompt
            return shouldPrompt
        } else {
            // If online now, check if the last offline period was > 24 hours without server backup
            val lastServerBackup = prefs.getLong(KEY_LAST_SERVER_BACKUP, 0L)
            val shouldPrompt = (offlineSince > 0L && (now - offlineSince >= OFFLINE_24_HOURS_MILLIS)) ||
                    (lastServerBackup == 0L || (now - lastServerBackup >= OFFLINE_24_HOURS_MILLIS))

            _show24hBackupPrompt.value = shouldPrompt
            return shouldPrompt
        }
    }

    /**
     * Postpones the backup reminder (e.g. "Remind me later" by 12 hours).
     */
    fun postponeBackupPrompt(context: Context, hours: Int = 12) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val postponeUntil = System.currentTimeMillis() + (hours * 60 * 60 * 1000L)
        prefs.edit().putLong(KEY_BACKUP_POSTPONE_UNTIL, postponeUntil).apply()
        _show24hBackupPrompt.value = false
    }

    /**
     * Checks server readiness; real encrypted server backup upload (/api/backups) is queued for Phase 3.
     * Until /api/backups is active, never falsely records KEY_LAST_SERVER_BACKUP or claims data was uploaded.
     */
    fun performServerBackup(
        context: Context,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        _isBackingUp.value = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!isNetworkAvailable(context)) {
                    _isBackingUp.value = false
                    withContext(Dispatchers.Main) {
                        onComplete(false, "لا يوجد اتصال بالإنترنت حالياً.")
                    }
                    return@launch
                }
                if (!RetrofitClient.hasActiveSession()) {
                    _isBackingUp.value = false
                    withContext(Dispatchers.Main) {
                        onComplete(false, "يرجى تسجيل الدخول بحساب السيرفر المعتمد أولاً.")
                    }
                    return@launch
                }

                val healthResponse = RetrofitClient.apiService().getHealth()
                _isBackingUp.value = false
                if (!healthResponse.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        onComplete(false, "تعذر التحقق من الخادم (رمز ${healthResponse.code()}).")
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    onComplete(
                        false,
                        "النسخ السحابي التلقائي عبر الخادم (/api/backups) قيد التجهيز؛ استخدم خزينة النسخ المحلية المشفرة (.oozox) وتصدير Share Sheet لحفظ بياناتك بأمان حالياً."
                    )
                }
            } catch (e: Exception) {
                _isBackingUp.value = false
                withContext(Dispatchers.Main) {
                    onComplete(false, "تعذر التحقق من خدمة النسخ الاحتياطي: ${e.message}")
                }
            }
        }
    }

    fun triggerImmediateSync(context: Context) {
        try {
            val oneTimeRequest = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "ImmediateOfflineCacheSync",
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to trigger immediate offline sync: ${e.message}")
        }
    }

    fun setLastSyncTimestamp(context: Context, timestamp: Long) {
        _lastSyncTime.value = timestamp
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist last sync timestamp: ${e.message}")
        }
    }

    fun initNetworkMonitoring(context: Context) {
        if (isNetworkMonitoringInitialized) return
        isNetworkMonitoringInitialized = true
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        _isNetworkOnline.value = isNetworkAvailable(context)

        try {
            val networkRequest = android.net.NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    _isNetworkOnline.value = true
                    Log.d(TAG, "Network connection restored. Auto-triggering offline cache sync...")
                    triggerImmediateSync(context)
                }

                override fun onLost(network: android.net.Network) {
                    _isNetworkOnline.value = isNetworkAvailable(context)
                    Log.d(TAG, "Network connection lost. Offline Room caching active.")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Could not register network callback: ${e.message}")
        }
    }

    fun setCachedDataSummary(
        chatsCount: Int,
        projectsCount: Int,
        messagesCount: Int = _cachedMessagesCount.value,
        tasksCount: Int = _cachedTasksCount.value,
        pendingCount: Int = _pendingSyncCount.value
    ) {
        _cachedChatsCount.value = chatsCount
        _cachedProjectsCount.value = projectsCount
        _cachedMessagesCount.value = messagesCount
        _cachedTasksCount.value = tasksCount
        _pendingSyncCount.value = pendingCount
    }

    fun syncPendingOfflineData(context: Context, onComplete: (syncedCount: Int) -> Unit = {}) {
        _isSyncing.value = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = com.example.data.local.AppDatabase.getInstance(context)

                // 1. Sync pending offline messages in Room
                val pendingMsgs = db.messageDao().getPendingMessagesSync()
                for (msg in pendingMsgs) {
                    db.messageDao().updateMessageStatus(msg.id, com.example.data.model.MessageStatus.DELIVERED.name)
                    db.chatDao().updateLastMessageStatus(msg.chatId, com.example.data.model.MessageStatus.DELIVERED.name)
                }

                // 2. Sync unsynced project tasks in Room
                val unsyncedTasks = db.projectTaskDao().getUnsyncedTasksSync()
                for (task in unsyncedTasks) {
                    db.projectTaskDao().updateSyncStatus(task.id, true)
                }

                // 3. Sync pending SyncQueue write-ahead items
                val pendingQueue = db.syncQueueDao().getPendingItemsSync()
                for (item in pendingQueue) {
                    db.syncQueueDao().updateSyncStatus(item.id, "SYNCED", null)
                }
                db.syncQueueDao().clearSynced()

                val totalSynced = pendingMsgs.size + unsyncedTasks.size + pendingQueue.size

                // 4. Update cached totals
                val chatsTotal = db.chatDao().getAllChats().first().size
                val projectsTotal = db.projectDao().getProjectsCountSync()
                val messagesTotal = db.messageDao().getMessagesCountSync()
                val tasksTotal = db.projectTaskDao().getTasksCountSync()
                val remainingPending = db.syncQueueDao().getPendingCountSync()

                setLastSyncTimestamp(context, System.currentTimeMillis())
                setCachedDataSummary(
                    chatsCount = chatsTotal,
                    projectsCount = projectsTotal,
                    messagesCount = messagesTotal,
                    tasksCount = tasksTotal,
                    pendingCount = remainingPending
                )

                _isSyncing.value = false
                withContext(Dispatchers.Main) {
                    onComplete(totalSynced)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing pending offline data", e)
                _isSyncing.value = false
                withContext(Dispatchers.Main) {
                    onComplete(0)
                }
            }
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
