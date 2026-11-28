# سياسة الخصوصية وحذف البيانات / Privacy Policy & Data Deletion

**تطبيق OOZOX (ذهبي)**  
**تاريخ السريان / Effective Date:** 2026-11-28  
**آخر تحديث / Last Updated:** 2026-11-28  
**البريد الإلكتروني للتواصل / Contact Email:** marco0000110@gmail.com  

---

## 🇸🇦 العربية (Arabic Version)

### 1. مقدمة
توضح هذه السياسة كيفية جمع البيانات وحفظها ومعالجتها وحذفها في منصة **OOZOX** (تطبيق الأندرويد، خادم الـ API، خادم الاتصال، ولوحة الويب) بما يتوافق مع متطلبات **Google Play Data Safety & Account Deletion**.

### 2. البيانات التي نعالجها وأين تُخزَّن
- **بيانات الحساب والمصادقة:** رقم الهاتف (بصيغة E.164)، الاسم المعروض، معرّف الجهاز، وبصمة الجلسة/التوثيق، وتُستخدم لتسجيل الدخول برمز التحقق (OTP) وحماية الجلسات.
- **البيانات التجارية والمحاسبية:** الفواتير، كشوف الحساب، مجموعات العمل، المشاريع، رسائل المحادثات، وسجلات المكالمات (تُخزَّن محلياً على جهازك في قاعدة بيانات مشفرة وتُزامن مع الخادم عند تفعيل المزامنة السحابية).
- **الإشعارات (FCM):** رمز تسجيل Firebase Cloud Messaging لتسليم رسائل الدردشة الفورية وتنبيهات المكالمات الواردة.
- **المساعد الذكي (Gemini AI):** تمر جميع استفسارات الذكاء الاصطناعي (النصية والمرئية) حصرياً عبر خادم OOZOX السحابي (`POST /api/ai/chat` و`POST /api/ai/vision`) دون تخزين أي مفاتيح API على جهاز المستخدم. **لا نقوم بتسجيل محتوى المحادثات أو المطالبات (Prompts) في سجلات الخادم**.
- **لا نقوم ببيع أو تأجير بياناتك أو مشاركتها مع شبكات إعلانات خارجية.**

### 3. الأذونات الحساسة في تطبيق الأندرويد
- **الكاميرا والوسائط (Zero-Permission Photo Picker):** تُستخدم الكاميرا لمسح الفواتير أو مكالمات الفيديو، بينما يتم اختيار الصور من المعرض عبر منتقي صور Android دون طلب أذونات تخزين شاملة.
- **الميكروفون والكاميرا وخدمات المقدمة:** لتشغيل مكالمات الصوت والفيديو المشفرة (P2P WebRTC).
- **إشعارات ملء الشاشة للمكالمات الواردة (`USE_FULL_SCREEN_INTENT`):** لعرض شاشة الرنين عند ورود مكالمة والهاتف مقفل، ما دام المستخدم قد منح هذا الإذن.
- **جهات الاتصال والموقع (اختياري حسب الميزة):** لإظهار جهات الاتصال المسجلة أو التحقق الجغرافي من حضور الموظفين في الشركات.

### 4. حذف الحساب وإزالة البيانات (Account Deletion Policy)
يمكنك حذف حسابك نهائياً من داخل التطبيق عبر:
**الملف الشخصي ← الإعدادات / خيارات الحساب ← حذف الحساب نهائياً**  
*(كما تتوفر بوابة الويب العامة لحذف الحساب على الرابط: **`https://oozox.com/delete-account`** ضمن `web-portal/` لمتطلبات Google Play Data Safety).*

تتطلب عملية الحذف (`DELETE /api/auth/me`) تحققاً فورياً برمز **OTP (Step-Up Verification)** لحماية حسابك من الحذف غير المصرح به.

#### أ) البيانات التي تُحذف أو تُلغى فوراً عند تأكيد حذف الحساب:
1. بيانات الحساب الأساسية (الاسم، الهاتف، معرّف المستخدم، وربط الجهاز).
2. جميع الجلسات النشطة ورموز التحديث (Refresh Tokens).
3. كافة الأجهزة الموثوقة (Trusted Devices).
4. جميع رموز الإشعارات السحابية (`FCM Tokens`).
5. جلسات نقل الحساب بين الهواتف وأجزاؤها المشفرة (`Transfer Sessions & Chunks`).
6. كافة النسخ الاحتياطية السحابية المشفرة المرتبطة بالحساب (`Cloud Backups`).
7. سجل رموز التحقق (OTP History)، إشعارات المستخدم، وعضويات المجموعات والمشاريع، بالإضافة إلى مسح قاعدة البيانات المحلية والقبو الآمن على الهاتف.

#### ب) السجلات المشتركة التي يتم **إخفاء هويتها (Anonymization)** والسبب القانوني/التشغيلي:
- **الفواتير والمعاملات المشتركة والمحادثات الثنائية وسجلات المكالمات ومهام الموارد البشرية المرتبطة بأطراف أخرى:** لا يجوز حذفها من دفاتر الطرف المقابل لتجنب إتلاف السجلات المالية والمحاسبية الخاصة بالمستخدمين الآخرين. بدلاً من ذلك، يتم إزالة اسمك ومعرفات الاتصال بك واستبدالها بوسم مجهول الهوية (`Deleted User`) وفك ربطها بحسابك نهائياً.
- **دفتر قياس استهلاك الذكاء الاصطناعي (`AiUsageLedger`):** يحتفظ فقط بعدادات التكلفة والكميات المجمعة دون أي نصوص أو رقم هاتف أو هوية شخصية لمنع إساءة الاستخدام ولأغراض التدقيق المالي.
- **حماية آخر مسؤول نظام:** إذا كان الحساب هو آخر حساب `ADMIN` متبقٍ في المنصة، يرفض الخادم الحذف برمز `409 LAST_ADMIN_CANNOT_DELETE` حتى يتم تعيين مسؤول بديل أولاً.

### 5. تواصل معنا
- **البريد الإلكتروني:** `marco0000110@gmail.com`

---

## 🇺🇸 English Version

### 1. Introduction
This Privacy Policy explains how **OOZOX** processes, stores, secures, and deletes your personal and business data across our Android application, API server, signaling service, and web portal in compliance with **Google Play Developer Program Policies**.

### 2. Data Collection, Processing & AI Proxy
- **Account & Authentication Data:** Phone number (E.164), display name, device ID, and session tokens used strictly for OTP authentication and device-trust protection.
- **Business & Communication Data:** Invoices, accounting ledgers, projects, chat messages, and call logs (stored encrypted locally and synced only when cloud sync is active).
- **Push Notifications (FCM):** Firebase Cloud Messaging tokens used to deliver messages and incoming call alerts.
- **Server-Side Gemini AI Proxy:** All AI requests (`/api/ai/chat`, `/api/ai/vision`) run strictly server-side with per-user daily/monthly quotas and a global monthly cost cap. No Gemini API keys are stored on user devices, and **prompt contents are never logged**.
- **No Ad Tracking or Data Sales:** We never sell, rent, or share personal or financial data with third-party advertisers.

### 3. Permissions & Minimal Access
- **Media Selection:** Uses Android's zero-permission Photo Picker rather than broad storage permissions.
- **Microphone / Camera / Foreground Services:** Used strictly for active WebRTC voice/video calls and barcode/document capture.
- **Full-Screen Incoming Calls (`USE_FULL_SCREEN_INTENT`):** Displays the lock-screen incoming call interface when a peer calls.

### 4. Account & Data Deletion Rights
Users can permanently delete their account inside the app via:
**Profile → Account Options → Permanently Delete Account**  
*(A companion public web deletion page is available at **`https://oozox.com/delete-account`** in `web-portal/` for Google Play Console Data Safety compliance.)*

Account deletion (`DELETE /api/auth/me`) requires a **fresh OTP step-up verification** (`SENSITIVE_ACTION`). If the user is the last remaining `ADMIN` on the platform, the server returns `409 LAST_ADMIN_CANNOT_DELETE` until another admin exists.

#### Data Deleted Immediately Upon Confirmation:
- Account identity (phone, display name, profile fields) and OTP challenge history
- All active login sessions and refresh tokens
- All trusted devices and FCM push tokens
- All device-to-device transfer sessions/chunks and cloud encrypted backups
- User notifications, group/project memberships, and all local encrypted app storage

#### Shared Counterparty Data (Anonymized Instead of Deleted) & Retention Rationale:
- **Multi-party invoices, chat messages, call history, and shared HR/project records:** Retained strictly for the remaining counterparty's accounting and communication integrity, with your identity scrubbed and replaced by an anonymous non-linkable placeholder (`Deleted User`).
- **AI usage ledger counters:** Retains aggregate token/cost counters with zero prompt text and zero personal identifiers for billing/abuse-cap accounting.

### 5. Contact
- **Email:** `marco0000110@gmail.com`
