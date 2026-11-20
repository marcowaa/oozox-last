package com.example.util

import android.content.Context
import com.example.data.model.DynamicExcelCustomFieldEntity
import com.example.data.model.DynamicExcelRowRecordEntity
import com.example.data.model.DynamicExcelTemplateEntity
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

/**
 * Result of evaluating a dynamic formula on a row record
 */
data class DynamicFormulaEvaluationResult(
    val fieldKey: String,
    val formulaExpression: String,
    val calculatedValue: Double,
    val humanExplanationAr: String
)

/**
 * Backend engine to evaluate mathematical formulas, handle natural-language-driven
 * column additions, and process dynamic spreadsheet calculations with high BigDecimal precision.
 */
object DynamicBackendFormulaProcessor {

    /**
     * Parses Arabic natural language prompt into a formal mathematical formula definition
     * e.g. "أريد حساب صافي الربح بعد خصم 14% ضريبة وإضافة 1000 حافز"
     */
    fun compileNaturalLanguageToFormula(
        prompt: String,
        targetFieldKey: String,
        targetFieldName: String,
        templateId: String
    ): DynamicExcelCustomFieldEntity {
        val p = prompt.trim()

        var taxPercent = 0.0
        var wastePercent = 0.0
        var discountPercent = 0.0
        var bonusAmount = 0.0

        if (p.contains("ضريب") || p.contains("14%")) {
            val taxMatch = Regex("(\\d+(\\.\\d+)?)\\s*%?\\s*ضريب").find(p)
                ?: Regex("ضريب[\\w\\s]*(\\d+(\\.\\d+)?)\\s*%?").find(p)
            taxPercent = taxMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 14.0
        }

        if (p.contains("هالك") || p.contains("فاقد") || p.contains("كسر")) {
            val wasteMatch = Regex("(\\d+(\\.\\d+)?)\\s*%?\\s*(هالك|فاقد|كسر)").find(p)
                ?: Regex("(هالك|فاقد|كسر)[\\w\\s]*(\\d+(\\.\\d+)?)\\s*%?").find(p)
            wastePercent = wasteMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 5.0
        }

        if (p.contains("خصم")) {
            val discMatch = Regex("(\\d+(\\.\\d+)?)\\s*%?\\s*خصم").find(p)
            discountPercent = discMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        }

        if (p.contains("حافز") || p.contains("مكافأة") || p.contains("إضافة") || p.contains("بونص")) {
            val bonusMatch = Regex("(\\d+)\\s*(جنيه|ج\\.م|حافز|مكافأة)").find(p)
                ?: Regex("(حافز|مكافأة|إضافة)[\\w\\s]*(\\d+)").find(p)
            bonusAmount = bonusMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 500.0
        }

        val expression = buildString {
            append("(revenue - expenses)")
            if (taxPercent > 0) append(" - (revenue * ${taxPercent / 100.0})")
            if (wastePercent > 0) append(" - (revenue * ${wastePercent / 100.0})")
            if (discountPercent > 0) append(" - (revenue * ${discountPercent / 100.0})")
            if (bonusAmount > 0) append(" + $bonusAmount")
        }

        return DynamicExcelCustomFieldEntity(
            id = "field_${System.currentTimeMillis()}_${(1000..9999).random()}",
            templateId = templateId,
            fieldName = targetFieldName,
            fieldKey = targetFieldKey,
            fieldType = "FORMULA",
            naturalLanguagePrompt = prompt,
            generatedFormulaExpression = expression,
            isHiddenBackendCalculation = false,
            evaluationOrder = 1
        )
    }

    /**
     * Evaluates a custom field formula against a given row's values map
     */
    fun evaluateRowFormula(
        field: DynamicExcelCustomFieldEntity,
        values: Map<String, Double>
    ): DynamicFormulaEvaluationResult {
        val rev = values["revenue"] ?: values["total_sales"] ?: values["contract_value"] ?: (values["quantity"] ?: 1.0) * (values["unit_rate"] ?: 0.0)
        val exp = values["expenses"] ?: values["total_cost"] ?: values["direct_expenses"] ?: ((values["materials_cost"] ?: 0.0) + (values["labor_cost"] ?: 0.0))

        val prompt = field.naturalLanguagePrompt ?: ""
        var taxPercent = 0.0
        var wastePercent = 0.0
        var discountPercent = 0.0
        var bonus = 0.0

        if (prompt.contains("ضريب") || prompt.contains("14%")) {
            val taxMatch = Regex("(\\d+(\\.\\d+)?)\\s*%?\\s*ضريب").find(prompt)
            taxPercent = taxMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 14.0
        }
        if (prompt.contains("هالك") || prompt.contains("فاقد")) {
            val wasteMatch = Regex("(\\d+(\\.\\d+)?)\\s*%?\\s*(هالك|فاقد)").find(prompt)
            wastePercent = wasteMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 5.0
        }
        if (prompt.contains("خصم")) {
            val discMatch = Regex("(\\d+(\\.\\d+)?)\\s*%?\\s*خصم").find(prompt)
            discountPercent = discMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        }
        if (prompt.contains("حافز") || prompt.contains("مكافأة") || prompt.contains("إضافة")) {
            val bonusMatch = Regex("(\\d+)\\s*(جنيه|ج\\.م|حافز|مكافأة)").find(prompt)
            bonus = bonusMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 500.0
        }

        val gross = BigDecimal.valueOf(rev).subtract(BigDecimal.valueOf(exp))
        val taxVal = BigDecimal.valueOf(rev).multiply(BigDecimal.valueOf(taxPercent / 100.0))
        val wasteVal = BigDecimal.valueOf(rev).multiply(BigDecimal.valueOf(wastePercent / 100.0))
        val discVal = BigDecimal.valueOf(rev).multiply(BigDecimal.valueOf(discountPercent / 100.0))
        val bonusVal = BigDecimal.valueOf(bonus)

        val finalResult = gross.subtract(taxVal).subtract(wasteVal).subtract(discVal).add(bonusVal)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()

        val explanation = buildString {
            append("= (الإيراد: ${rev.toInt()} - المصروف: ${exp.toInt()})")
            if (taxPercent > 0) append(" - ضريبة ${taxPercent}% (${taxVal.toInt()})")
            if (wastePercent > 0) append(" - هالك ${wastePercent}% (${wasteVal.toInt()})")
            if (discountPercent > 0) append(" - خصم ${discountPercent}% (${discVal.toInt()})")
            if (bonus > 0) append(" + حافز (${bonus.toInt()})")
        }

        return DynamicFormulaEvaluationResult(
            fieldKey = field.fieldKey,
            formulaExpression = field.generatedFormulaExpression ?: "= Formula",
            calculatedValue = finalResult,
            humanExplanationAr = explanation
        )
    }

    /**
     * Evaluates and updates a list of dynamic row entities in the backend
     */
    fun recalculateRowsWithCustomFields(
        rows: List<DynamicExcelRowRecordEntity>,
        customFields: List<DynamicExcelCustomFieldEntity>
    ): List<DynamicExcelRowRecordEntity> {
        return rows.map { row ->
            val valuesMap = mutableMapOf<String, Double>()
            try {
                val jVal = JSONObject(row.valuesJson)
                val keys = jVal.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    valuesMap[k] = jVal.optDouble(k, 0.0)
                }
            } catch (e: Exception) {
                // Ignore parse errors and use empty map
            }

            val calcResultsJson = JSONObject()
            for (field in customFields) {
                val res = evaluateRowFormula(field, valuesMap)
                calcResultsJson.put(field.fieldKey, res.calculatedValue)
                calcResultsJson.put("${field.fieldKey}_expr", res.humanExplanationAr)
            }

            row.copy(
                calculatedResultsJson = calcResultsJson.toString(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
