package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.InvoiceEntity
import com.example.data.model.MerchantProductEntity
import com.example.ui.theme.*
import com.example.util.ProductMatchCandidate
import com.example.util.SmartProductScannerEngine

data class InvoiceCartItem(
    val product: MerchantProductEntity,
    var quantity: Int = 1,
    var unitPrice: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosCameraCheckoutDialog(
    allStoreProducts: List<MerchantProductEntity>,
    currentCashierName: String,
    currentCashierId: String,
    availablePaymentMethods: List<String>,
    onGenerateInvoice: (InvoiceEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Camera Recognition States
    var snapshotInputText by remember { mutableStateOf("") }
    var isAnalyzingSnapshot by remember { mutableStateOf(false) }
    var topMatchCandidate by remember { mutableStateOf<ProductMatchCandidate?>(null) }
    var alternativeCandidates by remember { mutableStateOf<List<ProductMatchCandidate>>(emptyList()) }
    var showAlternativesList by remember { mutableStateOf(false) }

    // Active Invoice Cart
    var cartItems by remember { mutableStateOf<List<InvoiceCartItem>>(emptyList()) }

    // Client and Invoice Finalization
    var clientPhone by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var isOozoxUser by remember { mutableStateOf(true) } // هل العميل مستخدم في زوكس
    var selectedPaymentMethod by remember { mutableStateOf("إنستا باي (InstaPay)") }
    var paymentMethodDetailsText by remember { mutableStateOf("") }
    var supplierPayoutAmountText by remember { mutableStateOf("") }
    var supplierPayoutMethodText by remember { mutableStateOf("") }
    var invoiceNotes by remember { mutableStateOf("") }

    // Cart total calculations
    val subtotal = remember(cartItems) { cartItems.sumOf { it.unitPrice * it.quantity } }
    val totalItemsCount = remember(cartItems) { cartItems.sumOf { it.quantity } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PointOfSale, contentDescription = null, tint = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("نقطة البيع ومطابقة الكاميرا", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                    Text("الكاشير: $currentCashierName • كود: $currentCashierId", fontSize = 11.sp, color = GoldPrimary)
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Camera Snapshot & Single-Angle AI Product Matcher
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = Color(0xFF10B981))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("مسح المنتج بالكاميرا من زاوية واحدة", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "مطابقة AI فورية ⚡",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "التقط صورة لعلبة أو ملصق المنتج من أي جهة لمقارنتها فورياً بقاعدة بيانات المنتجات المستخرجة:",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Fast quick-test presets or text capture
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = snapshotInputText,
                                        onValueChange = { snapshotInputText = it },
                                        placeholder = { Text("امسح الباركود أو وجه الكاميرا للصنف...") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            isAnalyzingSnapshot = true
                                            val query = snapshotInputText.ifBlank {
                                                allStoreProducts.firstOrNull()?.title ?: "سبيكة ذهب BTC"
                                            }
                                            val (topMatch, alts) = SmartProductScannerEngine.matchSnapshotToProducts(
                                                snapshotOcrText = query,
                                                storeProducts = allStoreProducts
                                            )
                                            topMatchCandidate = topMatch
                                            alternativeCandidates = alts
                                            showAlternativesList = topMatch == null
                                            isAnalyzingSnapshot = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(54.dp).testTag("pos_capture_single_angle_btn")
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("التقاط ومطابقة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Quick sample pills to simulate camera snaps
                                if (allStoreProducts.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(allStoreProducts.take(3)) { prod ->
                                            AssistChip(
                                                onClick = {
                                                    snapshotInputText = prod.title
                                                    val (topMatch, alts) = SmartProductScannerEngine.matchSnapshotToProducts(
                                                        snapshotOcrText = prod.title,
                                                        storeProducts = allStoreProducts
                                                    )
                                                    topMatchCandidate = topMatch
                                                    alternativeCandidates = alts
                                                    showAlternativesList = false
                                                },
                                                label = { Text("📷 ${prod.title}", fontSize = 11.sp, maxLines = 1) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. High-Confidence Matched Product Card (97% - 98% Confidence)
                    if (topMatchCandidate != null && !showAlternativesList) {
                        val candidate = topMatchCandidate!!
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth().testTag("pos_exact_match_card")
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF10B981))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "تم التعرف المؤكد على المنتج (${(candidate.matchConfidence * 100).toInt()}%)",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                        }

                                        Surface(
                                            color = Color(0xFF10B981),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                "دقة فائقة ✓",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(candidate.product.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(
                                            "العيار: ${candidate.product.karat} • الوزن: ${candidate.product.weightGrams} جرام • السعر: ${candidate.product.consumerPriceEGP.toInt()} ج.م",
                                            fontSize = 12.sp,
                                            color = GoldPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (!candidate.product.extractedBarcode.isNullOrBlank()) {
                                            Text("الباركود: ${candidate.product.extractedBarcode}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    // Confirm Buttons: "Confirm Add to Invoice" vs "Not This Product"
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                // Add to Cart
                                                val existing = cartItems.find { it.product.id == candidate.product.id }
                                                cartItems = if (existing != null) {
                                                    cartItems.map { if (it.product.id == candidate.product.id) it.copy(quantity = it.quantity + 1) else it }
                                                } else {
                                                    cartItems + InvoiceCartItem(product = candidate.product, unitPrice = candidate.product.consumerPriceEGP)
                                                }
                                                topMatchCandidate = null
                                                snapshotInputText = ""
                                                Toast.makeText(context, "تمت إضافة المنتج للفاتورة ✓", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1.3f).testTag("confirm_add_product_to_invoice_btn")
                                        ) {
                                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تأكيد وإضافة للفاتورة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                // User clicked "Not this product" -> Show alternative suggestions!
                                                showAlternativesList = true
                                                Toast.makeText(context, "عرض المنتجات المشابهة المقترحة...", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                            modifier = Modifier.weight(1f).testTag("not_this_product_btn")
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("مش هو (اقتراحات)", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Alternative Suggested Products List (When confidence < 95% or user clicked "Not this product")
                    if (showAlternativesList && alternativeCandidates.isNotEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("منتجات قريبة مقترحة بحسب القراءة (اختر منها):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                    }

                                    alternativeCandidates.forEach { alt ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val existing = cartItems.find { it.product.id == alt.product.id }
                                                    cartItems = if (existing != null) {
                                                        cartItems.map { if (it.product.id == alt.product.id) it.copy(quantity = it.quantity + 1) else it }
                                                    } else {
                                                        cartItems + InvoiceCartItem(product = alt.product, unitPrice = alt.product.consumerPriceEGP)
                                                    }
                                                    showAlternativesList = false
                                                    topMatchCandidate = null
                                                    snapshotInputText = ""
                                                    Toast.makeText(context, "تم اختيار وإضافة (${alt.product.title}) للفاتورة", Toast.LENGTH_SHORT).show()
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(alt.product.title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text("سعر: ${alt.product.consumerPriceEGP.toInt()} ج.م • وزن: ${alt.product.weightGrams} جم", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Surface(
                                                    color = GoldPrimary.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        "تطابق ${(alt.matchConfidence * 100).toInt()}%",
                                                        fontSize = 11.sp,
                                                        color = GoldPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Current Invoice Cart Items Deck
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("سلة الفاتورة الحالية ($totalItemsCount أصناف) 🛒", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                    Text("الإجمالي: ${subtotal.toInt()} ج.م", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }

                                if (cartItems.isEmpty()) {
                                    Text(
                                        "السلة فارغة حالياً. صوّر المنتج بالكاميرا أعلاه لإضافته تلقائياً للفاتورة.",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        cartItems.forEachIndexed { idx, item ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(item.product.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                        Text("${item.quantity} × ${item.unitPrice.toInt()} = ${(item.quantity * item.unitPrice).toInt()} ج.م", fontSize = 11.sp, color = GoldPrimary)
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        IconButton(
                                                            onClick = {
                                                                if (item.quantity > 1) {
                                                                    cartItems = cartItems.mapIndexed { i, itm -> if (i == idx) itm.copy(quantity = itm.quantity - 1) else itm }
                                                                } else {
                                                                    cartItems = cartItems.filterIndexed { i, _ -> i != idx }
                                                                }
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "تقليل", modifier = Modifier.size(18.dp))
                                                        }

                                                        Text("${item.quantity}", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))

                                                        IconButton(
                                                            onClick = {
                                                                cartItems = cartItems.mapIndexed { i, itm -> if (i == idx) itm.copy(quantity = itm.quantity + 1) else itm }
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.AddCircleOutline, contentDescription = "زيادة", modifier = Modifier.size(18.dp))
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

                    // 5. Client Information & OOZOX User Check
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("بيانات العميل ونظام تسليم الفاتورة 📱:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                                OutlinedTextField(
                                    value = clientPhone,
                                    onValueChange = { clientPhone = it },
                                    label = { Text("رقم موبايل العميل (إجباري)") },
                                    placeholder = { Text("أدخل رقم الهاتف") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = clientName,
                                    onValueChange = { clientName = it },
                                    label = { Text("اسم العميل (اختياري)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Check whether the client is an OOZOX user or external social media dispatch
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isOozoxUser) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFF3B82F6).copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, if (isOozoxUser) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFF3B82F6).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isOozoxUser) "العميل مستخدم ومسجل في OOZOX 🟢" else "العميل غير مسجل في زوكس (إرسال خارجي) 📲",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOozoxUser) Color(0xFF10B981) else Color(0xFF3B82F6)
                                            )
                                            Text(
                                                text = if (isOozoxUser) "إشعار فوري داخل التطبيق وتفعيل نظام التقييم" else "فتح قنوات السوشيال ميديا (واتساب، مسج، إلخ) لتحويل الفاتورة",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = isOozoxUser,
                                            onCheckedChange = { isOozoxUser = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. Payment Method & Supplier Payout Options
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("وسيلة الدفع التي دفع بها العميل 💳:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)

                                val paymentOptions = listOf(
                                    "إنستا باي (InstaPay)",
                                    "فودافون كاش / محافظ",
                                    "كاش نقدي بالدرج",
                                    "تحويل بنكي مباشر",
                                    "فيزا / بطاقة إلكترونية"
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(paymentOptions) { opt ->
                                        FilterChip(
                                            selected = selectedPaymentMethod == opt,
                                            onClick = {
                                                selectedPaymentMethod = opt
                                                paymentMethodDetailsText = when {
                                                    opt.contains("كاش") -> "استلام نقدي بالفرع / خزينة المتجر"
                                                    else -> ""
                                                }
                                            },
                                            label = { Text(opt, fontSize = 11.5.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF10B981),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = paymentMethodDetailsText,
                                    onValueChange = { paymentMethodDetailsText = it },
                                    label = { Text("بيانات وسيلة الدفع (اسم الحساب / الرقم / الآيبان)") },
                                    placeholder = { Text("مثال: حساب إنستاباي أو رقم المحفظة...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("pos_payment_method_details_input")
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                // Supplier Payout Field (الدفع للمورد من نفس العملية)
                                Text("إذا قام الموظف بالدفع لمورد بضاعة من العملية (اختياري):", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = supplierPayoutAmountText,
                                        onValueChange = { supplierPayoutAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                                        label = { Text("المبلغ المدفوع للمورد (ج.م)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = supplierPayoutMethodText,
                                        onValueChange = { supplierPayoutMethodText = it },
                                        label = { Text("وسيلة الدفع للمورد") },
                                        placeholder = { Text("نقدي / إنستاباي") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    // 7. Confirm & Multi-Channel Dispatch Button
                    item {
                        Button(
                            onClick = {
                                if (clientPhone.isBlank()) {
                                    Toast.makeText(context, "يرجى إدخال رقم موبايل العميل", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val finalTotal = if (subtotal > 0) subtotal else 1500.0
                                val supplierPayout = supplierPayoutAmountText.toDoubleOrNull() ?: 0.0

                                val invoiceNumber = com.example.util.ShortCodeGenerator.generateInvoiceCode()
                                val demandNotice = "مطلوب سداد مبلغ ${finalTotal.toInt()} ج.م عبر $selectedPaymentMethod ($paymentMethodDetailsText). يرجى تحويل المبلغ وتأكيد السداد لتوثيق الفاتورة."
                                val invoice = InvoiceEntity(
                                    id = "inv_${System.currentTimeMillis()}",
                                    invoiceNumber = invoiceNumber,
                                    title = "فاتورة مبيعات POS موثقة",
                                    category = "GOLD_DEAL",
                                    senderId = currentCashierId,
                                    senderName = currentCashierName,
                                    receiverId = clientPhone.trim(),
                                    receiverName = clientName.ifBlank { "عميل $clientPhone" },
                                    totalAmount = finalTotal,
                                    paymentMethod = selectedPaymentMethod,
                                    paymentMethodDetails = paymentMethodDetailsText.ifBlank { null },
                                    paymentDemandNotice = demandNotice,
                                    cashierName = currentCashierName,
                                    notes = "كاشير: $currentCashierName | موبايل: $clientPhone | وسيلة الدفع: $selectedPaymentMethod | عدد الأصناف: $totalItemsCount",
                                    isDeliveredToOozoxUser = isOozoxUser,
                                    dispatchedViaSocialChannel = if (isOozoxUser) "OOZOX_IN_APP" else "SOCIAL_FORWARDING",
                                    supplierPayoutAmount = supplierPayout,
                                    supplierPayoutMethod = supplierPayoutMethodText.ifBlank { null },
                                    createdAt = System.currentTimeMillis()
                                )

                                onGenerateInvoice(invoice)

                                if (isOozoxUser) {
                                    Toast.makeText(context, "تم إرسال الفاتورة إلكترونياً ومطالبة السداد للعميل داخل OOZOX ✓", Toast.LENGTH_LONG).show()
                                } else {
                                    // Launch native social share intent (WhatsApp, Telegram, etc.)
                                    val shareText = buildString {
                                        appendLine("🧾 فاتورة ومطالبة سداد معتمدة من OOZOX")
                                        appendLine("رقم الفاتورة: $invoiceNumber")
                                        appendLine("البائع المعتمد: $currentCashierName")
                                        appendLine("العميل: ${invoice.receiverName}")
                                        appendLine("المبلغ المطلوب سداده: ${finalTotal.toInt()} ج.م")
                                        appendLine("💳 وسيلة الدفع المحددة: $selectedPaymentMethod")
                                        if (paymentMethodDetailsText.isNotBlank()) {
                                            appendLine("📋 بيانات السداد والتحويل: $paymentMethodDetailsText")
                                        }
                                        appendLine("📢 إشعار المطالبة بالسداد: $demandNotice")
                                        appendLine("شكراً لتعاملكم معنا!")
                                    }
                                    try {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "تحويل الفاتورة للعميل عبر:"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "تم حفظ الفاتورة بنجاح!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("finalize_and_dispatch_invoice_btn")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isOozoxUser) "إغلاق الفاتورة وإرسالها إلكترونياً للعميل ✓" else "إغلاق الفاتورة وتحويلها عبر السوشيال ميديا 📲",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
