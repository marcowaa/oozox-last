package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AccountingI18n
import com.example.util.BackupResult
import com.example.util.CloudStorageProvider
import com.example.util.LocalAppLanguage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.SecureBackupVaultEngine

@Composable
fun CloudBackupAuthSection(
    viewModel: MainViewModel? = null,
    recordsCount: Int = 0,
    groupsCount: Int = 0,
    profilesCount: Int = 0,
    companyName: String = "المركز المالي الشامل",
    onExportUnifiedExcel: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentLang = LocalAppLanguage.current
    val str: (String) -> String = { key -> AccountingI18n.get(key, currentLang) }
    val currentUser = viewModel?.currentUser?.collectAsStateWithLifecycle()?.value
    val cloudBackups = viewModel?.cloudBackups?.collectAsStateWithLifecycle()?.value ?: emptyList()
    val backupStatus = viewModel?.backupStatus?.collectAsStateWithLifecycle()?.value
    val isProcessing = viewModel?.isBackupProcessing?.collectAsStateWithLifecycle()?.value ?: false

    var backupPassphrase by remember { mutableStateOf("") }
    var restorePassphrase by remember { mutableStateOf("") }
    var isPassVisible by remember { mutableStateOf(false) }
    var selectedBackupScope by remember { mutableIntStateOf(0) } // 0 = Full Cloud + File, 1 = Local Vault File, 2 = Sub-Accounts Only
    var pendingRestoreContent by remember { mutableStateOf<String?>(null) }
    var pendingRestoreFileName by remember { mutableStateOf<String?>(null) }
    var pendingRestorePassphrase by remember { mutableStateOf("") }

    val primaryGold = adaptiveGoldPrimary
    val onGold = adaptiveOnPrimary
    val greenColor = adaptiveStatusGreen
    val redColor = adaptiveStatusRed
    val blueColor = adaptiveStatusBlue

    LaunchedEffect(currentUser?.phone) {
        viewModel?.refreshCloudBackups()
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    pendingRestoreContent = content
                    pendingRestoreFileName = uri.lastPathSegment ?: ".oozox"
                    pendingRestorePassphrase = restorePassphrase
                } else {
                    Toast.makeText(context, str("file_empty_error"), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, str("export_error"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Security Hero & Snapshot Readiness Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, primaryGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = str("sec_backup"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryGold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "$companyName • ${str("encrypted_badge")}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentUser != null) {
                                Text(
                                    "${currentUser.name} • ${currentUser.phone}",
                                    fontSize = 11.sp,
                                    color = greenColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Circular Shield Readiness Gauge
                        Box(
                            modifier = Modifier.size(58.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val readiness = if (cloudBackups.isNotEmpty()) 1.0f else 0.72f
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 5.5.dp.toPx()
                                drawArc(
                                    color = primaryGold.copy(alpha = 0.2f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = if (cloudBackups.isNotEmpty()) greenColor else primaryGold,
                                    startAngle = -90f,
                                    sweepAngle = 360f * readiness,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                            Icon(
                                imageVector = if (cloudBackups.isNotEmpty()) Icons.Default.CloudDone else Icons.Default.VerifiedUser,
                                contentDescription = str("sec_backup"),
                                tint = if (cloudBackups.isNotEmpty()) greenColor else primaryGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BackupMetricPill(str("sec_ledger"), "$recordsCount", primaryGold, Modifier.weight(1f))
                        BackupMetricPill(str("sec_groups"), "$groupsCount", blueColor, Modifier.weight(1f))
                        BackupMetricPill(str("sec_vaults"), "$profilesCount", greenColor, Modifier.weight(1f))
                    }
                }
            }
        }

        // Result Status Banner
        if (backupStatus != null) {
            item {
                val isSuccess = backupStatus is BackupResult.Success
                Surface(
                    color = (if (isSuccess) greenColor else redColor).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, (if (isSuccess) greenColor else redColor).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isSuccess) greenColor else redColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (backupStatus) {
                                    is BackupResult.Success -> backupStatus.message
                                    is BackupResult.Error -> backupStatus.errorMessage
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel?.clearBackupStatus() },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.Close, contentDescription = str("close"), modifier = Modifier.size(18.dp))
                            }
                        }

                        if (backupStatus is BackupResult.Success && backupStatus.uri != null) {
                            Button(
                                onClick = { SecureBackupVaultEngine.shareBackupFile(context, backupStatus.uri) },
                                colors = ButtonDefaults.buttonColors(containerColor = greenColor, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(str("share_backup"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Create Encrypted Backup Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = primaryGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(str("create_backup"), fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            0 to str("scope_full"),
                            1 to str("scope_local"),
                            2 to str("scope_vaults")
                        ).forEach { (idx, label) ->
                            FilterChip(
                                selected = selectedBackupScope == idx,
                                onClick = { selectedBackupScope = idx },
                                label = { Text(label, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryGold,
                                    selectedLabelColor = onGold
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = backupPassphrase,
                        onValueChange = { backupPassphrase = it },
                        label = { Text(str("backup_passcode")) },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = primaryGold) },
                        trailingIcon = {
                            IconButton(
                                onClick = { isPassVisible = !isPassVisible },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (isPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = str("backup_passcode")
                                )
                            }
                        },
                        visualTransformation = if (isPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cloud_backup_passphrase_input")
                    )

                    Button(
                        onClick = {
                            if (viewModel == null) {
                                Toast.makeText(context, str("sec_backup"), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (backupPassphrase.trim().length < 4) {
                                Toast.makeText(context, str("passcode_short"), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            when (selectedBackupScope) {
                                2 -> viewModel.createSubAccountsMigrationBackup(
                                    passphrase = backupPassphrase.trim(),
                                    cloudProvider = CloudStorageProvider.OOZOX_CLOUD
                                )
                                1 -> viewModel.createEncryptedBackup(
                                    passphrase = backupPassphrase.trim(),
                                    isGoogleDriveCloudExport = false
                                )
                                else -> viewModel.createEncryptedBackup(
                                    passphrase = backupPassphrase.trim(),
                                    isGoogleDriveCloudExport = true
                                )
                            }
                        },
                        enabled = !isProcessing && backupPassphrase.trim().length >= 4,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGold, contentColor = onGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("create_encrypted_backup_btn")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = onGold, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(str("encrypting"), fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(str("create_backup"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Restore Encrypted Backup Card + Unified Excel Export
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restore, contentDescription = null, tint = blueColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(str("restore_backup"), fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = restorePassphrase,
                        onValueChange = { restorePassphrase = it },
                        label = { Text(str("backup_passcode")) },
                        leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = blueColor) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { openBackupLauncher.launch(arrayOf("*/*")) },
                            enabled = !isProcessing,
                            border = BorderStroke(1.dp, blueColor.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("restore_backup_file_btn")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = blueColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(str("pick_backup_file"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = blueColor)
                        }

                        OutlinedButton(
                            onClick = onExportUnifiedExcel,
                            border = BorderStroke(1.dp, greenColor.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("backup_tab_export_excel_btn")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = greenColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(str("cmd_excel"), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = greenColor)
                        }
                    }
                }
            }
        }

        // Cloud Snapshot History Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${str("cloud_snapshots")} (${cloudBackups.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryGold
                )
                TextButton(
                    onClick = { viewModel?.refreshCloudBackups() },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(str("refresh"), fontSize = 12.sp)
                }
            }
        }

        if (cloudBackups.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(str("no_cloud_backups"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            items(cloudBackups, key = { it.id }) { snapshot ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(color = greenColor.copy(alpha = 0.14f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = greenColor, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(snapshot.fileName, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${snapshot.createdAtText} • ${snapshot.fileSizeFormatted} • ${snapshot.totalRecords} سجل",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    if (snapshot.encryptedPayloadBase64.isNotBlank()) {
                                        pendingRestoreContent = snapshot.encryptedPayloadBase64
                                        pendingRestoreFileName = snapshot.fileName
                                        pendingRestorePassphrase = restorePassphrase
                                    } else {
                                        viewModel?.downloadCloudBackupContent(snapshot.id) { res ->
                                            res.onSuccess { content ->
                                                pendingRestoreContent = content
                                                pendingRestoreFileName = snapshot.fileName
                                                pendingRestorePassphrase = restorePassphrase
                                            }.onFailure { err ->
                                                Toast.makeText(
                                                    context,
                                                    com.example.util.AppErrorMapper.fromThrowable(
                                                        err,
                                                        fallbackAr = "تعذر تنزيل النسخة السحابية",
                                                        fallbackEn = "Couldn't download cloud backup"
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                enabled = !isProcessing,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = "استعادة هذه النسخة", tint = blueColor)
                            }
                            IconButton(
                                onClick = { viewModel?.deleteCloudBackup(snapshot.id) },
                                enabled = !isProcessing,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف النسخة", tint = redColor)
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirm Restore Dialog with Passphrase
    if (pendingRestoreContent != null) {
        AlertDialog(
            onDismissRequest = { pendingRestoreContent = null },
            shape = RoundedCornerShape(16.dp),
            title = { Text(str("restore_backup"), fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        pendingRestoreFileName ?: ".oozox",
                        fontSize = 12.sp,
                        color = primaryGold,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    OutlinedTextField(
                        value = pendingRestorePassphrase,
                        onValueChange = { pendingRestorePassphrase = it },
                        label = { Text(str("backup_passcode")) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val content = pendingRestoreContent ?: return@Button
                        val pass = pendingRestorePassphrase.trim()
                        if (pass.length < 4) {
                            Toast.makeText(context, str("passcode_short"), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel?.restoreEncryptedBackup(content, pass) { result ->
                            if (result is BackupResult.Success) {
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                            }
                        }
                        pendingRestoreContent = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = blueColor, contentColor = Color.White),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(str("restore_now"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingRestoreContent = null },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(str("cancel"))
                }
            }
        )
    }
}

@Composable
private fun BackupMetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = accent)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
