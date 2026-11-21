# 🌐 دليل مزودي الخدمات الخارجية والاشتراكات الإنتاجية — OOZOX Platform

يوضح هذا الدليل بدقة **كافة الخدمات الخارجية** التي قد تحتاجها لتشغيل منظومة **OOZOX** في الإنتاج، مع التمييز الواضح بين:
1. **المتطلبات الإلزامية (لا يعمل السيرفر بدونها)**
2. **الخدمات المستبدلة مجاناً داخل Docker (لا تحتاج اشتراكاً خارجياً)**
3. **الخدمات الاختيارية الخاصة بميزات إضافية أو متاجر التطبيقات**

---

## 1️⃣ المتطلبات الإلزامية (2 فقط — البنية التحتية الأساسية)

| الخدمة | أمثلة مزودين موثوقين | التكلفة التقريبية | الغرض الفعلي وخطوات الإعداد |
| :--- | :--- | :--- | :--- |
| **1. خادم سحابي (Linux VPS)** | Hetzner, DigitalOcean, Vultr, Contabo, AWS EC2, Linode | ~$10 – $25 / شهرية *(Ubuntu 24.04/22.04 LTS, 4 vCPU, 8 GB RAM, 60 GB NVMe, Static IPv4)* | يستضيف كافة حاويات Docker السبع (MongoDB, Redis, API, Signaling, Web Portal, Nginx, Coturn). تأكد من فتح المنافذ: `22/tcp`, `80/tcp`, `443/tcp`, `3478/tcp+udp`, `5349/tcp`, `49152:65535/udp`. |
| **2. مسجّل نطاقات (Domain + DNS)** | Cloudflare DNS, Namecheap, Porkbun, GoDaddy | ~$10 – $15 / سنوياً *(نطاق `oozox.com`)* | توجيه 6 سجلات `A` إلى `SERVER_PUBLIC_IP`:<br>• `oozox.com`<br>• `www.oozox.com`<br>• `admin.oozox.com`<br>• `api.oozox.com`<br>• `signaling.oozox.com`<br>• `turn.oozox.com` **(مهم: اجعل Proxy مطفأً = DNS-Only لتمرير حزم UDP لـ WebRTC)** |

---

## 2️⃣ خدمات مستبدلة ذاتياً داخل Docker (التكلفة: $0 — لا تشترك فيها!)

بفضل البنية المدمجة في `docker-compose.prod.yml`، **لست بحاجة للاشتراك أو الدفع** في أي من المزودين السحابيين التاليين:

| نوع الخدمة | مزود خارجي تم الاستغناء عنه | البديل المدمج لديك في Docker |
| :--- | :--- | :--- |
| **قاعدة البيانات الوثائقية** | MongoDB Atlas (مدفوع للإنتاج) | حاوية **`oozox_mongo_prod`** (`mongo:7.0`) معزولة في شبكة `internal: true` ومشفرة الحقول بـ `AES-256-GCM` |
| **الذاكرة الوسيطة والطوابير** | Redis Cloud / Upstash | حاوية **`oozox_redis_prod`** (`redis:7-alpine`) مع `--requirepass` و`AOF` |
| **خادم STUN/TURN للمكالمات** | Twilio TURN / Metered.ca / Xirsys | حاوية **`oozox_coturn_prod`** (`coturn:4.6-alpine`) بمفاتيح HMAC مؤقتة |
| **استضافة بوابة الويب والإدارة** | Vercel / Netlify | حاوية **`oozox_web_prod`** (React 19 + Vite 7 + Nginx داخلي) |
| **استضافة الـ API وWebSockets** | Render / Railway / Heroku | حاويتا **`oozox_api_prod`** و**`oozox_signaling_prod`** |
| **شهادات الأمان SSL/TLS** | DigiCert / Sectigo المدفوعة | حاوية **`certbot/certbot`** تصدر وتجدد شهادات **Let's Encrypt** مجاناً |

---

## 3️⃣ الخدمات الاختيارية ومزودو ميزات التطبيق والمتاجر

> **ملاحظة:** الخادم الكامل، وبوابة الويب، وتسجيل الدخول، والفواتير، ومكالمات WebRTC تعمل **بدون أي من هذه الخدمات**. تشترك فقط فيما تريد تفعيله:

### أ) خدمات مجانية مبدئياً (Recommended عند الإطلاق العام للتطبيق)
1. **Firebase (Google) — لإشعارات الدفع الفورية على أندرويد (FCM):**
   - **الخطة:** Spark Plan (**مجانية $0** لإشعارات Cloud Messaging).
   - **متى تحتاجها:** إذا أردت وصول إشعارات التطبيق عندما يكون التطبيق مغلقاً في الخلفية.
   - **خطوة التفعيل:**
     1. أنشئ مشروعاً في [Firebase Console](https://console.firebase.google.com).
     2. أضف تطبيق Android بمعرف الحزمة الرسمي: `com.aistudio.goldchat.app.mirhxk`.
     3. حمّل ملف `google-services.json` وضعه في `/app/google-services.json` ثم أعد بناء APK/AAB.
   - **بدونها:** التطبيق والخادم يعملان دون أعطال، ولا تُفعل إشعارات FCM الخلفية فقط.

2. **Google AI Studio / Gemini API (`GEMINI_API_KEY`) — لمساعد المحاسبة الذكي داخل الأندرويد:**
   - **الخطة:** باقة مجانية محدودة / Pay-as-you-go حسب الاستهلاك.
   - **متى تحتاجها:** لتشغيل المساعد المالي الذكي وتحليلات Gemini داخل تطبيق الأندرويد (`GeminiAiService.kt`).
   - **خطوة التفعيل:** احصل على مفتاح من [Google AI Studio](https://aistudio.google.com) وأدخله في إعدادات الإدارة داخل التطبيق.
   - **بدونها:** بقية خصائص التطبيق والسيرفر تعمل طبيعياً تماماً.

### ب) حسابات متاجر التطبيقات (عند النشر للجمهور)
3. **Google Play Developer Account:**
   - **التكلفة:** **$25 لمرة واحدة** عبر [Google Play Console](https://play.google.com/console).
   - **متى تحتاجها:** فقط لنشر التطبيق رسمياً على متجر Google Play (وإنشاء `SERVICE_ACCOUNT_JSON_RAW` للنشر التلقائي عبر GitHub Actions).
   - **البديل المجاني:** يمكنك بناء وتوزيع ملف `app-release.apk` مباشرة للمستخدمين أو التجار دون حساب متجر.

4. **Apple Developer Program (مستقبلاً لنظام iOS فقط):**
   - **التكلفة:** **$99 / سنوياً** عبر [Apple Developer Portal](https://developer.apple.com) + يتطلب جهاز Mac / Xcode.
   - **متى تحتاجها:** فقط عند إصدار نسخة iOS رسمية على App Store.

### ج) خدمات خارجية اختيارية في شاشة الإعدادات أو الواجهة (لا تتطلب اشتراكاً لتشغيل المنظومة)
5. **مستودع GitHub (مجاني):** لاستضافة الكود وتشغيل خط البناء الآلي `.github/workflows/deploy.yml`.
6. **روابط مساعدة في التطبيق/الويب:**
   - روابط مشاركة الفواتير عبر واتساب (`wa.me`) في `UserAppPortal.tsx` مجانية ولا تحتاج WhatsApp Business API.
   - توليد صور QR في بوابة الويب يستخدم `api.qrserver.com` (مجاني بدون مفتاح).
   - حقول أسعار الذهب/الأخبار أو OneSignal في `AdminScreen.kt` هي إعدادات اختيارية يدخلها المدير إذا رغب في ربط مزود أسعار أو إشعارات خارجي إضافي.

---

## ✅ قائمة تحقق سريعة قبل تسليم السيرفر لـ OpenCode

- [ ] استئجار VPS بنظام Ubuntu 24.04/22.04 وامتلاك IP ثابت.
- [ ] ربط النطاق (`oozox.com` وفرعياته الستة) بالـ IP من لوحة تحكم الدومين (مع جعل `turn.oozox.com` DNS-Only).
- [ ] تشغيل `./scripts/deploy-prod.sh` (أو لصق `OPENCODE_PROMPT.txt` في OpenCode على السيرفر).
- [ ] *(اختياري لتطبيق الأندرويد)* تجهيز `app/google-services.json` ومفاتيح `release.keystore` وحساب Google Play Console ($25).
