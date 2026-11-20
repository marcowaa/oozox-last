package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.MerchantProductCategory
import com.example.data.model.MerchantProductEntity
import com.example.ui.theme.GoldPrimary
import com.example.util.ExtractedProductDataWithCorrections
import com.example.util.ProductImageAiOcrService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductOcrConfirmationDialog(
    initialRawText: String? = null,
    imageUri: String? = null,
    merchantShopName: String,
    currentStaffName: String? = null,
    onSaveConfirmedProduct: (MerchantProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val initialData = remember {
        ProductImageAiOcrService.processAndAutoCorrect(
            rawInputText = initialRawText,
            imageUri = imageUri
        )
    }

    var title by remember { mutableStateOf(initialData.title) }
    var karat by remember { mutableStateOf(initialData.karat) }
    var weightText by remember { mutableStateOf(initialData.weightGrams.toString()) }
    var barcodeText by remember { mutableStateOf(initialData.barcode ?: "") }
    var priceText by remember { mutableStateOf(initialData.estimatedPriceEGP.toInt().toString()) }
    var quantityText by remember { mutableStateOf("1") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("معالجة صورة المنتج والتصحيح التلقائي", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                    Text("استخراج تلقائي بدقة ${(initialData.confidenceScore * 100).toInt()}% • تأكيد قبل الحفظ", fontSize = 11.sp, color = GoldPrimary)
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Auto-Corrections Notice Card
                    if (initialData.rawExtractedText.isNotBlank()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.09f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تم فحص النصوص وإجراء التصحيحات التلقائية:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    }

                                    initialData.autoCorrectionsList.forEach { corr ->
                                        Text("• $corr", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Raw vs Corrected Comparison Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("النص الأصلي المقروء من الصورة:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = initialData.rawExtractedText,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(10.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 3. User Review & Confirmation Form
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("راجع البيانات المصححة واضغط تأكيد لحفظها في المخزن:", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("اسم المنتج / الصنف") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = weightText,
                                        onValueChange = { weightText = it },
                                        label = { Text("الوزن الصافي (جرام)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = karat,
                                        onValueChange = { karat = it },
                                        label = { Text("العيار المعتمد") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                OutlinedTextField(
                                    value = barcodeText,
                                    onValueChange = { barcodeText = it },
                                    label = { Text("الباركود / الكود التسلسلي") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = priceText,
                                        onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' } },
                                        label = { Text("سعر المستهلك المقترح (ج.م)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = quantityText,
                                        onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                                        label = { Text("الكمية") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        if (title.isNotBlank()) {
                                            val w = weightText.toDoubleOrNull() ?: 25.0
                                            val p = priceText.toDoubleOrNull() ?: (w * 3850.0)
                                            val q = quantityText.toDoubleOrNull() ?: 1.0

                                            val newProduct = MerchantProductEntity(
                                                id = "prod_ocr_${System.currentTimeMillis()}",
                                                merchantId = "merchant_store",
                                                merchantShopName = merchantShopName,
                                                title = title.trim(),
                                                category = MerchantProductCategory.BULLION.name,
                                                karat = karat,
                                                weightGrams = w,
                                                consumerPriceEGP = p,
                                                wholesalePriceEGP = p * 0.95,
                                                quantityInStock = q,
                                                addedByStaffName = currentStaffName ?: "الموظف المعتمد",
                                                photoUri = imageUri,
                                                extractedOcrText = initialData.rawExtractedText,
                                                extractedBarcode = barcodeText.ifBlank { null },
                                                extractedKeywords = "$title $karat $barcodeText",
                                                createdAt = System.currentTimeMillis()
                                            )
                                            onSaveConfirmedProduct(newProduct)
                                            Toast.makeText(context, "تم تأكيد وحفظ المنتج بنجاح في المخزن ✓", Toast.LENGTH_SHORT).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "يرجى كتابة اسم المنتج", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("confirm_auto_corrected_product_btn")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تأكيد وحفظ المنتج المصحح في المخزن ✓", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
