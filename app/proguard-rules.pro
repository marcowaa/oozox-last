# ============================================================================
# OOZOX Enterprise R8 / ProGuard Configuration
# Optimized for: Aggressive Code Shrinking, Resource Stripping & Bytecode Obfuscation
# ============================================================================

# ----------------------------------------------------------------------------
# 1. General Optimization & Shrinking Directives
# ----------------------------------------------------------------------------
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-allowaccessmodification
-repackageclasses 'com.example.core.obf'

# Preserve line numbers and source file attributes for crash de-obfuscation (Crashlytics/Play Console)
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Strip all debug logging and sensitive console prints from Release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

-assumenosideeffects class kotlin.io.ConsoleKt {
    public static void println(...);
    public static void print(...);
}

# ----------------------------------------------------------------------------
# 2. Kotlin & Kotlinx Coroutines Optimization
# ----------------------------------------------------------------------------
-dontwarn kotlin.**
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Coroutines internal stacktrace recovery and service loader preservation
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# ----------------------------------------------------------------------------
# 3. Kotlinx Serialization Rules
# ----------------------------------------------------------------------------
# Keep all Serializable classes and their generated companion serializers
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembers class * {
    companion object;
}
-keepclassmembers class * extends kotlinx.serialization.KSerializer {
    <fields>;
    <methods>;
}
-keepclassmembers class **$$serializer {
    <fields>;
    <methods>;
}
-keepclasseswithmembers class * {
    public static *** Companion;
}
-keep class com.example.data.model.** { *; }

# ----------------------------------------------------------------------------
# 4. AndroidX Room Database & SQLite Rules
# ----------------------------------------------------------------------------
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
    public <methods>;
}
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ----------------------------------------------------------------------------
# 5. Jetpack Compose Rules
# ----------------------------------------------------------------------------
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keepclassmembers class androidx.compose.runtime.Recomposer {
    <fields>;
    <methods>;
}
-dontwarn androidx.compose.**

# ----------------------------------------------------------------------------
# 6. Security, Cryptography & Hardware KeyStore Preservation
# ----------------------------------------------------------------------------
# Prevent R8 from inlining or stripping cryptographic engines and keystore providers
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keep class com.example.util.RoomLocalAES256Engine** { *; }
-keep class com.example.util.BiometricHelper** { *; }

# ----------------------------------------------------------------------------
# 7. WebRTC & Real-Time Media SDK Rules
# ----------------------------------------------------------------------------
-keep class org.webrtc.** { *; }
-keepclassmembers class org.webrtc.** {
    <fields>;
    <methods>;
}
-keepclassmembers class * implements org.webrtc.PeerConnection$Observer {
    <methods>;
}
-keep class com.example.util.WebRtcSignalingManager** { *; }

# ----------------------------------------------------------------------------
# 8. Third-Party Libraries & Dead-Code Elimination
# ----------------------------------------------------------------------------

# Coil Image Loader (strip unused decoders/fetchers)
-keep class coil.** { *; }
-dontwarn coil.**

# OkHttp & Ktor Network Stacks (strip unused engines)
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ----------------------------------------------------------------------------
# 9. App Entry Points & System Activities
# ----------------------------------------------------------------------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
