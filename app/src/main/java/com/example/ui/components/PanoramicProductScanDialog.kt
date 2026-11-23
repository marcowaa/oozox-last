package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.MerchantProductCategory
import com.example.data.model.MerchantProductEntity
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OozoxTeal
import com.example.util.Cylindrical360CameraAnalyzer
import com.example.util.ProductScanOcrResult
import com.example.util.SmartProductScannerEngine
import com.example.util.tr
import java.io.File
import java.util.concurrent.Executors

/**
 * Compact 360° Cylindrical Product Scan Card:
 * - Responsive card proportions matching 4:3 camera framing
 * - Real CameraX ImageAnalysis motion-gated frame capture (captures only while product moves)
 * - Automatic 360° rotation end detection via loop closure
 */
@Composable
fun PanoramicProductScanDialog(
    merchantShopName: String,
    currentStaffName: String? = null,
    onSaveProduct: (MerchantProductEntity) -> Unit,
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
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var useBackCamera by remember { mutableStateOf(true) }
    var currentAngle by remember { mutableIntStateOf(0) }
    var capturedFramesCount by remember { mutableIntStateOf(0) }
    var isProductMoving by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var scanCompleted by remember { mutableStateOf(false) }
    var unwrappedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var unwrappedFilePath by remember { mutableStateOf<String?>(null) }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    // Extracted / editable product fields
    var customSampleNote by remember { mutableStateOf("") }
    var ocrResult by remember { mutableStateOf<ProductScanOcrResult?>(null) }
    var title by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var karat by remember { mutableStateOf("21") }
    var barcodeText by remember { mutableStateOf("") }
    var consumerPriceText by remember { mutableStateOf("") }
    var wholesalePriceText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var saveImageFiles by remember { mutableStateOf(false) }

    val analyzer = remember {
        Cylindrical360CameraAnalyzer(
            context = context.applicationContext,
            onProgress = { angle, count, moving ->
                currentAngle = angle
                capturedFramesCount = count
                isProductMoving = moving
            },
            onCompleted = { count, bitmap, path ->
                currentAngle = 360
                capturedFramesCount = count
                isProductMoving = false
                isScanning = false
                scanCompleted = true
                unwrappedBitmap = bitmap
                unwrappedFilePath = path
                val res = SmartProductScannerEngine.processPanoramic360Scan(
                    customInputText = customSampleNote.trim().ifBlank { null },
                    capturedAnglesCount = count.coerceAtLeast(8)
                )
                ocrResult = res
                if (res.title.isNotBlank() && title.isBlank()) title = res.title
                if (res.weightGrams > 0 && weightText.isBlank()) weightText = res.weightGrams.toString()
                if (res.karat.isNotBlank()) karat = res.karat
                if (!res.barcode.isNullOrBlank() && barcodeText.isBlank()) barcodeText = res.barcode
                if (res.consumerPriceEGP > 0 && consumerPriceText.isBlank()) {
                    consumerPriceText = res.consumerPriceEGP.toInt().toString()
                }
                if (res.wholesalePriceEGP > 0 && wholesalePriceText.isBlank()) {
                    wholesalePriceText = res.wholesalePriceEGP.toInt().toString()
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                analyzer.stopScan()
                cameraProviderRef?.unbindAll()
                analysisExecutor.shutdownNow()
            } catch (_: Exception) {}
        }
    }

    // Bind CameraX Preview + Motion-Gated ImageAnalysis
    LaunchedEffect(hasCameraPermission, useBackCamera, previewViewRef) {
        if (!hasCameraPermission) return@LaunchedEffect
        val previewView = previewViewRef ?: return@LaunchedEffect

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                cameraProviderRef = provider

                val selector = if (useBackCamera) {
                    if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }
                } else {
                    if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                }

                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, analyzer) }

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    val savedToastText = tr("scan360.saved_toast")
    val nameRequiredText = tr("scan360.name_required")

    Dialog(
        onDismissRequest = {
            analyzer.stopScan()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 460.dp)
                .heightIn(min = 380.dp, max = 660.dp)
                .navigationBarsPadding()
                .imePadding()
                .testTag("panoramic_360_scan_card"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Compact Card Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Rotate90DegreesCcw,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = tr("scan360.title"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = {
                            analyzer.stopScan()
                            onDismiss()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = tr("common.close"))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. 360° Cylindrical Camera Scan Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Proportioned 4:3 Camera Viewport
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                        .heightIn(min = 210.dp, max = 270.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF0B1220)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasCameraPermission) {
                                        AndroidView(
                                            factory = { ctx ->
                                                PreviewView(ctx).apply {
                                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                                    previewViewRef = this
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Button(
                                            onClick = {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = GoldPrimary,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("grant_360_camera_btn")
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(tr("scan360.enable_camera"), fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Cylindrical framing guide + circular 360° arc progress
                                    val ringColor = when {
                                        scanCompleted -> Color(0xFF10B981)
                                        isScanning && isProductMoving -> Color(0xFF10B981)
                                        isScanning -> GoldPrimary
                                        else -> OozoxTeal
                                    }
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val padW = size.width * 0.20f
                                        val padH = size.height * 0.16f
                                        val cornerLen = 22.dp.toPx()
                                        val strokePx = 2.5.dp.toPx()

                                        val left = padW
                                        val right = size.width - padW
                                        val top = padH
                                        val bottom = size.height - padH

                                        // Top-left bracket
                                        drawLine(ringColor, Offset(left, top), Offset(left + cornerLen, top), strokePx)
                                        drawLine(ringColor, Offset(left, top), Offset(left, top + cornerLen), strokePx)
                                        // Top-right bracket
                                        drawLine(ringColor, Offset(right, top), Offset(right - cornerLen, top), strokePx)
                                        drawLine(ringColor, Offset(right, top), Offset(right, top + cornerLen), strokePx)
                                        // Bottom-left bracket
                                        drawLine(ringColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokePx)
                                        drawLine(ringColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokePx)
                                        // Bottom-right bracket
                                        drawLine(ringColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokePx)
                                        drawLine(ringColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokePx)

                                        // 360° Sweep Arc
                                        val radius = minOf(size.width, size.height) * 0.24f
                                        val arcTopLeft = Offset(center.x - radius, center.y - radius)
                                        val arcSize = Size(radius * 2f, radius * 2f)
                                        drawArc(
                                            color = Color.White.copy(alpha = 0.22f),
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            topLeft = arcTopLeft,
                                            size = arcSize,
                                            style = Stroke(width = 3.dp.toPx())
                                        )
                                        if (currentAngle > 0) {
                                            drawArc(
                                                color = ringColor,
                                                startAngle = -90f,
                                                sweepAngle = currentAngle.toFloat().coerceIn(0f, 360f),
                                                useCenter = false,
                                                topLeft = arcTopLeft,
                                                size = arcSize,
                                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }
                                    }

                                    // Compact angle + motion indicator pill (Top-Start)
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(ringColor)
                                            )
                                            Text(
                                                text = if (scanCompleted) "360° ✓" else "${currentAngle}° • $capturedFramesCount",
                                                color = Color.White,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Camera Switch Button (Top-End, 48dp touch target, 36dp circle)
                                    if (hasCameraPermission) {
                                        OozoxCircleIconButton(
                                            onClick = { useBackCamera = !useBackCamera },
                                            imageVector = Icons.Default.Cameraswitch,
                                            contentDescription = null,
                                            tint = Color.White,
                                            containerColor = Color.Black.copy(alpha = 0.55f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .testTag("flip_360_camera_btn")
                                        )
                                    }
                                }

                                // Stitched Unwrapped Strip Preview (when 360° completes)
                                val bmp = unwrappedBitmap
                                if (scanCompleted && bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(68.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                }

                                OutlinedTextField(
                                    value = customSampleNote,
                                    onValueChange = { customSampleNote = it },
                                    label = { Text(tr("scan360.label_input"), fontSize = 11.5.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Single clean Start / Stop / Rescan action button
                                Button(
                                    onClick = {
                                        if (!hasCameraPermission) {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            return@Button
                                        }
                                        if (isScanning) {
                                            analyzer.stopScan()
                                            isScanning = false
                                            isProductMoving = false
                                        } else {
                                            unwrappedBitmap = null
                                            unwrappedFilePath = null
                                            scanCompleted = false
                                            currentAngle = 0
                                            capturedFramesCount = 0
                                            analyzer.startScan()
                                            isScanning = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isScanning) Color(0xFFEF4444) else GoldPrimary,
                                        contentColor = if (isScanning) Color.White else Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("start_360_scan_btn")
                                ) {
                                    Icon(
                                        if (isScanning) Icons.Default.Stop
                                        else if (scanCompleted) Icons.Default.Refresh
                                        else Icons.Default.Camera,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            isScanning -> tr("scan360.stop_btn")
                                            scanCompleted -> tr("scan360.rescan_btn")
                                            else -> tr("scan360.start_btn")
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // 2. Compact Product Data Form (post-360 completion)
                    if (scanCompleted) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = tr("scan360.save_data_only"),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Switch(
                                            checked = !saveImageFiles,
                                            onCheckedChange = { saveImageFiles = !it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = { Text(tr("scan360.product_name")) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = weightText,
                                            onValueChange = { weightText = it },
                                            label = { Text(tr("scan360.weight_grams")) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = karat,
                                            onValueChange = { karat = it },
                                            label = { Text(tr("scan360.karat")) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    OutlinedTextField(
                                        value = barcodeText,
                                        onValueChange = { barcodeText = it },
                                        label = { Text(tr("scan360.barcode")) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = consumerPriceText,
                                            onValueChange = { consumerPriceText = it.filter { c -> c.isDigit() || c == '.' } },
                                            label = { Text(tr("scan360.consumer_price")) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = wholesalePriceText,
                                            onValueChange = { wholesalePriceText = it.filter { c -> c.isDigit() || c == '.' } },
                                            label = { Text(tr("scan360.wholesale_price")) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    OutlinedTextField(
                                        value = quantityText,
                                        onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                                        label = { Text(tr("scan360.stock_qty")) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            if (title.isNotBlank()) {
                                                val w = weightText.toDoubleOrNull() ?: 0.0
                                                val pCon = consumerPriceText.toDoubleOrNull() ?: 0.0
                                                val pWho = wholesalePriceText.toDoubleOrNull() ?: 0.0
                                                val qty = quantityText.toDoubleOrNull() ?: 1.0
                                                val savedUri = if (saveImageFiles && !unwrappedFilePath.isNullOrBlank()) {
                                                    Uri.fromFile(File(unwrappedFilePath!!)).toString()
                                                } else null

                                                val product = MerchantProductEntity(
                                                    id = "prod_${System.currentTimeMillis()}",
                                                    merchantId = "merchant_store",
                                                    merchantShopName = merchantShopName,
                                                    title = title.trim(),
                                                    category = MerchantProductCategory.BULLION.name,
                                                    karat = karat,
                                                    weightGrams = w,
                                                    consumerPriceEGP = pCon,
                                                    wholesalePriceEGP = pWho,
                                                    quantityInStock = qty,
                                                    addedByStaffName = currentStaffName ?: "Staff",
                                                    extractedOcrText = ocrResult?.extractedFullText ?: customSampleNote,
                                                    extractedBarcode = barcodeText.ifBlank { null },
                                                    extractedKeywords = ocrResult?.keywords?.joinToString(" "),
                                                    isPanoramicScanned = true,
                                                    panoramicScanUri = savedUri,
                                                    createdAt = System.currentTimeMillis()
                                                )
                                                onSaveProduct(product)
                                                Toast.makeText(context, savedToastText, Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            } else {
                                                Toast.makeText(context, nameRequiredText, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("confirm_save_360_product_btn")
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tr("scan360.save_product_btn"), fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
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
