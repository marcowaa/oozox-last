package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.*
import com.example.util.BiometricHelper
import com.example.util.BiometricStatus

@Composable
fun BiometricLockScreen(
    correctPin: String,
    userName: String = "المستخدم",
    userTypeLabel: String = "حساب مالي موثق",
    isBiometricEnabled: Boolean = true,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { BiometricHelper.findFragmentActivity(context) }
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPinModeOnly by remember { mutableStateOf(!isBiometricEnabled) }

    val biometricStatus = remember(context) {
        BiometricHelper.checkBiometricStatus(context)
    }

    val canUseBiometrics = remember(biometricStatus, isBiometricEnabled) {
        isBiometricEnabled && biometricStatus == BiometricStatus.AVAILABLE
    }

    val triggerBiometrics = {
        if (activity != null && canUseBiometrics) {
            errorMessage = null
            BiometricHelper.authenticate(
                activity = activity,
                title = "تأمين البيانات المالية لمنصة OOZOX",
                subtitle = "المصادقة ببصمة الإصبع أو التعرف على الوجه",
                description = "مرحباً $userName، يرجى تأكيد الهوية لفتح المحفظة وبيانات تداول الذهب",
                negativeButtonText = "استخدام رمز PIN",
                onSuccess = {
                    onUnlocked()
                },
                onError = { errorCode, errString ->
                    // If user canceled or chose negative button, fallback to PIN mode smoothly
                    isPinModeOnly = true
                    if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        errorMessage = errString.toString()
                    }
                },
                onFailed = {
                    errorMessage = "فشلت المصادقة البيومترية، يرجى المحاولة مجدداً أو إدخال رمز PIN"
                }
            )
        }
    }

    // Auto-prompt on initial appearance if biometrics are enabled and available
    LaunchedEffect(Unit) {
        if (canUseBiometrics && activity != null) {
            triggerBiometrics()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    fun vibratePhone() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (_: Exception) {}
    }

    fun handleKeyPress(key: String) {
        if (enteredPin.length < 6) {
            val newPin = enteredPin + key
            enteredPin = newPin
            errorMessage = null
            vibratePhone()

            // Auto-check if matches correct PIN
            if (newPin == correctPin) {
                onUnlocked()
            } else if (newPin.length >= correctPin.length.coerceAtLeast(4)) {
                // Wrong PIN attempt
                errorMessage = "رمز PIN غير صحيح، يرجى المحاولة مجدداً"
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

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF090D16),
                Color(0xFF0F172A),
                Color(0xFF050811)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                MaterialTheme.colorScheme.background
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("biometric_lock_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Brand & Security Shield
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Surface(
                    color = adaptiveGoldPrimary.copy(alpha = 0.15f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, adaptiveGoldPrimary.copy(alpha = 0.8f)),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "حماية أمنية",
                            tint = adaptiveGoldPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "منصة OOZOX المالية",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = adaptiveGoldPrimary,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "بيانات الذهب والمحفظة مشفرة ومؤمنة ️",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = adaptiveGoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$userName • $userTypeLabel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Middle Section: Biometric Trigger or PIN Dots
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                // PIN Visual Dots (4 to 6 dots)
                val targetLength = correctPin.length.coerceIn(4, 6)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    for (i in 0 until targetLength) {
                        val isFilled = i < enteredPin.length
                        Surface(
                            modifier = Modifier.size(18.dp),
                            shape = CircleShape,
                            color = if (isFilled) adaptiveGoldPrimary else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                if (isFilled) adaptiveGoldPrimary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {}
                    }
                }

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = if (canUseBiometrics) "أدخل رمز PIN أو استخدم بصمة الإصبع والوجه" else "أدخل رمز PIN السري للمتابعة",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Quick Biometric Trigger Chip
                if (canUseBiometrics) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = adaptiveGoldPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .scale(pulseScale)
                            .clickable { triggerBiometrics() }
                            .testTag("btn_trigger_biometrics")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = "بصمة", tint = adaptiveGoldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Face, contentDescription = "وجه", tint = adaptiveGoldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "فتح فوري بالبصمة أو الوجه",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = adaptiveGoldPrimary
                            )
                        }
                    }
                }
            }

            // Bottom Section: Numeric Keypad (1 - 9, Biometric Button, 0, Backspace)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIOMETRIC", "0", "BACKSPACE")
                )

                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "BIOMETRIC" -> {
                                    if (canUseBiometrics) {
                                        Surface(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(CircleShape)
                                                .clickable { triggerBiometrics() }
                                                .testTag("keypad_biometric"),
                                            color = adaptiveGoldPrimary.copy(alpha = 0.15f),
                                            shape = CircleShape,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, adaptiveGoldPrimary.copy(alpha = 0.4f))
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Fingerprint,
                                                    contentDescription = "بصمة الإصبع",
                                                    tint = adaptiveGoldPrimary,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(68.dp))
                                    }
                                }
                                "BACKSPACE" -> {
                                    Surface(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .clickable { handleDelete() }
                                            .testTag("keypad_backspace"),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Surface(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .clickable { handleKeyPress(key) }
                                            .testTag("keypad_$key"),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
