package com.example.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
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
    val avatar: String = ""
)

@Serializable
data class ApiAuthData(
    val user: ApiUserDto? = null,
    val token: String = ""
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

interface OozoxApiService {
    @GET("health")
    suspend fun getHealth(): Response<ApiEnvelope<ApiHealthData>>

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
}
