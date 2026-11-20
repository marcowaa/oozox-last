package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.WorkGroupEntity
import com.example.ui.theme.*
import com.example.util.acctStr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCompanyGroupDialog(
    parentGroups: List<WorkGroupEntity>,
    activeProfileId: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Boolean, String?, List<String>) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var companyName by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var capitalText by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var disableDMs by remember { mutableStateOf(false) }

    var selectedParentId by remember { mutableStateOf<String?>(null) }
    var departmentsInput by remember { mutableStateOf("") }

    val primaryGold = adaptiveGoldPrimary
    val onGold = adaptiveOnPrimary
    val greenColor = adaptiveStatusGreen
    val redColor = adaptiveStatusRed

    val isStep1Valid = companyName.isNotBlank() && groupName.isNotBlank()
    val isStep2Valid = capitalText.toDoubleOrNull() != null

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
                    .heightIn(max = 640.dp),
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
                                    Icon(Icons.Default.Business, contentDescription = null, tint = primaryGold, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = acctStr("wizard_company_title"),
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
                                        Text(acctStr("entity_info"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryGold)

                                        OutlinedTextField(
                                            value = companyName,
                                            onValueChange = { companyName = it },
                                            label = { Text(acctStr("company_name")) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryGold)
                                        )

                                        OutlinedTextField(
                                            value = groupName,
                                            onValueChange = { groupName = it },
                                            label = { Text(acctStr("branch_name")) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryGold)
                                        )
                                    }
                                    2 -> {
                                        Text(acctStr("hierarchy_budget"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryGold)

                                        OutlinedTextField(
                                            value = capitalText,
                                            onValueChange = { capitalText = it },
                                            label = { Text(acctStr("capital")) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryGold)
                                        )

                                        OutlinedTextField(
                                            value = departmentsInput,
                                            onValueChange = { departmentsInput = it },
                                            label = { Text(acctStr("departments_select")) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryGold)
                                        )

                                        if (parentGroups.isNotEmpty()) {
                                            Text(acctStr("parent_entity"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            parentGroups.forEach { parent ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedParentId = if (selectedParentId == parent.id) null else parent.id
                                                        }
                                                        .padding(vertical = 4.dp)
                                                ) {
                                                    RadioButton(
                                                        selected = selectedParentId == parent.id,
                                                        onClick = { selectedParentId = if (selectedParentId == parent.id) null else parent.id },
                                                        colors = RadioButtonDefaults.colors(selectedColor = primaryGold)
                                                    )
                                                    Text(parent.name, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }
                                    3 -> {
                                        Text(acctStr("privacy_rules"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryGold)

                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(acctStr("sec_vaults"), fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                                    Text(acctStr("sub_vaults"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Switch(
                                                    checked = isPrivate,
                                                    onCheckedChange = { isPrivate = it },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = onGold, checkedTrackColor = primaryGold)
                                                )
                                            }
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(acctStr("lock_dms_label"), fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                                    Text(acctStr("lock_dms_hint"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Switch(
                                                    checked = disableDMs,
                                                    onCheckedChange = { disableDMs = it },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = onGold, checkedTrackColor = primaryGold)
                                                )
                                            }
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
                                    val capital = capitalText.toDoubleOrNull() ?: 0.0
                                    val deps = departmentsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    onConfirm(groupName, companyName, capital, disableDMs, selectedParentId, deps)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = greenColor, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(acctStr("save"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
