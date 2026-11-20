package com.example.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class BusinessFieldTemplate(
    val id: String,
    val nameAr: String,
    val suggestedJobTitle: String,
    val suggestedBio: String,
    val suggestedSkills: List<String>,
    val suggestedCatalogCategories: List<String>,
    val ratingCriteria: Triple<String, String, String>,
    val isCustom: Boolean = false
)

/**
 * Provides multi-industry business field templates and persists user-created custom fields
 * so merchants are never restricted to gold stores.
 */
object MerchantFieldCatalog {

    val defaultFields: List<BusinessFieldTemplate> = listOf(
        BusinessFieldTemplate(
            id = "general_commerce",
            nameAr = "تجارة عامة ومتجر متعدد",
            suggestedJobTitle = "مدير متجر وتجارة عامة",
            suggestedBio = "متجر معتمد لتوفير أجود المنتجات بأسعار تنافسية وتوصيل سريع وفواتير موثقة.",
            suggestedSkills = listOf("توريد جملة وقطاعي", "إدارة مخزون وفواتير", "خدمة عملاء فورية", "تغليف وشحن سريع", "عروض وخصومات موسمية"),
            suggestedCatalogCategories = listOf("الأكثر مبيعاً", "عروض خاصة", "وصل حديثاً", "طلبات الجملة"),
            ratingCriteria = Triple("جودة المنتجات والمواصفات", "سرعة التجهيز والتوصيل", "الأمانة والشفافية")
        ),
        BusinessFieldTemplate(
            id = "contracting",
            nameAr = "مقاولات وإنشاءات",
            suggestedJobTitle = "مقاول عام ومهندس تنفيذ مواقع",
            suggestedBio = "تنفيذ مشاريع البناء والتشطيبات وتوريد خامات المواقع بإشراف هندسي وجداول تسليم دقيقة.",
            suggestedSkills = listOf("إدارة مواقع وإنشاءات", "توريد حديد وأسمنت", "تشطيبات سوبر لوكس", "حساب كميات ومستخلصات", "مقاولات باطن وإشراف"),
            suggestedCatalogCategories = listOf("مواد بناء", "باقات تشطيب", "معدات وتجهيز", "استشارات هندسية"),
            ratingCriteria = Triple("جودة التنفيذ والمواد", "الالتزام بالجدول الزمني", "وضوح المستخلصات والتكلفة")
        ),
        BusinessFieldTemplate(
            id = "gold_jewelry",
            nameAr = "ذهب ومجوهرات",
            suggestedJobTitle = "صائغ وخبير سبائك ومعايرة",
            suggestedBio = "متجر صاغة ومصوغات معتمد للسبائك والعملات والمشغولات بأعلى معايير الدقة والدمغة.",
            suggestedSkills = listOf("سبك وتصفية عيار 21 و 18", "فحص نقاء بالليزر", "توزيع سبائك معتمدة", "تقييم وتثمين ألماس", "صياغة وتصميم 3D"),
            suggestedCatalogCategories = listOf("سبائك ذهب", "مشغولات ومصاغ", "جنيهات وعملات", "فضيات وصياغة"),
            ratingCriteria = Triple("دقة الوزن والعيار", "سرعة المعاملة والتسليم", "الأمانة والنزاهة")
        ),
        BusinessFieldTemplate(
            id = "tech_software",
            nameAr = "تقنية وبرمجيات وتسويق",
            suggestedJobTitle = "مطور حلول رقمية وتسويق أعمال",
            suggestedBio = "تطوير التطبيقات والمواقع وإدارة الحملات الإعلانية وأنظمة نقاط البيع للشركات.",
            suggestedSkills = listOf("تصميم مواقع وتطبيقات", "إدارة متاجر إلكترونية", "تسويق رقمي وإعلانات", "هوية بصرية وبراندنج", "أتمتة ومبيعات"),
            suggestedCatalogCategories = listOf("تطوير برمجيات", "تصميم وهوية", "تسويق وإعلانات", "استشارات تقنية"),
            ratingCriteria = Triple("جودة التصميم والكود", "سرعة الاستجابة والدعم", "وضوح التسليم والنتائج")
        ),
        BusinessFieldTemplate(
            id = "electronics",
            nameAr = "إلكترونيات وهواتف وأجهزة",
            suggestedJobTitle = "تاجر ومهندس صيانة إلكترونيات",
            suggestedBio = "بيع وصيانة الهواتف الذكية وأجهزة الكمبيوتر والإكسسوارات الأصلية بضمان معتمد.",
            suggestedSkills = listOf("مبيعات هواتف ولابتوب", "صيانة فورية معتمدة", "إكسسوارات أصلية", "أنظمة مراقبة وكاميرات", "ضمان واستبدال"),
            suggestedCatalogCategories = listOf("هواتف ذكية", "لابتوب وكمبيوتر", "إكسسوارات", "صيانة ودعم"),
            ratingCriteria = Triple("أصالة الجهاز والضمان", "سرعة الفحص والتسليم", "مصداقية السعر والفاتورة")
        ),
        BusinessFieldTemplate(
            id = "real_estate",
            nameAr = "عقارات واستثمار عقاري",
            suggestedJobTitle = "مستشار ومسوق عقاري معتمد",
            suggestedBio = "تسويق وبيع وإيجار الوحدات السكنية والتجارية وإدارة المحافظ العقارية بشفافية.",
            suggestedSkills = listOf("تقييم عقاري دقيق", "وساطة بيع وإيجار", "إدارة أملاك وتحصيل", "تسهيل إجراءات وتعاقدات", "فرص استثمارية"),
            suggestedCatalogCategories = listOf("وحدات سكنية", "مقرات إدارية وتجارية", "أراضي ومشروعات", "إيجارات واستثمار"),
            ratingCriteria = Triple("دقة المعاينة والبيانات", "سرعة إنهاء الإجراءات", "الثقة والأمانة")
        ),
        BusinessFieldTemplate(
            id = "fashion_retail",
            nameAr = "أزياء وملابس وعطور",
            suggestedJobTitle = "مصمم وتاجر أزياء ومنتجات تجميل",
            suggestedBio = "تشكيلة مختارة من الأزياء العصرية والعطور الأصلية بخامات فاخرة وتجهيز هدايا.",
            suggestedSkills = listOf("تشكيلات موسمية حصرية", "مبيعات جملة وتجزئة", "تجهيز طلبات أونلاين", "استبدال مرن", "تنسيق هدايا"),
            suggestedCatalogCategories = listOf("ملابس رجالي وحريمي", "عطور وهدايا", "أحذية وحقائب", "عروض التشكيلة"),
            ratingCriteria = Triple("جودة الخامة والتفصيل", "سرعة الشحن والتغليف", "مطابقة الصورة للواقع")
        ),
        BusinessFieldTemplate(
            id = "food_hospitality",
            nameAr = "مطاعم وكافيهات وتموين",
            suggestedJobTitle = "مدير مطعم وتجهيز حفلات ووجبات",
            suggestedBio = "تقديم أشهى الوجبات والمشروبات المختصة وخدمات الكاترينج وتوريد ولائم الشركات.",
            suggestedSkills = listOf("وجبات طازجة يومياً", "توريد وجبات شركات", "تنظيم بوفيه وحفلات", "توصيل ساخن سريع", "جودة وسلامة غذائية"),
            suggestedCatalogCategories = listOf("الأطباق الرئيسية", "مشروبات وحلويات", "باقات الشركات", "تجهيز مناسبات"),
            ratingCriteria = Triple("جودة الطعم والنظافة", "سرعة التحضير والتوصيل", "تغليف وحرارة الطلب")
        ),
        BusinessFieldTemplate(
            id = "auto_logistics",
            nameAr = "سيارات وقطع غيار ونقل",
            suggestedJobTitle = "تاجر سيارات ومزود خدمات لوجستية",
            suggestedBio = "بيع وفحص السيارات وقطع الغيار الأصلية وخدمات الشحن والنقل التجاري بين المحافظات.",
            suggestedSkills = listOf("فحص شامل للسيارات", "قطع غيار أصلية", "نقل وشحن بضائع", "صيانة سريعة", "تخليص وفواتير"),
            suggestedCatalogCategories = listOf("سيارات", "قطع غيار", "خدمات نقل وشحن", "صيانة وفحص"),
            ratingCriteria = Triple("سلامة الحالة والمواصفات", "الالتزام بموعد التسليم", "الوضوح في التكلفة")
        )
    )

    fun templateForName(context: Context, fieldName: String): BusinessFieldTemplate {
        val all = loadAllFields(context)
        val clean = fieldName.trim()
        return all.firstOrNull {
            it.nameAr.equals(clean, ignoreCase = true) || clean.contains(it.nameAr, ignoreCase = true)
        } ?: createDynamicCustomTemplate(clean)
    }

    fun createDynamicCustomTemplate(customName: String): BusinessFieldTemplate {
        val label = customName.trim().ifBlank { "نشاط تجاري مخصص" }
        return BusinessFieldTemplate(
            id = "custom_${label.hashCode()}",
            nameAr = label,
            suggestedJobTitle = "مؤسس ومدير $label",
            suggestedBio = "متخصصون في مجال $label مع التزام كامل بالجودة، وضوح التسعير، وتوثيق جميع الفواتير عبر OOZOX.",
            suggestedSkills = listOf(
                "خبرة متخصصة في $label",
                "فواتير وعمليات موثقة",
                "سرعة تنفيذ وتسليم",
                "دعم ومتابعة مستمرة للعملاء"
            ),
            suggestedCatalogCategories = listOf("الخدمات الأساسية", "العروض المميزة", "المنتجات", "طلبات خاصة"),
            ratingCriteria = Triple("جودة الخدمة في $label", "الالتزام بالمواعيد والتسليم", "الأمانة ووضوح التعامل"),
            isCustom = true
        )
    }

    private const val PREFS = "oozox_merchant_fields_prefs"
    private const val KEY_CUSTOM_FIELDS = "custom_fields_json"
    private const val KEY_SELECTED_FIELD_PREFIX = "selected_field_"

    fun getSelectedField(context: Context, merchantId: String): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString("$KEY_SELECTED_FIELD_PREFIX$merchantId", null)
            ?.takeIf { it.isNotBlank() }
            ?: defaultFields.first().nameAr
    }

    fun setSelectedField(context: Context, merchantId: String, fieldName: String) {
        val clean = fieldName.trim()
        if (clean.isBlank()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("$KEY_SELECTED_FIELD_PREFIX$merchantId", clean)
            .apply()
    }

    fun loadAllFields(context: Context): List<BusinessFieldTemplate> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CUSTOM_FIELDS, null) ?: return defaultFields
        val customList = mutableListOf<BusinessFieldTemplate>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val name = obj.optString("nameAr").trim()
                if (name.isBlank()) continue
                customList.add(createDynamicCustomTemplate(name).copy(id = obj.optString("id", "custom_$i")))
            }
        } catch (_: Exception) {
        }
        return (defaultFields + customList).distinctBy { it.nameAr }
    }

    fun addCustomField(context: Context, fieldName: String): BusinessFieldTemplate {
        val clean = fieldName.trim()
        val template = createDynamicCustomTemplate(clean)
        if (clean.isBlank()) return template
        val existingCustom = loadAllFields(context).filter { it.isCustom }.toMutableList()
        if (existingCustom.none { it.nameAr.equals(clean, ignoreCase = true) } &&
            defaultFields.none { it.nameAr.equals(clean, ignoreCase = true) }
        ) {
            existingCustom.add(template)
            val arr = JSONArray()
            existingCustom.forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("nameAr", item.nameAr)
                    }
                )
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CUSTOM_FIELDS, arr.toString())
                .apply()
        }
        return template
    }
}
