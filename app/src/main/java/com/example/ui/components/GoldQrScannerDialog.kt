package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MetalType
import com.example.ui.theme.*
import com.example.util.GoldUtils
import org.json.JSONObject

data class ScannedGoldItem(
    val metalType: MetalType = MetalType.GOLD,
    val karat: String = "24",
    val weight: Double = 10.0,
    val pricePerGram: Double = 4150.0,
    val manufacturer: String = "BTC Gold",
    val manufacturingCost: Double = 120.0,
    val sellerShopName: String = "شركة BTC المعتمدة",
    val serialNumber: String? = null,
    val notes: String? = null
)

@Composable
fun GoldQrScannerDialog(
    onDismiss: () -> Unit,
    onAddPurchase: (
        metalType: MetalType,
        weight: Double,
        karat: String,
        pricePerGram: Double,
        manufacturer: String,
        manufacturingCost: Double,
        sellerShopName: String,
        notes: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var manualQrInput by remember { mutableStateOf("") }
    var scannedItem by remember {
        mutableStateOf<ScannedGoldItem?>(
            ScannedGoldItem(
                metalType = MetalType.GOLD,
                karat = "24",
                weight = 10.0,
                pricePerGram = 4150.0,
                manufacturer = "BTC Gold (سبيكة أصلية)",
                manufacturingCost = 115.0,
                sellerShopName = "مجوهرات BTC المعتمدة",
                serialNumber = "BTC-EG-99824"
            )
        )
    }

    var isScanningActive by remember { mutableStateOf(true) }
    var scanSuccessFlash by remember { mutableStateOf(false) }

    // Laser Animation for Viewfinder
    val infiniteTransition = rememberInfiniteTransition(label = "laser_anim")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    fun parseQrCode(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return

        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scanSuccessFlash = true

        try {
            // Attempt JSON parse
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val json = JSONObject(trimmed)
                val metalStr = json.optString("metal", "GOLD").uppercase()
                val metal = if (metalStr.contains("SILVER")) MetalType.SILVER else MetalType.GOLD
                val karat = json.optString("karat", if (metal == MetalType.GOLD) "24" else "925")
                    .replace("K", "").replace("k", "").replace("عيار", "").trim()
                val weight = json.optDouble("weight", 10.0)
                val price = json.optDouble("price", if (metal == MetalType.GOLD) 4150.0 else 52.0)
                val mfg = json.optString("manufacturer", json.optString("brand", "BTC"))
                val mfgCost = json.optDouble("mfgCost", 110.0)
                val shop = json.optString("shop", json.optString("seller", "محل معتمد"))
                val serial = json.optString("serial", "SN-${System.currentTimeMillis() % 10000}")

                scannedItem = ScannedGoldItem(
                    metalType = metal,
                    karat = karat,
                    weight = weight,
                    pricePerGram = price,
                    manufacturer = mfg,
                    manufacturingCost = mfgCost,
                    sellerShopName = shop,
                    serialNumber = serial
                )
                return
            }

            // Pipe-separated or comma-separated format:
            // e.g. GOLD|24|10.0|4150|BTC Gold|120|الصايغ|BTC-1234
            val delimiter = if (trimmed.contains("|")) "|" else if (trimmed.contains(",")) "," else null
            if (delimiter != null) {
                val parts = trimmed.split(delimiter).map { it.trim() }
                if (parts.size >= 3) {
                    val metal = if (parts[0].contains("SILVER", ignoreCase = true) || parts[0].contains("فضة")) MetalType.SILVER else MetalType.GOLD
                    val karat = parts[1].replace("K", "").replace("k", "").replace("عيار", "").trim()
                    val weight = parts[2].toDoubleOrNull() ?: 10.0
                    val price = parts.getOrNull(3)?.toDoubleOrNull() ?: 4150.0
                    val mfg = parts.getOrNull(4) ?: "مسبوكات ذهبية"
                    val mfgCost = parts.getOrNull(5)?.toDoubleOrNull() ?: 100.0
                    val shop = parts.getOrNull(6) ?: "محل مجوهرات"
                    val serial = parts.getOrNull(7) ?: "QR-${(1000..9999).random()}"

                    scannedItem = ScannedGoldItem(
                        metalType = metal,
                        karat = karat,
                        weight = weight,
                        pricePerGram = price,
                        manufacturer = mfg,
                        manufacturingCost = mfgCost,
                        sellerShopName = shop,
                        serialNumber = serial
                    )
                    return
                }
            }

            // Keyword pattern detection
            val isSilver = trimmed.contains("فضة", ignoreCase = true) || trimmed.contains("SILVER", ignoreCase = true)
            val metal = if (isSilver) MetalType.SILVER else MetalType.GOLD
            val karat = when {
                trimmed.contains("24") -> "24"
                trimmed.contains("21") -> "21"
                trimmed.contains("18") -> "18"
                trimmed.contains("925") -> "925"
                else -> if (metal == MetalType.GOLD) "21" else "925"
            }
            val weightRegex = Regex("""(\d+(\.\d+)?)\s*(g|gm|جرام|غرام)""", RegexOption.IGNORE_CASE)
            val weightMatch = weightRegex.find(trimmed)
            val weight = weightMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 10.0

            scannedItem = ScannedGoldItem(
                metalType = metal,
                karat = karat,
                weight = weight,
                pricePerGram = if (metal == MetalType.GOLD) 4150.0 else 52.0,
                manufacturer = if (trimmed.contains("SAM", ignoreCase = true)) "SAM Gold" else "BTC Gold",
                manufacturingCost = 120.0,
                sellerShopName = "فاتورة مسحوبة بالـ QR",
                serialNumber = "QR-${System.currentTimeMillis() % 100000}"
            )
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر قراءة كود QR: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ماسح فواتير وسبائك الذهب QR",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مسح سريع لشهادات وباركود السبائك والمشغولات",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_close_gold_qr_scanner")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Camera Scanner Viewfinder Simulation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.2.dp, GoldPrimary.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Center Targeting Box
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .border(2.dp, if (scanSuccessFlash) StatusGreen else GoldPrimary, RoundedCornerShape(14.dp))
                    ) {
                        // Four Corner Accents
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = null,
                            tint = if (scanSuccessFlash) StatusGreen else GoldPrimary,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                        )

                        // Animated Laser Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .offset(y = (laserOffset * 140).dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            if (scanSuccessFlash) StatusGreen else Color(0xFFFBBF24),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    // Scan Overlay Indicator
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "وجه الكاميرا نحو باركود أو رمز الـ QR على السبيكة/الفاتورة",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Quick Presets for Common Gold Certs & Bars
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "نماذج سريعة لشهادات السبائك والفواتير الشائعة:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickScanPresetChip(
                            label = "سبيكة BTC عيار 24 (10غ)",
                            badge = "BTC 24K",
                            onClick = {
                                parseQrCode("""{"metal":"GOLD","karat":"24","weight":10.0,"price":4150.0,"brand":"BTC Gold","mfgCost":115.0,"shop":"شركة BTC الرسمية","serial":"BTC-EG-778891"}""")
                            }
                        )

                        QuickScanPresetChip(
                            label = "سبيكة SAM عيار 24 (5غ)",
                            badge = "SAM 24K",
                            onClick = {
                                parseQrCode("""{"metal":"GOLD","karat":"24","weight":5.0,"price":4150.0,"brand":"سام للذهب SAM","mfgCost":130.0,"shop":"مجوهرات النور","serial":"SAM-99201"}""")
                            }
                        )

                        QuickScanPresetChip(
                            label = "جنيه ذهب BTC عيار 21 (8غ)",
                            badge = "جنيه 21K",
                            onClick = {
                                parseQrCode("""{"metal":"GOLD","karat":"21","weight":8.0,"price":3630.0,"brand":"BTC Gold","mfgCost":65.0,"shop":"صاغة الحسين","serial":"COIN-BTC-551"}""")
                            }
                        )

                        QuickScanPresetChip(
                            label = "سبيكة فضة سام عيار 925 (100غ)",
                            badge = "فضة 925",
                            onClick = {
                                parseQrCode("""{"metal":"SILVER","karat":"925","weight":100.0,"price":52.0,"brand":"سام للمعادن الثمينة","mfgCost":12.0,"shop":"سوق الصاغة","serial":"SLV-SAM-301"}""")
                            }
                        )
                    }
                }

                // Manual QR Code / Barcode Raw Payload Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualQrInput,
                        onValueChange = { manualQrInput = it },
                        placeholder = { Text("أدخل بيانات الـ QR أو الباركود يدوياً...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = GoldPrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_manual_gold_qr")
                    )

                    Button(
                        onClick = {
                            if (manualQrInput.isNotBlank()) {
                                parseQrCode(manualQrInput)
                                manualQrInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("قراءة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Parsed Data Result Card
                val item = scannedItem
                if (item != null) {
                    val totalRawMetal = item.weight * item.pricePerGram
                    val totalMfg = item.weight * item.manufacturingCost
                    val grandTotal = totalRawMetal + totalMfg

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.2.dp, GoldPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        color = GoldPrimary,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (item.metalType == MetalType.GOLD) "ذهب عيار ${item.karat}" else "فضة عيار ${item.karat}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    if (item.serialNumber != null) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Text(
                                                text = item.serialNumber,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "تم التعرف بنجاح 🟢",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreen
                                )
                            }

                            // Metric Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("الوزن الصافي", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${item.weight} جرام", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("سعر الجرام", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(GoldUtils.formatCurrency(item.pricePerGram), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("المصنعية/جرام", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(GoldUtils.formatCurrency(item.manufacturingCost), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusOrange)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("التاجر / المصنع المعتمد:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${item.manufacturer} • ${item.sellerShopName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("الإجمالي التقديري:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(GoldUtils.formatCurrency(grandTotal), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                                }
                            }
                        }
                    }

                    // Action Button: Add to Gold Purchases Now
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAddPurchase(
                                item.metalType,
                                item.weight,
                                item.karat,
                                item.pricePerGram,
                                item.manufacturer,
                                item.manufacturingCost,
                                item.sellerShopName,
                                "سجلت تلقائياً عبر ماسح الـ QR (رقم تسلسلي: ${item.serialNumber ?: "ممسوح"})"
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_confirm_scanned_gold_purchase")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إضافة المشتريات لمحفظة الذهب فوراً ⚡",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickScanPresetChip(
    label: String,
    badge: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.QrCode2, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
