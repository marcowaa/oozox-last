# -*- coding: utf-8 -*-
from common_langs import build_table

# 14-language tuple order:
# ar, en, fr, es, tr, de, zh, hi, ru, ja, pt, it, ur, fa

PART7 = build_table({
    "drawer.account_info": (
        "بيانات الحساب", "Account Info", "Infos du compte", "Info de la cuenta",
        "Hesap Bilgileri", "Kontoinformationen", "账户信息", "खाता जानकारी",
        "Данные аккаунта", "アカウント情報", "Dados da Conta", "Info Account",
        "اکاؤنٹ کی معلومات", "اطلاعات حساب"
    ),
    "drawer.logout": (
        "تسجيل الخروج", "Sign Out", "Déconnexion", "Cerrar sesión",
        "Çıkış Yap", "Abmelden", "退出登录", "लॉग आउट",
        "Выйти", "ログアウト", "Sair", "Esci",
        "لاگ آؤٹ", "خروج از حساب"
    ),
    "drawer.verified_badge": (
        "موثق", "Verified", "Vérifié", "Verificado",
        "Doğrulanmış", "Verifiziert", "已认证", "सत्यापित",
        "Проверено", "認証済み", "Verificado", "Verificato",
        "تصدیق شدہ", "تاییدشده"
    ),
    "sub.badge_paid_active": (
        "مشترك VIP 💎", "VIP Active 💎", "VIP Actif 💎", "VIP Activo 💎",
        "VIP Aktif 💎", "VIP Aktiv 💎", "VIP 会员 💎", "VIP सक्रिय 💎",
        "VIP Активен 💎", "VIP 有効 💎", "VIP Ativo 💎", "VIP Attivo 💎",
        "VIP فعال 💎", "اشتراک VIP 💎"
    ),
    "sub.badge_free_trial": (
        "تجربة مجانية", "Free Trial", "Essai gratuit", "Prueba gratis",
        "Ücretsiz Deneme", "Testphase", "免费试用", "निःशुल्क ट्रायल",
        "Пробный период", "無料体験", "Teste Grátis", "Prova Gratuita",
        "مفت ٹرائل", "دوره آزمایشی"
    ),
    "sub.badge_grace_1": (
        "تدرج 1 ⚠", "Stage 1 ⚠", "Étape 1 ⚠", "Etapa 1 ⚠",
        "Aşama 1 ⚠", "Stufe 1 ⚠", "阶段 1 ⚠", "चरण 1 ⚠",
        "Этап 1 ⚠", "段階 1 ⚠", "Estágio 1 ⚠", "Fase 1 ⚠",
        "مرحلہ 1 ⚠", "مرحله ۱ ⚠"
    ),
    "sub.badge_grace_2": (
        "تدرج 2 🚨", "Stage 2 🚨", "Étape 2 🚨", "Etapa 2 🚨",
        "Aşama 2 🚨", "Stufe 2 🚨", "阶段 2 🚨", "चरण 2 🚨",
        "Этап 2 🚨", "段階 2 🚨", "Estágio 2 🚨", "Fase 2 🚨",
        "مرحلہ 2 🚨", "مرحله ۲ 🚨"
    ),
    "sub.badge_restricted": (
        "حجب 🔒", "Locked 🔒", "Verrouillé 🔒", "Bloqueado 🔒",
        "Kilitli 🔒", "Gesperrt 🔒", "已锁定 🔒", "लॉक 🔒",
        "Ограничен 🔒", "制限中 🔒", "Bloqueado 🔒", "Bloccato 🔒",
        "مقفل 🔒", "قفل‌شده 🔒"
    ),
    "sub.badge_expired": (
        "منتهي ❌", "Expired ❌", "Expiré ❌", "Expirado ❌",
        "Süresi Doldu ❌", "Abgelaufen ❌", "已过期 ❌", "समाप्त ❌",
        "Истекла ❌", "期限切れ ❌", "Expirado ❌", "Scaduto ❌",
        "ختم ❌", "منقضی ❌"
    ),
    "gold_utils.currency_unit": (
        "ج.م", "EGP", "EGP", "EGP",
        "EGP", "EGP", "EGP", "EGP",
        "EGP", "EGP", "EGP", "EGP",
        "EGP", "EGP"
    ),
    "gold_utils.weight_unit": (
        "جم", "g", "g", "g",
        "g", "g", "克", "ग्राम",
        "г", "g", "g", "g",
        "گرام", "گرم"
    ),
    "gold_utils.today_fmt": (
        "اليوم • {time}", "Today • {time}", "Aujourd'hui • {time}", "Hoy • {time}",
        "Bugün • {time}", "Heute • {time}", "今天 • {time}", "आज • {time}",
        "Сегодня • {time}", "今日 • {time}", "Hoje • {time}", "Oggi • {time}",
        "آج • {time}", "امروز • {time}"
    ),
    "gold_utils.yesterday_fmt": (
        "أمس • {time}", "Yesterday • {time}", "Hier • {time}", "Ayer • {time}",
        "Dün • {time}", "Gestern • {time}", "昨天 • {time}", "कल • {time}",
        "Вчера • {time}", "昨日 • {time}", "Ontem • {time}", "Ieri • {time}",
        "کل • {time}", "دیروز • {time}"
    ),
    "gold_utils.default_initial": (
        "ع", "O", "O", "O",
        "O", "O", "O", "O",
        "О", "O", "O", "O",
        "ع", "ک"
    ),
    "gold_utils.status_purchased": (
        "مشترى", "Purchased", "Acheté", "Comprado",
        "Satın Alındı", "Gekauft", "已购入", "खरीदा गया",
        "Куплено", "購入済み", "Comprado", "Acquistato",
        "خریدا گیا", "خریداری‌شده"
    ),
    "gold_utils.status_returned": (
        "مُرجَع", "Returned", "Retourné", "Devuelto",
        "İade Edildi", "Zurückgegeben", "已退回", "लौटाया गया",
        "Возвращено", "返品済み", "Devolvido", "Reso",
        "واپس کیا گیا", "مرجوع‌شده"
    ),
    "gold_utils.status_resold": (
        "مُعاد بيعه", "Resold", "Revendu", "Revendido",
        "Yeniden Satıldı", "Weiterverkauft", "已转售", "पुनः बेचा गया",
        "Перепродано", "転売済み", "Revendido", "Rivenduto",
        "دوبارہ فروخت", "بازفروش‌شده"
    ),
    "gold_utils.type_jewelry": (
        "مصاغ", "Jewelry", "Bijoux", "Joyería",
        "Takı", "Schmuck", "首饰", "आभूषण",
        "Украшения", "宝飾品", "Joias", "Gioielli",
        "زیورات", "جواهرات"
    ),
    "gold_utils.type_bullion": (
        "سبائك", "Bullion Bars", "Lingots", "Lingotes",
        "Külçe", "Barren", "金条", "सोने की ईंट",
        "Слитки", "地金・インゴット", "Barras", "Lingotti",
        "سونے کے بسکٹ", "شمش"
    ),
    "gold_utils.type_coins": (
        "عملات", "Coins", "Pièces", "Monedas",
        "Sikke", "Münzen", "金币", "सिक्के",
        "Монеты", "金貨", "Moedas", "Monete",
        "سکے", "سکه"
    ),
    "gold_utils.type_pound": (
        "جنيه ذهب", "Gold Pound", "Livre d'or", "Libra de oro",
        "Altın Lira", "Goldpfund", "黄金镑币", "गोल्ड पाउंड",
        "Золотой фунт", "金貨ポンド", "Libra de Ouro", "Sterlina d'oro",
        "گولڈ پاؤنڈ", "پوند طلا"
    ),
    "gold_utils.type_bracelets": (
        "أساور", "Bracelets", "Bracelets", "Pulseras",
        "Bilezik", "Armbänder", "手镯", "कंगन",
        "Браслеты", "ブレスレット", "Pulseiras", "Bracciali",
        "کنگن", "دستبند"
    ),
    "gold_utils.type_rings": (
        "خواتم", "Rings", "Bagues", "Anillos",
        "Yüzük", "Ringe", "戒指", "अंगूठी",
        "Кольца", "指輪", "Anéis", "Anelli",
        "انگوٹھیاں", "انگشتر"
    ),
    "gold_utils.type_necklaces": (
        "قلائد", "Necklaces", "Colliers", "Collares",
        "Kolye", "Halsketten", "项链", "हार",
        "Ожерелья", "ネックレス", "Colares", "Collane",
        "ہار", "گردنبند"
    ),
    "gold_utils.type_earrings": (
        "أقراط", "Earrings", "Boucles d'oreilles", "Aretes",
        "Küpe", "Ohrringe", "耳环", "झुमके",
        "Серьги", "イヤリング", "Brincos", "Orecchini",
        "بالیاں", "گوشواره"
    ),
    "gold_utils.type_other": (
        "أخرى", "Other", "Autre", "Otros",
        "Diğer", "Sonstiges", "其他", "अन्य",
        "Другое", "その他", "Outros", "Altro",
        "دیگر", "سایر"
    ),
    "gold_utils.mfg_lazurde": (
        "لازوردي", "L'azurde", "L'azurde", "L'azurde",
        "L'azurde", "L'azurde", "L'azurde 品牌", "L'azurde",
        "L'azurde", "L'azurde", "L'azurde", "L'azurde",
        "لازوردی", "لازوردی"
    ),
    "gold_utils.mfg_mouawad": (
        "معوض", "Mouawad", "Mouawad", "Mouawad",
        "Mouawad", "Mouawad", "Mouawad 品牌", "Mouawad",
        "Mouawad", "Mouawad", "Mouawad", "Mouawad",
        "معوض", "معوض"
    ),
    "gold_utils.mfg_damas": (
        "داماس", "Damas", "Damas", "Damas",
        "Damas", "Damas", "Damas 品牌", "Damas",
        "Damas", "Damas", "Damas", "Damas",
        "داماس", "داماس"
    ),
    "gold_utils.mfg_alsubaie": (
        "السبيعي", "Alsubaie", "Alsubaie", "Alsubaie",
        "Alsubaie", "Alsubaie", "Alsubaie 品牌", "Alsubaie",
        "Alsubaie", "Alsubaie", "Alsubaie", "Alsubaie",
        "السبیعی", "السبیعی"
    ),
    "gold_utils.mfg_alfardan": (
        "الفردان", "Alfardan", "Alfardan", "Alfardan",
        "Alfardan", "Alfardan", "Alfardan 品牌", "Alfardan",
        "Alfardan", "Alfardan", "Alfardan", "Alfardan",
        "الفردان", "الفردان"
    ),
    "gold_utils.mfg_alromaizan": (
        "الرميزان", "Al Romaizan", "Al Romaizan", "Al Romaizan",
        "Al Romaizan", "Al Romaizan", "Al Romaizan 品牌", "Al Romaizan",
        "Al Romaizan", "Al Romaizan", "Al Romaizan", "Al Romaizan",
        "الرمیزان", "الرميزان"
    ),
    "gold_utils.mfg_taif": (
        "مجوهرات طيف", "Taif Jewelry", "Taif Jewelry", "Taif Jewelry",
        "Taif Mücevherat", "Taif Schmuck", "Taif 珠宝", "Taif ज्वैलरी",
        "Taif Jewelry", "Taif ジュエリー", "Taif Joias", "Taif Gioielli",
        "طیف جیولری", "جواهرات طیف"
    ),
    "gold_utils.mfg_cartier": (
        "كارتييه", "Cartier", "Cartier", "Cartier",
        "Cartier", "Cartier", "卡地亚 (Cartier)", "Cartier",
        "Cartier", "カルティエ", "Cartier", "Cartier",
        "کارٹیئر", "کارتیه"
    ),
    "gold_utils.mfg_bvlgari": (
        "بولغري", "Bvlgari", "Bvlgari", "Bvlgari",
        "Bvlgari", "Bvlgari", "宝格丽 (Bvlgari)", "Bvlgari",
        "Bvlgari", "ブルガリ", "Bvlgari", "Bvlgari",
        "بلگاری", "بولگاری"
    ),
})
