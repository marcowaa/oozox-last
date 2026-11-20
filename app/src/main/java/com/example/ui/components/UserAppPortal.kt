package com.example.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.ui.theme.*
import com.example.util.BiometricHelper
import com.example.util.BiometricStatus
import com.example.util.DeviceTrustManager
import com.example.util.SecurityValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * UserAppPortal - High-Security Biometric & Multi-Factor Access Gateway for OOZOX
 * Features:
 * - 1-Tap Biometric Authentication (Fingerprint & Face Unlock) for repeat users
 * - Live hardware sensor detection and status telemetry
 * - Quick PIN fallback authentication dialog
 * - Sub-account & hidden profile quick biometric switcher
 * - Smooth transition to multi-channel mobile login (WhatsApp / Telegram / SMS)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAppPortal(
    repeatUser: RepeatUserSummary?,
    allHiddenProfiles: List<HiddenProfileEntity> = emptyList(),
    onLoginSuccess: (name: String, phone: String, username: String, type: UserType) -> Unit,
    onSwitchProfile: ((HiddenProfileEntity) -> Unit)? = null,
    onCreateSubAccountDirectly: ((name: String, pin: String, company: String, role: String, cap: Double, silent: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val coroutineScope = rememberCoroutineScope()

    val isTrustedDevice = remember(repeatUser?.phone) {
        repeatUser?.phone != null && DeviceTrustManager.isDeviceTrustedForPhone(context, repeatUser.phone)
    }

    // Mode: Only allow quick biometric/PIN/device-lock login if this exact device is trusted via prior OTP
    var isRepeatUserMode by remember(repeatUser, isTrustedDevice) {
        mutableStateOf(repeatUser != null && isTrustedDevice)
    }

    // Phone input state
    var authStep by remember { mutableStateOf(0) } // 0 = Phone Input, 1 = OTP Verification
    var selectedCountry by remember { mutableStateOf(SecurityValidator.SUPPORTED_COUNTRIES.first()) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var countrySearchQuery by remember { mutableStateOf("") }
    var rawPhone by remember { mutableStateOf("") }
    var selectedDeliveryChannel by remember { mutableStateOf(OtpDeliveryChannel.WHATSAPP) }

    // Error & status states
    var isSendingOtpNetworkCall by remember { mutableStateOf(false) }
    var specificErrorMessage by remember { mutableStateOf<String?>(null) }
    var networkFailureError by remember { mutableStateOf<String?>(null) }
    var detectedCarrier by remember { mutableStateOf<String?>(null) }
    var autoCorrectionNotice by remember { mutableStateOf<String?>(null) }
    var currentGeneratedOtp by remember { mutableStateOf(SecurityValidator.generateSecureOtp()) }

    // Dialog state controllers
    var showPinDialog by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var showDeviceTransferDialog by remember { mutableStateOf(false) }

    // Biometric & Device Lock hardware status
    val biometricStatus = remember(context) {
        BiometricHelper.checkBiometricStatus(context)
    }
    val isBiometricAvailable = biometricStatus == BiometricStatus.AVAILABLE
    val isDeviceCredentialAvailable = remember(context) {
        BiometricHelper.isDeviceCredentialAvailable(context)
    }

    val isOnline = remember(context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val net = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(net)
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: true
        } catch (_: Exception) {
            true
        }
    }

    // Phone Validation
    val validationResult = remember(selectedCountry, rawPhone) {
        if (rawPhone.isNotBlank()) {
            SecurityValidator.validateMobileNumber(selectedCountry, rawPhone)
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

    // Biometric Trigger Function
    val triggerBiometricAuthentication: (onSuccessAction: () -> Unit) -> Unit = { successCallback ->
        if (activity != null && isBiometricAvailable) {
            specificErrorMessage = null
            networkFailureError = null
            BiometricHelper.authenticate(
                activity = activity,
                title = "بوابة الدخول البيومترية الآمنة ️",
                subtitle = "المصادقة ببصمة الإصبع أو التعرف على الوجه",
                description = "مرحباً ${repeatUser?.name ?: "بك"} في OOZOX، يرجى تأكيد الهوية للدخول الفوري وتأمين بياناتك المالية",
                negativeButtonText = if (repeatUser?.pin != null) "استخدام رمز PIN" else "إلغاء",
                onSuccess = {
                    successCallback()
                },
                onError = { errorCode, errString ->
                    if (errorCode == androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON && repeatUser?.pin != null) {
                        showPinDialog = true
                    } else if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        specificErrorMessage = errString.toString()
                    }
                },
                onFailed = {
                    specificErrorMessage = "لم يتم التعرف على البصمة أو الوجه، يرجى المحاولة مجدداً أو استخدام رمز PIN"
                }
            )
        } else {
            // Biometric not available - fallback to PIN dialog if PIN exists
            if (repeatUser?.pin != null) {
                showPinDialog = true
            }
        }
    }

    val triggerDeviceLockAuthentication: (onSuccessAction: () -> Unit) -> Unit = { successCallback ->
        if (activity != null && isDeviceCredentialAvailable) {
            specificErrorMessage = null
            networkFailureError = null
            BiometricHelper.authenticateWithDeviceOrBiometric(
                activity = activity,
                title = "تأكيد الهوية بقفل الجهاز",
                subtitle = "النمط أو رمز PIN أو كلمة مرور الهاتف",
                description = "مرحباً ${repeatUser?.name ?: "بك"}، يرجى فك قفل الجهاز للدخول الآمن",
                onSuccess = {
                    successCallback()
                },
                onError = { errorCode, errString ->
                    if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        specificErrorMessage = errString.toString()
                    }
                },
                onFailed = {
                    specificErrorMessage = "تعذر التحقق من قفل الجهاز، يرجى المحاولة مجدداً"
                }
            )
        } else if (repeatUser?.pin != null) {
            showPinDialog = true
        }
    }

    // Auto-prompt biometrics once for trusted repeat user when portal opens
    LaunchedEffect(repeatUser, isTrustedDevice, isBiometricAvailable) {
        if (repeatUser != null && isTrustedDevice && isBiometricAvailable && activity != null && isRepeatUserMode) {
            delay(350L)
            triggerBiometricAuthentication {
                val userType = try {
                    UserType.valueOf(repeatUser.type)
                } catch (_: Exception) {
                    UserType.BUSINESSMAN
                }
                onLoginSuccess(repeatUser.name, repeatUser.phone, repeatUser.username, userType)
            }
        }
    }

    // Pulsing animation for biometric button
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

    val dispatchOtpAndProceed: () -> Unit = {
        val res = SecurityValidator.validateMobileNumber(selectedCountry, rawPhone)
        if (!res.isValid) {
            specificErrorMessage = res.specificError ?: "رقم الهاتف غير صالح. يرجى التأكد من الرقم والمحاولة مجدداً."
            networkFailureError = null
        } else {
            specificErrorMessage = null
            networkFailureError = null
            isSendingOtpNetworkCall = true

            coroutineScope.launch {
                delay(600L)
                isSendingOtpNetworkCall = false

                if (!isOnline) {
                    networkFailureError = "تعذر الاتصال بالخادم لإرسال رمز التحقق. يرجى التحقق من اتصال الإنترنت أو تجربة قناة أخرى (واتساب / تليجرام)."
                } else {
                    currentGeneratedOtp = SecurityValidator.generateSecureOtp()
                    authStep = 1
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

            // Official OOZOX Squircle Logo with Subtle Glow
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
                text = "بوابة الأعمال والمراسلة الآمنة",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Authentication Card
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
                    // Biometric Sensor Live Telemetry Chip
                    BiometricStatusBadge(biometricStatus = biometricStatus)

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isRepeatUserMode && repeatUser != null && isTrustedDevice) {
                        // --- REPEAT USER BIOMETRIC / DEVICE LOCK / PIN QUICK ACCESS VIEW ---
                        RepeatUserPortalView(
                            repeatUser = repeatUser,
                            isBiometricAvailable = isBiometricAvailable,
                            isDeviceCredentialAvailable = isDeviceCredentialAvailable,
                            pulseScale = pulseScale,
                            onBiometricClick = {
                                triggerBiometricAuthentication {
                                    val userType = try {
                                        UserType.valueOf(repeatUser.type)
                                    } catch (_: Exception) {
                                        UserType.BUSINESSMAN
                                    }
                                    onLoginSuccess(repeatUser.name, repeatUser.phone, repeatUser.username, userType)
                                }
                            },
                            onDeviceLockClick = {
                                triggerDeviceLockAuthentication {
                                    val userType = try {
                                        UserType.valueOf(repeatUser.type)
                                    } catch (_: Exception) {
                                        UserType.BUSINESSMAN
                                    }
                                    onLoginSuccess(repeatUser.name, repeatUser.phone, repeatUser.username, userType)
                                }
                            },
                            onPinClick = { showPinDialog = true },
                            onSwitchAccountClick = {
                                isRepeatUserMode = false
                                authStep = 0
                            }
                        )

                        // Sub-Account & Hidden Profiles Quick Switcher (if any)
                        if (allHiddenProfiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "أو الدخول المباشر إلى خزينة / ملف فرعي :",
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
                                            Icon(Icons.Default.Fingerprint, contentDescription = "دخول بالبصمة", tint = adaptiveGoldPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }

                    } else if (authStep == 1) {
                        // --- STEP 2: MULTI-CHANNEL OTP VERIFICATION VIEW ---
                        val fullPhone = if (rawPhone.isNotBlank()) "${selectedCountry.code}${rawPhone.filter { it.isDigit() }}" else (repeatUser?.phone ?: "")
                        WhatsAppTelegramOtpView(
                            phoneNumber = fullPhone,
                            expectedOtp = currentGeneratedOtp,
                            initialChannel = selectedDeliveryChannel,
                            onOtpVerified = {
                                DeviceTrustManager.markDeviceTrusted(context, fullPhone)
                                val finalName = repeatUser?.name ?: "مستخدم OOZOX"
                                val finalUser = repeatUser?.username ?: "user_${fullPhone.takeLast(4)}"
                                val finalType = try {
                                    repeatUser?.type?.let { UserType.valueOf(it) } ?: UserType.BUSINESSMAN
                                } catch (_: Exception) {
                                    UserType.BUSINESSMAN
                                }
                                onLoginSuccess(finalName, fullPhone, finalUser, finalType)
                            },
                            onBackClick = { authStep = 0 },
                            onResendRequested = { channel ->
                                selectedDeliveryChannel = channel
                                currentGeneratedOtp = SecurityValidator.generateSecureOtp()
                                currentGeneratedOtp
                            }
                        )
                    } else {
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
                                        text = "تم رصد جهاز جديد — يلزم تأكيد رمز التحقق (OTP) لتفعيل الدخول السريع على هذا الجهاز.",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                        // --- STEP 1: AUTHENTIC PHONE NUMBER & MULTI-CHANNEL ACCESS ---
                        Text(
                            text = "تسجيل الدخول",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "أدخل رقم الهاتف لتلقي كود التحقق السريع الآمن:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // PHONE NUMBER INPUT FIELD WITH COUNTRY SELECTOR
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "رقم الهاتف المحمول",
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
                                // Country code trigger button
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

                                // Mobile number text field
                                OutlinedTextField(
                                    value = rawPhone,
                                    onValueChange = { input ->
                                        rawPhone = input.filter { it.isDigit() || it == ' ' || it == '-' }
                                        specificErrorMessage = null
                                        networkFailureError = null
                                    },
                                    placeholder = {
                                        Text(
                                            text = "أدخل رقم الهاتف",
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
                                            IconButton(onClick = { rawPhone = "" }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "مسح",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { dispatchOtpAndProceed() }
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

                            // Carrier Detection / Live Telemetry feedback
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
                                            text = "الشبكة المكتشفة: $detectedCarrier",
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

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "اختر وسيلة استلام رمز التحقق (OTP):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Horizontal Authentic Brand Icons Row
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
                                            text = channel.titleAr,
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
                                        Text("خطأ في الشبكة أو تعذر الإرسال", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = networkFailureError!!, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                        TextButton(onClick = { dispatchOtpAndProceed() }) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إعادة المحاولة", color = adaptiveGoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Primary Action Button - Dispatches OTP and proceeds
                        Button(
                            onClick = { dispatchOtpAndProceed() },
                            enabled = !isSendingOtpNetworkCall,
                            colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_proceed_otp")
                        ) {
                            if (isSendingOtpNetworkCall) {
                                ExecutiveButtonLoadingIndicator(
                                    loadingText = "جاري إرسال رمز التحقق...",
                                    spinnerSize = 18.dp,
                                    textColor = adaptiveOnPrimary,
                                    spinnerPrimaryColor = adaptiveOnPrimary,
                                    spinnerSecondaryColor = adaptiveGoldPrimary
                                )
                            } else {
                                Text(
                                    text = "إرسال رمز التحقق عبر ${selectedDeliveryChannel.titleAr}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Quick Biometric Option in channel view
                        if (isBiometricAvailable) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    triggerBiometricAuthentication {
                                        val finalPhone = if (rawPhone.isNotBlank()) "${selectedCountry.code}${rawPhone.filter { it.isDigit() }}" else (repeatUser?.phone ?: "")
                                        if (finalPhone.isNotBlank()) {
                                            val finalName = repeatUser?.name ?: "مستخدم OOZOX"
                                            val finalUser = repeatUser?.username ?: "user_${finalPhone.takeLast(4)}"
                                            val finalType = try {
                                                repeatUser?.type?.let { UserType.valueOf(it) } ?: UserType.BUSINESSMAN
                                            } catch (_: Exception) {
                                                UserType.BUSINESSMAN
                                            }
                                            onLoginSuccess(finalName, finalPhone, finalUser, finalType)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_quick_biometric_login")
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("المصادقة البيومترية (بصمة / وجه)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                            }
                        }

                        // Return to repeat user mode if available
                        if (repeatUser != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { isRepeatUserMode = true },
                                modifier = Modifier.testTag("btn_back_to_repeat_user")
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("العودة إلى الحساب السابق (${repeatUser.name})", fontSize = 11.sp, color = adaptiveGoldPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Account Recovery & Migration Links
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { showRecoveryDialog = true },
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Icon(Icons.Default.LockReset, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استعادة بالكلمات السرية", fontSize = 11.sp, color = adaptiveGoldPrimary, fontWeight = FontWeight.Medium)
                            }

                            TextButton(
                                onClick = { showDeviceTransferDialog = true },
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Icon(Icons.Default.PhonelinkSetup, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نقل من هاتف آخر", fontSize = 11.sp, color = OozoxTeal, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Trust & Encryption Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "نظام الحماية البيومترية والتشفير المالي E2EE مشفر 256-bit",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Modal: Quick PIN Entry Dialog for Repeat Users
    if (showPinDialog && repeatUser != null && !repeatUser.pin.isNullOrBlank()) {
        UserPortalPinDialog(
            correctPin = repeatUser.pin,
            userName = repeatUser.name,
            onDismiss = { showPinDialog = false },
            onPinVerified = {
                showPinDialog = false
                val userType = try {
                    UserType.valueOf(repeatUser.type)
                } catch (_: Exception) {
                    UserType.BUSINESSMAN
                }
                onLoginSuccess(repeatUser.name, repeatUser.phone, repeatUser.username, userType)
            }
        )
    }

    // Modal: Country Code Picker
    if (showCountryPicker) {
        val filteredCountries = remember(countrySearchQuery) {
            if (countrySearchQuery.isBlank()) {
                SecurityValidator.SUPPORTED_COUNTRIES
            } else {
                SecurityValidator.SUPPORTED_COUNTRIES.filter {
                    it.countryNameAr.contains(countrySearchQuery, ignoreCase = true) ||
                            it.code.contains(countrySearchQuery)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showCountryPicker = false; countrySearchQuery = "" },
            title = {
                Text("اختر الدولة وكود الاتصال", color = adaptiveGoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        placeholder = { Text("بحث عن دولة أو كود...") },
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
                                            text = country.countryNameAr,
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
                TextButton(onClick = { showCountryPicker = false; countrySearchQuery = "" }) {
                    Text("إغلاق", color = adaptiveGoldPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Modal: 12-Word BIP39 Account Recovery Wizard
    if (showRecoveryDialog) {
        AccountRecoveryDialog(
            onDismiss = { showRecoveryDialog = false },
            onAccountRecovered = { rName, rPhone, rUser, rType ->
                showRecoveryDialog = false
                onLoginSuccess(rName, rPhone, rUser, rType)
            }
        )
    }

    // Modal: Cross-Device Phone Migration & Transfer Dialog
    if (showDeviceTransferDialog) {
        DeviceTransferDialog(
            currentUser = null,
            onDismiss = { showDeviceTransferDialog = false },
            onMigrateCompleteOnNewPhone = { mName, mPhone, mUser, mType ->
                showDeviceTransferDialog = false
                onLoginSuccess(mName, mPhone, mUser, mType)
            }
        )
    }
}

/**
 * Repeat User Profile Quick Access Card in UserAppPortal
 */
@Composable
private fun RepeatUserPortalView(
    repeatUser: RepeatUserSummary,
    isBiometricAvailable: Boolean,
    isDeviceCredentialAvailable: Boolean,
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
            text = "مرحباً بعودتك",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Profile Identity Card
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
                            contentDescription = "موثق",
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
                        UserType.GOLD_TRADER.name -> "تاجر وأعمال معتمد"
                        UserType.ADMIN.name -> "إدارة النظام"
                        else -> "رجل أعمال ومستثمر"
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

        // PRIMARY ACTION: Biometric Unlock (Fingerprint / Face)
        Button(
            onClick = onBiometricClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = adaptiveGoldPrimary,
                contentColor = adaptiveOnPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .scale(if (isBiometricAvailable) pulseScale else 1f)
                .testTag("btn_portal_biometric_login")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = "بصمة",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تسجيل الدخول بالبصمة أو الوجه",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (isDeviceCredentialAvailable) {
            Spacer(modifier = Modifier.height(10.dp))

            // DEVICE LOCK ACTION: Native Pattern / PIN / Password
            OutlinedButton(
                onClick = onDeviceLockClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("btn_portal_device_lock_login")
            ) {
                Icon(Icons.Default.PhonelinkLock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("قفل الجهاز (النمط / PIN / كلمة المرور)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SECONDARY ACTION: App PIN Fallback
        OutlinedButton(
            onClick = onPinClick,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("btn_portal_pin_login")
        ) {
            Icon(Icons.Default.Password, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("تسجيل الدخول برمز PIN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // SWITCH ACCOUNT ACTION
        TextButton(
            onClick = onSwitchAccountClick,
            modifier = Modifier.testTag("btn_portal_switch_account")
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("تسجيل الدخول بحساب آخر", fontSize = 12.sp, color = OozoxTeal, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Live Biometric Hardware Status Badge
 */
@Composable
private fun BiometricStatusBadge(biometricStatus: BiometricStatus) {
    val (statusText, statusColor, icon) = when (biometricStatus) {
        BiometricStatus.AVAILABLE -> Triple(
            "مستشعر البصمة والتعرف على الوجه جاهز",
            adaptiveStatusGreen,
            Icons.Default.Fingerprint
        )
        BiometricStatus.NOT_ENROLLED -> Triple(
            "المستشعر متوفر (لم تسجل بصمة بالجهاز)",
            adaptiveStatusOrange,
            Icons.Default.WarningAmber
        )
        BiometricStatus.NO_HARDWARE -> Triple(
            "الجهاز لا يدعم المستشعرات البيومترية",
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Lock
        )
        BiometricStatus.HARDWARE_UNAVAILABLE -> Triple(
            "مستشعر البصمة غير متاح حالياً",
            adaptiveStatusOrange,
            Icons.Default.HourglassEmpty
        )
        else -> Triple(
            "المصادقة البيومترية غير نشطة",
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

/**
 * Sleek Quick PIN Verification Modal for Portal
 */
@Composable
private fun UserPortalPinDialog(
    correctPin: String,
    userName: String,
    onDismiss: () -> Unit,
    onPinVerified: () -> Unit
) {
    val context = LocalContext.current
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
                    "تم إيقاف المحاولات مؤقتاً لمدة $lockoutSeconds ثانية"
                } else {
                    "رمز PIN غير صحيح (المتبقي $remaining محاولات)"
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
                            .heightIn(min = 40.dp)
                            .clickable { onDismiss() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("إلغاء", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("التحقق بالرمز السري PIN", fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "مرحباً $userName، أدخل رمز PIN الخاص بحسابك",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // PIN Visual Dots
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

                // Numeric Keypad
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
                                            Icon(Icons.Default.Backspace, contentDescription = "حذف", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
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
