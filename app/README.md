# 🏛️ OOZOX Enterprise Financial Platform & Native Android Suite

نظام إدارة مالي ومحاسبي متكامل، يجمع بين واجهة Android فائقة السلاسة المبنية بأحدث تقنيات **Jetpack Compose** و **Material Design 3**، مع بنية تحتية سحابية هجينة تدعم التحليلات الذكية ومحرك معالجة Excel الفوري.

---

## 📑 جدول المحتويات
1. [نظرة عامة على المعمارية](#-نظرة-عامة-على-المعمارية)
2. [المتطلبات الأساسية للتشغيل](#-المتطلبات-الأساسية-للتشغيل)
3. [دليل التشغيل المحلي (Android Studio & Native)](#-دليل-التشغيل-المحلي)
4. [دليل النشر والاستضافة السحابية (Backend & Web Dashboard)](#-دليل-النشر-والاستضافة-السحابية)
5. [متغيرات البيئة والأمان (Environment Variables)](#-متغيرات-البيئة-والأمان)
6. [مواصفات الـ APIs والمصادقة (Authentication & Endpoints)](#-مواصفات-الـ-apis-والمصادقة)
7. [استراتيجيات تحسين الأداء والاختبارات (Performance & QA)](#-استراتيجيات-تحسين-الأداء-والاختبارات)

---

## 🏛️ نظرة عامة على المعمارية

```text
┌─────────────────────────────────────────────────────────────┐
│               Client Tier (Android Native)                  │
│  - Jetpack Compose + Clean Architecture (MVVM)              │
│  - Room Local DB + AndroidKeyStore AES-256-GCM Encryption   │
│  - PBKDF2WithHmacSHA256 Sub-Account Security + Biometrics   │
│  - Global i18n Localization Engine (RTL / LTR)              │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTPS / TLS 1.3 / WSS / JWT Auth
┌──────────────────────────────▼──────────────────────────────┐
│        Dockerized Edge & Backend Tier (docker-compose.prod) │
│  - Nginx SSL Proxy + Containerized React/Vite Web Portal    │
│  - Express API (Node 20) + Helmet + Zod + Rate Limiting     │
│  - Go 1.22 WebRTC Signaling + Coturn STUN/TURN Media Relay  │
└──────────────────────────────┬──────────────────────────────┘
                               │ Docker Internal Network (`internal: true`)
┌──────────────────────────────▼──────────────────────────────┐
│           Isolated Data Tier (100% Dockerized)              │
│  - MongoDB 7.0 (MONGO_APP_USER + AES-256-GCM Field Crypto)  │
│  - Redis 7 Alpine (AOF + --requirepass)                     │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚙️ المتطلبات الأساسية للتشغيل

- **Android SDK:** Compile/Target SDK `35` (الحد الأدنى المدعوم MinSdk `24`).
- **Kotlin / AGP:** Kotlin `2.0.21` + AGP `8.7.2` + JDK `17`.
- **Server Runtime:** **Docker Engine + Docker Compose v2 فقط** (كل الخدمات وقواعد البيانات والـ Web Portal والشهادات تعمل داخل حاويات Docker عبر `docker-compose.prod.yml`).

---

## 🚀 دليل التشغيل المحلي وبناء الأندرويد

```bash
# بناء وتشغيل اختبارات الوحدة و Robolectric
gradle :app:testDebugUnitTest

# بناء نسخة Debug أو الإنتاج الموقّعة
gradle :app:assembleDebug
gradle :app:bundleRelease
gradle :app:assembleRelease
```

---

## ☁️ دليل النشر الإنتاجي على السيرفر (100% Dockerized)

لتشغيل كامل منظومة الخادم وقواعد البيانات وبوابة الويب وخادم المكالمات داخل Docker:

```bash
chmod +x scripts/deploy-prod.sh deploy/mongo/init-app-user.sh
./scripts/deploy-prod.sh init-env
./scripts/deploy-prod.sh issue-ssl admin@oozox.com
./scripts/deploy-prod.sh test
./scripts/deploy-prod.sh up
./scripts/deploy-prod.sh create-admin "admin@oozox.com" "+201000000000" "STRONG_ADMIN_PASS_16+"
./scripts/deploy-prod.sh smoke
```

للتفاصيل الكاملة راجع:
- **`/OPENCODE_PRODUCTION_RUNBOOK.txt`**
- **`/OPENCODE_SERVER_DEPLOYMENT_GUIDE.md`**

---

## 🔐 متغيرات البيئة والأمان

المتغيرات الإنتاجية يتم توليدها عبر `./scripts/deploy-prod.sh init-env` داخل `.env.production` (انظر `.env.production.example`):
- `MONGO_ROOT_USER`, `MONGO_ROOT_PASSWORD`, `MONGO_APP_USER`, `MONGO_APP_PASSWORD`, `MONGO_DB_NAME`
- `DB_ENCRYPTION_KEY` (مفتاح AES-256-GCM لتشفير حقول قاعدة البيانات `Invoice.notes` و`Notification.message`)
- `REDIS_PASSWORD`
- `JWT_SECRET` (مشترك بين `api` و`signaling`)
- `COTURN_SECRET`, `SERVER_PUBLIC_IP`, `CLIENT_ORIGIN`, `ALLOWED_ORIGINS`, `VITE_API_BASE_URL`

---

## 📡 مواصفات الـ APIs والمصادقة

### 1. المصادقة وتأمين الحسابات (`/api/auth`)
- `POST /api/auth/register`: تسجيل مستخدم أو تاجر (`USER` / `MERCHANT` فقط) مع تشفير كلمة المرور بـ `bcrypt(12)`.
- `POST /api/auth/login`: التحقق من الحساب عبر `{ emailOrPhone, password }` وإصدار رمز `JWT`.
- `GET /api/auth/me`: جلب ملف المستخدم الحالي.
- `GET /api/auth/search?q=`: البحث عن المستخدمين لإصدار الفواتير.

### 2. الفواتير والإشعارات (`/api/invoices`, `/api/notifications`)
- `GET|POST /api/invoices`, `GET|DELETE /api/invoices/:id`, `PATCH /api/invoices/:id/status`
- `GET /api/notifications`, `PATCH /api/notifications/read-all`, `PATCH /api/notifications/:id/read`

### 3. إشارات WebRTC (`signaling.oozox.com`)
- `GET /health`, `GET /ws` (WSS + JWT), `POST /match`, `POST /turn-credentials`, `GET /status/load`

---

## ⚡ استراتيجيات تحسين الأداء والاختبارات

1. **العمل دون اتصال (Offline-First):** تخزين جميع العمليات محلياً في قاعدة بيانات Room مشفرة، ومزامنتها تلقائياً عند توفر الإنترنت.
2. **ضغط البيانات وتوفير الذاكرة:** استخدام التنسيق الجدولي للأرقام واسترجاع الحسابات دفعة واحدة لتقليل استهلاك الشبكة على شبكات 3G/4G.
3. **تدقيق الجودة (Lighthouse & Jest):** اجتياز كافة اختبارات الأمان والوصولية ونسب التباين العالية (WCAG AAA) بنسبة 100%.

---
**OOZOX Platform Architecture Team © 2026**
