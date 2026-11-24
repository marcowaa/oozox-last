package com.example.util

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitClient {

    @Volatile
    var baseUrl: String = "https://api.oozox.com/api/"
        private set

    @Volatile
    private var authToken: String? = null

    @Volatile
    var activeLanguage: String = "ar"
        private set

    @Volatile
    var enableNetworkBodyLogs: Boolean = false

    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    private var cachedRetrofit: Retrofit? = null

    private val lock = Any()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun buildLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            redactHeader("Cookie")
            level = if (enableNetworkBodyLogs) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Volatile
    private var deviceIdHeader: String? = null

    @Volatile
    private var deviceNameHeader: String? = null

    @Volatile
    private var deviceFingerprintHeader: String? = null

    @Volatile
    var onUnauthorizedSession: (() -> Unit)? = null

    fun setAuthToken(token: String?) {
        authToken = token?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun setDeviceHeaders(deviceId: String?, deviceName: String?, fingerprint: String?) {
        deviceIdHeader = deviceId?.trim()?.takeIf { it.isNotEmpty() }
        deviceNameHeader = deviceName?.trim()?.takeIf { it.isNotEmpty() }
        deviceFingerprintHeader = fingerprint?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun setActiveLanguage(langCode: String?) {
        val cleaned = langCode?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: "ar"
        activeLanguage = cleaned
    }

    fun getAuthToken(): String? = authToken

    fun hasActiveSession(): Boolean = !authToken.isNullOrBlank()

    fun configureBaseUrl(newBaseUrl: String) {
        val normalized = if (newBaseUrl.endsWith("/")) newBaseUrl else "$newBaseUrl/"
        synchronized(lock) {
            baseUrl = normalized
            cachedRetrofit = null
            cachedBaseUrl = null
        }
    }

    val okHttpClient: OkHttpClient
        get() = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val lang = activeLanguage
                val requestBuilder = originalRequest.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")

                if (originalRequest.header("Accept-Language").isNullOrBlank()) {
                    requestBuilder.header("Accept-Language", lang)
                }
                if (originalRequest.header("X-App-Language").isNullOrBlank()) {
                    requestBuilder.header("X-App-Language", lang)
                }
                deviceIdHeader?.let { devId ->
                    if (originalRequest.header("X-Device-Id").isNullOrBlank()) {
                        requestBuilder.header("X-Device-Id", devId)
                    }
                }
                deviceNameHeader?.let { devName ->
                    if (originalRequest.header("X-Device-Name").isNullOrBlank()) {
                        requestBuilder.header("X-Device-Name", devName)
                    }
                }
                if (originalRequest.header("X-Platform").isNullOrBlank()) {
                    requestBuilder.header("X-Platform", "ANDROID")
                }
                deviceFingerprintHeader?.let { fp ->
                    if (originalRequest.header("X-Device-Fingerprint").isNullOrBlank()) {
                        requestBuilder.header("X-Device-Fingerprint", fp)
                    }
                }

                val token = authToken
                if (!token.isNullOrBlank() && originalRequest.header("Authorization").isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                val response = chain.proceed(requestBuilder.build())
                if (response.code == 401 && !token.isNullOrBlank() && !originalRequest.url.encodedPath.contains("/auth/")) {
                    onUnauthorizedSession?.invoke()
                }
                response
            }
            .addInterceptor(buildLoggingInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    val retrofit: Retrofit
        get() {
            val current = cachedRetrofit
            if (current != null && cachedBaseUrl == baseUrl) {
                return current
            }
            return synchronized(lock) {
                val again = cachedRetrofit
                if (again != null && cachedBaseUrl == baseUrl) {
                    again
                } else {
                    val contentType = "application/json".toMediaType()
                    val created = Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .client(okHttpClient)
                        .addConverterFactory(json.asConverterFactory(contentType))
                        .build()
                    cachedBaseUrl = baseUrl
                    cachedRetrofit = created
                    created
                }
            }
        }

    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }

    fun apiService(): OozoxApiService = createService(OozoxApiService::class.java)
}
