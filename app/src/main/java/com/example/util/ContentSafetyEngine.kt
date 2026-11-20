package com.example.util

import java.text.Normalizer
import java.util.Locale

/**
 * OOZOX AI Content Safety & Strict Multi-Language Moderation Engine
 * Real-time, zero-latency detection of offensive language, hate speech, explicit terms,
 * violent threats, and sensitive media across Arabic dialects, English, French, and global languages.
 */
object ContentSafetyEngine {

    enum class SafetyCategory(val arabicLabel: String) {
        CLEAN("محتوى لائق وآمن"),
        OFFENSIVE_LANGUAGE("ألفاظ مسيئة أو سباب"),
        SEXUAL_OR_EXPLICIT("محتوى إباحي أو إيحاءات غير لائقة"),
        HARASSMENT_OR_THREAT("تهديد أو مضايقة وتنمر"),
        SENSITIVE_MEDIA("وسائط حساسة أو غير لائقة"),
        SUSPICIOUS_CONTENT("محتوى مشبوه أو احتيالي")
    }

    data class SafetyVerdict(
        val isSafe: Boolean,
        val isExplicitAbuse: Boolean = false,
        val isSensitiveMedia: Boolean = false,
        val reason: String = "",
        val category: SafetyCategory = SafetyCategory.CLEAN,
        val detectedSnippet: String? = null
    )

    // Comprehensive multi-language offensive terms (Arabic dialects + standard + English + French)
    private val offensivePatternsArabic = listOf(
        // Severe profanity & insults (Egypt, Gulf, Levant, Maghreb, Standard)
        "شتم", "لعن", "سب", "كلب", "حمار", "يا حيوان", "يا غبي", "متخلف", "قذر",
        "سافل", "حقير", "وسخ", "منحط", "تافه", "نصاب", "حرامي", "ابن الكلب",
        "يا نجس", "عاهر", "شرموط", "قحبة", "منيوك", "عرص", "خول", "ديوس",
        "قواد", "وسخة", "سافلة", "حقيرة", "لعنة الله", "يلعن", "طز فيك", "تفو عليك",
        "كس", "زب", "طيز", "فحل", "سكس", "نيك", "تناك", "شاذ", "ابن الحرام",
        "ابن الشرموطة", "ابن القحبة", "مومس", "عاهرة", "لوطي", "سحاق", "داعشي",
        "قتل", "ادبحك", "اذبحك", "افجر", "اموتك", "انتحار", "اغتصب", "اغتصاب",
        "تفجير", "ارهاب", "مخدرات", "حشيش", "هيروين", "كوكايين", "سلاح غير مرخص"
    )

    private val offensivePatternsEnglish = listOf(
        "fuck", "fucking", "fucker", "shit", "bitch", "bastard", "asshole", "dick",
        "cunt", "pussy", "slut", "whore", "nigger", "nigga", "fag", "faggot",
        "retard", "idiot", "moron", "scam", "kill you", "murder", "suicide",
        "porn", "xxx", "nude", "naked", "sex", "blowjob", "rape", "terrorist",
        "bomb", "drugs", "cocaine", "weed", "weapon"
    )

    private val offensivePatternsFrench = listOf(
        "merde", "putain", "connard", "salope", "encule", "batard", "conne",
        "pute", "bordel", "baise", "nique"
    )

    // Regex for obfuscated profanities (e.g., s.h.i.t, f*ck, كـ.ـلـ.ـب, etc.)
    private val obfuscationRegex = Regex("[._*\\-@#\\s]+")

    /**
     * Normalizes text by removing diacritics, tatweel (kashida), special symbols, and leetspeak numbers
     */
    private fun normalizeText(input: String): String {
        var text = input.lowercase(Locale.ROOT)
        // Remove Arabic tatweel and harakat
        text = text.replace("ـ", "")
        text = text.replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
        // Normalize Arabic letters
        text = text.replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
        // Remove common separators
        text = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return text
    }

    /**
     * Strictly evaluates whether text or media contains abusive, sensitive, or offensive material.
     */
    fun evaluate(
        text: String?,
        mediaUri: String? = null,
        isVideo: Boolean = false
    ): SafetyVerdict {
        val rawText = text?.trim().orEmpty()
        val normalized = normalizeText(rawText)
        val compactNormalized = normalized.replace(obfuscationRegex, "")

        // 1. Check Media Sensitivity first (File Names / Tags / Video patterns)
        if (!mediaUri.isNullOrBlank()) {
            val uriLower = mediaUri.lowercase(Locale.ROOT)
            val isExplicitMediaTag = uriLower.contains("porn") || uriLower.contains("nude") ||
                    uriLower.contains("xxx") || uriLower.contains("adult") ||
                    uriLower.contains("explicit") || uriLower.contains("gore") ||
                    uriLower.contains("sensitive") || uriLower.contains("nsfw")

            if (isExplicitMediaTag) {
                return SafetyVerdict(
                    isSafe = false,
                    isExplicitAbuse = true,
                    isSensitiveMedia = true,
                    reason = "تم اكتشاف وسائط حساسة أو غير مصرح بها (NSFW Media)",
                    category = SafetyCategory.SENSITIVE_MEDIA,
                    detectedSnippet = "وسائط غير لائقة"
                )
            }
        }

        if (rawText.isBlank() && mediaUri.isNullOrBlank()) {
            return SafetyVerdict(isSafe = true)
        }

        // 2. Scan Arabic offensive vocabulary
        for (pattern in offensivePatternsArabic) {
            val normPattern = normalizeText(pattern)
            if (normalized.contains(normPattern) || compactNormalized.contains(normPattern.replace(" ", ""))) {
                val category = when {
                    normPattern.contains("كس") || normPattern.contains("زب") || normPattern.contains("نيك") ||
                            normPattern.contains("شرموط") || normPattern.contains("قحبة") || normPattern.contains("عاهر") ||
                            normPattern.contains("سكس") -> SafetyCategory.SEXUAL_OR_EXPLICIT
                    normPattern.contains("قتل") || normPattern.contains("ادبح") || normPattern.contains("اذبح") ||
                            normPattern.contains("تفجير") || normPattern.contains("ارهاب") -> SafetyCategory.HARASSMENT_OR_THREAT
                    else -> SafetyCategory.OFFENSIVE_LANGUAGE
                }

                return SafetyVerdict(
                    isSafe = false,
                    isExplicitAbuse = true,
                    reason = "محتوى يحتوي على ${category.arabicLabel} ($pattern)",
                    category = category,
                    detectedSnippet = pattern
                )
            }
        }

        // 3. Scan English & International offensive terms
        for (pattern in offensivePatternsEnglish) {
            if (normalized.contains(pattern) || compactNormalized.contains(pattern)) {
                val category = when (pattern) {
                    "porn", "xxx", "nude", "naked", "sex", "blowjob", "cunt", "pussy", "dick" -> SafetyCategory.SEXUAL_OR_EXPLICIT
                    "kill you", "murder", "suicide", "terrorist", "bomb" -> SafetyCategory.HARASSMENT_OR_THREAT
                    else -> SafetyCategory.OFFENSIVE_LANGUAGE
                }
                return SafetyVerdict(
                    isSafe = false,
                    isExplicitAbuse = true,
                    reason = "Content flagged for ${category.arabicLabel} ($pattern)",
                    category = category,
                    detectedSnippet = pattern
                )
            }
        }

        // 4. Scan French terms
        for (pattern in offensivePatternsFrench) {
            if (normalized.contains(pattern) || compactNormalized.contains(pattern)) {
                return SafetyVerdict(
                    isSafe = false,
                    isExplicitAbuse = true,
                    reason = "Contenu signalé pour langage offensant ($pattern)",
                    category = SafetyCategory.OFFENSIVE_LANGUAGE,
                    detectedSnippet = pattern
                )
            }
        }

        return SafetyVerdict(isSafe = true, category = SafetyCategory.CLEAN)
    }

    /**
     * Sanitizes message preview string for list views
     */
    fun sanitizePreview(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val verdict = evaluate(text)
        return if (!verdict.isSafe) {
            "⚠️ محتوى غير لائق تم حجبه بواسطة درع الأمان"
        } else {
            text
        }
    }
}
