package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage
import com.example.util.Translations
import com.example.util.tr
import com.example.util.trFmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Supported OTP delivery channels in server fallback order:
 * TELEGRAM (Gateway) -> WHATSAPP (Twilio Verify) -> SMS (Twilio Verify)
 */
enum class OtpDeliveryChannel(
    val titleAr: String,
    val descriptionAr: String,
    val brandColor: Color
) {
    TELEGRAM("تليجرام", "تحقق رسمي فوري", TelegramBlue),
    WHATSAPP("واتساب", "تسليم فوري مشفر", WhatsAppGreen),
    SMS("رسالة SMS", "رسالة نصية مباشرة", SmsGold);

    fun localizedTitle(language: AppLanguage): String = Translations.get(
        when (this) {
            TELEGRAM -> "otp.ch_telegram"
            WHATSAPP -> "otp.ch_whatsapp"
            SMS -> "otp.ch_sms"
        },
        language
    )

    fun localizedDescription(language: AppLanguage): String = Translations.get(
        when (this) {
            TELEGRAM -> "otp.ch_telegram_desc"
            WHATSAPP -> "otp.ch_whatsapp_desc"
            SMS -> "otp.ch_sms_desc"
        },
        language
    )

    companion object {
        fun fromWire(value: String?): OtpDeliveryChannel = when (value?.trim()?.uppercase()) {
            "WHATSAPP" -> WHATSAPP
            "SMS" -> SMS
            else -> TELEGRAM
        }
    }
}

sealed class OtpVerifyUiResult {
    data object Success : OtpVerifyUiResult()
    data class Error(
        val message: String,
        val remainingAttempts: Int? = null,
        val lockRemainingSeconds: Int? = null
    ) : OtpVerifyUiResult()
}

sealed class OtpResendUiResult {
    data class Sent(
        val activeChannel: OtpDeliveryChannel,
        val cooldownSeconds: Int = 60,
        val notice: String? = null
    ) : OtpResendUiResult()

    data class Error(
        val message: String,
        val retryAfterSeconds: Int? = null
    ) : OtpResendUiResult()
}

/**
 * Render crisp vector brand icon for channel
 */
@Composable
fun ChannelBrandIcon(
    channel: OtpDeliveryChannel,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    when (channel) {
        OtpDeliveryChannel.TELEGRAM -> TelegramIcon(modifier = modifier, size = size)
        OtpDeliveryChannel.WHATSAPP -> WhatsAppIcon(modifier = modifier, size = size)
        OtpDeliveryChannel.SMS -> SmsIcon(modifier = modifier, size = size)
    }
}

/**
 * Server-Backed Multi-Channel OTP Verification View
 * - Locks channel switching while an OTP challenge is active.
 * - Going Back cancels and invalidates the pending OTP code on the server before returning to channel selection.
 */
@Composable
fun WhatsAppTelegramOtpView(
    phoneNumber: String,
    initialChannel: OtpDeliveryChannel = OtpDeliveryChannel.TELEGRAM,
    initialCooldownSeconds: Int = 60,
    fallbackNotice: String? = null,
    onVerifyOtpCode: suspend (code: String) -> OtpVerifyUiResult,
    onBackClick: suspend () -> Unit,
    onResendRequested: suspend (channel: OtpDeliveryChannel) -> OtpResendUiResult
) {
    val currentLang = LocalAppLanguage.current
    val coroutineScope = rememberCoroutineScope()
    var currentOtpInput by remember { mutableStateOf("") }
    var currentChannel by remember(initialChannel) { mutableStateOf(initialChannel) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    var isSuccessVerified by remember { mutableStateOf(false) }
    var isResendingNetworkCall by remember { mutableStateOf(false) }
    var resendSuccessNotice by remember(fallbackNotice) { mutableStateOf(fallbackNotice) }

    // Server-provided lockout state
    var lockRemainingSeconds by remember { mutableIntStateOf(0) }
    val isLocked = lockRemainingSeconds > 0

    // Resend cooldown ticker (for same active channel only)
    var resendCountdown by remember(initialCooldownSeconds) {
        mutableIntStateOf(initialCooldownSeconds.coerceIn(0, 300))
    }
    val canResend = resendCountdown <= 0 && !isResendingNetworkCall && !isVerifying && !isCancelling

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val handleBackAndCancel: () -> Unit = {
        if (!isSuccessVerified && !isCancelling) {
            isCancelling = true
            keyboardController?.hide()
            coroutineScope.launch {
                try {
                    onBackClick()
                } finally {
                    isCancelling = false
                }
            }
        }
    }

    // Intercept system hardware/gesture Back so the server always invalidates the sent OTP code
    BackHandler(enabled = !isSuccessVerified && !isVerifying && !isCancelling) {
        handleBackAndCancel()
    }

    // Resend Countdown Ticker
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000L)
            resendCountdown--
        }
    }

    // Server Lock Timer Ticker
    LaunchedEffect(lockRemainingSeconds) {
        if (lockRemainingSeconds > 0) {
            delay(1000L)
            lockRemainingSeconds--
        }
    }

    // Auto Focus Keyboard
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val verifyCode: (String) -> Unit = { code ->
        val cleanCode = code.trim().filter { it.isDigit() }
        if (isLocked) {
            errorMessage = Translations.format("otp.locked_err_fmt", currentLang, "sec" to lockRemainingSeconds)
        } else if (cleanCode.length == 6 && !isVerifying && !isSuccessVerified && !isCancelling) {
            isVerifying = true
            errorMessage = null

            coroutineScope.launch {
                when (val result = onVerifyOtpCode(cleanCode)) {
                    is OtpVerifyUiResult.Success -> {
                        isSuccessVerified = true
                        isVerifying = false
                        keyboardController?.hide()
                    }
                    is OtpVerifyUiResult.Error -> {
                        isVerifying = false
                        if ((result.lockRemainingSeconds ?: 0) > 0) {
                            lockRemainingSeconds = result.lockRemainingSeconds ?: 0
                        }
                        errorMessage = result.message
                    }
                }
            }
        }
    }

    val executeSameChannelResend: () -> Unit = {
        if (!isLocked && !isResendingNetworkCall && !isCancelling && resendCountdown <= 0) {
            isResendingNetworkCall = true
            errorMessage = null
            resendSuccessNotice = null

            coroutineScope.launch {
                when (val result = onResendRequested(currentChannel)) {
                    is OtpResendUiResult.Sent -> {
                        currentChannel = result.activeChannel
                        currentOtpInput = ""
                        resendCountdown = result.cooldownSeconds.coerceIn(10, 300)
                        isResendingNetworkCall = false
                        resendSuccessNotice = result.notice ?: Translations.format(
                            "otp.resent_fmt",
                            currentLang,
                            "channel" to result.activeChannel.localizedTitle(currentLang)
                        )
                    }
                    is OtpResendUiResult.Error -> {
                        isResendingNetworkCall = false
                        if ((result.retryAfterSeconds ?: 0) > 0) {
                            val sec = result.retryAfterSeconds ?: 0
                            if (sec > 60) {
                                lockRemainingSeconds = sec
                            } else {
                                resendCountdown = sec
                            }
                        }
                        errorMessage = result.message
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Back & Cancel Button + Active Locked Delivery Channel Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = handleBackAndCancel,
                enabled = !isVerifying && !isCancelling,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("btn_otp_back")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = GoldPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("otp.edit_phone_cd"),
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCancelling) "جاري إلغاء الرمز..." else tr("otp.edit_phone"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }
            }

            val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
            val channelBadgeTextColor = if (isDarkSurface) {
                currentChannel.brandColor
            } else {
                when (currentChannel) {
                    OtpDeliveryChannel.TELEGRAM -> Color(0xFF0369A1)
                    OtpDeliveryChannel.WHATSAPP -> Color(0xFF0F766E)
                    OtpDeliveryChannel.SMS -> Color(0xFFB45309)
                }
            }
            Surface(
                color = currentChannel.brandColor.copy(alpha = if (isDarkSurface) 0.16f else 0.10f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, currentChannel.brandColor.copy(alpha = 0.45f)),
                modifier = Modifier.testTag("otp_active_channel_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChannelBrandIcon(channel = currentChannel, size = 18.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = trFmt("otp.sent_via_fmt", "channel" to currentChannel.localizedTitle(currentLang)),
                        fontSize = 11.sp,
                        color = channelBadgeTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            ChannelBrandIcon(channel = currentChannel, size = 56.dp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = tr("otp.title"),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = tr("otp.sent_to_sub"),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = phoneNumber,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GoldPrimary,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (!resendSuccessNotice.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = StatusGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.3f)),
                modifier = Modifier.testTag("otp_delivery_notice")
            ) {
                Text(
                    text = resendSuccessNotice!!,
                    fontSize = 11.sp,
                    color = StatusGreen,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        BasicTextField(
            value = currentOtpInput,
            onValueChange = { input ->
                val clean = input.filter { it.isDigit() }.take(6)
                if (!isLocked && !isVerifying && !isCancelling) {
                    currentOtpInput = clean
                    errorMessage = null
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

                val targetBorder = when {
                    isSuccessVerified -> StatusGreen
                    errorMessage != null -> MaterialTheme.colorScheme.error
                    isCurrent -> currentChannel.brandColor
                    isFilled -> OozoxTeal
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                }
                val boxBorderColor by animateColorAsState(
                    targetValue = targetBorder,
                    animationSpec = tween(160),
                    label = "otp_box_border_$i"
                )

                val targetBg = when {
                    isSuccessVerified -> StatusGreen.copy(alpha = 0.15f)
                    isCurrent -> currentChannel.brandColor.copy(alpha = 0.12f)
                    isFilled -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surface
                }
                val boxBgColor by animateColorAsState(
                    targetValue = targetBg,
                    animationSpec = tween(160),
                    label = "otp_box_bg_$i"
                )

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

        if (isLocked) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("otp_lockout_banner")
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LockClock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tr("otp.lock_banner_title"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = trFmt("otp.lock_banner_wait_fmt", "sec" to lockRemainingSeconds),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("otp_error_banner")
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

        Spacer(modifier = Modifier.height(16.dp))

        // Channel Lock Notice: switching delivery channels is blocked until going Back (cancelling active OTP)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("otp_channel_locked_notice")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "لتغيير وسيلة الاستلام، ارجع خطوة للخلف لإلغاء صلاحية الرمز الحالي أولاً.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (!canResend && !isLocked && !isResendingNetworkCall) {
            Text(
                text = trFmt("otp.resend_countdown_fmt", "sec" to resendCountdown),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        } else if (!isLocked) {
            TextButton(
                onClick = executeSameChannelResend,
                enabled = !isResendingNetworkCall && !isCancelling,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_otp_resend_now")
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
                Text(
                    text = "${tr("otp.resend_now")} (${currentChannel.localizedTitle(currentLang)})",
                    fontSize = 12.sp,
                    color = adaptiveGoldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { verifyCode(currentOtpInput) },
            enabled = currentOtpInput.length == 6 && !isLocked && !isVerifying && !isCancelling,
            colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_verify_otp_submit")
        ) {
            if (isVerifying) {
                ExecutiveButtonLoadingIndicator(
                    loadingText = tr("otp.verifying"),
                    spinnerSize = 18.dp,
                    textColor = adaptiveOnPrimary,
                    spinnerPrimaryColor = adaptiveOnPrimary,
                    spinnerSecondaryColor = adaptiveGoldPrimary
                )
            } else {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("otp.confirm_enter"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
