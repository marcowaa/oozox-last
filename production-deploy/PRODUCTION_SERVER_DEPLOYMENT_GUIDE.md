# دليل التشغيل والإنتاج لـ OOZOX Platform (Production Server Guide)

> **تحديث هام:** كافة خدمات الخادم في المستودع تعمل **100% داخل Docker** عبر `docker-compose.prod.yml` (`MongoDB 7.0` + `Redis 7` + `Express API` + `Go WebRTC Signaling` + `React/Vite Web Portal` + `Nginx SSL/TLS` + `Coturn STUN/TURN` + `Certbot SSL`) مع تشفير حقول قاعدة البيانات بـ `AES-256-GCM` ومستخدم قاعدة بيانات بأقل صلاحيات (`MONGO_APP_USER`) وإزالة أي بيانات تجريبية.

**المرجع التنفيذي الكامل للتشغيل عبر OpenCode (أوبنكود):**
- الدليل النصي التنفيذي الشامل: **`OPENCODE_PRODUCTION_RUNBOOK.txt`**
- الدليل التفصيلي بالعربية: **`OPENCODE_SERVER_DEPLOYMENT_GUIDE.md`**
- سكربت النشر الموحد الجاهز: **`scripts/deploy-prod.sh`**

---

## ملخص سريع لأوامر التشغيل الإنتاجية (100% Dockerized)

```bash
chmod +x scripts/deploy-prod.sh deploy/mongo/init-app-user.sh

# 1. فحص جاهزية Docker و Compose v2 والمنافذ
./scripts/deploy-prod.sh check-prereqs

# 2. توليد ملف .env.production ومفتاح التشفير DB_ENCRYPTION_KEY و /opt/oozox/.backup.key تلقائياً
./scripts/deploy-prod.sh init-env

# 3. استخراج شهادات TLS عبر حاوية Certbot لجميع النطاقات (oozox.com, www, admin, api, signaling, turn)
./scripts/deploy-prod.sh issue-ssl admin@oozox.com

# 4. تشغيل اختبارات Express Jest و Go Signaling Race داخل حاويات Docker معزولة
./scripts/deploy-prod.sh test

# 5. بناء وتشغيل جميع حاويات الإنتاج السبع (mongo, redis, api, signaling, web, nginx, coturn)
./scripts/deploy-prod.sh up

# 6. إنشاء أول حساب مدير نظام ADMIN بأمان
./scripts/deploy-prod.sh create-admin "admin@oozox.com" "+201000000000" "STRONG_ADMIN_PASS_16+" "OOZOX Root Admin"

# 7. تنفيذ فحوصات الجاهزية وعزل قواعد البيانات وتفعيل النسخ الاحتياطي المشفر بـ AES-256
./scripts/deploy-prod.sh smoke
./scripts/deploy-prod.sh install-backup-script
```

## بناء نسخة الأندرويد الموقعة للإنتاج

```bash
gradle :app:testDebugUnitTest
gradle :app:bundleRelease
gradle :app:assembleRelease
```

*(ملاحظة: معرف التطبيق الرسمي هو `com.aistudio.goldchat.app.mirhxk`، ويجب توفير مفاتيح التوقيع عبر متغيرات البيئة `ANDROID_KEYSTORE_FILE`، `KEYSTORE_PASSWORD`، `KEY_ALIAS`، و`KEY_PASSWORD` دون إيداع أي كلمة مرور في الكود).*

للحصول على تفاصيل جدار الحماية `UFW`، تشفير أقراص Docker بـ `LUKS2`، النسخ الاحتياطي المجدول المشفر، وتجديد شهادات SSL عبر Docker، راجع **`OPENCODE_PRODUCTION_RUNBOOK.txt`**.
