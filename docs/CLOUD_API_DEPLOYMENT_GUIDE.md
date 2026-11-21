# 🚀 الدليل المحدّث لنشر خادم الـ API والمنظومة السحابية (100% Dockerized)
> **منصة OOZOX**  
> **تحديث هام:** يعمل الخادم حالياً بالكامل داخل **Docker** (`Express API + MongoDB 7 + Go Signaling + Redis 7 + Coturn + Web Portal + Nginx TLS + Certbot`) على VPS واحد دون حاجة لاشتراكات Render أو PostgreSQL خارجي.

---

## 📋 خطوات التشغيل السريع على الخادم (VPS)

1. تأكد من توجيه سجلات `DNS A` للنطاق (`oozox.com`، `www`، `admin`، `api`، `signaling`، `turn`) إلى IP السيرفر.
2. نفّذ الأوامر التالية من جذر المشروع:
   ```bash
   chmod +x scripts/deploy-prod.sh deploy/mongo/init-app-user.sh
   ./scripts/deploy-prod.sh check-prereqs
   ./scripts/deploy-prod.sh init-env
   ./scripts/deploy-prod.sh issue-ssl admin@oozox.com
   ./scripts/deploy-prod.sh test
   ./scripts/deploy-prod.sh up
   ./scripts/deploy-prod.sh create-admin "admin@oozox.com" "+201000000000" "STRONG_ADMIN_PASS_16+"
   ./scripts/deploy-prod.sh smoke
   ```
3. للتحقق بعد التشغيل:
   - `https://api.oozox.com/api/health` → يعيد `200 HEALTHY`
   - `https://signaling.oozox.com/health` → يعيد `200 HEALTHY`
   - `https://oozox.com` → بوابة الويب ولوحة الإدارة `/admin/login`

## 📱 نقاط الربط في تطبيق الأندرويد (`app/`)
- `RetrofitClient.kt`: `https://api.oozox.com/api/`
- `WebRtcSignalingManager.kt`: `wss://signaling.oozox.com/ws`

للمرجع التنفيذي الكامل وقائمة مزودي الخدمات الخارجية ودليل **OpenCode**، راجع المجلد الموحد **`production-deploy/`** (وخاصة `OPENCODE_PRODUCTION_RUNBOOK.txt` و`EXTERNAL_SERVICES_AND_SUBSCRIPTIONS.md`).
