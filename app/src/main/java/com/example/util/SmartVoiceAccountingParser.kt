package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.util.regex.Pattern

/**
 * Data structure representing a structured accounting record parsed from spoken or written input.
 */
data class ParsedVoiceAccountingRecord(
    val rawSpokenText: String = "",
    val suggestedTag: String = "#معاملة",
    val amount: Double = 0.0,
    val weightGrams: Double = 0.0,
    val karat: Int? = null,
    val unitPrice: Double? = null,
    val direction: String = "CREDIT", // "CREDIT" (له / سداد / مقبوضات) or "DEBIT" (عليه / مشتريات / مصروفات)
    val counterPartyName: String = "",
    val operationCategory: String = "عام", // "ذهب", "حديد", "فضة", "نقدية", "مصروفات", "توريد", "عام"
    val notes: String = "",
    val confidenceScore: Float = 1.0f, // 0.0 to 1.0
    val formattedHashtagMessage: String = "",
    val spokenConfirmationText: String = "",
    val balanceImpactText: String = ""
)

/**
 * Intelligent parser that transforms Arabic spoken phrases and written text
 * into accurate, structured accounting transactions with automatic hashtag detection,
 * number parsing (spoken words to digits), weight, karat, and direction recognition.
 */
object SmartVoiceAccountingParser {

    private val ARABIC_DIGIT_MAP = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    private val ARABIC_WORD_NUMBERS = mapOf(
        "صفر" to 0.0,
        "واحد" to 1.0,
        "واحدة" to 1.0,
        "اثنين" to 2.0,
        "اثنان" to 2.0,
        "ثلاثة" to 3.0,
        "ثلاث" to 3.0,
        "اربعة" to 4.0,
        "أربعة" to 4.0,
        "اربع" to 4.0,
        "أربع" to 4.0,
        "خمسة" to 5.0,
        "خمس" to 5.0,
        "ستة" to 6.0,
        "ست" to 6.0,
        "سبعة" to 7.0,
        "سبع" to 7.0,
        "ثمانية" to 8.0,
        "ثماني" to 8.0,
        "ثمان" to 8.0,
        "تسعة" to 9.0,
        "تسع" to 9.0,
        "عشرة" to 10.0,
        "عشر" to 10.0,
        "عشرين" to 20.0,
        "عشرون" to 20.0,
        "ثلاثين" to 30.0,
        "ثلاثون" to 30.0,
        "اربعين" to 40.0,
        "أربعين" to 40.0,
        "خمسين" to 50.0,
        "خمسون" to 50.0,
        "ستين" to 60.0,
        "ستون" to 60.0,
        "سبعين" to 70.0,
        "سبعون" to 70.0,
        "ثمانين" to 80.0,
        "ثمانون" to 80.0,
        "تسعين" to 90.0,
        "تسعون" to 90.0,
        "مائة" to 100.0,
        "مية" to 100.0,
        "ميه" to 100.0,
        "مئتان" to 200.0,
        "ميتين" to 200.0,
        "ثلاثمائة" to 300.0,
        "خمسمائة" to 500.0,
        "الف" to 1000.0,
        "ألف" to 1000.0,
        "الفين" to 2000.0,
        "ألفين" to 2000.0,
        "الاف" to 1000.0,
        "آلاف" to 1000.0,
        "مليون" to 1000000.0,
        "ملايين" to 1000000.0,
        "نصف" to 0.5,
        "نص" to 0.5,
        "ربع" to 0.25,
        "تلت" to 0.333,
        "ثلث" to 0.333
    )

    private val SPOKEN_HASHTAG_TRIGGER_REGEX = Regex(
        "(?:^|\\s)(?:هاشتاج|هاشتاغ|هشتاج|هشتاغ|هاش\\s+تاج|وسم|علامة\\s+الشباك|شباك|hashtag)(?:\\s+(?:جديد|اسمه|باسم|بعنوان))?\\s+(?!\\d+(?:[.,]\\d+)?(?:\\s|$))(#?[\\u0600-\\u06FF\\w_]+)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Converts spoken phrases like "هاشتاج توريد" or "وسم مصروفات" into "#توريد" / "#مصروفات".
     */
    fun normalizeSpokenHashtags(rawInput: String): String {
        if (rawInput.isBlank()) return rawInput
        val transformed = SPOKEN_HASHTAG_TRIGGER_REGEX.replace(rawInput) { match ->
            val rawWord = match.groupValues[1].trim().removePrefix("#")
            val cleanWord = rawWord.replace(Regex("[^\\u0600-\\u06FF\\w_]"), "")
            val prefixSpace = if (match.value.startsWith(" ")) " " else ""
            if (cleanWord.isNotBlank()) "${prefixSpace}#$cleanWord" else match.value
        }
        return transformed.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Parse any spoken text or raw input into structured accounting data.
     */
    fun parseVoiceAccountingText(rawInput: String, defaultContactName: String = ""): ParsedVoiceAccountingRecord {
        val withNormalizedTags = normalizeSpokenHashtags(rawInput)
        val normalized = normalizeArabicText(withNormalizedTags)

        // 1. Detect Category & Tag
        val (category, suggestedTag) = detectCategoryAndTag(normalized)

        // 2. Detect Karat (for gold/jewelry)
        val karat = detectKarat(normalized)

        // 3. Detect Weight / Quantity
        val weight = detectWeight(normalized)

        // 4. Detect Unit Price
        val unitPrice = detectUnitPrice(normalized)

        // 5. Detect Direction (Credit vs Debit)
        val direction = detectDirection(normalized)

        // 6. Detect Total Amount
        var amount = detectAmount(normalized)

        // If amount was not explicitly mentioned, but weight and unitPrice exist, compute total
        if (amount <= 0.0 && weight > 0.0 && unitPrice != null && unitPrice > 0.0) {
            amount = BigDecimal.valueOf(weight)
                .multiply(BigDecimal.valueOf(unitPrice))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()
        }

        // 7. Detect Counterparty Name
        val counterParty = detectCounterParty(normalized, defaultContactName)

        // 8. Generate Notes & Formatted Hashtag Message
        val notes = generateCleanNotes(withNormalizedTags, counterParty, suggestedTag)
        val formattedMessage = formatHashtagMessage(
            tag = suggestedTag,
            amount = amount,
            weight = weight,
            karat = karat,
            unitPrice = unitPrice,
            direction = direction,
            counterParty = counterParty,
            notes = notes
        )

        // 9. Generate Spoken Confirmation (for TTS listening) & Balance Impact Text
        val spokenText = generateSpokenConfirmation(
            tag = suggestedTag,
            amount = amount,
            weight = weight,
            karat = karat,
            unitPrice = unitPrice,
            direction = direction,
            counterParty = counterParty.ifBlank { defaultContactName }
        )
        val balanceImpact = generateBalanceImpactText(
            amount = amount,
            weight = weight,
            karat = karat,
            direction = direction,
            tag = suggestedTag
        )

        // 10. Compute Confidence Score
        var confidence = 0.5f
        if (suggestedTag.isNotBlank()) confidence += 0.15f
        if (amount > 0.0) confidence += 0.2f
        if (weight > 0.0) confidence += 0.1f
        if (counterParty.isNotBlank()) confidence += 0.05f

        return ParsedVoiceAccountingRecord(
            rawSpokenText = withNormalizedTags,
            suggestedTag = suggestedTag,
            amount = amount,
            weightGrams = weight,
            karat = karat,
            unitPrice = unitPrice,
            direction = direction,
            counterPartyName = counterParty.ifBlank { defaultContactName },
            operationCategory = category,
            notes = notes,
            confidenceScore = confidence.coerceIn(0.1f, 1.0f),
            formattedHashtagMessage = formattedMessage,
            spokenConfirmationText = spokenText,
            balanceImpactText = balanceImpact
        )
    }

    private fun normalizeArabicText(input: String): String {
        var res = input
        // Replace Arabic digits with standard ASCII digits
        ARABIC_DIGIT_MAP.forEach { (ar, en) ->
            res = res.replace(ar, en)
        }
        return res
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace("،", " ")
            .replace("  ", " ")
            .trim()
    }

    private fun detectCategoryAndTag(text: String): Pair<String, String> {
        val lower = text.lowercase(Locale.getDefault())

        // Check if there's an explicit hashtag already
        val explicitTagRegex = Regex("#([\\u0600-\\u06FF\\w]+)")
        val tagMatch = explicitTagRegex.find(text)
        if (tagMatch != null) {
            val explicit = tagMatch.groupValues[1].trim()
            return when {
                explicit.contains("ذهب") -> "ذهب" to "#$explicit"
                explicit.contains("حديد") -> "حديد" to "#$explicit"
                explicit.contains("فضة") -> "فضة" to "#$explicit"
                explicit.contains("قبض") || explicit.contains("استلام") -> "نقدية" to "#$explicit"
                explicit.contains("صرف") || explicit.contains("مصروف") -> "مصروفات" to "#$explicit"
                explicit.contains("مبيعات") || explicit.contains("بيع") -> "مبيعات" to "#$explicit"
                explicit.contains("مشتريات") || explicit.contains("شراء") -> "مشتريات" to "#$explicit"
                explicit.contains("سداد") || explicit.contains("دفعة") -> "نقدية" to "#$explicit"
                explicit.contains("توريد") -> "توريد" to "#$explicit"
                explicit.contains("خصم") -> "خصم" to "#$explicit"
                else -> "عام" to "#$explicit"
            }
        }

        return when {
            lower.contains("استلم") || lower.contains("قبض") || lower.contains("مقبوضات") || lower.contains("ايداع") || lower.contains("إيداع") -> {
                "نقدية" to "#قبض"
            }
            lower.contains("صرف") || lower.contains("مصروف") || lower.contains("مصاريف") || lower.contains("بوفيه") ||
                    lower.contains("ايجار") || lower.contains("رواتب") || lower.contains("نقل") || lower.contains("بنزين") || lower.contains("كهرباء") -> {
                "مصروفات" to "#صرف"
            }
            lower.contains("ذهب") || lower.contains("كسر") || lower.contains("سبيكة") || lower.contains("سبائك") ||
                    lower.contains("غوايش") || lower.contains("خاتم") || lower.contains("عيار") -> {
                "ذهب" to "#ذهب"
            }
            lower.contains("مبيعات") || lower.contains("بعت") || lower.contains("بيع") -> {
                "مبيعات" to "#مبيعات"
            }
            lower.contains("مشتريات") || lower.contains("اشتريت") || lower.contains("شراء") -> {
                "مشتريات" to "#مشتريات"
            }
            lower.contains("سداد") || lower.contains("سددت") || lower.contains("سددنا") || lower.contains("دفعة") || lower.contains("تحويل") -> {
                "نقدية" to "#سداد"
            }
            lower.contains("حديد") || lower.contains("تسليح") || lower.contains("عز") || lower.contains("طن") -> {
                "حديد" to "#حديد"
            }
            lower.contains("فضة") || lower.contains("ايطالي") || lower.contains("عيار 925") -> {
                "فضة" to "#فضة"
            }
            lower.contains("خصم") || lower.contains("تخفيض") -> {
                "خصم" to "#خصم"
            }
            lower.contains("توريد") || lower.contains("بضاعة") || lower.contains("شحنة") -> {
                "توريد" to "#توريد"
            }
            else -> {
                "عام" to "#قيد_عام"
            }
        }
    }

    private fun detectKarat(text: String): Int? {
        val karatRegex = Regex("(?:عيار|قيراط|درجة)\\s*(\\d{1,2})")
        val match = karatRegex.find(text)
        if (match != null) {
            val num = match.groupValues[1].toIntOrNull()
            if (num != null) return num
        }
        // Check isolated karat numbers in gold context
        if (text.contains("ذهب") || text.contains("كسر")) {
            if (text.contains("24") || text.contains("اربعة وعشرين") || text.contains("أربعة وعشرين")) return 24
            if (text.contains("21") || text.contains("واحد وعشرين")) return 21
            if (text.contains("18") || text.contains("تمانتاشر") || text.contains("ثمانية عشر")) return 18
            if (text.contains("22")) return 22
            if (text.contains("14")) return 14
            if (text.contains("925")) return 925
        }
        return null
    }

    private fun detectWeight(text: String): Double {
        // Match numbers before or after جرام / طن / كيلو
        val weightRegex = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:جرام|جم|غرام|طن|كيلو|كجم)")
        val match = weightRegex.find(text)
        if (match != null) {
            val raw = match.groupValues[1].replace(",", ".")
            return raw.toDoubleOrNull() ?: 0.0
        }

        val reverseWeightRegex = Regex("(?:جرام|جم|غرام|وزن|طن|كمية)\\s*(\\d+(?:[.,]\\d+)?)")
        val revMatch = reverseWeightRegex.find(text)
        if (revMatch != null) {
            val raw = revMatch.groupValues[1].replace(",", ".")
            return raw.toDoubleOrNull() ?: 0.0
        }

        // Look for spoken weight phrases like "خمسين جرام"
        ARABIC_WORD_NUMBERS.forEach { (word, value) ->
            if (text.contains("$word جرام") || text.contains("$word غرام") || text.contains("$word طن")) {
                return value
            }
        }

        return 0.0
    }

    private fun detectUnitPrice(text: String): Double? {
        val priceRegex = Regex("(?:سعر|بسعر|الجرام ب|الطن ب|علي|على)\\s*(\\d+(?:[.,]\\d+)?)")
        val match = priceRegex.find(text)
        if (match != null) {
            val raw = match.groupValues[1].replace(",", ".")
            return raw.toDoubleOrNull()
        }
        return null
    }

    private fun detectDirection(text: String): String {
        val lower = text.lowercase(Locale.getDefault())
        val creditKeywords = listOf("قبض", "استلم", "استلام", "مقبوضات", "سداد", "دفعة", "تحويل", "له", "قبضنا", "استلمنا", "دائن", "وارد", "إيداع", "ايداع", "مبيعات", "بعت", "كاش وارد", "تحصيل")
        val debitKeywords = listOf("صرف", "صرفت", "دفعت", "عليه", "مشتريات", "اشتريت", "اشترينا", "توريد", "مصروف", "مصاريف", "سحب", "مدين", "صادر", "خصم", "مطلوب منه", "بوفيه", "ايجار", "كهرباء", "بنزين", "رواتب")

        val hasCredit = creditKeywords.any { lower.contains(it) }
        val hasDebit = debitKeywords.any { lower.contains(it) }

        return if (hasCredit && !hasDebit) {
            "CREDIT"
        } else if (hasDebit && !hasCredit) {
            "DEBIT"
        } else if (hasDebit && hasCredit) {
            if (lower.startsWith("صرف") || lower.startsWith("دفع") || lower.startsWith("اشتر")) "DEBIT" else "CREDIT"
        } else {
            "CREDIT"
        }
    }

    private fun detectAmount(text: String): Double {
        // Look for explicitly labeled amounts like "مبلغ 50000" or "بمبلغ 50,000 جنيه" or "50000 ج"
        val amountRegex = Regex("(?:مبلغ|قيمة|اجمالي|إجمالي|فلوس|جنيه|ج\\.م|ريال|دولار|دفعة|بـ|ب)\\s*(?:قدره|قدرها)?\\s*(\\d+(?:[.,]\\d+)?)")
        val match = amountRegex.find(text)
        if (match != null) {
            val raw = match.groupValues[1].replace(",", ".")
            val amt = raw.toDoubleOrNull()
            if (amt != null && amt > 0.0) return amt
        }

        // Spoken thousands & millions
        if (text.contains("مليون")) return 1000000.0
        if (text.contains("نص مليون") || text.contains("نصف مليون")) return 500000.0
        if (text.contains("ميتين الف") || text.contains("مئتين ألف") || text.contains("ميتين ألف")) return 200000.0
        if (text.contains("مية الف") || text.contains("مائة ألف") || text.contains("ميه الف")) return 100000.0
        if (text.contains("تسعين الف") || text.contains("تسعين ألف")) return 90000.0
        if (text.contains("ثمانين الف") || text.contains("ثمانين ألف")) return 80000.0
        if (text.contains("سبعين الف") || text.contains("سبعين ألف")) return 70000.0
        if (text.contains("ستين الف") || text.contains("ستين ألف")) return 60000.0
        if (text.contains("خمسين الف") || text.contains("خمسين ألف")) return 50000.0
        if (text.contains("أربعين الف") || text.contains("اربعين الف") || text.contains("أربعين ألف")) return 40000.0
        if (text.contains("ثلاثين الف") || text.contains("ثلاثين ألف")) return 30000.0
        if (text.contains("خمسة وعشرين الف") || text.contains("خمسة وعشرين ألف")) return 25000.0
        if (text.contains("عشرين الف") || text.contains("عشرين ألف")) return 20000.0
        if (text.contains("خمسة عشر الف") || text.contains("خمسطاشر الف") || text.contains("خمسة عشر ألف")) return 15000.0
        if (text.contains("عشرة الاف") || text.contains("عشرة آلاف") || text.contains("عشر الاف")) return 10000.0
        if (text.contains("تسعة الاف") || text.contains("تسعة آلاف")) return 9000.0
        if (text.contains("ثمانية الاف") || text.contains("ثمانية آلاف")) return 8000.0
        if (text.contains("سبعة الاف") || text.contains("سبعة آلاف")) return 7000.0
        if (text.contains("ستة الاف") || text.contains("ستة آلاف")) return 6000.0
        if (text.contains("خمسة الاف") || text.contains("خمسة آلاف") || text.contains("خمس الاف")) return 5000.0
        if (text.contains("اربعة الاف") || text.contains("أربعة آلاف")) return 4000.0
        if (text.contains("ثلاثة الاف") || text.contains("ثلاثة آلاف")) return 3000.0
        if (text.contains("الفين") || text.contains("ألفين")) return 2000.0
        if (text.contains("الف وخمسمية") || text.contains("ألف وخمسمائة")) return 1500.0
        if (text.contains("الف") || text.contains("ألف")) return 1000.0
        if (text.contains("خمسمية") || text.contains("خمسمائة")) return 500.0
        if (text.contains("ميتين وخمسين")) return 250.0
        if (text.contains("ميتين") || text.contains("مئتان")) return 200.0

        // Look for general numbers in text (excluding known weights or karats)
        val allNumbers = Regex("(\\d+(?:[.,]\\d+)?)").findAll(text)
        for (m in allNumbers) {
            val raw = m.groupValues[1].replace(",", ".")
            val num = raw.toDoubleOrNull() ?: continue
            // Skip typical karats
            if (num == 18.0 || num == 21.0 || num == 24.0 || num == 22.0 || num == 14.0 || num == 9.0) continue
            if (num >= 50.0) {
                return num
            }
        }

        return 0.0
    }

    private fun detectCounterParty(text: String, fallback: String): String {
        val partyRegex = Regex("(?:للعميل|للاستاذ|للأستاذ|للصايغ|للصائغ|للتاجر|لشركة|للمهندس|من|مع|حساب|طرف)\\s+([\\u0600-\\u06FF\\w\\s]{2,20})")
        val match = partyRegex.find(text)
        if (match != null) {
            val raw = match.groupValues[1].trim()
            // Clean up trailing stop words
            val cleaned = raw.split(" ", "،")
                .take(3)
                .joinToString(" ")
                .replace(Regex("(سعر|بمبلغ|عيار|جرام|طن|دفعة|سداد).*"), "")
                .trim()
            if (cleaned.isNotBlank() && cleaned.length >= 3) {
                return cleaned
            }
        }
        return fallback
    }

    private fun generateCleanNotes(raw: String, counterParty: String, tag: String): String {
        return raw.trim()
            .replace(tag, "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun formatHashtagMessage(
        tag: String,
        amount: Double,
        weight: Double,
        karat: Int?,
        unitPrice: Double?,
        direction: String,
        counterParty: String,
        notes: String
    ): String {
        return buildString {
            append("$tag ")
            if (amount > 0.0) {
                append("${String.format(Locale.US, "%,.2f", amount)} ج.م ")
            }
            if (weight > 0.0) {
                append("• $weight ${if (tag.contains("حديد")) "طن" else "جرام"} ")
            }
            if (karat != null) {
                append("• عيار $karat ")
            }
            if (unitPrice != null && unitPrice > 0.0) {
                append("• سعر ${String.format(Locale.US, "%,.0f", unitPrice)} ")
            }
            val dirLabel = if (direction == "CREDIT") "سداد / دائن 🟢" else "مصروفات / مدين 🔴"
            append("($dirLabel) ")
            if (counterParty.isNotBlank()) {
                append("• طرف: $counterParty ")
            }
            if (notes.isNotBlank() && notes.length > 5) {
                append("\nبيان: $notes")
            }
        }.trim()
    }

    /**
     * Generate spoken Arabic script for Text-to-Speech audio review before execution.
     */
    fun generateSpokenConfirmation(
        tag: String,
        amount: Double,
        weight: Double,
        karat: Int?,
        unitPrice: Double?,
        direction: String,
        counterParty: String
    ): String {
        val isCredit = direction == "CREDIT"
        val dirLabel = if (isCredit) "قبض وتوريد لحسابك" else "صرف ومصروفات"
        val partyStr = if (counterParty.isNotBlank()) "لطرف $counterParty" else ""
        val amountStr = if (amount > 0.0) "بمبلغ ${formatSpokenNumber(amount)} جنيه" else ""
        val goldStr = if (weight > 0.0) "بوزن $weight جرام ${if (karat != null) "عيار $karat" else ""}" else ""
        val impactStr = if (isCredit) {
            "والتأثير المحاسبي هو زيادة رصيدك بمقدار ${formatSpokenNumber(amount)} جنيه"
        } else {
            "والتأثير المحاسبي هو خصم ${formatSpokenNumber(amount)} جنيه من رصيد الصندوق"
        }

        return buildString {
            append("القيد المحاسبي المقترح: عملية $dirLabel ")
            if (partyStr.isNotBlank()) append("$partyStr ")
            if (amountStr.isNotBlank()) append("$amountStr ")
            if (goldStr.isNotBlank()) append("$goldStr ")
            append("برمز هاشتاج $tag. ")
            if (amount > 0.0) append("$impactStr. ")
            append("هل تود تنفيذ القيد وإرسال الرسالة الآن؟")
        }.trim()
    }

    /**
     * Generate clear balance impact text for UI verification card before execution.
     */
    fun generateBalanceImpactText(
        amount: Double,
        weight: Double,
        karat: Int?,
        direction: String,
        tag: String
    ): String {
        val isCredit = direction == "CREDIT"
        val sign = if (isCredit) "+" else "-"
        val label = if (isCredit) "زيادة في الرصيد النقدي" else "خصم من الرصيد النقدي"
        val note = if (isCredit) "(إيداع / سداد / تحصيل)" else "(صرف / مصروفات / مشتريات)"

        return buildString {
            if (amount > 0.0) {
                append("$label: $sign${String.format(Locale.US, "%,.2f", amount)} ج.م $note")
            }
            if (weight > 0.0) {
                if (isNotEmpty()) append(" | ")
                val goldSign = if (isCredit) "+" else "-"
                append("رصيد الذهب: $goldSign$weight جم ${if (karat != null) "عيار $karat" else ""}")
            }
            if (isEmpty()) {
                append("قيد محاسبي توثيقي بدون تأثير على السيولة النقدية")
            }
        }
    }

    fun formatSpokenNumber(amount: Double): String {
        return when {
            amount == 1000.0 -> "ألف"
            amount == 2000.0 -> "ألفين"
            amount == 5000.0 -> "خمسة آلاف"
            amount == 10000.0 -> "عشرة آلاف"
            amount == 20000.0 -> "عشرين ألف"
            amount == 50000.0 -> "خمسين ألف"
            amount == 100000.0 -> "مائة ألف"
            amount == 500000.0 -> "نصف مليون"
            amount == 1000000.0 -> "مليون"
            amount % 1000 == 0.0 -> "${(amount / 1000).toInt()} آلاف"
            else -> String.format(Locale.US, "%,.0f", amount)
        }
    }
}
