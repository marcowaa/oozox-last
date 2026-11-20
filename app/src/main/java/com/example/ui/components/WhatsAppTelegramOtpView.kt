package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.SecurityValidator
import kotlinx.coroutines.delay

/**
 * Supported OTP delivery channels
 */
enum class OtpDeliveryChannel(
    val titleAr: String,
    val descriptionAr: String,
    val brandColor: Color
) {
    WHATSAPP("واتساب", "تسليم فوري مشفر E2EE", WhatsAppGreen),
    TELEGRAM("تليجرام", "بوت التحقق الآمن الفوري", TelegramBlue),
    SMS("رسالة SMS", "كود مباشر عبر شبكة الجوال", SmsGold)
}

/**
 * Render brand icon for channel
 */
@Composable
fun ChannelBrandIcon(
    channel: OtpDeliveryChannel,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    when (channel) {
        OtpDeliveryChannel.WHATSAPP -> WhatsAppIcon(modifier = modifier, size = size)
        OtpDeliveryChannel.TELEGRAM -> TelegramIcon(modifier = modifier, size = size)
        OtpDeliveryChannel.SMS -> SmsIcon(modifier = modifier, size = size)
    }
}

/**
 * Ultra-Clean, Intelligent OTP Verification View
 * Features:
 * - 6 glowing digit boxes with paste support and auto-advance
 * - WhatsApp, Telegram, and SMS multi-channel delivery and resend switcher
 * - 60s animated resend countdown timer
 * - Anti-brute-force rate limiter (lockout timer and attempt counter)
 * - Robust error handling with specific user-friendly error banners & network retry state
 */
@Composable
fun WhatsAppTelegramOtpView(
    phoneNumber: String,
    expectedOtp: String,
    initialChannel: OtpDeliveryChannel = OtpDeliveryChannel.WHATSAPP,
    onOtpVerified: () -> Unit,
    onBackClick: () -> Unit,
    onResendRequested: (channel: OtpDeliveryChannel) -> String
) {
    var currentOtpInput by remember { mutableStateOf("") }
    var currentExpectedOtp by remember { mutableStateOf(expectedOtp) }
    var currentChannel by remember { mutableStateOf(initialChannel) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var isSuccessVerified by remember { mutableStateOf(false) }
    var isResendingNetworkCall by remember { mutableStateOf(false) }
    var resendSuccessNotice by remember { mutableStateOf<String?>(null) }

    // Rate Limiter
    val rateLimiter = remember { SecurityValidator.RateLimiter(maxAttempts = 3, lockDurationMillis = 180_000L) }
    var isLocked by remember { mutableStateOf(false) }
    var lockRemainingSeconds by remember { mutableStateOf(0) }

    // Resend Timer (60s countdown)
    var resendCountdown by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Resend Countdown Ticker
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000L)
            resendCountdown--
            canResend = false
        } else {
            canResend = true
        }
    }

    // Lock Timer Ticker
    LaunchedEffect(isLocked) {
        while (rateLimiter.isLocked()) {
            isLocked = true
            lockRemainingSeconds = rateLimiter.getRemainingLockSeconds()
            delay(1000L)
        }
        isLocked = false
    }

    // Auto Focus Keyboard
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Verification Logic
    val verifyCode: (String) -> Unit = { code ->
        val cleanCode = code.trim().filter { it.isDigit() }
        if (isLocked) {
            errorMessage = "الحساب مقفل مؤقتاً لحمايتك يرجى الانتظار $lockRemainingSeconds ثانية."
        } else if (cleanCode.length == 6) {
            isVerifying = true
            errorMessage = null

            if (currentExpectedOtp.isNotBlank() && cleanCode == currentExpectedOtp) {
                rateLimiter.recordSuccess()
                isSuccessVerified = true
                isVerifying = false
                keyboardController?.hide()
                onOtpVerified()
            } else {
                rateLimiter.recordFailure()
                isVerifying = false
                if (rateLimiter.isLocked()) {
                    isLocked = true
                    lockRemainingSeconds = rateLimiter.getRemainingLockSeconds()
                    errorMessage = "تم تجاوز عدد المحاولات المسموح بها! تم قفل التحقق لمدة 3 دقائق لدواعي الأمان"
                } else {
                    errorMessage = "رمز التحقق غير صحيح. متبقي لك ${rateLimiter.getRemainingAttempts()} محاولات فقط! يرجى التأكد من الرمز المدخل"
                }
            }
        }
    }

    // Resend Helper
    val executeResend: (OtpDeliveryChannel) -> Unit = { targetChannel ->
        if (canResend && !isLocked) {
            currentChannel = targetChannel
            isResendingNetworkCall = true
            errorMessage = null
            resendSuccessNotice = null

            val newCode = onResendRequested(targetChannel)
            currentExpectedOtp = newCode
            currentOtpInput = ""
            resendCountdown = 60
            isResendingNetworkCall = false
            resendSuccessNotice = "تم إرسال رمز أمان جديد بنجاح عبر ${targetChannel.titleAr}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Back Button & Selected Delivery Channel Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable { onBackClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع لتعديل رقم الهاتف",
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تعديل الرقم",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }
            }

            Surface(
                color = currentChannel.brandColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, currentChannel.brandColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChannelBrandIcon(channel = currentChannel, size = 16.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مُرسل عبر ${currentChannel.titleAr}",
                        fontSize = 11.sp,
                        color = currentChannel.brandColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clean Icon Badge
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(currentChannel.brandColor.copy(alpha = 0.12f))
                .border(1.5.dp, currentChannel.brandColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            ChannelBrandIcon(channel = currentChannel, size = 32.dp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "رمز التحقق الأمني",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "تم إرسال رمز الأمان المكوّن من 6 أرقام إلى:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Text(
            text = phoneNumber,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GoldPrimary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp)
        )

        // Notice or Demo Quick Fill
        if (resendSuccessNotice != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = StatusGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f))
            ) {
                Text(
                    text = resendSuccessNotice!!,
                    fontSize = 11.sp,
                    color = StatusGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Hidden input textfield to manage soft keyboard and input state
        BasicTextField(
            value = currentOtpInput,
            onValueChange = { input ->
                val clean = input.filter { it.isDigit() }.take(6)
                if (!isLocked) {
                    currentOtpInput = clean
                    errorMessage = null
                    resendSuccessNotice = null
                    if (clean.length == 6) {
                        verifyCode(clean)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (currentOtpInput.length == 6) verifyCode(currentOtpInput)
                }
            ),
            modifier = Modifier
                .focusRequester(focusRequester)
                .size(1.dp)
                .testTag("otp_hidden_input")
        )

        // 6-Digit Visual Boxes Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
        ) {
            for (i in 0 until 6) {
                val digit = currentOtpInput.getOrNull(i)?.toString() ?: ""
                val isCurrent = currentOtpInput.length == i
                val isFilled = digit.isNotEmpty()

                val boxBorderColor = when {
                    isSuccessVerified -> StatusGreen
                    errorMessage != null -> MaterialTheme.colorScheme.error
                    isCurrent -> currentChannel.brandColor
                    isFilled -> OozoxTeal
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                }

                val boxBgColor = when {
                    isSuccessVerified -> StatusGreen.copy(alpha = 0.15f)
                    isCurrent -> currentChannel.brandColor.copy(alpha = 0.12f)
                    isFilled -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surface
                }

                Surface(
                    color = boxBgColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(if (isCurrent) 2.dp else 1.dp, boxBorderColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (digit.isNotEmpty()) {
                            Text(
                                text = digit,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSuccessVerified) StatusGreen else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        } else if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(20.dp)
                                    .background(currentChannel.brandColor, RoundedCornerShape(1.dp))
                            )
                        }
                    }
                }
            }
        }

        // Lockout or Error Banner
        if (isLocked) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LockClock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "النظام مقفل مؤقتاً لحماية الحساب ️",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "يرجى الانتظار $lockRemainingSeconds ثانية لإعادة المحاولة",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        } else if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Delivery Channels for Resending Code
        Text(
            text = "إعادة إرسال الرمز عبر قناة أخرى:",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OtpDeliveryChannel.entries.forEach { channel ->
                val isSelected = currentChannel == channel
                Surface(
                    onClick = {
                        if (canResend && !isLocked) {
                            executeResend(channel)
                        } else if (!isSelected) {
                            currentChannel = channel
                        }
                    },
                    enabled = !isLocked,
                    color = if (isSelected) channel.brandColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isSelected) channel.brandColor else Color.Transparent),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ChannelBrandIcon(channel = channel, size = 20.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = channel.titleAr,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) channel.brandColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Resend Timer Status
        if (!canResend && !isLocked) {
            Text(
                text = "يمكنك إعادة إرسال الرمز بعد ($resendCountdown) ثانية",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        } else if (canResend && !isLocked) {
            TextButton(
                onClick = { executeResend(currentChannel) }
            ) {
                if (isResendingNetworkCall) {
                    ExecutiveLoadingSpinner(
                        size = 14.dp,
                        strokeWidth = 1.8.dp,
                        primaryColor = adaptiveGoldPrimary,
                        secondaryColor = adaptiveOnPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text("إعادة إرسال الرمز الآن", fontSize = 12.sp, color = adaptiveGoldPrimary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual Verify & Submit Button
        Button(
            onClick = { verifyCode(currentOtpInput) },
            enabled = currentOtpInput.length == 6 && !isLocked && !isVerifying,
            colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_verify_otp_submit")
        ) {
            if (isVerifying) {
                ExecutiveButtonLoadingIndicator(
                    loadingText = "جاري التحقق من صحة الرمز...",
                    spinnerSize = 18.dp,
                    textColor = adaptiveOnPrimary,
                    spinnerPrimaryColor = adaptiveOnPrimary,
                    spinnerSecondaryColor = adaptiveGoldPrimary
                )
            } else {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تأكيد الرمز والدخول", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
