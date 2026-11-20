package com.example.util

import android.content.Context
import android.util.Base64
import com.example.data.model.HiddenSubAccountEntity
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * محرك الحماية والتشفير والتحقق لحسابات oozox الفرعية المخفية.
 * يوفر:
 * 1. تجزئة وتشفير كلمات المرور والرموز السرية باستخدام SHA-256 والتمليح (Salt).
 * 2. التحقق الذكي الفوري من مدخلات حقل البحث للتحويل السري التلقائي.
 * 3. التشفير المحلي للبيانات والدردشات الخاصة بالحساب الفرعي.
 * 4. إدارة سياسة الإشعارات المعزولة (كتم تام أو تمويه كإشعار نظام صامت).
 */
object HiddenAccountSecurityEngine {

    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PBKDF2_ITERATIONS = 65536
    private const val PBKDF2_KEY_BITS = 256
    private const val HASH_PREFIX_V2 = "pbkdf2_v2:"
    private const val SUB_ENC_PREFIX_V1 = "SUB_ENC:"
    private const val SUB_ENC_PREFIX_V2 = "SUB_ENC_V2:"
    private val secureRandom = SecureRandom()

    // Session cache of active unlocked sub-account
    @Volatile
    private var activeSubAccount: HiddenSubAccountEntity? = null

    fun getActiveSubAccount(): HiddenSubAccountEntity? = activeSubAccount

    fun setActiveSubAccount(account: HiddenSubAccountEntity?) {
        activeSubAccount = account
    }

    fun isSubAccountActive(): Boolean = activeSubAccount != null

    /**
     * توليد ملح تشفير عشوائي آمن بطول 16 بايت
     */
    fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    }

    /**
     * تجزئة كلمة المرور بالملح عبر PBKDF2WithHmacSHA256 (65,536 دورة)
     */
    fun hashPassword(password: String, salt: String): String {
        val saltBytes = salt.toByteArray(StandardCharsets.UTF_8)
        val keySpec = PBEKeySpec(password.toCharArray(), saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(keySpec)
            .encoded
        return HASH_PREFIX_V2 + keyBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPasswordLegacySha256(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(StandardCharsets.UTF_8))
        val digest = md.digest(password.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * التحقق من تطابق كلمة المرور المدخلة مع الحساب (بمقارنة ثابتة الزمن Constant-Time)
     */
    fun verifyPassword(candidate: String, account: HiddenSubAccountEntity): Boolean {
        val trimmedCandidate = candidate.trim()
        if (trimmedCandidate.isBlank()) return false

        // Check direct secret trigger match (constant-time)
        val storedTrigger = account.rawSecretTrigger.trim()
        if (storedTrigger.isNotEmpty() && MessageDigest.isEqual(
                storedTrigger.toByteArray(StandardCharsets.UTF_8),
                trimmedCandidate.toByteArray(StandardCharsets.UTF_8)
            )
        ) {
            return true
        }

        val storedLower = account.passwordHash.lowercase()
        val computedHash = if (storedLower.startsWith(HASH_PREFIX_V2)) {
            hashPassword(trimmedCandidate, account.passwordSalt).lowercase()
        } else {
            // Backward compatibility for legacy SHA-256 salted hashes
            hashPasswordLegacySha256(trimmedCandidate, account.passwordSalt).lowercase()
        }

        return MessageDigest.isEqual(
            computedHash.toByteArray(StandardCharsets.UTF_8),
            storedLower.toByteArray(StandardCharsets.UTF_8)
        )
    }

    /**
     * فحص حقل البحث:
     * إذا كتب المستخدم كلمة المرور الخاصة بالحساب الفرعي أو (اليوزر:كلمة المرور)،
     * يتم التحقق فورياً وإرجاع الحساب المطابق ليتم التحويل إليه تلقائياً دون إظهار أي أثر.
     */
    fun evaluateSearchTrigger(
        searchQuery: String,
        accounts: List<HiddenSubAccountEntity>
    ): HiddenSubAccountEntity? {
        val cleanQuery = searchQuery.trim()
        if (cleanQuery.length < 2 || accounts.isEmpty()) return null

        for (account in accounts) {
            // Pattern 1: Exact password or secret trigger typed into search
            if (verifyPassword(cleanQuery, account)) {
                return account
            }

            // Pattern 2: username:password format
            if (cleanQuery.contains(":") || cleanQuery.contains(" ")) {
                val delimiter = if (cleanQuery.contains(":")) ":" else " "
                val parts = cleanQuery.split(delimiter, limit = 2)
                if (parts.size == 2) {
                    val candidateUser = parts[0].trim().removePrefix("@")
                    val candidatePass = parts[1].trim()
                    if (candidateUser.equals(account.username.trim(), ignoreCase = true) &&
                        verifyPassword(candidatePass, account)
                    ) {
                        return account
                    }
                }
            }

            // Pattern 3: @username#password
            if (cleanQuery.startsWith("@") && cleanQuery.contains("#")) {
                val parts = cleanQuery.removePrefix("@").split("#", limit = 2)
                if (parts.size == 2) {
                    val candidateUser = parts[0].trim()
                    val candidatePass = parts[1].trim()
                    if (candidateUser.equals(account.username.trim(), ignoreCase = true) &&
                        verifyPassword(candidatePass, account)
                    ) {
                        return account
                    }
                }
            }
        }

        return null
    }

    /**
     * تشفير نص محلياً لمحتوى الحساب الفرعي باستخدام ملح عشوائي + PBKDF2-HMAC-SHA256 + AES-256-GCM
     */
    fun encryptSubAccountContent(plainText: String, secretKey: String): String {
        if (plainText.isBlank()) return plainText
        return try {
            val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
            val keySpecInput = PBEKeySpec(secretKey.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
            val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(keySpecInput)
                .encoded
            val keySpec = SecretKeySpec(keyBytes, "AES")

            val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

            val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

            val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            val combinedB64 = Base64.encodeToString(combined, Base64.NO_WRAP)
            "$SUB_ENC_PREFIX_V2$saltB64:$combinedB64"
        } catch (_: Exception) {
            plainText
        }
    }

    /**
     * فك تشفير نص خاص بالحساب الفرعي (يدعم الإصدار الجديد SUB_ENC_V2 والقديم SUB_ENC)
     */
    fun decryptSubAccountContent(cipherText: String, secretKey: String): String {
        if (cipherText.startsWith(SUB_ENC_PREFIX_V2)) {
            return try {
                val payload = cipherText.removePrefix(SUB_ENC_PREFIX_V2)
                val parts = payload.split(":", limit = 2)
                if (parts.size != 2) return cipherText
                val salt = Base64.decode(parts[0], Base64.NO_WRAP)
                val combined = Base64.decode(parts[1], Base64.NO_WRAP)
                if (combined.size <= GCM_IV_LENGTH) return cipherText

                val iv = ByteArray(GCM_IV_LENGTH)
                System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
                val cipherBytes = ByteArray(combined.size - GCM_IV_LENGTH)
                System.arraycopy(combined, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.size)

                val keySpecInput = PBEKeySpec(secretKey.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
                val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpecInput)
                    .encoded
                val keySpec = SecretKeySpec(keyBytes, "AES")

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(GCM_TAG_LENGTH, iv))
                String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
            } catch (_: Exception) {
                cipherText
            }
        }

        if (!cipherText.startsWith(SUB_ENC_PREFIX_V1)) return cipherText
        return try {
            val cleanBase64 = cipherText.removePrefix(SUB_ENC_PREFIX_V1)
            val combined = Base64.decode(cleanBase64, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH) return cipherText

            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)

            val cipherBytes = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.size)

            val keyBytes = MessageDigest.getInstance("SHA-256")
                .digest(secretKey.toByteArray(StandardCharsets.UTF_8))
            val keySpec = SecretKeySpec(keyBytes, "AES")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            cipherText
        }
    }
}
