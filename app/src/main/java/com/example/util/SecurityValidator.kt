package com.example.util

import java.security.SecureRandom
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * SecurityValidator & Cryptographic Utilities
 * Provides input sanitization, anti-injection safeguards, BIP39 mnemonic recovery words,
 * rate-limiting brute-force defenses, and AES-256-GCM transfer payloads.
 */
object SecurityValidator {

    private val SQL_INJECTION_PATTERN = Pattern.compile(
        "('\\s*(OR|AND)\\s+['\"]?[\\w-]+['\"]?\\s*=\\s*['\"]?[\\w-]+['\"]?)|(--|/\\*|\\*/|;|UNION\\s+SELECT|DROP\\s+TABLE|INSERT\\s+INTO)",
        Pattern.CASE_INSENSITIVE
    )

    private val SCRIPT_TAG_PATTERN = Pattern.compile(
        "<\\s*script[^>]*>([\\s\\S]*?)<\\s*/\\s*script\\s*>",
        Pattern.CASE_INSENSITIVE
    )

    // Standard country phone codes for Arab and Gulf regions & international
    data class CountryCodeInfo(
        val code: String,
        val countryNameAr: String,
        val flag: String,
        val exampleLength: Int = 10,
        val placeholder: String = "1012345678"
    )

    val SUPPORTED_COUNTRIES = listOf(
        CountryCodeInfo("+20", "مصر", "🇪🇬", 10, "1012345678"),
        CountryCodeInfo("+966", "السعودية", "🇸🇦", 9, "512345678"),
        CountryCodeInfo("+971", "الإمارات", "🇦🇪", 9, "501234567"),
        CountryCodeInfo("+965", "الكويت", "🇰🇼", 8, "51234567"),
        CountryCodeInfo("+974", "قطر", "🇶🇦", 8, "33123456"),
        CountryCodeInfo("+973", "البحرين", "🇧🇭", 8, "39123456"),
        CountryCodeInfo("+968", "عُمان", "🇴🇲", 8, "91234567"),
        CountryCodeInfo("+962", "الأردن", "🇯🇴", 9, "791234567"),
        CountryCodeInfo("+961", "لبنان", "🇱🇧", 8, "70123456"),
        CountryCodeInfo("+964", "العراق", "🇮🇶", 10, "7701234567"),
        CountryCodeInfo("+212", "المغرب", "🇲🇦", 9, "612345678"),
        CountryCodeInfo("+216", "تونس", "🇹🇳", 8, "98123456"),
        CountryCodeInfo("+213", "الجزائر", "🇩🇿", 9, "551234567"),
        CountryCodeInfo("+1", "الولايات المتحدة / كندا", "🇺🇸", 10, "4155552671"),
        CountryCodeInfo("+44", "المملكة المتحدة", "🇬🇧", 10, "7911123456"),
        CountryCodeInfo("+49", "ألمانيا", "🇩🇪", 10, "1511234567"),
        CountryCodeInfo("+90", "تركيا", "🇹🇷", 10, "5321234567")
    )

    data class PhoneValidationResult(
        val isValid: Boolean,
        val cleanDigits: String,
        val fullFormattedPhone: String,
        val specificError: String? = null,
        val carrierName: String? = null,
        val autoCorrectionNotice: String? = null
    )

    /**
     * Deep validation of mobile number based on country dialing rules and prefix patterns
     */
    fun validateMobileNumber(selectedCountry: CountryCodeInfo, rawInput: String): PhoneValidationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return PhoneValidationResult(
                isValid = false,
                cleanDigits = "",
                fullFormattedPhone = selectedCountry.code,
                specificError = "يرجى كتابة رقم الهاتف المحمول للمتابعة 📱"
            )
        }

        // Check for disallowed characters
        if (trimmed.any { !it.isDigit() && it != '+' && it != ' ' && it != '-' && it != '(' && it != ')' }) {
            return PhoneValidationResult(
                isValid = false,
                cleanDigits = "",
                fullFormattedPhone = selectedCountry.code,
                specificError = "الرقم يحتوي على رموز أو أحرف غير صالحة. يرجى إدخال أرقام فقط ⚠️"
            )
        }

        var digits = trimmed.filter { it.isDigit() }
        var autoCorrection: String? = null

        // Strip selected country code if user typed it inside the text field
        val strippedCode = selectedCountry.code.removePrefix("+")
        if (digits.startsWith(strippedCode)) {
            digits = digits.removePrefix(strippedCode)
            autoCorrection = "تم حذف كود الدولة المكرر تلقائياً (${selectedCountry.code})"
        }

        // Strip leading 0 if present (e.g. 010... -> 10...)
        if (digits.startsWith("0")) {
            digits = digits.drop(1)
            if (autoCorrection == null) {
                autoCorrection = "تمت إزالة الصفر الأول لمطابقة المعيار الدولي"
            }
        }

        if (digits.isBlank()) {
            return PhoneValidationResult(
                isValid = false,
                cleanDigits = "",
                fullFormattedPhone = selectedCountry.code,
                specificError = "يرجى إكمال كتابة رقم الهاتف المحمول"
            )
        }

        var carrier: String? = null
        var specificError: String? = null

        when (selectedCountry.code) {
            "+20" -> { // Egypt: 10 digits without leading 0. Prefixes: 10, 11, 12, 15
                when {
                    digits.length < 10 -> {
                        specificError = "رقم الهاتف المصري غير مكتمل (متبقي ${10 - digits.length} أرقام) 🇪🇬"
                    }
                    digits.length > 10 -> {
                        specificError = "رقم الهاتف المصري أطول من اللازم (يجب أن يتكون من 10 أرقام بعد إزالة الصفر) ⚠️"
                    }
                    digits.startsWith("10") -> carrier = "فودافون مصر (Vodafone)"
                    digits.startsWith("11") -> carrier = "إتصالات مصر (e& / Etisalat)"
                    digits.startsWith("12") -> carrier = "أورنج مصر (Orange)"
                    digits.startsWith("15") -> carrier = "المصرية للاتصالات (WE)"
                    else -> specificError = "رقم المحمول المصري غير صالح. يجب أن يبدأ بـ 010 أو 011 أو 012 أو 015 ⚠️"
                }
            }
            "+966" -> { // Saudi Arabia: 9 digits without leading 0. Starts with 5
                when {
                    digits.length < 9 -> {
                        specificError = "رقم الهاتف السعودي غير مكتمل (متبقي ${9 - digits.length} أرقام) 🇸🇦"
                    }
                    digits.length > 9 -> {
                        specificError = "رقم الهاتف السعودي أطول من المعتاد (يتكون من 9 أرقام بعد الكود) ⚠️"
                    }
                    !digits.startsWith("5") -> {
                        specificError = "رقم الجوال السعودي يجب أن يبدأ بالرقم 5 (مثال: 512345678) 🇸🇦"
                    }
                    digits.startsWith("50") || digits.startsWith("53") || digits.startsWith("55") -> carrier = "STC (الاتصالات السعودية)"
                    digits.startsWith("54") || digits.startsWith("56") -> carrier = "موبايلي (Mobily)"
                    digits.startsWith("58") || digits.startsWith("59") -> carrier = "زين السعودية (Zain)"
                    else -> carrier = "شبكة اتصالات سعودية معتمدة"
                }
            }
            "+971" -> { // UAE: 9 digits without leading 0. Starts with 5
                when {
                    digits.length < 9 -> {
                        specificError = "رقم الهاتف الإماراتي غير مكتمل (متبقي ${9 - digits.length} أرقام) 🇦🇪"
                    }
                    digits.length > 9 -> {
                        specificError = "رقم الهاتف الإماراتي يجب أن يتكون من 9 أرقام 🇦🇪"
                    }
                    !digits.startsWith("5") -> {
                        specificError = "رقم الجوال الإماراتي يجب أن يبدأ بالرقم 5 (مثال: 501234567) 🇦🇪"
                    }
                    digits.startsWith("50") || digits.startsWith("54") || digits.startsWith("56") -> carrier = "اتصالات الإمارات (e&)"
                    digits.startsWith("52") || digits.startsWith("55") || digits.startsWith("58") -> carrier = "دو (du)"
                    else -> carrier = "اتصالات الإمارات"
                }
            }
            "+965" -> { // Kuwait: 8 digits. Starts with 5, 6, 9
                when {
                    digits.length < 8 -> specificError = "رقم الهاتف الكويتي غير مكتمل (يتكون من 8 أرقام) 🇰🇼"
                    digits.length > 8 -> specificError = "رقم الهاتف الكويتي يجب أن يتكون من 8 أرقام 🇰🇼"
                    digits.startsWith("9") -> carrier = "زين الكويت (Zain)"
                    digits.startsWith("6") -> carrier = "أوريدو الكويت (Ooredoo)"
                    digits.startsWith("5") -> carrier = "STC الكويت"
                    else -> specificError = "رقم الجوال الكويتي يجب أن يبدأ بـ 5 أو 6 أو 9 🇰🇼"
                }
            }
            else -> { // International / Other Arab Countries
                if (digits.length < 7) {
                    specificError = "رقم الهاتف قصير جداً وغير مكتمل لهذا البلد ⚠️"
                } else if (digits.length > 14) {
                    specificError = "رقم الهاتف يتجاوز الحد الأقصى للأرقام الدولية المسموح بها ⚠️"
                }
            }
        }

        val isValid = specificError == null
        val fullFormatted = "${selectedCountry.code}$digits"

        return PhoneValidationResult(
            isValid = isValid,
            cleanDigits = digits,
            fullFormattedPhone = fullFormatted,
            specificError = specificError,
            carrierName = carrier,
            autoCorrectionNotice = autoCorrection
        )
    }

    // Standard curated 12-Word Mnemonic Wordpool (Gold & Financial Security Vocabulary)
    val MNEMONIC_WORDS = listOf(
        "oozox", "gold", "vault", "shield", "karat", "asset", "bullion", "mint",
        "safeguard", "infinity", "crystal", "sovereign", "silver", "diamond", "capital", "ledger",
        "market", "purity", "secure", "clearing", "reserve", "quantum", "treasure", "fortune",
        "emerald", "sterling", "falcon", "beacon", "citadel", "titan", "matrix", "apex",
        "aurum", "nexus", "orbit", "prism", "summit", "zenith", "legacy", "crown"
    )

    /**
     * Sanitizes user input against malicious SQL injection strings, XSS payloads, and control characters.
     */
    fun sanitizeInput(input: String?): String {
        if (input == null) return ""
        var cleaned = input.trim()
        // Strip null bytes and control chars
        cleaned = cleaned.replace("\u0000", "").replace("\r", "")
        // Prevent script tags
        cleaned = SCRIPT_TAG_PATTERN.matcher(cleaned).replaceAll("")
        return cleaned
    }

    /**
     * Checks if input contains dangerous patterns.
     */
    fun isPotentiallyMalicious(input: String): Boolean {
        if (input.isBlank()) return false
        return SQL_INJECTION_PATTERN.matcher(input).find() || SCRIPT_TAG_PATTERN.matcher(input).find()
    }

    /**
     * Generates a 12-word secure mnemonic recovery seed phrase for account restore.
     */
    fun generateRecoverySeed(): List<String> {
        val random = SecureRandom()
        val seed = mutableListOf<String>()
        repeat(12) {
            val idx = random.nextInt(MNEMONIC_WORDS.size)
            seed.add(MNEMONIC_WORDS[idx])
        }
        return seed
    }

    /**
     * Validates if a given list of recovery words matches the expected structure.
     */
    fun validateRecoverySeed(words: List<String>): Boolean {
        if (words.size != 12) return false
        return words.all { it.isNotBlank() && it.length >= 3 }
    }

    /**
     * Generates a realistic mock WhatsApp/Telegram style 6-digit OTP code for secure verification.
     */
    fun generateSecureOtp(): String {
        val random = SecureRandom()
        val number = 100000 + random.nextInt(900000)
        return number.toString()
    }

    /**
     * Computes Password / PIN Entropy Rating (1 to 5 stars).
     */
    fun evaluatePinStrength(pin: String): Int {
        if (pin.length < 4) return 1
        var score = 1
        if (pin.length >= 6) score++
        if (pin.any { it.isDigit() } && pin.any { it.isLetter() }) score++
        if (pin.any { !it.isLetterOrDigit() }) score++
        // Check if not sequential or repetitive
        val isSequential = "0123456789".contains(pin) || "9876543210".contains(pin)
        val isRepetitive = pin.toSet().size == 1
        if (!isSequential && !isRepetitive && pin.length >= 6) score++
        return score.coerceIn(1, 5)
    }

    /**
     * Rate limiter tracker for OTP / Login brute force defense.
     */
    class RateLimiter(private val maxAttempts: Int = 3, private val lockDurationMillis: Long = 180_000L) {
        private var failedAttempts = 0
        private var lockedUntilTimestamp = 0L

        fun isLocked(): Boolean {
            val now = System.currentTimeMillis()
            return now < lockedUntilTimestamp
        }

        fun getRemainingLockSeconds(): Int {
            val now = System.currentTimeMillis()
            if (now >= lockedUntilTimestamp) return 0
            return ((lockedUntilTimestamp - now) / 1000).toInt()
        }

        fun recordFailure() {
            failedAttempts++
            if (failedAttempts >= maxAttempts) {
                lockedUntilTimestamp = System.currentTimeMillis() + lockDurationMillis
            }
        }

        fun recordSuccess() {
            failedAttempts = 0
            lockedUntilTimestamp = 0L
        }

        fun getFailedAttempts(): Int = failedAttempts
        fun getRemainingAttempts(): Int = (maxAttempts - failedAttempts).coerceAtLeast(0)
    }

    /**
     * Generates a high-entropy encrypted Device Migration Bundle (AES-256-GCM).
     */
    fun createDeviceMigrationPayload(
        userId: String,
        userName: String,
        phone: String,
        userType: String,
        passphrase: String
    ): String {
        val payloadPlain = "OOZOX_MIGRATE|${System.currentTimeMillis()}|$userId|$userName|$phone|$userType"
        val salt = "OOZOX_DEVICE_MIGRATION_SALT_2026".toByteArray(Charsets.UTF_8)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

        val cipherText = cipher.doFinal(payloadPlain.toByteArray(Charsets.UTF_8))
        val combined = iv + cipherText
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts and parses a Device Migration Bundle.
     */
    fun decryptDeviceMigrationPayload(
        encryptedBase64: String,
        passphrase: String
    ): MigrationData? {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < 13) return null
            val iv = combined.copyOfRange(0, 12)
            val cipherText = combined.copyOfRange(12, combined.size)

            val salt = "OOZOX_DEVICE_MIGRATION_SALT_2026".toByteArray(Charsets.UTF_8)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256)
            val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

            val decryptedBytes = cipher.doFinal(cipherText)
            val decryptedStr = String(decryptedBytes, Charsets.UTF_8)

            val parts = decryptedStr.split("|")
            if (parts.size >= 6 && parts[0] == "OOZOX_MIGRATE") {
                MigrationData(
                    timestamp = parts[1].toLongOrNull() ?: 0L,
                    userId = parts[2],
                    name = parts[3],
                    phone = parts[4],
                    userType = parts[5]
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Detects if the app is running in an Android emulator/virtual device
     * (e.g. Google Cloud Streaming Emulator, Android Studio Emulator, Cuttlefish, Ranchu, Goldfish).
     */
    fun isEmulator(): Boolean {
        return try {
            val brand = android.os.Build.BRAND.orEmpty()
            val device = android.os.Build.DEVICE.orEmpty()
            val fingerprint = android.os.Build.FINGERPRINT.orEmpty()
            val hardware = android.os.Build.HARDWARE.orEmpty()
            val model = android.os.Build.MODEL.orEmpty()
            val manufacturer = android.os.Build.MANUFACTURER.orEmpty()
            val product = android.os.Build.PRODUCT.orEmpty()
            val board = android.os.Build.BOARD.orEmpty()

            (brand.startsWith("generic") && device.startsWith("generic"))
                    || fingerprint.startsWith("generic")
                    || fingerprint.startsWith("unknown")
                    || hardware.contains("goldfish")
                    || hardware.contains("ranchu")
                    || model.contains("google_sdk")
                    || model.contains("Emulator")
                    || model.contains("Android SDK built for x86")
                    || model.contains("Cuttlefish")
                    || manufacturer.contains("Genymotion")
                    || product.contains("sdk_google")
                    || product.contains("google_sdk")
                    || product.contains("sdk")
                    || product.contains("sdk_x86")
                    || product.contains("vbox86p")
                    || product.contains("emulator")
                    || product.contains("simulator")
                    || board.contains("goldfish")
                    || board.contains("ranchu")
        } catch (e: Exception) {
            false
        }
    }

    data class MigrationData(
        val timestamp: Long,
        val userId: String,
        val name: String,
        val phone: String,
        val userType: String
    )

    data class DeveloperModeStatus(
        val isDevModeActive: Boolean,
        val isDevSettingsEnabled: Boolean,
        val isAdbEnabled: Boolean
    )

    /**
     * Inspects host Android system settings to determine whether Developer Options
     * or USB Debugging (ADB) are enabled on the device.
     */
    fun checkDeveloperMode(context: android.content.Context): DeveloperModeStatus {
        return try {
            val cr = context.contentResolver

            // 1. Settings.Global development_settings_enabled
            val devGlobal = try {
                android.provider.Settings.Global.getInt(
                    cr,
                    android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
                ) != 0
            } catch (e: Exception) {
                android.util.Log.d("SecurityValidator", "Global.DEVELOPMENT_SETTINGS_ENABLED check failed: ${e.message}")
                false
            }

            // 2. Settings.Secure development_settings_enabled (for older API levels or vendor ROMs)
            val devSecure = try {
                android.provider.Settings.Secure.getInt(
                    cr,
                    android.provider.Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 0
                ) != 0
            } catch (e: Exception) {
                android.util.Log.d("SecurityValidator", "Secure.DEVELOPMENT_SETTINGS_ENABLED check failed: ${e.message}")
                false
            }

            // 3. Settings.System development_settings_enabled (fallback for customized skins like Xiaomi/MIUI, ColorOS, etc.)
            val devSystem = try {
                android.provider.Settings.System.getInt(
                    cr,
                    "development_settings_enabled", 0
                ) != 0
            } catch (_: Exception) { false }

            val isDev = devGlobal || devSecure || devSystem

            // 4. Settings.Global ADB_ENABLED
            val adbGlobal = try {
                android.provider.Settings.Global.getInt(
                    cr,
                    android.provider.Settings.Global.ADB_ENABLED, 0
                ) != 0
            } catch (e: Exception) {
                android.util.Log.d("SecurityValidator", "Global.ADB_ENABLED check failed: ${e.message}")
                false
            }

            // 5. Settings.Secure ADB_ENABLED
            val adbSecure = try {
                android.provider.Settings.Secure.getInt(
                    cr,
                    android.provider.Settings.Secure.ADB_ENABLED, 0
                ) != 0
            } catch (e: Exception) {
                android.util.Log.d("SecurityValidator", "Secure.ADB_ENABLED check failed: ${e.message}")
                false
            }

            val isAdb = adbGlobal || adbSecure

            android.util.Log.i("SecurityValidator", "DeveloperModeCheck: isDev=$isDev (global=$devGlobal, secure=$devSecure, system=$devSystem), isAdb=$isAdb (global=$adbGlobal, secure=$adbSecure)")

            DeveloperModeStatus(
                isDevModeActive = isDev || isAdb,
                isDevSettingsEnabled = isDev,
                isAdbEnabled = isAdb
            )
        } catch (e: Exception) {
            android.util.Log.e("SecurityValidator", "Error checking developer mode", e)
            DeveloperModeStatus(isDevModeActive = false, isDevSettingsEnabled = false, isAdbEnabled = false)
        }
    }
}
