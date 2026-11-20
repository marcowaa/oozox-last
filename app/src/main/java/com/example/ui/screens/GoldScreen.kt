package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.GoldUtils
import com.example.util.ReportExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoldScreen(
    goldPrices: GoldPricesModel,
    purchases: List<GoldPurchaseEntity>,
    alerts: List<PriceAlertEntity> = emptyList(),
    currentUser: UserEntity?,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    lastSyncTime: Long? = null,
    onUpdatePricesClick: (Double) -> Unit,
    onAddPurchaseClick: () -> Unit,
    onSubmitTraderRequest: (String, String, String) -> Unit,
    onOpenMerchantStore: (merchantId: String?, isManageMode: Boolean) -> Unit = { _, _ -> },
    onAddAlert: (metalType: String, karat: String, targetPrice: Double, direction: String, note: String?) -> Unit = { _, _, _, _, _ -> },
    onDeleteAlert: (alertId: String) -> Unit = {},
    onToggleAlert: (alertId: String, isActive: Boolean) -> Unit = { _, _ -> },
    onTestTriggerAlert: (alert: PriceAlertEntity) -> Unit = {},
    onResetAlertTrigger: (alertId: String) -> Unit = {},
    onOpenFinancialAccountant: () -> Unit = {},
    onAddScannedPurchase: ((
        metalType: MetalType,
        weight: Double,
        karat: String,
        pricePerGram: Double,
        manufacturer: String,
        manufacturingCost: Double,
        sellerShopName: String,
        notes: String?
    ) -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var activeSection by remember { mutableStateOf(GoldDrawerSection.MARKET) }

    BackHandler(enabled = drawerState.isOpen || activeSection != GoldDrawerSection.MARKET || onBackClick != null) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            activeSection != GoldDrawerSection.MARKET -> activeSection = GoldDrawerSection.MARKET
            onBackClick != null -> onBackClick()
        }
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var activeMetalTab by remember { mutableStateOf(MetalType.GOLD) }
    var selectedGoldKarat by remember { mutableStateOf(GoldKarat.KARAT_21) }
    var selectedSilverKarat by remember { mutableStateOf(SilverKarat.KARAT_925) }

    var calcWeightStr by remember { mutableStateOf("10") }
    var calcMfgStr by remember { mutableStateOf("150") }
    var zakatWeightStr by remember { mutableStateOf("100") }

    var showPriceEditModal by remember { mutableStateOf(false) }
    var showQuotationModal by remember { mutableStateOf(false) }
    var showAddAlertDialog by remember { mutableStateOf(false) }
    var showGoldQrScannerDialog by remember { mutableStateOf(false) }
    var showGoldQrGeneratorDialog by remember { mutableStateOf(false) }
    var selectedPurchaseForQr by remember { mutableStateOf<GoldPurchaseEntity?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "gold_sync_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gold_sync_angle"
    )

    val currentGramPrice = remember(activeMetalTab, selectedGoldKarat, selectedSilverKarat, goldPrices) {
        if (activeMetalTab == MetalType.GOLD) {
            when (selectedGoldKarat) {
                GoldKarat.KARAT_14 -> goldPrices.karat14
                GoldKarat.KARAT_18 -> goldPrices.karat18
                GoldKarat.KARAT_21 -> goldPrices.karat21
                GoldKarat.KARAT_24 -> goldPrices.karat24
            }
        } else {
            when (selectedSilverKarat) {
                SilverKarat.KARAT_800 -> goldPrices.silver800
                SilverKarat.KARAT_925 -> goldPrices.silver925
                SilverKarat.KARAT_999 -> goldPrices.silver999
            }
        }
    }

    val calcWeight = remember(calcWeightStr) { calcWeightStr.toDoubleOrNull() ?: 0.0 }
    val calcMfg = remember(calcMfgStr) { calcMfgStr.toDoubleOrNull() ?: 0.0 }
    val (totalMetalPrice, totalMfg, grandTotalPrice) = remember(calcWeight, currentGramPrice, calcMfg) {
        GoldUtils.calculateGoldPrice(calcWeight, currentGramPrice, calcMfg)
    }

    val zakatWeight = remember(zakatWeightStr) { zakatWeightStr.toDoubleOrNull() ?: 0.0 }
    val zakatRes = remember(zakatWeight, goldPrices.karat21) {
        GoldUtils.calculateZakat(zakatWeight, goldPrices.karat21)
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryAccent = adaptiveGoldPrimary
    val onPrimaryAccent = adaptiveOnPrimary
    val isMerchant = currentUser?.isVerified == true ||
        currentUser?.verificationStatus == "APPROVED" ||
        currentUser?.type == UserType.GOLD_TRADER.name

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            GoldSideDrawerContent(
                currentUser = currentUser,
                selectedSection = activeSection,
                purchasesCount = purchases.size,
                activeAlertsCount = alerts.count { it.isActive },
                onSelectSection = { activeSection = it },
                onOpenMerchantStore = { onOpenMerchantStore(currentUser?.id, isMerchant) },
                onAddPurchase = onAddPurchaseClick,
                onOpenQrScanner = { showGoldQrScannerDialog = true },
                onOpenQrGenerator = {
                    selectedPurchaseForQr = null
                    showGoldQrGeneratorDialog = true
                },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Unified Human-Crafted Top App Bar with Side Drawer Trigger
            Surface(
                color = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color(0xFFEFF3F8),
                tonalElevation = 1.dp,
                shadowElevation = if (isDarkTheme) 0.dp else 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_gold_open_drawer")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "قائمة قسم الذهب",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (onBackClick != null) {
                                IconButton(
                                    onClick = onBackClick,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "رجوع",
                                        tint = primaryAccent
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = if (activeSection == GoldDrawerSection.MARKET) "الذهب والمصوغات" else activeSection.titleAr,
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (lastSyncTime != null) {
                                        "محدث ${java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(lastSyncTime))}"
                                    } else {
                                        "تداول مباشر"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "تحديث الأسعار",
                                    tint = primaryAccent,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = if (isRefreshing) rotationAngle else 0f }
                                )
                            }

                            FilledTonalIconButton(
                                onClick = onAddPurchaseClick,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = primaryAccent,
                                    contentColor = onPrimaryAccent
                                ),
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("btn_gold_add_quick")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "تسجيل عملية",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        thickness = 0.8.dp
                    )
                }
            }

            // Main Spacious Viewport by Drawer Section
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (activeSection) {
                    GoldDrawerSection.MARKET -> {
                        // Metal Switcher (Gold / Silver)
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        MetalType.GOLD to "الذهب",
                                        MetalType.SILVER to "الفضة"
                                    ).forEach { (type, label) ->
                                        val isSel = activeMetalTab == type
                                        val bg by animateColorAsState(
                                            targetValue = if (isSel) primaryAccent else Color.Transparent,
                                            label = "metal_tab_bg"
                                        )
                                        Surface(
                                            color = bg,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    activeMetalTab = type
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = label,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) onPrimaryAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Spot Hero Banner (Clean, restrained M3 hierarchy)
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(18.dp),
                                tonalElevation = 1.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (activeMetalTab == MetalType.GOLD) "عيار 21" else "فضة 925",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = GoldUtils.formatCurrency(
                                                    if (activeMetalTab == MetalType.GOLD) goldPrices.karat21 else goldPrices.silver925
                                                ),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = primaryAccent
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedIconButton(
                                                onClick = { showPriceEditModal = true },
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "تعديل السعر",
                                                    tint = primaryAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (isLoading) {
                                        GoldPricesHeaderSkeleton()
                                    } else if (activeMetalTab == MetalType.GOLD) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            PriceTile("عيار 24", goldPrices.karat24, modifier = Modifier.weight(1f))
                                            PriceTile("عيار 18", goldPrices.karat18, modifier = Modifier.weight(1f))
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            PriceTile("عيار 14", goldPrices.karat14, modifier = Modifier.weight(1f))
                                            PriceTile("الجنيه الذهب", goldPrices.goldPound, modifier = Modifier.weight(1f))
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            PriceTile("الأوقية ($)", goldPrices.ounceUSD, isUsd = true, modifier = Modifier.weight(1f))
                                        }
                                    } else {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            PriceTile("فضة 999", goldPrices.silver999, modifier = Modifier.weight(1f))
                                            PriceTile("فضة 800", goldPrices.silver800, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        // Compact Calculator Card
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = if (activeMetalTab == MetalType.GOLD) "حاسبة الذهب" else "حاسبة الفضة",
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (activeMetalTab == MetalType.GOLD) {
                                            GoldKarat.entries.forEach { karat ->
                                                val isSel = selectedGoldKarat == karat
                                                FilterChip(
                                                    selected = isSel,
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        selectedGoldKarat = karat
                                                    },
                                                    label = {
                                                        Text(
                                                            karat.displayLabel(),
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = primaryAccent,
                                                        selectedLabelColor = onPrimaryAccent
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        } else {
                                            SilverKarat.entries.forEach { karat ->
                                                val isSel = selectedSilverKarat == karat
                                                FilterChip(
                                                    selected = isSel,
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        selectedSilverKarat = karat
                                                    },
                                                    label = {
                                                        Text(
                                                            karat.displayLabel(),
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = primaryAccent,
                                                        selectedLabelColor = onPrimaryAccent
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("1" to "1 جم", "5" to "5 جم", "10" to "10 جم", "8" to "جنيه", "31.1" to "أونصة").forEach { (w, lbl) ->
                                            SuggestionChip(
                                                onClick = { calcWeightStr = w },
                                                label = { Text(lbl, fontSize = 11.5.sp, fontWeight = FontWeight.Medium) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = if (calcWeightStr == w) primaryAccent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                )
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = calcWeightStr,
                                            onValueChange = { calcWeightStr = it },
                                            label = { Text("الوزن (جم)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = calcMfgStr,
                                            onValueChange = { calcMfgStr = it },
                                            label = { Text("المصنعية/جم") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("المعدن", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(GoldUtils.formatCurrency(totalMetalPrice), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("المصنعية", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(GoldUtils.formatCurrency(totalMfg), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("الإجمالي", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = primaryAccent)
                                                Text(GoldUtils.formatCurrency(grandTotalPrice), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = primaryAccent)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { showQuotationModal = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = primaryAccent, contentColor = onPrimaryAccent),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("btn_export_quotation")
                                        ) {
                                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("فاتورة PDF", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val quoteText = ReportExporter.formatQuotationText(
                                                    buyerName = "عميل",
                                                    metalType = if (activeMetalTab == MetalType.GOLD) "ذهب" else "فضة",
                                                    karat = if (activeMetalTab == MetalType.GOLD) selectedGoldKarat.name.replace("KARAT_", "عيار ") else selectedSilverKarat.name.replace("KARAT_", "عيار "),
                                                    weightGrams = calcWeight,
                                                    gramPrice = currentGramPrice,
                                                    mfgCostPerGram = calcMfg,
                                                    vatAmount = 0.0,
                                                    stampDuty = 0.0,
                                                    grandTotal = grandTotalPrice
                                                )
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Quotation", quoteText))
                                                Toast.makeText(context, "تم النسخ", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("btn_copy_quotation_text")
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("نسخ السعر", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = primaryAccent)
                                        }
                                    }
                                }
                            }
                        }

                        // Compact Merchant Store Shortcut
                        item {
                            Surface(
                                onClick = { onOpenMerchantStore(currentUser?.id, isMerchant) },
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(primaryAccent.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Storefront, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(20.dp))
                                        }
                                        Column {
                                            Text(
                                                text = if (isMerchant) "إدارة المتجر" else "معرض المتاجر",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "تصفح العروض أو أدر معرضك",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    GoldDrawerSection.PORTFOLIO -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "سجل الحيازة (${purchases.size})",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = onAddPurchaseClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryAccent, contentColor = onPrimaryAccent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(42.dp).testTag("btn_manual_add_gold_purchase")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }
                        }

                        if (isLoading) {
                            item { GoldPurchasesListSkeleton(count = 3) }
                        } else if (purchases.isEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(34.dp))
                                        Text("لا توجد فواتير مسجلة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        } else {
                            items(purchases, key = { it.id }) { purchase ->
                                val netW = (purchase.extractedWeight - purchase.metalLossGrams).coerceAtLeast(0.0)
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Surface(
                                                    color = if (purchase.metalType == "GOLD") primaryAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (purchase.metalType == "GOLD") "ذهب" else "فضة",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (purchase.metalType == "GOLD") primaryAccent else MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = "${GoldUtils.formatWeight(purchase.extractedWeight)} • ${purchase.karat}",
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = GoldUtils.formatCurrency(purchase.totalPaid),
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = primaryAccent
                                            )
                                        }

                                        Text(
                                            text = "${purchase.manufacturer} • ${purchase.sellerShopName ?: purchase.traderHashtag}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "الصافي: ${GoldUtils.formatWeight(netW)}",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = adaptiveStatusGreen
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    selectedPurchaseForQr = purchase
                                                    showGoldQrGeneratorDialog = true
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier
                                                    .height(34.dp)
                                                    .testTag("btn_generate_qr_purchase_${purchase.id}")
                                            ) {
                                                Icon(Icons.Default.QrCode2, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("QR", fontSize = 11.sp, color = primaryAccent, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GoldDrawerSection.ALERTS -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("تنبيهات الأسعار (${alerts.size})", fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { showAddAlertDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryAccent, contentColor = onPrimaryAccent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(42.dp).testTag("btn_add_price_alert")
                                ) {
                                    Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تنبيه جديد", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                }
                            }
                        }

                        if (alerts.isEmpty()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("لا توجد تنبيهات نشطة", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else {
                            items(alerts, key = { it.id }) { alert ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (alert.isTriggered) primaryAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("alert_item_${alert.id}")
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = when (alert.karat) {
                                                        "POUND", "GOLD_POUND" -> "الجنيه الذهب"
                                                        "OUNCE", "OUNCE_USD" -> "الأونصة"
                                                        else -> if (alert.metalType == "GOLD") "ذهب ${alert.karat}" else "فضة ${alert.karat}"
                                                    },
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = primaryAccent
                                                )
                                                Text(
                                                    text = if (alert.direction == "ABOVE" || alert.direction == "أعلى") "صعود" else "هبوط",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (alert.direction == "ABOVE" || alert.direction == "أعلى") adaptiveStatusGreen else adaptiveStatusRed
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Switch(
                                                    checked = alert.isActive,
                                                    onCheckedChange = { onToggleAlert(alert.id, it) },
                                                    modifier = Modifier.testTag("switch_alert_${alert.id}")
                                                )
                                                IconButton(
                                                    onClick = { onDeleteAlert(alert.id) },
                                                    modifier = Modifier.size(40.dp).testTag("btn_delete_alert_${alert.id}")
                                                ) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = adaptiveStatusRed)
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = GoldUtils.formatCurrency(alert.targetPrice),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                if (alert.isTriggered) {
                                                    OutlinedButton(
                                                        onClick = { onResetAlertTrigger(alert.id) },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(34.dp)
                                                    ) {
                                                        Text("إعادة", fontSize = 11.sp)
                                                    }
                                                }
                                                OutlinedButton(
                                                    onClick = { onTestTriggerAlert(alert) },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("تجربة", fontSize = 11.sp, color = primaryAccent)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GoldDrawerSection.ZAKAT -> {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("حاسبة الزكاة", fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = zakatWeightStr,
                                        onValueChange = { zakatWeightStr = it },
                                        label = { Text("الوزن المملوك (جم عيار 21)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Surface(
                                        color = if (zakatRes.isNisabMet) adaptiveStatusGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = if (zakatRes.isNisabMet) "بلغ النصاب الشرعي (85 جم)" else "أقل من النصاب",
                                                fontWeight = FontWeight.Bold,
                                                color = if (zakatRes.isNisabMet) adaptiveStatusGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (zakatRes.isNisabMet) {
                                                    "الزكاة المستحقة (2.5%): ${GoldUtils.formatCurrency(zakatRes.zakatAmount)}"
                                                } else {
                                                    "متبقي: ${GoldUtils.formatWeight((85.0 - zakatWeight).coerceAtLeast(0.0))}"
                                                },
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GoldDrawerSection.ADVISOR -> {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("financial_advisor_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(primaryAccent.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Insights, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(20.dp))
                                        }
                                        Column {
                                            Text("المستشار المالي", fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                                            Text("تحليل السوق وتوقيت البيع والشراء", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Button(
                                        onClick = onOpenFinancialAccountant,
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryAccent, contentColor = onPrimaryAccent),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Text("فتح المستشار المالي", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    GoldDrawerSection.QR_CENTER -> {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("مركز رموز QR", fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = { showGoldQrScannerDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = primaryAccent, contentColor = onPrimaryAccent),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("btn_scan_gold_qr_purchase")
                                        ) {
                                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("مسح QR", fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                selectedPurchaseForQr = null
                                                showGoldQrGeneratorDialog = true
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("btn_generate_gold_qr_purchase")
                                        ) {
                                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = primaryAccent, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("توليد QR", fontWeight = FontWeight.Bold, color = primaryAccent)
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

    // Edit Price Dialog (Keyboard & Nav Safe)
    if (showPriceEditModal) {
        var newPriceStr by remember { mutableStateOf(goldPrices.karat21.toString()) }
        Dialog(
            onDismissRequest = { showPriceEditModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .navigationBarsPadding()
                    .imePadding(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("تحديث سعر الذهب", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newPriceStr,
                        onValueChange = { newPriceStr = it },
                        label = { Text("سعر جرام عيار 21 (ج.م)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showPriceEditModal = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("إلغاء")
                        }
                        Button(
                            onClick = {
                                val p = newPriceStr.toDoubleOrNull()
                                if (p != null && p > 0) {
                                    onUpdatePricesClick(p)
                                    showPriceEditModal = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryAccent, contentColor = onPrimaryAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("تحديث", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Quotation PDF Dialog (Scrollable & Pinned Actions)
    if (showQuotationModal) {
        var buyerName by remember { mutableStateOf("") }
        var storeName by remember { mutableStateOf(currentUser?.traderShopName ?: currentUser?.name ?: "المتجر") }
        var invoiceNotes by remember { mutableStateOf("الأسعار سارية لنهاية اليوم.") }

        Dialog(
            onDismissRequest = { showQuotationModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.93f)
                    .navigationBarsPadding()
                    .imePadding()
                    .heightIn(max = 600.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إصدار فاتورة PDF", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showQuotationModal = false }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = buyerName,
                            onValueChange = { buyerName = it },
                            label = { Text("اسم العميل") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            label = { Text("اسم المتجر") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${if (activeMetalTab == MetalType.GOLD) "ذهب" else "فضة"} • $calcWeight جم",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("الإجمالي: ${GoldUtils.formatCurrency(grandTotalPrice)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryAccent)
                            }
                        }
                        OutlinedTextField(
                            value = invoiceNotes,
                            onValueChange = { invoiceNotes = it },
                            label = { Text("ملاحظات") },
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showQuotationModal = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("إلغاء")
                        }
                        Button(
                            onClick = {
                                ReportExporter.exportQuotationPdf(
                                    context = context,
                                    buyerName = buyerName,
                                    sellerStoreName = storeName,
                                    metalType = if (activeMetalTab == MetalType.GOLD) "ذهب" else "فضة",
                                    karat = if (activeMetalTab == MetalType.GOLD) selectedGoldKarat.name.replace("KARAT_", "عيار ") else selectedSilverKarat.name.replace("KARAT_", "عيار "),
                                    weightGrams = calcWeight,
                                    gramPrice = currentGramPrice,
                                    mfgCostPerGram = calcMfg,
                                    vatAmount = 0.0,
                                    stampDuty = 0.0,
                                    grandTotal = grandTotalPrice,
                                    notes = invoiceNotes
                                )
                                showQuotationModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryAccent, contentColor = onPrimaryAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("مشاركة PDF", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showGoldQrScannerDialog) {
        GoldQrScannerDialog(
            onDismiss = { showGoldQrScannerDialog = false },
            onAddPurchase = { metal, weight, karat, price, mfg, mfgCost, shop, notes ->
                if (onAddScannedPurchase != null) {
                    onAddScannedPurchase(metal, weight, karat, price, mfg, mfgCost, shop, notes)
                } else {
                    onAddPurchaseClick()
                }
            }
        )
    }

    if (showGoldQrGeneratorDialog) {
        GoldQrGeneratorDialog(
            initialPurchase = selectedPurchaseForQr,
            onDismiss = {
                showGoldQrGeneratorDialog = false
                selectedPurchaseForQr = null
            }
        )
    }

    if (showAddAlertDialog) {
        var alertMetalType by remember { mutableStateOf("GOLD") }
        var alertKarat by remember { mutableStateOf("21") }
        var alertDirection by remember { mutableStateOf("ABOVE") }
        var alertNote by remember { mutableStateOf("") }

        val currentRefPrice: Double = remember(alertMetalType, alertKarat, goldPrices) {
            if (alertMetalType == "SILVER") {
                when (alertKarat) {
                    "999" -> goldPrices.silver999
                    "925" -> goldPrices.silver925
                    "800" -> goldPrices.silver800
                    else -> goldPrices.silver925
                }
            } else {
                when (alertKarat) {
                    "24" -> goldPrices.karat24
                    "21" -> goldPrices.karat21
                    "18" -> goldPrices.karat18
                    "14" -> goldPrices.karat14
                    "POUND" -> goldPrices.goldPound
                    "OUNCE" -> goldPrices.ounceUSD
                    else -> goldPrices.karat21
                }
            }
        }
        var targetPriceStr by remember { mutableStateOf((currentRefPrice * 1.02).toInt().toString()) }

        Dialog(
            onDismissRequest = { showAddAlertDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.93f)
                    .navigationBarsPadding()
                    .imePadding()
                    .heightIn(max = 640.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تنبيه سعر جديد", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showAddAlertDialog = false }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    alertMetalType = "GOLD"
                                    alertKarat = "21"
                                    targetPriceStr = (goldPrices.karat21 * 1.02).toInt().toString()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (alertMetalType == "GOLD") primaryAccent else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (alertMetalType == "GOLD") onPrimaryAccent else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("ذهب", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    alertMetalType = "SILVER"
                                    alertKarat = "925"
                                    targetPriceStr = (goldPrices.silver925 * 1.02).toInt().toString()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (alertMetalType == "SILVER") primaryAccent else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (alertMetalType == "SILVER") onPrimaryAccent else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("فضة", fontWeight = FontWeight.Bold)
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val opts = if (alertMetalType == "GOLD") {
                                listOf("24" to "24", "21" to "21", "18" to "18", "14" to "14", "POUND" to "جنيه", "OUNCE" to "أونصة")
                            } else {
                                listOf("999" to "999", "925" to "925", "800" to "800")
                            }
                            opts.forEach { (k, label) ->
                                FilterChip(
                                    selected = alertKarat == k,
                                    onClick = { alertKarat = k },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = alertDirection == "ABOVE",
                                onClick = { alertDirection = "ABOVE" },
                                label = { Text("عند الصعود") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = alertDirection == "BELOW",
                                onClick = { alertDirection = "BELOW" },
                                label = { Text("عند الهبوط") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = targetPriceStr,
                            onValueChange = { targetPriceStr = it },
                            label = { Text("السعر المستهدف (${if (alertKarat == "OUNCE") "$" else "ج.م"})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = alertNote,
                            onValueChange = { alertNote = it },
                            label = { Text("ملاحظة (اختياري)") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddAlertDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("إلغاء")
                        }
                        Button(
                            onClick = {
                                val targetP = targetPriceStr.toDoubleOrNull() ?: 0.0
                                if (targetP > 0) {
                                    onAddAlert(alertMetalType, alertKarat, targetP, alertDirection, alertNote.ifBlank { null })
                                    showAddAlertDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryAccent, contentColor = onPrimaryAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("حفظ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PriceTile(
    title: String,
    price: Double,
    isUsd: Boolean = false,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "price_tile_scale"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    val formatted = if (isUsd) "$${price.toInt()}" else GoldUtils.formatCurrency(price)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("GoldPrice", "$title: $formatted"))
                    Toast.makeText(context, "تم نسخ $title", Toast.LENGTH_SHORT).show()
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isUsd) "$${price.toInt()}" else GoldUtils.formatCurrency(price),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
