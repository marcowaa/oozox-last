package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatAccountEntity
import com.example.data.model.GoldPricesModel
import com.example.data.model.HashtagAccountEntity
import com.example.data.model.HashtagEntryDirection
import com.example.data.model.HashtagEntryEntity
import com.example.data.model.HashtagEntryType
import com.example.data.model.InvoiceEntity
import com.example.data.model.InvoiceStatus
import com.example.data.model.MessageType
import com.example.data.model.ProjectEntity
import com.example.data.model.TransactionEntity
import com.example.ui.spatial.ChatSpatialMode
import com.example.ui.spatial.SpatialElevation
import com.example.ui.spatial.SpatialSurface
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusOrange
import com.example.ui.theme.StatusRed
import com.example.ui.theme.adaptiveGoldPrimary
import com.example.ui.theme.adaptiveOnPrimary
import com.example.util.GoldUtils
import com.example.util.ShortCodeGenerator

@Composable
internal fun ChatQuickHashtagDialog(
    hashtags: List<HashtagAccountEntity>,
    allProjects: List<ProjectEntity>,
    initialTag: String?,
    onRecordHashtagOperation: ((tag: String, entryType: HashtagEntryType, description: String, amount: Double, direction: HashtagEntryDirection, projectId: String?, projectName: String?, paymentMethod: String, isExternalBusiness: Boolean) -> Unit)?,
    onSendFallbackMessage: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val defaultTags = listOf("فاتورة", "توريد", "دفعة", "سداد", "ذهب", "عمل_خارجي")
    val existingTags = remember(hashtags) { hashtags.map { it.tag.trim().removePrefix("#") }.filter { it.isNotBlank() }.distinct() }
    val suggestedTags = remember(existingTags, initialTag) {
        (listOfNotNull(initialTag?.removePrefix("#")) + existingTags + defaultTags)
            .filter { it.isNotBlank() }
            .distinct()
    }

    var selectedTag by remember(initialTag) {
        mutableStateOf(initialTag?.removePrefix("#") ?: suggestedTags.firstOrNull() ?: "فاتورة")
    }
    var customTag by remember { mutableStateOf("") }
    var entryType by remember { mutableStateOf(HashtagEntryType.INVOICE) }
    var description by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var paymentMethod by remember { mutableStateOf("كاش") }
    var isExternalBusiness by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "قيد مالي وهاشتاج #${(customTag.ifBlank { selectedTag }).removePrefix("#")}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                color = GoldPrimary
                            )
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = amountStr,
                                onValueChange = { amountStr = it },
                                placeholder = { Text("5000", fontSize = 13.sp) },
                                label = { Text("المبلغ", fontSize = 12.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_hashtag_amount_input")
                            )
                            OutlinedTextField(
                                value = customTag,
                                onValueChange = { customTag = it.removePrefix("#") },
                                placeholder = { Text("توريد", fontSize = 13.sp) },
                                label = { Text("هاشتاج", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("بيان العملية", fontSize = 13.sp) },
                            label = { Text("الوصف", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quick_hashtag_desc_input")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = entryType == HashtagEntryType.INVOICE,
                                onClick = { entryType = HashtagEntryType.INVOICE },
                                label = { Text("فاتورة (عليه)", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = entryType == HashtagEntryType.PAYMENT,
                                onClick = { entryType = HashtagEntryType.PAYMENT },
                                label = { Text("سداد (دفع)", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text("هاشتاجات سريعة:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            suggestedTags.forEach { tagOption ->
                                val active = customTag.isBlank() && selectedTag == tagOption
                                FilterChip(
                                    selected = active,
                                    onClick = {
                                        selectedTag = tagOption
                                        customTag = ""
                                    },
                                    label = { Text("#$tagOption", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        if (allProjects.isNotEmpty()) {
                            Text("ربط بمشروع:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = selectedProjectId == null,
                                    onClick = { selectedProjectId = null },
                                    label = { Text("بدون مشروع", fontSize = 11.sp) }
                                )
                                allProjects.take(12).forEach { proj ->
                                    FilterChip(
                                        selected = selectedProjectId == proj.id,
                                        onClick = { selectedProjectId = proj.id },
                                        label = { Text(proj.name, fontSize = 11.sp, maxLines = 1) }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("كاش", "تحويل بنكي", "إنستا باي").forEach { method ->
                                FilterChip(
                                    selected = paymentMethod == method,
                                    onClick = { paymentMethod = method },
                                    label = { Text(method, fontSize = 11.sp) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("عمل خارجي مستقل", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = isExternalBusiness,
                                onCheckedChange = { isExternalBusiness = it }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("quick_hashtag_cancel_btn")
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val amt = amountStr.trim().toDoubleOrNull()
                                val finalTag = (customTag.ifBlank { selectedTag }).trim().removePrefix("#").ifBlank { "فاتورة" }
                                val finalDesc = description.trim().ifBlank {
                                    if (entryType == HashtagEntryType.INVOICE) "فاتورة #$finalTag" else "سداد #$finalTag"
                                }
                                if (amt != null && amt > 0.0) {
                                    val dir = if (entryType == HashtagEntryType.INVOICE) {
                                        HashtagEntryDirection.DEBIT
                                    } else {
                                        HashtagEntryDirection.CREDIT
                                    }
                                    val pickedProj = allProjects.find { it.id == selectedProjectId }
                                    if (onRecordHashtagOperation != null) {
                                        onRecordHashtagOperation(
                                            finalTag,
                                            entryType,
                                            finalDesc,
                                            amt,
                                            dir,
                                            pickedProj?.id,
                                            pickedProj?.name,
                                            paymentMethod,
                                            isExternalBusiness
                                        )
                                    } else {
                                        onSendFallbackMessage(
                                            "$finalDesc بمبلغ ${GoldUtils.formatCurrency(amt)} #$finalTag",
                                            if (entryType == HashtagEntryType.INVOICE) MessageType.INVOICE.name else MessageType.PAYMENT.name
                                        )
                                    }
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "أدخل مبلغاً صحيحاً أكبر من صفر", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("quick_hashtag_save_btn")
                        ) {
                            Text("حفظ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChatQuickPayInvoiceDialog(
    targetPayInv: InvoiceEntity,
    onDismiss: () -> Unit,
    onPayInvoice: (invoiceId: String, amount: Double, method: String, notes: String?) -> Unit
) {
    val context = LocalContext.current
    val remaining = (targetPayInv.totalAmount - targetPayInv.paidAmount).coerceAtLeast(0.0)
    var payAmountStr by remember(targetPayInv.id) {
        mutableStateOf(if (remaining > 0) remaining.toBigDecimal().stripTrailingZeros().toPlainString() else "")
    }
    var payMethod by remember(targetPayInv.id) { mutableStateOf("كاش") }
    var payNotes by remember(targetPayInv.id) { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "تسجيل سداد فاتورة ${targetPayInv.invoiceNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GoldPrimary
                    )
                    Text(
                        text = "المتبقي المستحق: ${GoldUtils.formatCurrency(remaining)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusOrange
                    )
                    OutlinedTextField(
                        value = payAmountStr,
                        onValueChange = { payAmountStr = it },
                        placeholder = { Text("مثال: 1500", fontSize = 13.sp) },
                        label = { Text("المبلغ المدفوع") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_pay_invoice_amount_input")
                    )
                    OutlinedTextField(
                        value = payNotes,
                        onValueChange = { payNotes = it },
                        placeholder = { Text("ملاحظات السداد", fontSize = 13.sp) },
                        label = { Text("ملاحظات (اختياري)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("كاش", "تحويل بنكي", "إنستا باي").forEach { method ->
                            FilterChip(
                                selected = payMethod == method,
                                onClick = { payMethod = method },
                                label = { Text(method, fontSize = 11.5.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                val amt = payAmountStr.trim().toDoubleOrNull()
                                if (amt != null && amt > 0.0) {
                                    onPayInvoice(targetPayInv.id, amt, payMethod, payNotes.trim().ifBlank { null })
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "أدخل مبلغ سداد صحيح", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("حفظ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChatHashtagInspectorDialog(
    inspectedTag: String,
    hashtags: List<HashtagAccountEntity>,
    allEntries: List<HashtagEntryEntity>,
    invoicesForChat: List<InvoiceEntity>,
    allInvoices: List<InvoiceEntity>,
    onDismiss: () -> Unit,
    onInspectInvoiceCode: (String) -> Unit,
    onOpenNewEntryForTag: (String) -> Unit
) {
    val cleanTag = inspectedTag.removePrefix("#").trim()
    val matchedAccounts = remember(hashtags, cleanTag) {
        hashtags.filter { it.tag.removePrefix("#").trim().equals(cleanTag, ignoreCase = true) }
    }
    val accountIds = remember(matchedAccounts) { matchedAccounts.map { it.id }.toSet() }
    val tagEntries = remember(allEntries, accountIds) {
        allEntries.filter { it.hashtagId in accountIds }.sortedByDescending { it.createdAt }
    }
    val combinedInvoices = remember(invoicesForChat, allInvoices, cleanTag, accountIds) {
        (invoicesForChat + allInvoices)
            .distinctBy { it.id }
            .filter { inv ->
                val invTag = inv.hashtagTag?.removePrefix("#")?.trim().orEmpty()
                invTag.equals(cleanTag, ignoreCase = true) || (inv.hashtagId != null && inv.hashtagId in accountIds)
            }
            .sortedByDescending { it.createdAt }
    }
    val tagCredit = tagEntries.filter { it.direction == HashtagEntryDirection.CREDIT.name }.sumOf { it.amount }
    val tagDebit = tagEntries.filter { it.direction == HashtagEntryDirection.DEBIT.name }.sumOf { it.amount }
    val tagBalance = tagCredit - tagDebit

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hashtag_inspector_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#$cleanTag",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = adaptiveGoldPrimary
                            )
                            Text(
                                text = "${tagEntries.size} عملية • ${combinedInvoices.size} فاتورة مرتبطة",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (tagBalance >= 0) StatusGreen.copy(alpha = 0.14f) else StatusRed.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "${if (tagBalance >= 0) "+" else ""}${GoldUtils.formatCurrency(tagBalance)}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tagBalance >= 0) StatusGreen else StatusRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusGreen.copy(alpha = 0.10f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("له / دائن", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(GoldUtils.formatCurrency(tagCredit), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusRed.copy(alpha = 0.10f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("عليه / مدين", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(GoldUtils.formatCurrency(tagDebit), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 310.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (combinedInvoices.isNotEmpty()) {
                            Text(
                                text = "الفواتير المرتبطة بالهاشتاج",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            combinedInvoices.forEach { inv ->
                                val isPaid = inv.status == InvoiceStatus.PAID.name || inv.paidAmount >= inv.totalAmount
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDismiss()
                                            onInspectInvoiceCode(inv.invoiceNumber)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${inv.invoiceNumber} • ${inv.title}",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = adaptiveGoldPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${inv.receiverName} • ${GoldUtils.formatCurrency(inv.totalAmount)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isPaid) StatusGreen.copy(alpha = 0.16f) else StatusOrange.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = if (isPaid) "مدفوعة" else "معلقة",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPaid) StatusGreen else StatusOrange,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (tagEntries.isNotEmpty()) {
                            Text(
                                text = "حركات الدفتر والقيود",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            tagEntries.take(15).forEach { entry ->
                                val isCreditEntry = entry.direction == HashtagEntryDirection.CREDIT.name
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.description.ifBlank { "قيد مالي" },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!entry.projectName.isNullOrBlank()) {
                                                Text(
                                                    text = "مشروع: ${entry.projectName}",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${if (isCreditEntry) "+" else "-"}${GoldUtils.formatCurrency(entry.amount)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCreditEntry) StatusGreen else StatusRed
                                        )
                                    }
                                }
                            }
                        }

                        if (combinedInvoices.isEmpty() && tagEntries.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "لا توجد قيود أو فواتير مسجلة لهذا الهاشتاج حتى الآن، يمكنك إضافة أول قيد مباشرة.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                onDismiss()
                                onOpenNewEntryForTag(cleanTag)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("hashtag_inspector_new_entry_btn")
                        ) {
                            Text("قيد جديد", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChatCodeInspectorDialog(
    inspectedCode: String,
    invoicesForChat: List<InvoiceEntity>,
    allInvoices: List<InvoiceEntity>,
    allTransactions: List<TransactionEntity>,
    canPayInvoices: Boolean,
    onDismiss: () -> Unit,
    onPayInvoiceTarget: (InvoiceEntity) -> Unit
) {
    val normCode = ShortCodeGenerator.normalize(inspectedCode)
    val matchedInvoice = remember(invoicesForChat, allInvoices, normCode) {
        (invoicesForChat + allInvoices)
            .distinctBy { it.id }
            .find {
                ShortCodeGenerator.normalize(it.invoiceNumber) == normCode ||
                    ShortCodeGenerator.normalize(it.id) == normCode
            }
    }
    val matchedTx = remember(allTransactions, normCode) {
        allTransactions.find {
            ShortCodeGenerator.normalize(it.id) == normCode ||
                (it.originalTransactionId != null && ShortCodeGenerator.normalize(it.originalTransactionId) == normCode)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("code_inspector_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (matchedInvoice != null) "تفاصيل الفاتورة" else if (matchedTx != null) "تفاصيل العملية" else "سجل المعاملة",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = normCode,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                color = adaptiveGoldPrimary
                            )
                        }
                        if (matchedInvoice != null) {
                            val isPaid = matchedInvoice.status == InvoiceStatus.PAID.name || matchedInvoice.paidAmount >= matchedInvoice.totalAmount
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isPaid) StatusGreen.copy(alpha = 0.16f) else StatusOrange.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = if (isPaid) "مدفوعة" else "معلقة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPaid) StatusGreen else StatusOrange,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    when {
                        matchedInvoice != null -> {
                            val remaining = (matchedInvoice.totalAmount - matchedInvoice.paidAmount).coerceAtLeast(0.0)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(matchedInvoice.title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("العميل: ${matchedInvoice.receiverName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("الإجمالي: ${GoldUtils.formatCurrency(matchedInvoice.totalAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("المدفوع: ${GoldUtils.formatCurrency(matchedInvoice.paidAmount)} • المتبقي: ${GoldUtils.formatCurrency(remaining)}", fontSize = 12.sp, color = if (remaining > 0.0) StatusOrange else StatusGreen)
                                    Text("وسيلة الدفع: ${matchedInvoice.paymentMethod}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (!matchedInvoice.hashtagTag.isNullOrBlank()) {
                                        Text("الهاشتاج: ${matchedInvoice.hashtagTag}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = adaptiveGoldPrimary)
                                    }
                                    if (!matchedInvoice.projectName.isNullOrBlank()) {
                                        Text("المشروع: ${matchedInvoice.projectName}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (!matchedInvoice.notes.isNullOrBlank()) {
                                        Text("ملاحظات: ${matchedInvoice.notes}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        matchedTx != null -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("${matchedTx.goldType} • ${matchedTx.goldKarat}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("الطرف: ${matchedTx.contactName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("الوزن: ${matchedTx.weight} جم • السعر الإجمالي: ${GoldUtils.formatCurrency(matchedTx.totalPrice)}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    Text("الحالة: ${matchedTx.status}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (!matchedTx.notes.isNullOrBlank()) {
                                        Text("البيان: ${matchedTx.notes}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "المعرف $normCode غير مرتبط بفاتورة أو حركة مسجلة حالياً في قاعدة البيانات.",
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }

                    val canPay = matchedInvoice != null && canPayInvoices && (matchedInvoice.totalAmount - matchedInvoice.paidAmount) > 0.0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.SemiBold)
                        }
                        if (canPay && matchedInvoice != null) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onPayInvoiceTarget(matchedInvoice)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = adaptiveGoldPrimary,
                                    contentColor = adaptiveOnPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("code_inspector_pay_btn")
                            ) {
                                Text("سداد", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ContextWorkspacePanel(
    mode: ChatSpatialMode,
    goldPrices: GoldPricesModel,
    accounts: List<ChatAccountEntity>,
    allEntries: List<HashtagEntryEntity>,
    onOpenLedgerSheet: () -> Unit,
    onOpenGoldDeal: () -> Unit,
    onOpenAIAccountant: () -> Unit
) {
    SpatialSurface(
        elevation = SpatialElevation.Level3,
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = GoldPrimary.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            when (mode) {
                ChatSpatialMode.GOLD -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("لوحة متابعة أسعار الذهب اليوم", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GoldPrimary)
                            Text("عيار 21: ${GoldUtils.formatCurrency(goldPrices.karat21)} | عيار 24: ${GoldUtils.formatCurrency(goldPrices.karat24)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = onOpenGoldDeal,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = adaptiveGoldPrimary,
                                contentColor = adaptiveOnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("صفقة جديدة +", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                ChatSpatialMode.FINANCIAL -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("الحساب المالي الجاري", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StatusGreen)
                            Text("${accounts.size} حسابات مالية مسجلة في المحادثة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = onOpenLedgerSheet,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("فتح الدفتر", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                ChatSpatialMode.AI_ACCOUNTANT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("مساعد المحاسب المتقدم", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFA78BFA))
                            Text("تحليل المعاملات واقتراح التوفير المالي تلقائياً", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = onOpenAIAccountant,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تحليل المحادثة", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("محادثة نشطة", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("اسحب للتنقل بين الأوضاع", fontSize = 11.sp, color = GoldAccent)
                    }
                }
            }
        }
    }
}
