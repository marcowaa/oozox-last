package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Cryptographic Device Trust & PIN Rate-Limit Manager.
 * - Uses server-issued 30-day deviceTrustToken stored in SecureCredentialVault.
 * - Local SharedPreferences flag alone NEVER grants online device trust.
 * - Offline biometric/PIN unlock is permitted strictly for cached local Room data when server trust cache is valid & unexpired.
 */
object DeviceTrustManager {

    private const val PREFS_NAME = "oozox_security_vault"
    private const val KEY_PIN_ATTEMPTS = "pin_failed_attempts"
    private const val KEY_PIN_LOCK_UNTIL = "pin_lock_until_ms"
    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30_000L
    const val TRUST_TTL_MILLIS = 30L * 24L * 60L * 60L * 1000L

    @SuppressLint("HardwareIds")
    fun getCurrentDeviceFingerprint(context: Context): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_id"
        } catch (_: Exception) {
            "unknown_id"
        }
        val raw = "$androidId|${Build.MANUFACTURER}|${Build.MODEL}|${Build.BRAND}"
        return sha256(raw)
    }

    fun getDeviceDisplayName(): String =
        listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { !it.isNullOrBlank() }
            .joinToString(" ")
            .ifBlank { "Android Device" }

    private fun normalizePhoneTail(phone: String?): String =
        phone.orEmpty().filter { it.isDigit() }.takeLast(10)

    fun parseIsoEpochMs(iso: String?): Long {
        if (iso.isNullOrBlank()) return 0L
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(iso)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return 0L
    }

    fun syncRetrofitDeviceHeaders(context: Context) {
        val deviceId = SecureCredentialVault.getOrCreateDeviceId(context)
        val fp = getCurrentDeviceFingerprint(context)
        RetrofitClient.setDeviceHeaders(
            deviceId = deviceId,
            deviceName = getDeviceDisplayName(),
            fingerprint = fp
        )
    }

    fun storeServerDeviceTrust(
        context: Context,
        phone: String,
        userId: String,
        deviceTrustToken: String,
        trustedUntilIso: String?
    ) {
        if (phone.isBlank() || deviceTrustToken.isBlank()) return
        val parsedExpiry = parseIsoEpochMs(trustedUntilIso)
        val trustedUntilMs = if (parsedExpiry > 0L) parsedExpiry else System.currentTimeMillis() + TRUST_TTL_MILLIS
        val fp = getCurrentDeviceFingerprint(context)
        SecureCredentialVault.saveServerDeviceTrust(
            context = context,
            phoneE164 = phone,
            userId = userId,
            deviceTrustToken = deviceTrustToken,
            trustedUntilEpochMs = trustedUntilMs,
            hardwareFingerprint = fp
        )
        syncRetrofitDeviceHeaders(context)
    }

    fun clearServerDeviceTrust(context: Context) {
        SecureCredentialVault.clearDeviceTrust(context)
    }

    /**
     * Returns true ONLY if a server-issued deviceTrustToken is present in SecureCredentialVault,
     * has not exceeded its 30-day expiry, matches the hardware fingerprint, and matches the target phone.
     */
    fun isDeviceTrustedForPhone(context: Context, phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false
        val trustToken = SecureCredentialVault.getServerDeviceTrustToken(context) ?: return false
        if (trustToken.isBlank()) return false
        val trustedUntil = SecureCredentialVault.getTrustedUntilEpochMs(context)
        if (trustedUntil <= System.currentTimeMillis()) return false
        val savedPhone = SecureCredentialVault.getTrustedPhone(context) ?: return false
        if (normalizePhoneTail(savedPhone) != normalizePhoneTail(phone)) return false
        val savedFp = SecureCredentialVault.getTrustedFingerprint(context) ?: return false
        return savedFp == getCurrentDeviceFingerprint(context)
    }

    fun isOfflineCacheAccessAllowed(context: Context, phone: String?): Boolean =
        isDeviceTrustedForPhone(context, phone)

    @Deprecated(
        message = "Device trust is issued by backend server only; call storeServerDeviceTrust with server token.",
        replaceWith = ReplaceWith("storeServerDeviceTrust(context, phone, userId, deviceTrustToken, trustedUntilIso)")
    )
    fun markDeviceTrusted(context: Context, phone: String?) {
        // Intentionally no-op: local callers must not mint server trust.
    }

    fun getRemainingLockoutSeconds(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockUntil = prefs.getLong(KEY_PIN_LOCK_UNTIL, 0L)
        val remaining = lockUntil - System.currentTimeMillis()
        return if (remaining > 0L) ((remaining + 999L) / 1000L).toInt() else 0
    }

    fun recordFailedPinAttempt(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val attempts = prefs.getInt(KEY_PIN_ATTEMPTS, 0) + 1
        val editor = prefs.edit()
        if (attempts >= MAX_ATTEMPTS) {
            editor.putInt(KEY_PIN_ATTEMPTS, 0)
            editor.putLong(KEY_PIN_LOCK_UNTIL, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
        } else {
            editor.putInt(KEY_PIN_ATTEMPTS, attempts)
        }
        editor.apply()
        return (MAX_ATTEMPTS - attempts).coerceAtLeast(0)
    }

    fun resetPinAttempts(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_PIN_ATTEMPTS, 0)
            .putLong(KEY_PIN_LOCK_UNTIL, 0L)
            .apply()
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
