package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

/**
 * Cryptographic Device Trust & PIN Rate-Limit Manager.
 * - Binds a user's phone account to a SHA-256 hardware device fingerprint.
 * - Forces OTP verification whenever an account is accessed from a new/untrusted device.
 * - Persists failed PIN attempts across dialog restarts to prevent brute-force bypass.
 */
object DeviceTrustManager {

    private const val PREFS_NAME = "oozox_security_vault"
    private const val KEY_DEVICE_PREFIX = "trusted_device_"
    private const val KEY_PIN_ATTEMPTS = "pin_failed_attempts"
    private const val KEY_PIN_LOCK_UNTIL = "pin_lock_until_ms"
    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30_000L

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

    private fun normalizePhone(phone: String): String =
        phone.filter { it.isDigit() }.takeLast(10)

    fun isDeviceTrustedForPhone(context: Context, phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedHash = prefs.getString("$KEY_DEVICE_PREFIX${normalizePhone(phone)}", null) ?: return false
        return savedHash == getCurrentDeviceFingerprint(context)
    }

    fun markDeviceTrusted(context: Context, phone: String?) {
        if (phone.isNullOrBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("$KEY_DEVICE_PREFIX${normalizePhone(phone)}", getCurrentDeviceFingerprint(context))
            .apply()
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
