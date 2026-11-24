package com.example.ui.components

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
    TELEGRAM("تليجرام", "Telegram Gateway — تحقق رسمي فوري", TelegramBlue),
    WHATSAPP("واتساب", "تسليم فوري عبر واتساب", WhatsAppGreen),
    SMS("رسالة SMS", "كود مباشر عبر شبكة الجوال", SmsGold);

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
 * Render brand icon for channel
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
 * - Verification & rate limiting are enforced strictly server-side (Twilio Verify + Telegram Gateway)
 * - Supports fallback routing notice when backend steps down TELEGRAM -> WHATSAPP -> SMS
 */
@Composable
fun WhatsAppTelegramOtpView(
    phoneNumber: String,
    initialChannel: OtpDeliveryChannel = OtpDeliveryChannel.TELEGRAM,
    initialCooldownSeconds: Int = 60,
    fallbackNotice: String? = null,
    onVerifyOtpCode: suspend (code: String) -> OtpVerifyUiResult,
    onBackClick: () -> Unit,
    onResendRequested: suspend (channel: OtpDeliveryChannel) -> OtpResendUiResult
) {
    val currentLang = LocalAppLanguage.current
    val coroutineScope = rememberCoroutineScope()
    var currentOtpInput by remember { mutableStateOf("") }
    var currentChannel by remember(initialChannel) { mutableStateOf(initialChannel) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var isSuccessVerified by remember { mutableStateOf(false) }
    var isResendingNetworkCall by remember { mutableStateOf(false) }
    var resendSuccessNotice by remember(fallbackNotice) { mutableStateOf(fallbackNotice) }

    // Server-provided lockout state
    var lockRemainingSeconds by remember { mutableIntStateOf(0) }
    val isLocked = lockRemainingSeconds > 0

    // Resend cooldown ticker
    var resendCountdown by remember(initialCooldownSeconds) {
        mutableIntStateOf(initialCooldownSeconds.coerceIn(0, 300))
    }
    val canResend = resendCountdown <= 0 && !isResendingNetworkCall && !isVerifying

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

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
        } else if (cleanCode.length == 6 && !isVerifying && !isSuccessVerified) {
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

    val executeResend: (OtpDeliveryChannel) -> Unit = { targetChannel ->
        if (!isLocked && !isResendingNetworkCall && (resendCountdown <= 0 || targetChannel != currentChannel)) {
            isResendingNetworkCall = true
            errorMessage = null
            resendSuccessNotice = null

            coroutineScope.launch {
                when (val result = onResendRequested(targetChannel)) {
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
        // Top Back Button & Active Delivery Channel Badge
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
                    .testTag("btn_otp_back")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("otp.edit_phone_cd"),
                        tint = GoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr("otp.edit_phone"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }
            }

            Surface(
                color = currentChannel.brandColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, currentChannel.brandColor.copy(alpha = 0.4f)),
                modifier = Modifier.testTag("otp_active_channel_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChannelBrandIcon(channel = currentChannel, size = 16.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = trFmt("otp.sent_via_fmt", "channel" to currentChannel.localizedTitle(currentLang)),
                        fontSize = 11.sp,
                        color = currentChannel.brandColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        Text(
            text = phoneNumber,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GoldPrimary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp)
        )

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
                if (!isLocked && !isVerifying) {
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

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = tr("otp.resend_other"),
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
                        if (!isLocked && !isResendingNetworkCall && (canResend || !isSelected)) {
                            executeResend(channel)
                        }
                    },
                    enabled = !isLocked && !isResendingNetworkCall,
                    color = if (isSelected) channel.brandColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isSelected) channel.brandColor else Color.Transparent),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("otp_resend_channel_${channel.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ChannelBrandIcon(channel = channel, size = 20.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = channel.localizedTitle(currentLang),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) channel.brandColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                onClick = { executeResend(currentChannel) },
                enabled = !isResendingNetworkCall,
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
                Text(tr("otp.resend_now"), fontSize = 12.sp, color = adaptiveGoldPrimary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
