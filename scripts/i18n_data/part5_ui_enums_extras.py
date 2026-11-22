from common_langs import build_table

# 14-language tuple order:
# ar, en, fr, es, tr, de, zh, hi, ru, ja, pt, it, ur, fa

def get_part5():
    rows = {}
    def add(k, vals):
        rows[k] = vals

    # Metals, Karats & Project Types (Models.kt)
    add("metal.gold", (
        "ذهب", "Gold", "Or", "Oro", "Altın", "Gold",
        "黄金", "सोना", "Золото", "金", "Ouro", "Oro", "سونا", "طلا"
    ))
    add("metal.silver", (
        "فضة", "Silver", "Argent", "Plata", "Gümüş", "Silber",
        "白银", "चांदी", "Серебро", "銀", "Prata", "Argento", "چاندی", "نقره"
    ))
    add("gold.karat_14", (
        "عيار 14", "14K", "14 carats", "14 quilates", "14 Ayar", "14 Karat",
        "14K", "14 कैरेट", "585 проба (14K)", "14金", "14K", "14 carati", "14 قیراط", "عیار ۱۴"
    ))
    add("gold.karat_18", (
        "عيار 18", "18K", "18 carats", "18 quilates", "18 Ayar", "18 Karat",
        "18K", "18 कैरेट", "750 проба (18K)", "18金", "18K", "18 carati", "18 قیراط", "عیار ۱۸"
    ))
    add("gold.karat_21", (
        "عيار 21", "21K", "21 carats", "21 quilates", "21 Ayar", "21 Karat",
        "21K", "21 कैरेट", "875 проба (21K)", "21金", "21K", "21 carati", "21 قیراط", "عیار ۲۱"
    ))
    add("gold.karat_24", (
        "عيار 24", "24K", "24 carats", "24 quilates", "24 Ayar", "24 Karat",
        "24K", "24 कैरेट", "999 проба (24K)", "24金", "24K", "24 carati", "24 قیراط", "عیار ۲۴"
    ))
    add("silver.karat_999", (
        "عيار 999", "999 Fine", "Argent 999", "Plata 999", "999 Ayar", "999 Silber",
        "999足银", "999 शुद्ध", "999 проба", "純銀999", "Prata 999", "Argento 999", "999 خالص", "عیار ۹۹۹"
    ))
    add("silver.karat_925", (
        "عيار 925", "925 Sterling", "Argent 925", "Plata 925", "925 Ayar", "925 Sterling",
        "925纯银", "925 स्टर्लिंग", "925 проба", "スターリング925", "Prata 925", "Argento 925", "925 سٹرلنگ", "عیار ۹۲۵"
    ))
    add("silver.karat_800", (
        "عيار 800", "800 Silver", "Argent 800", "Plata 800", "800 Ayar", "800 Silber",
        "800银", "800 चांदी", "800 проба", "銀800", "Prata 800", "Argento 800", "800 چاندی", "عیار ۸۰۰"
    ))
    add("silver.karat_999_full", (
        "999 - فضة نقية", "999 - Pure Silver", "999 - Argent pur", "999 - Plata pura",
        "999 - Saf Gümüş", "999 - Feinsilber", "999 - 纯银", "999 - शुद्ध चांदी",
        "999 - Чистое серебро", "999 - 純銀", "999 - Prata Pura", "999 - Argento Puro",
        "999 - خالص چاندی", "۹۹۹ - نقره خالص"
    ))
    add("silver.karat_925_full", (
        "925 - فضة استرلينية", "925 - Sterling Silver", "925 - Argent sterling", "925 - Plata de ley",
        "925 - Sterling Gümüş", "925 - Sterlingsilber", "925 - 925纯银", "925 - स्टर्लिंग चांदी",
        "925 - Стерлинговое серебро", "925 - スターリングシルバー", "925 - Prata Esterlina", "925 - Argento Sterling",
        "925 - سٹرلنگ چاندی", "۹۲۵ - نقره استرلینگ"
    ))
    add("silver.karat_800_full", (
        "800 - فضة قياسية", "800 - Standard Silver", "800 - Argent standard", "800 - Plata estándar",
        "800 - Standart Gümüş", "800 - Standardsilber", "800 - 标准银", "800 - मानक चांदी",
        "800 - Стандартное серебро", "800 - 標準銀", "800 - Prata Padrão", "800 - Argento Standard",
        "800 - معیاری چاندی", "۸۰۰ - نقره استاندارد"
    ))
    add("project.type_construction", (
        "إنشاءات ومباني", "Construction", "Construction", "Construcción",
        "İnşaat ve Yapı", "Hochbau", "建筑工程", "निर्माण",
        "Строительство", "建設・建築", "Construção", "Costruzione",
        "تعمیرات", "ساخت‌وساز"
    ))
    add("project.type_finishing", (
        "تشطيبات وتجهيز", "Finishing & Fit-out", "Finitions", "Acabados",
        "İç Dekorasyon", "Ausbau & Design", "装修工程", "फिनिशिंग",
        "Отделка", "内装・仕上げ", "Acabamentos", "Finiture",
        "فنشنگ", "نازک‌کاری"
    ))
    add("project.type_contracting", (
        "مقاولات وتوريد", "Contracting & Supply", "Entreprise & Fourniture", "Contratación",
        "Müteahhitlik", "Bauwesen & Lieferung", "工程承包", "ठेका और आपूर्ति",
        "Подряд и поставки", "請負・供給", "Empreitada", "Appalti e Forniture",
        "ٹھیکیداری", "پیمانکاری و تامین"
    ))
    add("project.type_other", (
        "مشروع آخر", "Other Project", "Autre projet", "Otro proyecto",
        "Diğer Proje", "Anderes Projekt", "其他项目", "अन्य परियोजना",
        "Другой проект", "その他", "Outro Projeto", "Altro Progetto",
        "دیگر منصوبہ", "پروژه دیگر"
    ))

    # Broadcast Categories
    add("broadcast.cat_all", (
        "الجميع (كافة مستخدمي المنصة)", "Everyone (All Platform Users)", "Tous les utilisateurs", "Todos los usuarios",
        "Herkes (Tüm Kullanıcılar)", "Alle Plattform-Nutzer", "全体用户", "सभी उपयोगकर्ता",
        "Все пользователи", "全ユーザー", "Todos os usuários", "Tutti gli utenti",
        "تمام صارفین", "همه کاربران پلتفرم"
    ))
    add("broadcast.cat_business", (
        "رجال الأعمال والمستثمرون", "Business Owners & Investors", "Entrepreneurs et Investisseurs", "Empresarios e Inversores",
        "İş İnsanları ve Yatırımcılar", "Unternehmer & Investoren", "企业主与投资者", "व्यवसायी और निवेशक",
        "Бизнес и инвесторы", "経営者・投資家", "Empresários e Investidores", "Imprenditori e Investitori",
        "کاروباری حضرات", "کارآفرینان و سرمایه‌گذاران"
    ))
    add("broadcast.cat_traders", (
        "الشركات والتجار المعتمدون", "Verified Merchants & Gold Traders", "Commerçants et Négociants Vérifiés", "Comerciantes Verificados",
        "Onaylı Şirketler ve Tüccarlar", "Verifizierte Händler", "认证商户与黄金商", "सत्यापित व्यापारी",
        "Проверенные торговцы", "認定加盟店", "Comerciantes Verificados", "Commercianti Verificati",
        "تصدیق شدہ تاجر", "شرکت‌ها و بازرگانان تاییدشده"
    ))
    add("broadcast.cat_subaccounts", (
        "أصحاب الحسابات الفرعية والخزائن", "Sub-Accounts & Staff Vaults", "Sous-comptes et Coffres", "Subcuentas y Bóvedas",
        "Alt Hesaplar ve Kasalar", "Unterkonten & Tresore", "子账户与金库", "उप-खाते और वॉल्ट",
        "Субаккаунты и сейфы", "サブ口座・金庫", "Subcontas e Cofres", "Sotto-conti e Caveau",
        "ذیلی اکاؤنٹس", "زیرحساب‌ها و صندوق‌ها"
    ))
    add("broadcast.cat_workgroups", (
        "مجموعات وفرق العمل والمشاريع", "Workgroups & Project Teams", "Groupes de travail et Projets", "Grupos de trabajo y Proyectos",
        "Çalışma Grupları ve Projeler", "Arbeitsgruppen & Projekte", "工作组与项目团队", "कार्य समूह और परियोजनाएं",
        "Рабочие группы и проекты", "作業グループ・プロジェクト", "Grupos e Projetos", "Gruppi e Progetti",
        "ورک گروپس اور منصوبے", "گروه‌های کاری و پروژه‌ها"
    ))
    add("broadcast.cat_active", (
        "المستخدمين النشطين مؤخراً", "Recently Active Users", "Utilisateurs récemment actifs", "Usuarios activos recientes",
        "Son Aktif Kullanıcılar", "Kürzlich aktive Nutzer", "近期活跃用户", "हाल ही में सक्रिय",
        "Недавно активные", "最近アクティブ", "Ativos recentemente", "Attivi di recente",
        "حال ہی میں فعال", "کاربران فعال اخیر"
    ))

    # Theme Card & Font Scale (AppProvider.kt & Type.kt)
    add("theme.card_title", (
        "مظهر التطبيق ووضع الشاشة", "App Appearance & Screen Mode", "Apparence de l'application", "Apariencia de la app",
        "Uygulama Görünümü ve Tema", "App-Erscheinungsbild", "应用外观与显示模式", "ऐप स्वरूप और थीम",
        "Оформление и тема", "外観とスクリーンモード", "Aparência e Modo", "Aspetto dell'app",
        "ایپ تھیم اور ڈسپلے", "ظاهر برنامه و حالت صفحه"
    ))
    add("theme.switch_label", (
        "التبديل بين الداكن والفاتح", "Dark / Light Switch", "Mode sombre / clair", "Modo oscuro / claro",
        "Karanlık / Aydınlık Geçişi", "Dunkel / Hell wechseln", "深色/浅色切换", "डार्क / लाइट स्विच",
        "Тёмная / Светлая тема", "ダーク／ライト切替", "Alternar Escuro / Claro", "Cambia Scuro / Chiaro",
        "ڈارک / لائٹ سوئچ", "تغییر حالت تیره و روشن"
    ))
    add("theme.switch_dark_sub", (
        "واجهة مريحة للعينين في الإضاءة المنخفضة", "Eye-comfort interface for low light",
        "Interface reposante pour faible luminosité", "Interfaz cómoda con poca luz",
        "Düşük ışıkta göz dostu arayüz", "Augenschonend bei wenig Licht",
        "低光环境下护眼深色界面", "कम रोशनी के लिए आरामदायक इंटरफ़ेस",
        "Комфортный тёмный интерфейс", "暗所でも目に優しい画面",
        "Confortável para pouca luz", "Comoda in condizioni di scarsa luce",
        "کم روشنی کے لیے آرام دہ", "نمای راحت برای نور کم"
    ))
    add("theme.switch_light_sub", (
        "واجهة نهارية مشرقة عالية الوضوح", "Bright high-clarity daylight interface",
        "Interface claire haute lisibilité", "Interfaz diurna de alta claridad",
        "Parlak ve yüksek netlikli gündüz arayüzü", "Helle Tageslicht-Oberfläche",
        "清晰明亮的日间界面", "स्पष्ट और चमकदार दिन इंटरफ़ेस",
        "Яркий дневной интерфейс", "日中でも見やすい明るい画面",
        "Interface clara diurna", "Interfaccia diurna nitida",
        "واضح دن کی روشنی والا انٹرفیس", "نمای روشن و شفاف روزانه"
    ))
    add("theme.switch_to_light", (
        "التبديل إلى الوضع الفاتح", "Switch to Light Mode", "Passer au mode clair", "Cambiar a modo claro",
        "Aydınlık Moda Geç", "Zum hellen Modus", "切换至浅色模式", "लाइट मोड पर जाएं",
        "Переключить на светлую", "ライトモードへ", "Mudar para Claro", "Passa a Chiaro",
        "لائٹ موڈ پر جائیں", "تغییر به حالت روشن"
    ))
    add("theme.switch_to_dark", (
        "التبديل إلى الوضع الداكن", "Switch to Dark Mode", "Passer au mode sombre", "Cambiar a modo oscuro",
        "Karanlık Moda Geç", "Zum dunklen Modus", "切换至深色模式", "डार्क मोड पर जाएं",
        "Переключить на тёмную", "ダークモードへ", "Mudar para Escuro", "Passa a Scuro",
        "ڈارک موڈ پر جائیں", "تغییر به حالت تیره"
    ))
    add("font_scale.card_title", (
        "حجم الخط ونصوص التطبيق", "App Font Size & Readability", "Taille de police", "Tamaño de fuente",
        "Yazı Boyutu ve Okunabilirlik", "Schriftgröße", "字体大小与可读性", "फ़ॉन्ट आकार",
        "Размер шрифта", "フォントサイズ", "Tamanho da fonte", "Dimensione carattere",
        "فونٹ سائز", "اندازه فونت برنامه"
    ))
    add("font_scale.card_sub", (
        "تكبير النصوص لسهولة القراءة ودعم ضعاف النظر", "Adjust text scale for comfort & accessibility",
        "Ajustez la taille du texte pour le confort", "Ajusta el texto para mejor lectura",
        "Rahat okuma ve erişilebilirlik için metni büyütün", "Textgröße für bessere Lesbarkeit anpassen",
        "调整文字大小以提升可读性", "आसान पढ़ने के लिए टेक्स्ट आकार बदलें",
        "Настройка масштаба текста", "読みやすさに合わせて文字サイズを調整",
        "Ajuste a escala de texto", "Regola la leggibilità del testo",
        "بہتر مطالعہ کے لیے متن بڑا کریں", "تنظیم اندازه متن برای خوانایی بهتر"
    ))
    add("font_scale.preview_label", (
        "معاينة فورية:", "Live Preview:", "Aperçu :", "Vista previa:",
        "Anlık Önizleme:", "Vorschau:", "实时预览：", "लाइव पूर्वावलोकन:",
        "Предпросмотр:", "プレビュー:", "Pré-visualização:", "Anteprima:",
        "فوری جھلک:", "پیش‌نمایش زنده:"
    ))
    add("font_scale.preview_sample", (
        "الذهب عيار 21: 3,450 | الرصيد: 125,000", "21K Gold: 3,450 | Balance: 125,000",
        "Or 21K : 3 450 | Solde : 125 000", "Oro 21K: 3,450 | Saldo: 125,000",
        "21 Ayar Altın: 3.450 | Bakiye: 125.000", "21K Gold: 3.450 | Kontostand: 125.000",
        "21K 黄金: 3,450 | 余额: 125,000", "21K सोना: 3,450 | शेष: 125,000",
        "Золото 21K: 3 450 | Баланс: 125 000", "21金: 3,450 | 残高: 125,000",
        "Ouro 21K: 3.450 | Saldo: 125.000", "Oro 21K: 3.450 | Saldo: 125.000",
        "21 قیراط سونا: 3,450 | بیلنس: 125,000", "طلای ۲۱ عیار: ۳,۴۵۰ | موجودی: ۱۲۵,۰۰۰"
    ))
    add("font_scale.standard_t", (
        "الحجم القياسي (100%)", "Standard (100%)", "Standard (100%)", "Estándar (100%)",
        "Standart (%100)", "Standard (100%)", "标准 (100%)", "मानक (100%)",
        "Стандартный (100%)", "標準 (100%)", "Padrão (100%)", "Standard (100%)",
        "معیاری (100%)", "استاندارد (۱۰۰٪)"
    ))
    add("font_scale.standard_d", (
        "الحجم الافتراضي لواجهات التطبيق", "Default app interface scale",
        "Échelle par défaut", "Escala predeterminada",
        "Varsayılan arayüz ölçeği", "Standard-Schriftgröße",
        "默认应用界面尺寸", "डिफ़ॉल्ट ऐप आकार",
        "Масштаб по умолчанию", "標準の表示サイズ",
        "Escala padrão do app", "Scala predefinita",
        "ایپ کا ڈیفالٹ سائز", "اندازه پیش‌فرض برنامه"
    ))
    add("font_scale.comfortable_t", (
        "مريح ومحسن (115%)", "Comfortable (115%)", "Confortable (115%)", "Cómodo (115%)",
        "Rahat (%115)", "Komfortabel (115%)", "舒适 (115%)", "आरामदायक (115%)",
        "Комфортный (115%)", "快適 (115%)", "Confortável (115%)", "Confortevole (115%)",
        "آرام دہ (115%)", "راحت (۱۱۵٪)"
    ))
    add("font_scale.comfortable_d", (
        "قراءة أسهل ومريحة للعين", "Easier and eye-friendly reading",
        "Lecture plus facile", "Lectura más cómoda",
        "Göz için daha rahat okuma", "Angenehmer zu lesen",
        "护眼易读放大比例", "आंखों के लिए आरामदायक",
        "Удобнее для чтения", "目に優しいサイズ",
        "Leitura mais confortável", "Lettura più rilassante",
        "پڑھنے میں آسان", "خواندن راحت‌تر"
    ))
    add("font_scale.large_t", (
        "كبير وواضح (130%)", "Large & Clear (130%)", "Grand (130%)", "Grande (130%)",
        "Büyük ve Net (%130)", "Groß (130%)", "大号清晰 (130%)", "बड़ा और स्पष्ट (130%)",
        "Крупный (130%)", "大 (130%)", "Grande (130%)", "Grande (130%)",
        "بڑا اور واضح (130%)", "بزرگ و واضح (۱۳۰٪)"
    ))
    add("font_scale.large_d", (
        "نصوص وأرقام بارزة وعريضة", "Prominent numbers and bold labels",
        "Textes et chiffres mis en avant", "Textos y cifras destacados",
        "Belirgin metin ve rakamlar", "Gut sichtbare Zahlen & Texte",
        "醒目数字与清晰标签", "स्पष्ट अक्षर और संख्याएं",
        "Крупные цифры и заголовки", "数字と文字がくっきり",
        "Números e textos destacados", "Testi e numeri ben visibili",
        "نمایاں حروف اور اعداد", "متون و اعداد برجسته"
    ))
    add("font_scale.xlarge_t", (
        "فائق الوضوح (145%)", "Extra Large (145%)", "Très grand (145%)", "Muy grande (145%)",
        "Ekstra Büyük (%145)", "Extra Groß (145%)", "超大清晰 (145%)", "अतिरिक्त बड़ा (145%)",
        "Очень крупный (145%)", "特大 (145%)", "Extra Grande (145%)", "Molto grande (145%)",
        "انتہائی واضح (145%)", "فوق‌العاده بزرگ (۱۴۵٪)"
    ))
    add("font_scale.xlarge_d", (
        "مخصص لضعاف النظر وكبار السن", "Accessibility maximum legibility",
        "Accessibilité maximale", "Máxima accesibilidad visual",
        "Maksimum erişilebilirlik ve okunabilirlik", "Maximale Barrierefreiheit",
        "无障碍最高清晰度", "अधिकतम पठनीयता",
        "Максимальная чёткость", "最大視認性モード",
        "Legibilidade máxima", "Massima accessibilità visiva",
        "زیادہ سے زیادہ وضاحت", "حداکثر خوانایی و دسترسی‌پذیری"
    ))

    # MainActivity Security Guard & Crash Recovery
    add("security.dev_mode_title", (
        "وضع المطور مفعّل 🛡️", "Developer Mode Enabled 🛡️", "Mode développeur activé 🛡️", "Modo desarrollador activado 🛡️",
        "Geliştirici Modu Etkin 🛡️", "Entwicklermodus aktiv 🛡️", "开发者模式已开启 🛡️", "डेवलपर मोड सक्रिय 🛡️",
        "Режим разработчика включён 🛡️", "開発者モード有効 🛡️", "Modo Programador Ativo 🛡️", "Modalità sviluppatore attiva 🛡️",
        "ڈویلپر موڈ فعال ہے 🛡️", "حالت توسعه‌دهنده فعال است 🛡️"
    ))
    add("security.dev_mode_desc", (
        "لحماية بياناتك المالية، يُرجى إيقاف خيارات المطور وتصحيح USB للمتابعة بأمان.",
        "To protect your financial records, please turn off Developer Options and USB Debugging to continue safely.",
        "Pour protéger vos données financières, désactivez les options développeur et le débogage USB.",
        "Para proteger sus datos financieros, desactive las opciones de desarrollador y la depuración USB.",
        "Finansal verilerinizi korumak için lütfen Geliştirici Seçeneklerini ve USB Hata Ayıklamayı kapatın.",
        "Zum Schutz Ihrer Finanzdaten deaktivieren Sie bitte die Entwickleroptionen und USB-Debugging.",
        "为保护您的财务数据安全，请关闭开发者选项和 USB 调试后继续。",
        "अपने वित्तीय डेटा की सुरक्षा के लिए कृपया डेवलपर विकल्प और USB डिबगिंग बंद करें।",
        "Для защиты финансовых данных отключите параметры разработчика и отладку по USB.",
        "財務データ保護のため、開発者向けオプションとUSBデバッグをオフにしてください。",
        "Para proteger seus dados financeiros, desative as Opções do Desenvolvedor e Depuração USB.",
        "Per proteggere i tuoi dati finanziari, disattiva Opzioni sviluppatore e Debug USB.",
        "اپنے مالی ڈیٹا کی حفاظت کے لیے ڈویلپر آپشنز اور USB ڈیبگنگ بند کریں۔",
        "برای محافظت از داده‌های مالی، گزینه‌های توسعه‌دهنده و اشکال‌زدایی USB را غیرفعال کنید."
    ))
    add("security.dev_options", (
        "خيارات المطور", "Developer Options", "Options développeur", "Opciones de desarrollador",
        "Geliştirici Seçenekleri", "Entwickleroptionen", "开发者选项", "डेवलपर विकल्प",
        "Для разработчиков", "開発者向けオプション", "Opções do Desenvolvedor", "Opzioni sviluppatore",
        "ڈویلپر آپشنز", "گزینه‌های توسعه‌دهنده"
    ))
    add("security.usb_debug", (
        "تصحيح USB", "USB Debugging", "Débogage USB", "Depuración USB",
        "USB Hata Ayıklama", "USB-Debugging", "USB 调试", "USB डिबगिंग",
        "Отладка по USB", "USBデバッグ", "Depuração USB", "Debug USB",
        "USB ڈیبگنگ", "اشکال‌زدایی USB"
    ))
    add("security.enabled_warn", (
        "مفعّلة ⚠️", "Enabled ⚠️", "Activé ⚠️", "Activado ⚠️",
        "Etkin ⚠️", "Aktiviert ⚠️", "已开启 ⚠️", "सक्रिय ⚠️",
        "Включено ⚠️", "有効 ⚠️", "Ativo ⚠️", "Attivo ⚠️",
        "فعال ⚠️", "فعال ⚠️"
    ))
    add("security.disabled_ok", (
        "معطّلة ✅", "Disabled ✅", "Désactivé ✅", "Desactivado ✅",
        "Devre Dışı ✅", "Deaktiviert ✅", "已关闭 ✅", "निष्क्रिय ✅",
        "Отключено ✅", "無効 ✅", "Desativado ✅", "Disattivato ✅",
        "غیر فعال ✅", "غیرفعال ✅"
    ))
    add("security.open_settings", (
        "فتح الإعدادات", "Open Settings", "Ouvrir les paramètres", "Abrir ajustes",
        "Ayarları Aç", "Einstellungen öffnen", "打开设置", "सेटिंग्स खोलें",
        "Открыть настройки", "設定を開く", "Abrir Configurações", "Apri Impostazioni",
        "ترتیبات کھولیں", "باز کردن تنظیمات"
    ))
    add("security.recheck", (
        "إعادة الفحص", "Re-check Status", "Revérifier", "Volver a comprobar",
        "Tekrar Kontrol Et", "Erneut prüfen", "重新检测", "पुनः जांचें",
        "Проверить снова", "再チェック", "Verificar novamente", "Ricontrolla",
        "دوبارہ چیک کریں", "بررسی مجدد"
    ))
    add("security.exit_app", (
        "إغلاق التطبيق", "Exit App", "Quitter", "Salir de la app",
        "Uygulamadan Çık", "App schließen", "退出应用", "ऐप बंद करें",
        "Выйти", "アプリを終了", "Sair do App", "Esci dall'app",
        "ایپ بند کریں", "خروج از برنامه"
    ))
    add("security.crash_title", (
        "حدث استثناء بسيط في التطبيق", "Minor Application Exception", "Erreur mineure détectée", "Ocurrió una excepción menor",
        "Küçük Bir Uygulama Hatası Oluştu", "Unerwarteter Fehler", "应用遇到轻微异常", "एक मामूली त्रुटि हुई",
        "Незначительный сбой", "一時的なエラーが発生しました", "Ocorreu um erro leve", "Si è verificato un errore",
        "ایپ میں معمولی خرابی", "خطای جزئی در برنامه رخ داد"
    ))
    add("security.crash_desc", (
        "تم الحفاظ على بياناتك بأمان. يمكنك إعادة تحميل التطبيق الآن.",
        "Your financial records are safely preserved. You can reload the app now.",
        "Vos données sont en sécurité. Rechargez l'application maintenant.",
        "Sus datos están seguros. Puede recargar la aplicación ahora.",
        "Verileriniz güvende. Uygulamayı şimdi yeniden yükleyebilirsiniz.",
        "Ihre Daten sind sicher gespeichert. Laden Sie die App jetzt neu.",
        "您的数据已安全保存，现在可以重新加载应用。",
        "आपका डेटा सुरक्षित है। अब ऐप पुनः लोड करें।",
        "Ваши данные в безопасности. Перезагрузите приложение.",
        "データは安全に保護されています。再読み込みしてください。",
        "Seus dados estão seguros. Recarregue o aplicativo.",
        "I tuoi dati sono al sicuro. Ricarica l'app ora.",
        "آپ کا ڈیٹا محفوظ ہے۔ ایپ دوبارہ لوڈ کریں۔",
        "اطلاعات شما ایمن است. می‌توانید برنامه را مجدد بارگذاری کنید."
    ))
    add("security.restart_app", (
        "إعادة تشغيل التطبيق", "Reload App", "Recharger l'app", "Reiniciar app",
        "Uygulamayı Yeniden Başlat", "App neu laden", "重新启动应用", "ऐप रीलोड करें",
        "Перезапустить", "再読み込み", "Reiniciar App", "Riavvia app",
        "ایپ دوبارہ چلائیں", "راه‌اندازی مجدد"
    ))

    # FCM Service fallback strings
    add("fcm.new_message", (
        "رسالة جديدة", "New message", "Nouveau message", "Nuevo mensaje",
        "Yeni mesaj", "Neue Nachricht", "新消息", "नया संदेश",
        "Новое сообщение", "新着メッセージ", "Nova mensagem", "Nuovo messaggio",
        "نیا پیغام", "پیام جدید"
    ))
    add("fcm.project_default", (
        "المشروع", "Project", "Projet", "Proyecto",
        "Proje", "Projekt", "项目", "परियोजना",
        "Проект", "プロジェクト", "Projeto", "Progetto",
        "منصوبہ", "پروژه"
    ))
    add("fcm.new_task", (
        "مهمة جديدة بالمشروع", "New Project Task", "Nouvelle tâche de projet", "Nueva tarea de proyecto",
        "Yeni Proje Görevi", "Neue Projektaufgabe", "新项目任务", "नया प्रोजेक्ट कार्य",
        "Новая задача проекта", "新しいプロジェクトタスク", "Nova tarefa do projeto", "Nuova attività",
        "نیا پراجیکٹ ٹاسک", "وظیفه جدید پروژه"
    ))
    add("fcm.task_updated", (
        "تم تعيين أو تحديث مهمة جديدة في خطة العمل", "A task was assigned or updated in your workflow",
        "Une tâche a été assignée ou mise à jour", "Se asignó o actualizó una tarea",
        "İş planında yeni bir görev atandı veya güncellendi", "Eine Aufgabe wurde zugewiesen oder aktualisiert",
        "工作计划中有任务已分配或更新", "कार्य योजना में नया कार्य अपडेट हुआ",
        "Назначена или обновлена задача", "タスクが割り当てまたは更新されました",
        "Uma tarefa foi atribuída ou atualizada", "Attività assegnata o aggiornata",
        "ورک پلان میں ٹاسک اپ ڈیٹ ہوا", "یک وظیفه در برنامه کاری تخصیص یا به‌روز شد"
    ))
    add("fcm.gold_deal_title", (
        "صفقة ذهب جديدة", "New Gold Deal", "Nouvelle transaction d'or", "Nueva operación de oro",
        "Yeni Altın İşlemi", "Neuer Gold-Deal", "新黄金交易", "नया सोना सौदा",
        "Новая сделка с золотом", "新しい金取引", "Novo negócio de ouro", "Nuova operazione oro",
        "نیا سونے کا سودا", "معامله جدید طلا"
    ))
    add("fcm.gold_deal_body", (
        "تم تسجيل أو تأكيد معاملة ذهب جديدة", "A new gold transaction was logged or confirmed",
        "Une transaction d'or a été enregistrée", "Se registró una operación de oro",
        "Yeni bir altın işlemi kaydedildi veya onaylandı", "Eine neue Goldtransaktion wurde bestätigt",
        "一笔新黄金交易已记录或确认", "नया स्वर्ण लेनदेन दर्ज किया गया",
        "Зафиксирована новая сделка с золотом", "新しい金取引が確認されました",
        "Nova transação de ouro confirmada", "Nuova transazione oro registrata",
        "نیا سونے کا لین دین ریکارڈ ہوا", "یک معامله جدید طلا ثبت یا تایید شد"
    ))
    add("fcm.system_title", (
        "تنبيه جديد من OOZOX", "New OOZOX Alert", "Nouvelle alerte OOZOX", "Nueva alerta de OOZOX",
        "Yeni OOZOX Bildirimi", "Neuer OOZOX Hinweis", "OOZOX 新通知", "नया OOZOX अलर्ट",
        "Уведомление OOZOX", "OOZOXからのお知らせ", "Novo alerta OOZOX", "Nuova notifica OOZOX",
        "نیا OOZOX الرٹ", "هشدار جدید از OOZOX"
    ))
    add("fcm.system_body", (
        "لديك إشعار جديد في حسابك", "You have a new notification in your account",
        "Vous avez une nouvelle notification", "Tiene una nueva notificación",
        "Hesabınızda yeni bir bildirim var", "Sie haben eine neue Benachrichtigung",
        "您的账户有一条新通知", "आपके खाते में एक नई सूचना है",
        "У вас новое уведомление в аккаунте", "アカウントに新しい通知があります",
        "Você tem uma nova notificação", "Hai una nuova notifica",
        "آپ کے اکاؤنٹ میں نیا نوٹیفکیشن ہے", "یک اعلان جدید در حساب شما وجود دارد"
    ))

    # Navigation & Language button
    add("nav.collapse_sidebar", (
        "طي القائمة الجانبية", "Collapse Sidebar", "Réduire le menu", "Contraer barra",
        "Menüyü Daralt", "Seitenleiste einklappen", "收起侧边栏", "साइडबार बंद करें",
        "Свернуть меню", "サイドバーを閉じる", "Recolher menu", "Comprimi barra",
        "سائیڈ بار بند کریں", "بستن منوی کناری"
    ))
    add("nav.luxury_edition", (
        "الإصدار التنفيذي", "Executive Edition", "Édition Exécutive", "Edición Ejecutiva",
        "Kurumsal Sürüm", "Executive Edition", "旗舰企业版", "एग्जीक्यूटिव एडिशन",
        "Корпоративная версия", "エグゼクティブ版", "Edição Executiva", "Edizione Executive",
        "ایگزیکٹو ایڈیشن", "نسخه اجرایی"
    ))
    add("acct.choose_language", (
        "تغيير لغة النظام (14 لغة)", "Switch App Language (14)", "Langue de l'app (14)", "Idioma de la app (14)",
        "Uygulama Dili (14)", "App-Sprache (14)", "切换应用语言 (14)", "ऐप भाषा बदलें (14)",
        "Язык приложения (14)", "表示言語の切替 (14)", "Idioma do App (14)", "Lingua dell'app (14)",
        "ایپ کی زبان (14)", "تغییر زبان برنامه (۱۴)"
    ))

    # Retroactive Complaint Dialog
    add("retro.cat_deficit", (
        "عجز صندوق", "Cash Deficit", "Déficit de caisse", "Déficit de caja",
        "Kasa Açığı", "Kassenfehlbetrag", "现金短款", "नकद कमी",
        "Недостача кассы", "現金不足", "Déficit de caixa", "Ammanco di cassa",
        "کیش خسارہ", "کسری صندوق"
    ))
    add("retro.cat_pos_gap", (
        "فرق كاشير", "POS / Cashier Gap", "Écart de caisse", "Diferencia POS",
        "Kasiyer Farkı", "Kassendifferenz", "收银差额", "कैशियर अंतर",
        "Расхождение кассы", "レジ差額", "Diferença de caixa", "Differenza POS",
        "کیشیئر فرق", "اختلاف صندوق‌دار"
    ))
    add("retro.cat_return", (
        "مرتجع عميل", "Client Return", "Retour client", "Devolución cliente",
        "Müşteri İadesi", "Kundenretoure", "客户退货", "ग्राहक वापसी",
        "Возврат клиента", "顧客返品", "Devolução cliente", "Reso cliente",
        "کسٹمر واپسی", "مرجوعی مشتری"
    ))
    add("retro.cat_craft", (
        "مصنعيات", "Workmanship Fee", "Façon / Main d'œuvre", "Hechura",
        "İşçilik Ücreti", "Verarbeitung", "工费差额", "कारीगरी शुल्क",
        "Стоимость работы", "加工賃", "Mão de obra", "Lavorazione",
        "مزدوری فرق", "اجرت ساخت"
    ))
    add("retro.cat_ledger", (
        "قيد دفتري", "Ledger Correction", "Écriture comptable", "Ajuste contable",
        "Defter Kaydı", "Buchungskorrektur", "账簿更正", "बही समायोजन",
        "Бухгалтерская правка", "帳簿訂正", "Ajuste contábil", "Rettifica contabile",
        "کھاتہ اندراج", "اصلاح دفتری"
    ))
    add("retro.cat_other", (
        "تسوية أخرى", "Other Adjustment", "Autre ajustement", "Otro ajuste",
        "Diğer Düzeltme", "Sonstige Anpassung", "其他调整", "अन्य समायोजन",
        "Прочая корректировка", "その他の調整", "Outro ajuste", "Altra rettifica",
        "دیگر ایڈجسٹمنٹ", "تسویه دیگر"
    ))
    add("retro.today", (
        "اليوم", "Today", "Aujourd'hui", "Hoy", "Bugün", "Heute",
        "今天", "आज", "Сегодня", "今日", "Hoje", "Oggi", "آج", "امروز"
    ))
    add("retro.yesterday", (
        "أمس", "Yesterday", "Hier", "Ayer", "Dün", "Gestern",
        "昨天", "कल", "Вчера", "昨日", "Ontem", "Ieri", "کل", "دیروز"
    ))
    add("retro.two_days_ago", (
        "قبل يومين", "2 Days Ago", "Avant-hier", "Anteayer", "2 Gün Önce", "Vorgestern",
        "前天", "परसों", "Позавчера", "一昨日", "Anteontem", "2 giorni fa", "پرسوں", "پریروز"
    ))
    add("retro.err_amount", (
        "يرجى إدخال مبلغ صحيح أكبر من الصفر", "Please enter a valid amount greater than 0",
        "Veuillez saisir un montant valide", "Ingrese un monto válido mayor a 0",
        "Lütfen sıfırdan büyük geçerli bir tutar girin", "Bitte gültigen Betrag > 0 eingeben",
        "请输入大于零的有效金额", "कृपया 0 से अधिक वैध राशि दर्ज करें",
        "Введите корректную сумму больше нуля", "0より大きい金額を入力してください",
        "Insira um valor válido maior que zero", "Inserisci un importo valido",
        "صفر سے زیادہ درست رقم درج کریں", "لطفاً مبلغ معتبر بیشتر از صفر وارد کنید"
    ))
    add("retro.err_title", (
        "يرجى كتابة عنوان مختصر للشكوى أو التسوية", "Please enter a short dispute/claim title",
        "Veuillez saisir un titre court", "Ingrese un título breve",
        "Lütfen kısa bir talep başlığı girin", "Bitte kurzen Titel eingeben",
        "请输入简短的争议标题", "कृपया संक्षिप्त शीर्षक दर्ज करें",
        "Введите краткий заголовок", "タイトルを入力してください",
        "Insira um título curto", "Inserisci un titolo breve",
        "براہ کرم مختصر عنوان درج کریں", "لطفاً عنوان کوتاهی وارد کنید"
    ))
    add("retro.err_details", (
        "يرجى كتابة سبب وتفاصيل المطالبة المالية", "Please provide full claim details",
        "Veuillez fournir les détails", "Proporcione los detalles del reclamo",
        "Lütfen talep detaylarını yazın", "Bitte Begründung angeben",
        "请填写详细的财务说明", "कृपया पूर्ण विवरण दें",
        "Укажите причину и детали", "詳細な理由を入力してください",
        "Informe os detalhes da solicitação", "Inserisci i dettagli completi",
        "براہ کرم مکمل تفصیلات لکھیں", "لطفاً جزئیات و دلیل را بنویسید"
    ))

    # Subscription Modal & Billing Extras
    add("sub.modal_title", (
        "باقات واشتراكات OOZOX للأعمال", "OOZOX Business Subscriptions",
        "Abonnements OOZOX Business", "Suscripciones OOZOX Business",
        "OOZOX İşletme Paketleri", "OOZOX Business-Tarife",
        "OOZOX 企业订阅方案", "OOZOX बिजनेस सदस्यता",
        "Тарифы OOZOX Бизнес", "OOZOX ビジネスプラン",
        "Assinaturas OOZOX Business", "Abbonamenti OOZOX Business",
        "OOZOX بزنس سبسکرپشنز", "بسته‌ها و اشتراک‌های تجاری OOZOX"
    ))
    add("sub.modal_sub", (
        "عروض الترقية، إدارة الأعمال وحالة باقتك الحالية", "Upgrade plans, business tools & active status",
        "Offres de mise à niveau et statut", "Planes de mejora y estado actual",
        "Yükseltme fırsatları ve aktif paket durumu", "Upgrade-Angebote & Tarifstatus",
        "方案升级、商业工具与当前订阅状态", "अपग्रेड योजनाएं और सक्रिय स्थिति",
        "Улучшение тарифа и текущий статус", "プラン変更と現在の契約状況",
        "Planos de upgrade e status atual", "Piani di aggiornamento e stato",
        "اپ گریڈ پلانز اور موجودہ صورتحال", "پیشنهادهای ارتقا و وضعیت اشتراک"
    ))
    add("sub.billing_monthly", (
        "اشتراك شهري", "Monthly", "Mensuel", "Mensual", "Aylık", "Monatlich",
        "按月付费", "मासिक", "Ежемесячно", "月払い", "Mensal", "Mensile", "ماہانہ", "ماهانه"
    ))
    add("sub.billing_annual", (
        "سنوي (وفر 17%)", "Annual (Save 17%)", "Annuel (-17%)", "Anual (-17%)",
        "Yıllık (%17 Tasarruf)", "Jährlich (-17%)", "按年付费 (省17%)", "वार्षिक (17% बचत)",
        "Годовой (-17%)", "年払い (17%お得)", "Anual (Poupe 17%)", "Annuale (-17%)",
        "سالانہ (17% بچت)", "سالانه (۱۷٪ تخفیف)"
    ))
    add("sub.current_plan", (
        "باقتك الحالية", "Current Plan", "Plan actuel", "Plan actual",
        "Mevcut Paket", "Aktueller Tarif", "当前方案", "वर्तमान योजना",
        "Текущий тариф", "現在のプラン", "Plano Atual", "Piano Attuale",
        "موجودہ پلان", "بسته فعلی شما"
    ))
    add("sub.select_plan", (
        "ترقية لهذه الباقة", "Select This Plan", "Choisir ce plan", "Elegir este plan",
        "Bu Paketi Seç", "Tarif wählen", "选择此方案", "यह योजना चुनें",
        "Выбрать тариф", "このプランを選択", "Selecionar Plano", "Seleziona Piano",
        "یہ پلان منتخب کریں", "انتخاب این بسته"
    ))
    add("sub.open_full_plans", (
        "استعراض كافة الباقات والتفاصيل", "View All Plans & Comparison",
        "Voir tous les plans", "Ver todos los planes",
        "Tüm Paketleri Karşılaştır", "Alle Tarife vergleichen",
        "查看全部方案详情", "सभी योजनाएं देखें",
        "Сравнить все тарифы", "全プランを比較",
        "Ver todos os planos", "Vedi tutti i piani",
        "تمام پلانز دیکھیں", "مشاهده همه بسته‌ها"
    ))
    add("sub.req_sent_fmt", (
        "تم إرسال طلب تفعيل {plan} بنجاح ✅", "Activation request sent for {plan} ✅",
        "Demande envoyée pour {plan} ✅", "Solicitud enviada para {plan} ✅",
        "{plan} aktivasyon talebi gönderildi ✅", "Anfrage für {plan} gesendet ✅",
        "已提交 {plan} 激活申请 ✅", "{plan} के लिए अनुरोध भेजा गया ✅",
        "Запрос на {plan} отправлен ✅", "{plan} の申請を送信しました ✅",
        "Solicitação enviada para {plan} ✅", "Richiesta inviata per {plan} ✅",
        "{plan} کی درخواست بھیج دی گئی ✅", "درخواست فعال‌سازی {plan} ارسال شد ✅"
    ))
    add("sub.announcements_title", (
        "العروض والإشعارات الترويجية", "Promotions & Special Offers",
        "Promotions et offres", "Ofertas y promociones",
        "Kampanyalar ve Özel Teklifler", "Aktionen & Angebote",
        "限时优惠与公告", "विशेष ऑफ़र",
        "Акции и спецпредложения", "キャンペーン・お知らせ",
        "Promoções e Ofertas", "Offerte speciali",
        "خصوصی آفرز", "پیشنهادها و اطلاعیه‌ها"
    ))

    # Web Portal Extra Keys (UserAppPortal, LandingPage, AdminDashboard, ContactUs)
    add("web.cta_explore", (
        "استكشف المميزات", "Explore Features", "Découvrir", "Explorar funciones",
        "Özellikleri Keşfet", "Funktionen entdecken", "探索功能", "विशेषताएं देखें",
        "Возможности", "機能を見る", "Explorar Recursos", "Scopri le funzioni",
        "خصوصیات دیکھیں", "مشاهده امکانات"
    ))
    add("web.hero_badge", (
        "منصة OOZOX السحابية للأعمال والذهب", "OOZOX Cloud ERP & Gold Commerce",
        "ERP Cloud & Commerce de l'Or OOZOX", "ERP Nube y Comercio de Oro OOZOX",
        "OOZOX Bulut ERP ve Altın Ticareti", "OOZOX Cloud ERP & Goldhandel",
        "OOZOX 云端 ERP 与黄金商贸平台", "OOZOX क्लाउड ERP और गोल्ड कॉमर्स",
        "Облачная ERP OOZOX и торговля золотом", "OOZOX クラウドERP・金取引",
        "ERP Cloud & Comércio de Ouro OOZOX", "OOZOX Cloud ERP & Oro",
        "OOZOX کلاؤڈ ای آر پی", "پلتفرم ابری ERP و تجارت طلای OOZOX"
    ))
    add("web.hero_title", (
        "أدر شركاتك، حساباتك، ومشاريعك في مكان واحد مشفر ومزامن لحظياً",
        "Manage Your Companies, Ledgers & Projects in One Real-Time Encrypted Workspace",
        "Gérez vos entreprises, comptes et projets dans un espace chiffré en temps réel",
        "Gestione sus empresas, finanzas y proyectos en un espacio cifrado en tiempo real",
        "Şirketlerinizi, Hesaplarınızı ve Projelerinizi Gerçek Zamanlı Şifreli Tek Merkezden Yönetin",
        "Unternehmen, Finanzen und Projekte sicher in Echtzeit verwalten",
        "在一个实时加密云端平台中管理您的企业、账务与项目",
        "एक रियल-टाइम एन्क्रिप्टेड प्लेटफॉर्म में कंपनियों, खातों और प्रोजेक्ट्स का प्रबंधन करें",
        "Управляйте компаниями, финансами и проектами в единой зашифрованной системе",
        "企業会計・金取引・プロジェクトをリアルタイム暗号化空間で一元管理",
        "Gerencie empresas, finanças e projetos em um ambiente criptografado em tempo real",
        "Gestisci aziende, contabilità e progetti in un unico spazio crittografato",
        "اپنی کمپنیوں، اکاؤنٹس اور پراجیکٹس کو ایک محفوظ رئیل ٹائم نظام میں چلائیں",
        "شرکت‌ها، حسابداری و پروژه‌های خود را در یک محیط رمزنگاری‌شده مدیریت کنید"
    ))
    add("web.hero_desc", (
        "نظام متكامل يجمع الدردشة المحاسبية الفورية، فواتير الذهب، الموازين بالذكاء الاصطناعي، وإدارة الموظفين عبر الهاتف والويب.",
        "Unified suite combining #hashtag ledger chats, precious metals invoicing, AI scale inspection, and shift HR tracking.",
        "Suite unifiée combinant messagerie comptable #hashtag, facturation or, inspection IA et gestion d'équipe.",
        "Suite unificada con chats contables #hashtag, facturación de oro, IA y control de personal.",
        "#hashtag muhasebe sohbeti, altın faturalama, yapay zeka terazi tarama ve vardiya takibini birleştiren sistem.",
        "Komplettlösung für #Hashtag-Buchhaltung, Goldfakturierung, KI-Analyse und Schichtverwaltung.",
        "集成#标签记账聊天、黄金发票、AI智能称重校验与员工考勤排班的全能企业系统。",
        "#हैशटैग लेजर चैट, गोल्ड बिलिंग, AI निरीक्षण और स्टाफ शिफ्ट प्रबंधन का एकीकृत मंच।",
        "Единая система: #хэштег-бухгалтерия в чатах, учёт золота, ИИ-аудит и контроль смен.",
        "ハッシュタグ仕訳チャット・金インボイス・AI検品・シフト管理を統合したクラウド。",
        "Plataforma unificada com chat contábil #hashtag, faturas de ouro, IA e gestão de turnos.",
        "Piattaforma unificata con chat contabile #hashtag, fatture oro, AI e gestione turni.",
        "ہیش ٹیگ کھاتہ چیٹ، سونے کی انوائسز، اے آئی معائنہ اور سٹاف شفٹ مینجمنٹ کا مکمل نظام۔",
        "مجموعه یکپارچه چت حسابداری #هشتگ، فاکتور طلا، هوش مصنوعی و مدیریت شیفت کارمندان."
    ))
    add("web.features_title", (
        "منظومة أدوات متكاملة للأعمال والمجوهرات", "Complete Enterprise & Bullion Capabilities",
        "Fonctionnalités complètes pour entreprises", "Capacidades empresariales completas",
        "İşletmeler ve Kuyumculuk İçin Tam Donanımlı Araçlar", "Umfassende Unternehmensfunktionen",
        "全方位的企业财务与贵金属工具", "संपूर्ण एंटरप्राइज और गोल्ड टूल्स",
        "Полный набор инструментов для бизнеса", "エンタープライズ＆貴金属向け機能",
        "Recursos Completos para Empresas e Ouro", "Strumenti completi per aziende e gioielleria",
        "انٹرپرائز اور گولڈ بزنس کے مکمل فیچرز", "امکانات جامع کسب‌وکار و طلافروشی"
    ))
    add("web.portal_sub", (
        "نسخة الويب التفاعلية المزامنة مع تطبيق أندرويد", "Interactive cloud portal synced with OOZOX Android",
        "Portail web synchronisé avec OOZOX Android", "Portal web sincronizado con OOZOX Android",
        "OOZOX Android ile senkronize etkileşimli web portalı", "Interaktives Webportal mit Android-Sync",
        "与 OOZOX 安卓端实时同步的网页工作台", "OOZOX Android के साथ सिंक इंटरैक्टिव वेब पोर्टल",
        "Веб-портал с синхронизацией Android", "OOZOX Androidと連動するWebポータル",
        "Portal web sincronizado com OOZOX Android", "Portale web sincronizzato con OOZOX Android",
        "اینڈرائیڈ ایپ سے منسلک انٹرایکٹو ویب پورٹل", "پورتال وب همگام با اپلیکیشن اندروید OOZOX"
    ))
    add("web.portal_search", (
        "بحث في المعاملات أو الحسابات...", "Search ledgers or accounts...",
        "Rechercher comptes ou écritures...", "Buscar cuentas o movimientos...",
        "Hesap veya işlem ara...", "Konten oder Buchungen suchen...",
        "搜索账本或账户...", "खाते या लेनदेन खोजें...",
        "Поиск по счетам и операциям...", "帳簿やアカウントを検索...",
        "Pesquisar contas ou lançamentos...", "Cerca conti o movimenti...",
        "کھاتوں یا لین دین میں تلاش کریں...", "جستجو در حساب‌ها و تراکنش‌ها..."
    ))
    add("web.portal_refresh", (
        "تحديث البيانات", "Refresh Live Data", "Actualiser", "Actualizar datos",
        "Verileri Yenile", "Daten aktualisieren", "刷新实时数据", "डेटा रीफ़्रेश करें",
        "Обновить данные", "データを更新", "Atualizar dados", "Aggiorna dati",
        "ڈیٹا تازہ کریں", "به‌روزرسانی داده‌ها"
    ))
    add("web.footer_rights", (
        "جميع الحقوق محفوظة لمنصة OOZOX", "All rights reserved to OOZOX Platform",
        "Tous droits réservés — OOZOX", "Todos los derechos reservados — OOZOX",
        "Tüm hakları OOZOX Platformuna aittir", "Alle Rechte vorbehalten — OOZOX",
        "保留所有权利 OOZOX 平台", "सर्वाधिकार सुरक्षित — OOZOX",
        "Все права защищены — OOZOX", "All rights reserved — OOZOX",
        "Todos os direitos reservados — OOZOX", "Tutti i diritti riservati — OOZOX",
        "جملہ حقوق محفوظ ہیں — OOZOX", "تمامی حقوق متعلق به پلتفرم OOZOX است"
    ))
    add("web.admin_dash_title", (
        "لوحة التحكم المركزية والإدارة", "Central Admin Governance Console",
        "Console d'administration centrale", "Consola central de administración",
        "Merkezi Yönetim Konsolu", "Zentrale Verwaltungskonsole",
        "中央管理控制面板", "केंद्रीय व्यवस्थापक कंसोल",
        "Центральная панель администратора", "中央管理コンソール",
        "Console Central Administrativo", "Console di amministrazione centrale",
        "مرکزی ایڈمن کنسول", "کنسول مدیریت مرکزی"
    ))
    add("web.admin_signout", (
        "تسجيل الخروج", "Sign Out", "Déconnexion", "Cerrar sesión",
        "Çıkış Yap", "Abmelden", "退出登录", "लॉग आउट",
        "Выйти", "ログアウト", "Sair", "Esci",
        "لاگ آؤٹ", "خروج از سیستم"
    ))
    add("web.contact_sub", (
        "فريق الدعم الفني وتطوير الأعمال جاهز للإجابة على استفساراتكم على مدار الساعة.",
        "Our enterprise support & onboarding specialists are available 24/7.",
        "Notre équipe support entreprise est disponible 24h/24 et 7j/7.",
        "Nuestro equipo de soporte empresarial está disponible 24/7.",
        "Kurumsal destek ekibimiz 7/24 sorularınızı yanıtlamaya hazırdır.",
        "Unser Enterprise-Support steht Ihnen rund um die Uhr zur Verfügung.",
        "我们的企业技术支持团队全天候 24/7 为您解答疑问。",
        "हमारी एंटरप्राइज सहायता टीम 24/7 उपलब्ध है।",
        "Наша служба поддержки для бизнеса работает круглосуточно.",
        "エンタープライズサポートチームが24時間365日対応いたします。",
        "Nossa equipe de suporte empresarial está disponível 24/7.",
        "Il nostro supporto aziendale è disponibile 24/7.",
        "ہماری سپورٹ ٹیم چوبیس گھنٹے آپ کی مدد کے لیے دستیاب ہے۔",
        "تیم پشتیبانی سازمانی ما به‌صورت ۲۴ ساعته آماده پاسخگویی به شماست."
    ))
    add("web.contact_name_label", (
        "الاسم الكامل *", "Full Name *", "Nom complet *", "Nombre completo *",
        "Ad Soyad *", "Vollständiger Name *", "全名 *", "पूरा नाम *",
        "Полное имя *", "お名前 *", "Nome completo *", "Nome e cognome *",
        "مکمل نام *", "نام و نام خانوادگی *"
    ))
    add("web.contact_email_label", (
        "البريد الإلكتروني *", "Work Email *", "E-mail *", "Correo electrónico *",
        "E-posta *", "E-Mail *", "电子邮箱 *", "ईमेल *",
        "Эл. почта *", "メールアドレス *", "E-mail *", "E-mail *",
        "ای میل *", "ایمیل *"
    ))
    add("web.contact_phone_label", (
        "رقم الهاتف / واتساب", "Phone / WhatsApp", "Téléphone / WhatsApp", "Teléfono / WhatsApp",
        "Telefon / WhatsApp", "Telefon / WhatsApp", "电话 / WhatsApp", "फ़ोन / व्हाट्सएप",
        "Телефон / WhatsApp", "電話 / WhatsApp", "Telefone / WhatsApp", "Telefono / WhatsApp",
        "فون / واٹس ایپ", "تلفن / واتساپ"
    ))
    add("web.contact_msg_label", (
        "رسالتك أو استفسارك *", "Inquiry Details *", "Votre message *", "Detalles de su consulta *",
        "Mesajınız *", "Ihre Nachricht *", "咨询内容 *", "आपका संदेश *",
        "Ваше сообщение *", "お問い合わせ内容 *", "Sua mensagem *", "Il tuo messaggio *",
        "آپ کا پیغام *", "پیام یا پرسش شما *"
    ))

    return build_table(rows)
