package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.example.data.model.HiddenProfileEntity
import com.example.data.model.RepeatUserSummary
import com.example.data.model.UserType
import com.example.data.repository.AuthOutcome
import com.example.data.repository.OtpRequestOutcome
import com.example.data.repository.OtpVerifyOutcome
import com.example.ui.theme.*
import com.example.util.BiometricHelper
import com.example.util.BiometricStatus
import com.example.util.DeviceTrustManager
import com.example.util.LocalAppLanguage
import com.example.util.OfflineSyncManager
import com.example.util.SecurityValidator
import com.example.util.Translations
import com.example.util.tr
import com.example.util.trFmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * UserAppPortal - Server-Backed Authentication, 30-Day Device Trust, and Biometric Access Gateway.
 * - Mandatory server OTP on registration, phone login, untrusted devices, and recovery.
 * - Biometric / Device Lock / PIN quick access allowed ONLY when server-issued 30-day trust token is valid.
 * - Offline biometric/PIN unlocks cached local Room workspace ONLY (no fake online session).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAppPortal(
    repeatUser: RepeatUserSummary?,
    allHiddenProfiles: List<HiddenProfileEntity> = emptyList(),
    onTrustedOnlineLogin: suspend (phone: String) -> AuthOutcome,
    onOfflineCacheUnlock: suspend (phone: String) -> Boolean,
    onLoginWithPassword: suspend (emailOrPhone: String, password: String, channel: String) -> AuthOutcome,
    onRegisterAccount: suspend (name: String, email: String, phone: String, password: String, role: String, channel: String) -> AuthOutcome,
    onRequestOtp: suspend (purpose: String, channel: String, phone: String?, preAuthToken: String?) -> OtpRequestOutcome,
    onVerifyOtp: suspend (challengeId: String, code: String, phone: String?, preAuthToken: String?, trustDevice: Boolean) -> OtpVerifyOutcome,
    onRequestRecoveryOtp: suspend (phone: String, channel: String) -> OtpRequestOutcome,
    onConfirmRecovery: suspend (challengeId: String, code: String, newPassword: String) -> AuthOutcome,
    onAuthenticatedComplete: () -> Unit,
    onSwitchProfile: ((HiddenProfileEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val coroutineScope = rememberCoroutineScope()
    val currentLang = LocalAppLanguage.current

    val isOnline by OfflineSyncManager.isNetworkOnline.collectAsState()
    val isTrustedDevice = remember(repeatUser?.phone) {
        repeatUser?.phone != null && DeviceTrustManager.isDeviceTrustedForPhone(context, repeatUser.phone)
    }

    // Quick Biometric/PIN access allowed ONLY if server-issued 30-day Device Trust token is valid
    var isRepeatUserMode by remember(repeatUser, isTrustedDevice) {
        mutableStateOf(repeatUser != null && isTrustedDevice)
    }

    // Auth Mode Tabs: 0 = Phone OTP, 1 = Email/Phone + Password, 2 = Register
    var selectedAuthTab by remember { mutableIntStateOf(0) }
    var authStep by remember { mutableIntStateOf(0) } // 0 = Form, 1 = Server OTP Verification

    // Shared Country + Phone inputs
    var selectedCountry by remember { mutableStateOf(SecurityValidator.SUPPORTED_COUNTRIES.first()) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var countrySearchQuery by remember { mutableStateOf("") }
    var rawPhone by remember { mutableStateOf("") }
    var selectedDeliveryChannel by remember { mutableStateOf(OtpDeliveryChannel.TELEGRAM) }

    // Password Login inputs
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Register inputs
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf("USER") } // USER or MERCHANT
    var showRegPassword by remember { mutableStateOf(false) }

    // Active OTP Challenge state (returned from backend)
    var otpChallengeId by remember { mutableStateOf("") }
    var otpPhoneDisplay by remember { mutableStateOf("") }
    var otpPreAuthToken by remember { mutableStateOf<String?>(null) }
    var otpPurpose by remember { mutableStateOf("LOGIN") }
    var otpCooldownSeconds by remember { mutableIntStateOf(60) }
    var otpFallbackNotice by remember { mutableStateOf<String?>(null) }
    var trustThisDeviceChecked by remember { mutableStateOf(true) }

    // UI Status & Errors
    var isProcessingNetwork by remember { mutableStateOf(false) }
    var specificErrorMessage by remember { mutableStateOf<String?>(null) }
    var networkFailureError by remember { mutableStateOf<String?>(null) }
    var detectedCarrier by remember { mutableStateOf<String?>(null) }
    var autoCorrectionNotice by remember { mutableStateOf<String?>(null) }

    // Dialog controllers
    var showPinDialog by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    val biometricStatus = remember(context) { BiometricHelper.checkBiometricStatus(context) }
    val isBiometricAvailable = biometricStatus == BiometricStatus.AVAILABLE
    val isDeviceCredentialAvailable = remember(context) { BiometricHelper.isDeviceCredentialAvailable(context) }

    val validationResult = remember(selectedCountry, rawPhone, currentLang) {
        if (rawPhone.isNotBlank()) {
            SecurityValidator.validateMobileNumber(selectedCountry, rawPhone, currentLang)
        } else null
    }

    LaunchedEffect(validationResult) {
        if (validationResult != null) {
            detectedCarrier = validationResult.carrierName
            autoCorrectionNotice = validationResult.autoCorrectionNotice
        } else {
            detectedCarrier = null
            autoCorrectionNotice = null
        }
    }

    val moveToOtpStep: (AuthOutcome.OtpRequired, String) -> Unit = { otpReq, fallbackPhone ->
        otpPurpose = otpReq.purpose.ifBlank { "LOGIN" }
        otpPreAuthToken = otpReq.preAuthToken
        otpPhoneDisplay = otpReq.maskedPhone?.takeIf { it.isNotBlank() } ?: fallbackPhone
        val ch = otpReq.challenge
        if (ch != null) {
            otpChallengeId = ch.challengeId
            selectedDeliveryChannel = OtpDeliveryChannel.fromWire(ch.activeChannel)
            otpCooldownSeconds = ch.cooldownSeconds
            otpFallbackNotice = otpReq.deliveryError?.message?.takeIf { it.isNotBlank() }
        } else {
            otpChallengeId = ""
            otpFallbackNotice = otpReq.deliveryError?.message
        }
        isRepeatUserMode = false
        authStep = 1
    }

    val handleRepeatUnlockVerified: () -> Unit = {
        val targetPhone = repeatUser?.phone.orEmpty()
        if (targetPhone.isNotBlank()) {
            coroutineScope.launch {
                isProcessingNetwork = true
                specificErrorMessage = null
                networkFailureError = null

                val hasInternet = isOnline && OfflineSyncManager.isNetworkAvailable(context)
                if (hasInternet) {
                    when (val res = onTrustedOnlineLogin(targetPhone)) {
                        is AuthOutcome.Authenticated -> {
                            isProcessingNetwork = false
                            onAuthenticatedComplete()
                        }
                        is AuthOutcome.OtpRequired -> {
                            isProcessingNetwork = false
                            moveToOtpStep(res, targetPhone)
                        }
                        is AuthOutcome.Error -> {
                            isProcessingNetwork = false
                            if (res.code == "NETWORK_ERROR") {
                                // Server unreachable even though network interface reported online -> allow explicit offline cache option
                                networkFailureError = "تعذر الوصول للخادم: يمكنك الدخول لوضع الأوفلاين للبيانات المحفوظة محلياً فقط."
                            } else {
                                specificErrorMessage = res.message
                            }
                        }
                    }
                } else {
                    val unlocked = onOfflineCacheUnlock(targetPhone)
                    isProcessingNetwork = false
                    if (unlocked) {
                        onAuthenticatedComplete()
                    } else {
                        specificErrorMessage = "انتهت صلاحية الثقة المحلية للجهاز (30 يوماً) — يلزم الاتصال بالإنترنت وإعادة التحقق عبر OTP."
                    }
                }
            }
        }
    }

    val triggerBiometricAuthentication: (onSuccessAction: () -> Unit) -> Unit = { successCallback ->
        if (activity != null && isBiometricAvailable) {
            specificErrorMessage = null
            networkFailureError = null
            val welcomeName = repeatUser?.name ?: Translations.get("portal.welcome_fallback_name", currentLang)
            BiometricHelper.authenticate(
                activity = activity,
                title = Translations.get("portal.bio_title", currentLang),
                subtitle = Translations.get("portal.bio_sub", currentLang),
                description = Translations.format("portal.bio_desc_fmt", currentLang, "name" to welcomeName),
                negativeButtonText = if (repeatUser?.pin != null) Translations.get("portal.use_pin_btn", currentLang) else Translations.get("common.cancel", currentLang),
                onSuccess = { successCallback() },
                onError = { errorCode, errString ->
                    if (errorCode == androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON && repeatUser?.pin != null) {
                        showPinDialog = true
                    } else if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        specificErrorMessage = errString.toString()
                    }
                },
                onFailed = {
                    specificErrorMessage = Translations.get("portal.bio_failed", currentLang)
                }
            )
        } else if (repeatUser?.pin != null) {
            showPinDialog = true
        }
    }

    val triggerDeviceLockAuthentication: (onSuccessAction: () -> Unit) -> Unit = { successCallback ->
        if (activity != null && isDeviceCredentialAvailable) {
            specificErrorMessage = null
            networkFailureError = null
            val welcomeName = repeatUser?.name ?: Translations.get("portal.welcome_fallback_name", currentLang)
            BiometricHelper.authenticateWithDeviceOrBiometric(
                activity = activity,
                title = Translations.get("portal.dev_lock_title", currentLang),
                subtitle = Translations.get("portal.dev_lock_sub", currentLang),
                description = Translations.format("portal.dev_lock_desc_fmt", currentLang, "name" to welcomeName),
                onSuccess = { successCallback() },
                onError = { errorCode, errString ->
                    if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        specificErrorMessage = errString.toString()
                    }
                },
                onFailed = {
                    specificErrorMessage = Translations.get("portal.dev_lock_failed", currentLang)
                }
            )
        } else if (repeatUser?.pin != null) {
            showPinDialog = true
        }
    }

    // Auto-prompt biometrics once for server-trusted repeat user
    LaunchedEffect(repeatUser, isTrustedDevice, isBiometricAvailable) {
        if (repeatUser != null && isTrustedDevice && isBiometricAvailable && activity != null && isRepeatUserMode) {
            delay(350L)
            triggerBiometricAuthentication { handleRepeatUnlockVerified() }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_portal")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScalePortal"
    )

    val dispatchPhoneOtp: () -> Unit = {
        val res = SecurityValidator.validateMobileNumber(selectedCountry, rawPhone, currentLang)
        if (!res.isValid) {
            specificErrorMessage = res.specificError ?: Translations.get("portal.invalid_phone", currentLang)
            networkFailureError = null
        } else {
            val fullPhone = res.fullFormattedPhone.ifBlank { "${selectedCountry.code}${rawPhone.filter { it.isDigit() }}" }
            specificErrorMessage = null
            networkFailureError = null
            isProcessingNetwork = true

            coroutineScope.launch {
                when (val otpRes = onRequestOtp("LOGIN", selectedDeliveryChannel.name, fullPhone, null)) {
                    is OtpRequestOutcome.Sent -> {
                        isProcessingNetwork = false
                        otpPurpose = "LOGIN"
                        otpPreAuthToken = null
                        otpChallengeId = otpRes.data.challengeId
                        otpPhoneDisplay = fullPhone
                        selectedDeliveryChannel = OtpDeliveryChannel.fromWire(otpRes.data.activeChannel)
                        otpCooldownSeconds = otpRes.data.cooldownSeconds
                        otpFallbackNotice = if (otpRes.data.attemptedChannels.distinct().size > 1) {
                            Translations.format(
                                "otp.fallback_notice_fmt",
                                currentLang,
                                "channel" to selectedDeliveryChannel.localizedTitle(currentLang)
                            )
                        } else null
                        authStep = 1
                    }
                    is OtpRequestOutcome.Error -> {
                        isProcessingNetwork = false
                        if (otpRes.code == "NETWORK_ERROR") {
                            networkFailureError = otpRes.message
                        } else {
                            specificErrorMessage = otpRes.message
                        }
                    }
                }
            }
        }
    }

    val dispatchPasswordLogin: () -> Unit = {
        val idTrim = loginIdentifier.trim()
        if (idTrim.isBlank() || loginPassword.length < 6) {
            specificErrorMessage = "يرجى إدخال البريد/الهاتف وكلمة السر (6 أحرف فأكثر)."
        } else {
            specificErrorMessage = null
            networkFailureError = null
            isProcessingNetwork = true
            coroutineScope.launch {
                when (val outcome = onLoginWithPassword(idTrim, loginPassword, selectedDeliveryChannel.name)) {
                    is AuthOutcome.Authenticated -> {
                        isProcessingNetwork = false
                        onAuthenticatedComplete()
                    }
                    is AuthOutcome.OtpRequired -> {
                        isProcessingNetwork = false
                        moveToOtpStep(outcome, idTrim)
                    }
                    is AuthOutcome.Error -> {
                        isProcessingNetwork = false
                        if (outcome.code == "NETWORK_ERROR") networkFailureError = outcome.message else specificErrorMessage = outcome.message
                    }
                }
            }
        }
    }

    val dispatchRegister: () -> Unit = {
        val res = SecurityValidator.validateMobileNumber(selectedCountry, rawPhone, currentLang)
        when {
            regName.trim().length < 2 -> specificErrorMessage = "يرجى إدخال الاسم الكامل."
            !regEmail.trim().contains("@") -> specificErrorMessage = "يرجى إدخال بريد إلكتروني صحيح."
            !res.isValid -> specificErrorMessage = res.specificError ?: Translations.get("portal.invalid_phone", currentLang)
            regPassword.length < 8 -> specificErrorMessage = "كلمة السر يجب أن تكون 8 أحرف على الأقل."
            else -> {
                val fullPhone = res.fullFormattedPhone.ifBlank { "${selectedCountry.code}${rawPhone.filter { it.isDigit() }}" }
                specificErrorMessage = null
                networkFailureError = null
                isProcessingNetwork = true
                coroutineScope.launch {
                    when (val outcome = onRegisterAccount(regName.trim(), regEmail.trim(), fullPhone, regPassword, regRole, selectedDeliveryChannel.name)) {
                        is AuthOutcome.Authenticated -> {
                            isProcessingNetwork = false
                            onAuthenticatedComplete()
                        }
                        is AuthOutcome.OtpRequired -> {
                            isProcessingNetwork = false
                            moveToOtpStep(outcome, fullPhone)
                        }
                        is AuthOutcome.Error -> {
                            isProcessingNetwork = false
                            if (outcome.code == "NETWORK_ERROR") networkFailureError = outcome.message else specificErrorMessage = outcome.message
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("user_app_portal_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = GoldPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.size(86.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        OozoxThumbnailImage(
                            model = com.example.R.drawable.oozox_official_clean_icon_1786654687391,
                            width = 72.dp,
                            height = 72.dp,
                            shape = RoundedCornerShape(18.dp),
                            contentDescription = "OOZOX Official Icon"
                        )
                    }
                }
            }

            Text(
                text = "OOZOX",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = OozoxPlatinum,
                letterSpacing = 2.5.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = tr("portal.subtitle"),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .testTag("user_app_portal_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BiometricStatusBadge(biometricStatus = biometricStatus)

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isRepeatUserMode && repeatUser != null && isTrustedDevice) {
                        // --- TRUSTED REPEAT USER MODE (Server-Verified 30-Day Trust Token) ---
                        if (!isOnline) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusOrange.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, StatusOrange.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "وضع أوفلاين: فتح البصمة أو PIN يعرض البيانات المحلية المحفوظة فقط.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        RepeatUserPortalView(
                            repeatUser = repeatUser,
                            isBiometricAvailable = isBiometricAvailable,
                            isDeviceCredentialAvailable = isDeviceCredentialAvailable,
                            isProcessing = isProcessingNetwork,
                            pulseScale = pulseScale,
                            onBiometricClick = {
                                triggerBiometricAuthentication { handleRepeatUnlockVerified() }
                            },
                            onDeviceLockClick = {
                                triggerDeviceLockAuthentication { handleRepeatUnlockVerified() }
                            },
                            onPinClick = { showPinDialog = true },
                            onSwitchAccountClick = {
                                isRepeatUserMode = false
                                authStep = 0
                                specificErrorMessage = null
                                networkFailureError = null
                            }
                        )

                        if (!specificErrorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = specificErrorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (!networkFailureError.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val ok = onOfflineCacheUnlock(repeatUser.phone)
                                        if (ok) onAuthenticatedComplete()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StatusOrange),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_portal_offline_cache_login")
                            ) {
                                Icon(Icons.Default.OfflineBolt, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("الدخول للبيانات المحلية فقط (أوفلاين)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusOrange)
                            }
                        }

                        if (allHiddenProfiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = tr("portal.sub_profile_heading"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                allHiddenProfiles.take(3).forEach { profile ->
                                    Surface(
                                        onClick = {
                                            triggerBiometricAuthentication {
                                                onSwitchProfile?.invoke(profile)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_portal_sub_profile_${profile.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = adaptiveGoldPrimary.copy(alpha = 0.15f),
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(profile.name.take(1), fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary, fontSize = 13.sp)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    Text(profile.roleTitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Icon(Icons.Default.Fingerprint, contentDescription = tr("portal.bio_unlock_cd"), tint = adaptiveGoldPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }

                    } else if (authStep == 1) {
                        // --- STEP 2: SERVER-SIDE OTP VERIFICATION ---
                        val fullPhoneTarget = if (rawPhone.isNotBlank()) {
                            "${selectedCountry.code}${rawPhone.filter { it.isDigit() }}"
                        } else {
                            otpPhoneDisplay.ifBlank { repeatUser?.phone.orEmpty() }
                        }

                        WhatsAppTelegramOtpView(
                            phoneNumber = otpPhoneDisplay.ifBlank { fullPhoneTarget },
                            initialChannel = selectedDeliveryChannel,
                            initialCooldownSeconds = otpCooldownSeconds,
                            fallbackNotice = otpFallbackNotice,
                            onVerifyOtpCode = { code ->
                                when (val verifyRes = onVerifyOtp(
                                    otpChallengeId,
                                    code,
                                    if (otpPreAuthToken == null) fullPhoneTarget else null,
                                    otpPreAuthToken,
                                    trustThisDeviceChecked
                                )) {
                                    is OtpVerifyOutcome.Verified -> {
                                        onAuthenticatedComplete()
                                        OtpVerifyUiResult.Success
                                    }
                                    is OtpVerifyOutcome.Error -> {
                                        OtpVerifyUiResult.Error(
                                            message = verifyRes.message,
                                            remainingAttempts = verifyRes.remainingAttempts,
                                            lockRemainingSeconds = verifyRes.retryAfterSeconds
                                        )
                                    }
                                }
                            },
                            onBackClick = {
                                authStep = 0
                                specificErrorMessage = null
                                networkFailureError = null
                            },
                            onResendRequested = { channel ->
                                selectedDeliveryChannel = channel
                                when (val resendRes = onRequestOtp(
                                    otpPurpose,
                                    channel.name,
                                    if (otpPreAuthToken == null) fullPhoneTarget else null,
                                    otpPreAuthToken
                                )) {
                                    is OtpRequestOutcome.Sent -> {
                                        otpChallengeId = resendRes.data.challengeId
                                        val activeCh = OtpDeliveryChannel.fromWire(resendRes.data.activeChannel)
                                        selectedDeliveryChannel = activeCh
                                        val notice = if (resendRes.data.attemptedChannels.distinct().size > 1) {
                                            Translations.format(
                                                "otp.fallback_notice_fmt",
                                                currentLang,
                                                "channel" to activeCh.localizedTitle(currentLang)
                                            )
                                        } else null
                                        OtpResendUiResult.Sent(
                                            activeChannel = activeCh,
                                            cooldownSeconds = resendRes.data.cooldownSeconds,
                                            notice = notice
                                        )
                                    }
                                    is OtpRequestOutcome.Error -> {
                                        OtpResendUiResult.Error(
                                            message = resendRes.message,
                                            retryAfterSeconds = resendRes.retryAfterSeconds
                                        )
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { trustThisDeviceChecked = !trustThisDeviceChecked }
                                .padding(horizontal = 4.dp)
                        ) {
                            Checkbox(
                                checked = trustThisDeviceChecked,
                                onCheckedChange = { trustThisDeviceChecked = it },
                                colors = CheckboxDefaults.colors(checkedColor = adaptiveGoldPrimary)
                            )
                            Text(
                                text = "تفعيل الثقة بهذا الجهاز لمدة 30 يوماً (للدخول بالبصمة وPIN)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                    } else {
                        // --- STEP 1: PHONE OTP / PASSWORD LOGIN / REGISTER ---
                        if (repeatUser != null && !isTrustedDevice) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhonelinkLock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = tr("portal.new_device_notice"),
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        Text(
                            text = tr("portal.login_title"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Auth Mode Selector: Phone OTP | Password | New Account
                        TabRow(
                            selectedTabIndex = selectedAuthTab,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            contentColor = adaptiveGoldPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Tab(
                                selected = selectedAuthTab == 0,
                                onClick = { selectedAuthTab = 0; specificErrorMessage = null; networkFailureError = null },
                                text = { Text("الهاتف (OTP)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                                modifier = Modifier.testTag("tab_auth_phone_otp")
                            )
                            Tab(
                                selected = selectedAuthTab == 1,
                                onClick = { selectedAuthTab = 1; specificErrorMessage = null; networkFailureError = null },
                                text = { Text("كلمة المرور", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                                modifier = Modifier.testTag("tab_auth_password")
                            )
                            Tab(
                                selected = selectedAuthTab == 2,
                                onClick = { selectedAuthTab = 2; specificErrorMessage = null; networkFailureError = null },
                                text = { Text("حساب جديد", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                                modifier = Modifier.testTag("tab_auth_register")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (selectedAuthTab == 1) {
                            // --- TAB 1: EMAIL / PHONE + PASSWORD LOGIN ---
                            OutlinedTextField(
                                value = loginIdentifier,
                                onValueChange = { loginIdentifier = it; specificErrorMessage = null; networkFailureError = null },
                                label = { Text("البريد الإلكتروني أو رقم الهاتف") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = adaptiveGoldPrimary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_login_identifier")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it; specificErrorMessage = null; networkFailureError = null },
                                label = { Text("كلمة المرور") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = adaptiveGoldPrimary) },
                                trailingIcon = {
                                    IconButton(onClick = { showLoginPassword = !showLoginPassword }, modifier = Modifier.size(48.dp)) {
                                        Icon(
                                            imageVector = if (showLoginPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "إظهار/إخفاء"
                                        )
                                    }
                                },
                                visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { dispatchPasswordLogin() }),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_login_password")
                            )
                        } else {
                            // --- TAB 0 (PHONE OTP) or TAB 2 (REGISTER) ---
                            if (selectedAuthTab == 2) {
                                OutlinedTextField(
                                    value = regName,
                                    onValueChange = { regName = it; specificErrorMessage = null },
                                    label = { Text("الاسم الكامل") },
                                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = adaptiveGoldPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_reg_name")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = regEmail,
                                    onValueChange = { regEmail = it; specificErrorMessage = null },
                                    label = { Text("البريد الإلكتروني") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = adaptiveGoldPrimary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_reg_email")
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            } else {
                                Text(
                                    text = tr("portal.enter_phone_sub"),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = tr("portal.mobile_label"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        onClick = { showCountryPicker = true },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .height(54.dp)
                                            .testTag("btn_country_picker")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(text = selectedCountry.flag, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = selectedCountry.code,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = adaptiveGoldPrimary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = adaptiveGoldPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = rawPhone,
                                        onValueChange = { input ->
                                            rawPhone = input.filter { it.isDigit() || it == ' ' || it == '-' }
                                            specificErrorMessage = null
                                            networkFailureError = null
                                        },
                                        placeholder = {
                                            Text(
                                                text = tr("portal.mobile_hint"),
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.PhoneAndroid,
                                                contentDescription = null,
                                                tint = adaptiveGoldPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (rawPhone.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { rawPhone = "" },
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = tr("portal.clear_cd"),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Phone,
                                            imeAction = if (selectedAuthTab == 0) ImeAction.Done else ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { if (selectedAuthTab == 0) dispatchPhoneOtp() }
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = adaptiveGoldPrimary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .testTag("input_phone_number")
                                    )
                                }

                                if (detectedCarrier != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp, start = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = StatusGreen.copy(alpha = 0.15f),
                                            border = BorderStroke(0.5.dp, StatusGreen.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = trFmt("portal.carrier_fmt", "carrier" to detectedCarrier!!),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (autoCorrectionNotice != null) {
                                    Text(
                                        text = autoCorrectionNotice!!,
                                        fontSize = 10.5.sp,
                                        color = GoldAccent,
                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                    )
                                }
                            }

                            if (selectedAuthTab == 2) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = regPassword,
                                    onValueChange = { regPassword = it; specificErrorMessage = null },
                                    label = { Text("كلمة المرور (8 أحرف فأكثر)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = adaptiveGoldPrimary) },
                                    trailingIcon = {
                                        IconButton(onClick = { showRegPassword = !showRegPassword }, modifier = Modifier.size(48.dp)) {
                                            Icon(
                                                imageVector = if (showRegPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "إظهار/إخفاء"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_reg_password")
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = regRole == "USER",
                                        onClick = { regRole = "USER" },
                                        label = { Text("رجل أعمال / مستثمر", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = regRole == "MERCHANT",
                                        onClick = { regRole = "MERCHANT" },
                                        label = { Text("تاجر معتمد", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = tr("portal.choose_channel"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Channels ordered strictly: TELEGRAM -> WHATSAPP -> SMS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OtpDeliveryChannel.entries.forEach { channel ->
                                val isSelected = selectedDeliveryChannel == channel
                                Surface(
                                    onClick = { selectedDeliveryChannel = channel },
                                    color = if (isSelected) channel.brandColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) channel.brandColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp)
                                        .testTag("channel_button_${channel.name.lowercase()}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        ChannelBrandIcon(channel = channel, size = 30.dp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = channel.localizedTitle(currentLang),
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) channel.brandColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        if (specificErrorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = specificErrorMessage!!,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (networkFailureError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(tr("portal.network_err_title"), color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = networkFailureError!!, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                        TextButton(
                                            onClick = {
                                                when (selectedAuthTab) {
                                                    1 -> dispatchPasswordLogin()
                                                    2 -> dispatchRegister()
                                                    else -> dispatchPhoneOtp()
                                                }
                                            },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(tr("common.retry"), color = adaptiveGoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                when (selectedAuthTab) {
                                    1 -> dispatchPasswordLogin()
                                    2 -> dispatchRegister()
                                    else -> dispatchPhoneOtp()
                                }
                            },
                            enabled = !isProcessingNetwork,
                            colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_proceed_otp")
                        ) {
                            if (isProcessingNetwork) {
                                ExecutiveButtonLoadingIndicator(
                                    loadingText = tr("portal.sending_otp"),
                                    spinnerSize = 18.dp,
                                    textColor = adaptiveOnPrimary,
                                    spinnerPrimaryColor = adaptiveOnPrimary,
                                    spinnerSecondaryColor = adaptiveGoldPrimary
                                )
                            } else {
                                Text(
                                    text = when (selectedAuthTab) {
                                        1 -> "تسجيل الدخول بكلمة المرور"
                                        2 -> "إنشاء الحساب وإرسال رمز التحقق"
                                        else -> trFmt("portal.send_otp_via_fmt", "channel" to selectedDeliveryChannel.localizedTitle(currentLang))
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (repeatUser != null && isTrustedDevice) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    isRepeatUserMode = true
                                    specificErrorMessage = null
                                    networkFailureError = null
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_back_to_repeat_user")
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(trFmt("portal.back_to_prev_fmt", "name" to repeatUser.name), fontSize = 11.sp, color = adaptiveGoldPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showRecoveryDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_portal_open_recovery")
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("نسيت كلمة المرور / استرجاع الحساب عبر OTP", fontSize = 11.5.sp, color = adaptiveGoldPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tr("portal.footer_e2ee"),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showPinDialog && repeatUser != null && !repeatUser.pin.isNullOrBlank()) {
        UserPortalPinDialog(
            correctPin = repeatUser.pin,
            userName = repeatUser.name,
            onDismiss = { showPinDialog = false },
            onPinVerified = {
                showPinDialog = false
                handleRepeatUnlockVerified()
            }
        )
    }

    if (showCountryPicker) {
        val filteredCountries = remember(countrySearchQuery, currentLang) {
            if (countrySearchQuery.isBlank()) {
                SecurityValidator.SUPPORTED_COUNTRIES
            } else {
                SecurityValidator.SUPPORTED_COUNTRIES.filter {
                    it.localizedName(currentLang).contains(countrySearchQuery, ignoreCase = true) ||
                        it.countryNameAr.contains(countrySearchQuery, ignoreCase = true) ||
                        it.code.contains(countrySearchQuery)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showCountryPicker = false; countrySearchQuery = "" },
            title = {
                Text(tr("portal.country_picker_title"), color = adaptiveGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    OutlinedTextField(
                        value = countrySearchQuery,
                        onValueChange = { countrySearchQuery = it },
                        placeholder = { Text(tr("portal.country_search_hint")) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = adaptiveGoldPrimary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = adaptiveGoldPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        filteredCountries.forEach { country ->
                            val isSelected = country.code == selectedCountry.code
                            Surface(
                                color = if (isSelected) adaptiveGoldPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCountry = country
                                        showCountryPicker = false
                                        countrySearchQuery = ""
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(country.flag, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = country.localizedName(currentLang),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) adaptiveGoldPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = country.code,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = adaptiveGoldPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showCountryPicker = false; countrySearchQuery = "" },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(tr("common.close"), color = adaptiveGoldPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showRecoveryDialog) {
        val defaultRecoveryPhone = if (rawPhone.isNotBlank()) {
            "${selectedCountry.code}${rawPhone.filter { it.isDigit() }}"
        } else {
            repeatUser?.phone.orEmpty()
        }
        AccountRecoveryDialog(
            initialPhone = defaultRecoveryPhone,
            onRequestRecoveryOtp = onRequestRecoveryOtp,
            onConfirmRecovery = onConfirmRecovery,
            onDismiss = { showRecoveryDialog = false },
            onAccountRecovered = {
                showRecoveryDialog = false
                onAuthenticatedComplete()
            }
        )
    }
}

@Composable
private fun RepeatUserPortalView(
    repeatUser: RepeatUserSummary,
    isBiometricAvailable: Boolean,
    isDeviceCredentialAvailable: Boolean,
    isProcessing: Boolean,
    pulseScale: Float,
    onBiometricClick: () -> Unit,
    onDeviceLockClick: () -> Unit,
    onPinClick: () -> Unit,
    onSwitchAccountClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = tr("portal.welcome_back"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = adaptiveGoldPrimary.copy(alpha = 0.18f),
                    border = BorderStroke(1.5.dp, adaptiveGoldPrimary),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = repeatUser.name.take(1),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = adaptiveGoldPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = repeatUser.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = tr("portal.verified_cd"),
                            tint = adaptiveGoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = repeatUser.phone,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )

                    val roleLabel = when (repeatUser.type) {
                        UserType.GOLD_TRADER.name -> tr("portal.role_trader")
                        UserType.ADMIN.name -> tr("portal.role_admin")
                        else -> tr("portal.role_businessman")
                    }

                    Text(
                        text = roleLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = adaptiveGoldPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onBiometricClick,
            enabled = !isProcessing,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = adaptiveGoldPrimary,
                contentColor = adaptiveOnPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .scale(if (isBiometricAvailable && !isProcessing) pulseScale else 1f)
                .testTag("btn_portal_biometric_login")
        ) {
            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = adaptiveOnPrimary)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = tr("portal.fingerprint_cd"),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tr("portal.biometric_signin_btn"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        if (isDeviceCredentialAvailable) {
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onDeviceLockClick,
                enabled = !isProcessing,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_portal_device_lock_login")
            ) {
                Icon(Icons.Default.PhonelinkLock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("portal.device_lock_btn"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        if (!repeatUser.pin.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onPinClick,
                enabled = !isProcessing,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_portal_pin_login")
            ) {
                Icon(Icons.Default.Password, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("portal.pin_signin_btn"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onSwitchAccountClick,
            enabled = !isProcessing,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("btn_portal_switch_account")
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(tr("portal.switch_account_btn"), fontSize = 12.sp, color = OozoxTeal, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BiometricStatusBadge(biometricStatus: BiometricStatus) {
    val (statusText, statusColor, icon) = when (biometricStatus) {
        BiometricStatus.AVAILABLE -> Triple(
            tr("portal.bio_ready"),
            adaptiveStatusGreen,
            Icons.Default.Fingerprint
        )
        BiometricStatus.NOT_ENROLLED -> Triple(
            tr("portal.bio_not_enrolled"),
            adaptiveStatusOrange,
            Icons.Default.WarningAmber
        )
        BiometricStatus.NO_HARDWARE -> Triple(
            tr("portal.bio_no_hw"),
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Lock
        )
        BiometricStatus.HARDWARE_UNAVAILABLE -> Triple(
            tr("portal.bio_unavail"),
            adaptiveStatusOrange,
            Icons.Default.HourglassEmpty
        )
        else -> Triple(
            tr("portal.bio_inactive"),
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Security
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = statusColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
        modifier = Modifier.testTag("biometric_status_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = statusColor
            )
        }
    }
}

@Composable
private fun UserPortalPinDialog(
    correctPin: String,
    userName: String,
    onDismiss: () -> Unit,
    onPinVerified: () -> Unit
) {
    val context = LocalContext.current
    val currentLang = LocalAppLanguage.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableIntStateOf(DeviceTrustManager.getRemainingLockoutSeconds(context)) }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds = DeviceTrustManager.getRemainingLockoutSeconds(context)
        }
    }

    fun vibratePhone() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(60)
            }
        } catch (_: Exception) {}
    }

    fun handleKeyPress(key: String) {
        if (lockoutSeconds > 0) return
        if (enteredPin.length < 6) {
            val newPin = enteredPin + key
            enteredPin = newPin
            errorMessage = null
            vibratePhone()

            if (newPin == correctPin) {
                DeviceTrustManager.resetPinAttempts(context)
                onPinVerified()
            } else if (newPin.length >= correctPin.length.coerceAtLeast(4)) {
                val remaining = DeviceTrustManager.recordFailedPinAttempt(context)
                lockoutSeconds = DeviceTrustManager.getRemainingLockoutSeconds(context)
                errorMessage = if (lockoutSeconds > 0) {
                    Translations.format("portal.pin_lockout_fmt", currentLang, "sec" to lockoutSeconds)
                } else {
                    Translations.format("portal.pin_wrong_fmt", currentLang, "rem" to remaining)
                }
                vibratePhone()
                enteredPin = ""
            }
        }
    }

    fun handleDelete() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
            vibratePhone()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides currentLang.direction,
            LocalAppLanguage provides currentLang
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable { onDismiss() }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(tr("common.cancel"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(tr("portal.pin_dialog_title"), fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = trFmt("portal.pin_dialog_sub_fmt", "name" to userName),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val targetLength = correctPin.length.coerceIn(4, 6)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        for (i in 0 until targetLength) {
                            val isFilled = i < enteredPin.length
                            Surface(
                                modifier = Modifier.size(16.dp),
                                shape = CircleShape,
                                color = if (isFilled) adaptiveGoldPrimary else Color.Transparent,
                                border = BorderStroke(2.dp, if (isFilled) adaptiveGoldPrimary else MaterialTheme.colorScheme.outlineVariant)
                            ) {}
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "DEL")
                    )

                    keys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                when (key) {
                                    "DEL" -> {
                                        Surface(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .clickable { handleDelete() },
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Backspace, contentDescription = tr("portal.backspace_cd"), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                    "" -> {
                                        Spacer(modifier = Modifier.size(56.dp))
                                    }
                                    else -> {
                                        Surface(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .clickable { handleKeyPress(key) },
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(text = key, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
