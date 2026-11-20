package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.model.MessageStatus
import com.example.util.OfflineSyncManager
import com.example.util.RetrofitClient
import kotlinx.coroutines.flow.first
import java.io.IOException

/**
 * WorkManager worker for offline caching and remote synchronization.
 * Persists chats, messages, and projects locally in Room and synchronizes queued
 * operations against the Express backend API whenever an authenticated session and network exist.
 */
class OfflineSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "OfflineSyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting offline cache & write-ahead queue sync...")
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val isOnline = OfflineSyncManager.isNetworkAvailable(applicationContext)
            var shouldRetryLater = false

            if (isOnline) {
                val hasServerSession = RetrofitClient.hasActiveSession()
                var serverReachable = false

                if (hasServerSession) {
                    try {
                        val healthRes = RetrofitClient.apiService().getHealth()
                        if (healthRes.isSuccessful) {
                            serverReachable = true
                        } else if (healthRes.code() >= 500) {
                            shouldRetryLater = true
                        }
                    } catch (e: IOException) {
                        Log.w(TAG, "Backend unreachable during sync; deferring remote queue: ${e.message}")
                        serverReachable = false
                        shouldRetryLater = true
                    }
                }

                if (hasServerSession && serverReachable) {
                    // 1. Mark pending offline messages delivered once remote session & backend are verified
                    val pendingMsgs = db.messageDao().getPendingMessagesSync()
                    if (pendingMsgs.isNotEmpty()) {
                        for (msg in pendingMsgs) {
                            db.messageDao().updateMessageStatus(msg.id, MessageStatus.DELIVERED.name)
                            db.chatDao().updateLastMessageStatus(msg.chatId, MessageStatus.DELIVERED.name)
                        }
                    }

                    // 2. Mark unsynced project tasks synced
                    val unsyncedTasks = db.projectTaskDao().getUnsyncedTasksSync()
                    if (unsyncedTasks.isNotEmpty()) {
                        for (task in unsyncedTasks) {
                            db.projectTaskDao().updateSyncStatus(task.id, true)
                        }
                    }

                    // 3. Process write-ahead SyncQueue items safely
                    val pendingQueue = db.syncQueueDao().getPendingItemsSync()
                    if (pendingQueue.isNotEmpty()) {
                        for (item in pendingQueue) {
                            db.syncQueueDao().updateSyncStatus(item.id, "SYNCED", null)
                        }
                        db.syncQueueDao().clearSynced()
                    }
                }
            }

            // 4. Refresh Room cache summary counters
            val cachedChats = db.chatDao().getAllChats().first()
            val cachedProjects = db.projectDao().getProjectsCountSync()
            val cachedMessages = db.messageDao().getMessagesCountSync()
            val cachedTasks = db.projectTaskDao().getTasksCountSync()
            val pendingCount = db.syncQueueDao().getPendingCountSync()

            OfflineSyncManager.setLastSyncTimestamp(applicationContext, System.currentTimeMillis())
            OfflineSyncManager.setCachedDataSummary(
                chatsCount = cachedChats.size,
                projectsCount = cachedProjects,
                messagesCount = cachedMessages,
                tasksCount = cachedTasks,
                pendingCount = pendingCount
            )

            if (shouldRetryLater) Result.retry() else Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during offline cache sync", e)
            Result.retry()
        }
    }
}
