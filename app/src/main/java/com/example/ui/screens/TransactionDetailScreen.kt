package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.components.DeleteInvoiceConfirmationDialog
import com.example.ui.theme.*
import com.example.util.GoldUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transaction: TransactionEntity?,
    onDeleteTransaction: ((String) -> Unit)? = null,
    onBackClick: () -> Unit
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (transaction == null) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GoldPrimary)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل صفقة الذهب", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = GoldPrimary)
                    }
                },
                actions = {
                    if (onDeleteTransaction != null) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("delete_transaction_button")
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "حذف سجل المعاملة",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("بيان فاتورة المعاملة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)

                    Spacer(modifier = Modifier.height(16.dp))

                    DetailRow("جهة التعامل:", transaction.contactName)
                    DetailRow("نوع الذهب:", transaction.goldType)
                    DetailRow("الوزن الإجمالي:", GoldUtils.formatWeight(transaction.weight))
                    DetailRow("سعر الجرام المسجل:", GoldUtils.formatCurrency(transaction.pricePerGram))
                    DetailRow("مصنعية الجرام:", GoldUtils.formatCurrency(transaction.manufacturingCostPerGram))

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

                    DetailRow("إجمالي قيمة الذهب الخالص:", GoldUtils.formatCurrency(transaction.totalGoldPrice))
                    DetailRow("إجمالي مبلغ المصنعية:", GoldUtils.formatCurrency(transaction.totalManufacturing))

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المبلغ الإجمالي الكلي:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                        Text(GoldUtils.formatCurrency(transaction.totalPrice), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = adaptiveGoldPrimary)
                    }

                    if (!transaction.notes.isNull_or_blank_safe()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("ملاحظات:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(transaction.notes!!, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    if (onDeleteTransaction != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_delete_invoice_card")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حذف هذه الفاتورة وسجل المعاملة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Delete Invoice / Transaction Confirmation Dialog Guard
    if (showDeleteConfirmDialog) {
        DeleteInvoiceConfirmationDialog(
            invoiceNumber = transaction.id.takeLast(8).uppercase(),
            partyName = transaction.contactName,
            amountFormatted = GoldUtils.formatCurrency(transaction.totalPrice),
            detailsSubtitle = "معاملة ذهب ${transaction.goldType} - ${GoldUtils.formatWeight(transaction.weight)}",
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                showDeleteConfirmDialog = false
                onDeleteTransaction?.invoke(transaction.id)
            }
        )
    }
}

private fun String?.isNull_or_blank_safe(): Boolean = this == null || this.isBlank()

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
