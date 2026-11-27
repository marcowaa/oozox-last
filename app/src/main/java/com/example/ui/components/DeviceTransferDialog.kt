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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OozoxTeal
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.util.ApiTransferInitiateRequest
import com.example.util.ApiTransferPendingItemDto
import com.example.util.CrossDeviceTransferEngine
import com.example.util.RetrofitClient
import com.example.util.TransferProgressState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * High-Security ECDH P-256 + HKDF-SHA256 + AES-256-GCM Device-to-Device Migration & Transfer Dialog.
 */
@Composable
fun DeviceTransferDialog(
    currentUser: UserEntity?,
    onDismiss: () -> Unit,
    onMigrateCompleteOnNewPhone: (name: String, phone: String, username: String, type: UserType) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var transferRoleTab by remember { mutableIntStateOf(if (currentUser != null) 0 else 1) }
    var showQrScanner by remember { mutableStateOf(false) }

    // Old device state
    var pendingRequests by remember { mutableStateOf<List<ApiTransferPendingItemDto>>(emptyList()) }
    var isLoadingPending by remember { mutableStateOf(false) }
    var oldSasCode by remember { mutableStateOf<String?>(null) }
    var oldProgress by remember { mutableStateOf<TransferProgressState?>(null) }
    var oldSuccessMessage by remember { mutableStateOf<String?>(null) }

    // New device state
    val targetKeyPair = remember { CrossDeviceTransferEngine.generateEphemeralKeyPair() }
    val targetPubB64 = remember(targetKeyPair) { CrossDeviceTransferEngine.encodePublicKeyBase64(targetKeyPair.public) }
    var activeTransferId by remember { mutableStateOf<String?>(null) }
    var activeQrPayload by remember { mutableStateOf<String?>(null) }
    var newSasCode by remember { mutableStateOf<String?>(null) }
    var newProgress by remember { mutableStateOf<TransferProgressState?>(null) }
    var hasStartedDownload by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun loadPendingOnOldPhone() {
        if (!RetrofitClient.hasActiveSession()) return
        isLoadingPending = true
        try {
            val res = RetrofitClient.apiService().getPendingDeviceTransfers()
            if (res.isSuccessful && res.body()?.success == true) {
                pendingRequests = res.body()?.data?.pending.orEmpty()
            }
        } catch (_: Exception) {
        } finally {
            isLoadingPending = false
        }
    }

    LaunchedEffect(transferRoleTab) {
        if (transferRoleTab == 0) {
            loadPendingOnOldPhone()
        } else if (activeTransferId == null && RetrofitClient.hasActiveSession()) {
            try {
                val res = RetrofitClient.apiService().initiateDeviceTransfer(
                    ApiTransferInitiateRequest(targetEcdhPubKey = targetPubB64)
                )
                val data = res.body()?.data
                if (res.isSuccessful && res.body()?.success == true && data != null) {
                    activeTransferId = data.transferId
                    activeQrPayload = data.qrPayload
                } else {
                    errorMessage = res.body()?.message ?: "تعذر بدء جلسة الاستقبال"
                }
            } catch (e: Exception) {
                errorMessage = com.example.util.AppErrorMapper.fromThrowable(
                    e,
                    fallbackAr = "تعذر بدء جلسة النقل، تحقق من الإنترنت",
                    fallbackEn = "Couldn't start transfer session"
                )
            }
        }
    }

    // Poll new device transfer session while on tab 1
    LaunchedEffect(transferRoleTab, activeTransferId) {
        val id = activeTransferId ?: return@LaunchedEffect
        if (transferRoleTab != 1) return@LaunchedEffect
        while (!hasStartedDownload) {
            delay(2000L)
            try {
                val res = RetrofitClient.apiService().getDeviceTransferStatus(id)
                val st = res.body()?.data ?: continue
                val peerPub = st.sourceEcdhPubKey
                if (!peerPub.isNullOrBlank() && newSasCode == null) {
                    val key = CrossDeviceTransferEngine.deriveAesSessionKey(targetKeyPair.private, peerPub, id)
                    newSasCode = CrossDeviceTransferEngine.computeSasCode(key, id, targetPubB64, peerPub)
                }
                if (st.status == "UPLOADING" && st.totalChunks > 0) {
                    newProgress = TransferProgressState(
                        phase = "UPLOADING",
                        progressPercent = 15 + (st.uploadedChunkIndexes.size * 50 / st.totalChunks),
                        message = "جاري استقبال الحزم المشفرة (${st.uploadedChunkIndexes.size}/${st.totalChunks})..."
                    )
                }
                if (st.status == "READY_FOR_DOWNLOAD" && !hasStartedDownload) {
                    hasStartedDownload = true
                    val dl = CrossDeviceTransferEngine.downloadDecryptAndRestore(
                        context = context,
                        statusData = st,
                        targetKeyPair = targetKeyPair,
                        onProgress = { newProgress = it }
                    )
                    dl.onSuccess {
                        onMigrateCompleteOnNewPhone(
                            currentUser?.name ?: "مستخدم أوزوكس",
                            currentUser?.phone ?: "",
                            currentUser?.username ?: "",
                            UserType.entries.find { t -> t.name == currentUser?.type } ?: UserType.BUSINESSMAN
                        )
                    }.onFailure { err ->
                        errorMessage = com.example.util.AppErrorMapper.fromThrowable(
                            err,
                            fallbackAr = "تعذر إكمال نقل البيانات، حاول مجدداً",
                            fallbackEn = "Couldn't complete transfer"
                        )
                        hasStartedDownload = false
                    }
                    break
                }
            } catch (_: Exception) {
            }
        }
    }

    fun startOldPhoneUpload(
        transferId: String,
        targetEcdhPubKey: String,
        approvalMethod: String,
        qrNonce: String? = null
    ) {
        scope.launch {
            errorMessage = null
            oldSuccessMessage = null
            val res = CrossDeviceTransferEngine.approveAndUploadFullAccount(
                context = context,
                transferId = transferId,
                targetEcdhPubKey = targetEcdhPubKey,
                ownerPhone = currentUser?.phone.orEmpty(),
                approvalMethod = approvalMethod,
                qrNonce = qrNonce,
                onSasReady = { oldSasCode = it },
                onProgress = { oldProgress = it }
            )
            res.onSuccess { summary ->
                oldSuccessMessage = "تم إرسال كافة البيانات المشفرة (${summary.totalRecords} سجل و ${summary.mediaFilesCount} وسائط). سيتم تسجيل خروج هذا الجهاز فور تأكيد الاستلام."
                loadPendingOnOldPhone()
            }.onFailure { err ->
                errorMessage = com.example.util.AppErrorMapper.fromThrowable(
                    err,
                    fallbackAr = "تعذر تفويض أو رفع البيانات",
                    fallbackEn = "Couldn't upload transfer data"
                )
            }
        }
    }

    if (showQrScanner) {
        QrScannerDialog(
            onQrScanned = { rawText ->
                showQrScanner = false
                val parsed = CrossDeviceTransferEngine.parseTransferQrPayload(rawText)
                if (parsed == null) {
                    errorMessage = "رمز QR غير صالح أو ليس رمز نقل OOZOX."
                } else {
                    startOldPhoneUpload(
                        transferId = parsed.transferId,
                        targetEcdhPubKey = parsed.targetEcdhPubKey,
                        approvalMethod = "QR_SCAN",
                        qrNonce = parsed.qrNonce
                    )
                }
            },
            onDismiss = { showQrScanner = false }
        )
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
                            Icon(Icons.Default.PhonelinkSetup, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(20.dp))
                        }

                        Column {
                            Text("نقل الحساب بالكامل بين الهواتف", fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                            Text("ECDH P-256 + AES-256-GCM + SAS", fontSize = 11.sp, color = OozoxTeal)
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                TabRow(
                    selectedTabIndex = transferRoleTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = OozoxTeal,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = transferRoleTab == 0,
                        onClick = { transferRoleTab = 0; errorMessage = null },
                        text = { Text("الهاتف القديم (تفويض وإرسال)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = transferRoleTab == 1,
                        onClick = { transferRoleTab = 1; errorMessage = null },
                        text = { Text("الهاتف الجديد (استقبال QR)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (transferRoleTab == 0) {
                        Surface(
                            color = OozoxTeal.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 84.dp)
                                .clickable { showQrScanner = true }
                                .testTag("btn_old_phone_scan_qr")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = OozoxTeal, modifier = Modifier.size(30.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("مسح رمز QR المعروض على الهاتف الجديد", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OozoxTeal)
                                    Text("أو الموافقة الفورية على طلب النقل أدناه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("طلبات النقل الواردة (${pendingRequests.size})", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { scope.launch { loadPendingOnOldPhone() } }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تحديث", fontSize = 12.sp)
                            }
                        }

                        if (isLoadingPending) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = OozoxTeal)
                        } else if (pendingRequests.isEmpty()) {
                            Text(
                                text = "لا توجد طلبات نقل معلقة حالياً. عند تسجيل الدخول على الهاتف الجديد سيظهر الطلب هنا أو يمكنك مسح QR مباشرة.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            pendingRequests.forEach { req ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("جهاز يطلب النقل: ${req.targetDeviceName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("ينتهي الطلب: ${req.expiresAt.take(19)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Button(
                                            onClick = {
                                                startOldPhoneUpload(
                                                    transferId = req.transferId,
                                                    targetEcdhPubKey = req.targetEcdhPubKey,
                                                    approvalMethod = "DEVICE_PUSH_APPROVAL"
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.Black),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                                        ) {
                                            Text("الموافقة ونقل كامل الحساب والوسائط", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        oldSasCode?.let { sas ->
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GoldPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("رمز التحقق المتطابق (SAS):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                    Text(sas, fontSize = 22.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        oldProgress?.let { prog ->
                            LinearProgressIndicator(
                                progress = { (prog.progressPercent / 100f).coerceIn(0f, 1f) },
                                color = OozoxTeal,
                                modifier = Modifier.fillMaxWidth().height(7.dp)
                            )
                            Text("${prog.message} (${prog.progressPercent}%)", fontSize = 11.5.sp, color = OozoxTeal)
                        }

                        oldSuccessMessage?.let { msg ->
                            Text(msg, color = StatusGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    } else {
                        if (!activeQrPayload.isNullOrBlank()) {
                            QrMatrixRenderer(content = activeQrPayload!!, size = 195.dp, accentColor = OozoxTeal)
                            Text(
                                text = "امسح هذا الرمز من الهاتف القديم لإرسال المحادثات والوسائط بتشفير ECDH:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            CircularProgressIndicator(color = OozoxTeal)
                        }

                        newSasCode?.let { sas ->
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GoldPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("رمز التحقق المتطابق (SAS):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                    Text(sas, fontSize = 22.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        newProgress?.let { prog ->
                            LinearProgressIndicator(
                                progress = { (prog.progressPercent / 100f).coerceIn(0f, 1f) },
                                color = OozoxTeal,
                                modifier = Modifier.fillMaxWidth().height(7.dp)
                            )
                            Text("${prog.message} (${prog.progressPercent}%)", fontSize = 11.5.sp, color = OozoxTeal)
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Text(errorMessage!!, color = StatusRed, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }

                OutlinedButton(
                    onClick = {
                        val id = activeTransferId
                        if (!id.isNullOrBlank()) {
                            scope.launch { runCatching { RetrofitClient.apiService().cancelDeviceTransfer(id) } }
                        }
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("إغلاق", fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Automatic Old-Device Incoming Transfer Request Approval Modal.
 * Triggered when another verified phone of the same user requests full account & media transfer.
 */
@Composable
fun IncomingTransferApprovalDialog(
    pendingItem: ApiTransferPendingItemDto,
    ownerPhone: String,
    onApprovedAndUploaded: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showQrScanner by remember { mutableStateOf(false) }
    var sasCode by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<TransferProgressState?>(null) }
    var isWorking by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    if (showQrScanner) {
        QrScannerDialog(
            onQrScanned = { rawText ->
                showQrScanner = false
                val parsed = CrossDeviceTransferEngine.parseTransferQrPayload(rawText)
                if (parsed == null || parsed.transferId != pendingItem.transferId) {
                    errorMsg = "رمز QR غير مطابق لطلب النقل الحالي."
                } else {
                    scope.launch {
                        isWorking = true
                        errorMsg = null
                        val res = CrossDeviceTransferEngine.approveAndUploadFullAccount(
                            context = context,
                            transferId = parsed.transferId,
                            targetEcdhPubKey = parsed.targetEcdhPubKey,
                            ownerPhone = ownerPhone,
                            approvalMethod = "QR_SCAN",
                            qrNonce = parsed.qrNonce,
                            onSasReady = { sasCode = it },
                            onProgress = { progress = it }
                        )
                        isWorking = false
                        res.onSuccess { onApprovedAndUploaded() }
                            .onFailure { errorMsg = it.message }
                    }
                }
            },
            onDismiss = { showQrScanner = false }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PhonelinkSetup, contentDescription = null, tint = OozoxTeal)
                Text("طلب نقل حساب أوزوكس", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "سجّل جهاز جديد (${pendingItem.targetDeviceName}) الدخول برقمك ويطلب نقل كامل المحادثات والوسائط والقيود بتشفير E2EE. سيبقى هذا الهاتف متصلاً أثناء النقل ثم يتم تسجيل الخروج تلقائياً عند الاكتمال.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                sasCode?.let { sas ->
                    Surface(
                        color = GoldPrimary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, GoldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("تطابق رمز الأمان (SAS):", fontSize = 11.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                            Text(sas, fontSize = 22.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                progress?.let { prog ->
                    LinearProgressIndicator(
                        progress = { (prog.progressPercent / 100f).coerceIn(0f, 1f) },
                        color = OozoxTeal,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${prog.message} (${prog.progressPercent}%)", fontSize = 11.5.sp, color = OozoxTeal)
                }

                if (!errorMsg.isNullOrBlank()) {
                    Text(errorMsg!!, color = StatusRed, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showQrScanner = true },
                    enabled = !isWorking,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = OozoxTeal)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("التفويض بمسح QR من الهاتف الجديد", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isWorking = true
                        errorMsg = null
                        val res = CrossDeviceTransferEngine.approveAndUploadFullAccount(
                            context = context,
                            transferId = pendingItem.transferId,
                            targetEcdhPubKey = pendingItem.targetEcdhPubKey,
                            ownerPhone = ownerPhone,
                            approvalMethod = "DEVICE_PUSH_APPROVAL",
                            onSasReady = { sasCode = it },
                            onProgress = { progress = it }
                        )
                        isWorking = false
                        res.onSuccess { onApprovedAndUploaded() }
                            .onFailure { errorMsg = it.message }
                    }
                },
                enabled = !isWorking,
                colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.heightIn(min = 46.dp)
            ) {
                Text(if (isWorking) "جاري النقل..." else "موافقة ونقل الآن", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!isWorking) onReject() },
                enabled = !isWorking
            ) {
                Text("رفض الطلب", color = StatusRed)
            }
        }
    )
}
