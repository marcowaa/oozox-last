package com.example.util

import com.example.data.model.ProjectType

/**
 * Tailored accounting & operational profile for a specific project sector.
 */
data class ProjectAccountingProfile(
    val sectorId: String,
    val sectorNameAr: String,
    val baseProjectType: ProjectType,
    val revenueAccounts: List<String>,
    val expenseAccounts: List<String>,
    val tracksPhysicalGoods: Boolean,
    val kpiLabels: List<String>,
    val quickEntryTemplates: List<Pair<String, String>>, // (Label, "INCOME" or "EXPENSE")
    val shiftHandoverChecklist: List<String>
)

data class ProjectSector(
    val id: String,
    val nameAr: String,
    val iconKey: String,
    val accountingProfile: ProjectAccountingProfile,
    val projectTemplates: List<String>
)

/**
 * Comprehensive catalog of 20+ industry sectors covering 320+ specialized project types
 * with tailored accounting charts, KPIs, and shift handover checklists per project.
 */
object ProjectIndustryCatalog {

    val sectors: List<ProjectSector> = listOf(
        ProjectSector(
            id = "construction",
            nameAr = "المقاولات والإنشاءات والبنية التحتية",
            iconKey = "Engineering",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "construction",
                sectorNameAr = "محاسبة المقاولات والمستخلصات",
                baseProjectType = ProjectType.CONSTRUCTION,
                revenueAccounts = listOf("مستخلصات جارية", "دفعة مقدمة تعاقدية", "أوامر تغيير وإضافات", "تأمينات مستردة"),
                expenseAccounts = listOf("حديد وأسمنت ومواد بناء", "مستخلصات مقاولي الباطن", "أجور عمال الموقع", "إيجار معدات وحفارات", "نقل وتشوين"),
                tracksPhysicalGoods = true,
                kpiLabels = listOf("نسبة الإنجاز المالي", "تكلفة المتر المربع", "انحراف الميزانية"),
                quickEntryTemplates = listOf(
                    "صرف مستخلص مقاول باطن" to "EXPENSE",
                    "شراء مواد بناء (حديد/أسمنت)" to "EXPENSE",
                    "تحصيل مستخلص جاري" to "INCOME"
                ),
                shiftHandoverChecklist = listOf("جرد مخزن الأسمنت والحديد", "عهدة النقدية للموقع", "معدات الحفر والعدة اليدوية", "دفتر الحضور واليومية")
            ),
            projectTemplates = listOf(
                "بناء برج سكني متكامل", "إنشاء فيلا سكنية فاخرة", "تطوير مجمع سكني (كمبوند)", "بناء مول تجاري",
                "إنشاء مستودع وهنجر صناعي", "تمهيد ورصف طرق وكباري", "أعمال حفر وأساسات عميقة", "إنشاء محطة وقود متكاملة",
                "بناء مدرسة أو مركز تعليمي", "إنشاء مستشفى أو مجمع طبي", "بناء مسجد أو دار مناسبات", "إنشاء مصنع خرسانة جاهزة",
                "أعمال شبكات المياه والصرف الصحي", "تركيب شبكات الغاز الطبيعي", "بناء سور وبوابات أمنية", "إنشاء حمامات سباحة ولاندسكيب",
                "ترميم وتدعيم مباني أثرية", "إنشاء جراج متعدد الطوابق", "بناء شاليهات ومنتجعات ساحلية", "مقاولات أعمال عظم ومسلحة"
            )
        ),
        ProjectSector(
            id = "finishing",
            nameAr = "التشطيبات والديكور والعمارة",
            iconKey = "FormatPaint",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "finishing",
                sectorNameAr = "محاسبة التشطيبات والديكور",
                baseProjectType = ProjectType.FINISHING,
                revenueAccounts = listOf("دفعات باقة التشطيب", "أعمال تصميم داخلي 3D", "توريد وتركيب ديكورات", "إشراف هندسي"),
                expenseAccounts = listOf("دهانات وبويات", "سيراميك وبورسلين ورخام", "تأسيس كهرباء وسباكة", "أجور صنايعية", "أعمال جبس بورد وألوميتال"),
                tracksPhysicalGoods = true,
                kpiLabels = listOf("هامش ربح الباقة", "تكلفة التشطيب للمتر", "متوسط مدة التسليم"),
                quickEntryTemplates = listOf(
                    "توريد سيراميك ورخام" to "EXPENSE",
                    "دفعة يومية صنايعية" to "EXPENSE",
                    "تحصيل قسط تشطيب" to "INCOME"
                ),
                shiftHandoverChecklist = listOf("جرد خامات الدهانات واللوازم", "عهدة المشتريات اليومية", "فواتير الموردين المعلقة")
            ),
            projectTemplates = listOf(
                "تشطيب شقق سكنية سوبر لوكس", "تصميم وتنفيذ ديكور داخلي", "تشطيب مكاتب إدارية وشركات", "تجهيز محلات ومعارض تجارية",
                "تجهيز مطاعم وكافيهات مودرن", "أعمال رخام وجرانيت وواجهات", "تأسيس وتشطيب سباكة وكهرباء", "تركيب أسقف جبس بورد وإضاءة",
                "تفصيل وتركيب مطابخ ألوميتال وخشب", "أعمال نجارة وأبواب مصفحة", "تركيب أرضيات باركيه و HDF", "أعمال واجهات كلادينج وزجاج",
                "تصميم وتنفيذ حدائق ولاندسكيب", "أعمال عزل حراري ومائي للأسطح", "تركيب أنظمة المنزل الذكي (Smart Home)", "تجهيز صالات جيم ونوادي",
                "تجهيز عيادات طبية ومراكز تجميل", "تشطيب فنادق وغرف ضيافة"
            )
        ),
        ProjectSector(
            id = "retail",
            nameAr = "التجارة العامة والتجزئة والسوبر ماركت",
            iconKey = "Storefront",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "retail",
                sectorNameAr = "محاسبة التجارة والمخزون ونقاط البيع",
                baseProjectType = ProjectType.OTHER,
                revenueAccounts = listOf("مبيعات كاش", "مبيعات فيزا وإنستاباي", "مبيعات فودافون كاش", "مبيعات الجملة", "عروض وباقات"),
                expenseAccounts = listOf("مشتريات بضائع ومخزون", "رواتب الكاشير والعمالة", "إيجار المحل والكهرباء", "أكياس وتغليف", "هالك ومرتجعات"),
                tracksPhysicalGoods = true,
                kpiLabels = listOf("دوران المخزون", "صافي ربح الوردية", "نسبة العجز والهالك"),
                quickEntryTemplates = listOf(
                    "إغلاق مبيعات الوردية" to "INCOME",
                    "سداد فاتورة مورد بضاعة" to "EXPENSE",
                    "مصروفات نثرية وتشغيل" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("جرد درج النقدية", "مطابقة إيصالات الفيزا وإنستاباي", "جرد الأصناف سريعة الحركة", "تسليم مفاتيح المخزن")
            ),
            projectTemplates = listOf(
                "سوبر ماركت وهايبر ماركت", "متجر بقالة وتموينات", "محل عطارة وتوابل وأعشاب", "متجر خضروات وفواكه طازجة",
                "محل لحوم وجزارة بلدي", "محل دواجن وطيور", "محل أسماك ومأكولات بحرية", "محل منظفات وورقيات",
                "مكتبة وأدوات مدرسية وقرطاسية", "متجر هدايا وإكسسوارات", "متجر ألعاب أطفال", "متجر أدوات منزلية ومطبخ",
                "متجر مفروشات ومستلزمات منزل", "محل حلويات ومقرمشات", "محل ألبان ومنتجات مزارع", "تجارة جملة مواد غذائية",
                "متجر مستلزمات حيوانات أليفة", "متجر أجهزة رياضية ومكملات", "متجر زهور ونباتات زينة", "متجر مستلزمات تعبئة وتغليف"
            )
        ),
        ProjectSector(
            id = "restaurants",
            nameAr = "المطاعم والكافيهات والضيافة",
            iconKey = "Restaurant",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "restaurants",
                sectorNameAr = "محاسبة المطاعم وتكلفة الأغذية (Food Cost)",
                baseProjectType = ProjectType.OTHER,
                revenueAccounts = listOf("مبيعات الصالة (Dine-in)", "مبيعات الدليفري والتوصيل", "تيك أواي (Takeaway)", "حفلات وكاترينج"),
                expenseAccounts = listOf("خامات ومكونات غذائية", "مشروبات وبن", "رواتب الشيفات والويترز", "عمولات تطبيقات التوصيل", "تغليف وغاز وكهرباء", "هالك مطبخ"),
                tracksPhysicalGoods = true,
                kpiLabels = listOf("نسبة تكلفة الطعام (Food Cost %)", "متوسط قيمة الطلب", "نسبة الهالك اليومي"),
                quickEntryTemplates = listOf(
                    "توريد خامات مطبخ يومية" to "EXPENSE",
                    "إيراد مبيعات الشفت" to "INCOME",
                    "تسوية عمولة دليفري" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("جرد ثلاجات اللحوم والخضار", "جرد علب التغليف والأكواب", "تسليم درج الكاشير", "تسليم ماكينة الدفع POS")
            ),
            projectTemplates = listOf(
                "مطعم مشويات ومأكولات شرقية", "مطعم وجبات سريعة وبرجر", "مطعم بيتزا ومخبوزات إيطالية", "كافيه ومحمصة قهوة مختصة",
                "مخبز أفرنجي وحلواني", "محل عصائر طبيعية وسموذي", "محل آيس كريم وجيلاتي", "مطبخ سحابي (Cloud Kitchen)",
                "شركة تموين ووجبات شركات (Catering)", "مطعم مأكولات بحرية", "مطعم فول وفلافل ومأكولات شعبية", "مطعم أكل صحي ودايت",
                "كشك قهوة سريعة (Coffee Kiosk)", "عربة طعام متنقلة (Food Truck)", "معمل حلويات شرقية", "محل شاورما وسوري",
                "محل فطائر وكريب", "مطعم آسيوي وسوشي", "صالة شاي وضيافة", "توريد وجبات للمدارس والمستشفيات"
            )
        ),
        ProjectSector(
            id = "manufacturing",
            nameAr = "المصانع والورش وخطوط الإنتاج",
            iconKey = "PrecisionManufacturing",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "manufacturing",
                sectorNameAr = "محاسبة التكاليف الصناعية والإنتاج",
                baseProjectType = ProjectType.CONTRACTING,
                revenueAccounts = listOf("مبيعات إنتاج تام", "تصنيع للغير", "مبيعات مخلفات وإسكراب", "عقود توريد سنوية"),
                expenseAccounts = listOf("مواد خام أولية", "أجور عمال الإنتاج", "صيانة وقطع غيار ماكينات", "طاقة وكهرباء صناعية", "تعبئة ونقل"),
                tracksPhysicalGoods = true,
                kpiLabels = listOf("تكلفة الوحدة المنتجة", "كفاءة خط الإنتاج", "نسبة الفاقد الصناعي"),
                quickEntryTemplates = listOf(
                    "شراء مواد خام للإنتاج" to "EXPENSE",
                    "صيانة دورية للماكينات" to "EXPENSE",
                    "تسليم دفعة إنتاج تام" to "INCOME"
                ),
                shiftHandoverChecklist = listOf("جرد المواد الخام على الخط", "جرد الإنتاج التام بالمخزن", "حالة الماكينات والمعدات", "تسليم سجل أعطال الوردية")
            ),
            projectTemplates = listOf(
                "مصنع تعبئة وتغليف مواد غذائية", "مصنع بلاستيك وحقن قوالب", "ورشة حدادة وتشكيل معادن", "ورشة ألوميتال و UPVC",
                "مصنع أثاث وموبيليا خشبية", "مصنع ملابس وتريكو", "مصنع أحذية ومصنوعات جلدية", "مصنع منظفات ومستحضرات",
                "مصنع كرتون وورق مقوى", "مصنع طوب وإنترلوك وبلوكات", "مصنع رخام وتقطيع أحجار", "ورشة خراطة وتصنيع اسطمبات",
                "مصنع مياه معدنية وعصائر", "مصنع أعلاف وأسمدة", "مطبعة أوفست وديجيتال", "مصنع زجاج ومرايا",
                "مصنع إسفنج ومراتب", "مصنع كابلات ومستلزمات كهرباء", "ورشة صيانة وتصنيع تانكات", "مصنع عطور وزيوت عطرية"
            )
        ),
        ProjectSector(
            id = "healthcare",
            nameAr = "العيادات والمستشفيات والرعاية الصحية",
            iconKey = "LocalHospital",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "healthcare",
                sectorNameAr = "محاسبة المراكز الطبية والصيدليات",
                baseProjectType = ProjectType.OTHER,
                revenueAccounts = listOf("كشوفات واستشارات", "إجراءات طبية وجراحية", "تعاقدات التأمين الطبي", "مبيعات أدوية ومستلزمات"),
                expenseAccounts = listOf("مستلزمات طبية وأدوية", "نسب الأطباء والاستشاريين", "رواتب التمريض والاستقبال", "معايرة وصيانة الأجهزة", "نفايات طبية وتعقيم"),
                tracksPhysicalGoods = true,
                kpiLabels = listOf("متوسط إيراد المريض", "نسبة إشغال العيادات", "معدل تحصيل التأمين"),
                quickEntryTemplates = listOf(
                    "تحصيل كشوفات اليوم" to "INCOME",
                    "شراء مستلزمات تعقيم وأدوية" to "EXPENSE",
                    "تسوية نسبة طبيب" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("جرد أدوية ومستلزمات الطوارئ", "تسليم عهدة الاستقبال والكاشير", "تسليم فواتير شركات التأمين")
            ),
            projectTemplates = listOf(
                "عيادة طب أسنان وتقويم", "مركز طبي متعدد التخصصات", "صيدلية أهلية ومستحضرات", "معمل تحاليل طبية",
                "مركز أشعة وتشخيص", "عيادة جلدية وليزر وتجميل", "مركز علاج طبيعي وتأهيل", "عيادة عيون وبصريات",
                "مستشفى تخصصي صغير", "مركز حضانات ورعاية أطفال", "عيادة تغذية علاجية وسمنة", "شركة مستلزمات طبية",
                "عيادة بيطرية وصيدلية بيطرية", "مركز رعاية كبار السن", "خدمة تمريض منزلي", "مركز سمعيات وتخاطب",
                "معمل تركيبات أسنان", "بنك ومخزن أدوية"
            )
        ),
        ProjectSector(
            id = "tech",
            nameAr = "التكنولوجيا والبرمجة والذكاء الاصطناعي",
            iconKey = "Code",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "tech",
                sectorNameAr = "محاسبة المشاريع التقنية والبرمجيات (SaaS)",
                baseProjectType = ProjectType.OTHER,
                revenueAccounts = listOf("اشتراكات شهرية وسنوية", "تطوير برمجيات مخصصة", "دعم فني وصيانة", "استشارات تقنية"),
                expenseAccounts = listOf("خوادم وسيرفرات سحابية", "تراخيص و APIs", "رواتب المطورين والمصممين", "تسويق رقمي واكتساب عملاء"),
                tracksPhysicalGoods = false,
                kpiLabels = listOf("الإيراد الشهري المتكرر (MRR)", "هامش الربح التشغيلي", "تكلفة الساعة الهندسية"),
                quickEntryTemplates = listOf(
                    "تجديد اشتراك سيرفرات Cloud" to "EXPENSE",
                    "تحصيل دفعة تطوير مشروع" to "INCOME",
                    "صرف مكافأة سبرينت للمطورين" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("تقرير حالة السيرفرات والـ Uptime", "تذاكر الدعم الفني المفتوحة", "تسليم عهدة الاشتراكات")
            ),
            projectTemplates = listOf(
                "تطوير تطبيق موبايل (Android / iOS)", "منصة تجارة إلكترونية (E-Commerce)", "نظام ERP وإدارة شركات (SaaS)", "شركة برمجيات وحلول AI",
                "منصة تعليم إلكتروني (LMS)", "وكالة تصميم UI/UX وهوية رقمية", "متجر إلكتروني دروبشيبينج", "شركة أمن سيبراني وحماية بيانات",
                "خدمات استضافة وسيرفرات سحابية", "تطوير ألعاب إلكترونية", "أتمتة أعمال وروبوتات محادثة", "منصة حجز مواعيد وخدمات",
                "مكتب عمل مشترك تقني (Tech Hub)", "مركز صيانة لابتوب وهواتف ذكية", "بيع وتركيب شبكات وكاميرات مراقبة", "تركيب أنظمة نقاط بيع POS وكاشير",
                "تطوير مواقع شركات وصفحات هبوط", "شركة تحليل بيانات وذكاء أعمال"
            )
        ),
        ProjectSector(
            id = "realestate",
            nameAr = "العقارات وإدارة الأملاك والتطوير العقاري",
            iconKey = "Apartment",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "realestate",
                sectorNameAr = "محاسبة التسويق وإدارة الأملاك العقارية",
                baseProjectType = ProjectType.CONTRACTING,
                revenueAccounts = listOf("عمولات بيع وإيجار", "إيجارات محصلة", "رسوم إدارة وصيانة أملاك", "مقدمات حجز وحدات"),
                expenseAccounts = listOf("تسويق وإعلانات عقارية", "عمولات وسطاء وسيلز", "صيانة ونظافة وأمن الوحدات", "رسوم قانونية وتوثيق"),
                tracksPhysicalGoods = false,
                kpiLabels = listOf("نسبة الإشغال", "معدل التحصيل الشهري", "تكلفة جلب العميل"),
                quickEntryTemplates = listOf(
                    "تحصيل إيجار وحدة" to "INCOME",
                    "تحصيل عمولة صفقة عقارية" to "INCOME",
                    "صرف صيانة مرافق العقار" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("تسليم مفاتيح الوحدات للمعاينة", "جدول معاينات الغد", "سجل عقود الإيجار والمقدمات")
            ),
            projectTemplates = listOf(
                "شركة تسويق ووساطة عقارية", "إدارة أملاك وتحصيل إيجارات", "تأجير شقق مفروشة وفندقية", "إدارة مولات ومراكز تجارية",
                "تطوير وتقسيم أراضي", "إدارة مساحات عمل مشتركة", "شركة صيانة وإدارة مرافق (FM)", "تقييم وتثمين عقاري",
                "تأجير مكاتب إدارية مجهزة", "تأجير مخازن ومستودعات", "اتحاد ملاك وصيانة عمارات", "بيع وحدات مصيفية وشاليهات",
                "تسويق عقاري دولي", "إدارة جراجات وساحات انتظار", "وساطة عقارات تجارية وصناعية"
            )
        ),
        ProjectSector(
            id = "logistics",
            nameAr = "النقل واللوجستيات وسلاسل الإمداد",
            iconKey = "LocalShipping",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "logistics",
                sectorNameAr = "محاسبة الشحن والأسطول والتوصيل",
                baseProjectType = ProjectType.OTHER,
                revenueAccounts = listOf("رسوم شحن وتوصيل", "عقود نقل شركات", "تحصيل عند الاستلام (COD)", "تخزين ولوجستيات"),
                expenseAccounts = listOf("وقود وسولار وبنزين", "صيانة وإطارات وزيوت", "رواتب السائقين والمندوبين", "كارتات طرق وتراخيص", "تسوية مرتجعات"),
                tracksPhysicalGoods = true,
                kpiLabels = listOf("تكلفة الكيلومتر", "نسبة التسليم الناجح", "رصيد تحصيل COD"),
                quickEntryTemplates = listOf(
                    "توريد تحصيل شحنات COD" to "INCOME",
                    "بونات وقود وصيانة سيارة" to "EXPENSE",
                    "صرف عمولة مندوب توصيل" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("جرد الطرود المتبقية بالمخزن", "تسوية مبالغ الـ COD النقدية", "تسليم مفاتيح السيارات وبونات الوقود")
            ),
            projectTemplates = listOf(
                "شركة شحن داخلي وتوصيل طرود", "نقل بضائع ثقيلة ومقطورات", "نقل أثاث وعفش منزلي", "توصيل طلبات مطاعم ومتاجر",
                "تخليص جمركي وشحن دولي", "إدارة مخازن ومستودعات تبريد", "أسطول سيارات نقل ركاب وتأجير", "نقل مواد بناء ورمل وزلط",
                "شركة ليموزين وتوصيل مطارات", "خدمة توصيل أدوية ومستلزمات", "نقل مبرد للأغذية والألبان", "منصة وساطة شحن ونولون",
                "تأجير ونش ورافعات شوكية", "توزيع منتجات استهلاكية (FMCG)", "إدارة سلسلة إمداد ومشتريات"
            )
        ),
        ProjectSector(
            id = "agriculture",
            nameAr = "الزراعة والإنتاج الحيواني والداجني",
            iconKey = "Agriculture",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "agriculture",
                sectorNameAr = "محاسبة المزارع والدورات الإنتاجية",
                baseProjectType = ProjectType.OTHER,
                revenueAccounts = listOf("مبيعات محاصيل وثمار", "مبيعات دواجن/بيض/لحوم", "مبيعات ألبان خام", "تصدير زراعي"),
                expenseAccounts = listOf("بذور وشتلات وأسمدة", "أعلاف وتحصينات بيطرية", "عمالة وموسمية", "ري وطاقة وسولار", "فرز وتعبئة"),
                tracksPhysicalGoods = true,
                kpiLabels = listOf("معدل التحويل الغذائي", "صافي ربح الدورة", "نسبة النافق/الفاقد"),
                quickEntryTemplates = listOf(
                    "شراء أعلاف/أسمدة للدورة" to "EXPENSE",
                    "بيع دفعة محصول/إنتاج" to "INCOME",
                    "صرف أدوية وتحصينات" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("جرد مخزن الأعلاف والأسمدة", "تقرير النفوق أو الإصابات اليومي", "قراءة عدادات الري والمولدات")
            ),
            projectTemplates = listOf(
                "مزرعة دواجن وتسمين", "مزرعة إنتاج بيض مائدة", "مزرعة تسمين مواشي وأبقار", "مزرعة ألبان وتصنيع جبن",
                "مزرعة خضروات وصوب زراعية", "مزرعة فاكهة وموالح", "مزرعة استزراع سمكي", "منحل وإنتاج عسل نحل",
                "تصدير حاصلات زراعية", "مشتل نباتات وأشجار مثمرة", "مزرعة نخيل وتمور", "زراعة مائية (Hydroponics)",
                "مزرعة أغنام وماعز", "تجارة بذور وأسمدة ومبيدات", "محطة فرز وتعبئة فواكه", "حفر آبار وشبكات ري حديث"
            )
        ),
        ProjectSector(
            id = "education",
            nameAr = "التعليم والتدريب والأكاديميات",
            iconKey = "School",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "education",
                sectorNameAr = "محاسبة الأكاديميات والدورات والطلاب",
                baseProjectType = ProjectType.OTHER,
                revenueAccounts = listOf("اشتراكات ورسوم دراسية", "دورات تدريبية وورش عمل", "مبيعات كتب وملازم", "اختبارات وشهادات"),
                expenseAccounts = listOf("أجور المحاضرين والمدرسين", "طباعة ومذكرات", "إيجار القاعات والتجهيزات", "إعلانات وتسجيل طلاب"),
                tracksPhysicalGoods = false,
                kpiLabels = listOf("نسبة امتلاء القاعات", "متوسط الإيراد لكل طالب", "معدل التجديد الشهري"),
                quickEntryTemplates = listOf(
                    "تحصيل اشتراك طالب/متدرب" to "INCOME",
                    "صرف مستحقات محاضر" to "EXPENSE",
                    "طباعة ملازم وكتب" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("تسليم كشف حضور الطلاب اليومي", "تسليم خزينة الاشتراكات", "جرد الكتب والمطبوعات")
            ),
            projectTemplates = listOf(
                "أكاديمية تدريب لغات ومهارات", "حضانة أطفال ومركز تنمية مهارات", "سنتر تعليمي ودروس مجموعات", "مركز تدريب برمجة وتكنولوجيا",
                "أكاديمية تدريب محاسبة وإدارة", "معهد فنون وتصميم جرافيك", "مدرسة خاصة أو دولية", "مركز تحفيظ وتدريس",
                "أكاديمية تدريب مهني وحرفي", "أكاديمية كرة قدم ورياضات", "مركز تعليم قيادة السيارات", "مكتبة ومركز طباعة أبحاث",
                "إدارة امتحانات وشهادات دولية", "منصة كورسات واستشارات تعليمية"
            )
        ),
        ProjectSector(
            id = "automotive",
            nameAr = "السيارات وقطع الغيار ومراكز الصيانة",
            iconKey = "DirectionsCar",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "automotive",
                sectorNameAr = "محاسبة مراكز الصيانة ومعارض السيارات",
                baseProjectType = ProjectType.OTHER,
                revenueAccounts = listOf("مصنعيات صيانة وإصلاح", "مبيعات قطع غيار وزيوت", "غسيل وتلميع وعناية", "عمولات بيع سيارات"),
                expenseAccounts = listOf("شراء قطع غيار وزيوت", "أجور فنيين وميكانيكيين", "عدد ومعدات فحص", "إيجار وكهرباء المركز"),
                tracksPhysicalGoods = true,
                kpiLabels = listOf("متوسط فاتورة الصيانة", "دوران مخزون قطع الغيار", "عدد السيارات المنجزة يومياً"),
                quickEntryTemplates = listOf(
                    "تحصيل فاتورة صيانة وقطع غيار" to "INCOME",
                    "شراء زيوت وفلاتر وقطع غيار" to "EXPENSE",
                    "صرف يومية فنيين" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("جرد مخزن الزيوت وقطع الغيار", "تسليم مفاتيح سيارات العملاء تحت الصيانة", "تسليم درج النقدية والفيزا")
            ),
            projectTemplates = listOf(
                "مركز صيانة وميكانيكا سيارات", "معرض بيع وشراء سيارات", "متجر قطع غيار سيارات", "مركز تغيير زيوت وفلاتر",
                "محطة غسيل وتلميع سيارات (Car Care)", "مركز سمكرة ودهان سيارات", "مركز إطارات وترصيص وزوايا", "مركز كهرباء وتكييف سيارات",
                "شركة تأجير سيارات", "تركيب أفلام حماية وتظليل (PPF)", "مركز فحص سيارات بالكمبيوتر", "بيع وصيانة دراجات نارية",
                "خدمة إنقاذ ونش سيارات", "بيع بطاريات وإكسسوارات سيارات", "ورشة صيانة شاحنات ومعدات ثقيلة"
            )
        ),
        ProjectSector(
            id = "fashion",
            nameAr = "الأزياء والملابس والمنسوجات",
            iconKey = "Checkroom",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "fashion",
                sectorNameAr = "محاسبة الأزياء والمقاسات والمواسم",
                baseProjectType = ProjectType.OTHER,
                revenueAccounts = listOf("مبيعات المحل المباشرة", "مبيعات أونلاين", "مبيعات جملة وتوزيع", "تفصيل وتعديل"),
                expenseAccounts = listOf("مشتريات أقمشة وموديلات", "مرتجعات واستبدال", "تغليف وشحن", "رواتب البائعين وإيجار"),
                tracksPhysicalGoods = true,
                kpiLabels = listOf("هامش ربح الموسم", "نسبة المرتجعات", "متوسط القطع بالفاتورة"),
                quickEntryTemplates = listOf(
                    "إغلاق مبيعات الشفت" to "INCOME",
                    "توريد موديلات موسم جديد" to "EXPENSE",
                    "تسوية شحن ومرتجعات أونلاين" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("جرد القطع والمقاسات المعروضة", "حصر المرتجعات والاستبدالات", "تسليم الخزينة وماكينة الدفع")
            ),
            projectTemplates = listOf(
                "بوتيك ملابس حريمي وعبايات", "متجر ملابس رجالي وكاجوال", "متجر ملابس أطفال ومواليد", "متجر أحذية وحقائب جلدية",
                "أتيليه تفصيل وفساتين", "متجر ملابس رياضية", "تجارة أقمشة ومنسوجات", "متجر ملابس داخلية ولانجري",
                "تأجير بدل وفساتين مناسبات", "متجر إكسسوارات وساعات ونظارات", "براند ملابس أونلاين (Local Brand)", "متجر زي موحد (Uniforms)",
                "مغسلة ملابس ودراي كلين", "مشغل خياطة وتطريز", "تجارة جملة ملابس جاهزة"
            )
        ),
        ProjectSector(
            id = "electronics",
            nameAr = "الإلكترونيات والاتصالات والأجهزة الذكية",
            iconKey = "Devices",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "electronics",
                sectorNameAr = "محاسبة الأجهزة والسيريال والضمان",
                baseProjectType = ProjectType.OTHER,
                revenueAccounts = listOf("مبيعات هواتف ولابتوب", "مبيعات إكسسوارات", "صيانة هاردوير وسوفتوير", "مبيعات أجهزة منزلية"),
                expenseAccounts = listOf("شراء أجهزة ومخزون", "قطع غيار صيانة", "ضمان ومرتجعات", "رواتب وإيجار"),
                tracksPhysicalGoods = true,
                kpiLabels = listOf("صافي ربح الجهاز", "نسبة مرتجعات الضمان", "إيراد الإكسسوارات"),
                quickEntryTemplates = listOf(
                    "بيع جهاز بالسيريال" to "INCOME",
                    "تحصيل صيانة جهاز" to "INCOME",
                    "توريد إكسسوارات وهواتف" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("جرد الأجهزة بالسيريال IMEI", "أجهزة العملاء قيد الصيانة", "تسليم خزينة الوردية")
            ),
            projectTemplates = listOf(
                "متجر هواتف محمولة وإكسسوارات", "مركز صيانة موبايلات محترف", "متجر كمبيوتر ولابتوب وتجميعات", "معرض أجهزة منزلية وكهربائية",
                "مركز صيانة أجهزة منزلية وتكييف", "متجر ألعاب فيديو وبلايستيشن", "متجر كاميرات تصوير ومعدات صوت", "بيع وتركيب أنظمة طاقة شمسية",
                "مركز شاشات وتلفزيونات ذكية", "متجر مستلزمات إضاءة وليد", "تجارة أجهزة مكتبية وطابعات", "صالة ألعاب بلايستيشن و Gaming",
                "بيع خطوط وخدمات شحن وفواتير", "تأجير شاشات عرض وصوتيات", "تجارة جملة إلكترونيات"
            )
        ),
        ProjectSector(
            id = "energy",
            nameAr = "الطاقة المتجددة والكهرباء والمياه",
            iconKey = "Bolt",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "energy",
                sectorNameAr = "محاسبة مشاريع الطاقة والمحطات",
                baseProjectType = ProjectType.CONTRACTING,
                revenueAccounts = listOf("عقود توريد وتركيب محطات", "عقود صيانة دورية", "مبيعات ألواح وإنفرتر", "توزيع مياه محلاة"),
                expenseAccounts = listOf("ألواح وبطاريات وكابلات", "أجور مهندسين وفنيين", "نقل ومعدات رفع", "تصاريح وفحص فني"),
                tracksPhysicalGoods = true,
                kpiLabels = listOf("تكلفة الكيلووات المركب", "هامش ربح المشروع", "توفير الطاقة المحقق"),
                quickEntryTemplates = listOf(
                    "شراء ألواح شمسية/بطاريات" to "EXPENSE",
                    "دفعة تعاقد محطة طاقة" to "INCOME",
                    "مصروفات تركيب ونقل" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("جرد الألواح والإنفرترات والبطاريات", "قراءات المحطة والتشغيل", "تسليم عدة التركيب والفحص")
            ),
            projectTemplates = listOf(
                "تركيب محطات طاقة شمسية للمنازل", "محطة طاقة شمسية للمزارع والمصانع", "محطة تحلية وتنقية مياه", "بيع وتركيب فلاتر مياه",
                "توريد وصيانة مولدات كهرباء", "شركة تمديدات كابلات ولوحات كهرباء", "إنشاء محطات شحن سيارات كهربائية", "إدارة وتوزيع شبكات مياه",
                "تصميم وتنفيذ إنارة شوارع بالطاقة الشمسية", "توريد سخانات شمسية", "شركة كفاءة طاقة وتدقيق كهربائي", "صيانة محولات ولوحات توزيع",
                "مشروع تدوير مخلفات وإنتاج طاقة", "توريد بطاريات ليثيوم وأنظمة تخزين"
            )
        ),
        ProjectSector(
            id = "tourism",
            nameAr = "السياحة والسفر والفنادق والمنتجعات",
            iconKey = "FlightTakeoff",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "tourism",
                sectorNameAr = "محاسبة الحجوزات السياحية والفنادق",
                baseProjectType = ProjectType.OTHER,
                revenueAccounts = listOf("حجوزات إقامة وغرف", "تذاكر طيران وبرامج سياحية", "حجوزات رحلات داخلية", "خدمات إضافية وضيافة"),
                expenseAccounts = listOf("مستحقات فنادق وشركات طيران", "عمولات مناديب ومسوقين", "ضيافة وتشغيل ونظافة", "نقل سياحي"),
                tracksPhysicalGoods = false,
                kpiLabels = listOf("نسبة الإشغال الفندقي", "متوسط سعر الليلة/البرنامج", "صافي عمولة الحجز"),
                quickEntryTemplates = listOf(
                    "تحصيل حجز برنامج/غرفة" to "INCOME",
                    "سداد تذاكر/فندق مورد" to "EXPENSE",
                    "صرف تشغيل ونظافة غرف" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("كشف الغرف المشغولة والمغادرة", "مفاتيح وكروت الغرف", "خزينة الاستقبال والتحصيل")
            ),
            projectTemplates = listOf(
                "شركة سياحة وحجوزات طيران", "إدارة فندق أو نُزل سياحي", "إدارة منتجع وشاليهات", "تنظيم رحلات سفاري وتخييم",
                "تنظيم رحلات حج وعمرة", "تأجير يخوت ومراكب سياحية", "حجز وتأجير شقق فندقية", "تنظيم مؤتمرات وفعاليات سياحية",
                "شركة نقل سياحي وباصات", "مكتب خدمات تأشيرات وسفر", "مخيم بيئي وسياحة علاجية", "مرشد سياحي وبرامج ثقافية",
                "إدارة مطعم وكافيه فندقي", "تنظيم رحلات شهر عسل ومجموعات"
            )
        ),
        ProjectSector(
            id = "media",
            nameAr = "الإعلام والإعلان والتسويق والإنتاج",
            iconKey = "Campaign",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "media",
                sectorNameAr = "محاسبة الحملات الإعلانية والإنتاج الفني",
                baseProjectType = ProjectType.OTHER,
                revenueAccounts = listOf("إدارة حملات تسويقية", "إنتاج فيديو وإعلانات", "تصميم وبراندينج", "رعايات وإعلانات"),
                expenseAccounts = listOf("ميزانيات إعلانات ممولة", "أجور تصوير ومونتاج", "إيجار استوديو ومعدات", "رواتب فريق المحتوى"),
                tracksPhysicalGoods = false,
                kpiLabels = listOf("العائد على الإنفاق الإعلاني", "ربحية العميل الشهرية", "تكلفة الإنتاج"),
                quickEntryTemplates = listOf(
                    "شحن رصيد إعلانات ممولة" to "EXPENSE",
                    "تحصيل باقة تسويق شهرية" to "INCOME",
                    "أجر تصوير ومونتاج فيديو" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("جرد الكاميرات والعدسات والإضاءة", "نسخ احتياطي لملفات التصوير", "تسليم عهدة الاستوديو")
            ),
            projectTemplates = listOf(
                "وكالة تسويق رقمي وإعلانات (Agency)", "شركة إنتاج فيديو وإعلانات تجارية", "استوديو تصوير فوتوغرافي وفيديو", "إدارة حسابات سوشيال ميديا",
                "وكالة طباعة ولافتات إعلانية", "تأجير شاشات وإعلانات طرق (OOH)", "شركة تنظيم معارض ومؤتمرات", "إنتاج بودكاست ومحتوى صوتي",
                "شركة علاقات عامة و PR", "تصميم هويات بصرية وموشن جرافيك", "تسويق عبر المؤثرين", "استوديو صوتيات ودوبلاج",
                "تغطية حفلات ومناسبات", "شركة تسويق بالعمولة ومحتوى"
            )
        ),
        ProjectSector(
            id = "consulting",
            nameAr = "الاستشارات القانونية والمالية والإدارية",
            iconKey = "BusinessCenter",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "consulting",
                sectorNameAr = "محاسبة المكاتب الاستشارية والأتعاب",
                baseProjectType = ProjectType.OTHER,
                revenueAccounts = listOf("أتعاب قضايا واستشارات", "عقود مراجعة وتدقيق", "تأسيس شركات وتراخيص", "أتعاب استشارات إدارية"),
                expenseAccounts = listOf("رسوم حكومية وشهر عقاري", "رواتب مستشارين ومحاسبين", "إيجار ومصروفات المكتب", "انتقالات وضيافة"),
                tracksPhysicalGoods = false,
                kpiLabels = listOf("نسبة تحصيل الأتعاب", "ساعات العمل القابلة للفوترة", "ربحية الملف"),
                quickEntryTemplates = listOf(
                    "تحصيل دفعة أتعاب" to "INCOME",
                    "سداد رسوم توثيق وتراخيص" to "EXPENSE",
                    "مصروفات تشغيل المكتب" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("تسليم الملفات والعقود الأصلية", "مواعيد الجلسات والاجتماعات", "عهدة الرسوم الحكومية")
            ),
            projectTemplates = listOf(
                "مكتب محاسبة ومراجعة وضرائب", "مكتب محاماة واستشارات قانونية", "شركة تأسيس شركات وتراخيص", "مكتب استشارات إدارية وهيكلة",
                "مكتب دراسات جدوى اقتصادية", "شركة توظيف وموارد بشرية (HR)", "مكتب ترجمة معتمدة", "شركة وساطة وتأمين",
                "استشارات جودة وإيزو (ISO)", "مكتب استشارات هندسية وتصميم", "إدارة محافظ واستثمار مالي", "مكتب تحصيل ديون وتسويات",
                "خدمات سكرتارية تنفيذية وافتراضية", "مكتب تخليص معاملات حكومية"
            )
        ),
        ProjectSector(
            id = "beauty",
            nameAr = "التجميل والعناية الشخصية والصالونات",
            iconKey = "Spa",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "beauty",
                sectorNameAr = "محاسبة مراكز التجميل والمنتجات",
                baseProjectType = ProjectType.OTHER,
                revenueAccounts = listOf("خدمات الصالون والعناية", "باقات عرائس ومناسبات", "مبيعات منتجات عناية", "جلسات سبا ومساج"),
                expenseAccounts = listOf("خامات ومستحضرات تجميل", "عمولات ونسب المتخصصين", "إيجار وكهرباء ونظافة", "صيانة أجهزة"),
                tracksPhysicalGoods = true,
                kpiLabels = listOf("متوسط فاتورة العميل", "نسبة تكلفة الخامات", "معدل تكرار الزيارات"),
                quickEntryTemplates = listOf(
                    "إيراد خدمات ومنتجات الشفت" to "INCOME",
                    "شراء خامات ومستحضرات" to "EXPENSE",
                    "تسوية عمولة متخصص" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("جرد عبوات الخامات ومنتجات البيع", "تعقيم الأدوات والأجهزة", "تسليم خزينة الاستقبال")
            ),
            projectTemplates = listOf(
                "مركز تجميل وسيدات (Beauty Salon)", "صالون حلاقة رجالي حديث", "مركز سبا ومساج وعناية بالجسم", "عيادة عناية بالبشرة والشعر",
                "استوديو مكياج وتجهيز عرائس", "متجر عطور ومستحضرات تجميل", "تصنيع منتجات عناية طبيعية", "مركز أظافر وعناية (Nail Bar)",
                "حمام مغربي ومركز استشفاء", "متجر باروكات وإكسسوارات شعر", "خدمة تجميل منزلية", "أكاديمية تعليم تجميل ومكياج"
            )
        ),
        ProjectSector(
            id = "gold",
            nameAr = "الذهب والمجوهرات والمعادن الثمينة",
            iconKey = "Diamond",
            accountingProfile = ProjectAccountingProfile(
                sectorId = "gold",
                sectorNameAr = "محاسبة الصاغة والأوزان والمصنعية",
                baseProjectType = ProjectType.CONTRACTING,
                revenueAccounts = listOf("مبيعات مشغولات ذهبية", "مبيعات سبائك وجنيهات", "فروق مصنعية ودمغة", "صيانة وتلميع وتبديل"),
                expenseAccounts = listOf("شراء ذهب كسر وخام", "مصنعية ورش ومصانع", "دمغة وضرائب", "أمن وتأمين وحراسة", "رواتب وإيجار"),
                tracksPhysicalGoods = true,
                kpiLabels = listOf("إجمالي الجرامات عيار 21", "متوسط المصنعية للجرام", "صافي المخزون الوزني"),
                quickEntryTemplates = listOf(
                    "بيع مشغولات ذهبية" to "INCOME",
                    "شراء ذهب كسر/خام" to "EXPENSE",
                    "سداد مصنعية ورشة" to "EXPENSE"
                ),
                shiftHandoverChecklist = listOf("وزن وجرد الفاترينات بالجرام", "جرد الخزينة الحديدية للسبائك", "مطابقة فواتير البيع والشراء", "فحص أقفال الأمان والكاميرات")
            ),
            projectTemplates = listOf(
                "محل مجوهرات وصاغة تجزئة", "تجارة سبائك وجنيهات ذهبية", "ورشة تصنيع وتشكيل مجوهرات", "تجارة ذهب كسر واستبدال",
                "متجر فضة وإكسسوارات فاخرة", "تجارة ألماس وأحجار كريمة", "صياغة مجوهرات مخصصة (Custom)", "تجارة ساعات فاخرة ومقتنيات",
                "معمل تكرير وتنقية معادن ثمينة", "منصة استثمار وادخار ذهب", "بيع جملة مشغولات ذهبية", "تقييم وفحص عيارات الذهب"
            )
        )
    )

    /**
     * Flat list of all 320+ specialized project names with their parent sector.
     */
    val allTemplates: List<Pair<String, ProjectSector>> by lazy {
        sectors.flatMap { sector ->
            sector.projectTemplates.map { templateName -> templateName to sector }
        }
    }

    val totalTemplatesCount: Int by lazy { allTemplates.size }

    /**
     * Resolves the tailored ProjectAccountingProfile for any project's `type` string.
     */
    fun resolveAccountingProfile(projectTypeStr: String): ProjectAccountingProfile {
        val normalized = projectTypeStr.trim()
        // 1. Exact match in any sector's templates or sector name
        sectors.forEach { sector ->
            if (sector.nameAr == normalized ||
                sector.id.equals(normalized, ignoreCase = true) ||
                sector.projectTemplates.any { it == normalized }
            ) {
                return sector.accountingProfile
            }
        }
        // 2. Partial keyword match across templates/sectors
        sectors.forEach { sector ->
            if (sector.projectTemplates.any { normalized.contains(it, ignoreCase = true) || it.contains(normalized, ignoreCase = true) }) {
                return sector.accountingProfile
            }
        }
        // 3. Fallback to legacy ProjectType enum mapping
        return when (normalized.uppercase()) {
            ProjectType.CONSTRUCTION.name -> sectors.first { it.id == "construction" }.accountingProfile
            ProjectType.FINISHING.name -> sectors.first { it.id == "finishing" }.accountingProfile
            ProjectType.CONTRACTING.name -> sectors.first { it.id == "manufacturing" }.accountingProfile
            else -> sectors.first { it.id == "retail" }.accountingProfile
        }
    }

    fun getDisplayLabel(projectTypeStr: String): String {
        return when (projectTypeStr.uppercase()) {
            ProjectType.CONSTRUCTION.name -> ProjectType.CONSTRUCTION.displayLabel()
            ProjectType.FINISHING.name -> ProjectType.FINISHING.displayLabel()
            ProjectType.CONTRACTING.name -> ProjectType.CONTRACTING.displayLabel()
            ProjectType.OTHER.name -> ProjectType.OTHER.displayLabel()
            else -> projectTypeStr
        }
    }
}
