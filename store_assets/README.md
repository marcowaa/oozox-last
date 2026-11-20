# 📦 حقيبة مواد النشر والتسويق لمنصة OOZOX
> **المنصة:** OOZOX (oozox.com) - CONNECT • THINK • DO  
> **تاريخ الإعداد:** 2026-08-15  

---

## 🗂️ هيكل المجلدات المنظم (Directory Structure)

```
store_assets/
├── README.md                                  <-- هذا الملف التعريفي الشامل
│
├── google_play/                               <-- مجلد متجر Google Play
│   ├── GOOGLE_PLAY_METADATA_AR.md             <-- النصوص الرسمية باللغة العربية (30 حرف، 80 حرف، 4000 حرف)
│   ├── GOOGLE_PLAY_METADATA_EN.md             <-- النصوص الرسمية باللغة الإنجليزية (ASO Keywords & Description)
│   ├── SCREENSHOTS_SPECIFICATIONS_AND_COPY.md <-- تفاصيل ولقطات الشاشة الـ 8 للهواتف والتابلت 7" و 10"
│   ├── PROMO_VIDEO_SCRIPT_AND_STORYBOARD.md   <-- سيناريو الفيديو الترويجي الرسمي (30 ثانية و 60 ثانية)
│   └── PLAY_CONSOLE_RELEASE_CHECKLIST.md      <-- قائمة فحص وإجراءات النشر على Play Console
│
└── apple_app_store/                           <-- مجلد متجر Apple App Store
    ├── APP_STORE_METADATA_AR.md               <-- نصوص App Store Connect بالعربية (Name, Subtitle, Promo)
    ├── APP_STORE_METADATA_EN.md               <-- نصوص App Store Connect بالإنجليزية
    ├── SCREENSHOTS_SPECIFICATIONS_AND_COPY.md <-- مواصفات شاشات iPhone (6.7", 6.5", 5.5") و iPad Pro (12.9")
    ├── APP_PREVIEW_VIDEO_SCRIPT.md            <-- سيناريو فيديو المعاينة وفق معايير Apple HIG الصارمة
    └── APP_STORE_CONNECT_SUBMISSION_GUIDE.md  <-- إرشادات المراجعة وحساب الفحص المخصص للمراجع ZOX
```

---

## ⚙️ ملف تشغيل السيرفر والإنتاج:
* تم إنشاء الملف الشامل في جذر المشروع: **`PRODUCTION_SERVER_DEPLOYMENT_GUIDE.md`**
* يغطي بالكامل: بنية السيرفر، مواصفات العتاد، ملف `docker-compose.prod.yml`، إعدادات Nginx و SSL Certbot، خادم Coturn STUN/TURN للمكالمات المشفرة، النسخ الاحتياطي الآلي، وبناء حزمة الأندرويد الإنتاجية `.aab`.
