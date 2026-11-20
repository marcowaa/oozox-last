package com.example.util

import com.example.data.model.HashtagEntryEntity
import com.example.data.model.LedgerEntryEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Severity level of an audit finding.
 */
enum class AuditSeverity(val label: String, val icon: String) {
    INFO("معلومات توثيقية", "ℹ️"),
    WARNING("تنبيه تدقيق محاسبي", "⚠️"),
    CRITICAL("شذوذ مالي خطر", "🚨")
}

/**
 * Single anomaly or audit item identified by the smart auditor.
 */
data class AuditAnomaly(
    val title: String,
    val description: String,
    val severity: AuditSeverity,
    val suggestedFix: String? = null,
    val affectedField: String? = null
)

/**
 * Complete audit verification result for any entry before it is committed.
 */
data class AuditVerificationReport(
    val isApproved: Boolean, // true if safe to commit directly (no critical blocking anomaly)
    val hasWarnings: Boolean,
    val score: Int, // 0 to 100
    val summaryTitle: String,
    val summaryDescription: String,
    val anomalies: List<AuditAnomaly>,
    val verifiedAmount: Double,
    val verifiedWeight: Double,
    val verifiedKarat: Int?,
    val calculatedDiscrepancy: Double = 0.0,
    val duplicateRiskDetected: Boolean = false,
    val verificationAuditHash: String
)

/**
 * The Smart Audit & Anomaly Detection Engine.
 * Provides strict pre-commit verification for accounting entries, voice transactions,
 * and hashtag ledgers to eliminate accounting mistakes, outlier values, karat mismatches,
 * price discrepancies, and accidental duplicate submissions.
 */
object SmartAuditEngine {

    // Valid gold karats commonly accepted in the market
    private val VALID_GOLD_KARATS = setOf(9, 12, 14, 18, 21, 22, 24, 925, 999)

    // Gold price sanity bounds per gram (EGP)
    private const val MIN_GOLD_PRICE_PER_GRAM = 1500.0
    private const val MAX_GOLD_PRICE_PER_GRAM = 12000.0

    // Iron price sanity bounds per ton (EGP)
    private const val MIN_IRON_PRICE_PER_TON = 15000.0
    private const val MAX_IRON_PRICE_PER_TON = 90000.0

    // Outlier single transaction threshold (EGP)
    private const val EXTREME_AMOUNT_THRESHOLD = 2000000.0

    /**
     * Audit a parsed voice accounting record or manual entry before database insertion.
     */
    fun auditEntry(
        record: ParsedVoiceAccountingRecord,
        recentEntries: List<HashtagEntryEntity> = emptyList(),
        recentLedgers: List<LedgerEntryEntity> = emptyList()
    ): AuditVerificationReport {
        val anomalies = mutableListOf<AuditAnomaly>()
        var score = 100

        val tag = record.suggestedTag.lowercase(Locale.getDefault())
        val amount = record.amount
        val weight = record.weightGrams
        val karat = record.karat
        val unitPrice = record.unitPrice
        val rawText = record.rawSpokenText

        // 1. Check Karat Sanity (Gold / Silver)
        if (tag.contains("ذهب") || tag.contains("كسر") || tag.contains("سبيكة")) {
            if (karat != null && !VALID_GOLD_KARATS.contains(karat)) {
                anomalies.add(
                    AuditAnomaly(
                        title = "عيار ذهب غير قياسي ($karat)",
                        description = "العيارات المعترف بها في سوق الصاغة هي (18، 21، 24، 22، 14، 9). العيار المذكور ($karat) يبدو غريباً أو خطأ في النطق.",
                        severity = AuditSeverity.CRITICAL,
                        suggestedFix = "تعديل العيار إلى 21 أو 18 أو 24",
                        affectedField = "karat"
                    )
                )
                score -= 35
            }
        }

        // 2. Check Unit Price vs Market Bounds
        if (tag.contains("ذهب") && unitPrice != null && unitPrice > 0.0) {
            if (unitPrice < MIN_GOLD_PRICE_PER_GRAM || unitPrice > MAX_GOLD_PRICE_PER_GRAM) {
                anomalies.add(
                    AuditAnomaly(
                        title = "سعر جرام الذهب خارج النطاق الطبيعي (${String.format(Locale.US, "%,.0f", unitPrice)} ج.م)",
                        description = "السعر المسجل للجرام (${String.format(Locale.US, "%,.0f", unitPrice)} ج.م) يبتعد كثيراً عن أسعار البورصة الحالية (المتوسط بين 3,000 إلى 5,000 ج.م).",
                        severity = AuditSeverity.WARNING,
                        suggestedFix = "مراجعة سعر الجرام قبل تثبيت القيد",
                        affectedField = "unitPrice"
                    )
                )
                score -= 20
            }
        } else if (tag.contains("حديد") && unitPrice != null && unitPrice > 0.0) {
            if (unitPrice < MIN_IRON_PRICE_PER_TON || unitPrice > MAX_IRON_PRICE_PER_TON) {
                anomalies.add(
                    AuditAnomaly(
                        title = "سعر طن الحديد خارج النطاق الطبيعي (${String.format(Locale.US, "%,.0f", unitPrice)} ج.م)",
                        description = "السعر المسجل للطن يبتعد عن المعدل السوقي لحديد التسليح (35,000 إلى 50,000 ج.م).",
                        severity = AuditSeverity.WARNING,
                        suggestedFix = "التأكد من سعر الطن",
                        affectedField = "unitPrice"
                    )
                )
                score -= 15
            }
        }

        // 3. Check Mathematical Discrepancy (Weight * UnitPrice vs Total Amount)
        var discrepancy = 0.0
        if (weight > 0.0 && unitPrice != null && unitPrice > 0.0 && amount > 0.0) {
            val expectedTotal = BigDecimal.valueOf(weight).multiply(BigDecimal.valueOf(unitPrice)).setScale(2, RoundingMode.HALF_UP).toDouble()
            discrepancy = kotlin.math.abs(expectedTotal - amount)
            if (discrepancy > 10.0) { // Tolerating small rounding differences up to 10 EGP
                anomalies.add(
                    AuditAnomaly(
                        title = "فارق حسابي بين الوزن والسعر والإجمالي",
                        description = "حاصل ضرب (الوزن $weight × السعر ${String.format(Locale.US, "%,.0f", unitPrice)}) = ${String.format(Locale.US, "%,.2f", expectedTotal)} ج.م، بينما الإجمالي المسجل هو ${String.format(Locale.US, "%,.2f", amount)} ج.م (فارق: ${String.format(Locale.US, "%,.2f", discrepancy)} ج.م).",
                        severity = AuditSeverity.CRITICAL,
                        suggestedFix = "تعديل الإجمالي ليكون ${String.format(Locale.US, "%,.2f", expectedTotal)} ج.م",
                        affectedField = "amount"
                    )
                )
                score -= 30
            }
        }

        // 4. Duplicate Entry Detection (Within last 15 minutes)
        val now = System.currentTimeMillis()
        val fifteenMinutesAgo = now - (15 * 60 * 1000)
        val recentDuplicate = recentEntries.find {
            kotlin.math.abs(it.amount - amount) < 0.01 &&
                    it.createdAt >= fifteenMinutesAgo &&
                    it.type == record.direction
        }
        var duplicateRisk = false
        if (recentDuplicate != null && amount > 0.0) {
            duplicateRisk = true
            anomalies.add(
                AuditAnomaly(
                    title = "احتمال تكرار القيد المحاسبي ⚠️",
                    description = "تم تسجيل قيد مطابق بنفس القيمة (${String.format(Locale.US, "%,.2f", amount)} ج.م) منذ بضع دقائق. يرجى التحقق حتى لا يتم إدراج القيد مرتين بالخطأ.",
                    severity = AuditSeverity.WARNING,
                    suggestedFix = "التأكد مما إذا كانت هذه معاملة جديدة منفصلة",
                    affectedField = "duplicate"
                )
            )
            score -= 25
        }

        // 5. Outlier & Extreme Value Check
        if (amount >= EXTREME_AMOUNT_THRESHOLD) {
            anomalies.add(
                AuditAnomaly(
                    title = "مبلغ استثنائي ضخم (${String.format(Locale.US, "%,.2f", amount)} ج.م)",
                    description = "المبلغ يتجاوز حاجز مليوني جنيه مصري. يتطلب تدقيقاً مزدوجاً للتأكد من صحة عدد الأصفار.",
                    severity = AuditSeverity.WARNING,
                    suggestedFix = "مراجعة الأصفار والتأكيد",
                    affectedField = "amount"
                )
            )
            score -= 10
        }

        // 6. Zero Amount Warning
        if (amount <= 0.0 && weight <= 0.0) {
            anomalies.add(
                AuditAnomaly(
                    title = "قيد بدون قيمة مالية أو وزن محدد",
                    description = "لم يتم التعرف على مبلغ مالي أو وزن في نص القيد. يرجى كتابة أو نطق القيمة بالأرقام.",
                    severity = AuditSeverity.CRITICAL,
                    suggestedFix = "إضافة المبلغ أو الوزن",
                    affectedField = "amount"
                )
            )
            score -= 40
        }

        // 7. Conflicting Financial Direction
        val rawLower = rawText.lowercase(Locale.getDefault())
        if ((rawLower.contains("قبض") || rawLower.contains("استلمنا")) &&
            (rawLower.contains("دفعنا") || rawLower.contains("سحب") || rawLower.contains("مصروف"))) {
            anomalies.add(
                AuditAnomaly(
                    title = "تناقض في اتجاه القيد (دائن / مدين)",
                    description = "النص يحتوي على مصطلحات قبض ودفع في نفس العبارة. تم اعتماد اتجاه (${if (record.direction == "CREDIT") "سداد / دائن" else "مشتريات / مدين"}).",
                    severity = AuditSeverity.WARNING,
                    suggestedFix = "تأكيد هل الحركة واردة (قبض) أم صادرة (دفع)",
                    affectedField = "direction"
                )
            )
            score -= 15
        }

        val hasCritical = anomalies.any { it.severity == AuditSeverity.CRITICAL }
        val hasWarnings = anomalies.any { it.severity == AuditSeverity.WARNING }
        val finalScore = score.coerceIn(0, 100)

        val summaryTitle = when {
            hasCritical -> "🚨 تنبيه تدقيق حرج: تم رصد شذوذ في الأرقام"
            hasWarnings -> "⚠️ تنبيه مراجعة: القيد يحتاج تأكيداً منك"
            else -> "✅ تدقيق مكتمل: القيد سليم ومتطابق 100%"
        }

        val summaryDesc = when {
            hasCritical -> "اكتشف الوكيل الذكي أخطاء أو شذوذاً في تفاصيل القيد. يُنصح بتصحيحها قبل الإدراج."
            hasWarnings -> "تمت مطابقة القيد مع وجود ملاحظات تحذيرية للتأكيد لضمان أعلى درجات الدقة."
            else -> "تم فحص العيار، الأسعار، الحسابات، وعدم التكرار بنجاح تام وفق المعايير المالية."
        }

        val auditHash = generateAuditSeal(record, finalScore)

        return AuditVerificationReport(
            isApproved = !hasCritical,
            hasWarnings = hasWarnings || hasCritical,
            score = finalScore,
            summaryTitle = summaryTitle,
            summaryDescription = summaryDesc,
            anomalies = anomalies,
            verifiedAmount = amount,
            verifiedWeight = weight,
            verifiedKarat = karat,
            calculatedDiscrepancy = discrepancy,
            duplicateRiskDetected = duplicateRisk,
            verificationAuditHash = auditHash
        )
    }

    private fun generateAuditSeal(record: ParsedVoiceAccountingRecord, score: Int): String {
        val payload = "${record.suggestedTag}|${record.amount}|${record.weightGrams}|${record.direction}|${System.currentTimeMillis()}|$score"
        return "AUDIT-" + payload.hashCode().toString().replace("-", "X").takeLast(8).uppercase(Locale.US)
    }
}
