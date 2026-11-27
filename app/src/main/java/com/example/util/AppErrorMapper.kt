package com.example.util

import org.json.JSONException
import retrofit2.HttpException
import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import javax.net.ssl.SSLException

/**
 * Centralized production error mapper & sanitizer.
 * Converts technical exceptions, HTTP failures, and backend codes into concise,
 * plain-language messages understandable by non-technical users.
 */
object AppErrorMapper {

    private val TECHNICAL_PATTERNS = listOf(
        "unable to resolve host",
        "no address associated",
        "failed to connect",
        "connection refused",
        "connection reset",
        "broken pipe",
        "software caused connection abort",
        "network is unreachable",
        "sockettimeout",
        "socket closed",
        "unknownhost",
        "econnrefused",
        "etimedout",
        "enotfound",
        "eai_again",
        "sslhandshake",
        "certpathvalidatorexception",
        "javax.",
        "java.lang.",
        "java.net.",
        "java.io.",
        "okhttp",
        "retrofit",
        "jsonexception",
        "end of input",
        "unexpected token",
        "syntaxerror",
        "nullpointerexception",
        "illegalstateexception",
        "illegalargumentexception",
        "sql",
        "stacktrace",
        "http 4",
        "http 5",
        "failed to fetch",
        "networkerror"
    )

    fun currentLang(): AppLanguage = Translations.currentAppLanguage

    fun fromThrowable(
        throwable: Throwable?,
        language: AppLanguage = currentLang(),
        fallbackAr: String = "حدث خطأ غير متوقع، حاول مجدداً",
        fallbackEn: String = "Something went wrong. Please try again"
    ): String {
        val isEn = language == AppLanguage.ENGLISH
        if (throwable == null) return if (isEn) fallbackEn else fallbackAr

        return when (throwable) {
            is UnknownHostException,
            is ConnectException,
            is NoRouteToHostException -> if (isEn) {
                "No connection. Check your internet"
            } else {
                "تعذر الاتصال، تحقق من الإنترنت"
            }

            is SocketTimeoutException -> if (isEn) {
                "Connection timed out. Try again"
            } else {
                "انتهت مهلة الاتصال، حاول مجدداً"
            }

            is SSLException -> if (isEn) {
                "Secure connection failed. Try again"
            } else {
                "تعذر تأمين الاتصال، حاول مجدداً"
            }

            is FileNotFoundException -> if (isEn) {
                "File not found"
            } else {
                "الملف غير موجود أو تعذر الوصول إليه"
            }

            is SecurityException -> if (isEn) {
                "Permission required for this action"
            } else {
                "الصلاحية مطلوبة لإتمام هذا الإجراء"
            }

            is JSONException -> if (isEn) {
                "Unable to read data"
            } else {
                "تعذر قراءة البيانات"
            }

            is HttpException -> fromHttpStatus(throwable.code(), language, fallbackAr, fallbackEn)

            is IOException -> {
                val raw = throwable.message
                if (looksTechnical(raw)) {
                    if (isEn) "Connection failed. Check your internet" else "تعذر الاتصال بالخادم، تحقق من الإنترنت"
                } else {
                    sanitizeMessage(raw, language, fallbackAr, fallbackEn)
                }
            }

            else -> sanitizeMessage(
                throwable.message ?: throwable.localizedMessage,
                language,
                fallbackAr,
                fallbackEn
            )
        }
    }

    fun fromHttpStatus(
        statusCode: Int,
        language: AppLanguage = currentLang(),
        fallbackAr: String = "تعذر إتمام الطلب، حاول مجدداً",
        fallbackEn: String = "Request failed. Please try again"
    ): String {
        val isEn = language == AppLanguage.ENGLISH
        return when (statusCode) {
            400 -> if (isEn) "Please check the entered details" else "تحقق من البيانات المدخلة"
            401 -> if (isEn) "Session expired. Please sign in again" else "انتهت الجلسة، سجل الدخول مجدداً"
            403 -> if (isEn) "You don't have permission for this" else "غير مصرح لك بهذا الإجراء"
            404 -> if (isEn) "Requested item was not found" else "البيانات المطلوبة غير موجودة"
            408 -> if (isEn) "Connection timed out. Try again" else "انتهت مهلة الطلب، حاول مجدداً"
            429 -> if (isEn) "Too many attempts. Please wait a moment" else "محاولات كثيرة، انتظر قليلاً ثم حاول"
            in 500..599 -> if (isEn) "Service temporarily unavailable. Try later" else "الخدمة غير متاحة مؤقتاً، حاول لاحقاً"
            else -> if (isEn) fallbackEn else fallbackAr
        }
    }

    fun fromApiError(
        code: String? = null,
        serverMessage: String? = null,
        statusCode: Int? = null,
        retryAfterSeconds: Int? = null,
        remainingAttempts: Int? = null,
        language: AppLanguage = currentLang(),
        fallbackAr: String = "تعذر إتمام الطلب، حاول مجدداً",
        fallbackEn: String = "Request failed. Please try again"
    ): String {
        val isEn = language == AppLanguage.ENGLISH
        val normalizedCode = code?.trim()?.uppercase(Locale.US).orEmpty()

        val codeMapped = when (normalizedCode) {
            "OTP_COOLDOWN" -> {
                val sec = (retryAfterSeconds ?: 60).coerceAtLeast(1)
                if (isEn) "Wait ${sec}s before requesting another code" else "انتظر $sec ثانية قبل طلب رمز جديد"
            }
            "OTP_LOCKED" -> {
                val sec = (retryAfterSeconds ?: 60).coerceAtLeast(1)
                if (isEn) "Too many attempts. Wait ${sec}s" else "محاولات كثيرة، انتظر $sec ثانية"
            }
            "OTP_DAILY_LIMIT" -> if (isEn) {
                "Daily code limit reached. Try tomorrow"
            } else {
                "وصلت للحد اليومي للرموز، حاول غداً"
            }
            "OTP_BUDGET_EXCEEDED",
            "OTP_DELIVERY_UNAVAILABLE",
            "OTP_ADAPTER_UNAVAILABLE" -> if (isEn) {
                "Couldn't send on this channel. Try another one"
            } else {
                "تعذر الإرسال عبر هذه الوسيلة، جرب وسيلة أخرى"
            }
            "OTP_CANCELLED" -> if (isEn) {
                "Code was cancelled. Request a new one"
            } else {
                "تم إلغاء الرمز، اطلب رمزاً جديداً"
            }
            "OTP_SUPERSEDED" -> if (isEn) {
                "Newer code sent. Enter the latest code"
            } else {
                "تم إرسال رمز أحدث، استخدم الرمز الجديد"
            }
            "OTP_EXPIRED" -> if (isEn) {
                "Code expired. Request a new one"
            } else {
                "انتهت صلاحية الرمز، اطلب رمزاً جديداً"
            }
            "OTP_ALREADY_USED",
            "OTP_CHALLENGE_NOT_FOUND" -> if (isEn) {
                "Code expired or already used. Request a new one"
            } else {
                "الرمز غير صالح، اطلب رمزاً جديداً"
            }
            "OTP_INVALID_CODE",
            "INVALID_OTP_FORMAT" -> if (remainingAttempts != null) {
                if (isEn) "Wrong code ($remainingAttempts attempts left)" else "الرمز غير صحيح (متبقي $remainingAttempts محاولات)"
            } else {
                if (isEn) "Incorrect verification code" else "رمز التحقق غير صحيح"
            }
            "INVALID_PHONE",
            "OTP_PHONE_MISMATCH" -> if (isEn) {
                "Check the phone number and try again"
            } else {
                "تحقق من رقم الهاتف وحاول مجدداً"
            }
            "COUNTRY_NOT_ALLOWED" -> if (isEn) {
                "Verification isn't available for this country yet"
            } else {
                "التحقق غير متاح لهذا البلد حالياً"
            }
            "RATE_LIMIT" -> if (isEn) {
                "Too many attempts. Wait a moment"
            } else {
                "محاولات كثيرة، انتظر قليلاً"
            }
            else -> null
        }
        if (codeMapped != null) return codeMapped

        if (!serverMessage.isNullOrBlank() && !looksTechnical(serverMessage)) {
            return serverMessage.trim()
        }
        if (statusCode != null && statusCode >= 400) {
            return fromHttpStatus(statusCode, language, fallbackAr, fallbackEn)
        }
        return if (isEn) fallbackEn else fallbackAr
    }

    fun sanitizeMessage(
        raw: String?,
        language: AppLanguage = currentLang(),
        fallbackAr: String = "حدث خطأ، حاول مجدداً",
        fallbackEn: String = "Something went wrong. Please try again"
    ): String {
        val isEn = language == AppLanguage.ENGLISH
        val fallback = if (isEn) fallbackEn else fallbackAr
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isBlank()) return fallback
        if (looksTechnical(trimmed)) return fallback
        // Avoid dumping overly long multi-line technical paragraphs
        return trimmed.lineSequence().firstOrNull()?.trim()?.take(140).orEmpty().ifBlank { fallback }
    }

    fun looksTechnical(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.lowercase(Locale.US)
        if (TECHNICAL_PATTERNS.any { lower.contains(it) }) return true
        // Raw host/port/url fragments or status codes wrapped in parens like "(502)"
        if (lower.contains("http://") || lower.contains("https://")) return true
        if (Regex("""\(\d{3}\)\s*$""").containsMatchIn(lower)) return true
        return false
    }
}
