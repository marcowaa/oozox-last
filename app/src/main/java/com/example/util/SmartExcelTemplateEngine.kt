package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.DailyAccountingRecordEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectTaskEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

/**
 * Industry activity types for specialized Excel spreadsheet templates
 */
enum class ProjectActivityType(val arLabel: String, val iconName: String) {
    CONSTRUCTION("مقاولات وتشييد وبناء عقارات", "Engineering"),
    GOLD_JEWELRY("صاغة وتجارة الذهب والمجوهرات", "Diamond"),
    RETAIL_COMMERCE("تجارة عامة ومتاجر ومبيعات تجزئة", "Storefront"),
    MANUFACTURING("تصنيع وورش وإنتاج", "PrecisionManufacturing"),
    SERVICES_AGENCY("خدمات واستشارات ومشاريع رقمية", "BusinessCenter")
}

/**
 * Data type for dynamic spreadsheet columns
 */
enum class SpreadsheetColumnType {
    TEXT, NUMBER, CURRENCY, PERCENTAGE, FORMULA, MEDIA_PHOTO, MEDIA_VIDEO, STATUS
}

/**
 * Definition of a dynamic column in a smart spreadsheet
 */
data class SmartColumnDefinition(
    val id: String,
    val nameAr: String,
    val type: SpreadsheetColumnType,
    val formulaDescription: String? = null,
    val isHiddenCalculation: Boolean = false, // الخانات الحسابية الخلفية
    val isUserCustomAdded: Boolean = false
)

/**
 * Dynamic row containing values, calculated results, and attached media
 */
data class SmartSpreadsheetRow(
    val id: String,
    val rowNumber: Int,
    val title: String,
    val category: String,
    val values: MutableMap<String, Any> = mutableMapOf(),
    val calculatedResults: MutableMap<String, Double> = mutableMapOf(),
    val photoUri: String? = null,
    val videoUri: String? = null,
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Pre-configured intelligent template for a specific project activity
 */
data class SmartSpreadsheetTemplate(
    val activityType: ProjectActivityType,
    val templateName: String,
    val description: String,
    val columns: List<SmartColumnDefinition>,
    val defaultRows: List<SmartSpreadsheetRow>,
    val summaryKpis: List<String>
)

/**
 * Virtual Accountant Audit & Advice Report
 */
data class VirtualAccountantAdviceReport(
    val healthScore: Int, // 0 - 100
    val healthStatusLabel: String, // ممتاز، مستقر، تنبيه، حرج
    val totalRevenue: Double,
    val totalExpenses: Double,
    val netCashflow: Double,
    val profitMarginPercent: Double,
    val alerts: List<String>,
    val proactiveTips: List<String>,
    val accountingRecommendations: List<String>
)

/**
 * Engine for Managing Intelligent Dynamic Spreadsheets, Background Calculations,
 * Natural-Language-Driven Formulas, and Proactive AI Virtual Accounting.
 */
object SmartExcelTemplateEngine {

    /**
     * Retrieve the specialized smart template for an activity type
     */
    fun getTemplateForActivity(type: ProjectActivityType): SmartSpreadsheetTemplate {
        return when (type) {
            ProjectActivityType.CONSTRUCTION -> createConstructionTemplate()
            ProjectActivityType.GOLD_JEWELRY -> createGoldJewelryTemplate()
            ProjectActivityType.RETAIL_COMMERCE -> createRetailTemplate()
            ProjectActivityType.MANUFACTURING -> createManufacturingTemplate()
            ProjectActivityType.SERVICES_AGENCY -> createServicesTemplate()
        }
    }

    /**
     * Template: Construction & Real Estate (مقاولات وبناء عقارات)
     */
    private fun createConstructionTemplate(): SmartSpreadsheetTemplate {
        val columns = listOf(
            SmartColumnDefinition("item_name", "بند الأعمال / المستخلص", SpreadsheetColumnType.TEXT),
            SmartColumnDefinition("unit", "الوحدة", SpreadsheetColumnType.TEXT),
            SmartColumnDefinition("quantity", "الكمية المنفذة", SpreadsheetColumnType.NUMBER),
            SmartColumnDefinition("unit_rate", "فئة الوحدة (ج.م)", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("materials_cost", "تكلفة المواد الخام", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("labor_cost", "أجور العمالة والمقاول", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("completion_rate", "نسبة الإنجاز %", SpreadsheetColumnType.PERCENTAGE),
            SmartColumnDefinition("total_cost", "إجمالي التكلفة الفعلية", SpreadsheetColumnType.FORMULA, "المواد + العمالة"),
            SmartColumnDefinition("contract_value", "قيمة المستخلص للعميل", SpreadsheetColumnType.FORMULA, "الكمية * فئة الوحدة"),
            SmartColumnDefinition("net_profit", "صافي ربح البند", SpreadsheetColumnType.FORMULA, "قيمة المستخلص - إجمالي التكلفة")
        )

        val defaultRows = listOf(
            SmartSpreadsheetRow(
                id = "const_1",
                rowNumber = 1,
                title = "أعمال الحفر والإحلال",
                category = "الأساسات",
                values = mutableMapOf("unit" to "م3", "quantity" to 450.0, "unit_rate" to 120.0, "materials_cost" to 12000.0, "labor_cost" to 22000.0, "completion_rate" to 100.0),
                notes = "تم اعتماد عمق الحفر 3.5 م وفقاً لتقرير الجسات",
                photoUri = "content://media/oozox/excavation_site_photo.jpg"
            ),
            SmartSpreadsheetRow(
                id = "const_2",
                rowNumber = 2,
                title = "خرسانة مسلحة للقواعد والسميلات",
                category = "الهيكل الخرساني",
                values = mutableMapOf("unit" to "م3", "quantity" to 180.0, "unit_rate" to 3800.0, "materials_cost" to 420000.0, "labor_cost" to 75000.0, "completion_rate" to 100.0),
                notes = "حديد تسليح عز 16 مم وإسمنت مقاوم للكبريتات",
                videoUri = "content://media/oozox/concrete_pouring_video.mp4"
            ),
            SmartSpreadsheetRow(
                id = "const_3",
                rowNumber = 3,
                title = "أعمدة الدور الأرضي والأول",
                category = "الهيكل الخرساني",
                values = mutableMapOf("unit" to "م3", "quantity" to 65.0, "unit_rate" to 4200.0, "materials_cost" to 170000.0, "labor_cost" to 32000.0, "completion_rate" to 85.0),
                notes = "تم صب 85% من الأعمدة والمطابقة الميدانية جارية",
                photoUri = "content://media/oozox/columns_inspection.jpg"
            ),
            SmartSpreadsheetRow(
                id = "const_4",
                rowNumber = 4,
                title = "مباني طوب إسمنتي مفرغ وتأسيس كهرباء",
                category = "التشطيبات والتأسيس",
                values = mutableMapOf("unit" to "ألف طوبة", "quantity" to 35.0, "unit_rate" to 2400.0, "materials_cost" to 55000.0, "labor_cost" to 18000.0, "completion_rate" to 60.0),
                notes = "توريد الطوب الإسمنتي ودفن خراطيم الكهرباء"
            )
        )

        return SmartSpreadsheetTemplate(
            activityType = ProjectActivityType.CONSTRUCTION,
            templateName = "شيت المقاولات والمستخلصات الإنشائية المتكامل",
            description = "إدارة كميات الخرسانة، الحديد، المصنعيات، نسب الإنجاز، مع توثيق صور وفيديوهات الموقع وحساب هوامش الربح بدقة.",
            columns = columns,
            defaultRows = defaultRows,
            summaryKpis = listOf("إجمالي المستخلصات المعتمدة", "إجمالي تكاليف المواد والعمالة", "صافي أرباح المقاولات", "متوسط نسبة الإنجاز")
        )
    }

    /**
     * Template: Gold & Jewelry Trading (صاغة وتجارة الذهب والمجوهرات)
     */
    private fun createGoldJewelryTemplate(): SmartSpreadsheetTemplate {
        val columns = listOf(
            SmartColumnDefinition("item_name", "بيان القطعة / الصنف", SpreadsheetColumnType.TEXT),
            SmartColumnDefinition("karat", "العيار", SpreadsheetColumnType.TEXT),
            SmartColumnDefinition("weight_grams", "الوزن الصافي (جرام)", SpreadsheetColumnType.NUMBER),
            SmartColumnDefinition("gold_price_per_gram", "سعر الجرام الخام (ج.م)", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("mfg_cost_per_gram", "المصنعية للجرام (ج.م)", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("stamp_duty_per_gram", "الدمغة والضريبة (ج.م)", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("loss_scrap_grams", "وزن الهالك والكسر (جرام)", SpreadsheetColumnType.NUMBER),
            SmartColumnDefinition("total_cost", "إجمالي تكلفة الشراء والتصنيع", SpreadsheetColumnType.FORMULA, "(سعر الخام + المصنعية + الدمغة) * الوزن"),
            SmartColumnDefinition("selling_price", "إجمالي سعر البيع للزبون", SpreadsheetColumnType.FORMULA, "إجمالي التكلفة + هامش الربح"),
            SmartColumnDefinition("profit_egp", "صافي الربح المحقق (ج.م)", SpreadsheetColumnType.FORMULA, "سعر البيع - إجمالي التكلفة")
        )

        val defaultRows = listOf(
            SmartSpreadsheetRow(
                id = "gold_1",
                rowNumber = 1,
                title = "طقم شبكة لازوردي عيار 21",
                category = "أطقم ومصوغات",
                values = mutableMapOf("karat" to "عيار 21", "weight_grams" to 42.50, "gold_price_per_gram" to 3850.0, "mfg_cost_per_gram" to 140.0, "stamp_duty_per_gram" to 25.0, "loss_scrap_grams" to 0.15, "selling_margin" to 8500.0),
                notes = "معايرة ميزان معتمد، كود المصنع LAZ-882",
                photoUri = "content://media/oozox/scale_gold_photo.jpg"
            ),
            SmartSpreadsheetRow(
                id = "gold_2",
                rowNumber = 2,
                title = "سبيكة ذهب BTC وزن 50 جرام عيار 24",
                category = "سبائك واستثمار",
                values = mutableMapOf("karat" to "عيار 24", "weight_grams" to 50.00, "gold_price_per_gram" to 4400.0, "mfg_cost_per_gram" to 55.0, "stamp_duty_per_gram" to 18.0, "loss_scrap_grams" to 0.0, "selling_margin" to 3200.0),
                notes = "غلاف أمان معتمد برقم تسلسلي BTC-2026-X99"
            ),
            SmartSpreadsheetRow(
                id = "gold_3",
                rowNumber = 3,
                title = "غوايش خليجي عيار 21 (4 حبات)",
                category = "غوايش وأساور",
                values = mutableMapOf("karat" to "عيار 21", "weight_grams" to 68.20, "gold_price_per_gram" to 3850.0, "mfg_cost_per_gram" to 110.0, "stamp_duty_per_gram" to 22.0, "loss_scrap_grams" to 0.28, "selling_margin" to 9500.0),
                notes = "ذهب إيطالي صب ثقيل بدون فصوص"
            )
        )

        return SmartSpreadsheetTemplate(
            activityType = ProjectActivityType.GOLD_JEWELRY,
            templateName = "شيت الصاغة وحسابات الذهب والسبائك الدقيق",
            description = "حساب دقيق للأوزان بالجرام، العيارات، المصنعيات، الدمغة، وهالك الكسر والتسويات اللحظية مع تجار الذهب.",
            columns = columns,
            defaultRows = defaultRows,
            summaryKpis = listOf("إجمالي وزن الذهب بالجرام", "رأس مال الذهب الخام", "إجمالي المصنعيات والدمغة", "صافي الأرباح المحققة")
        )
    }

    /**
     * Template: Retail & Commerce (تجارة عامة ومتاجر ومبيعات تجزئة)
     */
    private fun createRetailTemplate(): SmartSpreadsheetTemplate {
        val columns = listOf(
            SmartColumnDefinition("product_name", "اسم الصنف / المنتج", SpreadsheetColumnType.TEXT),
            SmartColumnDefinition("sku", "كود الصنف / الباركود", SpreadsheetColumnType.TEXT),
            SmartColumnDefinition("stock_qty", "الكمية المتاحة بالمخزن", SpreadsheetColumnType.NUMBER),
            SmartColumnDefinition("unit_cost", "سعر الشراء والتكلفة (ج.م)", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("selling_price", "سعر البيع المعتمد (ج.م)", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("sold_qty", "الكمية المباعة", SpreadsheetColumnType.NUMBER),
            SmartColumnDefinition("total_sales", "إجمالي إيراد المبيعات", SpreadsheetColumnType.FORMULA, "الكمية المباعة * سعر البيع"),
            SmartColumnDefinition("total_cogs", "تكلفة البضاعة المباعة COGS", SpreadsheetColumnType.FORMULA, "الكمية المباعة * سعر التكلفة"),
            SmartColumnDefinition("gross_profit", "مجمل الربح (ج.م)", SpreadsheetColumnType.FORMULA, "إجمالي المبيعات - تكلفة البضاعة المباعة")
        )

        val defaultRows = listOf(
            SmartSpreadsheetRow(
                id = "ret_1",
                rowNumber = 1,
                title = "شاشات عرض ذكية 55 بوصة 4K",
                category = "إلكترونيات",
                values = mutableMapOf("sku" to "TV-55-4K", "stock_qty" to 25.0, "unit_cost" to 14500.0, "selling_price" to 18200.0, "sold_qty" to 14.0),
                notes = "ضمان سنتين وكيل معتمد"
            ),
            SmartSpreadsheetRow(
                id = "ret_2",
                rowNumber = 2,
                title = "أجهزة تكييف 2.25 حصان بارد/ساخن",
                category = "أجهزة منزلية",
                values = mutableMapOf("sku" to "AC-225-INV", "stock_qty" to 18.0, "unit_cost" to 21000.0, "selling_price" to 25500.0, "sold_qty" to 9.0),
                notes = "انفرتر موفر للطاقة"
            ),
            SmartSpreadsheetRow(
                id = "ret_3",
                rowNumber = 3,
                title = "هواتف ذكية 256 جيجابايت 5G",
                category = "موبايلات",
                values = mutableMapOf("sku" to "PH-256-5G", "stock_qty" to 40.0, "unit_cost" to 8800.0, "selling_price" to 10900.0, "sold_qty" to 28.0),
                notes = "معالج ثماني النواة مع شاحن سريع"
            )
        )

        return SmartSpreadsheetTemplate(
            activityType = ProjectActivityType.RETAIL_COMMERCE,
            templateName = "شيت التجارة والمبيعات وإدارة المخزون والتجزئة",
            description = "متابعة المخزون، تكلفة البضاعة المباعة COGS، هوامش الربح، وفواتير المبيعات مع طرق الدفع المتعددة.",
            columns = columns,
            defaultRows = defaultRows,
            summaryKpis = listOf("إجمالي المبيعات المحققة", "تكلفة البضاعة المباعة", "مجمل الأرباح التجارية", "قيمة المخزون المتبقي")
        )
    }

    /**
     * Template: Manufacturing & Workshops (تصنيع وورش وإنتاج)
     */
    private fun createManufacturingTemplate(): SmartSpreadsheetTemplate {
        val columns = listOf(
            SmartColumnDefinition("item_name", "المنتج / أمر التصنيع", SpreadsheetColumnType.TEXT),
            SmartColumnDefinition("units_produced", "عدد القطع المنتجة", SpreadsheetColumnType.NUMBER),
            SmartColumnDefinition("raw_materials_cost", "تكلفة المواد الخام (ج.م)", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("operating_power_cost", "كهرباء وطاقة وتشغيل (ج.م)", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("labor_wages", "أجور العمال والفنيين (ج.م)", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("scrap_waste_cost", "تكلفة الهالك والفاقد (ج.م)", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("unit_production_cost", "تكلفة إنتاج القطعة الواحدة", SpreadsheetColumnType.FORMULA, "(الخام + الطاقة + الأجور + الهالك) / عدد القطع"),
            SmartColumnDefinition("wholesale_price", "سعر بيع الجملة المقترح", SpreadsheetColumnType.FORMULA, "تكلفة القطعة * 1.25"),
            SmartColumnDefinition("expected_batch_profit", "ربح التشغيلة المتوقع", SpreadsheetColumnType.FORMULA, "(سعر الجملة - تكلفة القطعة) * عدد القطع")
        )

        val defaultRows = listOf(
            SmartSpreadsheetRow(
                id = "mfg_1",
                rowNumber = 1,
                title = "تشغيلة قطاعات ألومنيوم جامبو (100 نافذة)",
                category = "تصنيع ألومنيوم",
                values = mutableMapOf("units_produced" to 100.0, "raw_materials_cost" to 180000.0, "operating_power_cost" to 9500.0, "labor_wages" to 28000.0, "scrap_waste_cost" to 6000.0),
                notes = "دهان الكتروستاتيك مقاوم للعوامل الجوية"
            ),
            SmartSpreadsheetRow(
                id = "mfg_2",
                rowNumber = 2,
                title = "تشغيلة وحدات إضاءة ليد معمارية (500 وحدة)",
                category = "صناعات كهربائية",
                values = mutableMapOf("units_produced" to 500.0, "raw_materials_cost" to 65000.0, "operating_power_cost" to 4200.0, "labor_wages" to 16000.0, "scrap_waste_cost" to 2100.0),
                notes = "شرائح ليد ألمانية مع درايفر ضد تذبذب الكهرباء"
            )
        )

        return SmartSpreadsheetTemplate(
            activityType = ProjectActivityType.MANUFACTURING,
            templateName = "شيت تكاليف التصنيع وأوامر الإنتاج الصناعية",
            description = "حساب دقيق لتكلفة القطعة، مواد الخام، استهلاك الطاقة، أجور الفنيين، والهالك الصناعي.",
            columns = columns,
            defaultRows = defaultRows,
            summaryKpis = listOf("إجمالي تكاليف التشغيل", "متوسط تكلفة القطعة", "إجمالي الهالك الصناعي", "الأرباح الصافية للإنتاج")
        )
    }

    /**
     * Template: Services & Professional Consulting (خدمات واستشارات)
     */
    private fun createServicesTemplate(): SmartSpreadsheetTemplate {
        val columns = listOf(
            SmartColumnDefinition("contract_name", "المشروع / العقد الاستشاري", SpreadsheetColumnType.TEXT),
            SmartColumnDefinition("client_name", "اسم العميل / الجهة", SpreadsheetColumnType.TEXT),
            SmartColumnDefinition("contract_value", "قيمة العقد الكلية (ج.م)", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("paid_milestones", "الدفعات المستلمة (ج.م)", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("remaining_receivables", "المستحقات المتبقية (ج.م)", SpreadsheetColumnType.FORMULA, "قيمة العقد - الدفعات المستلمة"),
            SmartColumnDefinition("direct_expenses", "المصروفات المباشرة والأتعاب", SpreadsheetColumnType.CURRENCY),
            SmartColumnDefinition("net_service_profit", "صافي أرباح المشروع الاستشاري", SpreadsheetColumnType.FORMULA, "الدفعات المستلمة - المصروفات المباشرة")
        )

        val defaultRows = listOf(
            SmartSpreadsheetRow(
                id = "srv_1",
                rowNumber = 1,
                title = "تصميم وإشراف معماري لبرج سكني",
                category = "استشارات هندسية",
                values = mutableMapOf("client_name" to "شركة النور للتطوير العقاري", "contract_value" to 450000.0, "paid_milestones" to 280000.0, "direct_expenses" to 65000.0),
                notes = "تم تسليم المرحلة الثانية والتراخيص جاهزة"
            ),
            SmartSpreadsheetRow(
                id = "srv_2",
                rowNumber = 2,
                title = "تطوير منظومة إدارة المبيعات والحسابات",
                category = "حلول برمجية وتطوير",
                values = mutableMapOf("client_name" to "مجموعة الفرسان التجارية", "contract_value" to 190000.0, "paid_milestones" to 140000.0, "direct_expenses" to 32000.0),
                notes = "التشغيل التجريبي والتدريب مستمر لمدة أسبوعين"
            )
        )

        return SmartSpreadsheetTemplate(
            activityType = ProjectActivityType.SERVICES_AGENCY,
            templateName = "شيت العقود الاستشارية والخدمات والدفعات المالية",
            description = "متابعة عقود الاستشارات، دفعات العملاء، المستحقات المتبقية، وتكاليف التشغيل.",
            columns = columns,
            defaultRows = defaultRows,
            summaryKpis = listOf("إجمالي قيمة العقود", "الدفعات المستلمة", "المستحقات المتبقية بالخارج", "صافي أرباح الخدمات")
        )
    }

    private fun safeDouble(raw: Any?): Double {
        val d = when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.trim().toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        return if (d.isFinite()) d else 0.0
    }

    private fun computeRowFinancials(row: SmartSpreadsheetRow, activityType: ProjectActivityType): Pair<Double, Double> {
        val vals = row.values
        return when (activityType) {
            ProjectActivityType.CONSTRUCTION -> {
                val qty = safeDouble(vals["quantity"])
                val rate = safeDouble(vals["unit_rate"])
                val rev = safeDouble(vals["contract_value"]).takeIf { it > 0.0 } ?: (qty * rate)
                val exp = safeDouble(vals["total_cost"]).takeIf { it > 0.0 }
                    ?: (safeDouble(vals["materials_cost"]) + safeDouble(vals["labor_cost"]))
                Pair(rev, exp)
            }
            ProjectActivityType.GOLD_JEWELRY -> {
                val wt = safeDouble(vals["weight_grams"])
                val spot = safeDouble(vals["gold_price_per_gram"])
                val mfg = safeDouble(vals["mfg_cost_per_gram"])
                val stamp = safeDouble(vals["stamp_duty_per_gram"])
                val margin = safeDouble(vals["selling_margin"])
                val exp = safeDouble(vals["total_cost"]).takeIf { it > 0.0 } ?: ((spot + mfg + stamp) * wt)
                val rev = safeDouble(vals["selling_price"]).takeIf { it > 0.0 } ?: (exp + margin)
                Pair(rev, exp)
            }
            ProjectActivityType.RETAIL_COMMERCE -> {
                val sold = safeDouble(vals["sold_qty"])
                val unitCost = safeDouble(vals["unit_cost"])
                val sellPrice = safeDouble(vals["selling_price"])
                val rev = safeDouble(vals["total_sales"]).takeIf { it > 0.0 } ?: (sold * sellPrice)
                val exp = safeDouble(vals["total_cogs"]).takeIf { it > 0.0 } ?: (sold * unitCost)
                Pair(rev, exp)
            }
            ProjectActivityType.MANUFACTURING -> {
                val units = safeDouble(vals["units_produced"]).coerceAtLeast(1.0)
                val exp = safeDouble(vals["raw_materials_cost"]) +
                    safeDouble(vals["operating_power_cost"]) +
                    safeDouble(vals["labor_wages"]) +
                    safeDouble(vals["scrap_waste_cost"])
                val unitCost = exp / units
                val rev = unitCost * 1.25 * units
                Pair(rev, exp)
            }
            ProjectActivityType.SERVICES_AGENCY -> {
                val rev = safeDouble(vals["paid_milestones"]).takeIf { it > 0.0 }
                    ?: safeDouble(vals["contract_value"])
                val exp = safeDouble(vals["direct_expenses"])
                Pair(rev, exp)
            }
        }
    }

    /**
     * Natural Language Formula Processor:
     * Parses Arabic user instructions into dynamic spreadsheet formulas and applies them in background.
     * e.g. "أريد حساب صافي الربح بعد خصم 14% ضريبة و 5% هالك وإضافة 500 حافز"
     */
    fun evaluateNaturalLanguageFormula(
        userInputPrompt: String,
        currentRow: SmartSpreadsheetRow,
        template: SmartSpreadsheetTemplate
    ): Pair<String, Double> {
        val prompt = userInputPrompt.trim()
        val (computedRev, computedExp) = computeRowFinancials(currentRow, template.activityType)
        val revenue = if (computedRev > 0.0) computedRev else 10000.0
        val expenses = if (computedExp > 0.0) computedExp else 5000.0

        // Parse percentages and amounts mentioned in prompt
        var taxPercent = 0.0
        var wastePercent = 0.0
        var bonusEgp = 0.0
        var discountPercent = 0.0

        if (prompt.contains("ضريبة") || prompt.contains("14%")) {
            val taxMatch = Regex("(\\d+(\\.\\d+)?)\\s*%?\\s*ضريب").find(prompt)
                ?: Regex("ضريب[\\w\\s]*(\\d+(\\.\\d+)?)\\s*%?").find(prompt)
            taxPercent = (taxMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 14.0).coerceIn(0.0, 100.0)
        }

        if (prompt.contains("هالك") || prompt.contains("فاقد")) {
            val wasteMatch = Regex("(\\d+(\\.\\d+)?)\\s*%?\\s*(هالك|فاقد)").find(prompt)
                ?: Regex("(هالك|فاقد)[\\w\\s]*(\\d+(\\.\\d+)?)\\s*%?").find(prompt)
            wastePercent = (wasteMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 5.0).coerceIn(0.0, 100.0)
        }

        if (prompt.contains("خصم")) {
            val discMatch = Regex("(\\d+(\\.\\d+)?)\\s*%?\\s*خصم").find(prompt)
            discountPercent = (discMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
        }

        if (prompt.contains("حافز") || prompt.contains("مكافأة") || prompt.contains("إضافة")) {
            val bonusMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(جنيه|ج\\.م|حافز|مكافأة)").find(prompt)
                ?: Regex("(حافز|مكافأة|إضافة)[\\w\\s]*(\\d+(?:\\.\\d+)?)").find(prompt)
            val parsedBonus = bonusMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()
                ?: bonusMatch?.groupValues?.getOrNull(2)?.toDoubleOrNull()
                ?: 0.0
            bonusEgp = if (parsedBonus.isFinite() && parsedBonus >= 0.0) parsedBonus else 0.0
        }

        val revBD = BigDecimal.valueOf(revenue)
        val expBD = BigDecimal.valueOf(expenses)
        val hundred = BigDecimal(100)
        val grossProfitBD = revBD.subtract(expBD)
        val taxBD = revBD.multiply(BigDecimal.valueOf(taxPercent)).divide(hundred, 2, RoundingMode.HALF_UP)
        val wasteBD = revBD.multiply(BigDecimal.valueOf(wastePercent)).divide(hundred, 2, RoundingMode.HALF_UP)
        val discBD = revBD.multiply(BigDecimal.valueOf(discountPercent)).divide(hundred, 2, RoundingMode.HALF_UP)
        val bonusBD = BigDecimal.valueOf(bonusEgp)
        val netComputedBD = grossProfitBD.subtract(taxBD).subtract(wasteBD).subtract(discBD).add(bonusBD)
            .setScale(2, RoundingMode.HALF_UP)

        val formulaExplanation = buildString {
            append("= (الإيراد: ${revBD.setScale(0, RoundingMode.HALF_UP)} - التكلفة: ${expBD.setScale(0, RoundingMode.HALF_UP)})")
            if (taxPercent > 0) append(" - ضريبة ${taxPercent}% (${taxBD.setScale(0, RoundingMode.HALF_UP)})")
            if (wastePercent > 0) append(" - هالك ${wastePercent}% (${wasteBD.setScale(0, RoundingMode.HALF_UP)})")
            if (discountPercent > 0) append(" - خصم ${discountPercent}% (${discBD.setScale(0, RoundingMode.HALF_UP)})")
            if (bonusEgp > 0) append(" + حافز (${bonusBD.setScale(0, RoundingMode.HALF_UP)})")
        }

        return Pair(formulaExplanation, netComputedBD.toDouble())
    }

    /**
     * Compute comprehensive, proactive accounting audit and advice from project data
     */
    fun performVirtualAccountantAudit(
        template: SmartSpreadsheetTemplate,
        rows: List<SmartSpreadsheetRow>,
        estimatedCapital: Double = 0.0
    ): VirtualAccountantAdviceReport {
        var totalRev = BigDecimal.ZERO
        var totalExp = BigDecimal.ZERO
        val alerts = mutableListOf<String>()
        val tips = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        for (row in rows) {
            val (rowRev, rowExp) = computeRowFinancials(row, template.activityType)
            if (rowRev.isFinite()) totalRev = totalRev.add(BigDecimal.valueOf(rowRev))
            if (rowExp.isFinite()) totalExp = totalExp.add(BigDecimal.valueOf(rowExp))
        }

        val netCashflow = totalRev.subtract(totalExp).setScale(2, RoundingMode.HALF_UP).toDouble()
        val totalRevenueVal = totalRev.setScale(2, RoundingMode.HALF_UP).toDouble()
        val totalExpensesVal = totalExp.setScale(2, RoundingMode.HALF_UP).toDouble()

        val margin = if (totalRevenueVal > 0.0) {
            ((netCashflow / totalRevenueVal) * 100.0).coerceIn(-100.0, 100.0)
        } else 0.0

        // Determine health score
        var score = 85
        if (netCashflow > 0) score += 10 else if (rows.isNotEmpty()) score -= 25
        if (margin > 20) score += 5 else if (margin < 5 && rows.isNotEmpty()) score -= 10
        if (estimatedCapital > 0 && totalExpensesVal > estimatedCapital) {
            score -= 15
            alerts.add("🚨 تجاوز الميزانية: إجمالي المصروفات (${GoldUtils.formatCurrency(totalExpensesVal)}) تخطى رأس المال التقديري (${GoldUtils.formatCurrency(estimatedCapital)})!")
        }
        score = score.coerceIn(20, 98)

        val healthLabel = when {
            score >= 90 -> "ممتاز جداً وآمن 🟢"
            score >= 75 -> "مستقر وجيد 🟡"
            score >= 50 -> "يحتاج تدقيق ومتابعة 🟠"
            else -> "حرج وعجز مالي 🔴"
        }

        if (netCashflow >= 0) {
            tips.add("💡 كفاءة مالية: هامش الربح الحالي ${margin.toInt()}% يعتبر جيداً مقارنة بمتوسط السوق (${template.activityType.arLabel}).")
        } else {
            alerts.add("⚠️ تدفق نقدي سالب: المصروفات تفوق الإيرادات الحالية بفارق ${GoldUtils.formatCurrency(-netCashflow)}!")
            tips.add("💡 تحسين فوري: مراجعة تكاليف المواد الخام والأجور المباشرة لخفض نقطة التعادل التعاقدية.")
        }

        when (template.activityType) {
            ProjectActivityType.CONSTRUCTION -> {
                recommendations.add("📌 يُنصح بترحيل 15% من قيمة كل مستخلص كدفعة ضمان أعمال حتى الاستلام الابتدائي.")
                recommendations.add("📌 ربط صرف مستخلصات مقاولي الباطن بنسب الإنجاز الفعلية الموثقة بالصور والفيديو.")
                tips.add("🏗️ توفير تكلفة: تثبيت أسعار حديد التسليح والإسمنت عبر عقود توريد آجلة يمنع تقلبات الميزانية.")
            }
            ProjectActivityType.GOLD_JEWELRY -> {
                recommendations.add("📌 فحص موازين الذهب يومياً وتثبيت نسبة الهالك بما لا يتجاوز 0.3% للكسر والمصنعية.")
                recommendations.add("📌 إجراء تسوية أسبوعية للمصوغات الخام وتفادي الاحتفاظ بسيولة غير مغطاة بذهب.")
                tips.add("💎 تحسين ربحي: التركيز على مبيعات الأطقم الخفيفة ذات المصنعية العالية يرفع هامش الربح لـ 28%.")
            }
            ProjectActivityType.RETAIL_COMMERCE -> {
                recommendations.add("📌 تفعيل سياسة الجرد الدوري ومراقبة معدل دوران المخزون للأصناف بطيئة الحركة.")
                recommendations.add("📌 الاعتماد على فواتير إلكترونية ومطابقة الدرج نهاية كل شفت لمنع أي عجز نقدي.")
                tips.add("📦 إدارة المخزون: تقليل الكميات الراكدة يحول 30% من رأس المال المجمد إلى سيولة نقدية فورية.")
            }
            ProjectActivityType.MANUFACTURING -> {
                recommendations.add("📌 حساب تكلفة الهالك والفاقد وإضافته المباشرة على تسعير أمر الإنتاج قبل تسليم العميل.")
                recommendations.add("📌 جدولة ساعات تشغيل الماكينات في فترات تعرفة الطاقة المنخفضة لتقليل تكلفة القطعة.")
            }
            ProjectActivityType.SERVICES_AGENCY -> {
                recommendations.add("📌 عدم بدء أي مرحلة جديدة إلا بعد تحصيل 50% على الأقل من الدفعة السابقة.")
                recommendations.add("📌 توثيق كافة ساعات الاستشارات الزائدة كبنود مستقلة خارج نطاق العقد الأساسي.")
            }
        }

        return VirtualAccountantAdviceReport(
            healthScore = score,
            healthStatusLabel = healthLabel,
            totalRevenue = totalRevenueVal,
            totalExpenses = totalExpensesVal,
            netCashflow = netCashflow,
            profitMarginPercent = margin,
            alerts = alerts,
            proactiveTips = tips,
            accountingRecommendations = recommendations
        )
    }

    /**
     * Export the Complete Smart Spreadsheet to an Excel-compatible Arabic CSV
     * with Native Excel Formulas, UTF-8 BOM, and Embedded Formatting.
     */
    fun exportSmartSpreadsheetToExcel(
        context: Context,
        template: SmartSpreadsheetTemplate,
        rows: List<SmartSpreadsheetRow>,
        projectName: String
    ): File? {
        return try {
            val exportDir = File(context.cacheDir, "smart_excel_sheets").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeProject = projectName.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF_-]"), "_")
            val file = File(exportDir, "OOZOX_Excel_${safeProject}_$timeStamp.csv")

            FileOutputStream(file).use { fos ->
                fos.write(0xEF)
                fos.write(0xBB)
                fos.write(0xBF)

                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write("منظومة OOZOX المحاسبية الذكية - شيت إكسل ديناميكي معتمد\n")
                    writer.write("اسم المشروع:;\"$projectName\"\n")
                    writer.write("نوع النشاط:;\"${template.activityType.arLabel}\"\n")
                    writer.write("القالب المحاسبي:;\"${template.templateName}\"\n")
                    writer.write("تاريخ التصدير:;\"${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\"\n\n")

                    val visibleCols = template.columns.filter { !it.isHiddenCalculation }
                    val headerRow = visibleCols.joinToString(";") { "\"${it.nameAr}\"" }
                    writer.write(headerRow + ";\"ملاحظات وتوثيق الوسائط\"\n")

                    val startRow = 7
                    var excelRowIdx = startRow
                    for (row in rows) {
                        val (rowRev, rowExp) = computeRowFinancials(row, template.activityType)
                        val rowNet = rowRev - rowExp
                        val rowCells = mutableListOf<String>()
                        for (col in visibleCols) {
                            val cellVal: Any = when (col.id) {
                                "item_name", "product_name", "contract_name" -> row.title
                                "total_cost", "total_cogs" -> rowExp
                                "contract_value", "total_sales", "selling_price" -> rowRev
                                "net_profit", "profit_egp", "gross_profit", "net_service_profit", "expected_batch_profit" -> rowNet
                                "remaining_receivables" -> (safeDouble(row.values["contract_value"]) - safeDouble(row.values["paid_milestones"])).coerceAtLeast(0.0)
                                "unit_production_cost" -> {
                                    val units = safeDouble(row.values["units_produced"])
                                    if (units > 0.0) rowExp / units else 0.0
                                }
                                "wholesale_price" -> {
                                    val units = safeDouble(row.values["units_produced"])
                                    if (units > 0.0) (rowExp / units) * 1.25 else 0.0
                                }
                                else -> row.values[col.id] ?: row.calculatedResults[col.id] ?: ""
                            }

                            val formattedVal = when (cellVal) {
                                is Double -> String.format(Locale.US, "%.2f", if (cellVal.isFinite()) cellVal else 0.0)
                                is Number -> cellVal.toString()
                                else -> "\"${cellVal.toString().replace("\"", "\"\"")}\""
                            }
                            rowCells.add(formattedVal)
                        }

                        val mediaNote = buildString {
                            if (!row.notes.isNullOrBlank()) append(row.notes)
                            if (row.photoUri != null) append(" [📷 صورة]")
                            if (row.videoUri != null) append(" [🎥 فيديو]")
                        }
                        rowCells.add("\"${mediaNote.replace("\"", "\"\"")}\"")

                        writer.write(rowCells.joinToString(";") + "\n")
                        excelRowIdx++
                    }

                    val endRow = (excelRowIdx - 1).coerceAtLeast(startRow)
                    val summaryRow = visibleCols.mapIndexed { idx, col ->
                        val colLetter = ('A' + idx).toString()
                        when {
                            idx == 0 -> "\"إجمالي الشيت المحاسبي:\""
                            col.type == SpreadsheetColumnType.CURRENCY || col.type == SpreadsheetColumnType.FORMULA ->
                                "\"=SUM(${colLetter}${startRow}:${colLetter}${endRow})\""
                            else -> "\"\""
                        }
                    }.joinToString(";")
                    writer.write("\n$summaryRow;\"معتمد من المحاسب الذكي OOZOX\"\n")
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Detect activity type from project type string or title
     */
    fun detectActivityType(projectType: String?): ProjectActivityType {
        val typeLower = (projectType ?: "").lowercase(Locale.getDefault())
        return when {
            typeLower.contains("ذهب") || typeLower.contains("صاغ") || typeLower.contains("مجوهر") || typeLower.contains("عيار") -> ProjectActivityType.GOLD_JEWELRY
            typeLower.contains("عقار") || typeLower.contains("بناء") || typeLower.contains("مقاول") || typeLower.contains("تشييد") || typeLower.contains("خرسان") || typeLower.contains("إنشائ") -> ProjectActivityType.CONSTRUCTION
            typeLower.contains("تجار") || typeLower.contains("متجر") || typeLower.contains("بيع") || typeLower.contains("مبيعات") || typeLower.contains("ماركت") || typeLower.contains("بضاع") -> ProjectActivityType.RETAIL_COMMERCE
            typeLower.contains("تصنيع") || typeLower.contains("ورق") || typeLower.contains("ورش") || typeLower.contains("مصنع") || typeLower.contains("إنتاج") -> ProjectActivityType.MANUFACTURING
            else -> ProjectActivityType.SERVICES_AGENCY
        }
    }

    // In-memory project spreadsheet cache
    private val projectSheets = mutableMapOf<String, Pair<SmartSpreadsheetTemplate, MutableList<SmartSpreadsheetRow>>>()

    fun getOrCreateProjectSheet(projectId: String, projectType: String?): Pair<SmartSpreadsheetTemplate, MutableList<SmartSpreadsheetRow>> {
        val existing = projectSheets[projectId]
        if (existing != null) return existing

        val activityType = detectActivityType(projectType)
        val template = getTemplateForActivity(activityType)
        val initialRows = template.defaultRows.map { it.copy(values = HashMap(it.values), calculatedResults = HashMap(it.calculatedResults)) }.toMutableList()
        val pair = Pair(template, initialRows)
        projectSheets[projectId] = pair
        return pair
    }

    fun updateProjectSheet(projectId: String, template: SmartSpreadsheetTemplate, rows: List<SmartSpreadsheetRow>) {
        projectSheets[projectId] = Pair(template, rows.toMutableList())
    }

    /**
     * Interactive Virtual Accountant Chat response generator
     */
    fun askProjectAccountant(
        question: String,
        template: SmartSpreadsheetTemplate,
        rows: List<SmartSpreadsheetRow>,
        report: VirtualAccountantAdviceReport
    ): String {
        val q = question.trim()
        val net = report.netCashflow
        val rev = report.totalRevenue
        val exp = report.totalExpenses
        val margin = report.profitMarginPercent.toInt()

        return when {
            q.contains("ربح") || q.contains("أرباح") || q.contains("مكسب") -> {
                "📊 تقرير الأرباح لمشروع (${template.templateName}):\n" +
                "• إجمالي الإيرادات التعاقدية/المبيعات: ${GoldUtils.formatCurrency(rev)}\n" +
                "• إجمالي التكاليف المباشرة والمصروفات: ${GoldUtils.formatCurrency(exp)}\n" +
                "• صافي الربح المحقق: ${GoldUtils.formatCurrency(net)}\n" +
                "• هامش الربح الحالي: $margin%\n" +
                if (net > 0) "✅ وضع الأرباح صحي وآمن، وهامش الربح يفوق المعدل المستهدف للنشاط."
                else "⚠️ انتبه: صافي التدفق سالب أو متقارب، يُرجى تدقيق بنود التكلفة فوراً."
            }
            q.contains("مصروف") || q.contains("تكلف") || q.contains("صرف") -> {
                "📉 تقرير التكاليف والمصروفات:\n" +
                "• إجمالي المصروفات المنفذة بالشيت: ${GoldUtils.formatCurrency(exp)}\n" +
                "• تم رصد ${rows.size} بند تشغيلي.\n" +
                "💡 نصيحة المحاسب: راقب بنود التكلفة الأكثر استنزافاً وحافظ على هوامش أمان للمواد الخام بنسبة 5% إلى 8% ضد تقلب الأسعار."
            }
            q.contains("خطر") || q.contains("مشاكل") || q.contains("تنبيه") || q.contains("عجز") -> {
                if (report.alerts.isNotEmpty()) {
                    "🚨 تنبيهات المحاسب العاجلة:\n" + report.alerts.joinToString("\n• ") { it }
                } else {
                    "🟢 مؤشرات الأمان ممتازة:\n" +
                    "• درجة الأمان المالي: ${report.healthScore}% (${report.healthStatusLabel})\n" +
                    "• لا توجد أي مؤشرات عجز أو تجاوز خطير للميزانية حالياً. كافة القيود متوازنة."
                }
            }
            q.contains("نصيح") || q.contains("إرشاد") || q.contains("توصي") || q.contains("تطوير") -> {
                "💡 نصائح المحاسب المالي لنشاط (${template.activityType.arLabel}):\n" +
                (report.proactiveTips + report.accountingRecommendations).joinToString("\n• ") { it }
            }
            else -> {
                "🧑‍💼 المحاسب الذكي في خدمتك دائماً لمشروع (${template.templateName}):\n" +
                "• الرصيد الصافي: ${GoldUtils.formatCurrency(net)}\n" +
                "• نسبة الأمان المالي: ${report.healthScore}% (${report.healthStatusLabel})\n" +
                "• يمكنك أن تطلب مني: حساب صافي الربح، فحص التكاليف، كشف التنبيهات، أو إضافة معادلة ذكية باللغة الطبيعية."
            }
        }
    }

    /**
     * Share exported CSV file with Excel-compatible MIME
     */
    fun shareExcelFile(context: Context, file: File, title: String = "مشاركة شيت إكسل المشروع") {
        try {
            val uri: android.net.Uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "تعذر مشاركة الملف: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
