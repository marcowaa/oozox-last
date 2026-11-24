package com.example.data.repository

import android.content.Context
import com.example.util.ApiAuthData
import com.example.util.ApiChangePasswordBody
import com.example.util.ApiChangePhoneBody
import com.example.util.ApiDeviceTrustDto
import com.example.util.ApiLoginRequest
import com.example.util.ApiOtpChallengeDto
import com.example.util.ApiOtpDeliveryErrorDto
import com.example.util.ApiOtpRequestBody
import com.example.util.ApiOtpRequestData
import com.example.util.ApiOtpVerifyBody
import com.example.util.ApiRecoverAccountBody
import com.example.util.ApiRefreshSessionBody
import com.example.util.ApiRegisterRequest
import com.example.util.ApiTrustedDeviceDto
import com.example.util.ApiTrustedLoginBody
import com.example.util.ApiUserDto
import com.example.util.DeviceTrustManager
import com.example.util.RetrofitClient
import com.example.util.SecureCredentialVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed class AuthOutcome {
    data class Authenticated(
        val user: ApiUserDto,
        val token: String,
        val refreshToken: String,
        val sessionId: String,
        val deviceTrust: ApiDeviceTrustDto? = null,
        val message: String = ""
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

sealed class PasswordChangeOutcome {
    data class Changed(
        val token: String,
        val refreshToken: String,
        val sessionId: String,
        val message: String
    ) : PasswordChangeOutcome()

    data class StepUpRequired(
        val maskedPhone: String?,
        val message: String
    ) : PasswordChangeOutcome()

    data class Error(
        val code: String,
        val message: String
    ) : PasswordChangeOutcome()
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

    fun parseErrorBody(rawBody: String?, fallbackMessage: String): ParsedApiError {
        if (rawBody.isNullOrBlank()) {
            return ParsedApiError(code = "HTTP_ERROR", message = fallbackMessage)
        }
        return try {
            val obj = JSONObject(rawBody)
            val msg = obj.optString("message").takeIf { it.isNotBlank() } ?: fallbackMessage
            val code = obj.optString("code").takeIf { it.isNotBlank() } ?: "HTTP_ERROR"
            val retry = if (obj.has("retryAfterSeconds") && !obj.isNull("retryAfterSeconds")) obj.optInt("retryAfterSeconds") else null
            val remaining = if (obj.has("remainingAttempts") && !obj.isNull("remainingAttempts")) obj.optInt("remainingAttempts") else null
            ParsedApiError(
                code = code,
                message = msg,
                retryAfterSeconds = retry,
                remainingAttempts = remaining,
                requiresOtp = obj.optBoolean("requiresOtp", false),
                requiresStepUpOtp = obj.optBoolean("requiresStepUpOtp", false),
                preAuthToken = obj.optString("preAuthToken").takeIf { it.isNotBlank() },
                maskedPhone = obj.optString("maskedPhone").takeIf { it.isNotBlank() },
                reason = obj.optString("reason").takeIf { it.isNotBlank() }
            )
        } catch (_: Exception) {
            ParsedApiError(code = "HTTP_ERROR", message = fallbackMessage)
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

    suspend fun registerAccount(
        context: Context,
        name: String,
        email: String,
        phone: String,
        password: String,
        role: String = "USER",
        preferredLanguage: String = "ar",
        preferredChannel: String = "AUTO"
    ): AuthOutcome = withContext(Dispatchers.IO) {
        try {
            DeviceTrustManager.syncRetrofitDeviceHeaders(context)
            val deviceId = SecureCredentialVault.getOrCreateDeviceId(context)
            val resp = RetrofitClient.apiService().register(
                ApiRegisterRequest(
                    name = name.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    password = password,
                    role = role,
                    preferredLanguage = preferredLanguage,
                    preferredChannel = preferredChannel,
                    autoSendOtp = true,
                    deviceId = deviceId
                )
            )
            if (!resp.isSuccessful) {
                val err = parseErrorBody(resp.errorBody()?.string(), "تعذر إتمام التسجيل (${resp.code()})")
                return@withContext AuthOutcome.Error(err.code, err.message, err.retryAfterSeconds, err.remainingAttempts)
            }
            val body = resp.body()
            val data = body?.data
            if (body?.success != true || data == null) {
                return@withContext AuthOutcome.Error("REGISTER_FAILED", body?.message ?: "فشل إنشاء الحساب")
            }
            AuthOutcome.OtpRequired(
                purpose = data.purpose ?: "REGISTER",
                preAuthToken = data.preAuthToken,
                challenge = data.otpChallenge,
                maskedPhone = data.maskedPhone,
                deliveryError = data.otpDeliveryError,
                message = body.message
            )
        } catch (e: Exception) {
            AuthOutcome.Error("NETWORK_ERROR", e.message ?: "تعذر الاتصال بالسيرفر")
        }
    }

    suspend fun loginWithPassword(
        context: Context,
        emailOrPhone: String,
        password: String,
        preferredChannel: String = "AUTO",
        autoSendOtp: Boolean = true
    ): AuthOutcome = withContext(Dispatchers.IO) {
        try {
            DeviceTrustManager.syncRetrofitDeviceHeaders(context)
            val deviceId = SecureCredentialVault.getOrCreateDeviceId(context)
            val trustToken = SecureCredentialVault.getServerDeviceTrustToken(context)
            val fp = DeviceTrustManager.getCurrentDeviceFingerprint(context)

            val resp = RetrofitClient.apiService().login(
                ApiLoginRequest(
                    emailOrPhone = emailOrPhone.trim(),
                    password = password,
                    deviceId = deviceId,
                    deviceTrustToken = trustToken,
                    hardwareFingerprint = fp,
                    preferredChannel = preferredChannel,
                    autoSendOtp = autoSendOtp
                )
            )
            if (!resp.isSuccessful) {
                val err = parseErrorBody(resp.errorBody()?.string(), "بيانات الدخول غير صحيحة (${resp.code()})")
                return@withContext AuthOutcome.Error(err.code, err.message, err.retryAfterSeconds, err.remainingAttempts)
            }
            val body = resp.body()
            val data = body?.data
            if (body?.success != true || data == null) {
                return@withContext AuthOutcome.Error("LOGIN_FAILED", body?.message ?: "تعذر تسجيل الدخول")
            }
            if (data.requiresOtp) {
                return@withContext AuthOutcome.OtpRequired(
                    purpose = data.purpose ?: "LOGIN",
                    preAuthToken = data.preAuthToken,
                    challenge = data.otpChallenge,
                    maskedPhone = data.maskedPhone,
                    deliveryError = data.otpDeliveryError,
                    message = body.message
                )
            }
            val user = data.user ?: return@withContext AuthOutcome.Error("INVALID_RESPONSE", "بيانات المستخدم مفقودة")
            applyAuthenticatedSession(context, data)
            AuthOutcome.Authenticated(
                user = user,
                token = data.token,
                refreshToken = data.refreshToken,
                sessionId = data.sessionId,
                deviceTrust = data.trustedDevice ?: data.deviceTrust,
                message = body.message
            )
        } catch (e: Exception) {
            AuthOutcome.Error("NETWORK_ERROR", e.message ?: "تعذر الاتصال بالسيرفر")
        }
    }

    suspend fun requestOtp(
        context: Context,
        purpose: String,
        channel: String = "AUTO",
        phone: String? = null,
        newPhone: String? = null,
        preAuthToken: String? = null,
        allowFallback: Boolean = true
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
                    deviceId = deviceId,
                    deviceName = DeviceTrustManager.getDeviceDisplayName(),
                    platform = "ANDROID",
                    hardwareFingerprint = fp
                )
            )
            if (!resp.isSuccessful) {
                val err = parseErrorBody(resp.errorBody()?.string(), "تعذر إرسال رمز التحقق (${resp.code()})")
                return@withContext OtpRequestOutcome.Error(err.code, err.message, err.retryAfterSeconds)
            }
            val body = resp.body()
            val data = body?.data
            if (body?.success != true || data == null) {
                return@withContext OtpRequestOutcome.Error("OTP_REQUEST_FAILED", body?.message ?: "فشل إرسال الرمز")
            }
            OtpRequestOutcome.Sent(data = data, message = body.message)
        } catch (e: Exception) {
            OtpRequestOutcome.Error("NETWORK_ERROR", e.message ?: "تعذر الاتصال بسيرفر التحقق")
        }
    }

    suspend fun verifyOtp(
        context: Context,
        challengeId: String,
        code: String,
        phone: String? = null,
        preAuthToken: String? = null,
        trustDevice: Boolean = true
    ): OtpVerifyOutcome = withContext(Dispatchers.IO) {
        try {
            DeviceTrustManager.syncRetrofitDeviceHeaders(context)
            val deviceId = SecureCredentialVault.getOrCreateDeviceId(context)
            val fp = DeviceTrustManager.getCurrentDeviceFingerprint(context)
            val resp = RetrofitClient.apiService().postOtpVerify(
                ApiOtpVerifyBody(
                    challengeId = challengeId,
                    code = code.trim(),
                    phone = phone?.trim(),
                    preAuthToken = preAuthToken,
                    trustDevice = trustDevice,
                    deviceId = deviceId,
                    deviceName = DeviceTrustManager.getDeviceDisplayName(),
                    platform = "ANDROID",
                    hardwareFingerprint = fp
                )
            )
            if (!resp.isSuccessful) {
                val err = parseErrorBody(resp.errorBody()?.string(), "رمز التحقق غير صحيح (${resp.code()})")
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
                return@withContext OtpVerifyOutcome.Error("OTP_VERIFY_FAILED", body?.message ?: "فشل التحقق من الرمز")
            }
            if (data.token.isNotBlank()) {
                applyAuthenticatedSession(context, data)
            }
            OtpVerifyOutcome.Verified(data = data, message = body.message)
        } catch (e: Exception) {
            OtpVerifyOutcome.Error("NETWORK_ERROR", e.message ?: "تعذر الاتصال بالسيرفر")
        }
    }

    suspend fun trustedDeviceLogin(
        context: Context,
        emailOrPhone: String? = null
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

            val resp = RetrofitClient.apiService().postTrustedLogin(
                ApiTrustedLoginBody(
                    emailOrPhone = emailOrPhone?.trim()?.takeIf { it.isNotEmpty() },
                    userId = userId?.takeIf { it.isNotEmpty() },
                    deviceId = deviceId,
                    deviceTrustToken = trustToken,
                    hardwareFingerprint = fp
                )
            )
            if (!resp.isSuccessful) {
                val err = parseErrorBody(resp.errorBody()?.string(), "تعذر تسجيل الدخول بالجهاز الموثوق (${resp.code()})")
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
                return@withContext AuthOutcome.Error("TRUSTED_LOGIN_FAILED", body?.message ?: "فشل تسجيل الدخول السريع")
            }
            applyAuthenticatedSession(context, data)
            AuthOutcome.Authenticated(
                user = user,
                token = data.token,
                refreshToken = data.refreshToken,
                sessionId = data.sessionId,
                deviceTrust = data.trustedDevice ?: data.deviceTrust,
                message = body.message
            )
        } catch (e: Exception) {
            AuthOutcome.Error("NETWORK_ERROR", e.message ?: "تعذر الاتصال بالسيرفر")
        }
    }

    suspend fun requestRecoveryOtp(
        context: Context,
        phone: String,
        channel: String = "AUTO"
    ): OtpRequestOutcome = withContext(Dispatchers.IO) {
        try {
            DeviceTrustManager.syncRetrofitDeviceHeaders(context)
            val deviceId = SecureCredentialVault.getOrCreateDeviceId(context)
            val fp = DeviceTrustManager.getCurrentDeviceFingerprint(context)
            val resp = RetrofitClient.apiService().postRecoverAccount(
                ApiRecoverAccountBody(
                    phone = phone.trim(),
                    channel = channel,
                    deviceId = deviceId,
                    deviceName = DeviceTrustManager.getDeviceDisplayName(),
                    platform = "ANDROID",
                    hardwareFingerprint = fp
                )
            )
            if (!resp.isSuccessful) {
                val err = parseErrorBody(resp.errorBody()?.string(), "تعذر إرسال رمز الاسترداد (${resp.code()})")
                return@withContext OtpRequestOutcome.Error(err.code, err.message, err.retryAfterSeconds)
            }
            val body = resp.body()
            val data = body?.data
            if (body?.success != true || data == null) {
                return@withContext OtpRequestOutcome.Error("RECOVER_FAILED", body?.message ?: "تعذر بدء استعادة الحساب")
            }
            val requestData = ApiOtpRequestData(
                challengeId = data.challengeId ?: data.otpChallenge?.challengeId.orEmpty(),
                activeChannel = data.activeChannel ?: data.otpChallenge?.activeChannel ?: "TELEGRAM",
                attemptedChannels = data.attemptedChannels,
                expiresAt = data.expiresAt.ifBlank { data.otpChallenge?.expiresAt.orEmpty() },
                cooldownSeconds = data.cooldownSeconds ?: data.otpChallenge?.cooldownSeconds ?: 60,
                maxAttempts = data.maxAttempts ?: data.otpChallenge?.maxAttempts ?: 5,
                maskedPhone = data.maskedPhone ?: data.otpChallenge?.maskedPhone.orEmpty(),
                preAuthToken = data.preAuthToken
            )
            OtpRequestOutcome.Sent(data = requestData, message = body.message)
        } catch (e: Exception) {
            OtpRequestOutcome.Error("NETWORK_ERROR", e.message ?: "تعذر الاتصال بالسيرفر")
        }
    }

    suspend fun confirmRecoveryWithOtp(
        context: Context,
        challengeId: String,
        code: String,
        newPassword: String
    ): AuthOutcome = withContext(Dispatchers.IO) {
        try {
            DeviceTrustManager.syncRetrofitDeviceHeaders(context)
            val deviceId = SecureCredentialVault.getOrCreateDeviceId(context)
            val fp = DeviceTrustManager.getCurrentDeviceFingerprint(context)
            val resp = RetrofitClient.apiService().postRecoverAccount(
                ApiRecoverAccountBody(
                    challengeId = challengeId,
                    code = code.trim(),
                    newPassword = newPassword,
                    deviceId = deviceId,
                    deviceName = DeviceTrustManager.getDeviceDisplayName(),
                    platform = "ANDROID",
                    hardwareFingerprint = fp
                )
            )
            if (!resp.isSuccessful) {
                val err = parseErrorBody(resp.errorBody()?.string(), "فشل استعادة الحساب (${resp.code()})")
                return@withContext AuthOutcome.Error(err.code, err.message, err.retryAfterSeconds, err.remainingAttempts)
            }
            val body = resp.body()
            val data = body?.data
            val user = data?.user
            if (body?.success != true || data == null || user == null) {
                return@withContext AuthOutcome.Error("RECOVER_FAILED", body?.message ?: "تعذر استعادة الحساب")
            }
            applyAuthenticatedSession(context, data)
            AuthOutcome.Authenticated(
                user = user,
                token = data.token,
                refreshToken = data.refreshToken,
                sessionId = data.sessionId,
                deviceTrust = data.trustedDevice ?: data.deviceTrust,
                message = body.message
            )
        } catch (e: Exception) {
            AuthOutcome.Error("NETWORK_ERROR", e.message ?: "تعذر الاتصال بالسيرفر")
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

    suspend fun listTrustedDevices(context: Context): Result<List<ApiTrustedDeviceDto>> = withContext(Dispatchers.IO) {
        try {
            DeviceTrustManager.syncRetrofitDeviceHeaders(context)
            val resp = RetrofitClient.apiService().getTrustedDevices()
            if (!resp.isSuccessful) {
                val err = parseErrorBody(resp.errorBody()?.string(), "تعذر تحميل قائمة الأجهزة")
                return@withContext Result.failure(IllegalStateException(err.message))
            }
            Result.success(resp.body()?.data?.devices.orEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeTrustedDevice(context: Context, deviceId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resp = RetrofitClient.apiService().revokeTrustedDevice(deviceId)
            if (!resp.isSuccessful) {
                val err = parseErrorBody(resp.errorBody()?.string(), "تعذر إلغاء وثوق الجهاز")
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
            Result.failure(e)
        }
    }

    suspend fun changePassword(
        context: Context,
        currentPassword: String,
        newPassword: String,
        stepUpChallengeId: String? = null,
        stepUpCode: String? = null
    ): PasswordChangeOutcome = withContext(Dispatchers.IO) {
        try {
            DeviceTrustManager.syncRetrofitDeviceHeaders(context)
            val deviceId = SecureCredentialVault.getOrCreateDeviceId(context)
            val resp = RetrofitClient.apiService().postChangePassword(
                ApiChangePasswordBody(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    stepUpChallengeId = stepUpChallengeId,
                    stepUpCode = stepUpCode,
                    deviceId = deviceId
                )
            )
            if (!resp.isSuccessful) {
                val err = parseErrorBody(resp.errorBody()?.string(), "تعذر تغيير كلمة المرور")
                if (err.requiresStepUpOtp || err.code == "STEP_UP_OTP_REQUIRED") {
                    return@withContext PasswordChangeOutcome.StepUpRequired(err.maskedPhone, err.message)
                }
                return@withContext PasswordChangeOutcome.Error(err.code, err.message)
            }
            val body = resp.body()
            val data = body?.data ?: return@withContext PasswordChangeOutcome.Error("EMPTY_RESPONSE", "استجابة غير صالحة")
            // Server revoked all 30-day trusted devices on password change
            DeviceTrustManager.clearServerDeviceTrust(context)
            if (data.token.isNotBlank()) {
                SecureCredentialVault.saveSessionTokens(
                    context = context,
                    accessToken = data.token,
                    refreshToken = data.refreshToken.takeIf { it.isNotBlank() },
                    sessionId = data.sessionId.takeIf { it.isNotBlank() }
                )
                RetrofitClient.setAuthToken(data.token)
            }
            PasswordChangeOutcome.Changed(
                token = data.token,
                refreshToken = data.refreshToken,
                sessionId = data.sessionId,
                message = body.message
            )
        } catch (e: Exception) {
            PasswordChangeOutcome.Error("NETWORK_ERROR", e.message ?: "تعذر الاتصال بالسيرفر")
        }
    }

    fun logout(context: Context, clearDeviceTrust: Boolean = false) {
        RetrofitClient.setAuthToken(null)
        if (clearDeviceTrust) {
            SecureCredentialVault.clearAll(context)
        } else {
            SecureCredentialVault.clearSessionOnly(context)
        }
    }
}
