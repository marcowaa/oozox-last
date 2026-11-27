package com.example.data.repository

import android.content.Context
import com.example.util.ApiAuthData
import com.example.util.ApiDeviceTrustDto
import com.example.util.ApiFcmTokenBody
import com.example.util.ApiLogoutBody
import com.example.util.ApiOtpCancelBody
import com.example.util.ApiOtpChallengeDto
import com.example.util.ApiOtpDeliveryErrorDto
import com.example.util.ApiOtpRequestBody
import com.example.util.ApiOtpRequestData
import com.example.util.ApiOtpVerifyBody
import com.example.util.ApiRefreshSessionBody
import com.example.util.ApiTrustedDeviceDto
import com.example.util.ApiTrustedLoginBody
import com.example.util.ApiUpdateProfileBody
import com.example.util.ApiUserDto
import com.example.util.AppErrorMapper
import com.example.util.DeviceTrustManager
import com.example.util.OozoxFirebaseMessagingService
import com.example.util.RetrofitClient
import com.example.util.SecureCredentialVault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed class AuthOutcome {
    data class Authenticated(
        val user: ApiUserDto,
        val token: String,
        val refreshToken: String,
        val sessionId: String,
        val deviceTrust: ApiDeviceTrustDto? = null,
        val message: String = "",
        val isExistingUser: Boolean = true,
        val hasOtherActiveDevice: Boolean = false
    ) : AuthOutcome()

    data class OtpRequired(
        val purpose: String,
        val preAuthToken: String? = null,
        val challenge: ApiOtpChallengeDto? = null,
        val maskedPhone: String? = null,
        val deliveryError: ApiOtpDeliveryErrorDto? = null,
        val message: String = ""
    ) : AuthOutcome()

    data class Error(
        val code: String,
        val message: String,
        val retryAfterSeconds: Int? = null,
        val remainingAttempts: Int? = null
    ) : AuthOutcome()
}

sealed class OtpRequestOutcome {
    data class Sent(
        val data: ApiOtpRequestData,
        val message: String
    ) : OtpRequestOutcome()

    data class Error(
        val code: String,
        val message: String,
        val retryAfterSeconds: Int? = null
    ) : OtpRequestOutcome()
}

sealed class OtpVerifyOutcome {
    data class Verified(
        val data: ApiAuthData,
        val message: String
    ) : OtpVerifyOutcome()

    data class Error(
        val code: String,
        val message: String,
        val retryAfterSeconds: Int? = null,
        val remainingAttempts: Int? = null
    ) : OtpVerifyOutcome()
}

data class ParsedApiError(
    val code: String,
    val message: String,
    val retryAfterSeconds: Int? = null,
    val remainingAttempts: Int? = null,
    val requiresOtp: Boolean = false,
    val requiresStepUpOtp: Boolean = false,
    val preAuthToken: String? = null,
    val maskedPhone: String? = null,
    val reason: String? = null
)

object AuthRepository {

    fun parseErrorBody(
        rawBody: String?,
        fallbackAr: String,
        fallbackEn: String = "Request failed. Please try again",
        statusCode: Int? = null
    ): ParsedApiError {
        if (rawBody.isNullOrBlank()) {
            val friendly = AppErrorMapper.fromApiError(
                statusCode = statusCode,
                fallbackAr = fallbackAr,
                fallbackEn = fallbackEn
            )
            return ParsedApiError(code = "HTTP_ERROR", message = friendly)
        }
        return try {
            val obj = JSONObject(rawBody)
            val rawMsg = obj.optString("message").takeIf { it.isNotBlank() }
            val code = obj.optString("code").takeIf { it.isNotBlank() } ?: "HTTP_ERROR"
            val retry = if (obj.has("retryAfterSeconds") && !obj.isNull("retryAfterSeconds")) obj.optInt("retryAfterSeconds") else null
            val remaining = if (obj.has("remainingAttempts") && !obj.isNull("remainingAttempts")) obj.optInt("remainingAttempts") else null
            val friendlyMsg = AppErrorMapper.fromApiError(
                code = code,
                serverMessage = rawMsg,
                statusCode = statusCode,
                retryAfterSeconds = retry,
                remainingAttempts = remaining,
                fallbackAr = fallbackAr,
                fallbackEn = fallbackEn
            )
            ParsedApiError(
                code = code,
                message = friendlyMsg,
                retryAfterSeconds = retry,
                remainingAttempts = remaining,
                requiresOtp = obj.optBoolean("requiresOtp", false),
                requiresStepUpOtp = obj.optBoolean("requiresStepUpOtp", false),
                preAuthToken = obj.optString("preAuthToken").takeIf { it.isNotBlank() },
                maskedPhone = obj.optString("maskedPhone").takeIf { it.isNotBlank() },
                reason = obj.optString("reason").takeIf { it.isNotBlank() }
            )
        } catch (_: Exception) {
            ParsedApiError(
                code = "HTTP_ERROR",
                message = AppErrorMapper.fromApiError(
                    statusCode = statusCode,
                    fallbackAr = fallbackAr,
                    fallbackEn = fallbackEn
                )
            )
        }
    }

    fun restoreSessionOnStartup(context: Context) {
        DeviceTrustManager.syncRetrofitDeviceHeaders(context)
        val savedAccessToken = SecureCredentialVault.getAccessToken(context)
        if (!savedAccessToken.isNullOrBlank()) {
            RetrofitClient.setAuthToken(savedAccessToken)
        }
    }

    fun applyAuthenticatedSession(context: Context, data: ApiAuthData) {
        if (data.token.isNotBlank()) {
            SecureCredentialVault.saveSessionTokens(
                context = context,
                accessToken = data.token,
                refreshToken = data.refreshToken.takeIf { it.isNotBlank() },
                sessionId = data.sessionId.takeIf { it.isNotBlank() }
            )
            RetrofitClient.setAuthToken(data.token)
        }

        val trust = data.trustedDevice ?: data.deviceTrust
        val user = data.user
        if (trust != null && trust.deviceTrustToken.isNotBlank() && user != null) {
            val phone = user.phoneE164.ifBlank { user.phone }
            val userId = user.id.ifBlank { user.mongoId }
            DeviceTrustManager.storeServerDeviceTrust(
                context = context,
                phone = phone,
                userId = userId,
                deviceTrustToken = trust.deviceTrustToken,
                trustedUntilIso = trust.trustedUntil
            )
        } else {
            DeviceTrustManager.syncRetrofitDeviceHeaders(context)
        }
    }

    suspend fun requestOtp(
        context: Context,
        purpose: String = "LOGIN",
        channel: String = "AUTO",
        phone: String? = null,
        newPhone: String? = null,
        preAuthToken: String? = null,
        allowFallback: Boolean = true,
        portalScope: String? = null
    ): OtpRequestOutcome = withContext(Dispatchers.IO) {
        try {
            DeviceTrustManager.syncRetrofitDeviceHeaders(context)
            val deviceId = SecureCredentialVault.getOrCreateDeviceId(context)
            val fp = DeviceTrustManager.getCurrentDeviceFingerprint(context)
            val resp = RetrofitClient.apiService().postOtpRequest(
                ApiOtpRequestBody(
                    purpose = purpose,
                    channel = channel,
                    allowFallback = allowFallback,
                    phone = phone?.trim(),
                    newPhone = newPhone?.trim(),
                    preAuthToken = preAuthToken,
                    portalScope = portalScope,
                    deviceId = deviceId,
                    deviceName = DeviceTrustManager.getDeviceDisplayName(),
                    platform = "ANDROID",
                    hardwareFingerprint = fp
                )
            )
            if (!resp.isSuccessful) {
                val err = parseErrorBody(
                    rawBody = resp.errorBody()?.string(),
                    fallbackAr = "تعذر إرسال رمز التحقق، حاول مجدداً",
                    fallbackEn = "Couldn't send code. Please try again",
                    statusCode = resp.code()
                )
                return@withContext OtpRequestOutcome.Error(err.code, err.message, err.retryAfterSeconds)
            }
            val body = resp.body()
            val data = body?.data
            if (body?.success != true || data == null) {
                return@withContext OtpRequestOutcome.Error(
                    "OTP_REQUEST_FAILED",
                    AppErrorMapper.sanitizeMessage(
                        body?.message,
                        fallbackAr = "تعذر إرسال رمز التحقق",
                        fallbackEn = "Couldn't send verification code"
                    )
                )
            }
            OtpRequestOutcome.Sent(data = data, message = body.message)
        } catch (e: Exception) {
            OtpRequestOutcome.Error(
                "NETWORK_ERROR",
                AppErrorMapper.fromThrowable(
                    e,
                    fallbackAr = "تعذر الاتصال بالخادم، تحقق من الإنترنت",
                    fallbackEn = "No connection. Check your internet"
                )
            )
        }
    }

    suspend fun verifyOtp(
        context: Context,
        challengeId: String,
        code: String,
        phone: String? = null,
        preAuthToken: String? = null,
        trustDevice: Boolean = true,
        portalScope: String? = null
    ): OtpVerifyOutcome = withContext(Dispatchers.IO) {
        try {
            DeviceTrustManager.syncRetrofitDeviceHeaders(context)
            val deviceId = SecureCredentialVault.getOrCreateDeviceId(context)
            val fp = DeviceTrustManager.getCurrentDeviceFingerprint(context)
            val savedFcm = OozoxFirebaseMessagingService.getSavedToken(context)
                ?.takeIf { it.isNotBlank() && !it.startsWith("oozox_device_") }
            val resp = RetrofitClient.apiService().postOtpVerify(
                ApiOtpVerifyBody(
                    challengeId = challengeId,
                    code = code.trim(),
                    phone = phone?.trim(),
                    preAuthToken = preAuthToken,
                    portalScope = portalScope,
                    trustDevice = trustDevice,
                    deviceId = deviceId,
                    deviceName = DeviceTrustManager.getDeviceDisplayName(),
                    platform = "ANDROID",
                    hardwareFingerprint = fp,
                    fcmToken = savedFcm
                )
            )
            if (!resp.isSuccessful) {
                val err = parseErrorBody(
                    rawBody = resp.errorBody()?.string(),
                    fallbackAr = "رمز التحقق غير صحيح",
                    fallbackEn = "Incorrect verification code",
                    statusCode = resp.code()
                )
                return@withContext OtpVerifyOutcome.Error(
                    code = err.code,
                    message = err.message,
                    retryAfterSeconds = err.retryAfterSeconds,
                    remainingAttempts = err.remainingAttempts
                )
            }
            val body = resp.body()
            val data = body?.data
            if (body?.success != true || data == null) {
                return@withContext OtpVerifyOutcome.Error(
                    "OTP_VERIFY_FAILED",
                    AppErrorMapper.sanitizeMessage(
                        body?.message,
                        fallbackAr = "تعذر التحقق من الرمز",
                        fallbackEn = "Couldn't verify code"
                    )
                )
            }
            if (data.token.isNotBlank()) {
                applyAuthenticatedSession(context, data)
                if (!savedFcm.isNullOrBlank()) {
                    try {
                        RetrofitClient.apiService().registerFcmToken(
                            ApiFcmTokenBody(fcmToken = savedFcm, deviceId = deviceId, platform = "ANDROID")
                        )
                    } catch (_: Exception) {}
                }
            }
            OtpVerifyOutcome.Verified(data = data, message = body.message)
        } catch (e: Exception) {
            OtpVerifyOutcome.Error(
                "NETWORK_ERROR",
                AppErrorMapper.fromThrowable(
                    e,
                    fallbackAr = "تعذر الاتصال بالخادم، تحقق من الإنترنت",
                    fallbackEn = "No connection. Check your internet"
                )
            )
        }
    }

    suspend fun cancelOtp(
        context: Context,
        challengeId: String,
        phone: String? = null,
        preAuthToken: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (challengeId.isBlank()) return@withContext false
        try {
            DeviceTrustManager.syncRetrofitDeviceHeaders(context)
            val resp = RetrofitClient.apiService().postOtpCancel(
                ApiOtpCancelBody(
                    challengeId = challengeId,
                    phone = phone?.trim()?.takeIf { it.isNotEmpty() },
                    preAuthToken = preAuthToken?.takeIf { it.isNotBlank() }
                )
            )
            resp.isSuccessful && resp.body()?.success == true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun trustedDeviceLogin(
        context: Context,
        phoneOrIdentifier: String? = null
    ): AuthOutcome = withContext(Dispatchers.IO) {
        try {
            DeviceTrustManager.syncRetrofitDeviceHeaders(context)
            val deviceId = SecureCredentialVault.getOrCreateDeviceId(context)
            val trustToken = SecureCredentialVault.getServerDeviceTrustToken(context)
            val userId = SecureCredentialVault.getTrustedUserId(context)
            val fp = DeviceTrustManager.getCurrentDeviceFingerprint(context)

            if (trustToken.isNullOrBlank()) {
                return@withContext AuthOutcome.OtpRequired(
                    purpose = "DEVICE_TRUST",
                    message = "هذا الجهاز غير موثوق حالياً — مطلوب رمز تحقق OTP."
                )
            }

            val normalized = phoneOrIdentifier?.trim()?.takeIf { it.isNotEmpty() }
            val resp = RetrofitClient.apiService().postTrustedLogin(
                ApiTrustedLoginBody(
                    phone = normalized,
                    emailOrPhone = normalized,
                    userId = userId?.takeIf { it.isNotEmpty() },
                    deviceId = deviceId,
                    deviceTrustToken = trustToken,
                    hardwareFingerprint = fp
                )
            )
            if (!resp.isSuccessful) {
                val err = parseErrorBody(
                    rawBody = resp.errorBody()?.string(),
                    fallbackAr = "تعذر الدخول السريع، جرب رمز التحقق",
                    fallbackEn = "Quick sign-in unavailable. Use verification code",
                    statusCode = resp.code()
                )
                if (err.requiresOtp) {
                    DeviceTrustManager.clearServerDeviceTrust(context)
                    return@withContext AuthOutcome.OtpRequired(
                        purpose = "DEVICE_TRUST",
                        preAuthToken = err.preAuthToken,
                        maskedPhone = err.maskedPhone,
                        message = err.message
                    )
                }
                return@withContext AuthOutcome.Error(err.code, err.message, err.retryAfterSeconds, err.remainingAttempts)
            }
            val body = resp.body()
            val data = body?.data
            val user = data?.user
            if (body?.success != true || data == null || user == null) {
                return@withContext AuthOutcome.Error(
                    "TRUSTED_LOGIN_FAILED",
                    AppErrorMapper.sanitizeMessage(
                        body?.message,
                        fallbackAr = "تعذر تسجيل الدخول السريع",
                        fallbackEn = "Quick sign-in failed"
                    )
                )
            }
            applyAuthenticatedSession(context, data)
            AuthOutcome.Authenticated(
                user = user,
                token = data.token,
                refreshToken = data.refreshToken,
                sessionId = data.sessionId,
                deviceTrust = data.trustedDevice ?: data.deviceTrust,
                message = body.message,
                isExistingUser = data.isExistingUser,
                hasOtherActiveDevice = data.hasOtherActiveDevice
            )
        } catch (e: Exception) {
            AuthOutcome.Error(
                "NETWORK_ERROR",
                AppErrorMapper.fromThrowable(
                    e,
                    fallbackAr = "تعذر الاتصال بالخادم، تحقق من الإنترنت",
                    fallbackEn = "No connection. Check your internet"
                )
            )
        }
    }

    suspend fun refreshSession(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val refresh = SecureCredentialVault.getRefreshToken(context) ?: return@withContext false
            val deviceId = SecureCredentialVault.getOrCreateDeviceId(context)
            val resp = RetrofitClient.apiService().postRefreshSession(
                ApiRefreshSessionBody(refreshToken = refresh, deviceId = deviceId)
            )
            if (!resp.isSuccessful) return@withContext false
            val data = resp.body()?.data ?: return@withContext false
            if (data.token.isBlank()) return@withContext false
            applyAuthenticatedSession(context, data)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun registerFcmToken(context: Context, token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!RetrofitClient.hasActiveSession() || token.isBlank()) return@withContext false
            val deviceId = SecureCredentialVault.getOrCreateDeviceId(context)
            val resp = RetrofitClient.apiService().registerFcmToken(
                ApiFcmTokenBody(fcmToken = token, deviceId = deviceId, platform = "ANDROID")
            )
            resp.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateMyProfile(
        name: String? = null,
        email: String? = null,
        preferredLanguage: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!RetrofitClient.hasActiveSession()) return@withContext false
            val resp = RetrofitClient.apiService().updateMe(
                ApiUpdateProfileBody(
                    name = name?.trim()?.takeIf { it.isNotEmpty() },
                    email = email?.trim(),
                    preferredLanguage = preferredLanguage
                )
            )
            resp.isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun listTrustedDevices(context: Context): Result<List<ApiTrustedDeviceDto>> = withContext(Dispatchers.IO) {
        try {
            DeviceTrustManager.syncRetrofitDeviceHeaders(context)
            val resp = RetrofitClient.apiService().getTrustedDevices()
            if (!resp.isSuccessful) {
                val err = parseErrorBody(
                    rawBody = resp.errorBody()?.string(),
                    fallbackAr = "تعذر تحميل قائمة الأجهزة",
                    fallbackEn = "Couldn't load devices",
                    statusCode = resp.code()
                )
                return@withContext Result.failure(IllegalStateException(err.message))
            }
            Result.success(resp.body()?.data?.devices.orEmpty())
        } catch (e: Exception) {
            Result.failure(IllegalStateException(AppErrorMapper.fromThrowable(e)))
        }
    }

    suspend fun revokeTrustedDevice(context: Context, deviceId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = RetrofitClient.apiService().revokeTrustedDevice(deviceId)
            if (!resp.isSuccessful) {
                val err = parseErrorBody(
                    rawBody = resp.errorBody()?.string(),
                    fallbackAr = "تعذر إلغاء وثوق الجهاز",
                    fallbackEn = "Couldn't remove trusted device",
                    statusCode = resp.code()
                )
                return@withContext Result.failure(IllegalStateException(err.message))
            }
            val currentDeviceId = SecureCredentialVault.getOrCreateDeviceId(context)
            if (deviceId == currentDeviceId) {
                DeviceTrustManager.clearServerDeviceTrust(context)
                SecureCredentialVault.clearSessionOnly(context)
                RetrofitClient.setAuthToken(null)
            }
            Result.success(resp.body()?.message ?: "تم إلغاء وثوق الجهاز بنجاح")
        } catch (e: Exception) {
            Result.failure(IllegalStateException(AppErrorMapper.fromThrowable(e)))
        }
    }

    fun logout(context: Context, clearDeviceTrust: Boolean = false) {
        if (RetrofitClient.hasActiveSession()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    RetrofitClient.apiService().postLogout(ApiLogoutBody(revokeDeviceTrust = clearDeviceTrust))
                } catch (_: Exception) {
                } finally {
                    RetrofitClient.setAuthToken(null)
                }
            }
        } else {
            RetrofitClient.setAuthToken(null)
        }
        if (clearDeviceTrust) {
            SecureCredentialVault.clearAll(context)
        } else {
            SecureCredentialVault.clearSessionOnly(context)
        }
    }
}
