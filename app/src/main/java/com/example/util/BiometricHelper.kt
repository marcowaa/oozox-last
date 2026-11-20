package com.example.util

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED
}

object BiometricHelper {

    /**
     * Checks if Biometric authentication (Fingerprint, Face, Iris) is supported and enrolled on the device.
     */
    fun checkBiometricStatus(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SECURITY_UPDATE_REQUIRED
            else -> BiometricStatus.UNSUPPORTED
        }
    }

    fun isBiometricAvailable(context: Context): Boolean {
        return checkBiometricStatus(context) == BiometricStatus.AVAILABLE
    }

    /**
     * Checks if the device has a secure lock screen (Pattern, PIN, Password, or Biometrics).
     */
    fun isDeviceCredentialAvailable(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        if (km?.isDeviceSecure == true) return true
        val bm = BiometricManager.from(context)
        val auth = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return bm.canAuthenticate(auth) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Traverses the context hierarchy to locate a FragmentActivity.
     */
    fun findFragmentActivity(context: Context): FragmentActivity? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is FragmentActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun getStatusDescriptionAr(status: BiometricStatus): String {
        return when (status) {
            BiometricStatus.AVAILABLE -> "مستشعر البصمة والتعرف على الوجه جاهز ونشط"
            BiometricStatus.NOT_ENROLLED -> "المستشعر متوفر (لم يتم تسجيل بصمة بالجهاز)"
            BiometricStatus.NO_HARDWARE -> "الجهاز لا يحتوي على مستشعر بيومتري"
            BiometricStatus.HARDWARE_UNAVAILABLE -> "مستشعر البصمة غير متاح حالياً"
            BiometricStatus.SECURITY_UPDATE_REQUIRED -> "يتطلب المستشعر تحديث أمني للنظام"
            BiometricStatus.UNSUPPORTED -> "المصادقة البيومترية غير مدعومة"
        }
    }

    /**
     * Launches the native Android BiometricPrompt for Face/Fingerprint verification.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "المصادقة البيومترية لتأمين البيانات المالية",
        subtitle: String = "بصمة الإصبع أو التعرف على الوجه",
        description: String = "يرجى تأكيد هويتك لحماية محفظتك والصفقات والبيانات المالية الحساسة",
        negativeButtonText: String = "استخدام رمز PIN",
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setConfirmationRequired(false)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Launches Android's native Biometric + Device Lock Screen (Pattern / PIN / Password) prompt.
     * Note: setNegativeButtonText must NOT be set when DEVICE_CREDENTIAL is included.
     */
    fun authenticateWithDeviceOrBiometric(
        activity: FragmentActivity,
        title: String = "تأكيد الهوية",
        subtitle: String = "البصمة أو قفل الجهاز (نمط / PIN / كلمة مرور)",
        description: String = "يرجى التحقق بهوية جهازك للمتابعة بأمان",
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val allowed = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(allowed)
            .setConfirmationRequired(false)
            .build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}
