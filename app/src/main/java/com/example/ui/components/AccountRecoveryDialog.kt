package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.AuthOutcome
import com.example.data.repository.OtpRequestOutcome
import com.example.ui.theme.*
import com.example.util.LocalAppLanguage
import kotlinx.coroutines.launch

/**
 * Server-Verified 2-Step Account Recovery Wizard.
 * Replaces legacy client-side 12-word / master-key bypass with backend OTP verification
 * (/api/auth/recover/request -> /api/auth/recover/confirm).
 * Note: .oozox local encrypted vaults still require their own AES backup passphrase.
 */
@Composable
fun AccountRecoveryDialog(
    initialPhone: String = "",
    onRequestRecoveryOtp: suspend (phone: String, channel: String) -> OtpRequestOutcome,
    onConfirmRecovery: suspend (challengeId: String, code: String, newPassword: String) -> AuthOutcome,
    onDismiss: () -> Unit,
    onAccountRecovered: (AuthOutcome.Authenticated) -> Unit
) {
    val currentLang = LocalAppLanguage.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(0) } // 0: Request OTP, 1: Verify OTP & Set New Password
    var phoneInput by remember(initialPhone) { mutableStateOf(initialPhone) }
    var selectedChannel by remember { mutableStateOf(OtpDeliveryChannel.TELEGRAM) }
    var activeChannel by remember { mutableStateOf(OtpDeliveryChannel.TELEGRAM) }
    var challengeId by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var deliveryNotice by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 480.dp)
                .testTag("account_recovery_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "استرجاع الحساب عبر السيرفر",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (step == 0) "الخطوة 1: طلب رمز التحقق (OTP)" else "الخطوة 2: تأكيد الرمز وتعيين كلمة سر جديدة",
                                fontSize = 11.sp,
                                color = GoldPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    thickness = 0.8.dp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (step == 0) {
                        Text(
                            text = "أدخل رقم الهاتف المسجل (+رمز الدولة) لإرسال رمز استرجاع الحساب وإلغاء جميع الأجهزة الموثوقة السابقة تلقائياً.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = {
                                phoneInput = it
                                errorMessage = null
                            },
                            label = { Text("رقم الهاتف المسجل (مثال: +2010...)") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = GoldPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recover_phone_input")
                        )

                        Text(
                            text = "قناة استلام رمز الاسترجاع (الأولوية: Telegram -> WhatsApp -> SMS):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OtpDeliveryChannel.entries.forEach { channel ->
                                val selected = selectedChannel == channel
                                Surface(
                                    onClick = { selectedChannel = channel },
                                    color = if (selected) channel.brandColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        if (selected) 1.5.dp else 1.dp,
                                        if (selected) channel.brandColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
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
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) channel.brandColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            color = OozoxTeal.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ملاحظة: فك تشفير خزائن النسخ الاحتياطي (.oozox) يظل يتطلب كلمة مرور الخزينة الأصلية عند الاستعادة.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = activeChannel.brandColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, activeChannel.brandColor.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ChannelBrandIcon(channel = activeChannel, size = 20.dp)
                                Text(
                                    text = deliveryNotice ?: "تم إرسال رمز من 6 أرقام عبر ${activeChannel.localizedTitle(currentLang)}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { input ->
                                otpCode = input.filter { it.isDigit() }.take(6)
                                errorMessage = null
                            },
                            label = { Text("رمز التحقق (6 أرقام)") },
                            leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GoldPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recover_otp_input")
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                errorMessage = null
                            },
                            label = { Text("كلمة السر الجديدة (8 أحرف فأكثر)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showPassword = !showPassword },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "إظهار/إخفاء كلمة السر"
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recover_new_password_input")
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                errorMessage = null
                            },
                            label = { Text("تأكيد كلمة السر الجديدة") },
                            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = GoldPrimary) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recover_confirm_password_input")
                        )
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 0.8.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (step == 1 && !isProcessing) {
                                step = 0
                                errorMessage = null
                            } else {
                                onDismiss()
                            }
                        },
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(if (step == 1) "رجوع" else "إلغاء", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (step == 0) {
                                val cleanPhone = phoneInput.trim()
                                if (cleanPhone.filter { it.isDigit() }.length < 8) {
                                    errorMessage = "يرجى إدخال رقم هاتف صحيح مسبوقاً برمز الدولة."
                                    return@Button
                                }
                                isProcessing = true
                                errorMessage = null
                                scope.launch {
                                    when (val res = onRequestRecoveryOtp(cleanPhone, selectedChannel.name)) {
                                        is OtpRequestOutcome.Sent -> {
                                            isProcessing = false
                                            challengeId = res.data.challengeId
                                            activeChannel = OtpDeliveryChannel.fromWire(res.data.activeChannel)
                                            deliveryNotice = if (res.data.attemptedChannels.distinct().size > 1) {
                                                "تعذر ${selectedChannel.localizedTitle(currentLang)} — تم الإرسال تلقائياً عبر ${activeChannel.localizedTitle(currentLang)}"
                                            } else {
                                                res.message
                                            }
                                            step = 1
                                        }
                                        is OtpRequestOutcome.Error -> {
                                            isProcessing = false
                                            errorMessage = res.message
                                        }
                                    }
                                }
                            } else {
                                if (otpCode.length != 6) {
                                    errorMessage = "يرجى إدخال رمز التحقق المكوّن من 6 أرقام."
                                    return@Button
                                }
                                if (newPassword.length < 8) {
                                    errorMessage = "كلمة السر الجديدة يجب أن تكون 8 أحرف على الأقل."
                                    return@Button
                                }
                                if (newPassword != confirmPassword) {
                                    errorMessage = "كلمتا السر غير متطابقتين."
                                    return@Button
                                }
                                isProcessing = true
                                errorMessage = null
                                scope.launch {
                                    when (val res = onConfirmRecovery(challengeId, otpCode, newPassword)) {
                                        is AuthOutcome.Authenticated -> {
                                            isProcessing = false
                                            onAccountRecovered(res)
                                        }
                                        is AuthOutcome.OtpRequired -> {
                                            isProcessing = false
                                            errorMessage = res.message.ifBlank { "يلزم إكمال التحقق الأمني." }
                                        }
                                        is AuthOutcome.Error -> {
                                            isProcessing = false
                                            errorMessage = res.message
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.background),
                        modifier = Modifier
                            .weight(1.8f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_confirm_account_recovery")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.background
                            )
                        } else {
                            Icon(
                                imageVector = if (step == 0) Icons.Default.Sms else Icons.Default.LockReset,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (step == 0) "إرسال رمز الاسترجاع" else "تأكيد وتحديث كلمة السر",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
