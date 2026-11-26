package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HiddenProfileEntity
import com.example.ui.theme.OozoxTeal
import com.example.util.BackupMetadataPreview
import com.example.util.BackupResult
import com.example.util.CloudBackupRecord
import com.example.util.CloudStorageProvider
import com.example.util.SecureBackupVaultEngine

@Composable
internal fun ProfileCreateBackupDialog(
    userPhone: String,
    onStartBackup: (passphrase: String, isGoogleDrive: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var backupPassphrase by remember { mutableStateOf("") }
    var backupPassphraseConfirm by remember { mutableStateOf("") }
    var backupIsGoogleDrive by remember { mutableStateOf(false) }
    var backupValidationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = OozoxTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إنشاء نسخة احتياطية مشفرة", color = OozoxTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "سيتم تشفير جميع بيانات حسابك واليوميات والصفقات والمجموعات بتشفير AES-256 وربطها برقم هاتفك المسجل ($userPhone).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                OutlinedTextField(
                    value = backupPassphrase,
                    onValueChange = {
                        backupPassphrase = it
                        backupValidationError = null
                    },
                    label = { Text("كلمة المرور", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = backupPassphraseConfirm,
                    onValueChange = {
                        backupPassphraseConfirm = it
                        backupValidationError = null
                    },
                    label = { Text("تأكيد كلمة المرور", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { backupIsGoogleDrive = !backupIsGoogleDrive }
                ) {
                    Checkbox(
                        checked = backupIsGoogleDrive,
                        onCheckedChange = { backupIsGoogleDrive = it },
                        colors = CheckboxDefaults.colors(checkedColor = OozoxTeal)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تجهيز نسخة قابلة للمشاركة (.oozox)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                if (backupValidationError != null) {
                    Text(backupValidationError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("إلغاء")
                }

                Button(
                    onClick = {
                        if (backupPassphrase.length < 4) {
                            backupValidationError = "كلمة المرور يجب أن تكون 4 أحرف أو أرقام على الأقل."
                        } else if (backupPassphrase != backupPassphraseConfirm) {
                            backupValidationError = "كلمتا المرور غير متطابقتين."
                        } else {
                            onDismiss()
                            onStartBackup(backupPassphrase, backupIsGoogleDrive)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("بدء النسخ", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {},
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
internal fun ProfileSubAccountsMigrationDialog(
    hiddenProfiles: List<HiddenProfileEntity>,
    onCreateSubAccountsBackup: (passphrase: String, selectedProfileIds: List<String>?, provider: CloudStorageProvider) -> Unit,
    onDismiss: () -> Unit
) {
    var subAccountsPassphrase by remember { mutableStateOf("") }
    var subAccountsPassphraseConfirm by remember { mutableStateOf("") }
    var selectedCloudProvider by remember { mutableStateOf(CloudStorageProvider.GOOGLE_DRIVE) }
    var selectedSubProfileIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectAllSubProfiles by remember { mutableStateOf(true) }
    var subAccountsMigrationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DriveFolderUpload, contentDescription = null, tint = OozoxTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ترحيل الحسابات الفرعية والخزائن ⚡", color = OozoxTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "تتيح لك هذه الأداة تصدير وترحيل الحسابات الفرعية والخزائن السرية واليوميات المحاسبية المرتبطة بها في ملف .oozox مشفر ومشاركته يدوياً مع هاتفك الجديد.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                Text("وجهة الحفظ المحلي / التصدير:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CloudStorageProvider.entries.forEach { provider ->
                        val isSelected = selectedCloudProvider == provider
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) OozoxTeal.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) BorderStroke(1.dp, OozoxTeal) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedCloudProvider = provider }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = when (provider) {
                                        CloudStorageProvider.GOOGLE_DRIVE -> Icons.Default.Share
                                        CloudStorageProvider.OOZOX_CLOUD -> Icons.Default.EnhancedEncryption
                                        CloudStorageProvider.LOCAL_DEVICE -> Icons.Default.FolderSpecial
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) OozoxTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (provider) {
                                        CloudStorageProvider.GOOGLE_DRIVE -> "تصدير Share"
                                        CloudStorageProvider.OOZOX_CLOUD -> "خزينة الجهاز"
                                        CloudStorageProvider.LOCAL_DEVICE -> "ملف .oozox"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) OozoxTeal else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (hiddenProfiles.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تحديد الحسابات الفرعية (${hiddenProfiles.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        TextButton(
                            onClick = {
                                if (selectAllSubProfiles) {
                                    selectedSubProfileIds = emptySet()
                                    selectAllSubProfiles = false
                                } else {
                                    selectedSubProfileIds = hiddenProfiles.map { it.id }.toSet()
                                    selectAllSubProfiles = true
                                }
                            }
                        ) {
                            Text(if (selectAllSubProfiles) "إلغاء التحديد" else "تحديد الكل", fontSize = 11.sp, color = OozoxTeal)
                        }
                    }

                    hiddenProfiles.forEach { profile ->
                        val isChecked = selectedSubProfileIds.contains(profile.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSubProfileIds = if (isChecked) {
                                        selectedSubProfileIds - profile.id
                                    } else {
                                        selectedSubProfileIds + profile.id
                                    }
                                    selectAllSubProfiles = selectedSubProfileIds.size == hiddenProfiles.size
                                }
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedSubProfileIds = if (checked) {
                                        selectedSubProfileIds + profile.id
                                    } else {
                                        selectedSubProfileIds - profile.id
                                    }
                                    selectAllSubProfiles = selectedSubProfileIds.size == hiddenProfiles.size
                                },
                                colors = CheckboxDefaults.colors(checkedColor = OozoxTeal)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(profile.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("الشركة: ${profile.companyName} | ${profile.roleTitle}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = subAccountsPassphrase,
                    onValueChange = {
                        subAccountsPassphrase = it
                        subAccountsMigrationError = null
                    },
                    label = { Text("كلمة المرور", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subAccountsPassphraseConfirm,
                    onValueChange = {
                        subAccountsPassphraseConfirm = it
                        subAccountsMigrationError = null
                    },
                    label = { Text("تأكيد كلمة المرور", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                if (subAccountsMigrationError != null) {
                    Text(subAccountsMigrationError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("إلغاء")
                }

                Button(
                    onClick = {
                        if (subAccountsPassphrase.length < 4) {
                            subAccountsMigrationError = "يجب أن تكون كلمة المرور 4 أحرف أو أرقام على الأقل."
                        } else if (subAccountsPassphrase != subAccountsPassphraseConfirm) {
                            subAccountsMigrationError = "كلمتا المرور غير متطابقتين."
                        } else {
                            onDismiss()
                            val chosenIds = if (hiddenProfiles.isNotEmpty() && !selectAllSubProfiles) {
                                selectedSubProfileIds.toList()
                            } else null
                            onCreateSubAccountsBackup(subAccountsPassphrase, chosenIds, selectedCloudProvider)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("بدء الترحيل", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {},
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
internal fun ProfileCloudBackupsVaultDialog(
    userPhone: String,
    cloudBackups: List<CloudBackupRecord>,
    onRestoreBackup: (CloudBackupRecord) -> Unit,
    onDeleteCloudBackup: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FolderZip, contentDescription = null, tint = OozoxTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("خزينة سحابة أوزوكس المشفرة", color = OozoxTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "النسخ الاحتياطية المشفرة المحفوظة على الخادم والمربوطة بحساب رقم هاتفك ($userPhone). يتم تنزيلها والتحقق من بصمتها SHA-256 قبل فك التشفير.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                if (cloudBackups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("لا توجد نسخ احتياطية مرفوعة على الخادم لهذا الحساب حتى الآن.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    cloudBackups.forEach { backup ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.EnhancedEncryption,
                                            contentDescription = null,
                                            tint = OozoxTeal,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(backup.provider.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }

                                    Text(backup.fileSizeFormatted, fontSize = 11.sp, color = OozoxTeal, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("تاريخ الرفع: ${backup.createdAtText}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("السجلات: ${backup.totalRecords} قيد | الحسابات: ${backup.subProfilesCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onRestoreBackup(backup) },
                                        colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("استعادة النسخة", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { onDeleteCloudBackup(backup.id) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
internal fun ProfileRestoreBackupDialog(
    userPhone: String,
    restoreFileContent: String?,
    restoreFileName: String?,
    restorePassphrase: String,
    onRestorePassphraseChange: (String) -> Unit,
    restoreValidationError: String?,
    onRestoreValidationErrorChange: (String?) -> Unit,
    backupMetadataPreview: BackupMetadataPreview?,
    onPickFileClick: () -> Unit,
    onConfirmRestore: (fileContent: String, passphrase: String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Restore, contentDescription = null, tint = OozoxTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("استعادة نسخة احتياطية مشفرة 🔄", color = OozoxTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "اختر ملف النسخة الاحتياطية (.oozox) أو اختر من النسخ السحابية. سيتحقق النظام تلقائياً من أن النسخة مشفرة ومربوطة برقم هاتفك ($userPhone).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                OutlinedButton(
                    onClick = onPickFileClick,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, if (restoreFileContent != null) OozoxTeal else MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, tint = if (restoreFileContent != null) OozoxTeal else MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (restoreFileName != null) "الملف: $restoreFileName ✅" else "اختر ملف النسخة من الهاتف 📁",
                        color = if (restoreFileContent != null) OozoxTeal else MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }

                backupMetadataPreview?.let { preview ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (preview.isPhoneMatched) OozoxTeal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, if (preview.isPhoneMatched) OozoxTeal.copy(alpha = 0.6f) else MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (preview.isPhoneMatched) "معاينة النسخة: متوافقة مع رقم هاتفك ✅" else "تحذير: النسخة تتبع لرقم هاتف آخر ⚠️",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (preview.isPhoneMatched) OozoxTeal else MaterialTheme.colorScheme.error
                                )
                                Text(preview.createdAtText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("نوع النسخة: ${preview.backupType}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("مالك النسخة: ${preview.ownerPhoneMasked} | السجلات: ${preview.totalRecordsCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                OutlinedTextField(
                    value = restorePassphrase,
                    onValueChange = {
                        onRestorePassphraseChange(it)
                        onRestoreValidationErrorChange(null)
                    },
                    label = { Text("كلمة المرور", style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )

                if (restoreValidationError != null) {
                    Text(restoreValidationError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("إلغاء")
                }

                Button(
                    onClick = {
                        val content = restoreFileContent
                        if (content == null) {
                            onRestoreValidationErrorChange("يرجى اختيار ملف النسخة الاحتياطية أولاً.")
                        } else if (restorePassphrase.isEmpty()) {
                            onRestoreValidationErrorChange("يرجى إدخال كلمة مرور فك التشفير.")
                        } else {
                            onDismiss()
                            onConfirmRestore(content, restorePassphrase)
                        }
                    },
                    enabled = restoreFileContent != null,
                    colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Text("استعادة النسخة", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {},
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
internal fun ProfileBackupProgressAndResultDialogs(
    isBackupProcessing: Boolean,
    backupStatus: BackupResult?,
    onClearBackupStatus: () -> Unit
) {
    val context = LocalContext.current
    if (isBackupProcessing) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("جاري معالجة التشفير والبيانات... ⏳", color = OozoxTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = OozoxTeal)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("جاري معالجة البيانات بنظام الأمان العالي...", fontSize = 13.sp)
                }
            },
            confirmButton = {},
            containerColor = MaterialTheme.colorScheme.surface
        )
    } else if (backupStatus != null) {
        AlertDialog(
            onDismissRequest = onClearBackupStatus,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isSuccess = backupStatus is BackupResult.Success
                    Icon(
                        if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isSuccess) OozoxTeal else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isSuccess) "تم بنجاح! 🛡️✨" else "تنبيه أمني / خطأ ⚠️",
                        color = if (isSuccess) OozoxTeal else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val msg = when (backupStatus) {
                        is BackupResult.Success -> backupStatus.message
                        is BackupResult.Error -> backupStatus.errorMessage
                    }
                    Text(msg, fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurface)

                    if (backupStatus is BackupResult.Success && backupStatus.uri != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                SecureBackupVaultEngine.shareBackupFile(context, backupStatus.uri)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة النسخة الاحتياطية", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onClearBackupStatus) {
                    Text("حسناً", color = OozoxTeal, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
