package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.QrMatrixRenderer
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OozoxTeal
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.util.ApiTransferInitiateRequest
import com.example.util.CrossDeviceTransferEngine
import com.example.util.RetrofitClient
import com.example.util.TransferCompletionStats
import com.example.util.TransferProgressState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Post-OTP Cross-Device Encrypted Account & Media Transfer Gateway.
 * Shown ONLY when the backend reports `hasOtherActiveDevice == true`.
 */
@Composable
fun PostOtpTransferGatewayScreen(
    phone: String,
    isExistingUser: Boolean,
    otherDeviceSummary: String,
    onTransferCompleted: () -> Unit,
    onSkipTransfer: (isExistingUser: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val targetKeyPair = remember { CrossDeviceTransferEngine.generateEphemeralKeyPair() }
    val targetPubBase64 = remember(targetKeyPair) { CrossDeviceTransferEngine.encodePublicKeyBase64(targetKeyPair.public) }

    var transferId by remember { mutableStateOf<String?>(null) }
    var qrPayload by remember { mutableStateOf<String?>(null) }
    var transferStatus by remember { mutableStateOf("INITIATING") }
    var sourceDeviceLabel by remember { mutableStateOf(otherDeviceSummary.ifBlank { "الهاتف السابق" }) }
    var sasCode by remember { mutableStateOf<String?>(null) }
    var progressState by remember { mutableStateOf<TransferProgressState?>(null) }
    var completionStats by remember { mutableStateOf<TransferCompletionStats?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isInitiating by remember { mutableStateOf(false) }
    var isBusyRevoking by remember { mutableStateOf(false) }
    var hasStartedDownload by remember { mutableStateOf(false) }
    var revokeMessage by remember { mutableStateOf<String?>(null) }

    suspend fun initiateSession() {
        isInitiating = true
        errorMessage = null
        hasStartedDownload = false
        try {
            val res = RetrofitClient.apiService().initiateDeviceTransfer(
                ApiTransferInitiateRequest(targetEcdhPubKey = targetPubBase64)
            )
            val data = res.body()?.data
            if (res.isSuccessful && res.body()?.success == true && data != null && data.transferId.isNotBlank()) {
                transferId = data.transferId
                qrPayload = data.qrPayload
                transferStatus = data.status
            } else {
                errorMessage = res.body()?.message ?: "تعذر بدء جلسة النقل الآمن (${res.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "تعذر الاتصال بالخادم لبدء النقل: ${e.message}"
        } finally {
            isInitiating = false
        }
    }

    LaunchedEffect(Unit) {
        initiateSession()
    }

    // Poll transfer status every 2 seconds until completed or terminal
    LaunchedEffect(transferId) {
        val activeId = transferId ?: return@LaunchedEffect
        while (completionStats == null && transferStatus !in setOf("CANCELLED", "EXPIRED", "COMPLETED")) {
            delay(2000L)
            try {
                val res = RetrofitClient.apiService().getDeviceTransferStatus(activeId)
                val statusData = res.body()?.data
                if (res.isSuccessful && res.body()?.success == true && statusData != null) {
                    transferStatus = statusData.status
                    if (!statusData.sourceDeviceName.isNullOrBlank()) {
                        sourceDeviceLabel = statusData.sourceDeviceName
                    }
                    val peerPub = statusData.sourceEcdhPubKey
                    if (!peerPub.isNullOrBlank() && sasCode == null) {
                        val aesKey = CrossDeviceTransferEngine.deriveAesSessionKey(
                            targetKeyPair.private,
                            peerPub,
                            activeId
                        )
                        sasCode = CrossDeviceTransferEngine.computeSasCode(
                            aesKey,
                            activeId,
                            targetPubBase64,
                            peerPub
                        )
                    }

                    if (statusData.status == "UPLOADING" && statusData.totalChunks > 0) {
                        val pct = 15 + ((statusData.uploadedChunkIndexes.size * 50) / statusData.totalChunks)
                        progressState = TransferProgressState(
                            phase = "WAITING_UPLOAD",
                            progressPercent = pct.coerceIn(10, 65),
                            currentChunk = statusData.uploadedChunkIndexes.size,
                            totalChunks = statusData.totalChunks,
                            message = "الهاتف القديم يرفع البيانات المشفرة (${statusData.uploadedChunkIndexes.size}/${statusData.totalChunks})..."
                        )
                    }

                    if (statusData.status == "READY_FOR_DOWNLOAD" && !hasStartedDownload) {
                        hasStartedDownload = true
                        val dlOutcome = CrossDeviceTransferEngine.downloadDecryptAndRestore(
                            context = context,
                            statusData = statusData,
                            targetKeyPair = targetKeyPair,
                            onProgress = { progressState = it }
                        )
                        dlOutcome.onSuccess { stats ->
                            completionStats = stats
                            transferStatus = "COMPLETED"
                        }.onFailure { err ->
                            errorMessage = err.message ?: "فشل استلام وفك تشفير بيانات النقل"
                            hasStartedDownload = false
                        }
                    }

                    if (statusData.status in setOf("CANCELLED", "EXPIRED")) {
                        errorMessage = if (statusData.status == "EXPIRED") {
                            "انتهت صلاحية جلسة النقل. يمكنك إعادة المحاولة."
                        } else {
                            "تم إلغاء طلب النقل من أحد الجهازين."
                        }
                    }
                }
            } catch (_: Exception) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(OozoxTeal.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhonelinkSetup,
                    contentDescription = "نقل الحساب بين الهواتف",
                    tint = OozoxTeal,
                    modifier = Modifier.size(34.dp)
                )
            }

            Text(
                text = "تم رصد جلسة نشطة على هاتف آخر",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "حسابك ($phone) مفتوح حالياً على ($sourceDeviceLabel). يمكنك نقل كامل المحادثات والوسائط والقيود بتشفير ECDH P-256 + AES-256-GCM قبل تسجيل الخروج التلقائي من الهاتف القديم.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            // Completion Card
            val doneStats = completionStats
            if (doneStats != null) {
                Surface(
                    color = StatusGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.2.dp, StatusGreen.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(40.dp))
                        Text(
                            text = "تم نقل الحساب بالكامل وإنهاء الجلسة القديمة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusGreen,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "تمت استعادة ${doneStats.restoredRecords} سجل محادثات وحسابات و ${doneStats.restoredMedia} ملف وسائط بنجاح.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = onTransferCompleted,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("btn_transfer_done_continue")
                        ) {
                            Text("الدخول إلى حسابي الآن", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                // QR Code & Approval Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "طريقتا تفويض النقل المشفر (E2EE)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )

                        if (isInitiating) {
                            CircularProgressIndicator(color = OozoxTeal)
                            Text("جاري تجهيز رمز النقل المشفر...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else if (!qrPayload.isNullOrBlank()) {
                            QrMatrixRenderer(
                                content = qrPayload!!,
                                size = 210.dp,
                                accentColor = OozoxTeal
                            )
                            Text(
                                text = "1) امسح رمز QR من داخل التطبيق في الهاتف القديم\n2) أو وافق مباشرة من إشعار النقل الفوري الذي وصل للهاتف القديم",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }

                        if (!sasCode.isNullOrBlank()) {
                            Surface(
                                color = OozoxTeal.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("رمز التحقق الأمني المطابق (SAS):", fontSize = 11.5.sp, color = OozoxTeal, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = sasCode!!,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = 6.sp
                                    )
                                    Text("تأكد أن هذا الرقم ظاهر أيضاً على شاشة الهاتف القديم.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        progressState?.let { prog ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { (prog.progressPercent / 100f).coerceIn(0f, 1f) },
                                    color = OozoxTeal,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Text(
                                    text = "${prog.message} (${prog.progressPercent}%)",
                                    fontSize = 12.sp,
                                    color = OozoxTeal,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = StatusRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(errorMessage!!, color = StatusRed, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                onClick = { scope.launch { initiateSession() } },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إعادة إنشاء جلسة النقل")
                            }
                        }
                    }
                }

                if (!revokeMessage.isNullOrBlank()) {
                    Text(
                        text = revokeMessage!!,
                        fontSize = 12.sp,
                        color = StatusGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Option: Revoke old device only without transferring
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isBusyRevoking = true
                            try {
                                val res = RetrofitClient.apiService().revokeOtherDevices()
                                if (res.isSuccessful && res.body()?.success == true) {
                                    revokeMessage = "تم تسجيل الخروج من جميع الأجهزة الأخرى بنجاح."
                                } else {
                                    errorMessage = res.body()?.message ?: "تعذر إنهاء جلسات الأجهزة الأخرى"
                                }
                            } catch (e: Exception) {
                                errorMessage = e.message
                            } finally {
                                isBusyRevoking = false
                            }
                        }
                    },
                    enabled = !isBusyRevoking,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_revoke_other_devices")
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = StatusRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إنهاء جلسات الهاتف الآخر فقط", color = StatusRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Clear Skip Action
                TextButton(
                    onClick = {
                        val activeId = transferId
                        scope.launch {
                            if (!activeId.isNullOrBlank()) {
                                runCatching { RetrofitClient.apiService().cancelDeviceTransfer(activeId) }
                            }
                            onSkipTransfer(isExistingUser)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("btn_skip_transfer")
                ) {
                    Text(
                        text = if (isExistingUser) "تخطي النقل والانتقال لخيارات النسخ الاحتياطي" else "تخطي والدخول إلى التطبيق",
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }
}
