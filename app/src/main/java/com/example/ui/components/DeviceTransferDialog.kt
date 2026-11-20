package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserEntity
import com.example.data.model.UserType
import com.example.ui.theme.*
import com.example.util.SecurityValidator

/**
 * High-Security Device-to-Device Migration & Transfer Dialog with Luxury Design
 */
@Composable
fun DeviceTransferDialog(
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onMigrateCompleteOnNewPhone: (name: String, phone: String, username: String, type: UserType) -> Unit
) {
    var transferRoleTab by remember { mutableStateOf(if (currentUser != null) 0 else 1) }
    var passphrase by remember { mutableStateOf("") }
    var transferCodeInput by remember { mutableStateOf("") }
    var receiverPassphraseInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val migrationToken = remember(currentUser, passphrase) {
        if (currentUser != null) {
            SecurityValidator.createDeviceMigrationPayload(
                userId = currentUser.id,
                userName = currentUser.name,
                phone = currentUser.phone,
                userType = currentUser.type,
                passphrase = passphrase
            )
        } else "OOZOX_TRANSFER_PAYLOAD_TOKEN"
    }

    val transferReadableCode = remember(migrationToken) {
        "OOZX-${migrationToken.take(4).uppercase()}-${migrationToken.takeLast(4).uppercase()}"
    }

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
            border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.45f))
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
                                .background(OozoxTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhonelinkSetup,
                                contentDescription = null,
                                tint = OozoxTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "نقل البيانات بين الهواتف",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مزامنة ونقل مباشر مشفر E2EE",
                                fontSize = 11.sp,
                                color = OozoxTeal
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TabRow(
                        selectedTabIndex = transferRoleTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        contentColor = OozoxTeal,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = transferRoleTab == 0,
                            onClick = { transferRoleTab = 0; errorMessage = null },
                            text = { Text("الهاتف القديم (إرسال)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = transferRoleTab == 1,
                            onClick = { transferRoleTab = 1; errorMessage = null },
                            text = { Text("الهاتف الجديد (استقبال)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    if (transferRoleTab == 0) {
                        Text(
                            text = "افتح التطبيق على هاتفك الجديد، واضغط استقبال الحساب، ثم امسح رمز الـ QR أدناه:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        QrMatrixRenderer(
                            content = migrationToken,
                            size = 160.dp,
                            primaryColor = GoldPrimary,
                            accentColor = OozoxTeal
                        )

                        OutlinedTextField(
                            value = passphrase,
                            onValueChange = { passphrase = it },
                            label = { Text("رمز حماية النقل (Passphrase)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.8.dp, OozoxTeal.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("أو أدخل كود النقل السريع على الهاتف الجديد:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = transferReadableCode,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "استقبال ونقل الحساب من الهاتف القديم بتشفير كامل:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            color = OozoxTeal.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .clickable {
                                    errorMessage = "يرجى مسح رمز الـ QR أو لصق كود النقل ورمز الحماية يدوياً."
                                }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(30.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("اضغط لمسح رمز الـ QR من الهاتف القديم", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = OozoxTeal)
                            }
                        }

                        OutlinedTextField(
                            value = transferCodeInput,
                            onValueChange = { transferCodeInput = it; errorMessage = null },
                            label = { Text("أو أدخل كود النقل (مثال: OOZX-...)") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = GoldPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = receiverPassphraseInput,
                            onValueChange = { receiverPassphraseInput = it; errorMessage = null },
                            label = { Text("رمز الحماية السري المحدد على الهاتف القديم") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
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

                    if (transferRoleTab == 1) {
                        Button(
                            onClick = {
                                if (transferCodeInput.isBlank() || receiverPassphraseInput.isBlank()) {
                                    errorMessage = "يرجى مسح الرمز أو إدخال كود النقل ورمز الحماية."
                                } else {
                                    val decoded = SecurityValidator.decryptDeviceMigrationPayload(
                                        encryptedBase64 = transferCodeInput.trim(),
                                        passphrase = receiverPassphraseInput
                                    )
                                    if (decoded != null) {
                                        onMigrateCompleteOnNewPhone(
                                            decoded.name,
                                            decoded.phone,
                                            "migrated_${decoded.name.take(4)}",
                                            UserType.entries.find { it.name == decoded.userType } ?: UserType.BUSINESSMAN
                                        )
                                    } else {
                                        errorMessage = "فشل فك التشفير: يرجى التحقق من كود النقل ورمز الحماية السري."
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.Black),
                            modifier = Modifier
                                .weight(1.8f)
                                .testTag("btn_confirm_device_migration")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إتمام النقل وتهيئة الهاتف", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = MaterialTheme.colorScheme.background),
                            modifier = Modifier.weight(1.8f)
                        ) {
                            Text("تم وجاهز للمسح", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }
    }
}
