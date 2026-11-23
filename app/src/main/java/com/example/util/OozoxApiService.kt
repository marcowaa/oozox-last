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
data class ApiRegisterRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: String = "USER"
)

@Serializable
data class ApiLoginRequest(
    val emailOrPhone: String,
    val password: String
)

@Serializable
data class ApiUserDto(
    val id: String = "",
    @SerialName("_id") val mongoId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "USER",
    val avatar: String = "",
    val preferredLanguage: String = "ar"
)

@Serializable
data class ApiAuthData(
    val user: ApiUserDto? = null,
    val token: String = ""
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

    @POST("auth/register")
    suspend fun register(@Body request: ApiRegisterRequest): Response<ApiEnvelope<ApiAuthData>>

    @POST("auth/login")
    suspend fun login(@Body request: ApiLoginRequest): Response<ApiEnvelope<ApiAuthData>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiEnvelope<ApiAuthData>>

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
