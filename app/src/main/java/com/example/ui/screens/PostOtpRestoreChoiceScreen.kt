package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OozoxTeal
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.util.BackupMetadataPreview
import com.example.util.BackupResult
import com.example.util.CloudBackupRecord
import com.example.util.SecureBackupVaultEngine
import kotlinx.coroutines.launch

/**
 * Post-OTP Returning-User Restore Choice Screen.
 * Shown ONLY when `isExistingUser == true` after OTP verification (never for newly registered accounts).
 * Options:
 * 1) Real Server Cloud Backups (/api/backups)
 * 2) Local encrypted .oozox file via Android file picker (with metadata preview + ownership check)
 * 3) Lossless Skip ("تخطي الآن") — never wipes or overwrites local data
 */
@Composable
fun PostOtpRestoreChoiceScreen(
    phone: String,
    onRestoreComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cloudBackups by remember { mutableStateOf<List<CloudBackupRecord>>(emptyList()) }
    var isLoadingCloud by remember { mutableStateOf(true) }
    var cloudError by remember { mutableStateOf<String?>(null) }

    var selectedCloudBackup by remember { mutableStateOf<CloudBackupRecord?>(null) }
    var localVaultContent by remember { mutableStateOf<String?>(null) }
    var localFileName by remember { mutableStateOf<String?>(null) }
    var localPreview by remember { mutableStateOf<BackupMetadataPreview?>(null) }

    var passphraseInput by remember { mutableStateOf("") }
    var isPassVisible by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var restoreResult by remember { mutableStateOf<BackupResult?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    suspend fun loadBackups() {
        isLoadingCloud = true
        cloudError = null
        val res = SecureBackupVaultEngine.listServerCloudBackups(phone)
        res.onSuccess { list ->
            cloudBackups = list
        }.onFailure { err ->
            cloudError = err.message ?: "تعذر تحميل قائمة النسخ السحابية"
        }
        isLoadingCloud = false
    }

    LaunchedEffect(phone) {
        loadBackups()
    }

    val openLocalOozoxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (content.isNullOrBlank()) {
                    validationError = "ملف النسخة الاحتياطية المختار فارغ."
                } else {
                    val preview = SecureBackupVaultEngine.inspectBackupMetadata(content, phone)
                    localVaultContent = content
                    localFileName = uri.lastPathSegment ?: "backup.oozox"
                    localPreview = preview
                    selectedCloudBackup = null
                    validationError = if (!preview.isValid) preview.errorMessage else null
                }
            } catch (e: Exception) {
                validationError = "تعذر قراءة الملف: ${e.localizedMessage}"
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "استعادة النسخة الاحتياطية",
                        tint = GoldPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "مرحباً بعودتك إلى OOZOX",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تم التحقق من رقم هاتفك ($phone). يمكنك استعادة محادثاتك وبياناتك والوسائط الآن أو التخطي والاستعادة لاحقاً من الإعدادات.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            }

            // Success Result Card
            val resSuccess = restoreResult as? BackupResult.Success
            if (resSuccess != null) {
                Surface(
                    color = StatusGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.2.dp, StatusGreen.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(40.dp))
                        Text(resSuccess.message, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusGreen, textAlign = TextAlign.Center)
                        Button(
                            onClick = onRestoreComplete,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("btn_restore_success_enter")
                        ) {
                            Text("الدخول إلى التطبيق", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
                return@Column
            }

            // Section 1: Real Server Cloud Backups
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = OozoxTeal)
                            Text("1. النسخ السحابية المشفرة", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = { scope.launch { loadBackups() } },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث النسخ السحابية", tint = OozoxTeal)
                        }
                    }

                    if (isLoadingCloud) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OozoxTeal, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري البحث عن نسخك المحفوظة على الخادم...", fontSize = 12.sp)
                        }
                    } else if (!cloudError.isNullOrBlank()) {
                        Surface(
                            color = StatusRed.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cloudError!!, fontSize = 11.5.sp, color = StatusRed, modifier = Modifier.weight(1f))
                                TextButton(onClick = { scope.launch { loadBackups() } }) {
                                    Text("إعادة المحاولة", color = OozoxTeal, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (cloudBackups.isEmpty()) {
                        Text(
                            text = "لا توجد نسخ احتياطية مرفوعة على السحابة لهذا الرقم.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        cloudBackups.forEach { backup ->
                            val isSelected = selectedCloudBackup?.id == backup.id
                            Surface(
                                onClick = {
                                    selectedCloudBackup = backup
                                    localVaultContent = null
                                    localPreview = null
                                    localFileName = null
                                    validationError = null
                                    restoreResult = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) OozoxTeal.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, if (isSelected) OozoxTeal else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cloud_backup_option_${backup.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = backup.fileName,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(backup.fileSizeFormatted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = OozoxTeal)
                                    }
                                    Text(
                                        text = "التاريخ: ${backup.createdAtText} • الجهاز: ${backup.deviceName.ifBlank { "Android" }}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "السجلات: ${backup.totalRecords} • المحادثات: ${backup.chatsCount} • الرسائل: ${backup.messagesCount} • الوسائط: ${backup.mediaFilesCount}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Local Encrypted .oozox File Picker
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = GoldPrimary)
                        Text("2. استيراد ملف .oozox من الهاتف", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { openLocalOozoxLauncher.launch(arrayOf("*/*")) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_pick_local_oozox")
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (localFileName != null) "الملف المختار: $localFileName" else "اختيار ملف نسخة احتياطية (.oozox)",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }

                    localPreview?.let { prev ->
                        Surface(
                            color = if (prev.isValid && prev.isPhoneMatched) StatusGreen.copy(alpha = 0.1f) else StatusRed.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (prev.isValid && prev.isPhoneMatched) StatusGreen.copy(alpha = 0.5f) else StatusRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!prev.isValid) {
                                    Text(prev.errorMessage ?: "ملف غير صالح", color = StatusRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text(
                                        text = if (prev.isPhoneMatched) "معاينة الملف: متطابق مع رقم هاتفك ✅" else "خطأ ملكية: الملف يتبع لرقم هاتف مختلف ⚠️",
                                        color = if (prev.isPhoneMatched) StatusGreen else StatusRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "التاريخ: ${prev.createdAtText} • الحجم: ${prev.sizeFormatted} • الإصدار V${prev.version}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "السجلات: ${prev.totalRecordsCount} • المحادثات: ${prev.chatsCount} • الرسائل: ${prev.messagesCount} • الوسائط: ${prev.mediaFilesCount}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Passphrase & Confirm Restore (when either cloud backup or valid local file is chosen)
            val readyCloud = selectedCloudBackup
            val readyLocal = localVaultContent?.takeIf { localPreview?.isValid == true && localPreview?.isPhoneMatched == true }

            if (readyCloud != null || readyLocal != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.2.dp, OozoxTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (readyCloud != null) "تأكيد استعادة السحابة: ${readyCloud.fileName}" else "تأكيد استعادة الملف: ${localFileName ?: ".oozox"}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = OozoxTeal
                        )

                        OutlinedTextField(
                            value = passphraseInput,
                            onValueChange = {
                                passphraseInput = it
                                validationError = null
                                restoreResult = null
                            },
                            label = { Text("كلمة مرور فك تشفير النسخة") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = GoldPrimary) },
                            trailingIcon = {
                                IconButton(onClick = { isPassVisible = !isPassVisible }) {
                                    Icon(if (isPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                                }
                            },
                            visualTransformation = if (isPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("restore_passphrase_input")
                        )

                        if (!validationError.isNullOrBlank()) {
                            Text(validationError!!, color = StatusRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        val errRes = restoreResult as? BackupResult.Error
                        if (errRes != null) {
                            Text(errRes.errorMessage, color = StatusRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val pass = passphraseInput.trim()
                                if (pass.length < 4) {
                                    validationError = "أدخل كلمة مرور فك التشفير (4 أحرف على الأقل)."
                                    return@Button
                                }
                                scope.launch {
                                    isRestoring = true
                                    restoreResult = null
                                    val db = AppDatabase.getInstance(context)
                                    val result = if (readyCloud != null) {
                                        val dl = SecureBackupVaultEngine.downloadServerCloudBackup(readyCloud.id)
                                        if (dl.isFailure) {
                                            BackupResult.Error(dl.exceptionOrNull()?.message ?: "تعذر تنزيل النسخة السحابية.")
                                        } else {
                                            SecureBackupVaultEngine.restoreEncryptedBackup(
                                                context = context,
                                                db = db,
                                                backupFileContent = dl.getOrNull()?.vaultContent.orEmpty(),
                                                currentUserPhone = phone,
                                                enteredPassphrase = pass
                                            )
                                        }
                                    } else {
                                        SecureBackupVaultEngine.restoreEncryptedBackup(
                                            context = context,
                                            db = db,
                                            backupFileContent = readyLocal!!,
                                            currentUserPhone = phone,
                                            enteredPassphrase = pass
                                        )
                                    }
                                    isRestoring = false
                                    restoreResult = result
                                }
                            },
                            enabled = !isRestoring,
                            colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.Black),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("btn_confirm_restore_choice")
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري التنزيل وفك التشفير والاستعادة...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Restore, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("فك التشفير والاستعادة الآن", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section 3: Clear Lossless Skip Action
            OutlinedButton(
                onClick = onSkip,
                enabled = !isRestoring,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("btn_skip_restore_choice")
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = null, tint = GoldPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("تخطي الآن (الاحتفاظ ببياناتي الحالية دون حذف)", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
