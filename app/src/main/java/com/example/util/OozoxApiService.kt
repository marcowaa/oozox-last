package com.example.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = false,
    val message: String = "",
    val data: T? = null
)

@Serializable
data class ApiHealthData(
    val status: String = "UNKNOWN",
    val uptime: Double? = null
)

@Serializable
data class ApiOtpChallengeDto(
    val challengeId: String = "",
    val activeChannel: String = "TELEGRAM",
    val expiresAt: String = "",
    val cooldownSeconds: Int = 60,
    val maxAttempts: Int = 5,
    val maskedPhone: String = "",
    val phoneE164: String = ""
)

@Serializable
data class ApiOtpDeliveryErrorDto(
    val code: String = "",
    val message: String = "",
    val retryAfterSeconds: Int? = null,
    val attemptedChannels: List<String> = emptyList()
)

@Serializable
data class ApiDeviceTrustDto(
    val deviceId: String = "",
    val deviceTrustToken: String = "",
    val trustedUntil: String = "",
    val trustDays: Int = 30
)

@Serializable
data class ApiOtpRequestBody(
    val purpose: String = "LOGIN",
    val channel: String = "AUTO",
    val allowFallback: Boolean = true,
    val phone: String? = null,
    val newPhone: String? = null,
    val preAuthToken: String? = null,
    val portalScope: String? = null,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val platform: String = "ANDROID",
    val hardwareFingerprint: String? = null
)

@Serializable
data class ApiOtpRequestData(
    val challengeId: String = "",
    val activeChannel: String = "TELEGRAM",
    val attemptedChannels: List<String> = emptyList(),
    val expiresAt: String = "",
    val cooldownSeconds: Int = 60,
    val maxAttempts: Int = 5,
    val maskedPhone: String = "",
    val preAuthToken: String? = null,
    val isExistingUser: Boolean = false
)

@Serializable
data class ApiOtpVerifyBody(
    val challengeId: String,
    val code: String,
    val phone: String? = null,
    val preAuthToken: String? = null,
    val portalScope: String? = null,
    val trustDevice: Boolean = true,
    val deviceId: String? = null,
    val deviceName: String = "Android",
    val platform: String = "ANDROID",
    val hardwareFingerprint: String? = null,
    val fcmToken: String? = null
)

@Serializable
data class ApiOtpCancelBody(
    val challengeId: String,
    val phone: String? = null,
    val preAuthToken: String? = null
)

@Serializable
data class ApiOtpCancelData(
    val cancelled: Boolean = false,
    val challengeId: String = "",
    val status: String = ""
)

@Serializable
data class ApiTrustedLoginBody(
    val phone: String? = null,
    val emailOrPhone: String? = null,
    val userId: String? = null,
    val deviceId: String,
    val deviceTrustToken: String,
    val hardwareFingerprint: String? = null
)

@Serializable
data class ApiRefreshSessionBody(
    val refreshToken: String,
    val deviceId: String? = null
)

@Serializable
data class ApiChangePhoneBody(
    val newPhone: String,
    val channel: String = "AUTO",
    val stepUpChallengeId: String? = null,
    val stepUpCode: String? = null
)

@Serializable
data class ApiUpdateProfileBody(
    val name: String? = null,
    val email: String? = null,
    val avatar: String? = null,
    val preferredLanguage: String? = null
)

@Serializable
data class ApiLogoutBody(
    val revokeDeviceTrust: Boolean = false
)

@Serializable
data class ApiFcmTokenBody(
    val fcmToken: String,
    val deviceId: String? = null,
    val platform: String = "ANDROID"
)

@Serializable
data class ApiOtherActiveDeviceDto(
    val sessionId: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val platform: String = "ANDROID",
    val lastActiveAt: String? = null,
    val expiresAt: String? = null
)

@Serializable
data class ApiBackupSummaryDto(
    val count: Int = 0,
    val latestBackupId: String? = null,
    val latestCreatedAt: String? = null,
    val latestFileName: String? = null,
    val latestTotalRecords: Int = 0,
    val latestSizeBytes: Long = 0L
)

@Serializable
data class ApiTrustedDeviceDto(
    val deviceId: String = "",
    val deviceName: String = "",
    val platform: String = "ANDROID",
    val trustedAt: String = "",
    val trustedUntil: String = "",
    val lastActiveAt: String = "",
    val lastIp: String = "",
    val revokedAt: String? = null,
    val revokeReason: String? = null,
    val isActive: Boolean = false,
    val isCurrent: Boolean = false
)

@Serializable
data class ApiDevicesListData(
    val devices: List<ApiTrustedDeviceDto> = emptyList()
)

@Serializable
data class ApiAuthSessionDto(
    val sessionId: String = "",
    val deviceId: String = "",
    val authMethod: String = "",
    val issuedAt: String = "",
    val expiresAt: String = "",
    val lastStepUpAt: String? = null,
    val ipAddress: String = "",
    val isCurrent: Boolean = false
)

@Serializable
data class ApiSessionsListData(
    val sessions: List<ApiAuthSessionDto> = emptyList()
)

@Serializable
data class ApiUserDto(
    val id: String = "",
    @SerialName("_id") val mongoId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val phoneE164: String = "",
    val role: String = "USER",
    val avatar: String = "",
    val preferredLanguage: String = "ar",
    val isVerified: Boolean = false
)

@Serializable
data class ApiAuthData(
    val user: ApiUserDto? = null,
    val token: String = "",
    val refreshToken: String = "",
    val sessionId: String = "",
    val expiresAt: String = "",
    val trustedUntil: String? = null,
    val requiresOtp: Boolean = false,
    val requiresStepUpOtp: Boolean = false,
    val code: String? = null,
    val reason: String? = null,
    val purpose: String? = null,
    val preAuthToken: String? = null,
    val maskedPhone: String? = null,
    val availableChannels: List<String> = emptyList(),
    val attemptedChannels: List<String> = emptyList(),
    val challengeId: String? = null,
    val activeChannel: String? = null,
    val cooldownSeconds: Int? = null,
    val maxAttempts: Int? = null,
    val otpChallenge: ApiOtpChallengeDto? = null,
    val otpDeliveryError: ApiOtpDeliveryErrorDto? = null,
    val trustedDevice: ApiDeviceTrustDto? = null,
    val deviceTrust: ApiDeviceTrustDto? = null,
    val allDevicesRevoked: Boolean = false,
    val isExistingUser: Boolean = false,
    val hasOtherActiveDevice: Boolean = false,
    val otherActiveDevices: List<ApiOtherActiveDeviceDto> = emptyList(),
    val backupSummary: ApiBackupSummaryDto? = null
)

// --- Cloud Backups DTOs ---
@Serializable
data class ApiBackupStatsDto(
    val usersCount: Int = 0,
    val chatsCount: Int = 0,
    val messagesCount: Int = 0,
    val recordsCount: Int = 0,
    val subProfilesCount: Int = 0,
    val workGroupsCount: Int = 0,
    val projectsCount: Int = 0,
    val mediaFilesCount: Int = 0,
    val totalRecords: Int = 0
)

@Serializable
data class ApiUploadBackupRequest(
    val fileName: String,
    val backupType: String = "FULL_VAULT_V3",
    val formatVersion: Int = 3,
    val phoneHash: String,
    val vaultContent: String,
    val deviceName: String = "Android",
    val stats: ApiBackupStatsDto = ApiBackupStatsDto()
)

@Serializable
data class ApiCloudBackupItemDto(
    val backupId: String = "",
    val backupType: String = "FULL_VAULT_V3",
    val formatVersion: Int = 3,
    val fileName: String = "",
    val sizeBytes: Long = 0L,
    val payloadSha256: String = "",
    val deviceName: String = "",
    val stats: ApiBackupStatsDto = ApiBackupStatsDto(),
    val createdAt: String = "",
    val vaultContent: String? = null
)

@Serializable
data class ApiCloudBackupsListData(
    val backups: List<ApiCloudBackupItemDto> = emptyList()
)

@Serializable
data class ApiUploadBackupResponse(
    val backup: ApiCloudBackupItemDto? = null
)

// --- Encrypted Cross-Device Transfer DTOs ---
@Serializable
data class ApiTransferInitiateRequest(
    val targetEcdhPubKey: String
)

@Serializable
data class ApiTransferInitiateData(
    val transferId: String = "",
    val status: String = "AWAITING_OLD_DEVICE",
    val expiresAt: String = "",
    val qrPayload: String = "",
    val targetEcdhPubKey: String = ""
)

@Serializable
data class ApiTransferPendingItemDto(
    val transferId: String = "",
    val status: String = "",
    val targetDeviceId: String = "",
    val targetDeviceName: String = "",
    val targetEcdhPubKey: String = "",
    val qrNonce: String = "",
    val expiresAt: String = "",
    val createdAt: String = ""
)

@Serializable
data class ApiTransferPendingData(
    val pending: List<ApiTransferPendingItemDto> = emptyList()
)

@Serializable
data class ApiTransferApproveRequest(
    val sourceEcdhPubKey: String,
    val approvalMethod: String = "DEVICE_PUSH_APPROVAL",
    val qrNonce: String? = null
)

@Serializable
data class ApiTransferUploadChunkRequest(
    val chunkIndex: Int,
    val totalChunks: Int,
    val cipherChunkBase64: String,
    val ivBase64: String,
    val chunkSha256: String
)

@Serializable
data class ApiTransferSummaryDto(
    val totalRecords: Int = 0,
    val mediaFilesCount: Int = 0,
    val totalBytes: Long = 0L
)

@Serializable
data class ApiTransferFinalizeUploadRequest(
    val totalChunks: Int,
    val encryptedManifestBase64: String,
    val manifestIvBase64: String,
    val manifestSha256: String,
    val summary: ApiTransferSummaryDto = ApiTransferSummaryDto()
)

@Serializable
data class ApiTransferStatusData(
    val transferId: String = "",
    val status: String = "",
    val approvalMethod: String? = null,
    val targetDeviceId: String = "",
    val targetDeviceName: String = "",
    val sourceDeviceId: String? = null,
    val sourceDeviceName: String? = null,
    val targetEcdhPubKey: String = "",
    val sourceEcdhPubKey: String? = null,
    val totalChunks: Int = 0,
    val receivedChunksCount: Int = 0,
    val uploadedChunkIndexes: List<Int> = emptyList(),
    val encryptedManifestBase64: String? = null,
    val manifestIvBase64: String? = null,
    val manifestSha256: String? = null,
    val summary: ApiTransferSummaryDto? = null,
    val expiresAt: String = ""
)

@Serializable
data class ApiTransferDownloadChunkData(
    val transferId: String = "",
    val chunkIndex: Int = 0,
    val totalChunks: Int = 0,
    val cipherChunkBase64: String = "",
    val ivBase64: String = "",
    val chunkSha256: String = ""
)

@Serializable
data class ApiTransferCancelRequest(
    val revokeOtherDevices: Boolean = false
)

// --- Calls, Budget & Signaling TURN DTOs ---
@Serializable
data class ApiCallAuthorizeRequest(
    val calleeId: String,
    val callType: String = "AUDIO",
    val callId: String? = null
)

@Serializable
data class ApiCallAuthorizeData(
    val allowed: Boolean = true,
    val callId: String = "",
    val callType: String = "AUDIO",
    val exp: Long = 0L,
    val callAuthSignature: String = "",
    val usagePct: Double = 0.0
)

@Serializable
data class ApiCallBudgetData(
    val monthKey: String = "",
    val totalSpentUsd: Double = 0.0,
    val monthlyBudgetUsd: Double = 0.0,
    val usagePct: Double = 0.0,
    val blockNewCalls: Boolean = false
)

@Serializable
data class ApiTurnCredentialsRequest(
    val ttl: Int = 3600
)

@Serializable
data class ApiTurnCredentialsResponse(
    val urls: List<String> = emptyList(),
    val username: String = "",
    val credential: String = "",
    val ttl: Int = 3600
)

// --- Workspace Cloud Sync DTOs ---
@Serializable
data class ApiWorkspaceCreateGroupRequest(
    val name: String,
    val description: String = "",
    val isPrivate: Boolean = true,
    val companyName: String = "",
    val clientId: String? = null
)

@Serializable
data class ApiWorkspaceCreateProjectRequest(
    val name: String,
    val type: String = "GOLD_TRADING",
    val workGroupId: String? = null,
    val estimatedCapital: Double = 0.0,
    val location: String = "",
    val clientId: String? = null
)

@Serializable
data class ApiWorkspaceCreateChatRequest(
    val type: String = "DIRECT",
    val targetUserId: String? = null,
    val title: String = "",
    val workGroupId: String? = null,
    val clientId: String? = null
)

@Serializable
data class ApiWorkspaceSendMessageRequest(
    val content: String,
    val type: String = "TEXT",
    val mediaUrl: String? = null,
    val clientId: String? = null
)

@Serializable
data class ApiSupportedLanguageDto(
    val code: String = "",
    val name: String = "",
    val nativeName: String = "",
    val flag: String = "",
    val dir: String = "ltr"
)

@Serializable
data class ApiLanguagesListData(
    val version: String = "",
    val defaultLanguage: String = "ar",
    val languages: List<ApiSupportedLanguageDto> = emptyList()
)

@Serializable
data class ApiLocalizationBundleData(
    val language: String = "ar",
    val dir: String = "ltr",
    val version: String = "",
    val count: Int = 0,
    val strings: Map<String, String> = emptyMap()
)

@Serializable
data class ApiUpdateLanguageRequest(
    val language: String
)

@Serializable
data class ApiInvoiceItemDto(
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val karat: Int = 0
)

@Serializable
data class ApiCreateInvoiceRequest(
    val receiverId: String,
    val items: List<ApiInvoiceItemDto>,
    val taxAmount: Double = 0.0,
    val discount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val dueDate: String,
    val notes: String? = null
)

@Serializable
data class ApiInvoiceDto(
    @SerialName("_id") val id: String = "",
    val invoiceNumber: String = "",
    val subTotal: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: String = "PENDING",
    val paymentMethod: String = "CASH",
    val dueDate: String = "",
    val notes: String = ""
)

@Serializable
data class ApiInvoiceData(
    val invoice: ApiInvoiceDto? = null
)

@Serializable
data class ApiPaginationDto(
    val total: Int = 0,
    val page: Int = 1,
    val pages: Int = 1,
    val limit: Int = 20
)

@Serializable
data class ApiInvoiceListData(
    val invoices: List<ApiInvoiceDto> = emptyList(),
    val pagination: ApiPaginationDto? = null
)

@Serializable
data class ApiUpdateInvoiceStatusRequest(
    val status: String
)

@Serializable
data class ApiNotificationDto(
    @SerialName("_id") val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val isRead: Boolean = false
)

@Serializable
data class ApiNotificationsData(
    val notifications: List<ApiNotificationDto> = emptyList(),
    val unreadCount: Int = 0
)

@Serializable
data class ApiBusinessProfileRequest(
    val companyName: String,
    val activityType: String,
    val customActivityLabel: String = "",
    val commercialRegistration: String = "",
    val taxNumber: String = "",
    val phone: String = "",
    val address: String = "",
    val currency: String = "EGP",
    val hqLatitude: Double? = null,
    val hqLongitude: Double? = null,
    val geofenceRadiusMeters: Int = 250,
    val companyGroupName: String = ""
)

@Serializable
data class ApiEmployeePermissionsDto(
    val manageBusiness: Boolean = false,
    val manageStaff: Boolean = false,
    val managePayroll: Boolean = false,
    val manageProjects: Boolean = false,
    val assignTasks: Boolean = false,
    val manageAccounting: Boolean = true,
    val createInvoices: Boolean = true,
    val approveDiscounts: Boolean = false,
    val viewReports: Boolean = false,
    val directChat: Boolean = true
)

@Serializable
data class ApiCreateEmployeeRequest(
    val fullName: String,
    val username: String,
    val phone: String = "",
    val password: String,
    val roleTitle: String,
    val department: String = "العمليات",
    val baseSalary: Double = 0.0,
    val salaryCurrency: String = "EGP",
    val geofenceEnforced: Boolean = true,
    val permissions: ApiEmployeePermissionsDto = ApiEmployeePermissionsDto()
)

@Serializable
data class ApiEmployeeLoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class ApiAttendanceGeoRequest(
    val employeeId: String? = null,
    val gpsEnabled: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class ApiCreateTaskRequest(
    val projectId: String,
    val projectName: String,
    val workGroupId: String = "wg_company_main",
    val title: String,
    val description: String = "",
    val timingMode: String = "HOURS",
    val durationValue: Int = 1,
    val assignees: List<String> = emptyList(),
    val priority: String = "HIGH",
    val financialAmount: Double = 0.0,
    val financialNotes: String = ""
)

@Serializable
data class ApiUpdateEmployeeRequest(
    val fullName: String? = null,
    val phone: String? = null,
    val password: String? = null,
    val roleTitle: String? = null,
    val department: String? = null,
    val baseSalary: Double? = null,
    val salaryCurrency: String? = null,
    val geofenceEnforced: Boolean? = null,
    val isActive: Boolean? = null,
    val permissions: ApiEmployeePermissionsDto? = null
)

@Serializable
data class ApiPayrollAdjustmentRequest(
    val employeeId: String,
    val type: String,
    val amount: Double,
    val reason: String,
    val effectiveDate: String
)

@Serializable
data class ApiEmployeeLeaveRequest(
    val employeeId: String,
    val leaveType: String,
    val startDate: String,
    val endDate: String,
    val notes: String = ""
)

@Serializable
data class ApiUpdateTaskStatusRequest(
    val status: String
)

interface OozoxApiService {
    @GET("health")
    suspend fun getHealth(): Response<ApiEnvelope<ApiHealthData>>

    @GET("i18n/languages")
    suspend fun getSupportedLanguages(): Response<ApiEnvelope<ApiLanguagesListData>>

    @GET("i18n/{lang}")
    suspend fun getLocalizationBundle(
        @Path("lang") lang: String,
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<ApiEnvelope<ApiLocalizationBundleData>>

    @PATCH("auth/language")
    suspend fun updatePreferredLanguage(
        @Body request: ApiUpdateLanguageRequest
    ): Response<ApiEnvelope<ApiAuthData>>

    @POST("auth/trusted-login")
    suspend fun postTrustedLogin(@Body request: ApiTrustedLoginBody): Response<ApiEnvelope<ApiAuthData>>

    @POST("auth/otp/request")
    suspend fun postOtpRequest(@Body request: ApiOtpRequestBody): Response<ApiEnvelope<ApiOtpRequestData>>

    @POST("auth/otp/verify")
    suspend fun postOtpVerify(@Body request: ApiOtpVerifyBody): Response<ApiEnvelope<ApiAuthData>>

    @POST("auth/otp/cancel")
    suspend fun postOtpCancel(@Body request: ApiOtpCancelBody): Response<ApiEnvelope<ApiOtpCancelData>>

    @POST("auth/session/refresh")
    suspend fun postRefreshSession(@Body request: ApiRefreshSessionBody): Response<ApiEnvelope<ApiAuthData>>

    @POST("auth/logout")
    suspend fun postLogout(@Body request: ApiLogoutBody = ApiLogoutBody()): Response<ApiEnvelope<Map<String, String>>>

    @GET("auth/devices")
    suspend fun getTrustedDevices(): Response<ApiEnvelope<ApiDevicesListData>>

    @DELETE("auth/devices/{deviceId}")
    suspend fun revokeTrustedDevice(@Path("deviceId") deviceId: String): Response<ApiEnvelope<Map<String, String>>>

    @GET("auth/sessions")
    suspend fun getAuthSessions(): Response<ApiEnvelope<ApiSessionsListData>>

    @DELETE("auth/sessions/{sessionId}")
    suspend fun revokeAuthSession(@Path("sessionId") sessionId: String): Response<ApiEnvelope<Map<String, String>>>

    @POST("auth/change-phone")
    suspend fun postChangePhone(@Body request: ApiChangePhoneBody): Response<ApiEnvelope<ApiOtpRequestData>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiEnvelope<ApiAuthData>>

    @PATCH("auth/me")
    suspend fun updateMe(@Body request: ApiUpdateProfileBody): Response<ApiEnvelope<ApiAuthData>>

    @POST("devices/fcm-token")
    suspend fun registerFcmToken(@Body request: ApiFcmTokenBody): Response<ApiEnvelope<Map<String, String>>>

    // --- Cloud Backups ---
    @GET("backups")
    suspend fun listCloudBackups(): Response<ApiEnvelope<ApiCloudBackupsListData>>

    @POST("backups")
    suspend fun uploadCloudBackup(@Body request: ApiUploadBackupRequest): Response<ApiEnvelope<ApiUploadBackupResponse>>

    @GET("backups/{backupId}/download")
    suspend fun downloadCloudBackup(@Path("backupId") backupId: String): Response<ApiEnvelope<ApiCloudBackupItemDto>>

    @DELETE("backups/{backupId}")
    suspend fun deleteCloudBackup(@Path("backupId") backupId: String): Response<ApiEnvelope<Unit>>

    // --- Cross-Device Encrypted Transfer ---
    @POST("transfer/initiate")
    suspend fun initiateDeviceTransfer(@Body request: ApiTransferInitiateRequest): Response<ApiEnvelope<ApiTransferInitiateData>>

    @GET("transfer/pending")
    suspend fun getPendingDeviceTransfers(): Response<ApiEnvelope<ApiTransferPendingData>>

    @POST("transfer/{transferId}/approve")
    suspend fun approveDeviceTransfer(
        @Path("transferId") transferId: String,
        @Body request: ApiTransferApproveRequest
    ): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("transfer/{transferId}/chunks")
    suspend fun uploadTransferChunk(
        @Path("transferId") transferId: String,
        @Body request: ApiTransferUploadChunkRequest
    ): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("transfer/{transferId}/finalize-upload")
    suspend fun finalizeTransferUpload(
        @Path("transferId") transferId: String,
        @Body request: ApiTransferFinalizeUploadRequest
    ): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @GET("transfer/{transferId}/status")
    suspend fun getDeviceTransferStatus(@Path("transferId") transferId: String): Response<ApiEnvelope<ApiTransferStatusData>>

    @GET("transfer/{transferId}/chunks/{chunkIndex}")
    suspend fun downloadTransferChunk(
        @Path("transferId") transferId: String,
        @Path("chunkIndex") chunkIndex: Int
    ): Response<ApiEnvelope<ApiTransferDownloadChunkData>>

    @POST("transfer/{transferId}/complete")
    suspend fun completeDeviceTransfer(@Path("transferId") transferId: String): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("transfer/{transferId}/cancel")
    suspend fun cancelDeviceTransfer(
        @Path("transferId") transferId: String,
        @Body request: ApiTransferCancelRequest = ApiTransferCancelRequest()
    ): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("transfer/revoke-other-devices")
    suspend fun revokeOtherDevices(): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    // --- Calls, Budget & Signaling TURN ---
    @POST("calls/authorize")
    suspend fun authorizeCall(@Body request: ApiCallAuthorizeRequest): Response<ApiEnvelope<ApiCallAuthorizeData>>

    @GET("calls/status")
    suspend fun getCallBudgetStatus(): Response<ApiEnvelope<ApiCallBudgetData>>

    @GET("calls/budget")
    suspend fun getAdminCallBudget(): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @GET("calls/history")
    suspend fun getCallHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST
    suspend fun fetchTurnCredentials(
        @retrofit2.http.Url url: String = "https://signaling.oozox.com/turn-credentials",
        @Body request: ApiTurnCredentialsRequest = ApiTurnCredentialsRequest()
    ): Response<ApiTurnCredentialsResponse>

    // --- Workspace Cloud Sync ---
    @GET("groups")
    suspend fun getGroups(): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("groups")
    suspend fun createGroup(@Body request: ApiWorkspaceCreateGroupRequest): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @GET("projects")
    suspend fun getProjects(): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("projects")
    suspend fun createProject(@Body request: ApiWorkspaceCreateProjectRequest): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @PATCH("projects/{projectId}")
    suspend fun updateProject(
        @Path("projectId") projectId: String,
        @Body body: Map<String, kotlinx.serialization.json.JsonElement>
    ): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @GET("chats")
    suspend fun getChats(): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("chats")
    suspend fun createChat(@Body request: ApiWorkspaceCreateChatRequest): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @GET("chats/{conversationId}/messages")
    suspend fun getChatMessages(
        @Path("conversationId") conversationId: String,
        @Query("limit") limit: Int = 50
    ): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("chats/{conversationId}/messages")
    suspend fun sendChatMessage(
        @Path("conversationId") conversationId: String,
        @Body request: ApiWorkspaceSendMessageRequest
    ): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("chats/{conversationId}/read")
    suspend fun markChatReadRemote(@Path("conversationId") conversationId: String): Response<ApiEnvelope<Unit>>

    @POST("invoices")
    suspend fun createInvoice(@Body request: ApiCreateInvoiceRequest): Response<ApiEnvelope<ApiInvoiceData>>

    @GET("invoices")
    suspend fun getInvoices(
        @Query("role") role: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null
    ): Response<ApiEnvelope<ApiInvoiceListData>>

    @PATCH("invoices/{id}/status")
    suspend fun updateInvoiceStatus(
        @Path("id") invoiceId: String,
        @Body body: ApiUpdateInvoiceStatusRequest
    ): Response<ApiEnvelope<ApiInvoiceData>>

    @DELETE("invoices/{id}")
    suspend fun deleteInvoice(@Path("id") invoiceId: String): Response<ApiEnvelope<Unit>>

    @GET("notifications")
    suspend fun getNotifications(): Response<ApiEnvelope<ApiNotificationsData>>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<ApiEnvelope<Unit>>

    @GET("business/profile")
    suspend fun getBusinessProfile(): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @GET("business/summary")
    suspend fun getBusinessSummary(): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @retrofit2.http.PUT("business/profile")
    suspend fun upsertBusinessProfile(@Body request: ApiBusinessProfileRequest): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @GET("hr/employees")
    suspend fun getEmployees(): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("hr/employees")
    suspend fun createEmployee(@Body request: ApiCreateEmployeeRequest): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @PATCH("hr/employees/{id}")
    suspend fun updateEmployee(
        @Path("id") employeeId: String,
        @Body request: ApiUpdateEmployeeRequest
    ): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @DELETE("hr/employees/{id}")
    suspend fun deleteEmployee(@Path("id") employeeId: String): Response<ApiEnvelope<Unit>>

    @POST("hr/employee-login")
    suspend fun employeeLogin(@Body request: ApiEmployeeLoginRequest): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @GET("hr/payroll")
    suspend fun getPayrollAdjustments(): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("hr/payroll")
    suspend fun addPayrollAdjustment(@Body request: ApiPayrollAdjustmentRequest): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @DELETE("hr/payroll/{id}")
    suspend fun deletePayrollAdjustment(@Path("id") adjustmentId: String): Response<ApiEnvelope<Unit>>

    @GET("hr/leaves")
    suspend fun getEmployeeLeaves(): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("hr/leaves")
    suspend fun addEmployeeLeave(@Body request: ApiEmployeeLeaveRequest): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @DELETE("hr/leaves/{id}")
    suspend fun deleteEmployeeLeave(@Path("id") leaveId: String): Response<ApiEnvelope<Unit>>

    @GET("hr/attendance")
    suspend fun getAttendanceRecords(): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("hr/attendance/check-in")
    suspend fun checkInAttendance(@Body request: ApiAttendanceGeoRequest): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("hr/attendance/heartbeat")
    suspend fun sendAttendanceHeartbeat(@Body request: ApiAttendanceGeoRequest): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @GET("tasks")
    suspend fun getWorkTasks(
        @Query("projectId") projectId: String? = null,
        @Query("workGroupId") workGroupId: String? = null
    ): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @POST("tasks")
    suspend fun createWorkTask(@Body request: ApiCreateTaskRequest): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>

    @PATCH("tasks/{id}/status")
    suspend fun updateWorkTaskStatus(
        @Path("id") taskId: String,
        @Body request: ApiUpdateTaskStatusRequest
    ): Response<ApiEnvelope<Map<String, kotlinx.serialization.json.JsonElement>>>
}
