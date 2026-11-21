# دليل التشغيل والإنتاج الشامل لمنظومة OOZOX عبر OpenCode (أوبنكود)

> **الغرض من الملف:** مرجع تنفيذي دقيق ومطابق 100% للكود الفعلي في المستودع لتمكين وكيل **OpenCode** (أو مهندس التشغيل) من فحص، اختبار، تأمين، وتشغيل كامل منظومة **OOZOX** في بيئة الإنتاج على خادم Linux (VPS) دون أخطاء أو بيانات تجريبية.

---

## 1. الخريطة المعمارية الفعلية للمستودع (Verified Architecture)

يتكوّن المشروع من المكونات التالية بالضبط كما هي مبنية في المستودع:

| المكون | المسار في المستودع | التقنية والمنفذ الداخلي | الدور في الإنتاج |
| :--- | :--- | :--- | :--- |
| **Core REST API** | `backend/` | Node.js 20 + Express + Mongoose (منفذ `5000`) | المصادقة (`JWT`)، الفواتير، الإشعارات، البحث، فحص الصحة `/api/health` |
| **WebRTC Signaling & Matchmaking** | `backend-signaling/` | Go 1.22 + Gorilla WebSocket + Redis (منفذ `8080`) | إشارات المكالمات `wss://signaling.oozox.com/ws`، `/match`، توليد بيانات `TURN`، فحص الصحة `/health` |
| **MongoDB 7.0** | حاوية `oozox_mongo_prod` | `mongo:7.0` (منفذ داخلي `27017` غير مكشوف للإنترنت) | قاعدة البيانات الرئيسية للمستخدمين والفواتير والإشعارات |
| **Redis 7** | حاوية `oozox_redis_prod` | `redis:7-alpine` (`AOF` + `--requirepass`, `512MB`) | توجيه WebSocket، قوائم الانتظار، و`Rate Limit` لبيانات TURN |
| **Coturn STUN/TURN** | حاوية `oozox_coturn_prod` | `coturn/coturn:4.6-alpine` (`network_mode: host`) | منافذ `3478` (TCP/UDP)، `5349` (TLS)، ومدى الوسائط `49152-65535/udp` |
| **Nginx Reverse Proxy** | `deploy/nginx/nginx.conf` | `nginx:1.27-alpine` (منافذ `80`, `443`) | إنهاء TLS/HTTPS، تمرير `oozox.com` و`api.oozox.com` و`signaling.oozox.com` |
| **Web Portal & Admin Gate** | `deploy/web/Dockerfile` (`artifacts/mockup-sandbox/`) | حاوية `oozox_web_prod` (`nginx:1.27-alpine` داخلي) | الموقع التعريفي، بوابة الويب، لوحة الإدارة `/admin/login` داخل Docker بالكامل |
| **Certbot SSL** | `certbot` (`profiles: ["certbot"]`) | `certbot/certbot:latest` | إصدار وتجديد شهادات Let's Encrypt عبر Docker دون تثبيته على النظام المضيف |
| **Android Native App** | `app/` | Kotlin + Jetpack Compose + Room + Retrofit | التطبيق الرئيسي (`applicationId`: `com.aistudio.goldchat.app.mirhxk`, `SDK 24..35`) |

---

## 2. نقاط النهاية (Endpoints) وعقود التشغيل الحقيقية

### أ) خادم API الرئيسي (`https://api.oozox.com`)
- `GET /api/health` — فحص حالة الخادم وقاعدة بيانات MongoDB (يعيد `200 HEALTHY` أو `503 DEGRADED`).
- `POST /api/auth/register` — تسجيل حساب جديد (`USER` أو `MERCHANT` فقط؛ ممنوع تسجيل `ADMIN` من الواجهة العامة).
- `POST /api/auth/login` — تسجيل الدخول باستخدام الحقول:
  ```json
  { "emailOrPhone": "<EMAIL_OR_PHONE>", "password": "<PASSWORD>" }
  ```
  ويعيد `{ "success": true, "data": { "user": { ... }, "token": "<JWT>" } }`.
- `GET /api/auth/me` — جلب بيانات المستخدم الحالي (يتطلب `Authorization: Bearer <JWT>`).
- `GET /api/auth/search?q=...` — البحث عن مستخدمين عبر الهاتف أو البريد (يتطلب JWT).
- `GET|POST /api/invoices`, `GET|DELETE /api/invoices/:id`, `PATCH /api/invoices/:id/status` — إدارة الفواتير (محمية بـ JWT).
- `GET /api/notifications`, `PATCH /api/notifications/read-all`, `PATCH /api/notifications/:id/read` — الإشعارات (محمية بـ JWT).

### ب) خادم الإشارات والمكالمات (`https://signaling.oozox.com`)
- `GET /health` — فحص اتصال Redis وحالة الحمولة (يعيد `200 HEALTHY` أو `503 DEGRADED`).
- `GET /ws` — اتصال WebSocket (`wss://signaling.oozox.com/ws`) يتطلب JWT في رأس `Authorization: Bearer <JWT>` أو معامل `?token=`.
- `POST /match` — مطابقة المكالمات (يتطلب JWT).
- `POST /turn-credentials` — توليد بيانات TURN مؤقتة بـ HMAC-SHA1 صالحة حتى `3600` ثانية بحد أقصى طلب واحد كل `30` ثانية لكل مستخدم.
- `GET /status/load` — قراءة حالة الحماية من الضغط (`LoadShedder`).
- **ملاحظة أمنية:** نقطة `POST /auth/token` معطلة تلقائياً في الإنتاج عبر `APP_ENV=production` و`AUTH_TOKEN_ENABLED=false`.
- **توحيد `JWT_SECRET`:** يجب أن يكون متطابقاً تماماً بين `api` و`signaling` وبحد أدنى `32` حرفاً (الموصى به `48-64` بايت hex).

---

## 3. متطلبات الخادم (VPS Prerequisites) وDNS وجدار الحماية

### أ) مواصفات الخادم المقترحة
- نظام التشغيل: **Ubuntu 24.04 LTS x86_64** (أو 22.04 LTS)
- العتاد: **4 vCPU / 8 GB RAM** على الأقل (أو 2 vCPU / 4 GB للبدء) + **60 GB NVMe SSD**
- عنوان IPv4 ثابت (`SERVER_PUBLIC_IP`)

### ب) سجلات DNS المطلوبة قبل التشغيل
أضف سجلات `A` تشير إلى `SERVER_PUBLIC_IP` في مزود النطاق (`oozox.com`):
- `oozox.com` → `<SERVER_PUBLIC_IP>`
- `www.oozox.com` → `<SERVER_PUBLIC_IP>`
- `admin.oozox.com` → `<SERVER_PUBLIC_IP>`
- `api.oozox.com` → `<SERVER_PUBLIC_IP>`
- `signaling.oozox.com` → `<SERVER_PUBLIC_IP>`
- `turn.oozox.com` → `<SERVER_PUBLIC_IP>` (يجب تعطيل Proxy السحابي/Cloudflare البرتقالي لـ `turn.oozox.com` وجعله DNS-Only لمرور UDP)

### ج) إعداد الخادم وجدار الحماية UFW + Docker (دون تثبيت Nginx/Certbot/Mongo/Redis على النظام المضيف)
نفذ الأوامر التالية على الخادم بصلاحية `sudo`:

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y curl git ufw openssl jq cryptsetup

# إيقاف أي خدمات مضيفة قد تتعارض مع حاويات Docker على المنافذ 80/443
sudo systemctl disable --now nginx apache2 mongod redis-server 2>/dev/null || true

# تفعيل جدار الحماية بالمنافذ الإنتاجية المطلوبة فقط
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp              # SSH
sudo ufw allow 80/tcp              # HTTP / Certbot ACME
sudo ufw allow 443/tcp             # HTTPS / WSS
sudo ufw allow 3478/tcp            # Coturn STUN/TURN TCP
sudo ufw allow 3478/udp            # Coturn STUN/TURN UDP
sudo ufw allow 5349/tcp            # Coturn TURNS (TLS)
sudo ufw allow 49152:65535/udp     # WebRTC RTP/SRTP Media Relay
sudo ufw --force enable

# تثبيت Docker و Docker Compose v2
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"
newgrp docker
docker --version && docker compose version
```

---

## 4. خطوات التشغيل الدقيقة بواسطة OpenCode (أوبنكود)

> **توجيه مباشر لـ OpenCode:** عند تسليم المشروع على السيرفر، اتبع الخطوات بالترتيب التالي ولا تتخطَّ خطوة استخراج الشهادات قبل تشغيل Nginx.

### الخطوة 1: استنساخ المشروع وتجهيز مجلدات العمل
```bash
sudo mkdir -p /opt/oozox /etc/letsencrypt /var/www/certbot /opt/oozox/backups
sudo chown -R "$USER":"$USER" /opt/oozox
git clone <YOUR_REPOSITORY_URL> /opt/oozox/repo
cd /opt/oozox/repo
chmod +x scripts/deploy-prod.sh deploy/mongo/init-app-user.sh
```

> **المسار السريع المؤتمت (موصى به):** يمكنك تنفيذ كافة الخطوات التالية عبر السكربت الموحد **`./scripts/deploy-prod.sh`**:
> `./scripts/deploy-prod.sh init-env` ← `./scripts/deploy-prod.sh issue-ssl admin@oozox.com` ← `./scripts/deploy-prod.sh test` ← `./scripts/deploy-prod.sh up` ← `./scripts/deploy-prod.sh create-admin ...` ← `./scripts/deploy-prod.sh smoke`

### الخطوة 2: توليد المفاتيح السرية وإنشاء ملف `.env.production`
يمكنك توليد الملف تلقائياً عبر `./scripts/deploy-prod.sh init-env` (والذي ينشئ أيضاً `/opt/oozox/.backup.key`) أو يدوياً:
```bash
export SERVER_PUBLIC_IP=$(curl -4 -fsS https://ifconfig.me)
export MONGO_PASS=$(openssl rand -hex 24)
export MONGO_APP_PASS=$(openssl rand -hex 24)
export DB_ENC_KEY=$(openssl rand -hex 32)
export REDIS_PASS=$(openssl rand -hex 24)
export JWT_KEY=$(openssl rand -hex 48)
export COTURN_KEY=$(openssl rand -hex 32)
export BACKUP_ENC_PASS=$(openssl rand -hex 32)

cat > .env.production <<EOF
SERVER_PUBLIC_IP=${SERVER_PUBLIC_IP}
TURN_REALM=turn.oozox.com

MONGO_ROOT_USER=oozox_root
MONGO_ROOT_PASSWORD=${MONGO_PASS}
MONGO_APP_USER=oozox_app
MONGO_APP_PASSWORD=${MONGO_APP_PASS}
MONGO_DB_NAME=oozox_prod_db

DB_ENCRYPTION_KEY=${DB_ENC_KEY}

REDIS_PASSWORD=${REDIS_PASS}

JWT_SECRET=${JWT_KEY}
JWT_EXPIRES_IN=7d

CLIENT_ORIGIN=https://oozox.com,https://www.oozox.com,https://admin.oozox.com
ALLOWED_ORIGINS=https://oozox.com,https://www.oozox.com,https://admin.oozox.com
API_RATE_LIMIT_MAX=500

COTURN_SECRET=${COTURN_KEY}
TURN_URLS=turn:turn.oozox.com:3478?transport=udp,turn:turn.oozox.com:3478?transport=tcp,turns:turn.oozox.com:5349?transport=tcp
TURN_TTL=3600

VITE_API_BASE_URL=https://api.oozox.com
EOF

chmod 600 .env.production
echo "BACKUP_ENCRYPTION_PASSPHRASE=${BACKUP_ENC_PASS}" > /opt/oozox/.backup.key
chmod 600 /opt/oozox/.backup.key
```
> **تنبيه أمني حرج:** احتفظ بنسخة احتياطية خارجية آمنة من `DB_ENCRYPTION_KEY` ومن `/opt/oozox/.backup.key`؛ بدونهما لا يمكن فك تشفير حقول `Invoice.notes` / `Notification.message` أو استعادة النسخ الاحتياطية المشفرة.

تحقق قبل المتابعة أنه لا توجد أي قيم متبقية من `CHANGE_ME`:
```bash
grep -n "CHANGE_ME" .env.production && echo "ERROR: Replace placeholders!" || echo "ENV_CLEAN_OK"
```

### الخطوة 3: استخراج شهادات SSL/TLS عبر حاوية Certbot (إلزامي قبل تشغيل Nginx)
> **تنبيه حرج:** ملف `deploy/nginx/nginx.conf` يقرأ شهادات `oozox.com` و`api.oozox.com` و`signaling.oozox.com`، كما يقرأ `turn.oozox.com` في `coturn`. استخرج الشهادات أولاً عبر حاوية `certbot` المدمجة في `docker-compose.prod.yml`:

```bash
cd /opt/oozox/repo
docker compose -f docker-compose.prod.yml --env-file .env.production run --rm --service-ports certbot \
  certonly --standalone --non-interactive --agree-tos --email admin@oozox.com -d oozox.com -d www.oozox.com -d admin.oozox.com

docker compose -f docker-compose.prod.yml --env-file .env.production run --rm --service-ports certbot \
  certonly --standalone --non-interactive --agree-tos --email admin@oozox.com -d api.oozox.com

docker compose -f docker-compose.prod.yml --env-file .env.production run --rm --service-ports certbot \
  certonly --standalone --non-interactive --agree-tos --email admin@oozox.com -d signaling.oozox.com

docker compose -f docker-compose.prod.yml --env-file .env.production run --rm --service-ports certbot \
  certonly --standalone --non-interactive --agree-tos --email admin@oozox.com -d turn.oozox.com
```

### الخطوة 4: تشغيل اختبارات الخادم داخل حاويات Docker
#### أ) اختبارات Express Backend (`backend/`)
```bash
docker run --rm -v /opt/oozox/repo/backend:/app -w /app node:20-alpine sh -c "npm ci && npm test"
```
#### ب) اختبارات Go Signaling (`backend-signaling/`)
```bash
docker run --rm -v /opt/oozox/repo/backend-signaling:/app -w /app golang:1.22-alpine go test -race -v ./...
```

### الخطوة 5: تشغيل المنظومة كاملة 100% داخل Docker
```bash
cd /opt/oozox/repo
docker compose -f docker-compose.prod.yml --env-file .env.production up -d --build
```
راقب الحالة حتى تصبح جميع الحاويات `healthy` / `Up`:
```bash
docker compose -f docker-compose.prod.yml --env-file .env.production ps
docker compose -f docker-compose.prod.yml --env-file .env.production logs --tail=50 api signaling nginx coturn
```

### الخطوة 6: إنشاء أول حساب مدير نظام (`ADMIN`) بأمان
> تم تنظيف المشروع بالكامل من أي حسابات افتراضية أو كلمات مرور مدمجة (`zox`, `8888`, إلخ). لا تسمح واجهة `/api/auth/register` بإنشاء `ADMIN`. يجب إنشاء أول حساب إدارة مباشرة من سطر الأوامر داخل الخادم عبر حاوية `api` (بوجود `mongoose` و`bcryptjs` مثبتين مسبقاً في الحاوية):

```bash
docker compose -f docker-compose.prod.yml --env-file .env.production exec -T api node -e '
const mongoose = require("mongoose");
const User = require("./src/models/User");
(async () => {
  await mongoose.connect(process.env.MONGO_URI);
  const email = process.argv[1];
  const phone = process.argv[2];
  const password = process.argv[3];
  const name = process.argv[4] || "System Administrator";
  if (!email || !phone || !password || password.length < 10) {
    console.error("Usage args: <email> <phone> <password>=10+chars [name]");
    process.exit(1);
  }
  const exists = await User.findOne({ $or: [{ email }, { phone }] });
  if (exists) {
    exists.role = "ADMIN";
    exists.password = password;
    await exists.save();
    console.log("Updated existing user to ADMIN:", exists._id.toString());
  } else {
    const created = await User.create({ name, email, phone, password, role: "ADMIN", isVerified: true });
    console.log("Created production ADMIN:", created._id.toString());
  }
  await mongoose.disconnect();
})();
' "admin@oozox.com" "+201XXXXXXXXX" "STRONG_ADMIN_PASS_16+" "OOZOX Root Admin"
```

---

## 5. التحقق الإلزامي بعد التشغيل (Post-Deployment Smoke Checks)

نفّذ هذه الفحوصات وتأكد أن كل فحص ينجح قبل إعلان جاهزية الخادم:

```bash
# 1. فحص API الرئيسي ومصادقة MongoDB عبر HTTPS
curl -fsS https://api.oozox.com/api/health | jq .

# 2. فحص خدمة الإشارات واتصال Redis عبر HTTPS
curl -fsS https://signaling.oozox.com/health | jq .

# 3. التأكد من إغلاق نقطة التوليد التطويري في الإنتاج (يجب أن يعيد 403)
curl -i -s -X POST https://signaling.oozox.com/auth/token -d '{"user_id":"test"}' | head -n 5

# 4. اختبار تسجيل دخول الأدمن واستلام JWT حقيقي + جلب بيانات TURN المؤقتة
TOKEN=$(curl -fsS -X POST https://api.oozox.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"emailOrPhone":"admin@oozox.com","password":"STRONG_ADMIN_PASS_16+"}' | jq -r '.data.token')

curl -fsS -X POST https://signaling.oozox.com/turn-credentials \
  -H "Authorization: Bearer ${TOKEN}" | jq .

# 5. التحقق من أن قواعد البيانات غير مكشوفة على المنافذ الخارجية
ss -tulpn | grep -E '27017|6379' || echo "Mongo and Redis properly isolated inside Docker network"
```

---

## 6. بناء واستضافة بوابة الويب ولوحة الإدارة (`deploy/web/Dockerfile`)

تم دمج بناء واستضافة واجهة الويب (`artifacts/mockup-sandbox`) بالكامل داخل حاوية **`web`** (`oozox_web_prod`) ضمن `docker-compose.prod.yml` دون حاجة لتثبيت Node.js أو pnpm على الخادم المضيف. عند تنفيذ `docker compose -f docker-compose.prod.yml --env-file .env.production up -d --build` يتم:
1. بناء حزمة Vite الإنتاجية تلقائياً مع تمرير `VITE_API_BASE_URL` (من `.env.production`).
2. تشغيل خادم Nginx داخلي خفيف (`oozox_web_prod`) يدعم SPA routing.
3. ربط النطاقات `oozox.com` و`www.oozox.com` و`admin.oozox.com` عبر `oozox_nginx_prod` مع تمرير `/api/` مباشرة إلى حاوية `api:5000`.

---

## 7. بناء وتوقيع تطبيق الأندرويد للإنتاج (`app/`)

### أ) التحقق من نقاط الربط في تطبيق الأندرويد
- عنوان API الافتراضي مضبوط في `RetrofitClient.kt` على `https://api.oozox.com/api/`
- عنوان WebSocket Signaling الافتراضي مضبوط في `WebRtcSignalingManager.kt` على `wss://signaling.oozox.com/ws`
- معرف الحزمة الرسمي في `app/build.gradle.kts` و`.github/workflows/deploy.yml`: `com.aistudio.goldchat.app.mirhxk`

### ب) تجهيز Firebase لإشعارات الدفع (FCM)
تمت إزالة كافة مفاتيح Firebase المدمجة من الكود (`FcmNotificationManager.kt`).
لتفعيل إشعارات FCM في الإنتاج:
1. أنشئ مشروعاً في Firebase Console وأضف تطبيق أندرويد بالمعرف `com.aistudio.goldchat.app.mirhxk`.
2. حمّل ملف `google-services.json` وضعه في `/opt/oozox/repo/app/google-services.json`.

### ج) توليد Keystore وبناء `.aab` و`.apk` للإنتاج
```bash
keytool -genkeypair -v \
  -keystore /opt/oozox/release.keystore \
  -alias releasekey \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=OOZOX, OU=Mobile, O=OOZOX, L=Cairo, C=EG"

export ANDROID_KEYSTORE_FILE=/opt/oozox/release.keystore
export KEYSTORE_PASSWORD="<YOUR_KEYSTORE_PASSWORD>"
export KEY_ALIAS="releasekey"
export KEY_PASSWORD="<YOUR_KEY_PASSWORD>"

cd /opt/oozox/repo
gradle :app:testDebugUnitTest
gradle :app:bundleRelease
gradle :app:assembleRelease
```
المخرجات النهائية الموقّعة والمعمّاة بـ R8:
- حزمة متجر Play: `app/build/outputs/bundle/release/app-release.aab`
- ملف التثبيت المباشر: `app/build/outputs/apk/release/app-release.apk`

### د) متغيرات GitHub Actions المطلوبة للنشر الآلي (`.github/workflows/deploy.yml`)
- `ANDROID_KEYSTORE_BASE64`: ناتج `base64 -w 0 release.keystore`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS` (`releasekey`)
- `KEY_PASSWORD`
- `SERVICE_ACCOUNT_JSON_RAW`: ملف JSON لحساب خدمة Google Play Console

---

## 8. النسخ الاحتياطي اليومي، التجديد التلقائي للشهادات، والصيانة

### أ) سكربت النسخ الاحتياطي الآلي المشفر بـ AES-256-CBC لـ MongoDB (`/opt/oozox/backup_mongo.sh`)
يتم تثبيته مباشرة عبر الأمر:
```bash
/opt/oozox/repo/scripts/deploy-prod.sh install-backup-script
```
أو عبر تنفيذ النسخ يدوياً في أي وقت:
```bash
/opt/oozox/repo/scripts/deploy-prod.sh backup
```
(تُشفّر النسخ الناتجة بـ `openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000` باستخدام مفتاح `/opt/oozox/.backup.key` وتُحفظ بصلاحية `600` مع حذف النسخ الأقدم من 30 يوماً).

### ب) مهام `cron` (النسخ الاحتياطي المشفر + تجديد SSL عبر حاوية Certbot)
أضف عبر `crontab -e`:
```cron
# نسخ احتياطي يومي مشفر لقاعدة MongoDB الساعة 03:00 فجراً
0 3 * * * /opt/oozox/backup_mongo.sh >> /var/log/oozox_backup.log 2>&1

# تجديد شهادات Let's Encrypt عبر حاوية Certbot وإعادة تحميل حاوية Nginx أسبوعياً
0 4 * * 1 cd /opt/oozox/repo && ./scripts/deploy-prod.sh renew-ssl >> /var/log/oozox_certbot.log 2>&1
```

### ج) التحديث دون توقف (Rolling Update)
```bash
cd /opt/oozox/repo
git pull origin main
./scripts/deploy-prod.sh update
```

---

## 9. Prompt جاهز لتسليمه مباشرة إلى OpenCode على السيرفر

انسخ هذا النص وأعطه لـ **OpenCode** داخل مجلد المشروع على السيرفر:

```text
Read OPENCODE_PRODUCTION_RUNBOOK.txt completely and deploy the 100% Dockerized OOZOX stack on this server:
1. Verify Docker, Docker Compose v2, UFW ports (22, 80, 443, 3478/tcp+udp, 5349/tcp, 49152:65535/udp), and DNS resolution for oozox.com, www.oozox.com, admin.oozox.com, api.oozox.com, signaling.oozox.com, and turn.oozox.com.
2. Generate strong secrets with openssl rand into .env.production (including MONGO_APP_USER, MONGO_APP_PASSWORD, DB_ENCRYPTION_KEY, REDIS_PASSWORD, JWT_SECRET, COTURN_SECRET, VITE_API_BASE_URL) and /opt/oozox/.backup.key. Confirm zero CHANGE_ME placeholders remain.
3. Issue Let's Encrypt TLS certificates via the containerized certbot service BEFORE starting nginx and coturn.
4. Run Express Jest tests (backend) and Go race tests (backend-signaling) via Docker containers and verify all pass.
5. Start all 7 containers (mongo, redis, api, signaling, web, nginx, coturn) via: docker compose -f docker-compose.prod.yml --env-file .env.production up -d --build
6. Create the initial production ADMIN account via the Node/Mongoose script documented in Section 8 (ask me for email/phone/password if not provided).
7. Run every smoke check in Section 9, configure AES-256 encrypted backups + cron jobs from Section 10, and report the exact outputs.
```
