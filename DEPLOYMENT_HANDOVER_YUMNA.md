# 🚀 دليل التجهيز للإنتاج والنشر التلقائي - منصة وتطبيق OOZOX
**موجه إلى:** المهندسة يمنى (Eng. Yumna) وفريق التطوير والتشغيل (DevOps Team)

---

## 1. معلومات المشروع الأساسية (Project Specifications)

- **اسم التطبيق:** OOZOX
- **نوع التطبيق:** Android Native (Kotlin, Jetpack Compose, Material 3, Room AES-256-GCM + AndroidKeyStore, Retrofit)
- **المعرف الفريد (Application ID / Package Name):** `com.aistudio.goldchat.app.mirhxk`
- **إصدار النظام Target SDK:** `35` (Android 15) | **Min SDK:** `24` (Android 7.0 Nougat)
- **المستودع الرئيسي:** GitHub Repository
- **المنصات المستهدفة:**
  1. **Android:** Google Play Console (مستهدف رئيسي - بيئة Native جاهزة 100%)
  2. **iOS:** Apple App Store (خطة التوسع عبر Kotlin Multiplatform / React NativeWrapper)
- **الحسابات والتراخيص المطلوبة:**
  - **Google Play Developer Account:** $25 (دفعة واحدة)
  - **Apple Developer Program:** $99 / سنوياً

---

## 2. ما تم إنجازه في المستودع (Production Readiness Actions)

لقد قمنا بتجهيز المستودع وتأهيله بالكامل للإنتاج بفرعين تقنيين:

1. **إعداد بناء الإنتاج (Production Release Build Config):**
   - تم تفعيل ميزتي الضغط والتعمية الحزمتية R8 / ProGuard في `/app/build.gradle.kts`:
     - `isMinifyEnabled = true` (تقليل حجم الكود وتعميته)
     - `isShrinkResources = true` (حذف الموارد والرموز غير المستخدمة)
   - إعداد ربط مفاتيح التوقيع الرقمي (Release Keystore) عبر متغيرات البيئة تلقائياً دون إظهار كلمة المرور داخل الكود.

2. **قواعد الحماية والتعمية (ProGuard & R8 Optimization):**
   - تم تحديث وتجهيز ملف `/app/proguard-rules.pro` ليشمل استثناءات Jetpack Compose, Room, Retrofit, Serialization, Firebase Messaging لمنع أي انهيار في نسخة الإنتاج.

3. **أتمتة النشر التلقائي (CI/CD Pipeline):**
   - تم إنشاء ملف العمالة التلقائية `.github/workflows/deploy.yml` في GitHub Actions لتشغيل الاختبارات وتوقيع وبناء ملفات `.aab` و `.apk` ونشرها تلقائياً عند الدفع على فرع `main`.

4. **سكربتات Fastlane للنشر السريع:**
   - تم إنشاء ملفات `fastlane/Appfile` و `fastlane/Fastfile` لأتمتة الرفع المباشر إلى Google Play Internal / Beta Track.

---

## 3. خطة إعداد المفاتيح والتوقيع الرقمي (Code Signing Setup)

### أ. إنشاء ملف التوقيع الرقمي لـ Android (Release Keystore)
يتم تنفيذ الأمر التالي في مبدئياً على جهاز المطور لإنشاء ملف `release.keystore`:

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias releasekey \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=OOZOX, OU=DevOps, O=OOZOX, L=Cairo, C=EG" \
  -storepass YOUR_SAFE_KEYSTORE_PASSWORD \
  -keypass YOUR_SAFE_KEY_PASSWORD
```

### ب. تشفير المفتاح وحفظه في GitHub Secrets
لرفع المفتاح بأمان إلى GitHub Actions:

1. تحويل الملف إلى نص مشفر بـ Base64:
   ```bash
   base64 -w 0 release.keystore > keystore_base64.txt
   ```
2. الانتقال إلى مستودع GitHub -> **Settings** -> **Secrets and variables** -> **Actions**.
3. إضافة المتغيرات السرية التالية (GitHub Secrets):
   - `ANDROID_KEYSTORE_BASE64`: محتوى ملف `keystore_base64.txt`
   - `KEYSTORE_PASSWORD`: كلمة مرور Keystore
   - `KEY_ALIAS`: `releasekey`
   - `KEY_PASSWORD`: كلمة مرور Alias
   - `SERVICE_ACCOUNT_JSON_RAW`: نص ملف JSON الخاص بحساب خدمة Google Play Service Account للنشر التلقائي.

---

## 4. أوامر البناء للإنتاج (Production Build Commands)

يمكن إخراج الملفات للإنتاج يدوياً من خلال الأوامر التالية:

### 1. بناء حزمة تطبيق أندرويد المتجر (AAB - Recommended for Google Play):
```bash
gradle :app:testDebugUnitTest
gradle :app:bundleRelease
```
*المسار الناتج:* `app/build/outputs/bundle/release/app-release.aab`

### 2. بناء ملف التثبيت المباشر (APK - For Direct Distribution / Testing):
```bash
gradle :app:assembleRelease
```
*المسار الناتج:* `app/build/outputs/apk/release/app-release.apk`

---

## 5. خطوات رفع التطبيق على المتجر (للمفهومية والمهندسة يمنى)

### أولاً: Google Play Console (Android)
1. الدخول إلى [Google Play Console](https://play.google.com/console).
2. إنشاء تطبيق جديد باسم: **OOZOX** وتحديد اللغة الافتراضية (العربية).
3. **إعدادات المتجر (Main Store Listing):**
   - رفع أيقونة التطبيق (512x512 PNG).
   - رفع صورة المعاينة الفنية Feature Graphic (1024x500 PNG).
   - رفع لقطات الشاشة (Screenshots) للهواتف والأجهزة اللوحية.
   - كتابة الوصف القصير والوصف الكامل.
4. **سياسة الخصوصية (Privacy Policy):**
   - إضافة رابط سياسة الخصوصية الخاص بالموقع (`https://oozox.com/privacy-policy`).
5. **إصدار التطبيق (Releases):**
   - الانتقال إلى **Testing** -> **Internal testing** (أو **Production**).
   - رفع ملف `app-release.aab` الناتج من بناء المشروع.
   - مراجعة الإصدار وإرساله للمراجعة (Review).

### ثانياً: App Store Connect (iOS - في حال توسيع المشروع لـ Cross-Platform)
1. إنشاء App ID في [Apple Developer Portal](https://developer.apple.com).
2. ربط شهادة التوزيع Apple Distribution Certificate وملف Provisioning Profile.
3. فتح المشروع وتوليد ملف `.ipa` الموقّع باستخدام `xcodebuild` أو `eas build` أو `fastlane`.
4. استخدام **Transporter** لرفع ملف `.ipa` إلى App Store Connect.
5. اختيار النسخة في **TestFlight** للاختبار الداخلي، ثم إرسالها لمراجعة App Store.

---

## 6. قائمة التحقق النهائية قبل الإطلاق (Pre-Launch Checklist)

- [x] **Application ID / Package Name:** تم ضبط المعرف المعتمد `com.aistudio.goldchat.app.mirhxk`.
- [x] **ProGuard / R8 Shrinking:** مفعّل وتعمل كافة القواعد بنجاح بدون مشاكل.
- [x] **Debug Mode & Backup Hardening:** وضع التصحيح والنسخ الاحتياطي السحابي غير المشفر معطلان (`android:allowBackup="false"`).
- [x] **Encryption & Keystore Security:** تشفير Room المحلي بـ `AES-256-GCM` عبر `AndroidKeyStore`، وحسابات الإدارة الفرعية بـ `PBKDF2WithHmacSHA256`؛ وكود التوقيع يقرأ مفاتيح الإصدار من متغيرات البيئة فقط.
- [x] **Network Permissions:** تم التأكد من صلاحيات الإنترنت والإشعارات `INTERNET`, `POST_NOTIFICATIONS` في `AndroidManifest.xml`.
- [x] **CI/CD & Server Docker Stack:** خط البناء والاختبار الآلي في `.github/workflows/deploy.yml`، وتشغيل السيرفر 100% عبر `docker-compose.prod.yml` و`./scripts/deploy-prod.sh`.
- [ ] **Firebase `google-services.json`:** إضافة ملف Firebase الحقيقي في `app/google-services.json` قبل البناء النهائي لتفعيل FCM.
- [ ] **Physical Device Testing:** يُوصى باختبار النسخة النهائية `.aab` / `.apk` على جهاز أندرويد فعلي قبل النشر للجمهور.
- [ ] **Google Play Console Service Account:** إنشاء مفتاح JSON وإضافته لـ GitHub Secrets لأتمتة الرفع بنسبة 100%.

---

## 7. تسليم السيرفر والتشغيل الإنتاجي (100% Dockerized Stack)

لتشغيل الخادم الكامل (`MongoDB 7` + `Redis 7` + `Express API` + `Go WebRTC Signaling` + `Web Portal` + `Nginx TLS` + `Coturn` + `Certbot`) داخل Docker على VPS:
- راجع **`OPENCODE_PRODUCTION_RUNBOOK.txt`** (مرجع أوبنكود الرسمي).
- نفذ النشر مباشرة باستخدام السكربت الموحد **`./scripts/deploy-prod.sh`** (`init-env` -> `issue-ssl` -> `test` -> `up` -> `create-admin` -> `smoke`).

---

## 8. صلاحيات ومتطلبات تحتاجها المهندسة يمنى (Eng. Yumna Access Checklist)

لتتمكن المهندسة يمنى من إتمام مرحلة النشر بسلاسة، يلزم تزويدها بـ:
1. **صلاحية مدير أو مسؤول نشر على Google Play Console** (Owner / Admin Access).
2. **صلاحية Admin / Developer على Apple Developer Account** (في حال إطلاق نسخة iOS).
3. **صلاحيات الأدمين على مستودع GitHub** لإضافة الـ Secrets المشفرة (`ANDROID_KEYSTORE_BASE64` وغيرها).
4. **رابط سياسة الخصوصية الرسمي (Privacy Policy URL)** المعتمد للشركة (`https://oozox.com/privacy-policy`).

---
**تم إعداد وتهيئة المشروع بالكامل ليصبح جاهزاً للإنتاج (Production-Ready).**
