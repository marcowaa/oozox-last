package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FinancialComplaintEntity
import com.example.data.model.WorkGroupEntity
import com.example.ui.theme.*
import com.example.util.LocalAppLanguage
import com.example.util.Translations
import com.example.util.acctStr
import com.example.util.tr
import java.text.SimpleDateFormat
import java.util.*

/**
 * Executive Dialog to submit a retroactive financial adjustment or dispute claim.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetroactiveComplaintDialog(
    groups: List<WorkGroupEntity>,
    initialGroup: WorkGroupEntity? = null,
    currentStaffName: String = "",
    currentStaffRole: String = "",
    currentStaffId: String = "staff_current",
    onDismiss: () -> Unit,
    onSubmitComplaint: (FinancialComplaintEntity) -> Unit
) {
    val lang = LocalAppLanguage.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val today = remember { dateFormat.format(Date()) }
    val yesterday = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        dateFormat.format(cal.time)
    }
    val twoDaysAgo = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -2)
        dateFormat.format(cal.time)
    }

    val categories = remember(lang) {
        listOf(
            Translations.get("retro.cat_deficit", lang),
            Translations.get("retro.cat_pos_gap", lang),
            Translations.get("retro.cat_return", lang),
            Translations.get("retro.cat_craft", lang),
            Translations.get("retro.cat_ledger", lang),
            Translations.get("retro.cat_other", lang)
        )
    }

    var selectedGroup by remember { mutableStateOf(initialGroup ?: groups.firstOrNull()) }
    val defaultDept = tr("retro.main_treasury")
    val defaultStaffName = tr("lock.default_user")
    val defaultStaffRole = tr("retro.default_staff_role")
    var targetDate by remember { mutableStateOf(yesterday) }
    var customDateInput by remember { mutableStateOf("") }
    var isCustomDate by remember { mutableStateOf(false) }

    var impactType by remember { mutableStateOf("DEDUCTION") }
    var amountText by remember { mutableStateOf("") }
    var category by remember(categories) { mutableStateOf(categories.first()) }
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var staffName by remember(currentStaffName, defaultStaffName) {
        mutableStateOf(currentStaffName.ifBlank { defaultStaffName })
    }
    var staffRole by remember(currentStaffRole, defaultStaffRole) {
        mutableStateOf(currentStaffRole.ifBlank { defaultStaffRole })
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val primaryGold = adaptiveGoldPrimary
    val redColor = adaptiveStatusRed
    val greenColor = adaptiveStatusGreen

    val effectiveDate = if (isCustomDate && customDateInput.isNotBlank()) customDateInput.trim() else targetDate
    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0

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
                    .heightIn(max = 650.dp)
                    .testTag("retroactive_complaint_dialog"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            primaryGold.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                color = if (impactType == "DEDUCTION") redColor.copy(alpha = 0.15f) else greenColor.copy(alpha = 0.15f),
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (impactType == "DEDUCTION") Icons.Default.ReportProblem else Icons.Default.AddCircleOutline,
                                        contentDescription = null,
                                        tint = if (impactType == "DEDUCTION") redColor else greenColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    acctStr("retro_title"),
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${acctStr("retro_affected_note")} $effectiveDate",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Close, contentDescription = acctStr("close"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (groups.size > 1) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(acctStr("target_entity"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primaryGold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    groups.take(3).forEach { g ->
                                        val isSelected = selectedGroup?.id == g.id
                                        Surface(
                                            color = if (isSelected) primaryGold.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, if (isSelected) primaryGold else Color.Transparent),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 42.dp)
                                                .clickable { selectedGroup = g }
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
                                                Text(
                                                    g.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) primaryGold else MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Target Date Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(acctStr("target_date"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primaryGold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    acctStr("filter_today") to today,
                                    acctStr("filter_yesterday") to yesterday,
                                    twoDaysAgo.takeLast(5) to twoDaysAgo
                                ).forEach { (label, dateVal) ->
                                    val isSelected = !isCustomDate && targetDate == dateVal
                                    Surface(
                                        color = if (isSelected) primaryGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, if (isSelected) primaryGold else Color.Transparent),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 42.dp)
                                            .clickable {
                                                isCustomDate = false
                                                targetDate = dateVal
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                                            Text(
                                                label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) primaryGold else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isCustomDate = !isCustomDate },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isCustomDate,
                                    onCheckedChange = { isCustomDate = it }
                                )
                                Text(acctStr("retro_date_custom"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }

                            if (isCustomDate) {
                                OutlinedTextField(
                                    value = customDateInput,
                                    onValueChange = { customDateInput = it },
                                    label = { Text(acctStr("target_date")) },
                                    placeholder = { Text("YYYY-MM-DD") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryGold,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                            }
                        }

                        // Impact Type
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(acctStr("retro_impact"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primaryGold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = if (impactType == "DEDUCTION") redColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, if (impactType == "DEDUCTION") redColor else Color.Transparent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clickable { impactType = "DEDUCTION" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.RemoveCircle, contentDescription = null, tint = redColor, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            acctStr("retro_deduct"),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (impactType == "DEDUCTION") redColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Surface(
                                    color = if (impactType == "ADDITION") greenColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, if (impactType == "ADDITION") greenColor else Color.Transparent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clickable { impactType = "ADDITION" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = greenColor, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            acctStr("retro_add"),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (impactType == "ADDITION") greenColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Amount
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text(acctStr("claim_amount")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = primaryGold)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("complaint_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryGold,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        // Category Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(acctStr("claim_type"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primaryGold)
                            categories.chunked(2).forEach { rowCats ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    rowCats.forEach { cat ->
                                        val isSelected = category == cat
                                        Surface(
                                            color = if (isSelected) primaryGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, if (isSelected) primaryGold else Color.Transparent),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 38.dp)
                                                .clickable { category = cat }
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)) {
                                                Text(
                                                    cat,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) primaryGold else MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Title & Details
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(acctStr("dispute_title")) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryGold,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        OutlinedTextField(
                            value = details,
                            onValueChange = { details = it },
                            label = { Text(acctStr("dispute_details")) },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryGold,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = staffName,
                                onValueChange = { staffName = it },
                                label = { Text(acctStr("staff_name")) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryGold,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                            OutlinedTextField(
                                value = staffRole,
                                onValueChange = { staffRole = it },
                                label = { Text(acctStr("job_title")) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryGold,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 12.sp,
                                color = redColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(acctStr("cancel"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        val claimAmountErr = acctStr("claim_amount")
                        val disputeTitleErr = acctStr("dispute_title")
                        val disputeDetailsErr = acctStr("dispute_details")
                        val targetDateErr = acctStr("target_date")
                        Button(
                            onClick = {
                                if (parsedAmount <= 0) {
                                    errorMessage = claimAmountErr
                                    return@Button
                                }
                                if (title.isBlank()) {
                                    errorMessage = disputeTitleErr
                                    return@Button
                                }
                                if (details.isBlank()) {
                                    errorMessage = disputeDetailsErr
                                    return@Button
                                }
                                if (effectiveDate.isBlank()) {
                                    errorMessage = targetDateErr
                                    return@Button
                                }

                                val complaint = FinancialComplaintEntity(
                                    id = UUID.randomUUID().toString(),
                                    groupId = selectedGroup?.id ?: "company_group_main",
                                    subGroupId = defaultDept,
                                    profileId = selectedGroup?.id,
                                    submittedByStaffId = currentStaffId,
                                    submittedByStaffName = staffName.trim(),
                                    submittedByStaffRole = staffRole.trim(),
                                    title = title.trim(),
                                    details = details.trim(),
                                    targetDate = effectiveDate,
                                    impactType = impactType,
                                    amount = parsedAmount,
                                    category = category,
                                    status = "APPLIED",
                                    timestamp = System.currentTimeMillis()
                                )

                                onSubmitComplaint(complaint)
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .heightIn(min = 48.dp)
                                .testTag("submit_complaint_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (impactType == "DEDUCTION") redColor else greenColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                acctStr("approve_adjust"),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
