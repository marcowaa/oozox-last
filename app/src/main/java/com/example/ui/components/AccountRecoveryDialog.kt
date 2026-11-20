package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserType
import com.example.ui.theme.*
import com.example.util.SecurityValidator

/**
 * High-Security Account Recovery Wizard with Luxury Design
 */
@Composable
fun AccountRecoveryDialog(
    onDismiss: () -> Unit,
    onAccountRecovered: (name: String, phone: String, username: String, type: UserType) -> Unit
) {
    var recoveryMode by remember { mutableStateOf(0) } // 0: 12-Words Seed, 1: Emergency Key
    var seedWordsInput by remember { mutableStateOf(List(12) { "" }) }
    var singleSeedPaste by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var emergencyPassphrase by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                text = "استعادة وتأمين الحساب",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "استرداد البيانات المشفرة E2EE",
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
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 0.8.dp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Recovery Method Tabs
                    TabRow(
                        selectedTabIndex = recoveryMode,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        contentColor = GoldPrimary,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = recoveryMode == 0,
                            onClick = { recoveryMode = 0; errorMessage = null },
                            text = { Text("عبارة الـ 12 كلمة", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = recoveryMode == 1,
                            onClick = { recoveryMode = 1; errorMessage = null },
                            text = { Text("رمز الطوارئ والهاتف", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    if (recoveryMode == 0) {
                        Text(
                            text = "أدخل كلمات الاسترداد الـ 12 بترتيبها الصحيح لاستعادة محفظتك فورياً:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = singleSeedPaste,
                            onValueChange = { paste ->
                                singleSeedPaste = paste
                                val split = paste.trim().split("\\s+".toRegex())
                                if (split.size == 12) {
                                    seedWordsInput = split
                                    errorMessage = null
                                }
                            },
                            label = { Text("لصق العبارة كاملة (12 كلمة)") },
                            placeholder = { Text("oozox gold vault shield karat asset ...") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = emergencyPhone,
                            onValueChange = { emergencyPhone = it; errorMessage = null },
                            label = { Text("رقم الهاتف المرتبط بالحساب") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = GoldPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("الكلمات المدخلة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                        // 12 Words Grid
                        for (row in 0 until 4) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (col in 0 until 3) {
                                    val index = row * 3 + col
                                    val word = seedWordsInput.getOrElse(index) { "" }
                                    Surface(
                                        color = if (word.isNotBlank()) OozoxTeal.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(0.8.dp, if (word.isNotBlank()) OozoxTeal else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${index + 1}.",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoldPrimary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = word.ifBlank { "..." },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (word.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "استعادة الحساب عبر الهاتف وكلمة المرور المشفرة:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = emergencyPhone,
                            onValueChange = { emergencyPhone = it; errorMessage = null },
                            label = { Text("رقم الهاتف المسجل") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = GoldPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = emergencyPassphrase,
                            onValueChange = { emergencyPassphrase = it; errorMessage = null },
                            label = { Text("رمز الحماية السري / Master Key") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = GoldPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
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
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
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
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (recoveryMode == 0) {
                                val cleanPhone = emergencyPhone.trim()
                                if (cleanPhone.isBlank()) {
                                    errorMessage = "يرجى إدخال رقم الهاتف المرتبط بعبارة الاسترداد."
                                } else if (SecurityValidator.validateRecoverySeed(seedWordsInput)) {
                                    isProcessing = true
                                    onAccountRecovered(
                                        "صاحب الحساب",
                                        cleanPhone,
                                        "user_${cleanPhone.filter { it.isDigit() }.takeLast(4)}",
                                        UserType.BUSINESSMAN
                                    )
                                } else {
                                    errorMessage = "يرجى التأكد من إدخال كافة الكلمات الـ 12 بالترتيب الصحيح."
                                }
                            } else {
                                if (emergencyPhone.isBlank() || emergencyPassphrase.length < 4) {
                                    errorMessage = "يرجى إدخال رقم هاتف صحيح ورمز سري لا يقل عن 4 خانات."
                                } else {
                                    onAccountRecovered(
                                        "صاحب الحساب",
                                        emergencyPhone.trim(),
                                        "restored_trader",
                                        UserType.GOLD_TRADER
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.background),
                        modifier = Modifier
                            .weight(1.8f)
                            .testTag("btn_confirm_account_recovery")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استعادة الحساب فورياً", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}
