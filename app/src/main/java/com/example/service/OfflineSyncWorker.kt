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
                    val api = RetrofitClient.apiService()
                    // 1. Replay pending offline messages via /api/chats/:id/messages
                    val pendingMsgs = db.messageDao().getPendingMessagesSync()
                    if (pendingMsgs.isNotEmpty()) {
                        for (msg in pendingMsgs) {
                            val sentOk = try {
                                val plain = com.example.util.RoomLocalAES256Engine.decryptMessage(msg, null, applicationContext)
                                api.sendChatMessage(
                                    conversationId = plain.chatId,
                                    request = com.example.util.ApiWorkspaceSendMessageRequest(
                                        content = plain.content,
                                        type = plain.type,
                                        clientId = plain.id
                                    )
                                ).isSuccessful
                            } catch (_: Exception) {
                                false
                            }
                            if (sentOk) {
                                db.messageDao().updateMessageStatus(msg.id, MessageStatus.DELIVERED.name)
                                db.chatDao().updateLastMessageStatus(msg.chatId, MessageStatus.DELIVERED.name)
                            }
                        }
                    }

                    // 2. Sync unsynced project tasks via /api/tasks
                    val unsyncedTasks = db.projectTaskDao().getUnsyncedTasksSync()
                    if (unsyncedTasks.isNotEmpty()) {
                        for (task in unsyncedTasks) {
                            val taskOk = try {
                                api.createWorkTask(
                                    com.example.util.ApiCreateTaskRequest(
                                        projectId = task.projectId,
                                        projectName = task.projectName,
                                        workGroupId = task.workGroupId ?: "wg_company_main",
                                        title = task.title,
                                        description = task.description.orEmpty(),
                                        timingMode = task.timingMode,
                                        durationValue = task.durationValue,
                                        assignees = listOfNotNull(task.assignedToName?.takeIf { it.isNotBlank() }),
                                        priority = task.priority,
                                        financialAmount = task.financialAmount
                                    )
                                ).isSuccessful
                            } catch (_: Exception) {
                                false
                            }
                            if (taskOk) {
                                db.projectTaskDao().updateSyncStatus(task.id, true)
                            }
                        }
                    }

                    // 3. Process write-ahead SyncQueue items against real workspace endpoints
                    val pendingQueue = db.syncQueueDao().getPendingItemsSync()
                    if (pendingQueue.isNotEmpty()) {
                        for (item in pendingQueue) {
                            val ok = try {
                                val json = org.json.JSONObject(item.payloadJson)
                                when (item.actionType) {
                                    "SEND_MESSAGE" -> {
                                        val cId = json.optString("chatId")
                                        val content = json.optString("content")
                                        if (cId.isNotBlank() && content.isNotBlank()) {
                                            api.sendChatMessage(
                                                conversationId = cId,
                                                request = com.example.util.ApiWorkspaceSendMessageRequest(
                                                    content = content,
                                                    type = json.optString("type", "TEXT"),
                                                    clientId = json.optString("messageId").takeIf { it.isNotBlank() }
                                                )
                                            ).isSuccessful
                                        } else true
                                    }
                                    "CREATE_GROUP" -> {
                                        api.createGroup(
                                            com.example.util.ApiWorkspaceCreateGroupRequest(
                                                name = json.optString("name", "مجموعة عمل"),
                                                description = json.optString("description"),
                                                isPrivate = json.optBoolean("isPrivate", true),
                                                clientId = json.optString("groupId").takeIf { it.isNotBlank() }
                                            )
                                        ).isSuccessful
                                    }
                                    "ADD_PROJECT" -> {
                                        api.createProject(
                                            com.example.util.ApiWorkspaceCreateProjectRequest(
                                                name = json.optString("name", "مشروع جديد"),
                                                type = json.optString("type", "GOLD_TRADING"),
                                                workGroupId = json.optString("workGroupId").takeIf { it.isNotBlank() },
                                                estimatedCapital = json.optDouble("capital", 0.0),
                                                location = json.optString("location"),
                                                clientId = json.optString("projectId").takeIf { it.isNotBlank() }
                                            )
                                        ).isSuccessful
                                    }
                                    else -> true
                                }
                            } catch (_: Exception) {
                                false
                            }
                            if (ok) {
                                db.syncQueueDao().updateSyncStatus(item.id, "SYNCED", null)
                            } else {
                                shouldRetryLater = true
                            }
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
