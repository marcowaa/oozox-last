package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size as AndroidSize
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OozoxTeal
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real scannable ZXing QR Code Renderer.
 * Generates an ISO/IEC 18004 BitMatrix with ErrorCorrectionLevel.M and clean quiet zone.
 */
@Composable
fun QrMatrixRenderer(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    primaryColor: Color = Color(0xFF0E1320),
    accentColor: Color = OozoxTeal
) {
    val bitMatrix: BitMatrix? = remember(content) {
        try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            QRCodeWriter().encode(content.ifBlank { "OOZOX" }, BarcodeFormat.QR_CODE, 0, 0, hints)
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, accentColor.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitMatrix != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cols = bitMatrix.width.coerceAtLeast(1)
                val rows = bitMatrix.height.coerceAtLeast(1)
                val cellW = this.size.width / cols
                val cellH = this.size.height / rows
                val moduleColor = if (primaryColor == GoldPrimary) Color(0xFF101522) else primaryColor

                for (y in 0 until rows) {
                    for (x in 0 until cols) {
                        if (bitMatrix.get(x, y)) {
                            drawRect(
                                color = moduleColor,
                                topLeft = Offset(x * cellW, y * cellH),
                                size = Size(cellW + 0.35f, cellH + 0.35f)
                            )
                        }
                    }
                }
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = accentColor)
        }
    }
}

/**
 * CameraX + ZXing QR Code Scanner Dialog with runtime CAMERA permission + manual fallback.
 */
@Composable
fun QrScannerDialog(
    title: String = "مسح رمز النقل السريع (QR)",
    subtitle: String = "وجّه كاميرا الهاتف القديم نحو رمز QR المعروض على الهاتف الجديد",
    onQrScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var manualInput by remember { mutableStateOf("") }
    var showManualFallback by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) showManualFallback = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("qr_scanner_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, OozoxTeal.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = OozoxTeal)
                        Text(title, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hasCameraPermission && !showManualFallback) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, OozoxTeal, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraXQrPreview(
                            onDetected = { scannedText ->
                                onQrScanned(scannedText)
                            }
                        )
                        // Clean targeting reticle overlay
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .border(2.dp, GoldPrimary.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(32.dp))
                            Text(
                                text = "لم يتم تفعيل الكاميرا أو فضّلت الإدخال اليدوي. يمكنك لصق كود/بيانات QR هنا:",
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!hasCameraPermission) {
                                TextButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                    Text("منح صلاحية الكاميرا", color = OozoxTeal, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { showManualFallback = !showManualFallback },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = if (showManualFallback) "استخدام الكاميرا" else "إدخال / لصق الكود يدوياً",
                        fontSize = 12.sp,
                        color = GoldPrimary
                    )
                }

                if (showManualFallback || !hasCameraPermission) {
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        label = { Text("بيانات رمز النقل (JSON / QR)") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qr_manual_input")
                    )

                    Button(
                        onClick = {
                            if (manualInput.trim().isNotBlank()) {
                                onQrScanned(manualInput.trim())
                            }
                        },
                        enabled = manualInput.trim().isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = OozoxTeal, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("تأكيد الكود", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraXQrPreview(
    onDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val alreadyHandled = remember { AtomicBoolean(false) }

    val reader = remember {
        MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.TRY_HARDER to true
                )
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                @Suppress("DEPRECATION")
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(AndroidSize(960, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { imageProxy ->
                    if (alreadyHandled.get()) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    decodeQrFromImageProxy(imageProxy, reader)?.let { text ->
                        if (alreadyHandled.compareAndSet(false, true)) {
                            ContextCompat.getMainExecutor(context).execute {
                                onDetected(text)
                            }
                        }
                    }
                    imageProxy.close()
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (_: Exception) {
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (_: Exception) {
            }
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

private fun decodeQrFromImageProxy(
    imageProxy: ImageProxy,
    reader: MultiFormatReader
): String? {
    return try {
        val plane = imageProxy.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val width = imageProxy.width
        val height = imageProxy.height
        val yBytes = ByteArray(width * height)
        if (rowStride == width) {
            buffer.get(yBytes)
        } else {
            val row = ByteArray(rowStride)
            for (r in 0 until height) {
                val remaining = buffer.remaining()
                if (remaining < width) break
                val toRead = minOf(rowStride, remaining)
                buffer.get(row, 0, toRead)
                System.arraycopy(row, 0, yBytes, r * width, width)
            }
        }
        val source = PlanarYUVLuminanceSource(
            yBytes,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        reader.decodeWithState(bitmap).text
    } catch (_: Exception) {
        null
    } finally {
        reader.reset()
    }
}
