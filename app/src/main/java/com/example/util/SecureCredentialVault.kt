package com.example.util

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Hardware-backed credential vault for JWT access/refresh tokens and server-issued
 * 30-day Trusted Device tokens.
 * Uses Android Keystore AES/GCM when available on device, with a deterministic
 * device-local AES-GCM fallback when Keystore provider is unavailable in JVM unit tests.
 */
object SecureCredentialVault {

    private const val PREFS_NAME = "oozox_secure_cred_vault_v2"
    private const val KEYSTORE_ALIAS = "oozox_cred_master_key_v1"
    private const val FALLBACK_KEY_PREF = "_local_fallback_k"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_TRUST_TOKEN = "device_trust_token"
    private const val KEY_TRUSTED_UNTIL = "device_trusted_until_ms"
    private const val KEY_TRUSTED_PHONE = "device_trusted_phone"
    private const val KEY_TRUSTED_USER_ID = "device_trusted_user_id"
    private const val KEY_TRUSTED_FINGERPRINT = "device_trusted_fingerprint"
    private const val KEY_LAST_ONLINE_AUTH = "last_online_auth_ms"

    private const val GCM_IV_LEN = 12
    private const val GCM_TAG_BITS = 128

    private fun getOrCreateKey(context: Context): SecretKey {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                val existing = ks.getKey(KEYSTORE_ALIAS, null) as? SecretKey
                if (existing != null) return existing

                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val spec = KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGen.init(spec)
                return keyGen.generateKey()
            } catch (_: Throwable) {
                // Fall back on Robolectric JVM or restricted emulators without StrongBox/Keystore
            }
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encoded = prefs.getString(FALLBACK_KEY_PREF, null)
        val raw = if (!encoded.isNullOrBlank()) {
            Base64.decode(encoded, Base64.NO_WRAP)
        } else {
            ByteArray(32).also { SecureRandom().nextBytes(it) }.also { bytes ->
                prefs.edit().putString(FALLBACK_KEY_PREF, Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
            }
        }
        return SecretKeySpec(raw, "AES")
    }

    private fun encryptString(context: Context, plaintext: String): String {
        val key = getOrCreateKey(context)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        val packed = ByteArray(1 + iv.size + cipherText.size)
        packed[0] = iv.size.toByte()
        System.arraycopy(iv, 0, packed, 1, iv.size)
        System.arraycopy(cipherText, 0, packed, 1 + iv.size, cipherText.size)
        return "v1:" + Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    private fun decryptString(context: Context, encoded: String?): String? {
        if (encoded.isNullOrBlank() || !encoded.startsWith("v1:")) return null
        return try {
            val packed = Base64.decode(encoded.removePrefix("v1:"), Base64.NO_WRAP)
            if (packed.size < 2) return null
            val ivLen = packed[0].toInt() and 0xFF
            if (packed.size <= 1 + ivLen) return null
            val iv = packed.copyOfRange(1, 1 + ivLen)
            val cipherText = packed.copyOfRange(1 + ivLen, packed.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(context), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherText), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = decryptString(context, prefs.getString(KEY_DEVICE_ID, null))
        if (!existing.isNullOrBlank()) return existing
        val generated = "dev_" + UUID.randomUUID().toString().replace("-", "")
        prefs.edit().putString(KEY_DEVICE_ID, encryptString(context, generated)).apply()
        return generated
    }

    fun saveSessionTokens(
        context: Context,
        accessToken: String?,
        refreshToken: String?,
        sessionId: String? = null
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        if (!accessToken.isNullOrBlank()) {
            editor.putString(KEY_ACCESS_TOKEN, encryptString(context, accessToken.trim()))
            editor.putLong(KEY_LAST_ONLINE_AUTH, System.currentTimeMillis())
        } else {
            editor.remove(KEY_ACCESS_TOKEN)
        }
        if (!refreshToken.isNullOrBlank()) {
            editor.putString(KEY_REFRESH_TOKEN, encryptString(context, refreshToken.trim()))
        } else {
            editor.remove(KEY_REFRESH_TOKEN)
        }
        if (!sessionId.isNullOrBlank()) {
            editor.putString(KEY_SESSION_ID, encryptString(context, sessionId.trim()))
        }
        editor.apply()
    }

    fun getAccessToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return decryptString(context, prefs.getString(KEY_ACCESS_TOKEN, null))
    }

    fun getRefreshToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return decryptString(context, prefs.getString(KEY_REFRESH_TOKEN, null))
    }

    fun getSessionId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return decryptString(context, prefs.getString(KEY_SESSION_ID, null))
    }

    fun saveServerDeviceTrust(
        context: Context,
        phoneE164: String,
        userId: String,
        deviceTrustToken: String,
        trustedUntilEpochMs: Long,
        hardwareFingerprint: String
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TRUST_TOKEN, encryptString(context, deviceTrustToken.trim()))
            .putString(KEY_TRUSTED_PHONE, encryptString(context, phoneE164.trim()))
            .putString(KEY_TRUSTED_USER_ID, encryptString(context, userId.trim()))
            .putString(KEY_TRUSTED_FINGERPRINT, encryptString(context, hardwareFingerprint.trim()))
            .putLong(KEY_TRUSTED_UNTIL, trustedUntilEpochMs)
            .apply()
    }

    fun getServerDeviceTrustToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return decryptString(context, prefs.getString(KEY_TRUST_TOKEN, null))
    }

    fun getTrustedUntilEpochMs(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_TRUSTED_UNTIL, 0L)
    }

    fun getTrustedPhone(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return decryptString(context, prefs.getString(KEY_TRUSTED_PHONE, null))
    }

    fun getTrustedUserId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return decryptString(context, prefs.getString(KEY_TRUSTED_USER_ID, null))
    }

    fun getTrustedFingerprint(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return decryptString(context, prefs.getString(KEY_TRUSTED_FINGERPRINT, null))
    }

    fun clearSessionOnly(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_SESSION_ID)
            .apply()
    }

    fun clearDeviceTrust(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_TRUST_TOKEN)
            .remove(KEY_TRUSTED_PHONE)
            .remove(KEY_TRUSTED_USER_ID)
            .remove(KEY_TRUSTED_FINGERPRINT)
            .remove(KEY_TRUSTED_UNTIL)
            .apply()
    }

    fun clearAll(context: Context) {
        clearSessionOnly(context)
        clearDeviceTrust(context)
    }
}
