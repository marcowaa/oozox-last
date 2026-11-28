# استراتيجية وخارطة طريق تطوير وتشغيل تطبيق OOZOX على Android و iOS

> **وثيقة التطوير والتشغيل الشامل (Cross-Platform Architecture & Deployment Roadmap)**  
> **اسم المشروع:** OOZOX Platform  
> **الإصدار المستهدف:** 1.0.0 Multi-Platform (Android & iOS)

---

## 1. نظرة عامة على الوضع الحالي للمشروع (Current Architecture Analysis)

يحتوي مستودع مشروع **OOZOX** الحالي على البنية التالية:
1. **تطبيق Android الأصلي (Native Jetpack Compose):**
   * كود كامل ومبني بلغة Kotlin و Jetpack Compose داخل مجلد `/app`.
   * يعتمد على ميزات نظام أندرويد (Material 3، Room DB، Android Biometric Authentication، WorkManager، Foreground Services للمكالمات).
2. **الواجهة الخلفية (Backend Services):**
   * خادم Node.js / Express وخادم إشارات WebRTC Signaling داخل مجلدات `/backend` و `/backend-signaling`.
3. **نماذج العملاء القديمة المؤرشفة (Legacy Archived Prototypes):**
   * تم أرشفة نماذج `/mobile-client` (React Native) و`/mobile_app` (Flutter) غير المستخدمة في `archive/legacy-mobile-clients.tar.gz` وإزالتها من الشجرة النشطة لصالح تطبيق Android الأصلي (`/app`).

لضمان تشغيل التطبيق بأعلى كفاءة وسلاسة على **كلا النظامين (Android و iOS)** مع الحفاظ على الهوية البصرية ونفس الأداء العالي، يوضح هذا الدليل المسارات الفنية، متطلبات المطور، إعدادات Xcode، وخطوات التشغيل خطوة بخطوة.

---

## 2. المسارات التقنية المقترحة للتشغيل على النظامين (Engineering Options)

لدينا مساران استراتيجيان لتشغيل OOZOX على Android و iOS:

### المسار الأول: Kotlin Multiplatform (KMP + Compose Multiplatform) [الموصى به تقنياً]
* **الميزة:** الحفاظ على 85% من كود الـ Android Kotlin الحالي ومشاركة المنطق (Business Logic)، طبقة البيانات (Ktor + SqlDelight)، والواجهات (Compose Multiplatform) مع تشغيلها مباشرة على iOS عبر Objective-C/Swift Framework.
* **الأداء:** Native 100% بدون أي فاقد أداء (Zero Overhead).

### المسار الثاني: تفعيل مشروع React Native / Expo أو Flutter المتكامل
* **الميزة:** استخدام شاشات `mobile-client` المكتوبة بالفعل، وتجهيز مجلد `ios/` ومجلد `android/` قياسيين.
* **الأداء:** سرعة عالية جداً في النشر والتحديثات الفورية (OTA Updates).

---

## 3. متطلبات المطور وأجهزة العمل (Developer Prerequisites)

### 3.1 متطلبات بيئة نظام Apple (لنظام iOS):
| المتطلب | المواصفات الموصى بها | الأهمية والهدف |
| :--- | :--- | :--- |
| **جهاز حاسوب** | Mac (MacBook Air/Pro أو Mac Mini أو Mac Studio) بمعالج Apple Silicon (M1/M2/M3/M4) | **إلزامي:** نظام macOS لا يمكن بناء أو تصدير تطبيقات iOS إلا من خلاله رسمياً. |
| **نظام التشغيل** | macOS Sonoma (14.x) أو macOS Sequoia (15.x) | لتوافق أحدث أدوات Apple SDK. |
| **Xcode** | Xcode 15.4 أو Xcode 16+ | بيئة التطوير الرسمية من Apple للبناء والتجميع والمحاكاة. |
| **Command Line Tools** | مثبتة عبر أمر `xcode-select --install` | أدوات المترجم الطرفي و Git. |
| **CocoaPods / SPM** | CocoaPods (`brew install cocoapods`) أو Swift Package Manager | لإدارة مكتبات وملفات الاعتماد لنظام iOS (Podfiles). |
| **حساب Apple Developer** | Apple Developer Program ($99 سنوياً) | لتوقيع التطبيق بشهادات رسمية واختباره عبر TestFlight ورفعه لـ App Store. |

### 3.2 متطلبات بيئة نظام Android:
| المتطلب | المواصفات |
| :--- | :--- |
| **Android Studio** | Ladybug / Koala أو أحدث. |
| **JDK** | OpenJDK 17 أو OpenJDK 21. |
| **Android SDK** | SDK Platform 34 و 35 مع Android SDK Build-Tools. |
| **حساب Google Play Console** | حساب مطور معتمد ($25 لمرة واحدة) للنشر على متجر Google Play. |

---

## 4. إعدادات وتكوين Xcode بالتفصيل (Xcode Setup & Configuration)

عند فتح مجلد مشروع الـ iOS في Xcode (`.xcworkspace` أو مشروع KMP المشتق)، يجب تطبيق الإعدادات التالية بدقة:

### 4.1 إعدادات الهوية والتوقيع (Signing & Capabilities)
1. افتح المشروع في Xcode:
   ```bash
   open ios/OOZOX.xcworkspace
   ```
2. اختر Target الخاص بالتطبيق: `OOZOX`.
3. انتقل إلى تبويب **Signing & Capabilities**:
   * قم بتفعيل خيار **Automatically manage signing**.
   * اختر الـ **Team** الخاص بحسابك في Apple Developer.
   * حدد معرف الحزمة الموحد (Bundle Identifier):  
     `com.oozox.app` (أو المعرف المعتمد لديك).
4. أضف الصلاحيات الضرورية (Capabilities):
   * **Push Notifications:** لاستقبال إشعارات الرسائل والمكالمات في الخلفية.
   * **Background Modes:** فعّل الخيارات التالية:
     * *Audio, AirPlay, and Picture in Picture* (للمكالمات الصوتية والمرئية).
     * *Voice over IP (VoIP / PushKit)* (لاستقبال رنين المكالمات أثناء إغلاق التطبيق).
     * *Remote notifications*.
     * *Background fetch*.

### 4.2 أذونات ملف الخصوصية (Info.plist)
يفرض نظام iOS قيوداً صارمة على الخصوصية، ويجب إضافة النصوص التوضيحية لسبب طلب كل إذن باللغتين الإنجليزية والعربية:

```xml
<!-- كاميرا لمكالمات الفيديو وصور الفواتير -->
<key>NSCameraUsageDescription</key>
<string>يحتاج تطبيق OOZOX إلى الوصول للكاميرا لإجراء مكالمات الفيديو ومسح المستندات والفواتير.</string>

<!-- الميكروفون للمكالمات الصوتية والرسائل الصوتية والذكاء الاصطناعي -->
<key>NSMicrophoneUsageDescription</key>
<string>يحتاج OOZOX إلى استخدام الميكروفون لإجراء المكالمات وإرسال التسجيلات الصوتية والتفاعل الصوتي.</string>

<!-- الموقع الجغرافي لتحديد مكان المعاملات ومسارات العمل -->
<key>NSLocationWhenInUseUsageDescription</key>
<string>يستخدم OOZOX موقعك الجغرافي لتسجيل مواقع الفواتير وتوفير الخدمات القريبة.</string>

<!-- التعرف على الوجه / البصمة (Face ID / Touch ID) -->
<key>NSFaceIDUsageDescription</key>
<string>يستخدم OOZOX تقنية Face ID لتأمين حسابك ومعاملاتك المالية بدلاً من كتابة كلمة المرور.</string>

<!-- حفظ الصور في ألبوم الصور -->
<key>NSPhotoLibraryAddUsageDescription</key>
<string>يتطلب التطبيق هذا الإذن لحفظ تقارير الفواتير والصور في ألبوم هاتفك.</string>
```

### 4.3 إعدادات البناء (Build Settings):
* **iOS Deployment Target:** اضبط الحد الأدنى إلى **iOS 15.0** أو **iOS 16.0** لضمان دعم 96%+ من أجهزة iPhone الحديثة مع استقرار واجهات ومكتبات WebRTC و Swift الحديثة.
* **Architectures:** اضبط القيمة على `Standard Architectures (arm64)`.
* **Bitcode:** قم بتعطيله (`Enable Bitcode = NO`) لأن Apple ألغت دعمه رسمياً في إصدارات Xcode الأخيرة.

---

## 5. توافق وتجهيز ميزات أندرويد الحالية للعمل على iOS

لكي يعمل كل ما تم بناؤه في أندرويد بنفس القوة على iOS، يتم تطبيق البدائل المعمارية التالية:

| الميزة في أندرويد (الحالية) | المقابل التقني في iOS | آلية التنفيذ الموصى بها |
| :--- | :--- | :--- |
| **Android BiometricPrompt** | **LocalAuthentication (Face ID / Touch ID)** | استخدام واجهة `LAContext` مع تقييم `deviceOwnerAuthenticationWithBiometrics`. |
| **Foreground Service للمكالمات** | **Apple CallKit Framework** | استخدام CallKit لتظهر المكالمات كواجهة اتصال أصلية لشاشة قفل الآيفون (Native Call Screen). |
| **WebRTC Audio/Video** | **WebRTC iOS SDK (M114+)** | إعداد `RTCPeerConnectionFactory` مع تشغيل مخارج الصوت عبر `AVAudioSession` بفئة `playAndRecord`. |
| **Room Database (SQLite)** | **GRDB.swift / CoreData / SQLDelight** | تشفير قاعدة البيانات باستخدام SQLCipher لتأمين الدفاتر المحاسبية. |
| **Firebase Cloud Messaging** | **APNs (Apple Push Notification service)** | ربط مفتاح `AuthKey_XXXX.p8` في كونسول Firebase لإرسال إشعارات فورية موثوقة لأجهزة iOS. |

---

## 6. خطوات التشغيل والاختبار خطوة بخطوة (Step-by-Step Execution Guide)

### الخطوة 1: استنساخ المستودع وتثبيت بيئة الاعتماديات
```bash
# 1. الدخول إلى مجلد المشروع
cd oozox-project

# 2. في حال استخدام واجهة العميل المشتركة (Mobile Client):
cd mobile-client
npm install # أو pnpm install

# 3. تثبيت Pods لنظام iOS (على جهاز Mac فقط):
cd ios
pod install --repo-update
cd ..
```

### الخطوة 2: تشغيل التطبيق على محاكي أندرويد (Android Emulator)
* من خلال Android Studio: افتح مجلد `/app` واضغط **Run (Shift + F10)**.
* أو من الطرفية:
  ```bash
  gradle :app:installDebug
  ```

### الخطوة 3: تشغيل التطبيق على محاكي آيفون (iOS Simulator)
```bash
# تشغيل أداة المحاكي المباشرة
npx react-native run-ios --simulator="iPhone 15 Pro"
# أو فتح المشروع داخل Xcode واختيار أيقونة التشغيل (Cmd + R) بعد تحديد محاكي iPhone
```

### الخطوة 4: التشغيل والاختبار على هاتف آيفون حقيقي (Physical iPhone Device)
1. قم بتوصيل هاتف الـ iPhone بالماك بواسطة كابل USB أو عبر نفس شبكة الـ Wi-Fi.
2. فعّل **Developer Mode** على الآيفون من:  
   *الإعدادات (Settings) -> الخصوصية والأمن (Privacy & Security) -> نمط المطور (Developer Mode) -> تفعيل وإعادة تشغيل الهاتف*.
3. داخل Xcode: اختر هاتفك الفعلي من قائمة الأجهزة في الشريط العلوي.
4. اضغط على زر **Play / Run**. سيتم تجميع التطبيق وتثبيته مباشرة على الهاتف.

---

## 7. خارطة طريق الإطلاق للمتاجر (Production Release & CI/CD Roadmap)

### المرحلة الأولى: إعداد البيئة السحابية والشهادات
- [ ] إنشاء تطبيق على **Google Play Console** وتوليد `upload-keystore.jks`.
- [ ] إنشاء App ID في **Apple Developer Portal** وتوليد شهادة Distribution Profile.
- [ ] ضبط مفاتيح `GoogleService-Info.plist` لنظام iOS و `google-services.json` لنظام Android.

### المرحلة الثانية: الاختبارات التجريبية المغلقة
- [ ] نشر نسخ أندرويد عبر **Google Play Internal Testing Track**.
- [ ] نشر نسخ iOS للمختبرين والمستخدمين التجريبيين عبر **Apple TestFlight** (حتى 10,000 مختبر خارجي).

### المرحلة الثالثة: إطلاق النسخة النهائية للجمهور (Global Production)
- [ ] تجهيز لقطات الشاشة بجميع المقاسات (iPhone 6.7" و 6.5" و 5.5"، و iPad).
- [ ] اعتماد سياسة الخصوصية وشروط الاستخدام (`PRIVACY_POLICY.md` المتوفرة بالمشروع).
- [ ] إرسال التطبيق للمراجعة النهائية في **App Store Review** و **Google Play Review**.

---

**ملاحظة:** ملف التكوين هذا جاهز ليكون المرجع الفني الأساسي لفريق التطوير والمهندسين لضمان أعلى معايير الجودة والأداء على كلا النظامين دون أي تعارضات أو تأخير في التحديثات المستقبلية.
