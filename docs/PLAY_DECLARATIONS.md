# إقرارات وسياسات Google Play Console — تطبيق OOZOX

**معرّف التطبيق (`applicationId`):** `com.aistudio.goldchat.app.mirhxk` *(يُستبدل بمعرّف المالك النهائي قبل أول رفع على Play Console كما هو موثق في `docs/CLOUD_API_DEPLOYMENT_GUIDE.md`)*  
**مستوى SDK المستهدف (`targetSdk`):** `36` (Android 16)  
**جهة الاتصال:** `marco0000110@gmail.com`

---

## 1. مطابقة أذونات وخدمات المقدمة مع `app/src/main/AndroidManifest.xml`

| الإذن / نوع الخدمة في Manifest | المكوّن الفعلي في التطبيق | الغرض التشغيلي وحالة الامتثال |
|---|---|---|
| `RECORD_AUDIO`, `CAMERA`, `MODIFY_AUDIO_SETTINGS` + `FOREGROUND_SERVICE_MICROPHONE` / `FOREGROUND_SERVICE_CAMERA` (`CallForegroundService`, `foregroundServiceType="microphone\|camera"`) | `CallForegroundService`, `WebRtcAudioCallScreen` | مكالمات الصوت والفيديو المباشرة عبر WebRTC P2P مع إشعار `NotificationCompat.CallStyle` وأزرار كتم/إنهاء؛ لا يُطلب الإذن إلا وقت تشغيل المكالمة. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_REMOTE_MESSAGING` (`FloatingBubbleService`, `foregroundServiceType="remoteMessaging"`) | `FloatingBubbleService` | عرض وإدارة فقاعة المحادثات العائمة للرد الفوري خارج التطبيق عند تفعيلها اختيارياً من المستخدم. |
| `USE_FULL_SCREEN_INTENT`, `VIBRATE`, `POST_NOTIFICATIONS` | `IncomingCallActivity`, `OozoxFirebaseMessagingService` | عرض شاشة المكالمة الواردة فوق شاشة القفل مع إشعار `NotificationCompat.CallStyle` ورنين/اهتزاز، وإشعارات الدفع الفورية (`POST_NOTIFICATIONS` يُطلب على Android 13+). |
| `SYSTEM_ALERT_WINDOW` | `FloatingBubbleService` | فتح نافذة المكالمة المصغرة وفقاعة الدردشة العائمة فوق التطبيقات الأخرى بعد موافقة المستخدم الصريحة من شاشة إعدادات النظام (`Settings.ACTION_MANAGE_OVERLAY_PERMISSION`). |
| `READ_CONTACTS` | `NewChatDialog`, `MainViewModel` | مطابقة جهات الاتصال لبدء المحادثات التجارية داخل OOZOX عند فتح قائمة جهات الاتصال فقط. |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | `BusinessSetupTab`, `BusinessGeofenceGroupTab`, `BusinessEmployeeLoginDialog`, `VisualAttendanceLogger`, `GeofenceMonitoringWorker` | التحقق من النطاق الجغرافي لحضور الموظفين (`Geofencing`) ومشاركة موقع التسليم عند طلب المستخدم الصريح. |
| `USE_BIOMETRIC` | قفل التطبيق والمصادقة على الجهاز الموثوق | تأمين فتح التطبيق عبر البصمة/قفل النظام المرتبط بالجهاز الموثوق (`TrustedDevice`). |
| `INTERNET`, `ACCESS_NETWORK_STATE` | `OozoxApiClient`, `WebRtcSignalingClient` | الاتصال الآمن بالخادم (`https://api.oozox.com`) وخادم الإشارات (`wss://signaling.oozox.com/ws`). |
| منتقي الصور والفيديو (بدون أذونات تخزين) | `ActivityResultContracts.PickVisualMedia` | لا يطلب التطبيق أذونات `READ_MEDIA_IMAGES` أو `READ_MEDIA_VIDEO` أو `READ_EXTERNAL_STORAGE` أو `RECEIVE_BOOT_COMPLETED`. |

---

## 2. توافق سلوك `targetSdk 36` (Android 16)
- **Edge-to-Edge (`enableEdgeToEdge()`):** مفعّل في `MainActivity` و `IncomingCallActivity` و `Theme.kt` مع التعامل مع `WindowInsets` عبر `Scaffold` و `safeDrawing`/`imePadding`.
- **Predictive Back (`android:enableOnBackInvokedCallback="true"`):** مفعّل في `AndroidManifest.xml` مع `BackHandler` داخل الشاشات والقوائم الجانبية (`MainAppNavigation`, `ChatListScreen`, `GoldScreen`, `CompanyAccountingHubScreen`, `AdminScreen`, `ProjectsScreen`, `ProjectDetailScreen`, `WhatsAppTelegramOtpView`, `IncomingCallActivity`).
- **Foreground Service Types (`Android 14–16`):** كل خدمة مقدمة في `AndroidManifest.xml` تحمل نوعاً محدداً (`CallForegroundService` -> `microphone|camera`, `FloatingBubbleService` -> `remoteMessaging`).
- **Full-Screen Intent (`Android 14–16`):** مقيّد حصرياً لإشعارات المكالمات الواردة المرتبطة بـ `NotificationCompat.CallStyle` على `IncomingCallActivity`.

---

## 3. إقرار أمان البيانات وحذف الحساب (Google Play Data Safety)

| نوع البيانات | هل يُجمع؟ | الغرض | التشفير والمشاركة |
|---|---|---|---|
| **رقم الهاتف** | نعم (إلزامي للدخول) | المصادقة عبر OTP، ربط الأجهزة الموثوقة، واكتشاف أطراف المحادثة. | مشفّر أثناء النقل (TLS 1.3) ومشفّر في قاعدة البيانات (`AES-256-GCM`)؛ لا يُشارك مع معلنين. |
| **الاسم وبيانات ملف العمل** | نعم | عرض الهوية التجارية في المحادثات والفواتير. | مشفّر أثناء النقل؛ يتحكم المستخدم في تعديله وحذفه. |
| **المحتوى المالي والنسخ الاحتياطي** | نعم | الفواتير وإدارة المشاريع والنسخ الاحتياطي المشفر. | تشفير محلي و E2EE للنسخ الاحتياطية بكلمة مرور المستخدم. |
| **استفسارات Gemini AI** | تعالج لحظياً على الخادم | الرد على استفسارات المستخدم وتحليل الصور/المستندات. | تمر عبر الخادم بحصص يومية/شهرية وسقف تكلفة شهري؛ **لا تُسجل نصوص المطالبات في Logs**. |
| **معرّف الجهاز ورمز FCM** | نعم | حماية الجلسات (`Device Trust`) وتوصيل الإشعارات الفورية. | مرتبط بالجلسة ويُحذف فوراً عند تسجيل الخروج أو حذف الحساب. |

- **الحذف من داخل التطبيق:** `الملف الشخصي ← الإعدادات ← حذف الحساب نهائياً` (محمي بتحقق OTP فوري عبر `DELETE /api/auth/me`).
- **رابط الويب العام لحذف الحساب:** `https://oozox.com/delete-account` (صفحة ويب عامة في `web-portal/` مرتبطة بـ `POST /api/auth/otp/request` و `DELETE /api/auth/me`).

---

## 4. متطلبات ما قبل الرفع النهائي على Google Play
1. تغيير `applicationId` من القيمة الافتراضية `com.aistudio.goldchat.app.mirhxk` إلى المعرّف النهائي إذا رغب المالك (قبل أول رفع AAB لأن Play Console يقفله نهائياً).
2. إضافة ملف **`app/google-services.json`** الفعلي المطابق للـ `applicationId` النهائي.
3. توفير **`release.keystore`** الحقيقي وتوقيع ملف **AAB (`./gradlew --no-daemon :app:bundleRelease`)** ونشر بصمة SHA-256 داخل `https://oozox.com/.well-known/assetlinks.json`.
