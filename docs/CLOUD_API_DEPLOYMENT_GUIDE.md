# 🚀 دليل النشر الإنتاجي والتكوين (Production API, Signaling & Android Build)

> **تحديث الحالة:** اكتمل التحصين الأمني لخوادم Node/Go، ونظام النسخ الاحتياطي المشفر والاستعادة والمراقبة، وتكوين الأندرويد ويبقى استبدال المفاتيح الحقيقية والنطاقات على الخادم الفعلي.

---

## 1. تنبيه حرج: تغيير `applicationId` قبل أول رفع على Google Play
يبقي المشروع حالياً على المعرّف الافتراضي **`com.aistudio.goldchat.app.mirhxk`** (بينما يظل `namespace = "com.example"` ثابتاً كما هو في الكود).
> **تحذير Play Console:** لا يمكن تغيير `applicationId` أبداً بعد رفع أول ملف AAB على Google Play Console! إذا كنت ستستخدم معرّفاً نهائياً مختلفاً (مثل `com.oozox.app`)، قم بتغييره **قبل** استخراج `google-services.json` وأول رفع في الملفات التالية فقط:
1. `app/build.gradle.kts` (`defaultConfig.applicationId`)
2. `app/google-services.json` (`client_info.android_client_info.package_name` — استخرجه من Firebase لنفس المعرّف)
3. `docs/assetlinks.json.template` (`target.package_name` عند رفعه إلى `https://oozox.com/.well-known/assetlinks.json`)
4. `.github/workflows/deploy.yml` (`packageName` في خطوة `r0adkll/upload-google-play@v1`)
5. `docs/PLAY_DECLARATIONS.md` (السطر التعريفي للمعرف)

---

## 2. المتطلبات المسبقة قبل التشغيل على الخادم (`.env.production`)
1. انسخ `.env.production.example` إلى `.env.production` أو ولّده تلقائياً عبر `./scripts/deploy-prod.sh init-env`:
   - **قاعدة البيانات والذاكرة:** `MONGO_ROOT_PASSWORD`, `MONGO_APP_PASSWORD`, `REDIS_PASSWORD`
   - **مفاتيح التوقيع والتشفير:**
     - `JWT_SECRET`, `DB_ENCRYPTION_KEY` (64 محرف سداسي عشري)
     - `INTERNAL_SIGNALING_HMAC_SECRET` (إلزامي بطول >= 32 محرفاً — يرفض خادم Go الإقلاع بدونه ولا يوجد أي secret افتراضي)
   - **مزود التحقق (OTP) المعتمد — Telegram Gateway:**
     - `OTP_CHANNEL_ORDER=TELEGRAM,WHATSAPP,SMS`
     - `TELEGRAM_GATEWAY_TOKEN` + `TELEGRAM_GATEWAY_CALLBACK_URL=https://api.oozox.com/api/auth/webhooks/telegram-gateway`
     - *(اختياري كبديل أو fallback)* `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_VERIFY_SERVICE_SID`
   - **الذكاء الاصطناعي (خادمي فقط - Gemini Server Proxy):**
     - `GEMINI_API_KEY` (في بيئة الخادم فقط — ممنوع تماماً في الأندرويد)
     - `GEMINI_USER_DAILY_LIMIT`, `GEMINI_USER_MONTHLY_LIMIT`, `GEMINI_MONTHLY_BUDGET_USD`
   - **بنية المكالمات (WebRTC + Coturn):**
     - `COTURN_SECRET`, `TURN_URLS`, `SERVER_PUBLIC_IP`
   - **المراقبة والتنبيهات والنسخ الاحتياطي خارج الخادم:**
     - `ALERT_TELEGRAM_BOT_TOKEN`, `ALERT_TELEGRAM_CHAT_ID` (لتنبيهات المراقبة والأعطال والنسخ الاحتياطي)
     - `BACKUP_RCLONE_REMOTE` أو `BACKUP_S3_URI` (لنسخ الباك أب خارج السيرفر)
     - `LOG_LEVEL=info` واختيارياً `SENTRY_DSN`
   - **إشعارات الدفع (FCM):**
     - `FIREBASE_SERVICE_ACCOUNT_JSON` (أو `FIREBASE_SERVICE_ACCOUNT_PATH`) على الخادم
     - ملف **`app/google-services.json`** الحقيقي في مجلد `app/` عند بناء إصدار الأندرويد (`bundleRelease` / `assembleRelease`)

---

## 3. أوامر التشغيل والتحقق عبر `scripts/deploy-prod.sh`

```bash
chmod +x scripts/deploy-prod.sh deploy/mongo/init-app-user.sh
./scripts/deploy-prod.sh check-prereqs
./scripts/deploy-prod.sh init-env
# عدّل .env.production وأدخل TELEGRAM_GATEWAY_TOKEN و GEMINI_API_KEY و FIREBASE_SERVICE_ACCOUNT_JSON و ALERT_TELEGRAM_* و BACKUP_*
./scripts/deploy-prod.sh validate-env
./scripts/deploy-prod.sh issue-ssl admin@oozox.com
./scripts/deploy-prod.sh test
./scripts/deploy-prod.sh up
```

### إنشاء أول حساب مسؤول (`ADMIN`) وجدولة المراقبة والنسخ الاحتياطي
```bash
./scripts/deploy-prod.sh create-admin "+201000000000" "OOZOX Admin"
./scripts/deploy-prod.sh smoke
./scripts/deploy-prod.sh install-backup-script
```
*(راجع `docs/BACKUP_RESTORE_DRILL.md` لتجربة الاستعادة على قاعدة تجريبية معزولة `oozox_restore_drill`.)*

---

## 4. نقاط الربط وبناء إصدار الأندرويد (`app/`)
- **Android `applicationId`:** `com.aistudio.goldchat.app.mirhxk` (`compileSdk = 36`, `targetSdk = 36`)
- **API Base URL:** `https://api.oozox.com/api/`
- **Signaling WebSocket:** `wss://signaling.oozox.com/ws`
- **Asset Links (App Links):** استبدل البصمة SHA-256 في `docs/assetlinks.json.template` وانشر الملف على `https://oozox.com/.well-known/assetlinks.json`
- **شروط بناء الإصدار (`bundleRelease` / `assembleRelease`):** يرفض Gradle بناء إصدار Release إذا كان `app/google-services.json` أو `release.keystore` (`ANDROID_KEYSTORE_FILE`) مفقوداً، بينما يعمل Debug والاختبارات بدون Firebase.
