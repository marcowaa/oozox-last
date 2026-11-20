package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.BiometricHelper
import com.example.util.DeviceTrustManager
import kotlinx.coroutines.delay

/**
 * High-Security Verification Dialog for sensitive actions (e.g. Chat Deletion).
 * Supports App PIN (with persistent 5-attempt lockout), Biometrics (Fingerprint/Face),
 * and Native Device Lock (Pattern / PIN / Password).
 */
@Composable
fun SecurityVerificationDialog(
    title: String = "تأكيد الحذف",
    subtitle: String = "يرجى التحقق بالرمز السري أو البصمة أو قفل الجهاز",
    expectedPin: String,
    onVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { BiometricHelper.findFragmentActivity(context) }
    val isBiometricAvailable = remember(context) { BiometricHelper.isBiometricAvailable(context) }
    val isDeviceCredentialAvailable = remember(context) { BiometricHelper.isDeviceCredentialAvailable(context) }

    val colors = MaterialTheme.colorScheme
    val targetPinLength = remember(expectedPin) { expectedPin.length.coerceIn(4, 6) }

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableIntStateOf(DeviceTrustManager.getRemainingLockoutSeconds(context)) }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds = DeviceTrustManager.getRemainingLockoutSeconds(context)
        }
    }

    fun vibrate(durationMs: Long = 35L) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun handleKeyPress(digit: String) {
        if (lockoutSeconds > 0) return
        if (enteredPin.length < targetPinLength) {
            val updated = enteredPin + digit
            enteredPin = updated
            errorMessage = null
            vibrate(25L)

            if (updated.length == targetPinLength) {
                if (updated == expectedPin) {
                    DeviceTrustManager.resetPinAttempts(context)
                    onVerified()
                } else {
                    vibrate(90L)
                    val remaining = DeviceTrustManager.recordFailedPinAttempt(context)
                    lockoutSeconds = DeviceTrustManager.getRemainingLockoutSeconds(context)
                    errorMessage = if (lockoutSeconds > 0) {
                        "تم إيقاف المحاولات مؤقتاً لمدة $lockoutSeconds ثانية"
                    } else {
                        "رمز PIN غير صحيح (المتبقي $remaining محاولات)"
                    }
                    enteredPin = ""
                }
            }
        }
    }

    fun launchBiometric() {
        if (activity == null) return
        errorMessage = null
        BiometricHelper.authenticate(
            activity = activity,
            title = title,
            subtitle = "التحقق بالبصمة أو الوجه",
            description = subtitle,
            negativeButtonText = "استخدام رمز PIN",
            onSuccess = {
                DeviceTrustManager.resetPinAttempts(context)
                onVerified()
            },
            onError = { errorCode, errString ->
                if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    errorMessage = errString.toString()
                }
            },
            onFailed = {
                errorMessage = "لم تتطابق البصمة، حاول مجدداً أو استخدم رمز PIN"
            }
        )
    }

    fun launchDeviceLock() {
        if (activity == null) return
        errorMessage = null
        BiometricHelper.authenticateWithDeviceOrBiometric(
            activity = activity,
            title = title,
            subtitle = "قفل الجهاز (النمط / PIN / كلمة المرور)",
            description = subtitle,
            onSuccess = {
                DeviceTrustManager.resetPinAttempts(context)
                onVerified()
            },
            onError = { errorCode, errString ->
                if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    errorMessage = errString.toString()
                }
            },
            onFailed = {
                errorMessage = "تعذر التحقق من قفل الجهاز، حاول مجدداً"
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .wrapContentHeight()
                .testTag("security_verification_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = colors.error.copy(alpha = 0.14f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = colors.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            Text(
                                text = subtitle,
                                fontSize = 11.5.sp,
                                color = colors.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إلغاء",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Quick Biometric & Device Lock Buttons
                if (activity != null && (isBiometricAvailable || isDeviceCredentialAvailable)) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isBiometricAvailable) {
                            Surface(
                                onClick = { launchBiometric() },
                                shape = RoundedCornerShape(14.dp),
                                color = colors.primaryContainer,
                                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_verify_biometric")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "البصمة",
                                        tint = colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "البصمة",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        if (isDeviceCredentialAvailable) {
                            Surface(
                                onClick = { launchDeviceLock() },
                                shape = RoundedCornerShape(14.dp),
                                color = colors.surfaceVariant,
                                border = BorderStroke(1.dp, colors.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_verify_device_lock")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhonelinkLock,
                                        contentDescription = "قفل الجهاز",
                                        tint = colors.onSurface,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "قفل الجهاز",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "أو أدخل رمز PIN للتطبيق",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until targetPinLength) {
                        val filled = i < enteredPin.length
                        val dotColor by animateColorAsState(
                            targetValue = when {
                                lockoutSeconds > 0 -> colors.error.copy(alpha = 0.5f)
                                filled -> colors.primary
                                else -> Color.Transparent
                            },
                            animationSpec = tween(150),
                            label = "pin_dot_color"
                        )
                        Surface(
                            modifier = Modifier.size(15.dp),
                            shape = CircleShape,
                            color = dotColor,
                            border = BorderStroke(
                                1.8.dp,
                                if (filled) colors.primary else colors.outlineVariant
                            )
                        ) {}
                    }
                }

                AnimatedVisibility(visible = errorMessage != null || lockoutSeconds > 0) {
                    Text(
                        text = if (lockoutSeconds > 0) {
                            "تم حظر المحاولات مؤقتاً ($lockoutSeconds ثانية)"
                        } else {
                            errorMessage.orEmpty()
                        },
                        color = colors.error,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "DEL")
                )

                keypadRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "" -> Spacer(modifier = Modifier.size(54.dp))
                                "DEL" -> {
                                    Surface(
                                        onClick = {
                                            if (enteredPin.isNotEmpty() && lockoutSeconds <= 0) {
                                                enteredPin = enteredPin.dropLast(1)
                                                errorMessage = null
                                                vibrate(20L)
                                            }
                                        },
                                        shape = CircleShape,
                                        color = colors.surfaceVariant,
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "مسح",
                                                tint = colors.onSurfaceVariant,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Surface(
                                        onClick = { handleKeyPress(key) },
                                        enabled = lockoutSeconds <= 0,
                                        shape = CircleShape,
                                        color = colors.surfaceVariant.copy(alpha = if (lockoutSeconds > 0) 0.4f else 1f),
                                        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f)),
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                fontSize = 19.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.onSurface
                                            )
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
