package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.GoldPurchaseEntity
import com.example.data.model.MetalType
import com.example.ui.theme.*
import com.example.util.GoldUtils
import org.json.JSONObject

/**
 * QR Code Generator Dialog for Gold & Silver purchase transactions.
 * Generates an authentic, shareable 2D matrix QR code seeded by transaction details,
 * allowing instant export, sharing, or scanning back into the app.
 */
@Composable
fun GoldQrGeneratorDialog(
    initialPurchase: GoldPurchaseEntity? = null,
    purchases: List<GoldPurchaseEntity> = emptyList(),
    onDismiss: () -> Unit,
    onTestScan: ((rawPayload: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    // Active purchase selection
    var selectedPurchaseId by remember {
        mutableStateOf(initialPurchase?.id ?: purchases.firstOrNull()?.id)
    }

    val activePurchase = remember(selectedPurchaseId, purchases, initialPurchase) {
        purchases.find { it.id == selectedPurchaseId } ?: initialPurchase
    }

    // Editable fallback attributes if no purchase entity is available
    var customMetalType by remember { mutableStateOf(activePurchase?.metalType ?: MetalType.GOLD.name) }
    var customKarat by remember { mutableStateOf(activePurchase?.karat ?: "24") }
    var customWeight by remember { mutableStateOf(activePurchase?.extractedWeight ?: 10.0) }
    var customPricePerGram by remember { mutableStateOf(activePurchase?.pricePerGram ?: 4150.0) }
    var customManufacturer by remember { mutableStateOf(activePurchase?.manufacturer ?: "BTC Gold") }
    var customMfgCost by remember { mutableStateOf(activePurchase?.manufacturingCost ?: 115.0) }
    var customShop by remember { mutableStateOf(activePurchase?.sellerShopName ?: activePurchase?.traderHashtag ?: "شركة BTC المعتمدة") }

    // Synchronize when activePurchase changes
    LaunchedEffect(activePurchase) {
        if (activePurchase != null) {
            customMetalType = activePurchase.metalType
            customKarat = activePurchase.karat
            customWeight = activePurchase.extractedWeight
            customPricePerGram = activePurchase.pricePerGram
            customManufacturer = activePurchase.manufacturer
            customMfgCost = activePurchase.manufacturingCost
            customShop = activePurchase.sellerShopName ?: activePurchase.traderHashtag.ifBlank { "محل معتمد" }
        }
    }

    val serialCode = remember(activePurchase, selectedPurchaseId) {
        val idSeed = activePurchase?.id?.take(6)?.uppercase() ?: (1000..9999).random().toString()
        "BTC-EG-$idSeed"
    }

    // Generate JSON payload compatible with GoldQrScannerDialog
    val qrJsonPayload = remember(customMetalType, customKarat, customWeight, customPricePerGram, customManufacturer, customMfgCost, customShop, serialCode) {
        JSONObject().apply {
            put("metal", customMetalType)
            put("karat", customKarat.replace("K", "").replace("k", "").replace("عيار", "").trim())
            put("weight", customWeight)
            put("price", customPricePerGram)
            put("brand", customManufacturer)
            put("mfgCost", customMfgCost)
            put("shop", customShop)
            put("serial", serialCode)
            put("type", "OOZOX_GOLD_CERTIFICATE")
            put("timestamp", System.currentTimeMillis())
        }.toString()
    }

    val totalCost = remember(customWeight, customPricePerGram, customMfgCost) {
        (customWeight * customPricePerGram) + (customWeight * customMfgCost)
    }

    val shareText = remember(customMetalType, customKarat, customWeight, totalCost, customManufacturer, customShop, serialCode, qrJsonPayload) {
        val metalAr = if (customMetalType.contains("SILVER", ignoreCase = true)) "فضة" else "ذهب"
        """
        🪙 شهادة وفاتورة $metalAr معتمدة - تطبيق أوزوكس (OOZOX)
        ────────────────────────
        • المعدن: $metalAr عيار $customKarat
        • الوزن الصافي: $customWeight جرام
        • المصنع: $customManufacturer
        • البائع/المتجر: $customShop
        • السيريال: $serialCode
        • الإجمالي التقديري: ${GoldUtils.formatCurrency(totalCost)}
        
        📌 كود الفاتورة الإلكتروني (QR Payload):
        $qrJsonPayload
        """.trimIndent()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .testTag("dialog_gold_qr_generator")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = GoldPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "توليد كود QR لمعاملات الذهب",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مشاركة تفاصيل الفاتورة والسبائك عبر رمز QR مشفر",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_close_gold_qr_generator")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // If multiple purchases exist, allow selecting between them
                if (purchases.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "اختر من مشتريات محفظتك لتوليد الـ QR:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            purchases.forEach { purchase ->
                                val isSelected = purchase.id == selectedPurchaseId
                                Surface(
                                    onClick = { selectedPurchaseId = purchase.id },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (isSelected) GoldPrimary else MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "${purchase.extractedWeight}غ ${purchase.karat} (${purchase.manufacturer})",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // QR Code Display Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 6.dp,
                    border = BorderStroke(2.dp, GoldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // The actual QR Matrix Renderer
                        QrMatrixRenderer(
                            content = qrJsonPayload,
                            size = 200.dp,
                            primaryColor = GoldPrimary,
                            accentColor = OozoxTeal,
                            modifier = Modifier.testTag("qr_code_matrix_gold")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            color = GoldPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "شهادة معتمدة: $serialCode",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF854D0E)
                                )
                            }
                        }
                    }
                }

                // Details Breakdown Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المعدن والعيار:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (customMetalType.contains("SILVER", ignoreCase = true)) "فضة عيار $customKarat" else "ذهب عيار $customKarat",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الوزن الإجمالي:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$customWeight جرام", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المصنع / الشركة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(customManufacturer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("البائع / المتجر:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(customShop, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("إجمالي القيمة التقديرية:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = GoldUtils.formatCurrency(totalCost),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusGreen
                            )
                        }
                    }
                }

                // Action Buttons: Share & Copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            try {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_SUBJECT, "شهادة شراء ذهب - $serialCode")
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "مشاركة كود الـ QR وتفاصيل المعاملة:")
                                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(shareIntent)
                            } catch (e: Exception) {
                                clipboard.setText(AnnotatedString(qrJsonPayload))
                                ToastNotificationBus.showToast(
                                    message = "تم نسخ كود الـ QR إلى الحافظة بنجاح 📋",
                                    type = AppToastType.SUCCESS
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_share_gold_qr")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة الـ QR 📤", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            clipboard.setText(AnnotatedString(qrJsonPayload))
                            ToastNotificationBus.showToast(
                                message = "تم نسخ رمز الـ QR (JSON) للحافظة 📋",
                                type = AppToastType.SUCCESS
                            )
                        },
                        border = BorderStroke(1.2.dp, GoldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_copy_gold_qr_payload")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ الكود", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 12.5.sp)
                    }
                }

                // If test scan callback provided, give option to simulate scanning this generated QR right away
                if (onTestScan != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onTestScan(qrJsonPayload)
                        },
                        modifier = Modifier.testTag("btn_test_scan_generated_qr")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اختبار هذا الكود في الماسح الضوئي 📷", fontSize = 12.sp, color = GoldPrimary)
                    }
                }
            }
        }
    }
}
