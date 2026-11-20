package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ContactEntity
import com.example.data.model.GoldKarat
import com.example.data.model.MetalType
import com.example.data.model.SilverKarat
import com.example.data.model.UserType
import com.example.ui.theme.*
import com.example.util.GoldUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoldPurchaseDialog(
    contacts: List<ContactEntity> = emptyList(),
    onAddPurchase: (
        metalType: MetalType,
        weight: Double,
        karat: String,
        pricePerGram: Double,
        manufacturer: String,
        manufacturingCost: Double,
        mfgType: String,
        stampDuty: Double,
        cashbackRefund: Double,
        traderHashtag: String,
        sellerShopName: String,
        sellerLocation: String,
        scalePhotoUri: String?,
        metalLossRatePercent: Double,
        metalLossGrams: Double
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMetal by remember { mutableStateOf(MetalType.GOLD) }
    var selectedGoldKarat by remember { mutableStateOf(GoldKarat.KARAT_21) }
    var selectedSilverKarat by remember { mutableStateOf(SilverKarat.KARAT_925) }

    var isScanningScalePhoto by remember { mutableStateOf(false) }
    var scalePhotoCaptured by remember { mutableStateOf(false) }
    var showScaleOcrDialog by remember { mutableStateOf(false) }

    var weightStr by remember { mutableStateOf("15.50") }
    var priceStr by remember { mutableStateOf("3850.0") }
    var manufacturer by remember { mutableStateOf("BTC Gold") }

    var mfgType by remember { mutableStateOf("PER_GRAM") } // "PER_GRAM" or "PER_PIECE"
    var mfgCostStr by remember { mutableStateOf("120.0") }
    var stampDutyStr by remember { mutableStateOf("18.0") }
    var cashbackRefundStr by remember { mutableStateOf("25.0") }

    // Metal Erosion / Loss Rate %
    var metalLossRateStr by remember { mutableStateOf("0.5") } // Default 0.5% for gold

    // Trader Mention
    var selectedTraderContact by remember { mutableStateOf<ContactEntity?>(null) }
    var traderHashtag by remember { mutableStateOf("تاجر_الأمانة") }
    var isExternalShop by remember { mutableStateOf(false) }
    var externalShopName by remember { mutableStateOf("مجوهرات الصايغ") }
    var externalShopLocation by remember { mutableStateOf("شارع المعز، القاهرة") }

    // Calculate totals
    val weight = weightStr.toDoubleOrNull() ?: 0.0
    val pricePerGram = priceStr.toDoubleOrNull() ?: 0.0
    val mfgValue = mfgCostStr.toDoubleOrNull() ?: 0.0
    val stampDuty = stampDutyStr.toDoubleOrNull() ?: 0.0
    val cashback = cashbackRefundStr.toDoubleOrNull() ?: 0.0
    val metalLossRatePercent = metalLossRateStr.toDoubleOrNull() ?: 0.0

    // Metal Loss / Erosion calculations
    val metalLossGrams = weight * (metalLossRatePercent / 100.0)
    val netWeightAfterLoss = (weight - metalLossGrams).coerceAtLeast(0.0)
    val metalLossValue = metalLossGrams * pricePerGram

    val pureMetalTotal = weight * pricePerGram
    val totalMfgCost = if (mfgType == "PER_GRAM") weight * mfgValue else mfgValue
    val totalStamp = weight * stampDuty
    val grandTotalPaid = pureMetalTotal + totalMfgCost + totalStamp

    // Anti-Fraud Market Analysis Engine
    val averageMarketMfg = when (selectedMetal) {
        MetalType.GOLD -> if (manufacturer.contains("BTC", true) || manufacturer.contains("Sam", true)) 125.0 else 85.0
        MetalType.SILVER -> 25.0
    }
    val effectiveMfgPerGram = if (mfgType == "PER_GRAM") mfgValue else if (weight > 0) mfgValue / weight else 0.0
    val isMfgOverpriced = effectiveMfgPerGram > (averageMarketMfg * 1.35)

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
            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedMetal == MetalType.GOLD) Icons.Default.Diamond else Icons.Default.Circle,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "تسجيل شراء فاتورة ${selectedMetal.label}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "توثيق حيازة رسمية وتحويل ملكية",
                                fontSize = 11.sp,
                                color = GoldPrimary
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Metal Selection Switcher (ذهب / فضة)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMetal == MetalType.GOLD,
                        onClick = {
                            selectedMetal = MetalType.GOLD
                            priceStr = "3850.0"
                        },
                        label = { Text("الذهب", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedMetal == MetalType.SILVER,
                        onClick = {
                            selectedMetal = MetalType.SILVER
                            priceStr = "44.0"
                        },
                        label = { Text("الفضة", fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFC0C0C0),
                            selectedLabelColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Karat Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (selectedMetal == MetalType.GOLD) {
                        GoldKarat.entries.forEach { karat ->
                            FilterChip(
                                selected = selectedGoldKarat == karat,
                                onClick = { selectedGoldKarat = karat },
                                label = { Text(karat.displayLabel(), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary,
                                    selectedLabelColor = MaterialTheme.colorScheme.background
                                )
                            )
                        }
                    } else {
                        SilverKarat.entries.forEach { karat ->
                            FilterChip(
                                selected = selectedSilverKarat == karat,
                                onClick = { selectedSilverKarat = karat },
                                label = { Text(karat.displayLabel(), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFC0C0C0),
                                    selectedLabelColor = MaterialTheme.colorScheme.background
                                )
                            )
                        }
                    }
                }

                // 2. Scale Photo Direct Camera Verification
                Surface(
                    color = if (scalePhotoCaptured) StatusGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (scalePhotoCaptured) StatusGreen else GoldPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (scalePhotoCaptured) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = if (scalePhotoCaptured) StatusGreen else GoldPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("تصوير شاشة الميزان بالكاميرا", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                    Text("فحص مباشر بالكاميرا بدون معرض", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Button(
                                onClick = { showScaleOcrDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = adaptiveGoldPrimary, contentColor = adaptiveOnPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (scalePhotoCaptured) "إعادة الفحص" else "فتح الكاميرا", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (scalePhotoCaptured) {
                            Surface(
                                color = StatusGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "تم توثيق الميزان بالذكاء الاصطناعي بدقة: $weightStr جرام",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusGreen
                                    )
                                }
                            }
                        }
                    }
                }

                // Weight & Price per Gram Fields
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text("الوزن (جرام)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("سعر الجرام الصافي") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (showScaleOcrDialog) {
                    val currentKarat = if (selectedMetal == MetalType.GOLD) selectedGoldKarat.displayLabel() else selectedSilverKarat.displayLabel()
                    ScaleOcrDialog(
                        inputWeightGrams = weight,
                        inputKarat = currentKarat,
                        onVerified = { extractedWeight, isMatch, confidence, photoCaptured ->
                            if (extractedWeight > 0) weightStr = extractedWeight.toString()
                            scalePhotoCaptured = photoCaptured
                        },
                        onDismiss = { showScaleOcrDialog = false }
                    )
                }

                // Manufacturer & Workmanship Type
                OutlinedTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = { Text("الشركة المصنعة / الدمغة (BTC, Sam, Master, ورشة)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Workmanship Mode Selector (على الجرام vs على القطعة)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("طريقة احتساب المصنعية:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = mfgType == "PER_GRAM",
                            onClick = { mfgType = "PER_GRAM" },
                            label = { Text("على الجرام", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = mfgType == "PER_PIECE",
                            onClick = { mfgType = "PER_PIECE" },
                            label = { Text("على القطعة", fontSize = 11.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = mfgCostStr,
                        onValueChange = { mfgCostStr = it },
                        label = { Text(if (mfgType == "PER_GRAM") "المصنعية/جرام" else "إجمالي المصنعية") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = stampDutyStr,
                        onValueChange = { stampDutyStr = it },
                        label = { Text("الدمغة والضريبة/جرام") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = cashbackRefundStr,
                    onValueChange = { cashbackRefundStr = it },
                    label = { Text("الكاش باك المسترد عند إعادة البيع/جرام") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // --- NEW: Metal Loss / Erosion Section (متوسط تآكل/هالك المعدن سوا ذهب أو فضة) ---
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("متوسط تآكل المعدن (نسبة الفقود/الهالك %)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            }

                            Text(
                                text = "${GoldUtils.formatDecimal(metalLossRatePercent)}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        }

                        // Preset Chips for Metal Loss
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = if (selectedMetal == MetalType.GOLD) {
                                listOf("0.1" to "سبايك 0.1%", "0.5" to "مصاغ 0.5%", "1.0" to "كسر/صهر 1.0%", "2.0" to "مشغولات دقيقة 2%")
                            } else {
                                listOf("0.5" to "سبايك فضة 0.5%", "1.5" to "فضة صهر 1.5%", "2.5" to "فضة تلميع 2.5%", "3.5" to "خردة فضة 3.5%")
                            }

                            presets.forEach { (valStr, label) ->
                                FilterChip(
                                    selected = metalLossRateStr == valStr,
                                    onClick = { metalLossRateStr = valStr },
                                    label = { Text(label, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFF59E0B),
                                        selectedLabelColor = MaterialTheme.colorScheme.background
                                    )
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = metalLossRateStr,
                                onValueChange = { metalLossRateStr = it },
                                label = { Text("نسبة التآكل (%)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text("وزن التآكل/الهالك:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "-${GoldUtils.formatWeight(metalLossGrams)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "الوزن الصافي المستلم: ${GoldUtils.formatWeight(netWeightAfterLoss)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreen
                                )
                            }
                        }
                    }
                }

                // Anti-Fraud Warning Box
                if (isMfgOverpriced) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "️ تنبيه كاشف الغش: قيمة المصنعية أعلى بنسبة 35% من متوسط السوق المعياري لشركة $manufacturer!",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Trader Mention / Store Tagging
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("جهة الشراء والبائع:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { isExternalShop = !isExternalShop }) {
                        Text(if (isExternalShop) "من تاجر بالتطبيق" else "محل خارجي ليس لدينا", fontSize = 11.sp, color = GoldPrimary)
                    }
                }

                if (isExternalShop) {
                    OutlinedTextField(
                        value = externalShopName,
                        onValueChange = { externalShopName = it },
                        label = { Text("اسم المحل / الصاغة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = externalShopLocation,
                        onValueChange = { externalShopLocation = it },
                        label = { Text("عنوان وتحديد موقع المحل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = traderHashtag,
                        onValueChange = { traderHashtag = it },
                        label = { Text("منشن / هاشتاج التاجر (مثال: @تاجر_الأمانة أو الصاغة)") },
                        leadingIcon = {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = GoldPrimary)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Suggested verified traders from contacts
                    val verifiedSuggestions = contacts.filter { contact ->
                        contact.isVerified || contact.type == UserType.GOLD_TRADER.name
                    }

                    if (verifiedSuggestions.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("محلات وتجار موثقون مقترحون:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                verifiedSuggestions.forEach { contact ->
                                    val isSelected = (traderHashtag == "@${contact.name}" || traderHashtag == contact.username)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            traderHashtag = if (!contact.username.isNullOrEmpty()) "@${contact.username}" else "@${contact.name}"
                                        },
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Verified, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.background else GoldPrimary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(contact.name, fontSize = 11.sp)
                                            }
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = GoldPrimary,
                                            selectedLabelColor = MaterialTheme.colorScheme.background
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Summary Calculation Box
                Surface(
                    color = GoldDark.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الوزن القائم الصافي:", fontSize = 12.sp)
                            Text(GoldUtils.formatWeight(weight), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("تآكل المعدن (${GoldUtils.formatDecimal(metalLossRatePercent)}%):", fontSize = 12.sp, color = Color(0xFFF59E0B))
                            Text("-${GoldUtils.formatWeight(metalLossGrams)} (${GoldUtils.formatCurrency(metalLossValue)})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الوزن الصافي القابل للاسترداد:", fontSize = 12.sp, color = StatusGreen)
                            Text(GoldUtils.formatWeight(netWeightAfterLoss), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                        }
                        HorizontalDivider(color = GoldPrimary.copy(alpha = 0.2f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("قيمة المعدن القائم:", fontSize = 12.sp)
                            Text(GoldUtils.formatCurrency(pureMetalTotal), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي المصنعية + الدمغة:", fontSize = 12.sp)
                            Text(GoldUtils.formatCurrency(totalMfgCost + totalStamp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = GoldPrimary.copy(alpha = 0.3f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الإجمالي الكلي للفاتورة:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                            Text(GoldUtils.formatCurrency(grandTotalPaid), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                        }
                    }
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

                    Button(
                        onClick = {
                            val karatStr = if (selectedMetal == MetalType.GOLD) selectedGoldKarat.name else selectedSilverKarat.name
                            val shop = if (isExternalShop) externalShopName else traderHashtag
                            val loc = if (isExternalShop) externalShopLocation else "داخل التطبيق"

                            if (weight > 0 && pricePerGram > 0) {
                                onAddPurchase(
                                    selectedMetal,
                                    weight,
                                    karatStr,
                                    pricePerGram,
                                    manufacturer,
                                    mfgValue,
                                    mfgType,
                                    stampDuty,
                                    cashback,
                                    traderHashtag,
                                    shop,
                                    loc,
                                    if (scalePhotoCaptured) "scale_ocr_capture_${System.currentTimeMillis()}.jpg" else null,
                                    metalLossRatePercent,
                                    metalLossGrams
                                )
                                onDismiss()
                            }
                        },
                        enabled = weight > 0 && pricePerGram > 0,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.weight(1.8f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ الفاتورة والتحويل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
