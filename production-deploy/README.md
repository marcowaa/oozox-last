# 📦 الحزمة الموحدة للنشر الإنتاجي — OOZOX Production Deployment Suite

يجمع هذا المجلد (**`production-deploy/`**) **كافة** الوثائق المحدّثة، ووثيقة المتطلبات **`SRS.md`**، وقائمة مزوّدي الخدمات الخارجية، وملفات **OpenCode (أوبنكود)**، وسكربت التشغيل الموحد في مكان واحد جاهز للاستخدام على الخادم الإنتاجي.

> **ملاحظة:** تعمل جميع خدمات الخادم وقواعد البيانات وبوابة الويب وخادم المكالمات والشهادات **100% داخل حاويات Docker** عبر `docker-compose.prod.yml` في جذر المستودع. تم الحفاظ على الملفات الأصلية في جذر المستودع لتعمل مسارات Docker Compose وGitHub Actions دون كسر، مع توفير نسخة محدثة ومزامَنة بالكامل هنا.

---

## 📂 محتويات المجلد

| الملف | الوظيفة والغرض |
| :--- | :--- |
| **`README.md`** | الفهرس الرئيسي وخارطة التشغيل السريعة |
| **`EXTERNAL_SERVICES_AND_SUBSCRIPTIONS.md`** | **دليل مزودي الخدمات الخارجية والاشتراكات:** ما يلزم الاشتراك فيه فعلياً (VPS + Domain) مقابل ما تم استبداله مجاناً بـ Docker، والخدمات الاختيارية (FCM / Gemini / Google Play) |
| **`OPENCODE_PROMPT.txt`** | **برومبت جاهز للنسخ واللصق لـ OpenCode (أوبنكود)** لتنفيذ النشر الكامل على السيرفر تلقائياً |
| **`OPENCODE_PRODUCTION_RUNBOOK.txt`** | الدليل النصي التنفيذي الصارم لـ OpenCode (بنية الحاويات السبع + UFW + LUKS2 + الأسرار + Certbot + Smoke Checks + النسخ الاحتياطي المشفر) |
| **`OPENCODE_SERVER_DEPLOYMENT_GUIDE.md`** | الدليل العربي التفصيلي الشامل لأوبنكود وخطوات التثبيت والاختبار والصيانة |
| **`PRODUCTION_SERVER_DEPLOYMENT_GUIDE.md`** | الدليل المختصر المحدّث لأوامر التشغيل الإنتاجية |
| **`DEPLOYMENT_HANDOVER_YUMNA.md`** | دليل التسليم الهندسي للمهندسة يمنى وفريق المتاجر (`com.aistudio.goldchat.app.mirhxk`) |
| **`SRS.md`** | وثيقة المتطلبات البرمجية المحدّثة (**الإصدار 4.0** المطابق للكود الفعلي وأمن قواعد البيانات) |
| **`deploy-prod.sh`** | واجهة تنفيذية موحدة تشغّل `../scripts/deploy-prod.sh` مباشرة من أي مسار |
| **`.env.production.example`** | قالب متغيرات البيئة الإنتاجية للمراجعة المرجعية |

---

## ⚡ التشغيل السريع على السيرفر (عبر السكربت الموحد)

```bash
chmod +x scripts/deploy-prod.sh deploy/mongo/init-app-user.sh production-deploy/deploy-prod.sh

# 1. فحص Docker والمنافذ
./production-deploy/deploy-prod.sh check-prereqs

# 2. توليد الأسرار ومفاتيح التشفير (.env.production + /opt/oozox/.backup.key)
./production-deploy/deploy-prod.sh init-env

# 3. إصدار شهادات SSL المجانية عبر حاوية Certbot
./production-deploy/deploy-prod.sh issue-ssl admin@oozox.com

# 4. تشغيل اختبارات Express Jest و Go Signaling Race داخل حاويات Docker
./production-deploy/deploy-prod.sh test

# 5. بناء وتشغيل كافة الحاويات السبع (mongo, redis, api, signaling, web, nginx, coturn)
./production-deploy/deploy-prod.sh up

# 6. إنشاء أول حساب مدير نظام ADMIN
./production-deploy/deploy-prod.sh create-admin "admin@oozox.com" "+201000000000" "STRONG_ADMIN_PASS_16+"

# 7. فحص الجاهزية وتفعيل النسخ الاحتياطي المشفر بـ AES-256
./production-deploy/deploy-prod.sh smoke
./production-deploy/deploy-prod.sh install-backup-script
```

---

## 🤖 التشغيل التلقائي عبر OpenCode (أوبنكود)

1. ارفع المشروع أو استنسخه على السيرفر داخل `/opt/oozox/repo`.
2. افتح **OpenCode** داخل `/opt/oozox/repo`.
3. انسخ محتوى ملف **`production-deploy/OPENCODE_PROMPT.txt`** وأرسله إلى OpenCode.
