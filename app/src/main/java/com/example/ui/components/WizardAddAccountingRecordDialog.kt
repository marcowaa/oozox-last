package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import com.example.util.GoldUtils
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DailyAccountingRecordEntity
import com.example.data.model.WorkGroupEntity
import com.example.ui.theme.*
import com.example.util.ParsedVoiceAccountingRecord
import com.example.util.acctStr
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountingRecordDialog(
    groups: List<WorkGroupEntity>,
    defaultGroup: WorkGroupEntity?,
    activeProfileId: String?,
    onDismiss: () -> Unit,
    onConfirm: (DailyAccountingRecordEntity) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var selectedGroup by remember { mutableStateOf(defaultGroup ?: groups.firstOrNull()) }
    var entryMode by remember { mutableStateOf(0) } // 0: Revenue, 1: Expense, 2: Deficit

    var amountText by remember { mutableStateOf("") }
    var categoryNote by remember { mutableStateOf("مبيعات ذهب عيار 21") }
    var paymentMethod by remember { mutableStateOf("نقدي (كاش)") }
    var notes by remember { mutableStateOf("") }
    var selectedScreenshotUri by remember { mutableStateOf<String?>(null) }
    var showVoiceReviewDialog by remember { mutableStateOf<DailyAccountingRecordEntity?>(null) }

    val primaryGold = adaptiveGoldPrimary
    val onGold = adaptiveOnPrimary
    val greenColor = adaptiveStatusGreen
    val redColor = adaptiveStatusRed
    val orangeColor = adaptiveStatusOrange

    if (showVoiceReviewDialog != null) {
        val candRecord = showVoiceReviewDialog!!
        val isCredit = entryMode == 0
        VoiceTransactionReviewDialog(
            candidateRecord = candRecord,
            parsedVoiceRecord = ParsedVoiceAccountingRecord(
                rawSpokenText = categoryNote,
                amount = candRecord.incomingAmount + candRecord.dailyRevenue + candRecord.outgoingAmount + candRecord.deficitAmount,
                direction = if (isCredit) "CREDIT" else "DEBIT",
                operationCategory = if (isCredit) "مقبوضات" else "مصروفات",
                suggestedTag = if (isCredit) "#مبيعات" else if (entryMode == 1) "#مصروفات" else "#عجز_وتسوية",
                counterPartyName = "المدير"
            ),
            onConfirmDatabaseUpdate = { finalRec ->
                showVoiceReviewDialog = null
                onConfirm(finalRec)
            },
            onDismiss = {
                showVoiceReviewDialog = null
            }
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedScreenshotUri = uri.toString()
        }
    }

    val revenueCategories = listOf("مبيعات ذهب عيار 21", "مبيعات ذهب عيار 18", "تحصيل دفعة عميل", "إيراد مصنعية")
    val expenseCategories = listOf("شراء ذهب كسر", "رواتب وعمالة", "نثريات محل", "إيجار ومرافق")
    val deficitCategories = listOf("عجز بالخزينة", "فارق جرد يومي", "تسوية حسابات")
    val paymentMethods = listOf("نقدي", "تحويل بنكي", "محافظ إلكترونية", "شيك")

    val isStep1Valid = selectedGroup != null
    val isStep2Valid = (amountText.toDoubleOrNull() ?: 0.0) > 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = 650.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            primaryGold.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = primaryGold.copy(alpha = 0.14f),
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = primaryGold, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = acctStr("cmd_new_record"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = acctStr("step_of_3").format(step),
                            fontSize = 12.sp,
                            color = primaryGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { step / 3f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = primaryGold,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        AnimatedContent(
                            targetState = step,
                            transitionSpec = { fadeIn(tween(260)).togetherWith(fadeOut(tween(260))) },
                            label = "wizard_content"
                        ) { currentStep ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                when (currentStep) {
                                    1 -> {
                                        Text(acctStr("entry_type"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryGold)

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            val modes = listOf(
                                                acctStr("type_in") to 0,
                                                acctStr("type_out") to 1,
                                                acctStr("type_deficit") to 2
                                            )
                                            modes.forEach { (label, modeIdx) ->
                                                val isSelected = entryMode == modeIdx
                                                val color = when (modeIdx) {
                                                    0 -> greenColor
                                                    1 -> redColor
                                                    else -> orangeColor
                                                }
                                                Surface(
                                                    color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, if (isSelected) color else Color.Transparent),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .heightIn(min = 48.dp)
                                                        .clickable {
                                                            entryMode = modeIdx
                                                            categoryNote = when (modeIdx) {
                                                                0 -> revenueCategories.first()
                                                                1 -> expenseCategories.first()
                                                                else -> deficitCategories.first()
                                                            }
                                                        }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Text(acctStr("target_entity"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            groups.forEach { group ->
                                                val isSelected = selectedGroup?.id == group.id
                                                Surface(
                                                    color = if (isSelected) primaryGold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, if (isSelected) primaryGold else Color.Transparent),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = 48.dp)
                                                        .clickable { selectedGroup = group }
                                                ) {
                                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            if (group.parentGroupId == null) Icons.Default.Business else Icons.Default.Folder,
                                                            contentDescription = null,
                                                            tint = if (isSelected) primaryGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            group.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) primaryGold else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    2 -> {
                                        Text(acctStr("amount_currency"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryGold)

                                        OutlinedTextField(
                                            value = amountText,
                                            onValueChange = { amountText = it },
                                            label = { Text(acctStr("amount_currency")) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(),
                                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = primaryGold) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryGold)
                                        )

                                        val currentCats = when (entryMode) {
                                            0 -> revenueCategories
                                            1 -> expenseCategories
                                            else -> deficitCategories
                                        }

                                        Text(acctStr("category"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            currentCats.chunked(2).forEach { rowCats ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    rowCats.forEach { cat ->
                                                        val isSelected = categoryNote == cat
                                                        Surface(
                                                            color = if (isSelected) primaryGold else MaterialTheme.colorScheme.surfaceVariant,
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .heightIn(min = 44.dp)
                                                                .clickable { categoryNote = cat }
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                                                Text(
                                                                    cat,
                                                                    fontSize = 11.5.sp,
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (isSelected) onGold else MaterialTheme.colorScheme.onSurface,
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (entryMode != 2) {
                                            Text(acctStr("payment_method"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                paymentMethods.forEach { method ->
                                                    FilterChip(
                                                        selected = paymentMethod == method,
                                                        onClick = { paymentMethod = method },
                                                        label = { Text(method, fontSize = 11.sp) },
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = primaryGold.copy(alpha = 0.2f),
                                                            selectedLabelColor = primaryGold
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    3 -> {
                                        Text(acctStr("review_attach"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryGold)

                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(acctStr("entry_type"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(
                                                        text = when (entryMode) {
                                                            0 -> acctStr("type_in")
                                                            1 -> acctStr("type_out")
                                                            else -> acctStr("type_deficit")
                                                        },
                                                        fontSize = 12.sp,
                                                        color = primaryGold,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(acctStr("amount_currency"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(
                                                        GoldUtils.formatCurrency(amountText.toDoubleOrNull() ?: 0.0),
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(acctStr("category"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(categoryNote, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = notes,
                                            onValueChange = { notes = it },
                                            label = { Text(acctStr("notes_optional")) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedButton(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, primaryGold)
                                        ) {
                                            Icon(
                                                if (selectedScreenshotUri != null) Icons.Default.CheckCircle else Icons.Default.Image,
                                                contentDescription = null,
                                                tint = primaryGold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                if (selectedScreenshotUri != null) acctStr("receipt_attached") else acctStr("attach_receipt"),
                                                color = primaryGold,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                                val curDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                                val candRecord = DailyAccountingRecordEntity(
                                                    id = UUID.randomUUID().toString(),
                                                    groupId = selectedGroup?.id ?: "",
                                                    subGroupId = selectedGroup?.name ?: "",
                                                    profileId = activeProfileId,
                                                    entryDate = curDate,
                                                    staffId = "current-user",
                                                    staffName = "المدير",
                                                    staffRole = "مدير مالي",
                                                    dailyRevenue = if (entryMode == 0) amount else 0.0,
                                                    incomingAmount = 0.0,
                                                    outgoingAmount = if (entryMode == 1) amount else 0.0,
                                                    deficitAmount = if (entryMode == 2) amount else 0.0,
                                                    notes = if (notes.isNotBlank()) "$categoryNote - $notes" else categoryNote,
                                                    screenshotUri = selectedScreenshotUri,
                                                    timestamp = System.currentTimeMillis()
                                                )
                                                showVoiceReviewDialog = candRecord
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, primaryGold.copy(alpha = 0.6f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryGold)
                                        ) {
                                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = primaryGold, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(acctStr("voice_review"), fontWeight = FontWeight.Bold, color = primaryGold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step > 1) {
                            OutlinedButton(
                                onClick = { step-- },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(acctStr("back"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            TextButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(acctStr("cancel"), color = redColor)
                            }
                        }

                        if (step < 3) {
                            Button(
                                onClick = { step++ },
                                enabled = if (step == 1) isStep1Valid else isStep2Valid,
                                colors = ButtonDefaults.buttonColors(containerColor = primaryGold, contentColor = onGold),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(acctStr("next"), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val amount = amountText.toDoubleOrNull() ?: 0.0
                                    val curDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                                    val finalRecord = DailyAccountingRecordEntity(
                                        id = UUID.randomUUID().toString(),
                                        groupId = selectedGroup?.id ?: "",
                                        subGroupId = selectedGroup?.name ?: "",
                                        profileId = activeProfileId,
                                        entryDate = curDate,
                                        staffId = "current-user",
                                        staffName = "المدير",
                                        staffRole = "مدير مالي",
                                        dailyRevenue = if (entryMode == 0) amount else 0.0,
                                        incomingAmount = 0.0,
                                        outgoingAmount = if (entryMode == 1) amount else 0.0,
                                        deficitAmount = if (entryMode == 2) amount else 0.0,
                                        notes = if (notes.isNotBlank()) "$categoryNote - $notes" else categoryNote,
                                        screenshotUri = selectedScreenshotUri,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    onConfirm(finalRecord)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = greenColor, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(acctStr("save_approve"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
